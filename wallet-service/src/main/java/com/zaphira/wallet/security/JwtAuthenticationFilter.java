package com.zaphira.wallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
            throws IOException, jakarta.servlet.ServletException {

        String authHeader = request.getHeader("Authorization");

        var existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null && existingAuth.getPrincipal() instanceof AuthenticatedUser au) {
            request.setAttribute("userId", au.getId());
            request.setAttribute("userEmail", au.getEmail());
            request.setAttribute("userRoles", au.getRoles());
            log.debug("SecurityContext already populated for {} userId={}", request.getRequestURI(), au.getId());
            filterChain.doFilter(request, response);
            return;
        }

        boolean protectedPath = request.getRequestURI() != null && request.getRequestURI().startsWith("/api/wallets");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                Long userId = jwtUtil.extractUserId(token);
                List<String> roles = jwtUtil.extractRoles(token);

                if (userId == null) {
                    log.warn("JWT missing userId claim for {}", request.getRequestURI());
                    if (protectedPath) {
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
                    log.debug("Authenticated request {} userId={} email={}", request.getRequestURI(), userId, email);
                }
            } else {
                log.warn("Invalid JWT for {}", request.getRequestURI());
                if (protectedPath) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            }
        } else if (protectedPath) {
            log.warn("Missing Authorization header for protected path {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
