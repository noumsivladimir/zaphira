package com.zaphira.auth.controller;

import com.zaphira.auth.dto.AuthResponse;
import com.zaphira.auth.dto.LoginRequest;
import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.dto.UserResponse;
import com.zaphira.auth.model.User;
import com.zaphira.auth.security.JwtUtil;
import com.zaphira.auth.service.RefreshTokenService;
import com.zaphira.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", ""})
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    // ✅ Register User
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // Inscription avec email + phoneNumber
        User user = userService.registerUser(request);

        var accessToken = jwtUtil.generateAccessToken(user.getEmail());
        var refreshToken = refreshTokenService.createToken(user.getId());

        var userResponse = new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getWallet() != null ? user.getWallet().getId() : null,
                user.getPhoneNumber() // <-- Ajout phoneNumber dans la réponse
        );

        return ResponseEntity.ok(
                new AuthResponse(accessToken, refreshToken.getToken(), userResponse)
        );
    }

    // ✅ Login User
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // Authentification par email ou phoneNumber
        User user = userService.authenticate(request.getEmailOrPhone(), request.getPassword());
        if (user == null) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        var accessToken = jwtUtil.generateAccessToken(user.getEmail());
        var refreshToken = refreshTokenService.createToken(user.getId());

        var userResponse = new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getWallet() != null ? user.getWallet().getId() : null,
                user.getPhoneNumber() // <-- Ajout phoneNumber dans la réponse
        );

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), userResponse));
    }

    // ✅ Refresh Token
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken) {
        try {
            if (!refreshTokenService.validate(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                    .body("Refresh token invalid or expired");
            }

            String email = jwtUtil.extractEmail(refreshToken);
            String newAccessToken = jwtUtil.generateAccessToken(email);

            // Optionnel : générer un nouveau refresh token
            // String newRefreshToken = refreshTokenService.generate(email);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("Invalid token format");
        }
    }


    // ✅ Logout = delete refresh token from DB
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        refreshTokenService.revokeTokenByAccessToken(token);

        return ResponseEntity.ok("Logged out successfully");
    }

    // ✅ Current user
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);

            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found");
            }

            var userResponse = new UserResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getWallet() != null ? user.getWallet().getId() : null,
                    user.getPhoneNumber() // <-- Ajout phoneNumber
            );

            return ResponseEntity.ok(userResponse);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }
    }
}
