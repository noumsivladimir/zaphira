package com.zaphira.transaction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité pour logger toutes les opérations sensibles (Reversal/Refund).
 * OBLIGATOIRE pour conformité réglementaire et traçabilité.
 */
@Entity
@Table(name = "transaction_audit_log", indexes = {
    @Index(name = "idx_audit_log_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_audit_log_actor_id", columnList = "actor_user_id"),
    @Index(name = "idx_audit_log_action_type", columnList = "action_type"),
    @Index(name = "idx_audit_log_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID de la transaction affectée
     */
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    
    /**
     * ID de l'utilisateur qui a effectué l'action
     */
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;
    
    /**
     * Email de l'acteur (pour traçabilité sans dépendre de la table users)
     */
    @Column(name = "actor_email")
    private String actorEmail;
    
    /**
     * Rôle de l'utilisateur qui a effectué l'action
     * Examples: ADMIN, SUPPORT, MERCHANT
     */
    @Column(name = "actor_role", nullable = false)
    private String actorRole;
    
    /**
     * Type d'action effectuée
     * Examples: REVERSE, REFUND, REFUND_PARTIAL, REFUND_FEE
     */
    @Column(name = "action_type", nullable = false)
    private String actionType;
    
    /**
     * Montant de l'opération
     */
    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;
    
    /**
     * Devises
     */
    @Column(name = "currency")
    private String currency;
    
    /**
     * Raison de l'opération
     */
    @Column(name = "reason", length = 500)
    private String reason;
    
    /**
     * Détails supplémentaires (JSON)
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    /**
     * Statut avant l'opération
     */
    @Column(name = "status_before")
    private String statusBefore;
    
    /**
     * Statut après l'opération
     */
    @Column(name = "status_after")
    private String statusAfter;
    
    /**
     * Résultat de l'opération: SUCCESS, FAILED
     */
    @Column(name = "result")
    private String result;
    
    /**
     * Message d'erreur (si opération échouée)
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    /**
     * Timestamp de l'opération (UTC)
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    /**
     * Adresse IP du client
     */
    @Column(name = "ip_address")
    private String ipAddress;
    
    /**
     * User-Agent du client
     */
    @Column(name = "user_agent")
    private String userAgent;
    
    /**
     * Device ID du client (si disponible)
     */
    @Column(name = "device_id")
    private String deviceId;
    
    /**
     * Request ID pour traçabilité distribuée
     */
    @Column(name = "request_id")
    private String requestId;

    @PrePersist
    void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
