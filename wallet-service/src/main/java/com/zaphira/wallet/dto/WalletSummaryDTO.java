package com.zaphira.wallet.dto;

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
public class WalletSummaryDTO {
    private Long userId;
    private Integer totalWallets;
    private List<WalletDTO> wallets;
    private BigDecimal totalBalanceAllWallets;
}