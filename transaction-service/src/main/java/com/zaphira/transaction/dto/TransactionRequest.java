package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Request DTO pour la création d'une transaction
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Sender wallet number is required")
    @JsonProperty("senderWalletNumber")
    private String senderWalletNumber;

    @NotBlank(message = "Receiver wallet number is required")
    @JsonProperty("receiverWalletNumber")
    private String receiverWalletNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @JsonProperty("currency")
    private String currency;

    @NotNull(message = "Transaction type is required")
    @JsonProperty("type")
    private TransactionType type;

    @NotNull(message = "Transaction channel is required")
    @JsonProperty("channel")
    private TransactionChannel channel;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @JsonProperty("description")
    private String description;

    @JsonProperty("processInstantly")
    @Builder.Default
    private boolean processInstantly = true;

    @NotBlank(message = "Requested by is required")
    @JsonProperty("requestedBy")
    private String requestedBy;
}



