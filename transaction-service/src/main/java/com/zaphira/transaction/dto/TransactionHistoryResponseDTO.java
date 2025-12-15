package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse pour l'historique des états de transaction
 * Contient la liste des TransactionStateHistoryDTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionHistoryResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("transactionId")
    private Long transactionId;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("historyCount")
    private Integer historyCount;

    @JsonProperty("history")
    private List<TransactionStateHistoryDTO> history;

    @JsonProperty("retrievedAt")
    private LocalDateTime retrievedAt;
}
