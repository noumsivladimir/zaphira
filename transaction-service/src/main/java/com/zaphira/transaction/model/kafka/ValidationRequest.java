package com.zaphira.transaction.model.kafka;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité JPA pour tracer les demandes de validation traitées
 * Utilisée pour garantir l'idempotence lors de la réception de messages Kafka
 * 
 * Pattern: Pour chaque correlationId, vérifier qu'on n'a pas déjà traité ce message
 * 
 * Table: validation_requests
 * Clé primaire: correlationId (unique)
 * Index: transactionId (pour recherche rapide)
 */
@Entity
@Table(
    name = "validation_requests",
    indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_processed_at", columnList = "processed_at"),
        @Index(name = "idx_correlation_id", columnList = "correlation_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant unique de la requête (UUID)
     * Utilisé comme clé pour l'idempotence
     */
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String correlationId;

    /**
     * Référence à la transaction associée
     * Permet de retrouver rapidement les validations d'une transaction
     */
    @Column(nullable = false, name = "transaction_id")
    private Long transactionId;

    /**
     * Timestamp de création de la requête côté transaction-service
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /**
     * Timestamp de traitement du résultat (quand le message Kafka a été reçu et traité)
     * Null si pas encore traité
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Statut de la requête
     * PENDING: En attente de réponse
     * PROCESSED: Résultat reçu et transaction mise à jour
     * EXPIRED: Timeout dépassé (pas de réponse)
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * Timestamp d'expiration (au-delà duquel la requête est considérée comme expirée)
     * Utilisé pour nettoyer les anciennes requêtes
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
