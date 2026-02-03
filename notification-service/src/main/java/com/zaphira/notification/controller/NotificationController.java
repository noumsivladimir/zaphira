package com.zaphira.notification.controller;

import com.zaphira.notification.dto.VerifyOtpRequest;
import com.zaphira.notification.service.EmailService;
import com.zaphira.notification.service.UserServiceClient;
import com.zaphira.notification.service.TransactionServiceClient;
import com.zaphira.notification.service.VerificationService;
import com.zaphira.notification.service.NotificationEventPublisher;
import com.zaphira.notification.service.SmsService;
import com.zaphira.notification.repository.VerificationTokenRepository;

import com.zaphira.common.dto.TransactionDTO;
import com.zaphira.common.model.enums.AccountStatus;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.Map;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final EmailService emailService;
    private final UserServiceClient userServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final VerificationService verificationService;
    private final Optional<NotificationEventPublisher> notificationEventPublisher;
    private final SmsService smsService;
    private final VerificationTokenRepository tokenRepository;

    /**
     * Envoie une notification de transaction par email
     */
    @PostMapping("/send/transaction/{transactionId}")
    public ResponseEntity<?> sendTransactionNotification(@PathVariable Long transactionId) {
        try {
            log.info("Envoi email transaction: {}", transactionId);

            TransactionDTO transaction = transactionServiceClient.getTransactionById(transactionId);
            if (transaction == null) {
                return ResponseEntity.status(404).body("{\"error\":\"Transaction not found\"}");
            }

            sendTransactionNotificationToUsers(transaction);
            return ResponseEntity.ok("{\"status\":\"transaction_email_sent\"}");

        } catch (Exception e) {
            log.error("Erreur notification transaction {}", transactionId, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Envoie un code de vérification par email
     */
    @PostMapping("/send/verification/{userId}")
    public ResponseEntity<?> sendVerificationCode(@PathVariable Long userId) {
        try {
            log.info("Envoi email code de vérification pour user {}", userId);

            String code = verificationService.generateAndSaveCode(userId);

            // Récupération correcte de l'email via UserNotificationInfo
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
            String email = (info != null) ? info.getEmail() : null;

            if (email == null) {
                return ResponseEntity.status(404).body("{\"error\":\"User email not found\"}");
            }

            String subject = "Code de vérification Zaphira";
            String body = String.format(
                    "Bonjour,\n\nVotre code de vérification est : %s\n\nCe code est valide pendant 10 minutes.\n\nZaphira",
                    code
            );

            emailService.sendEmail(email, subject, body);

            // Publish event for verification email sent
            notificationEventPublisher.ifPresent(publisher -> publisher.publishVerificationEmailSentEvent(userId, email, code, false));

            return ResponseEntity.ok("{\"status\":\"verification_email_sent\"}");

        } catch (Exception e) {
            log.error("Erreur envoi code vérification user {}", userId, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Renvoie un code de vérification par email (pour les utilisateurs existants)
     */
    @PostMapping("/resend/verification/{userId}")
    public ResponseEntity<?> resendVerificationCode(@PathVariable Long userId) {
        try {
            log.info("Renvoi email code de vérification pour user {}", userId);

            // Vérifier si l'utilisateur existe
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
            if (info == null || info.getEmail() == null) {
                return ResponseEntity.status(404).body("{\"error\":\"User not found or email not available\"}");
            }

            String code = verificationService.generateAndSaveCode(userId);

            String subject = "Code de vérification Zaphira - Renvoi";
            String body = String.format(
                    "Bonjour %s,\n\nVotre nouveau code de vérification est : %s\n\nCe code est valide pendant 10 minutes.\n\nSi vous n'avez pas demandé ce code, ignorez ce message.\n\nZaphira",
                    info.getFirstName() != null ? info.getFirstName() : "utilisateur",
                    code
            );

            emailService.sendEmail(info.getEmail(), subject, body);

            // Publish event for verification email resent
            notificationEventPublisher.ifPresent(publisher -> publisher.publishVerificationEmailSentEvent(userId, info.getEmail(), code, true));

            return ResponseEntity.ok("{\"status\":\"verification_email_resent\"}");

        } catch (Exception e) {
            log.error("Erreur renvoi code vérification user {}", userId, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Envoie un code OTP par SMS
     */
    @PostMapping("/send/otp/{userId}")
    public ResponseEntity<?> sendOtpCode(@PathVariable Long userId) {
        try {
            log.info("Génération OTP pour user {}", userId);

            // Récupération des informations utilisateur
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
            if (info == null || info.getPhoneNumber() == null) {
                return ResponseEntity.status(404).body("{\"error\":\"User not found or phone number not available\"}");
            }

            // Vérifier que le compte est en attente de vérification
            if (info.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
                log.warn("Tentative d'envoi OTP pour un compte non en attente de vérification: userId={}, status={}", userId, info.getAccountStatus());
                return ResponseEntity.status(400).body("{\"error\":\"OTP can only be sent for accounts pending verification\"}");
            }

            String code = verificationService.generateOtpForJsonResponse(userId);

            // Publish event for OTP SMS sent (même si on n'envoie pas de SMS, on garde l'événement pour la traçabilité)
            notificationEventPublisher.ifPresent(publisher -> publisher.publishVerificationSmsSentEvent(userId, info.getPhoneNumber(), code, false));

            return ResponseEntity.ok(Map.of(
                "status", "otp_generated",
                "otp", code,
                "expiresIn", "10 minutes",
                "message", "OTP généré avec succès. Utilisez ce code pour vérifier votre compte."
            ));

        } catch (Exception e) {
            log.error("Erreur génération OTP user {}", userId, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Renvoie un code OTP par SMS
     */
    @PostMapping("/resend/otp/{userId}")
    public ResponseEntity<?> resendOtpCode(@PathVariable Long userId) {
        try {
            log.info("Regénération OTP pour user {}", userId);

            // Vérifier si l'utilisateur existe
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
            if (info == null || info.getPhoneNumber() == null) {
                return ResponseEntity.status(404).body("{\"error\":\"User not found or phone number not available\"}");
            }

            String code = verificationService.generateOtpForJsonResponse(userId);

            // Publish event for OTP SMS resent (même si on n'envoie pas de SMS, on garde l'événement pour la traçabilité)
            notificationEventPublisher.ifPresent(publisher -> publisher.publishVerificationSmsSentEvent(userId, info.getPhoneNumber(), code, true));

            return ResponseEntity.ok(Map.of(
                "status", "otp_regenerated",
                "otp", code,
                "expiresIn", "10 minutes",
                "message", "Nouveau OTP généré avec succès. Utilisez ce code pour vérifier votre compte."
            ));

        } catch (Exception e) {
            log.error("Erreur regénération OTP user {}", userId, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Envoie un OTP via SMS en utilisant les données fournies dans le payload
     */
    @PostMapping("/send/otp")
    public ResponseEntity<?> sendOtpViaPayload(@RequestBody Map<String, Object> payload) {
        try {
            String phoneNumber = (String) payload.get("phoneNumber");
            String otp = (String) payload.get("otp");
            Integer expiresInMinutes = (Integer) payload.get("expiresInMinutes");

            if (phoneNumber == null || otp == null) {
                return ResponseEntity.status(400).body("{\"error\":\"phoneNumber and otp are required\"}");
            }

            // Validation du format du numéro de téléphone
            if (!phoneNumber.startsWith("+")) {
                return ResponseEntity.status(400).body("{\"error\":\"phoneNumber must be in international format starting with +\"}");
            }

            log.info("Sending OTP via SMS to phone: {}", maskPhoneNumber(phoneNumber));

            String message = String.format("Zaphira - Votre code de vérification est: %s. Ce code expire dans %d minutes.",
                    otp, expiresInMinutes != null ? expiresInMinutes : 5);

            smsService.sendSms(phoneNumber, message);

            return ResponseEntity.ok(Map.of(
                "status", "otp_sent",
                "message", "OTP envoyé par SMS avec succès"
            ));

        } catch (Exception e) {
            log.error("Erreur envoi OTP via payload", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Vérifie un code OTP
     */
    @PostMapping("/verify/otp")
    public ResponseEntity<?> verifyOtpCode(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            log.info("Vérification OTP pour user {}", request.getUserId());

            boolean isValid = verificationService.verifyCode(request.getUserId(), request.getCode());

            if (isValid) {
                return ResponseEntity.ok(Map.of(
                    "status", "otp_verified",
                    "message", "OTP vérifié avec succès"
                ));
            } else {
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Invalid or expired OTP code",
                    "message", "Le code OTP est invalide ou a expiré"
                ));
            }

        } catch (Exception e) {
            log.error("Erreur vérification OTP user {}", request.getUserId(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "message", "Erreur lors de la vérification de l'OTP"
            ));
        }
    }

    /**
     * Notifications transaction pour expéditeur et destinataire
     */
    private void sendTransactionNotificationToUsers(TransactionDTO transaction) {

        // Expéditeur
        if (transaction.getSenderWalletNumber() != null) {
            Long senderId = userServiceClient.getUserIdFromWalletNumber(transaction.getSenderWalletNumber());
            sendTransactionEmail(senderId, transaction);
        }

        // Destinataire
        if (transaction.getReceiverWalletNumber() != null) {
            Long receiverId = userServiceClient.getUserIdFromWalletNumber(transaction.getReceiverWalletNumber());
            sendTransactionEmail(receiverId, transaction);
        }
    }

    /**
     * Envoi réel d'un email de transaction
     */
    private void sendTransactionEmail(Long userId, TransactionDTO transaction) {
        if (userId == null) return;

        UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
        if (info == null || info.getEmail() == null) {
            log.warn("Email introuvable pour userId {}", userId);
            return;
        }

        emailService.sendTransactionNotification(
                info.getEmail(),
                transaction.getReference(),
                transaction.getAmount() + " " + transaction.getCurrency(),
                transaction.getStatus()
        );
    }

    /**
     * Statistiques des tokens OTP (pour monitoring)
     */
    @GetMapping("/admin/token-stats")
    public ResponseEntity<?> getTokenStats() {
        try {
            long totalTokens = tokenRepository.count();
            long activeTokens = tokenRepository.countByUsedFalse();
            long expiredTokens = tokenRepository.countByExpiresAtBefore(LocalDateTime.now());
            long usedTokens = tokenRepository.countByUsedTrue();

            var stats = Map.of(
                "totalTokens", totalTokens,
                "activeTokens", activeTokens,
                "expiredTokens", expiredTokens,
                "usedTokens", usedTokens,
                "lastCleanup", LocalDateTime.now()
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur récupération stats tokens: {}", e.getMessage());
            return ResponseEntity.status(500).body("{\"error\":\"Erreur récupération statistiques\"}");
        }
    }

    /**
     * Masque un numéro de téléphone pour les logs (garde seulement les 4 derniers chiffres)
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "*".repeat(Math.max(0, phoneNumber.length() - 4)) + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
