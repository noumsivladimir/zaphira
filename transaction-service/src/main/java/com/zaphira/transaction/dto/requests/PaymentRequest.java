package com.zaphira.transaction.dto.requests;

import com.zaphira.transaction.model.enums.TransactionCategory;
import com.zaphira.transaction.model.enums.TransactionChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank
    private String payerWalletNumber;

    @NotBlank
    private String merchantWalletNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionCategory category;

    @NotNull
    private TransactionChannel channel;

    private String description;
}
