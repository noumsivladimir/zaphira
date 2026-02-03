package com.zaphira.common.security;

/**
 * Constants for security configuration
 */
public class SecurityConstants {
    
    // JWT Header
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // JWT Claims
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_MERCHANT_ID = "merchantId";
    public static final String CLAIM_ROLES = "roles";
    
    // Role Names (matching RoleType enum)
    public static final String ROLE_REGULAR = "REGULAR";
    public static final String ROLE_MERCHANT = "MERCHANT";
    public static final String ROLE_ADMIN = "ADMIN";
    
    // SpEL Expression prefixes for @PreAuthorize
    public static final String HAS_ROLE_REGULAR = "hasRole('REGULAR')";
    public static final String HAS_ROLE_MERCHANT = "hasRole('MERCHANT')";
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    
    public static final String HAS_ANY_ROLE_USER = "hasAnyRole('REGULAR', 'MERCHANT')";
    public static final String HAS_ANY_ROLE_ALL = "hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')";
    
    // Public endpoints (no authentication required)
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/verify-otp",
            "/api/auth/verify-email",
            "/api/auth/resend-otp",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };
    
    private SecurityConstants() {
        // Utility class - no instantiation
    }
}
