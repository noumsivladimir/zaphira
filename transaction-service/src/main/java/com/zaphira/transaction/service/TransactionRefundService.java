package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.TransactionRefundRequest;
import com.zaphira.transaction.dto.TransactionRefundRequest.RefundType;
import com.zaphira.transaction.dto.TransactionRefundResponse;
import com.zaphira.transaction.kafka.event.TransactionRefundedEvent;
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
 * Service pour gérer les refunds (remboursements) de transactions.
 * 
 * Responsabilités:
 * 1. Vérifier les autorisations via TransactionAuthorizationService
 * 2. Valider les montants de remboursement (full/partial)
 * 3. Créer une transaction de remboursement compensatoire
 * 4. Mettre à jour les soldes des wallets de manière atomique
 * 5. Enregistrer l'opération dans l'audit log
 * 6. Publier l'événement TransactionRefundedEvent
 * 
 * Supporte:
 * - Remboursements complets
 * - Remboursements partiels
 * - Remboursement des frais séparément
 * 
 * IMPORTANT: Opération atomique et sécurisée
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionRefundService {
    
    private final TransactionRepository transactionRepository;
    private final TransactionAuditLogRepository auditLogRepository;
    private final TransactionAuthorizationService authorizationService;
    @Nullable
    private final KafkaTemplate<String, TransactionRefundedEvent> kafkaTemplate;
    private final WalletService walletService; // Service pour mettre à jour les wallets
    
    private static final String REFUND_TOPIC = "transaction-refunded";
    
    /**
     * Effectue un remboursement de transaction.
     * 
     * Supporte les remboursements complets et partiels.
     * Valide que le montant de remboursement ne dépasse pas le montant original.
     * 
     * @param user L'utilisateur authentifié qui effectue le refund
     * @param userRoles Les rôles de l'utilisateur
     * @param userPermissions Les permissions de l'utilisateur
     * @param request La requête de refund
     * @param ipAddress L'adresse IP du client
     * @param userAgent L'user-agent du client
     * @param requestId L'ID de la requête pour traçabilité distribuée
     * @return La réponse du refund
     * @throws ResourceNotFoundException Si la transaction n'existe pas
     * @throws AccessDeniedException Si l'utilisateur n'est pas autorisé
     * @throws IllegalArgumentException Si les montants sont invalides
     */
    @Transactional
    public TransactionRefundResponse refund(
        AuthenticatedUser user,
        java.util.List<String> userRoles,
        java.util.List<String> userPermissions,
        TransactionRefundRequest request,
        String ipAddress,
        String userAgent,
        String requestId) {
        
        log.info("Starting refund for transaction {} by user {}, amount: {}", 
            request.getTransactionId(), user.getId(), request.getRefundAmount());
        
        // 1. Récupérer la transaction
        Transaction originalTransaction = transactionRepository.findById(request.getTransactionId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction not found: " + request.getTransactionId()
            ));
        
        // 2. Déterminer le type de remboursement
        boolean isPartialRefund = request.getRefundAmount().compareTo(originalTransaction.getAmount()) < 0;
        if (request.getRefundType() == null) {
            request.setRefundType(isPartialRefund ? RefundType.PARTIAL : RefundType.FULL);
        }
        
        // 3. Autoriser le refund (vérifications multi-niveaux)
        authorizationService.authorizeRefund(
            user,
            userRoles,
            userPermissions,
            originalTransaction,
            request.getRefundAmount(),
            isPartialRefund
        );
        
        LocalDateTime now = LocalDateTime.now();
        String refundReference = "RFD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        
        // 4. Calculer les montants de remboursement
        BigDecimal refundFees = BigDecimal.ZERO;
        if (request.getIncludeFees() != null && request.getIncludeFees()) {
            refundFees = request.getRefundFeeAmount() != null 
                ? request.getRefundFeeAmount() 
                : originalTransaction.getFeeAmount();
        }
        BigDecimal totalRefundAmount = request.getRefundAmount().add(refundFees);
        
        try {
            // 5. Créer la transaction de remboursement compensatoire
            Transaction refundTransaction = Transaction.builder()
                .reference(refundReference)
                // Inverser les wallets: sender devient receiver (remboursement au client)
                .senderWalletNumber(originalTransaction.getReceiverWalletNumber())
                .receiverWalletNumber(originalTransaction.getSenderWalletNumber())
                .senderWallet(originalTransaction.getReceiverWallet())
                .receiverWallet(originalTransaction.getSenderWallet())
                .amount(request.getRefundAmount())
                .currency(originalTransaction.getCurrency())
                .feeAmount(refundFees)
                .feeCurrency(originalTransaction.getFeeCurrency())
                .feeType(originalTransaction.getFeeType())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.COMPLETED)
                .channel(originalTransaction.getChannel())
                .description("Refund of transaction " + originalTransaction.getReference() + 
                            " (" + request.getRefundType() + "). Reason: " + request.getReason())
                .initiatedBy(user.getEmail())
                .lastUpdatedBy(user.getEmail())
                .metadata("{\"original_transaction_id\": \"" + originalTransaction.getId() + 
                         "\", \"refund_type\": \"" + request.getRefundType() + 
                         "\", \"reason\": \"" + request.getReason() + "\"}")
                .build();
            
            // Sauvegarder la transaction de refund
            Transaction savedRefundTx = transactionRepository.save(refundTransaction);
            
            // 6. Mettre à jour le statut de la transaction originale
            if (isPartialRefund) {
                // Remboursement partiel: garder le statut mais marquer comme "partiellement remboursée"
                originalTransaction.setMetadata("{\"partial_refund\": \"true\", \"refunded_amount\": \"" + 
                    request.getRefundAmount() + "\"}");
            } else {
                // Remboursement complet
                originalTransaction.setStatus(TransactionStatus.REFUNDED);
            }
            originalTransaction.setLastUpdatedBy(user.getEmail());
            transactionRepository.save(originalTransaction);
            
            // 7. Mettre à jour les soldes des wallets de manière atomique
            updateWalletBalancesForRefund(
                originalTransaction,
                request.getRefundAmount(),
                refundFees
            );
            
            // 8. Calculer le montant restant remboursable (pour refunds partiels)
            BigDecimal remainingRefundable = originalTransaction.getAmount().subtract(request.getRefundAmount());
            
            // 9. Enregistrer dans l'audit log
            recordAuditLog(
                user,
                userRoles.stream().findFirst().orElse("UNKNOWN"),
                originalTransaction.getId(),
                request.getRefundType() == RefundType.FULL ? "REFUND" : "REFUND_PARTIAL",
                request.getRefundAmount(),
                originalTransaction.getCurrency(),
                request.getReason(),
                TransactionStatus.COMPLETED.toString(),
                isPartialRefund ? TransactionStatus.COMPLETED.toString() : TransactionStatus.REFUNDED.toString(),
                "SUCCESS",
                null,
                ipAddress,
                userAgent,
                requestId
            );
            
            // 10. Publier l'événement Kafka
            publishRefundEvent(user, userRoles, originalTransaction, savedRefundTx, request, 
                            totalRefundAmount, remainingRefundable, requestId);
            
            log.info("Refund completed successfully for transaction {}", request.getTransactionId());
            
            // 11. Construire et retourner la réponse
            return TransactionRefundResponse.builder()
                .originalTransactionId(originalTransaction.getId())
                .refundTransactionId(savedRefundTx.getId())
                .refundReference(refundReference)
                .refundAmount(request.getRefundAmount())
                .refundFees(refundFees)
                .totalRefundAmount(totalRefundAmount)
                .remainingRefundable(isPartialRefund ? remainingRefundable : BigDecimal.ZERO)
                .currency(originalTransaction.getCurrency())
                .refundType(request.getRefundType().toString())
                .originalTransactionStatus(isPartialRefund ? TransactionStatus.COMPLETED : TransactionStatus.REFUNDED)
                .refundTransactionStatus(TransactionStatus.COMPLETED)
                .reason(request.getReason())
                .refundTimestamp(now)
                .performedByUserId(user.getId())
                .performedByRole(userRoles.stream().findFirst().orElse("UNKNOWN"))
                .message("Transaction refund (" + request.getRefundType() + ") completed successfully")
                .code("REFUND_SUCCESS")
                .build();
                
        } catch (Exception e) {
            log.error("Error during refund of transaction {}: {}", request.getTransactionId(), e.getMessage(), e);
            
            // Enregistrer l'erreur dans l'audit log
            recordAuditLog(
                user,
                userRoles.stream().findFirst().orElse("UNKNOWN"),
                originalTransaction.getId(),
                "REFUND",
                request.getRefundAmount(),
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
     * Met à jour les soldes des wallets après un refund.
     * Transaction atomique requise par le caller.
     */
    private void updateWalletBalancesForRefund(
        Transaction originalTransaction,
        BigDecimal refundAmount,
        BigDecimal refundFees) {
        
        // Receiver (client) reçoit le montant + frais
        if (originalTransaction.getReceiverWallet() != null) {
            BigDecimal newBalance = originalTransaction.getReceiverWallet().getBalance().add(refundAmount).add(refundFees);
            walletService.updateWalletBalance(originalTransaction.getReceiverWalletNumber(), newBalance);
        }
        
        // Sender (merchant) perd le montant + frais
        if (originalTransaction.getSenderWallet() != null) {
            BigDecimal newBalance = originalTransaction.getSenderWallet().getBalance().subtract(refundAmount).subtract(refundFees);
            walletService.updateWalletBalance(originalTransaction.getSenderWalletNumber(), newBalance);
        }
    }
    
    /**
     * Enregistre l'opération de refund dans l'audit log.
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
     * Publie l'événement TransactionRefundedEvent sur Kafka.
     */
    private void publishRefundEvent(
        AuthenticatedUser user,
        java.util.List<String> userRoles,
        Transaction originalTransaction,
        Transaction refundTransaction,
        TransactionRefundRequest request,
        BigDecimal totalRefundAmount,
        BigDecimal remainingRefundable,
        String requestId) {
        
        try {
            TransactionRefundedEvent event = TransactionRefundedEvent.builder()
                .transactionId(originalTransaction.getId())
                .transactionReference(originalTransaction.getReference())
                .refundTransactionId(refundTransaction.getId())
                .refundTransactionReference(refundTransaction.getReference())
                .refundAmount(request.getRefundAmount())
                .refundFees(request.getIncludeFees() != null && request.getIncludeFees() 
                    ? (request.getRefundFeeAmount() != null ? request.getRefundFeeAmount() : originalTransaction.getFeeAmount())
                    : BigDecimal.ZERO)
                .totalRefundAmount(totalRefundAmount)
                .currency(originalTransaction.getCurrency())
                .refundType(request.getRefundType() != null ? request.getRefundType() :
                           (request.getRefundAmount().compareTo(originalTransaction.getAmount()) < 0 ? 
                            RefundType.PARTIAL : RefundType.FULL))
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
                kafkaTemplate.send(REFUND_TOPIC, originalTransaction.getId().toString(), event);
                log.info("TransactionRefundedEvent published for transaction {}", originalTransaction.getId());
            } else {
                log.warn("[KAFKA_SKIP] KafkaTemplate unavailable, skipping TransactionRefundedEvent publication for transaction {}", originalTransaction.getId());
            }
            
        } catch (Exception e) {
            log.error("Error publishing TransactionRefundedEvent: {}", e.getMessage(), e);
            // Ne pas lever d'exception: l'event est asynchrone
        }
    }
}
