package com.zaphira.notification.service;

import com.zaphira.common.model.enums.OtpPurpose;
import com.zaphira.notification.model.OtpCode;
import com.zaphira.notification.repository.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    /**
     * Génère un OTP pour un numéro de téléphone
     */
    @Transactional
    public String generateOtp(String phoneNumber, OtpPurpose purpose) {
        // Supprimer les anciens OTP non utilisés
        otpCodeRepository.deleteByPhoneNumberAndPurpose(phoneNumber, purpose);

        // Générer un nouveau code
        String code = generateOtpCode();

        // Sauvegarder le code
        OtpCode otpCode = OtpCode.builder()
                .phone(phoneNumber)
                .phoneNumber(phoneNumber)
                .purpose(purpose)
                .otpHash("") // Peut être hashé si nécessaire
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .consumed(false)
                .attempts(0)
                .build();

        otpCodeRepository.save(otpCode);

        log.info("OTP generated for phone: {}", maskPhoneNumber(phoneNumber));
        return code;
    }

    /**
     * Génère un OTP pour un email
     */
    @Transactional
    public String generateOtpForEmail(String email, OtpPurpose purpose) {
        // Supprimer les anciens OTP non utilisés
        otpCodeRepository.deleteByEmailAndPurpose(email, purpose);

        // Générer un nouveau code
        String code = generateOtpCode();

        // Sauvegarder le code
        OtpCode otpCode = OtpCode.builder()
                .phone("") // Vide pour les OTP email
                .phoneNumber("") // Vide pour les OTP email
                .email(email)
                .purpose(purpose)
                .otpHash("")
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .consumed(false)
                .attempts(0)
                .build();

        otpCodeRepository.save(otpCode);

        log.info("OTP generated for email: {}", maskEmail(email));
        return code;
    }

    /**
     * Génère un code OTP aléatoire
     */
    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", code);
    }

    /**
     * Masque un numéro de téléphone pour les logs
     */
    private String maskPhoneNumber(String phone) {
        if (phone.length() <= 4) return "****";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * Masque un email pour les logs
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return username + "***@" + domain;
        }

        return username.substring(0, 2) + "***@" + domain;
    }
}