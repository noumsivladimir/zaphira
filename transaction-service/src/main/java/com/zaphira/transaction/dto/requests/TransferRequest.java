package com.zaphira.transaction.dto.requests;

import com.zaphira.transaction.model.enums.TransactionCategory;
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
public class TransferRequest {



    @NotBlank(message = "Source wallet number is required")
    private String senderWalletNumber;

//    private String sourceAccountNumber;

    @NotBlank(message = "Destination wallet number is required")
    private String receiverWalletNumber;

//    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

//    @NotBlank(message = "Currency is required")
//    private Currency currency = Currency.XAF;

    @NotNull(message = "Category is required")
    private TransactionCategory category;

    private String description;

    private String ipAddress;

    private String deviceInfo;
}