package com.zaphira.transaction.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Événement Kafka publié lors de la création d'une transaction
 * Demande de validation auprès des services externes (compliance, risk, etc.)
 *
 * Clé: transactionId
 * Topic: transaction.validation.request
 * Garantie: Au moins une fois (idempotence via correlationId)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionValidationRequest {

    /**
     * Identifiant unique de la requête pour idempotence
     * Format: UUID généré côté transaction-service
     */
    private String correlationId;

    /**
     * Identifiant de la transaction à valider
     * Utilisé comme clé Kafka
     */
    private Long transactionId;

    /**
     * Numéro de référence unique de la transaction
     */
    private String reference;

    /**
     * Numéro du portefeuille expéditeur (pas d'ID sensible)
     */
    private String senderWalletNumber;

    /**
     * Numéro du portefeuille destinataire (pas d'ID sensible)
     */
    private String receiverWalletNumber;

    /**
     * Montant de la transaction
     */
    private BigDecimal amount;

    /**
     * Devise (ex: XOF, USD)
     */
    private String currency;

    /**
     * Montant des frais associés
     */
    private BigDecimal feeAmount;

    /**
     * Type de transaction (P2P_TRANSFER, WITHDRAWAL, DEPOSIT, etc.)
     */
    private String type;

    /**
     * Canal de transaction (MOBILE, WEB, USSD, etc.)
     */
    private String channel;

    /**
     * Score de risque initial calculé côté transaction-service
     * Intervalle: 0-100
     */
    private Integer initialRiskScore;

    /**
     * Timestamp de création de la transaction
     */
    private LocalDateTime initiatedAt;

    /**
     * Adresse IP du client (si disponible)
     */
    private String clientIpAddress;

    /**
     * Device ID ou User-Agent (anonymisé)
     */
    private String deviceIdentifier;

    /**
     * Version du schéma pour évolution future
     */
    @Builder.Default
    private Integer schemaVersion = 1;
}
