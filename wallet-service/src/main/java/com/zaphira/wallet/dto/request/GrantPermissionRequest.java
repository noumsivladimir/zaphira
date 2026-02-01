package com.zaphira.wallet.dto.request;

import com.zaphira.common.model.enums.PermissionType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionRequest {

    @NotNull
    private Long walletId;

    @Enumerated
    @NotNull
    private PermissionType permissionType;

    private BigDecimal maxAmount;

    private BigDecimal dailyLimit;

    private Boolean requiresApproval;

}

