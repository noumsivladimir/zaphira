package com.zaphira.transaction.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Événement publié quand une transaction est reversée (annulée).
 * Utilisé pour mettre à jour les wallets, notifier les utilisateurs, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReversedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * ID de la transaction originale reversée
     */
    private Long transactionId;
    
    /**
     * Référence de la transaction originale
     */
    private String transactionReference;
    
    /**
     * ID de la transaction compensatoire
     */
    private Long reversalTransactionId;
    
    /**
     * Référence de la transaction compensatoire
     */
    private String reversalTransactionReference;
    
    /**
     * Montant reversé
     */
    private BigDecimal amount;
    
    /**
     * Frais reversés (si applicables)
     */
    private BigDecimal fees;
    
    /**
     * Devise
     */
    private String currency;
    
    /**
     * ID du sender
     */
    private Long senderUserId;
    
    /**
     * Numéro de wallet du sender
     */
    private String senderWalletNumber;
    
    /**
     * ID du receiver
     */
    private Long receiverUserId;
    
    /**
     * Numéro de wallet du receiver
     */
    private String receiverWalletNumber;
    
    /**
     * Raison du reversal
     */
    private String reason;
    
    /**
     * ID de l'utilisateur qui a effectué le reversal
     */
    private Long performedByUserId;
    
    /**
     * Rôle de l'utilisateur qui a effectué le reversal
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
