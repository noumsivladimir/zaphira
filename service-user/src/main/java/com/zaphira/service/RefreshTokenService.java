package com.zaphira.zaphira;

import com.zaphira.zaphira.RefreshToken;
import com.zaphira.zaphira.RefreshTokenRepository;
import com.zaphira.zaphira.UserRepository;
import com.zaphira.zaphira.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // ✅ Create database refresh token
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

    // ✅ Validate (DB token + expiration)
    public boolean validate(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiryDate().isAfter(Instant.now()))
                .isPresent();
    }

    // ✅ Logout (revoke refresh tokens)
    public void revokeTokenByAccessToken(String accessToken) {
        String email = jwtUtil.extractEmail(accessToken);

        var user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            refreshTokenRepository.revokeTokensByUserId(user.getId());
        }
    }

    // ✅ Cleanup expired tokens
    public void removeExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }

    @PostConstruct
    public void initCleanup() {
        removeExpiredTokens();
    }

    @Scheduled(cron = "0 0 * * * *") // chaque heure
    public void scheduledCleanup() {
        removeExpiredTokens();
    }
}
