package com.zaphira.wallet.dto.request;

import com.zaphira.wallet.models.enums.PermissionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class GrantPermissionRequest {
    @NotBlank
    private String walletNumber;

    @NotNull
    private Long userId; // Utilisateur à qui accorder la permission

    @NotNull
    private PermissionLevel level;

    private Boolean canView;
    private Boolean canTransact;
    private Boolean canManageAccounts;
    private Boolean canManagePermissions;
    private Boolean canManageSubWallets;

    private BigDecimal maxTransactionAmount;
    private LocalDateTime expiresAt;
    private String notes;
}