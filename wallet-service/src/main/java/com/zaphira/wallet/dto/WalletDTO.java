package com.zaphira.wallet.dto;

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
    private String frozenBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime lastLimitReset;
    private String metadata;
    private Long version;
}
