package com.zaphira.transaction.security;

//import com.zaphira.transaction.integration.user.FeignUserClient;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws java.io.IOException, jakarta.servlet.ServletException {

        String authHeader = request.getHeader("Authorization");
        // If a SecurityContext authentication is already present (e.g., tests using MockMvc request post-processor), honor it
        var existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null && existingAuth.getPrincipal() instanceof AuthenticatedUser) {
            AuthenticatedUser au = (AuthenticatedUser) existingAuth.getPrincipal();
            request.setAttribute("userId", au.getId());
            request.setAttribute("userEmail", au.getEmail());
            request.setAttribute("userRoles", au.getRoles());
            log.info("SecurityContext pre-populated for request {} userId={}", request.getRequestURI(), au.getId());
            filterChain.doFilter(request, response);
            return;
        }
        boolean isTransactionPath = request.getRequestURI() != null && request.getRequestURI().startsWith("/api/transactions");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                Long userId = jwtUtil.extractUserId(token);
                List<String> roles = jwtUtil.extractRoles(token);

                // Require userId claim to be present in the token.
                if (userId == null) {
                    log.warn("JWT missing 'userId' claim for request {}", request.getRequestURI());
                    if (isTransactionPath) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token missing userId claim");
                        return;
                    }
                } else {
                    List<SimpleGrantedAuthority> authorities = roles == null
                            ? Collections.emptyList()
                            : roles.stream()
                                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());

                    AuthenticatedUser principal = new AuthenticatedUser(userId, email, roles);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    request.setAttribute("userId", userId);
                    request.setAttribute("userEmail", email);
                    request.setAttribute("userRoles", roles);
                    log.info("Authenticated request to {} for userId={} email={}", request.getRequestURI(), userId, email);
                }
            } else {
                log.warn("Invalid JWT token for request {}", request.getRequestURI());
                if (isTransactionPath) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            }
        } else {
            // No Authorization header present
            if (isTransactionPath) {
                log.warn("Missing Authorization header for protected path {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
