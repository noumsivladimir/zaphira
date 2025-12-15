package com.zaphira.service_user.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité pour tracker les transactions Kafka traitées par user-service.
 * Utilisée pour garantir l'idempotence: une transaction ne sera traitée qu'une seule fois.
 * 
 * Clé d'idempotence: userId + transactionId
 */
@Entity
@Table(name = "transaction_processed_log", indexes = {
    @Index(name = "idx_user_transaction", columnList = "user_id,transaction_id", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_processed_at", columnList = "processed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionProcessedLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID de l'utilisateur concerné
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * ID de la transaction Kafka
     */
    @Column(nullable = false)
    private Long transactionId;
    
    /**
     * Référence de la transaction
     */
    @Column(length = 100)
    private String transactionReference;
    
    /**
     * Montant de la transaction traitée
     */
    @Column(precision = 19, scale = 4)
    private java.math.BigDecimal amount;
    
    /**
     * Statut du traitement: SUCCESS, FAILED, PENDING
     */
    @Column(nullable = false, length = 20)
    private String processingStatus;
    
    /**
     * Message d'erreur si le traitement a échoué
     */
    @Column(length = 500)
    private String errorMessage;
    
    /**
     * Timestamp du traitement
     */
    @Column(nullable = false)
    private LocalDateTime processedAt;
    
    /**
     * Timestamp de création du log
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.processedAt == null) {
            this.processedAt = LocalDateTime.now();
        }
    }
}
