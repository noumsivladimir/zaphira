package com.zaphira.service_user.services;

import com.zaphira.service_user.model.entities.OtpCode;
import com.zaphira.service_user.model.enums.OtpPurpose;
import com.zaphira.service_user.repository.OtpCodeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final SmsService smsService;
    private final EmailService emailService;
    private final TelegramService telegramService;
    private final com.zaphira.service_user.client.NotificationServiceClient notificationServiceClient;

    @Value("${app.verification.base-url}")
    private String verificationBaseUrl;

    @Value("${telegram.chat.id}")
    private String defaultChatId;

    public OtpServiceImpl(OtpCodeRepository otpCodeRepository, SmsService smsService, EmailService emailService, TelegramService telegramService, com.zaphira.service_user.client.NotificationServiceClient notificationServiceClient) {
        this.otpCodeRepository = otpCodeRepository;
        this.smsService = smsService;
        this.emailService = emailService;
        this.telegramService = telegramService;
        this.notificationServiceClient = notificationServiceClient;
    }

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    @Override
    @Transactional
    public String generateOtp(String phoneNumber, OtpPurpose purpose) {
        // Supprimer les anciens OTP non utilisés
        otpCodeRepository.deleteByPhoneNumberAndPurpose(phoneNumber, purpose);

        // Générer un nouveau code
        String code = generateOtpCode();

        // Sauvegarder le code
        OtpCode otpCode = OtpCode.builder()
                .phone(phoneNumber) // Pour compatibilité avec la table
                .phoneNumber(phoneNumber)
                .purpose(purpose)
                .otpHash("") // Peut être laissé vide ou hashé selon les besoins
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

    @Override
    @Transactional
    public String generateAndSendOtp(String phoneNumber, OtpPurpose purpose) {
        return generateAndSendOtp(phoneNumber, null, purpose);
    }

    @Override
    @Transactional
    public String generateAndSendOtp(String phoneNumber, String email, OtpPurpose purpose) {
        // Supprimer les anciens OTP non utilisés
        if (email != null) {
            otpCodeRepository.deleteByEmailAndPurpose(email, purpose);
        } else {
            otpCodeRepository.deleteByPhoneNumberAndPurpose(phoneNumber, purpose);
        }

        // Générer un nouveau code
        String code = generateOtpCode();

        // Sauvegarder le code
        OtpCode otpCode = OtpCode.builder()
                .phone(phoneNumber) // Pour compatibilité avec la table
                .phoneNumber(phoneNumber)
                .email(email)
                .purpose(purpose)
                .otpHash("") // Peut être laissé vide ou hashé selon les besoins
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .consumed(false)
                .attempts(0)
                .build();

        otpCodeRepository.save(otpCode);

        // Envoyer selon le canal disponible
        if (email != null) {
            // Envoyer par email et par Telegram
            sendOtpEmail(email, code);
            sendOtpByTelegram(email, code);
        } else {
            // Envoyer via notification-service pour SMS
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("phoneNumber", phoneNumber);
                payload.put("otp", code);
                payload.put("purpose", purpose.name());
                payload.put("expiresInMinutes", OTP_EXPIRY_MINUTES);

                notificationServiceClient.sendOtpPayload(payload);
                log.info("OTP sent via notification-service for phone: {}", maskPhoneNumber(phoneNumber));
            } catch (Exception ex) {
                log.warn("Failed to send via notification-service, falling back to direct SMS for phone: {}", maskPhoneNumber(phoneNumber), ex);
                String message = String.format("Votre code de vérification Zaphira est: %s. Valide %d minutes.", code, OTP_EXPIRY_MINUTES);
                smsService.sendSms(phoneNumber, message);
            }
        }

        log.info("OTP sent to {} for purpose: {}",
            email != null ? maskEmail(email) : maskPhoneNumber(phoneNumber),
            purpose);

        return code;
    }

    @Override
    @Transactional
    public boolean verifyOtp(String phoneNumber, String code, OtpPurpose purpose) {
        OtpCode otpCode = otpCodeRepository
                .findByPhoneNumberAndPurposeAndUsedFalse(phoneNumber, purpose)
                .orElse(null);

        if (otpCode == null) {
            log.warn("No OTP found for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Vérifier si expiré
        if (otpCode.isExpired()) {
            log.warn("OTP expired for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Vérifier le nombre de tentatives
        if (!otpCode.canAttempt()) {
            log.warn("Max OTP attempts reached for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Incrémenter les tentatives
        otpCode.setAttempts(otpCode.getAttempts() + 1);

        // Vérifier le code
        if (!otpCode.getCode().equals(code)) {
            otpCodeRepository.save(otpCode);
            log.warn("Invalid OTP code for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Marquer comme utilisé
        otpCode.setUsed(true);
        otpCode.setVerifiedAt(LocalDateTime.now());
        otpCodeRepository.save(otpCode);

        log.info("OTP verified successfully for phone: {}", maskPhoneNumber(phoneNumber));
        return true;
    }

    @Override
    @Transactional
    public boolean verifyOtpByEmail(String email, String code, OtpPurpose purpose) {
        OtpCode otpCode = otpCodeRepository
                .findByEmailAndPurposeAndUsedFalse(email, purpose)
                .orElse(null);

        if (otpCode == null) {
            log.warn("No OTP found for email: {}", maskEmail(email));
            return false;
        }

        // Vérifier si expiré
        if (otpCode.isExpired()) {
            log.warn("OTP expired for email: {}", maskEmail(email));
            return false;
        }

        // Vérifier le nombre de tentatives
        if (!otpCode.canAttempt()) {
            log.warn("Max OTP attempts reached for email: {}", maskEmail(email));
            return false;
        }

        // Incrémenter les tentatives
        otpCode.setAttempts(otpCode.getAttempts() + 1);

        // Vérifier le code
        if (!otpCode.getCode().equals(code)) {
            otpCodeRepository.save(otpCode);
            log.warn("Invalid OTP code for email: {}", maskEmail(email));
            return false;
        }

        // Marquer comme utilisé
        otpCode.setUsed(true);
        otpCode.setVerifiedAt(LocalDateTime.now());
        otpCodeRepository.save(otpCode);

        log.info("OTP verified successfully for email: {}", maskEmail(email));
        return true;
    }
    @Override
    @Transactional
    public void markOtpAsConsumed(String phoneNumber, OtpPurpose purpose) {
        OtpCode otpCode = otpCodeRepository
                .findByPhoneNumberAndPurposeAndUsedFalse(phoneNumber, purpose)
                .orElse(null);

        if (otpCode != null) {
            otpCode.setConsumed(true);
            otpCodeRepository.save(otpCode);
            log.info("OTP marked as consumed for phone: {}", maskPhoneNumber(phoneNumber));
        }
    }
    private void sendVerificationEmail(String email, String code) {
        String subject = "Zaphira - Vérifiez votre email";

        // Créer l'URL de vérification
        String verificationUrl = String.format("%s/api/users/verify-email-link?email=%s&code=%s",
                verificationBaseUrl,
                java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8),
                code);

        // Email HTML avec lien de vérification
        String htmlBody = String.format(
            "<!DOCTYPE html>" +
            "<html lang='fr'>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>Vérification Email - Zaphira</title>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f4f4f4; margin: 0; padding: 20px; }" +
            ".container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
            ".header { text-align: center; margin-bottom: 30px; }" +
            ".logo { font-size: 24px; font-weight: bold; color: #007bff; margin-bottom: 10px; }" +
            ".title { font-size: 20px; color: #333; margin-bottom: 20px; }" +
            ".message { font-size: 16px; margin-bottom: 30px; line-height: 1.6; }" +
            ".verification-button { display: inline-block; padding: 15px 30px; background: #28a745; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 20px 0; }" +
            ".verification-button:hover { background: #218838; }" +
            ".code-box { background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; padding: 15px; margin: 20px 0; text-align: center; }" +
            ".code { font-size: 24px; font-weight: bold; color: #007bff; letter-spacing: 2px; }" +
            ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6; font-size: 14px; color: #666; text-align: center; }" +
            ".warning { background: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 10px; border-radius: 5px; margin: 20px 0; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='container'>" +
            "<div class='header'>" +
            "<div class='logo'>Zaphira</div>" +
            "<h1 class='title'>Vérification de votre email</h1>" +
            "</div>" +
            "<p class='message'>" +
            "Bienvenue sur Zaphira ! Pour finaliser votre inscription et activer votre compte, " +
            "veuillez vérifier votre adresse email en cliquant sur le bouton ci-dessous :" +
            "</p>" +
            "<div style='text-align: center;'>" +
            "<a href='%s' class='verification-button'>Vérifier mon email</a>" +
            "</div>" +
            "<div class='warning'>" +
            "<strong>Important :</strong> Ce lien expirera dans %d minutes. Si le bouton ne fonctionne pas, " +
            "vous pouvez copier et coller ce code dans l'application :" +
            "</div>" +
            "<div class='code-box'>" +
            "<div class='code'>%s</div>" +
            "</div>" +
            "<p class='message'>" +
            "Si vous n'avez pas créé de compte sur Zaphira, ignorez simplement cet email." +
            "</p>" +
            "<div class='footer'>" +
            "Cordialement,<br>" +
            "L'équipe Zaphira<br>" +
            "<small>Si vous avez des questions, contactez-nous à support@zaphira.com</small>" +
            "</div>" +
            "</div>" +
            "</body>" +
            "</html>",
            verificationUrl, OTP_EXPIRY_MINUTES, code
        );

        // Corps texte alternatif
        String textBody = String.format(
            "Bienvenue sur Zaphira !\n\n" +
            "Pour vérifier votre email, cliquez sur ce lien :\n%s\n\n" +
            "Ou utilisez ce code de vérification : %s\n\n" +
            "Ce code expirera dans %d minutes.\n\n" +
            "Si vous n'avez pas créé de compte, ignorez cet email.\n\n" +
            "L'équipe Zaphira",
            verificationUrl, code, OTP_EXPIRY_MINUTES
        );

        // Envoyer l'email HTML
        emailService.sendHtmlEmail(email, subject, htmlBody);
    }

    /**
     * Envoie un email de vérification OTP
     */
    private void sendOtpEmail(String email, String code) {
        String subject = "Code de vérification Zaphira";

        String htmlBody = String.format(
            "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
            "<div style='background-color: #f8f9fa; padding: 20px; text-align: center;'><h1 style='color: #333; margin: 0;'>Zaphira</h1></div>" +
            "<div style='padding: 30px; background-color: white;'><h2 style='color: #333;'>Code de vérification</h2>" +
            "<p>Bonjour,</p><p>Voici votre code de vérification pour accéder à votre compte Zaphira :</p>" +
            "<div style='background-color: #f8f9fa; border: 1px solid #dee2e6; padding: 20px; text-align: center; margin: 20px 0;'>" +
            "<h1 style='color: #007bff; font-size: 32px; margin: 0; letter-spacing: 5px;'>%s</h1></div>" +
            "<p><strong>Ce code expirera dans %d minutes.</strong></p>" +
            "<p>Si vous n'avez pas demandé ce code, veuillez ignorer cet email.</p>" +
            "<p>Cordialement,<br>L'équipe Zaphira</p></div>" +
            "<div style='background-color: #343a40; color: white; padding: 20px; text-align: center;'>" +
            "<p style='margin: 0;'>&copy; 2024 Zaphira. Tous droits réservés.</p></div></body></html>",
            code, OTP_EXPIRY_MINUTES
        );

        emailService.sendHtmlEmail(email, subject, htmlBody);
        log.info("OTP email sent to: {}", maskEmail(email));
    }

    /**
     * Envoie un message OTP via Telegram
     */
    private void sendOtpByTelegram(String email, String code) {
        // Pour Telegram, nous utilisons l'email comme identifiant temporaire
        // En production, il faudrait mapper l'email vers un chatId Telegram
        String chatId = defaultChatId; // Utilise le chat ID par défaut configuré

        String message = String.format(
            "🔐 Code de vérification Zaphira\n\n" +
            "Votre code : %s\n\n" +
            "Ce code expirera dans %d minutes.\n\n" +
            "Si vous n'avez pas demandé ce code, ignorez ce message.",
            code, OTP_EXPIRY_MINUTES
        );

        log.info("Sending OTP {} to Telegram chat {} for email {}", code, chatId, maskEmail(email));

        // Envoyer le message OTP via Telegram
        telegramService.sendOtpMessage(chatId, code);
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

    private void sendVerificationByTelegram(String email, String code) {
        // Pour Telegram, nous utilisons l'email comme identifiant temporaire
        // En production, il faudrait mapper l'email vers un chatId Telegram
        String chatId = defaultChatId; // Utilise le chat ID par défaut configuré

        log.info("Sending OTP {} to Telegram chat {} for email {}", code, chatId, maskEmail(email));

        // Envoyer le message OTP via Telegram
        telegramService.sendOtpMessage(chatId, code);
    }
}