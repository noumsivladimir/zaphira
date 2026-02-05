package com.zaphira.auth.controller;

import com.zaphira.auth.dto.*;
import com.zaphira.auth.model.RefreshToken;
import com.zaphira.common.model.entities.User;
import com.zaphira.auth.service.RefreshTokenService;
import com.zaphira.auth.service.UserServiceAsync;
import com.zaphira.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceAsync userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Authentification
        User user = userService.authenticate(request.getPhoneNumber(), request.getPin());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        // Génération du JWT
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getPhoneNumber(), user.getRoleType());
        
        // Génération du refresh token
        RefreshToken refreshToken = refreshTokenService.createToken(user.getUserId());

        // Création de UserResponse via constructeur
        UserResponse userResponse = new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoleType().name(),
                user.getWalletId()
        );

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), userResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("🔄 Refresh token request received");
        
        String refreshTokenString = request.getRefreshToken();
        
        if (refreshTokenString == null || refreshTokenString.isEmpty()) {
            log.warn("⚠️ Refresh token is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token is required");
        }

        // Validate refresh token
        if (!refreshTokenService.validate(refreshTokenString)) {
            log.warn("⚠️ Invalid or expired refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        // Get userId from refresh token
        Long userId = refreshTokenService.getUserId(refreshTokenString);
        String email = refreshTokenService.getEmail(refreshTokenString);

        if (userId == null || email == null) {
            log.error("❌ Could not extract user information from refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        // Get user to retrieve role
        User user = userService.getUserById(userId);
        if (user == null) {
            log.error("❌ User not found: {}", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        // Generate new access token with same claims
        String newAccessToken = jwtUtil.generateAccessToken(userId, email, user.getRoleType());

        log.info("✅ Access token refreshed successfully for user: {}", userId);

        // Return new access token with same refresh token
        UserResponse userResponse = new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoleType().name(),
                user.getWalletId()
        );

        return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshTokenString, userResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        log.info("🚪 Logout request received");
        
        String accessToken = request.getAccessToken();
        
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("⚠️ Access token is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Access token is required");
        }

        try {
            // Revoke all refresh tokens for this user
            refreshTokenService.revokeTokenByAccessToken(accessToken);
            
            log.info("✅ User logged out successfully");
            return ResponseEntity.ok("Logged out successfully");
            
        } catch (Exception e) {
            log.error("❌ Error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        log.info("🔍 Token validation request received");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ Invalid Authorization header format");
            return ResponseEntity.ok(ValidateTokenResponse.builder()
                    .valid(false)
                    .message("Invalid Authorization header format")
                    .build());
        }

        String token = authHeader.substring(7);

        try {
            // Validate JWT signature and expiration
            boolean isValid = jwtUtil.validateToken(token);

            if (!isValid) {
                log.warn("⚠️ Token validation failed");
                return ResponseEntity.ok(ValidateTokenResponse.builder()
                        .valid(false)
                        .message("Invalid or expired token")
                        .build());
            }

            // Extract claims
            Long userId = jwtUtil.extractUserId(token);
            String email = jwtUtil.extractEmail(token);

            // Get expiration time
            String expiresAt = getExpirationTime(token);

            log.info("✅ Token is valid for user: {}", userId);

            return ResponseEntity.ok(ValidateTokenResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .email(email)
                    .expiresAt(expiresAt)
                    .message("Token is valid")
                    .build());

        } catch (Exception e) {
            log.error("❌ Token validation error: {}", e.getMessage());
            return ResponseEntity.ok(ValidateTokenResponse.builder()
                    .valid(false)
                    .message("Token validation failed: " + e.getMessage())
                    .build());
        }
    }

    private String getExpirationTime(String token) {
        try {
            var claims = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                            jwtUtil.toString().getBytes())) // Simplified for demo
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration() != null) {
                return claims.getExpiration().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
        } catch (Exception e) {
            log.warn("Could not extract expiration time: {}", e.getMessage());
        }
        return null;
    }
}
