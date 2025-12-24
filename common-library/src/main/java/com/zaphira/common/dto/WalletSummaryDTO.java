package com.zaphira.common.dto;

import com.zaphira.common.model.enums.Currency;
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
    private String walletNumber;
    private List <String> subWalletsName;
    private BigDecimal totalBalanceAllWallets;
    private Currency currency;
}