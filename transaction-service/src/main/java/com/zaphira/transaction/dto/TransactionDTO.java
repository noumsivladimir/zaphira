package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.ComplianceStatus;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour les transactions
 * Contient toutes les informations nécessaires sans charger les relations lazy
 * Évite les problèmes LazyInitializationException et ByteBuddyInterceptor
 * Implémente Serializable pour Kafka
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Transaction identifiers
    @JsonProperty("id")
    private Long id;

    @JsonProperty("reference")
    private String reference;

    // Wallet information (simplified to avoid lazy loading)
    @JsonProperty("senderWalletNumber")
    private String senderWalletNumber;

    @JsonProperty("receiverWalletNumber")
    private String receiverWalletNumber;

    @JsonProperty("senderWallet")
    private WalletSummaryDTO senderWallet;

    @JsonProperty("receiverWallet")
    private WalletSummaryDTO receiverWallet;

    // Amount information
    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    // Fee information
    @JsonProperty("feeAmount")
    private BigDecimal feeAmount;

    @JsonProperty("feeCurrency")
    private String feeCurrency;

    @JsonProperty("feeType")
    private String feeType;

    // Transaction details
    @JsonProperty("type")
    private TransactionType type;

    @JsonProperty("status")
    private TransactionStatus status;

    @JsonProperty("channel")
    private TransactionChannel channel;

    @JsonProperty("description")
    private String description;

    // Authorization information
    @JsonProperty("authorizationMethod")
    private AuthorizationMethod authorizationMethod;

    @JsonProperty("authorizationRequired")
    private Boolean authorizationRequired;

    // Retry and scheduling information
    @JsonProperty("retryCount")
    private Integer retryCount;

    @JsonProperty("maxRetry")
    private Integer maxRetry;

    @JsonProperty("scheduled")
    private Boolean scheduled;

    @JsonProperty("scheduledFor")
    private LocalDateTime scheduledFor;

    // Metadata and audit information
    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("initiatedBy")
    private String initiatedBy;

    @JsonProperty("lastUpdatedBy")
    private String lastUpdatedBy;

    // Risk and compliance
    @JsonProperty("riskScore")
    private Integer riskScore;

    @JsonProperty("complianceStatus")
    private ComplianceStatus complianceStatus;

    // Timestamp information
    @JsonProperty("initiatedAt")
    private LocalDateTime initiatedAt;

    @JsonProperty("pendingAt")
    private LocalDateTime pendingAt;

    @JsonProperty("authorizedAt")
    private LocalDateTime authorizedAt;

    @JsonProperty("processingAt")
    private LocalDateTime processingAt;

    @JsonProperty("completedAt")
    private LocalDateTime completedAt;

    @JsonProperty("failedAt")
    private LocalDateTime failedAt;

    @JsonProperty("cancelledAt")
    private LocalDateTime cancelledAt;

    @JsonProperty("reversedAt")
    private LocalDateTime reversedAt;

    @JsonProperty("refundedAt")
    private LocalDateTime refundedAt;

    @JsonProperty("expiredAt")
    private LocalDateTime expiredAt;

    @JsonProperty("onHoldAt")
    private LocalDateTime onHoldAt;

    @JsonProperty("underReviewAt")
    private LocalDateTime underReviewAt;
}
