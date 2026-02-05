package com.zaphira.transaction.config;

import com.zaphira.common.security.SecurityConstants;
import com.zaphira.transaction.security.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for Transaction Service
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {
    
    @Autowired(required = false)
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (stateless JWT authentication)
                .csrf(AbstractHttpConfigurer::disable)
                
                // Disable CORS (or configure as needed)
                .cors(AbstractHttpConfigurer::disable)
                
                // Stateless session management
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );
                
                // Add JWT filter before Spring Security's UsernamePasswordAuthenticationFilter
                if (jwtAuthenticationFilter != null) {
                    log.info("JWT Authentication Filter is enabled");
                    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                } else {
                    log.warn("JWT Authentication Filter is DISABLED - only for test environments!");
                }
        
        return http.build();
    }
}
