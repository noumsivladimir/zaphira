package com.zaphira.auth.controller;

import com.zaphira.auth.dto.AuthResponse;
import com.zaphira.auth.dto.LoginRequest;
import com.zaphira.auth.dto.UserResponse;
import com.zaphira.common.model.entities.User;
import com.zaphira.auth.service.UserServiceAsync;
import com.zaphira.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceAsync userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Authentification
        User user = userService.authenticate(request.getPhoneNumber(), request.getPin());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        // Génération du JWT
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getPhoneNumber());

        // Création de UserResponse via constructeur
        UserResponse userResponse = new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoleType().name(),
                user.getWalletId()
        );

        return ResponseEntity.ok(new AuthResponse(accessToken, null, userResponse));
    }
}
