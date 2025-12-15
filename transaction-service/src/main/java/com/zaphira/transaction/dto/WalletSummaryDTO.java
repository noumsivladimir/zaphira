package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO simplifiée pour résumer les informations du portefeuille
 * Utilisée dans TransactionDTO pour éviter les problèmes de LazyInitializationException
 * et les intercepteurs ByteBuddy
 * Implémente Serializable pour Kafka
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("walletNumber")
    private String walletNumber;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("currency")
    private String currency;
}
