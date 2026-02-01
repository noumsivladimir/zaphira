package com.zaphira.common.dto.response;

import com.zaphira.common.model.enums.PermissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckResponse {
    private Long walletId;
    private PermissionType permissionType;
    private Boolean hasPermission;
    private String message; // Optionnel
}