package com.zaphira.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for microservices
 * Extracts JWT from Authorization header, validates it, and sets Spring Security context
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final String jwtSecret;
    
    public JwtAuthenticationFilter(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && JwtParser.validateToken(jwt, jwtSecret)) {
                // Parse JWT and extract security context
                com.zaphira.common.security.SecurityContext securityContext = JwtParser.parseToken(jwt, jwtSecret);
                
                // Store in thread-local for easy access
                com.zaphira.common.security.SecurityContextHolder.setContext(securityContext);
                
                // Also set Spring Security context
                List<SimpleGrantedAuthority> authorities = securityContext.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                securityContext.getEmail(), 
                                null, 
                                authorities
                        );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("✅ JWT authenticated for user: {} with roles: {}", 
                         securityContext.getEmail(), securityContext.getRoles());
            }
            
        } catch (Exception e) {
            log.error("❌ JWT authentication failed: {}", e.getMessage());
        } finally {
            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clear thread-local context after request
                com.zaphira.common.security.SecurityContextHolder.clear();
            }
        }
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        
        if (bearerToken != null && bearerToken.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return bearerToken.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        
        return null;
    }
}
