package com.zaphira.wallet.dto;

import com.zaphira.wallet.model.enums.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class WalletPermissionDTO {
    private Long id;
    private String walletNumber;
    private Long userId;
    private PermissionLevel level;
    private Boolean canView;
    private Boolean canTransact;
    private Boolean canManageAccounts;
    private Boolean canManagePermissions;
    private Boolean canManageSubWallets;
    private BigDecimal maxTransactionAmount;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
}