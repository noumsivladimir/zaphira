package com.zaphira.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceUpdatedEvent {
    private String walletNumber;
    private Long userId;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private String transactionReference;
    private String updateType; // CREDIT, DEBIT, TRANSFER
    private String correlationId;
}