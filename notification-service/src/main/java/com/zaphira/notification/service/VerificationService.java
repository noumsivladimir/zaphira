package com.zaphira.notification.service;

import com.zaphira.notification.model.VerificationToken;
import com.zaphira.notification.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateAndSaveCode(Long userId) {
        // Clean up expired tokens
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // Generate 6-digit code
        String code = String.format("%06d", secureRandom.nextInt(999999));

        VerificationToken token = VerificationToken.builder()
                .userId(userId)
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        tokenRepository.save(token);
        log.info("Generated verification code for user: {}", userId);
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

        // Générer le code de vérification
        String code = generateAndSaveCode(userId);

        // Le reste de la logique sera géré par UserEventListener via Kafka
        // Ici nous ne faisons que logger pour le test
        log.info("Verification code {} generated for user registration: {}", code, userId);
    }
}