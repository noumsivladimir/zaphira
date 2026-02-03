package com.zaphira.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

/**
 * Global JWT validation filter for API Gateway
 * Validates JWT tokens and adds user context headers to downstream requests
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/verify-otp",
            "/api/auth/verify-email",
            "/api/auth/resend-otp"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("✅ Public endpoint accessed: {}", path);
            return chain.filter(exchange);
        }
        
        // Extract JWT from Authorization header
        String token = extractJwtFromRequest(request);
        
        if (token == null) {
            log.warn("❌ No JWT token found in request to: {}", path);
            return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }
        
        try {
            // Validate JWT and extract claims
            Claims claims = validateTokenAndExtractClaims(token);
            
            // Extract roles safely
            Object rolesObj = claims.get("roles");
            String rolesHeader = "";
            if (rolesObj instanceof List<?>) {
                rolesHeader = String.join(",", ((List<?>) rolesObj).stream()
                        .map(Object::toString)
                        .toList());
            }
            
            // Add user context headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", claims.get("userId", Long.class).toString())
                    .header("X-User-Email", claims.getSubject())
                    .header("X-User-Roles", rolesHeader)
                    .build();
            
            // Add merchantId header if present
            Object merchantId = claims.get("merchantId");
            if (merchantId != null) {
                modifiedRequest = modifiedRequest.mutate()
                        .header("X-Merchant-Id", merchantId.toString())
                        .build();
            }
            
            log.debug("✅ JWT validated for user: {} accessing: {}", claims.getSubject(), path);
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (Exception e) {
            log.error("❌ JWT validation failed: {}", e.getMessage());
            return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
        }
    }
    
    /**
     * Check if path is a public endpoint
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String extractJwtFromRequest(ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        
        String bearerToken = headers.get(0);
        if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    /**
     * Validate JWT token and extract claims
     */
    private Claims validateTokenAndExtractClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Return error response
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        String errorJson = String.format("{\"error\": \"%s\", \"status\": %d}", message, status.value());
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorJson.getBytes())));
    }
    
    @Override
    public int getOrder() {
        return -100; // Execute before other filters
    }
}
