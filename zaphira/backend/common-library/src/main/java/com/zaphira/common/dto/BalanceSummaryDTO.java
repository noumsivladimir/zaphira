package com.zaphira.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSummaryDTO {
    private String walletNumber;
    private BigDecimal ownBalance;
    private BigDecimal subWalletsBalance;
    private BigDecimal totalBalance;
    private List<SubWalletBalance> subWalletBalances;
}