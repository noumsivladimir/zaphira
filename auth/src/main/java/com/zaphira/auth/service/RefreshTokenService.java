package com.zaphira.auth.service;

import com.zaphira.auth.model.RefreshToken;
import com.zaphira.auth.repository.RefreshTokenRepository;
import com.zaphira.auth.repository.UserRepository;
import com.zaphira.auth.security.JwtUtil;
import com.zaphira.auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // ✅ Création d'un token avec limite de 3 tokens valides
    public RefreshToken createToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Récupérer tous les tokens valides de l'utilisateur
        List<RefreshToken> validTokens = refreshTokenRepository
                .findByUserAndRevokedFalseAndExpiryDateAfter(user, Instant.now());

        // Limite à 3 tokens : supprimer le plus ancien si nécessaire
        if (validTokens.size() >= 3) {
            RefreshToken oldestToken = validTokens.stream()
                    .min(Comparator.comparing(RefreshToken::getExpiryDate))
                    .get();
            refreshTokenRepository.delete(oldestToken);
        }

        // Créer le nouveau token
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plusMillis(jwtUtil.getRefreshExpirationMillis()))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // ✅ Validation du token (DB + expiration)
    public boolean validate(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiryDate().isAfter(Instant.now()))
                .map(t -> {
                    t.setRevoked(true);           // marque comme utilisé
                    refreshTokenRepository.save(t);
                    return true;
                })
                .orElse(false);
    }

    // ✅ Déconnexion (révocation des refresh tokens)
    public void revokeTokenByAccessToken(String accessToken) {
        String email = jwtUtil.extractEmail(accessToken);
        var user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            refreshTokenRepository.revokeTokensByUserId(user.getId());
        }
    }

    // ✅ Nettoyage des tokens expirés
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
