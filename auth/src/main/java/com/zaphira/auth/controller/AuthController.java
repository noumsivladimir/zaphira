package com.zaphira.auth.controller;

import com.zaphira.auth.dto.AuthResponse;
import com.zaphira.auth.dto.LoginRequest;
import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.dto.UserResponse;
import com.zaphira.auth.security.JwtUtil;
import com.zaphira.auth.service.RefreshTokenService;
import com.zaphira.auth.service.UserService;
import lombok.RequiredArgsConstructor;
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

            var user = userService.registerUser(request);

            var accessToken = jwtUtil.generateAccessToken(user.getEmail());
            var refreshToken = refreshTokenService.createToken(user.getId());

        var userResponse = new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getWallet().getId()        // <-- AJOUT
        );

        return ResponseEntity.ok(
                new AuthResponse(accessToken, refreshToken.getToken(), userResponse)
        );
    }


    // ✅ Login User
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        var user = userService.authenticate(request.getEmail(), request.getPassword());
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
        user.getWallet() != null ? user.getWallet().getId() : null // <-- AJOUT DU 5ème PARAM
);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), userResponse));
    }

    // ✅ Refresh Token
@PostMapping("/refresh")
public ResponseEntity<?> refresh(@RequestParam String refreshToken) {

    var tokenEntity = refreshTokenService.findByToken(refreshToken);

    if (tokenEntity.isEmpty() || !refreshTokenService.validate(refreshToken)) {
        return ResponseEntity.status(401).body("Refresh token invalid or expired");
    }

    var user = tokenEntity.get().getUser();
    String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());

    return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken, null));
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

@GetMapping("/me")
public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
    try {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        var user = userService.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        var userResponse = new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole().name(),
        user.getWallet() != null ? user.getWallet().getId() : null // <-- AJOUT DU 5ème PARAM
);

        return ResponseEntity.ok(userResponse);

    } catch (Exception e) {
        return ResponseEntity.status(401).body("Invalid or expired token");
    }
}

}
