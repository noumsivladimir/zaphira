package com.zaphira.common.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.List;

/**
 * Utility class for parsing and validating JWT tokens in microservices
 */
public class JwtParser {
    
    /**
     * Parse JWT token and extract security context
     */
    public static SecurityContext parseToken(String token, String secret) {
        try {
            Key key = Keys.hmacShaKeyFor(secret.getBytes());
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String email = claims.getSubject();
            Long userId = extractLong(claims, "userId");
            Long merchantId = extractLong(claims, "merchantId");
            List<String> roles = extractRoles(claims);
            
            return SecurityContext.builder()
                    .userId(userId)
                    .email(email)
                    .roles(roles)
                    .merchantId(merchantId)
                    .build();
                    
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JWT token", e);
        }
    }
    
    /**
     * Validate JWT token
     */
    public static boolean validateToken(String token, String secret) {
        try {
            Key key = Keys.hmacShaKeyFor(secret.getBytes());
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract Long value from claims
     */
    private static Long extractLong(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }
    
    /**
     * Extract roles list from claims
     */

    private static List<String> extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }
        if (roles instanceof String single) {
            return List.of(single);
        }
        return List.of();
    }
}
