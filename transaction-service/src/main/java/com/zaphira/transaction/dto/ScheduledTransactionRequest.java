package com.zaphira.transaction.dto;

import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ScheduledTransactionRequest {

    @NotBlank
    private String senderWalletNumber;

    @NotBlank
    private String receiverWalletNumber;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private TransactionType type;

    @NotNull
    private TransactionChannel channel;

    private String description;

    @NotBlank
    private String requestedBy;

    @NotNull
    @Future
    private LocalDateTime scheduledFor;
}


