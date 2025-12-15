package com.zaphira.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Événement Kafka publié quand une transaction passe au statut COMPLETED.
 * Utilisé pour déclencher des actions asynchrones comme la mise à jour du solde utilisateur.
 *
 * Topic Kafka: transaction-completed
 * Producer: transaction-service
 * Consumers: user-service, notification-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent {
    
    /**
     * ID unique de la transaction
     */
    private Long transactionId;
    
    /**
     * Référence de la transaction (UUID)
     */
    private String reference;
    
    /**
     * ID de l'utilisateur initiateur (sender)
     */
    private Long initiatorUserId;
    
    /**
     * Numéro de portefeuille de l'expéditeur
     */
    private String senderWalletNumber;
    
    /**
     * Numéro de portefeuille du destinataire
     */
    private String receiverWalletNumber;
    
    /**
     * Montant de la transaction
     */
    private BigDecimal amount;
    
    /**
     * Devise de la transaction
     */
    private String currency;
    
    /**
     * Montant des frais appliqués
     */
    private BigDecimal feeAmount;
    
    /**
     * Statut final de la transaction (COMPLETED, FAILED, etc.)
     */
    private String status;
    
    /**
     * Timestamp de complétion
     */
    private LocalDateTime completedAt;
    
    /**
     * Timestamp de création de l'événement
     */
    private LocalDateTime eventTimestamp;
}
