package com.zaphira.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WalletPermissionResponse - DTO for wallet permission details.
 * 
 * Used for GET /api/wallet-permissions/{walletId} endpoint.
 * Maps WalletPermission entity to response format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPermissionResponse {
    
    /**
     * Permission ID
     */
    private Long id;
    
    /**
     * Wallet ID this permission applies to
     */
    private Long walletId;
    
    /**
     * Wallet number
     */
    private String walletNumber;
    
    /**
     * Type of permission (TRANSFER, WITHDRAWAL, DEPOSIT, etc.)
     */
    private String permissionType;
    
    /**
     * Maximum amount per transaction
     */
    private BigDecimal maxAmount;
    
    /**
     * Daily transaction limit
     */
    private BigDecimal dailyLimit;
    
    /**
     * Whether this permission is currently enabled
     */
    private Boolean enabled;
    
    /**
     * Whether transactions require approval
     */
    private Boolean requiresApproval;
    
    /**
     * When this permission was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When this permission was last updated
     */
    private LocalDateTime updatedAt;
}
