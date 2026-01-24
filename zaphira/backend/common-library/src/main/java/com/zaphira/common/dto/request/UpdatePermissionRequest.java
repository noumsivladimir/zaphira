package com.zaphira.common.dto.request;

import com.zaphira.common.model.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionRequest {

    @NotNull(message = "Permission level is required")
    private PermissionLevel level;

    private Boolean canView;

    private Boolean canTransact;

    private Boolean canManageAccounts;

    private Boolean canManagePermissions;

    private Boolean canManageSubWallets;

    private BigDecimal maxTransactionAmount;

    private LocalDateTime expiresAt;

    private String notes;

    // Utilisé pour activer/désactiver la permission
    private Boolean isActive;
}