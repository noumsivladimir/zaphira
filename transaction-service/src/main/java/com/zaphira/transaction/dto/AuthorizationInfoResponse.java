package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO pour les informations d'autorisation d'une transaction
 * Fournit les détails nécessaires pour vérifier le statut d'autorisation
 * sans charger les relations lazy
 * Implémente Serializable pour Kafka
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("transactionId")
    private Long transactionId;

    @JsonProperty("status")
    private TransactionStatus status;

    @JsonProperty("authorizationMethod")
    private AuthorizationMethod authorizationMethod;

    @JsonProperty("authorizationRequired")
    private Boolean authorizationRequired;

    @JsonProperty("challengeCode")
    private String challengeCode;

    @JsonProperty("pendingAt")
    private LocalDateTime pendingAt;

    @JsonProperty("authorizedAt")
    private LocalDateTime authorizedAt;
}


