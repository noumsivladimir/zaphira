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
public class WithdrawalRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Wallet number is required")
    private String walletNumber;

    private String accountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Category is required")
    private TransactionCategory category;

    private String description;

    private String withdrawalMethod; // CASH, BANK_TRANSFER, MOBILE_MONEY

    private String bankAccount;

    private String mobileMoneyNumber;

    private String ipAddress;

    private String deviceInfo;
}