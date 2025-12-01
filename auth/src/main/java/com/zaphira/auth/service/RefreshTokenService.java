package com.zaphira.auth.service;

import com.zaphira.auth.model.RefreshToken;
import com.zaphira.auth.repository.RefreshTokenRepository;
import com.zaphira.auth.repository.UserRepository;
import com.zaphira.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // ----------------------------------------------------------
    // 🎯 1. Create refresh token in DB
    // ----------------------------------------------------------
    public RefreshToken createToken(Long userId) {
        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(userRepository.findById(userId).orElseThrow())
                .expiryDate(Instant.now().plusMillis(jwtUtil.getRefreshExpirationMillis()))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // ----------------------------------------------------------
    // 🎯 2. Validate refresh token
    // ----------------------------------------------------------
    public boolean validate(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiryDate().isAfter(Instant.now()))
                .isPresent();
    }

    // ----------------------------------------------------------
    // 🎯 3. 🔍 findByToken() → utilisé pour le /refresh
    // ----------------------------------------------------------
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // ----------------------------------------------------------
    // 🎯 4. getUserId(token) → renvoie l'ID du user lié au refresh token
    // ----------------------------------------------------------
    public Long getUserId(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(rt -> rt.getUser().getId())
                .orElse(null);
    }

    // ----------------------------------------------------------
    // 🎯 5. getEmail(token) → renvoie l’email du user lié au refresh token
    // ----------------------------------------------------------
    public String getEmail(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(rt -> rt.getUser().getEmail())
                .orElse(null);
    }

    // ----------------------------------------------------------
    // 🎯 6. Revoke (logout)
    // ----------------------------------------------------------
    public void revokeTokenByAccessToken(String accessToken) {
        String email = jwtUtil.extractEmail(accessToken);

        var user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            refreshTokenRepository.revokeTokensByUserId(user.getId());
        }
    }

    // ----------------------------------------------------------
    // 🎯 7. Cleanup expired tokens
    // ----------------------------------------------------------
    public void removeExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }

    @PostConstruct
    public void initCleanup() {
        removeExpiredTokens();
    }

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void scheduledCleanup() {
        removeExpiredTokens();
    }
}
