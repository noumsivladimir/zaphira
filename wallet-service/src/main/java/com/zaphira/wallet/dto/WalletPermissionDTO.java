package com.zaphira.wallet.dto;

import com.zaphira.common.model.enums.PermissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class WalletPermissionDTO {
//    private Long id;
    private String walletNumber;
    private Long walletId;
    private List<PermissionType> walletPermission;
//    private boolean isEnabled;
//    private PermissionLevel level;
//    private Boolean canView;
//    private Boolean canTransact;
//    private Boolean canManageAccounts;
//    private Boolean canManagePermissions;
//    private Boolean canManageSubWallets;
//    private BigDecimal maxTransactionAmount;
//    private LocalDateTime grantedAt;
//    private LocalDateTime expiresAt;
//    private Boolean isActive;
}