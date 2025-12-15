package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour les événements Kafka de transactions
 * Utilisé pour publier les changements de transactions sur les topics Kafka
 * Garantit la sérialisation JSON correcte sans problèmes de lazy loading
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEventDTO {

    @JsonProperty("transactionId")
    private Long transactionId;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("senderWalletNumber")
    private String senderWalletNumber;

    @JsonProperty("receiverWalletNumber")
    private String receiverWalletNumber;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("type")
    private TransactionType type;

    @JsonProperty("status")
    private TransactionStatus status;

    @JsonProperty("previousStatus")
    private TransactionStatus previousStatus;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("eventType")
    private String eventType; // CREATED, STATUS_CHANGED, AUTHORIZED, FAILED

    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("initiatedBy")
    private String initiatedBy;

    @JsonProperty("changedBy")
    private String changedBy;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("feeAmount")
    private BigDecimal feeAmount;

    @JsonProperty("feeCurrency")
    private String feeCurrency;

    @JsonProperty("riskScore")
    private Integer riskScore;
}
