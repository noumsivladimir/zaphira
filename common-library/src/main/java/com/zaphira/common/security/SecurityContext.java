package com.zaphira.common.security;

import com.zaphira.common.model.enums.RoleType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Security context holder for authenticated user information extracted from JWT
 */
@Data
@Builder
public class SecurityContext {
    
    private Long userId;
    private String email;
    private List<String> roles;
    private Long merchantId; // Only present for MERCHANT role
    
    /**
     * Check if user has a specific role
     */
    public boolean hasRole(RoleType role) {
        return roles != null && roles.contains(role.name());
    }
    
    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(RoleType... roles) {
        if (this.roles == null) {
            return false;
        }
        for (RoleType role : roles) {
            if (this.roles.contains(role.name())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if user is a merchant
     */
    public boolean isMerchant() {
        return hasRole(RoleType.MERCHANT);
    }
    
    /**
     * Check if user is an admin
     */
    public boolean isAdmin() {
        return hasRole(RoleType.ADMIN);
    }
    
    /**
     * Check if user is a regular user
     */
    public boolean isRegular() {
        return hasRole(RoleType.USER);
    }
    
    /**
     * Get merchant ID (throws exception if not a merchant)
     */
    public Long requireMerchantId() {
        if (merchantId == null) {
            throw new IllegalStateException("User is not a merchant or merchantId not available");
        }
        return merchantId;
    }
}
