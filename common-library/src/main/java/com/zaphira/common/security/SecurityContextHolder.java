package com.zaphira.common.security;

import com.zaphira.common.model.enums.RoleType;

/**
 * Thread-local storage for security context
 */
public class SecurityContextHolder {
    
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();
    
    /**
     * Set the security context for the current thread
     */
    public static void setContext(SecurityContext context) {
        contextHolder.set(context);
    }
    
    /**
     * Get the security context for the current thread
     */
    public static SecurityContext getContext() {
        return contextHolder.get();
    }
    
    /**
     * Clear the security context for the current thread
     */
    public static void clear() {
        contextHolder.remove();
    }
    
    /**
     * Get the current user ID
     */
    public static Long getCurrentUserId() {
        SecurityContext context = getContext();
        return context != null ? context.getUserId() : null;
    }
    
    /**
     * Get the current user email
     */
    public static String getCurrentUserEmail() {
        SecurityContext context = getContext();
        return context != null ? context.getEmail() : null;
    }
    
    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(RoleType role) {
        SecurityContext context = getContext();
        return context != null && context.hasRole(role);
    }
    
    /**
     * Check if current user is a merchant
     */
    public static boolean isMerchant() {
        SecurityContext context = getContext();
        return context != null && context.isMerchant();
    }
    
    /**
     * Get current merchant ID (throws exception if not a merchant)
     */
    public static Long getCurrentMerchantId() {
        SecurityContext context = getContext();
        if (context == null) {
            throw new IllegalStateException("No security context available");
        }
        return context.requireMerchantId();
    }
}
