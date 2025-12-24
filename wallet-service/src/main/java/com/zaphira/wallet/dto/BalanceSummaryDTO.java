package com.zaphira.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BalanceSummaryDTO {
    private String walletNumber;
    private BigDecimal ownBalance;
    private BigDecimal subWalletsBalance;
    private BigDecimal totalBalance;
    private List<SubWalletBalance> subWalletBalances;
}