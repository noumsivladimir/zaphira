package com.zaphira.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotBlank
    private String senderWalletNumber;

    @NotBlank
    private String receiverWalletNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String currency;
    private String reference;
    private String description;
}

