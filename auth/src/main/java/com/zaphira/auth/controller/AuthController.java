package com.zaphira.auth.controller;

import com.zaphira.auth.dto.AuthResponse;
import com.zaphira.auth.dto.LoginRequest;
import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.dto.RegisterResponse;
import com.zaphira.auth.dto.UserResponse;
import com.zaphira.auth.security.JwtUtil;
import com.zaphira.auth.service.RefreshTokenService;
import com.zaphira.auth.service.UserService;
import com.zaphira.common.dto.WalletDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/auth", ""})
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * Enregistre un nouvel utilisateur et crée automatiquement un wallet.
     * 
     * @param request Les informations d'inscription
     * @return RegisterResponse contenant les tokens, l'utilisateur et le wallet créé
     */
    @PostMapping("/register")
public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    try {
        // Créer l'utilisateur (et créer son wallet si nécessaire)
        var user = userService.registerUser(request);

        // Générer les tokens
        var accessToken = jwtUtil.generateAccessToken(user.getEmail());
        var refreshToken = refreshTokenService.createToken(user.getId());

        // Récupérer le wallet via l'ID si disponible
        WalletDTO wallet = null;
        if (user.getWalletId() != null) {
            wallet = new WalletDTO();
            wallet.setId(user.getWalletId());
            // Optionnel : tu peux compléter walletNumber ou currency si nécessaire
        }

        // Construire la réponse utilisateur
        var userResponse = new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getWalletId()
        );

        // Construire la réponse complète avec wallet
        var registerResponse = RegisterResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(userResponse)
                .wallet(wallet)
                .build();

        // Retourner la réponse
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);

    } catch (Exception e) {
        log.error("❌ Error during user registration", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error during registration: " + e.getMessage());
    }
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
        null  // Wallet ID fetched separately if needed
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
        null  // Wallet ID fetched separately if needed
);

        return ResponseEntity.ok(userResponse);

    } catch (Exception e) {
        return ResponseEntity.status(401).body("Invalid or expired token");
    }
}

}
