package com.zaphira.wallet.dto;

import com.zaphira.wallet.models.enums.PermissionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EffectivePermission {

    private PermissionType type;
    private Boolean enabled;
    private BigDecimal maxAmount;
    private BigDecimal dailyLimit;
    private Boolean customized; // Indique si c'est personnalisé
    private String description;
}
