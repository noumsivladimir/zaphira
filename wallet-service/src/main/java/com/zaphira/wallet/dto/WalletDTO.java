package com.zaphira.wallet.dto;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.wallet.models.entities.SubWallet;
import com.zaphira.wallet.models.enums.WalletStatus;
import com.zaphira.wallet.models.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private Long id;
    private Long userId;
    private String walletNumber;
    private WalletType type;
    private WalletStatus status;
    private List<SubWallet> subWallets;
    private Currency currency;
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private BigDecimal totalBalance;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailySpent;
    private BigDecimal monthlySpent;
    private Boolean isPrimary;
    private String frozenReason;
    private LocalDateTime frozenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String metadata;
}
