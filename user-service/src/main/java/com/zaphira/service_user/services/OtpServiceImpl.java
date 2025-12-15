package com.zaphira.service_user.services;

import com.zaphira.service_user.model.entities.OtpToken;
import com.zaphira.service_user.model.enums.OtpPurpose;
import com.zaphira.service_user.repository.OtpTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final SmsService smsService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    @Override
    @Transactional
    public String generateAndSendOtp(String phoneNumber, OtpPurpose purpose) {
        // Supprimer les anciens OTP non utilisés
        otpTokenRepository.deleteByPhoneNumberAndPurpose(phoneNumber, purpose);

        // Générer un nouveau code
        String code = generateOtpCode();

        // Sauvegarder le token
        OtpToken otpToken = OtpToken.builder()
                .code(code)
                .phoneNumber(phoneNumber)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .attempts(0)
                .build();

        otpTokenRepository.save(otpToken);

        // Envoyer le SMS
        String message = String.format("Votre code de vérification Zaphira est: %s. Valide %d minutes.", code, OTP_EXPIRY_MINUTES);
        smsService.sendSms(phoneNumber, message);

        log.info("OTP sent to {}", maskPhoneNumber(phoneNumber));
        return code;
    }

    @Override
    @Transactional
    public boolean verifyOtp(String phoneNumber, String code, OtpPurpose purpose) {
        OtpToken otpToken = otpTokenRepository
                .findByPhoneNumberAndPurposeAndUsedFalse(phoneNumber, purpose)
                .orElse(null);

        if (otpToken == null) {
            log.warn("No OTP found for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Vérifier si expiré
        if (otpToken.isExpired()) {
            log.warn("OTP expired for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Vérifier le nombre de tentatives
        if (otpToken.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("Max OTP attempts reached for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Incrémenter les tentatives
        otpToken.setAttempts(otpToken.getAttempts() + 1);

        // Vérifier le code
        if (!otpToken.getCode().equals(code)) {
            otpTokenRepository.save(otpToken);
            log.warn("Invalid OTP code for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Marquer comme utilisé
        otpToken.setUsed(true);
        otpToken.setVerifiedAt(LocalDateTime.now());
        otpTokenRepository.save(otpToken);

        log.info("OTP verified successfully for phone: {}", maskPhoneNumber(phoneNumber));
        return true;
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", code);
    }

    private String maskPhoneNumber(String phone) {
        if (phone.length() <= 4) return "****";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }
}