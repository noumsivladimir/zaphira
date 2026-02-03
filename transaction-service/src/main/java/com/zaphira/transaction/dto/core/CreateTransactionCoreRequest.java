package com.zaphira.transaction.dto.core;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateTransactionCoreRequest - Request DTO for creating basic transactions
 * 
 * Validations:
 * - senderWalletId required
 * - receiverWalletId required
 * - amount > 0
 * - currency required
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionCoreRequest {

    @NotNull(message = "Sender wallet ID is required")
    private Long senderWalletId;

    @NotNull(message = "Receiver wallet ID is required")
    private Long receiverWalletId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private String currency;

    private String description;
}
