package com.zaphira.transaction.kafka.event;

import com.zaphira.transaction.dto.requests.TransactionRefundRequest.RefundType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Événement publié quand une transaction est remboursée.
 * Supporte les remboursements complets et partiels.
 * Utilisé pour mettre à jour les wallets, notifier les utilisateurs, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRefundedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * ID de la transaction originale remboursée
     */
    private Long transactionId;
    
    /**
     * Référence de la transaction originale
     */
    private String transactionReference;
    
    /**
     * ID de la transaction de remboursement
     */
    private Long refundTransactionId;
    
    /**
     * Référence de la transaction de remboursement
     */
    private String refundTransactionReference;
    
    /**
     * Montant remboursé
     */
    private BigDecimal refundAmount;
    
    /**
     * Frais remboursés (si applicables)
     */
    private BigDecimal refundFees;
    
    /**
     * Montant total remboursé
     */
    private BigDecimal totalRefundAmount;
    
    /**
     * Devise
     */
    private String currency;
    
    /**
     * Type de remboursement: FULL ou PARTIAL
     */
    private RefundType refundType;
    
    /**
     * ID du sender/merchant
     */
    private Long senderUserId;
    
    /**
     * Numéro de wallet du sender
     */
    private String senderWalletNumber;
    
    /**
     * ID du receiver/client
     */
    private Long receiverUserId;
    
    /**
     * Numéro de wallet du receiver
     */
    private String receiverWalletNumber;
    
    /**
     * Raison du remboursement
     */
    private String reason;
    
    /**
     * ID de l'utilisateur qui a effectué le remboursement
     */
    private Long performedByUserId;
    
    /**
     * Rôle de l'utilisateur qui a effectué le remboursement
     */
    private String performedByRole;
    
    /**
     * Timestamp de l'événement (UTC)
     */
    private LocalDateTime timestamp;
    
    /**
     * Request ID pour traçabilité distribuée
     */
    private String requestId;
}
