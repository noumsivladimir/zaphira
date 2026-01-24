package com.zaphira.auth.controller;

import com.zaphira.auth.dto.AuthResponse;
import com.zaphira.auth.dto.LoginRequest;
import com.zaphira.auth.dto.UserResponse;
import com.zaphira.auth.exception.InvalidCredentialsException;
import com.zaphira.common.model.entities.User;
import com.zaphira.auth.service.UserServiceAsync;
import com.zaphira.auth.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Authentification
        User user = userService.authenticate(request.getPhoneNumber(), request.getPin());

        if (user == null) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String phoneNumber = user.getPhoneNumber() != null ? user.getPhoneNumber() : request.getPhoneNumber();

        // Génération du JWT
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), phoneNumber);

        String role = user.getRoleType() != null ? user.getRoleType().name() : "USER";
        String accountStatus = user.getAccountStatus() != null ? user.getAccountStatus().name() : "UNKNOWN";

        // Création de UserResponse via constructeur
        UserResponse userResponse = new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
            user.getEmail(),
            accountStatus,
            role,
                user.getWalletId()
        );

        return ResponseEntity.ok(new AuthResponse(accessToken, null, userResponse));
    }
}
