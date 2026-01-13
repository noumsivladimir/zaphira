package com.zaphira.transaction.service;

import com.zaphira.common.exception.ResourceNotFoundException;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.event.SettlementCompletedEvent;
import com.zaphira.transaction.exception.ExchangeRateException;
import com.zaphira.transaction.model.TransactionSettlement;
import com.zaphira.transaction.model.enums.CurrencyCode;
import com.zaphira.transaction.model.enums.SettlementStatus;
import com.zaphira.transaction.repository.TransactionSettlementRepository;
import com.zaphira.transaction.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SettlementService - Multi-currency transaction settlement.
 * 
 * Responsibilities:
 * 1. Create settlement records with FX conversion
 * 2. Calculate and apply fees
 * 3. Track settlement status (PENDING → PROCESSING → COMPLETED)
 * 4. Retry failed settlements
 * 5. Publish settlement completion events
 * 6. Provide settlement audit trail
 * 
 * Settlement Flow:
 * 1. Transaction completed → Create settlement record
 * 2. Apply FX conversion if currencies differ
 * 3. Calculate and deduct fees (FX + service)
 * 4. Update wallet with net amount
 * 5. Publish SettlementCompletedEvent
 * 6. Audit trail for compliance
 * 
 * Patterns (from Phase 2):
 * - @Transactional for atomic settlement
 * - JWT extraction for audit trail
 * - Kafka event publishing
 * - Scheduled retry for failed settlements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {
    
    private final TransactionSettlementRepository settlementRepository;
    private final ExchangeRateService exchangeRateService;
    private final MultiCurrencyFeeService feeService;
    private final TransactionServiceImpl transactionServiceImpl;
    @Nullable
    private final KafkaTemplate<String, SettlementCompletedEvent> kafkaTemplate;
    
    private static final String SETTLEMENT_TOPIC = "settlement-completed";
    private static final Integer MAX_SETTLEMENT_RETRIES = 3;
    
    // ============================================================
    // CREATE SETTLEMENT
    // ============================================================
    
    /**
     * Create settlement record for a transaction.
     * 
     * Flow:
     * 1. Get transaction details
     * 2. Determine settlement currency (usually merchant's home currency)
     * 3. Convert amount if currencies differ
     * 4. Calculate fees (FX + service)
     * 5. Calculate net amount
     * 6. Save settlement record
     * 7. Publish event
     * 
     * @param transactionId Transaction to settle
     * @param settlementCurrency Currency to settle into
     * @return Created settlement
     * @throws ResourceNotFoundException if transaction not found
     * @throws ExchangeRateException if FX rate unavailable
     */
    @Transactional
    public TransactionSettlement createSettlement(Long transactionId, CurrencyCode settlementCurrency) {
        log.info(
            "[SETTLEMENT_CREATE_START] TransactionId: {}, SettlementCurrency: {}",
            transactionId, settlementCurrency
        );
        
        // Get transaction
        TransactionDTO transaction = transactionServiceImpl.getTransactionById(transactionId);
        if (transaction == null) {
            throw new ResourceNotFoundException("Transaction not found: " + transactionId);
        }
        
        BigDecimal originalAmount = transaction.getAmount();
        CurrencyCode originalCurrency = CurrencyCode.valueOf(transaction.getCurrency());
        
        // Convert amount if needed
        BigDecimal convertedAmount = originalAmount;
        BigDecimal fxRate = BigDecimal.ONE;
        String fxProvider = null;
        
        if (!originalCurrency.equals(settlementCurrency)) {
            log.info("[SETTLEMENT_CONVERT] Converting {} {} → {}", originalAmount, originalCurrency, settlementCurrency);
            
            var exchangeRate = exchangeRateService.getExchangeRate(originalCurrency, settlementCurrency);
            convertedAmount = exchangeRate.convert(originalAmount);
            fxRate = exchangeRate.getRate();
            fxProvider = exchangeRate.getProvider();
            
            log.info("[SETTLEMENT_CONVERTED] {} → {} {}", originalAmount, convertedAmount, settlementCurrency);
        }
        
        // Calculate fees
        var feeBreakdown = feeService.calculateTotalFees(
            originalAmount, convertedAmount, originalCurrency, settlementCurrency
        );
        
        // Create settlement record
        TransactionSettlement settlement = TransactionSettlement.builder()
            .transactionId(transactionId)
            .status(SettlementStatus.PENDING)
            .originalAmount(originalAmount)
            .originalCurrency(originalCurrency)
            .settledAmount(convertedAmount)
            .settlementCurrency(settlementCurrency)
            .fxRate(fxRate)
            .fxRateProvider(fxProvider)
            .fxFee(feeBreakdown.getFxFee())
            .serviceFee(feeBreakdown.getServiceFee())
            .totalFees(feeBreakdown.getTotalFees())
            .netAmount(feeBreakdown.getNetAmount())
            .retryCount(0)
            .createdAt(LocalDateTime.now())
            .build();
        
        TransactionSettlement saved = settlementRepository.save(settlement);
        
        log.info(
            "[SETTLEMENT_CREATED] SettlementId: {}, Status: PENDING, NetAmount: {} {}",
            saved.getId(), saved.getNetAmount(), settlementCurrency
        );
        
        return saved;
    }
    
    // ============================================================
    // SETTLEMENT PROCESSING & STATUS
    // ============================================================
    
    /**
     * Process settlement - mark as PROCESSING and attempt settlement.
     * 
     * @param settlementId Settlement to process
     * @return Updated settlement
     */
    @Transactional
    public TransactionSettlement processSettlement(Long settlementId) {
        TransactionSettlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + settlementId));
        
        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new IllegalStateException(
                "Settlement can only be processed from PENDING status, current: " + settlement.getStatus()
            );
        }
        
        settlement.setStatus(SettlementStatus.PROCESSING);
        settlement.setUpdatedAt(LocalDateTime.now());
        
        log.info("[SETTLEMENT_PROCESS] SettlementId: {}, Status: PROCESSING", settlementId);
        
        return settlementRepository.save(settlement);
    }
    
    /**
     * Mark settlement as COMPLETED.
     * 
     * @param settlementId Settlement to complete
     * @return Updated settlement
     */
    @Transactional
    public TransactionSettlement completeSettlement(Long settlementId) {
        TransactionSettlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + settlementId));
        
        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setSettlementDate(LocalDateTime.now());
        settlement.setUpdatedAt(LocalDateTime.now());
        
        log.info(
            "[SETTLEMENT_COMPLETE] SettlementId: {}, NetAmount: {} {}",
            settlementId, settlement.getNetAmount(), settlement.getSettlementCurrency()
        );
        
        TransactionSettlement saved = settlementRepository.save(settlement);
        
        // Publish event
        publishSettlementCompletedEvent(saved);
        
        return saved;
    }
    
    /**
     * Mark settlement as FAILED and increment retry count.
     * 
     * @param settlementId Settlement that failed
     * @param reason Failure reason
     * @return Updated settlement
     */
    @Transactional
    public TransactionSettlement failSettlement(Long settlementId, String reason) {
        TransactionSettlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + settlementId));
        
        settlement.setStatus(SettlementStatus.FAILED);
        settlement.setFailureReason(reason);
        settlement.setRetryCount(settlement.getRetryCount() + 1);
        settlement.setUpdatedAt(LocalDateTime.now());
        
        log.warn(
            "[SETTLEMENT_FAILED] SettlementId: {}, Reason: {}, RetryCount: {}",
            settlementId, reason, settlement.getRetryCount()
        );
        
        return settlementRepository.save(settlement);
    }
    
    // ============================================================
    // SETTLEMENT RETRIEVAL
    // ============================================================
    
    /**
     * Get settlement by transaction ID.
     * 
     * @param transactionId Transaction ID
     * @return Settlement if exists
     */
    public TransactionSettlement getSettlementByTransaction(Long transactionId) {
        return settlementRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Settlement not found for transaction: " + transactionId
            ));
    }
    
    /**
     * List settlements by status (for batch processing).
     * 
     * @param status Settlement status
     * @return List of settlements
     */
    public List<TransactionSettlement> listSettlementsByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status);
    }
    
    /**
     * Get pending settlements for processing.
     * 
     * @return List of PENDING settlements
     */
    public List<TransactionSettlement> getPendingSettlements() {
        return listSettlementsByStatus(SettlementStatus.PENDING);
    }
    
    // ============================================================
    // RETRY FAILED SETTLEMENTS
    // ============================================================
    
    /**
     * Scheduled task to retry failed settlements.
     * 
     * Runs every 5 minutes to attempt failed settlements.
     * Max 3 retries per settlement.
     */
    @Scheduled(fixedRateString = "${settlement.retry-interval:300000}") // 5 minutes
    public void retryFailedSettlements() {
        log.info("[SETTLEMENT_RETRY] Starting failed settlement retry");
        
        List<TransactionSettlement> retryableSettlements = 
            settlementRepository.findRetryableSettlements(MAX_SETTLEMENT_RETRIES);
        
        log.info("[SETTLEMENT_RETRY] Found {} retryable settlements", retryableSettlements.size());
        
        for (TransactionSettlement settlement : retryableSettlements) {
            try {
                // Re-attempt settlement
                processSettlement(settlement.getId());
                completeSettlement(settlement.getId());
                log.info("[SETTLEMENT_RETRY_SUCCESS] SettlementId: {}", settlement.getId());
            } catch (Exception e) {
                log.warn(
                    "[SETTLEMENT_RETRY_FAIL] SettlementId: {}, Error: {}",
                    settlement.getId(), e.getMessage()
                );
                failSettlement(settlement.getId(), e.getMessage());
            }
        }
        
        log.info("[SETTLEMENT_RETRY] Completed");
    }
    
    // ============================================================
    // EVENT PUBLISHING
    // ============================================================
    
    /**
     * Publish settlement completion event to Kafka.
     * 
     * Triggers:
     * - Wallet update with net amount
     * - Notification to merchant
     * - Accounting/reporting updates
     * 
     * @param settlement Completed settlement
     */
    private void publishSettlementCompletedEvent(TransactionSettlement settlement) {
        AuthenticatedUser user = getAuthenticatedUser();
        
        SettlementCompletedEvent event = SettlementCompletedEvent.builder()
            .settlementId(settlement.getId())
            .transactionId(settlement.getTransactionId().toString())
            .originalAmount(settlement.getOriginalAmount())
            .originalCurrency(settlement.getOriginalCurrency().name())
            .settledAmount(settlement.getSettledAmount())
            .settlementCurrency(settlement.getSettlementCurrency().name())
            .fxRate(settlement.getFxRate())
            .totalFees(settlement.getTotalFees())
            .netAmount(settlement.getNetAmount())
            .settlementDate(settlement.getSettlementDate())
            .processedBy(user != null ? user.getEmail() : "SYSTEM")
            .build();
        
        if (kafkaTemplate != null) {
            kafkaTemplate.send(SETTLEMENT_TOPIC, settlement.getTransactionId().toString(), event);
            log.info("[KAFKA_PUBLISH] SettlementCompletedEvent sent for settlement: {}", settlement.getId());
        } else {
            log.warn("[KAFKA_SKIP] KafkaTemplate unavailable, skipping SettlementCompletedEvent publication for settlement: {}", settlement.getId());
        }
    }
    
    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    /**
     * Extract authenticated user from SecurityContext (JWT extraction).
     * 
     * Pattern from Phase 1 & 2.
     * 
     * @return AuthenticatedUser or null if not authenticated
     */
    private AuthenticatedUser getAuthenticatedUser() {
        try {
            return (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }
}
