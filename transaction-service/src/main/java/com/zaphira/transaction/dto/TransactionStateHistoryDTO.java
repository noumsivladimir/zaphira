package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaphira.transaction.model.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO pour l'historique des états des transactions
 * Contient uniquement les champs essentiels sans aucune relation lazy
 * Exclut les propriétés Hibernate internes pour éviter ByteBuddyInterceptor
 * Implémente Serializable pour Kafka
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionStateHistoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // State history identifiers
    @JsonProperty("id")
    private Long id;

    @JsonProperty("transactionId")
    private Long transactionId;

    // Status information
    @JsonProperty("status")
    private TransactionStatus status;

    // Audit information
    @JsonProperty("changedBy")
    private String changedBy;

    @JsonProperty("reason")
    private String reason;

    // Timestamp
    @JsonProperty("changedAt")
    private LocalDateTime changedAt;
}
