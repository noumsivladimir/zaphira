package com.zaphira.transaction.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Événement Kafka consommé pour mettre à jour le statut de validation
 * Publié par les services de validation (compliance-service, risk-service, etc.)
 *
 * Clé: transactionId
 * Topic: transaction.validation.result
 * Garantie: Au moins une fois (idempotence via correlationId + DB)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionValidationResult {

    /**
     * Identifiant unique de la requête (même que dans TransactionValidationRequest)
     * Permet d'identifier la réponse et d'éviter les doubles traitements
     */
    private String correlationId;

    /**
     * Identifiant de la transaction validée
     * Utilisé comme clé Kafka
     */
    private Long transactionId;

    /**
     * Statut de validation
     * APPROVED: Transaction validée et autorisée
     * REJECTED: Transaction rejetée (fraude, limite, etc.)
     * EXPIRED: La validation a expiré (timeout)
     */
    @JsonProperty("validationStatus")
    private ValidationStatus validationStatus;

    /**
     * Raison détaillée du résultat (pour logs/debug)
     * Ex: "Compliance check passed", "High risk score detected"
     */
    private String reason;

    /**
     * ID du service validateur qui a traité la demande
     * Ex: "compliance-service-pod-1", "risk-engine-2"
     */
    private String validatorId;

    /**
     * Timestamp du traitement par le validateur
     */
    private LocalDateTime validatedAt;

    /**
     * Score de risque final calculé par le service de validation
     * Intervalle: 0-100
     * Null si non applicable
     */
    private Integer riskScoreFinal;

    /**
     * Statut de conformité après vérification
     * CLEAR: Pas d'alerte
     * FLAGGED: Alerte mineure, transaction acceptée mais monitoring
     * BLOCKED: Transaction bloquée au niveau compliance
     */
    @JsonProperty("complianceStatus")
    private ComplianceStatus complianceStatus;

    /**
     * Métadonnées additionnelles pour audit et debugging
     * Ex: {
     *   "verificationMethod": "auto_check",
     *   "amlStatus": "PASS",
     *   "kycStatus": "VERIFIED",
     *   "velocityCheck": "OK"
     * }
     */
    private Map<String, Object> metadata;

    /**
     * Version du schéma pour évolution future
     */
    @Builder.Default
    private Integer schemaVersion = 1;

    /**
     * Énumération des statuts de validation possibles
     */
    public enum ValidationStatus {
        APPROVED,   // Validation réussie
        REJECTED,   // Validation échouée
        EXPIRED     // Délai d'attente dépassé
    }

    /**
     * Énumération des statuts de conformité
     */
    public enum ComplianceStatus {
        CLEAR,      // Conforme
        FLAGGED,    // Signalé (monitoring)
        BLOCKED     // Bloqué
    }
}
