package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.TransactionReversalRequest;
import com.zaphira.transaction.dto.TransactionReversalResponse;
import com.zaphira.transaction.kafka.event.TransactionReversedEvent;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.TransactionAuditLog;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.TransactionAuditLogRepository;
import com.zaphira.transaction.security.AuthenticatedUser;
import com.zaphira.transaction.security.TransactionAuthorizationService;
import com.zaphira.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service pour gérer les reversals (annulation) de transactions.
 * 
 * Responsabilités:
 * 1. Vérifier les autorisations via TransactionAuthorizationService
 * 2. Créer une transaction compensatoire
 * 3. Mettre à jour les soldes des wallets de manière atomique
 * 4. Enregistrer l'opération dans l'audit log
 * 5. Publier l'événement TransactionReversedEvent
 * 
 * IMPORTANT: Opération atomique et sécurisée
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionReversalService {
    
    private final TransactionRepository transactionRepository;
    private final TransactionAuditLogRepository auditLogRepository;
    private final TransactionAuthorizationService authorizationService;
    @Nullable
    private final KafkaTemplate<String, TransactionReversedEvent> kafkaTemplate;
    private final WalletService walletService; // Service pour mettre à jour les wallets
    
    private static final String REVERSAL_TOPIC = "transaction-reversed";
    
    /**
     * Effectue un reversal de transaction.
     * 
     * @param user L'utilisateur authentifié qui effectue le reversal
     * @param userRoles Les rôles de l'utilisateur
     * @param userPermissions Les permissions de l'utilisateur
     * @param request La requête de reversal
     * @param ipAddress L'adresse IP du client
     * @param userAgent L'user-agent du client
     * @param requestId L'ID de la requête pour traçabilité distribuée
     * @return La réponse du reversal
     * @throws ResourceNotFoundException Si la transaction n'existe pas
     * @throws AccessDeniedException Si l'utilisateur n'est pas autorisé
     */
    @Transactional
    public TransactionReversalResponse reverse(
        AuthenticatedUser user,
        java.util.List<String> userRoles,
        java.util.List<String> userPermissions,
        TransactionReversalRequest request,
        String ipAddress,
        String userAgent,
        String requestId) {
        
        log.info("Starting reversal for transaction {} by user {}", request.getTransactionId(), user.getId());
        
        // 1. Récupérer la transaction
        Transaction originalTransaction = transactionRepository.findById(request.getTransactionId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction not found: " + request.getTransactionId()
            ));
        
        // 2. Autoriser le reversal (vérifications multi-niveaux)
        authorizationService.authorizeReversal(
            user,
            userRoles,
            userPermissions,
            originalTransaction,
            request.getAmount()
        );
        
        LocalDateTime now = LocalDateTime.now();
        String reversalReference = "REV-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        BigDecimal reversalFees = request.getIncludesFees() != null && request.getIncludesFees() 
            ? originalTransaction.getFeeAmount() 
            : BigDecimal.ZERO;
        
        try {
            // 3. Créer la transaction compensatoire
            Transaction reversalTransaction = Transaction.builder()
                .reference(reversalReference)
                // Inverser les wallets: sender devient receiver et vice-versa
                .senderWalletNumber(originalTransaction.getReceiverWalletNumber())
                .receiverWalletNumber(originalTransaction.getSenderWalletNumber())
                .senderWallet(originalTransaction.getReceiverWallet())
                .receiverWallet(originalTransaction.getSenderWallet())
                .amount(request.getAmount())
                .currency(originalTransaction.getCurrency())
                .feeAmount(reversalFees)
                .feeCurrency(originalTransaction.getFeeCurrency())
                .feeType(originalTransaction.getFeeType())
                .type(TransactionType.REVERSAL)
                .status(TransactionStatus.COMPLETED)
                .channel(originalTransaction.getChannel())
                .description("Reversal of transaction " + originalTransaction.getReference() + ". Reason: " + request.getReason())
                .initiatedBy(user.getEmail())
                .lastUpdatedBy(user.getEmail())
                .metadata("{\"original_transaction_id\": \"" + originalTransaction.getId() + 
                         "\", \"reason\": \"" + request.getReason() + "\"}")
                .build();
            
            // Sauvegarder la transaction de reversal
            Transaction savedReversalTx = transactionRepository.save(reversalTransaction);
            
            // 4. Mettre à jour le statut de la transaction originale
            originalTransaction.setStatus(TransactionStatus.REVERSED);
            originalTransaction.setLastUpdatedBy(user.getEmail());
            transactionRepository.save(originalTransaction);
            
            // 5. Mettre à jour les soldes des wallets de manière atomique
            updateWalletBalancesForReversal(
                originalTransaction,
                request.getAmount(),
                reversalFees
            );
            
            // 6. Enregistrer dans l'audit log
            recordAuditLog(
                user,
                userRoles.stream().findFirst().orElse("UNKNOWN"),
                originalTransaction.getId(),
                "REVERSE",
                request.getAmount(),
                originalTransaction.getCurrency(),
                request.getReason(),
                TransactionStatus.COMPLETED.toString(),
                TransactionStatus.REVERSED.toString(),
                "SUCCESS",
                null,
                ipAddress,
                userAgent,
                requestId
            );
            
            // 7. Publier l'événement Kafka
            publishReversalEvent(user, userRoles, originalTransaction, reversalTransaction, request, requestId);
            
            log.info("Reversal completed successfully for transaction {}", request.getTransactionId());
            
            // 8. Construire et retourner la réponse
            return TransactionReversalResponse.builder()
                .originalTransactionId(originalTransaction.getId())
                .reversalTransactionId(savedReversalTx.getId())
                .reversalReference(reversalReference)
                .reversalAmount(request.getAmount())
                .reversalFees(reversalFees)
                .currency(originalTransaction.getCurrency())
                .originalTransactionStatus(TransactionStatus.REVERSED)
                .reversalTransactionStatus(TransactionStatus.COMPLETED)
                .reason(request.getReason())
                .reversalTimestamp(now)
                .performedByUserId(user.getId())
                .performedByRole(userRoles.stream().findFirst().orElse("UNKNOWN"))
                .message("Transaction reversal completed successfully")
                .code("REVERSAL_SUCCESS")
                .build();
                
        } catch (Exception e) {
            log.error("Error during reversal of transaction {}: {}", request.getTransactionId(), e.getMessage(), e);
            
            // Enregistrer l'erreur dans l'audit log
            recordAuditLog(
                user,
                userRoles.stream().findFirst().orElse("UNKNOWN"),
                originalTransaction.getId(),
                "REVERSE",
                request.getAmount(),
                originalTransaction.getCurrency(),
                request.getReason(),
                TransactionStatus.COMPLETED.toString(),
                null,
                "FAILED",
                e.getMessage(),
                ipAddress,
                userAgent,
                requestId
            );
            
            throw e;
        }
    }
    
    /**
     * Met à jour les soldes des wallets après un reversal.
     * Transaction atomique requise par le caller.
     */
    private void updateWalletBalancesForReversal(
        Transaction originalTransaction,
        BigDecimal amount,
        BigDecimal fees) {
        
        // Sender reçoit le montant + frais
        if (originalTransaction.getSenderWallet() != null) {
            BigDecimal newBalance = originalTransaction.getSenderWallet().getBalance().add(amount).add(fees);
            walletService.updateWalletBalance(originalTransaction.getSenderWalletNumber(), newBalance);
        }
        
        // Receiver perd le montant + frais
        if (originalTransaction.getReceiverWallet() != null) {
            BigDecimal newBalance = originalTransaction.getReceiverWallet().getBalance().subtract(amount).subtract(fees);
            walletService.updateWalletBalance(originalTransaction.getReceiverWalletNumber(), newBalance);
        }
    }
    
    /**
     * Enregistre l'opération de reversal dans l'audit log.
     */
    private void recordAuditLog(
        AuthenticatedUser user,
        String userRole,
        Long transactionId,
        String actionType,
        BigDecimal amount,
        String currency,
        String reason,
        String statusBefore,
        String statusAfter,
        String result,
        String errorMessage,
        String ipAddress,
        String userAgent,
        String requestId) {
        
        TransactionAuditLog auditLog = TransactionAuditLog.builder()
            .transactionId(transactionId)
            .actorUserId(user.getId())
            .actorEmail(user.getEmail())
            .actorRole(userRole)
            .actionType(actionType)
            .amount(amount)
            .currency(currency)
            .reason(reason)
            .statusBefore(statusBefore)
            .statusAfter(statusAfter)
            .result(result)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .requestId(requestId)
            .build();
        
        auditLogRepository.save(auditLog);
        log.info("Audit log recorded for {} on transaction {}", actionType, transactionId);
    }
    
    /**
     * Publie l'événement TransactionReversedEvent sur Kafka.
     */
    private void publishReversalEvent(
        AuthenticatedUser user,
        java.util.List<String> userRoles,
        Transaction originalTransaction,
        Transaction reversalTransaction,
        TransactionReversalRequest request,
        String requestId) {
        
        try {
            TransactionReversedEvent event = TransactionReversedEvent.builder()
                .transactionId(originalTransaction.getId())
                .transactionReference(originalTransaction.getReference())
                .reversalTransactionId(reversalTransaction.getId())
                .reversalTransactionReference(reversalTransaction.getReference())
                .amount(request.getAmount())
                .fees(request.getIncludesFees() != null && request.getIncludesFees() 
                    ? originalTransaction.getFeeAmount() 
                    : BigDecimal.ZERO)
                .currency(originalTransaction.getCurrency())
                .senderUserId(originalTransaction.getSenderWallet() != null 
                    ? originalTransaction.getSenderWallet().getUserId() 
                    : null)
                .senderWalletNumber(originalTransaction.getSenderWalletNumber())
                .receiverUserId(originalTransaction.getReceiverWallet() != null 
                    ? originalTransaction.getReceiverWallet().getUserId() 
                    : null)
                .receiverWalletNumber(originalTransaction.getReceiverWalletNumber())
                .reason(request.getReason())
                .performedByUserId(user.getId())
                .performedByRole(userRoles.stream().findFirst().orElse("UNKNOWN"))
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .build();
            
            if (kafkaTemplate != null) {
                kafkaTemplate.send(REVERSAL_TOPIC, originalTransaction.getId().toString(), event);
                log.info("TransactionReversedEvent published for transaction {}", originalTransaction.getId());
            } else {
                log.warn("[KAFKA_SKIP] KafkaTemplate not available, skipping event publish for transaction: {}", originalTransaction.getId());
            }
            
        } catch (Exception e) {
            log.error("Error publishing TransactionReversedEvent: {}", e.getMessage(), e);
            // Ne pas lever d'exception: l'event est asynchrone
        }
    }
}
