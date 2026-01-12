package com.zaphira.notification.service;

import com.zaphira.notification.model.VerificationToken;
import com.zaphira.notification.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final SmsService smsService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.max-tokens-per-user:5}")
    private int maxTokensPerUser;

    @Transactional
    public String generateAndSaveCode(Long userId) {
        // Nettoyer les tokens expirés avant de créer un nouveau
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // Limiter le nombre de tokens actifs par utilisateur
        cleanupOldTokensForUser(userId);

        // Générer un code unique pour cet utilisateur
        String code = generateUniqueCodeForUser(userId);

        VerificationToken token = VerificationToken.builder()
                .userId(userId)
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        tokenRepository.save(token);
        log.info("✅ Code de vérification généré pour l'utilisateur: {} (Total tokens actifs: {})",
                userId, tokenRepository.countByUserIdAndUsedFalse(userId));

        return code;
    }

    /**
     * Génère un code unique qui n'existe pas déjà pour cet utilisateur
     */
    private String generateUniqueCodeForUser(Long userId) {
        String code;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            code = String.format("%06d", secureRandom.nextInt(999999));
            attempts++;

            if (attempts >= maxAttempts) {
                log.warn("⚠️ Difficulté à générer un code unique pour l'utilisateur {} après {} tentatives", userId, attempts);
                // Forcer la suppression des anciens tokens pour cet utilisateur
                tokenRepository.deleteByUserIdAndUsedFalse(userId);
                break;
            }
        } while (tokenRepository.existsByUserIdAndCodeAndUsedFalse(userId, code));

        return code;
    }

    /**
     * Nettoie les anciens tokens pour un utilisateur spécifique
     */
    private void cleanupOldTokensForUser(Long userId) {
        List<VerificationToken> userTokens = tokenRepository.findByUserIdAndUsedFalseOrderByCreatedAtDesc(userId);

        if (userTokens.size() >= maxTokensPerUser) {
            // Garder seulement les N tokens les plus récents
            List<VerificationToken> tokensToDelete = userTokens.subList(maxTokensPerUser - 1, userTokens.size());
            tokenRepository.deleteAll(tokensToDelete);
            log.info("🧹 Nettoyage utilisateur {}: {} anciens tokens supprimés", userId, tokensToDelete.size());
        }
    }

    @Transactional
    public String generateAndSendOtp(Long userId, String phoneNumber) {
        String code = generateAndSaveCode(userId);

        try {
            smsService.sendOtpSms(phoneNumber, code);
            log.info("OTP sent successfully to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send OTP SMS to user {}: {}", userId, e.getMessage());
            // Note: We don't throw exception here to avoid exposing SMS failures to the user
            // The code is still generated and saved, user can request again
        }

        return code;
    }

    @Transactional
    public String generateOtpForJsonResponse(Long userId) {
        String code = generateAndSaveCode(userId);
        log.info("OTP generated for JSON response for user: {}", userId);
        return code;
    }

    @Transactional
    public boolean verifyCode(Long userId, String code) {
        LocalDateTime now = LocalDateTime.now();
        var tokenOpt = tokenRepository.findByUserIdAndCodeAndUsedFalseAndExpiresAtAfter(userId, code, now);

        if (tokenOpt.isPresent()) {
            tokenRepository.markAsUsed(userId, code);
            log.info("Verification code verified for user: {}", userId);
            return true;
        }

        log.warn("Invalid or expired verification code for user: {}", userId);
        return false;
    }

    public void handleUserRegistration(String firstName, String lastName, String phoneNumber, Long userId) {
        log.info("Handling user registration for: {} {} ({})", firstName, lastName, phoneNumber);

        // Générer le code de vérification (sans envoi SMS automatique)
        String code = generateAndSaveCode(userId);

        log.info("Verification code {} generated for user registration: {}", code, userId);
    }
}