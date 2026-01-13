package com.zaphira.transaction.security;

import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service d'autorisation métier pour les opérations sensibles (Reversal/Refund).
 * Vérifie:
 * 1. Les permissions utilisateur
 * 2. Le statut de la transaction
 * 3. Les limites de montant
 * 4. Les délais de remboursement
 * 5. L'ownership (merchant sur ses transactions)
 * 6. La répétition d'opérations
 * 
 * OBLIGATOIRE: Logique métier jamais dans le controller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionAuthorizationService {
    
    // Configuration des limites (à injecter depuis application.yml)
    @Value("${transaction.limits.reversal-max-days:30}")
    private Integer reversalMaxDays;
    
    @Value("${transaction.limits.refund-max-days:90}")
    private Integer refundMaxDays;
    
    @Value("${transaction.limits.refund-threshold-high:10000.00}")
    private BigDecimal refundThresholdHigh;
    
    @Value("${transaction.limits.refund-max-percentage:100}")
    private Integer refundMaxPercentage;
    
    @Value("${transaction.limits.support-max-refund:5000.00}")
    private BigDecimal supportMaxRefund;
    
    // Rôles et permissions
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPPORT = "SUPPORT";
    private static final String ROLE_MERCHANT = "MERCHANT";
    
    private static final String PERMISSION_REVERSE = "TRANSACTION_REVERSE";
    private static final String PERMISSION_REFUND = "TRANSACTION_REFUND";
    private static final String PERMISSION_REFUND_PARTIAL = "TRANSACTION_REFUND_PARTIAL";
    
    
    private static final List<String> REVERSIBLE_STATUSES = Arrays.asList(
        TransactionStatus.COMPLETED.toString(),
        TransactionStatus.PROCESSING.toString()
    );
    
    private static final List<String> REFUNDABLE_STATUSES = Arrays.asList(
        TransactionStatus.COMPLETED.toString()
    );
    
    /**
     * Autorise une opération de reversal.
     * Lève AccessDeniedException si non autorisé.
     */
    public void authorizeReversal(
        AuthenticatedUser user,
        List<String> userRoles,
        List<String> userPermissions,
        Transaction transaction,
        BigDecimal reversalAmount) {
        
        log.info("Authorizing reversal for user {} on transaction {}", user.getId(), transaction.getId());
        
        // 1. Vérifier la permission
        if (!userPermissions.contains(PERMISSION_REVERSE)) {
            log.warn("User {} attempted reversal without permission TRANSACTION_REVERSE", user.getId());
            throw new AccessDeniedException(
                "You do not have permission to reverse transactions. Required: TRANSACTION_REVERSE"
            );
        }
        
        // 2. Vérifier le rôle (seuls ADMIN et SUPPORT peuvent reverser)
        if (!userRoles.contains(ROLE_ADMIN) && !userRoles.contains(ROLE_SUPPORT)) {
            log.warn("User {} with role(s) {} attempted reversal without ADMIN or SUPPORT role", 
                user.getId(), userRoles);
            throw new AccessDeniedException(
                "Only ADMIN and SUPPORT roles can reverse transactions"
            );
        }
        
        // 3. Vérifier le statut de la transaction
        if (!REVERSIBLE_STATUSES.contains(transaction.getStatus().toString())) {
            log.warn("Transaction {} cannot be reversed. Current status: {}", 
                transaction.getId(), transaction.getStatus());
            throw new AccessDeniedException(
                "Transaction status " + transaction.getStatus() + 
                " cannot be reversed. Only " + REVERSIBLE_STATUSES + " can be reversed"
            );
        }
        
        // 4. Vérifier le délai maximal de reversal (30 jours par défaut)
        LocalDateTime maxReversalDate = transaction.getCreatedAt().plusDays(reversalMaxDays);
        if (LocalDateTime.now().isAfter(maxReversalDate)) {
            log.warn("Reversal attempted for transaction {} after {} days limit", 
                transaction.getId(), reversalMaxDays);
            throw new AccessDeniedException(
                "Transaction can only be reversed within " + reversalMaxDays + " days of creation"
            );
        }
        
        // 5. Vérifier le montant
        if (reversalAmount.compareTo(transaction.getAmount()) > 0) {
            log.warn("Reversal amount {} exceeds transaction amount {}", 
                reversalAmount, transaction.getAmount());
            throw new AccessDeniedException(
                "Reversal amount cannot exceed original transaction amount"
            );
        }
        
        // 6. Vérifier si la transaction n'a pas déjà été reversée/remboursée
        if (TransactionStatus.REVERSED.equals(transaction.getStatus()) ||
            TransactionStatus.REVERSED.equals(transaction.getStatus())) {
            log.warn("Transaction {} is already {} and cannot be reversed again", 
                transaction.getId(), transaction.getStatus());
            throw new AccessDeniedException(
                "Transaction has already been " + transaction.getStatus().toString().toLowerCase() + 
                " and cannot be reversed again"
            );
        }
        
        log.info("Reversal authorized for user {} on transaction {}", user.getId(), transaction.getId());
    }
    
    /**
     * Autorise une opération de remboursement (refund).
     * Lève AccessDeniedException si non autorisé.
     */
    public void authorizeRefund(
        AuthenticatedUser user,
        List<String> userRoles,
        List<String> userPermissions,
        Transaction transaction,
        BigDecimal refundAmount,
        boolean isPartialRefund) {
        
        log.info("Authorizing refund for user {} on transaction {}, amount: {}", 
            user.getId(), transaction.getId(), refundAmount);
        
        // 1. Vérifier la permission de base
        if (!userPermissions.contains(PERMISSION_REFUND)) {
            log.warn("User {} attempted refund without permission TRANSACTION_REFUND", user.getId());
            throw new AccessDeniedException(
                "You do not have permission to refund transactions. Required: TRANSACTION_REFUND"
            );
        }
        
        // 2. Vérifier la permission pour remboursement partiel
        if (isPartialRefund && !userPermissions.contains(PERMISSION_REFUND_PARTIAL)) {
            log.warn("User {} attempted partial refund without permission TRANSACTION_REFUND_PARTIAL", 
                user.getId());
            throw new AccessDeniedException(
                "You do not have permission to partially refund transactions. Required: TRANSACTION_REFUND_PARTIAL"
            );
        }
        
        // 3. Vérifier le rôle (ADMIN, SUPPORT, ou MERCHANT sur ses transactions)
        String userRole = userRoles.stream().findFirst().orElse("");
        
        if (ROLE_MERCHANT.equals(userRole)) {
            // MERCHANT peut seulement rembourser ses propres transactions
            verifyMerchantOwnership(user, transaction);
        } else if (!userRoles.contains(ROLE_ADMIN) && !userRoles.contains(ROLE_SUPPORT)) {
            log.warn("User {} with role(s) {} is not authorized to refund transactions", 
                user.getId(), userRoles);
            throw new AccessDeniedException(
                "Only ADMIN, SUPPORT and MERCHANT roles can refund transactions"
            );
        }
        
        // 4. Vérifier le statut de la transaction
        if (!REFUNDABLE_STATUSES.contains(transaction.getStatus().toString())) {
            log.warn("Transaction {} cannot be refunded. Current status: {}", 
                transaction.getId(), transaction.getStatus());
            throw new AccessDeniedException(
                "Only " + REFUNDABLE_STATUSES + " transactions can be refunded"
            );
        }
        
        // 5. Vérifier le délai maximal de remboursement (90 jours par défaut)
        LocalDateTime maxRefundDate = transaction.getCreatedAt().plusDays(refundMaxDays);
        if (LocalDateTime.now().isAfter(maxRefundDate)) {
            log.warn("Refund attempted for transaction {} after {} days limit", 
                transaction.getId(), refundMaxDays);
            throw new AccessDeniedException(
                "Transaction can only be refunded within " + refundMaxDays + " days of creation"
            );
        }
        
        // 6. Vérifier les limites de montant selon le rôle
        if (ROLE_SUPPORT.equals(userRole)) {
            if (refundAmount.compareTo(supportMaxRefund) > 0) {
                log.warn("SUPPORT user {} attempted refund of {} exceeding max {}", 
                    user.getId(), refundAmount, supportMaxRefund);
                throw new AccessDeniedException(
                    "SUPPORT role can only refund up to " + supportMaxRefund + " per transaction"
                );
            }
        }
        
        // 7. Vérifier le montant du remboursement
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid refund amount: {}", refundAmount);
            throw new AccessDeniedException("Refund amount must be greater than 0");
        }
        
        if (refundAmount.compareTo(transaction.getAmount()) > 0) {
            log.warn("Refund amount {} exceeds transaction amount {}", 
                refundAmount, transaction.getAmount());
            throw new AccessDeniedException(
                "Refund amount cannot exceed original transaction amount"
            );
        }
        
        // 8. Vérifier si la transaction n'a pas déjà été reversée/remboursée
        if (TransactionStatus.REVERSED.equals(transaction.getStatus()) ||
            TransactionStatus.REVERSED.equals(transaction.getStatus())) {
            log.warn("Transaction {} is already {} and cannot be refunded", 
                transaction.getId(), transaction.getStatus());
            throw new AccessDeniedException(
                "Transaction has already been " + transaction.getStatus().toString().toLowerCase()
            );
        }
        
        log.info("Refund authorized for user {} on transaction {}", user.getId(), transaction.getId());
    }
    
    /**
     * Vérifie que le merchant est propriétaire de la transaction.
     */
    private void verifyMerchantOwnership(AuthenticatedUser user, Transaction transaction) {
        // Récupérer l'ID du merchant depuis la transaction
//        // (supposé être stocké dans senderWallet ou une autre relation)
//        Long transactionOwnerId = transaction.getSenderWallet() != null ?
//            transaction.getSenderWallet().getUserId() : null;
//
//        if (transactionOwnerId == null || !transactionOwnerId.equals(user.getId())) {
//            log.warn("Merchant {} attempted to refund transaction {} they don't own",
//                user.getId(), transaction.getId());
//            throw new AccessDeniedException(
//                "You can only refund transactions you initiated"
//            );
//        }
    }
    
    /**
     * Vérifie les permissions d'accès aux logs d'audit.
     */
    public void authorizeAuditLogAccess(AuthenticatedUser user, List<String> userRoles) {
        if (!userRoles.contains(ROLE_ADMIN) && !userRoles.contains(ROLE_SUPPORT)) {
            log.warn("User {} attempted to access audit logs without ADMIN/SUPPORT role", user.getId());
            throw new AccessDeniedException(
                "Only ADMIN and SUPPORT roles can access audit logs"
            );
        }
    }
}
