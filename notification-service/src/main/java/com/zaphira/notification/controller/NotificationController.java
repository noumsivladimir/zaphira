package com.zaphira.notification.controller;

import com.zaphira.notification.dto.VerifyOtpRequest;
import com.zaphira.notification.dto.TransactionStatusNotificationRequest;
import com.zaphira.notification.dto.DisputeUpdateNotificationRequest;
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

    // ========== LOT 2: Transaction Status & Dispute Notifications ==========

    /**
     * LOT 2: Send notification for transaction status change
     * Notifies both sender and receiver when transaction status changes
     */
    @PostMapping("/send/transaction-status")
    public ResponseEntity<?> sendTransactionStatusNotification(
            @Valid @RequestBody TransactionStatusNotificationRequest request) {
        try {
            log.info("Sending transaction status notification: {} -> {} for transaction {}", 
                    request.getPreviousStatus(), request.getNewStatus(), request.getTransactionReference());

            int notificationsSent = 0;

            // Notify sender
            if (request.getSenderWalletNumber() != null) {
                Long senderId = userServiceClient.getUserIdFromWalletNumber(request.getSenderWalletNumber());
                if (senderId != null) {
                    sendStatusChangeEmail(senderId, request, true);
                    notificationsSent++;
                }
            }

            // Notify receiver
            if (request.getReceiverWalletNumber() != null) {
                Long receiverId = userServiceClient.getUserIdFromWalletNumber(request.getReceiverWalletNumber());
                if (receiverId != null) {
                    sendStatusChangeEmail(receiverId, request, false);
                    notificationsSent++;
                }
            }

            log.info("✅ Transaction status notifications sent: {} for transaction {}", 
                    notificationsSent, request.getTransactionReference());

            return ResponseEntity.ok(Map.of(
                "status", "notifications_sent",
                "notificationsSent", notificationsSent,
                "transactionReference", request.getTransactionReference(),
                "newStatus", request.getNewStatus()
            ));

        } catch (Exception e) {
            log.error("❌ Error sending transaction status notification for {}: {}", 
                    request.getTransactionReference(), e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "message", "Failed to send transaction status notification"
            ));
        }
    }

    /**
     * LOT 2: Send notification for dispute updates
     * Notifies initiator and/or respondent based on update type
     */
    @PostMapping("/send/dispute-update")
    public ResponseEntity<?> sendDisputeUpdateNotification(
            @Valid @RequestBody DisputeUpdateNotificationRequest request) {
        try {
            log.info("Sending dispute update notification: type={}, status={} for dispute {}", 
                    request.getUpdateType(), request.getNewStatus(), request.getDisputeReference());

            int notificationsSent = 0;

            // Determine who to notify based on update type
            boolean notifyInitiator = shouldNotifyInitiator(request.getUpdateType());
            boolean notifyRespondent = shouldNotifyRespondent(request.getUpdateType());

            // Notify initiator
            if (notifyInitiator && request.getInitiatorUserId() != null) {
                sendDisputeEmail(request.getInitiatorUserId(), request, true);
                notificationsSent++;
            }

            // Notify respondent
            if (notifyRespondent && request.getRespondentUserId() != null) {
                sendDisputeEmail(request.getRespondentUserId(), request, false);
                notificationsSent++;
            }

            log.info("✅ Dispute update notifications sent: {} for dispute {}", 
                    notificationsSent, request.getDisputeReference());

            return ResponseEntity.ok(Map.of(
                "status", "notifications_sent",
                "notificationsSent", notificationsSent,
                "disputeReference", request.getDisputeReference(),
                "updateType", request.getUpdateType()
            ));

        } catch (Exception e) {
            log.error("❌ Error sending dispute update notification for {}: {}", 
                    request.getDisputeReference(), e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "message", "Failed to send dispute update notification"
            ));
        }
    }

    /**
     * Send email for transaction status change
     */
    private void sendStatusChangeEmail(Long userId, TransactionStatusNotificationRequest request, boolean isSender) {
        UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
        if (info == null || info.getEmail() == null) {
            log.warn("Email not found for userId {} - skipping notification", userId);
            return;
        }

        String role = isSender ? "envoyée" : "reçue";
        String subject = String.format("Zaphira - Mise à jour de votre transaction %s", request.getTransactionReference());
        
        StringBuilder body = new StringBuilder();
        body.append(String.format("Bonjour %s,\n\n", info.getFirstName() != null ? info.getFirstName() : ""));
        body.append(String.format("Votre transaction %s a changé de statut.\n\n", role));
        body.append(String.format("📋 Référence: %s\n", request.getTransactionReference()));
        body.append(String.format("💰 Montant: %s %s\n", request.getAmount(), request.getCurrency()));
        body.append(String.format("📊 Ancien statut: %s\n", request.getPreviousStatus()));
        body.append(String.format("✅ Nouveau statut: %s\n", request.getNewStatus()));
        
        if (request.getReason() != null && !request.getReason().isEmpty()) {
            body.append(String.format("\n📝 Raison: %s\n", request.getReason()));
        }
        
        body.append("\nCordialement,\nL'équipe Zaphira");

        emailService.sendEmail(info.getEmail(), subject, body.toString());
        log.debug("Transaction status email sent to {} for transaction {}", info.getEmail(), request.getTransactionReference());
    }

    /**
     * Send email for dispute update
     */
    private void sendDisputeEmail(Long userId, DisputeUpdateNotificationRequest request, boolean isInitiator) {
        UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
        if (info == null || info.getEmail() == null) {
            log.warn("Email not found for userId {} - skipping dispute notification", userId);
            return;
        }

        String subject = String.format("Zaphira - Mise à jour de votre litige %s", request.getDisputeReference());
        
        StringBuilder body = new StringBuilder();
        body.append(String.format("Bonjour %s,\n\n", info.getFirstName() != null ? info.getFirstName() : ""));
        body.append(String.format("Une mise à jour a été effectuée sur votre litige.\n\n"));
        body.append(String.format("📋 Référence litige: %s\n", request.getDisputeReference()));
        body.append(String.format("🔗 Transaction concernée: %s\n", request.getTransactionReference()));
        body.append(String.format("📊 Type de mise à jour: %s\n", getUpdateTypeLabel(request.getUpdateType())));
        
        if (request.getNewStatus() != null) {
            body.append(String.format("✅ Nouveau statut: %s\n", request.getNewStatus()));
        }
        
        if (request.getResolution() != null) {
            body.append(String.format("⚖️ Résolution: %s\n", getResolutionLabel(request.getResolution())));
        }
        
        if (request.getMessage() != null && !request.getMessage().isEmpty()) {
            body.append(String.format("\n📝 Message: %s\n", request.getMessage()));
        }
        
        body.append("\nPour plus de détails, connectez-vous à votre espace Zaphira.");
        body.append("\n\nCordialement,\nL'équipe Zaphira");

        emailService.sendEmail(info.getEmail(), subject, body.toString());
        log.debug("Dispute update email sent to {} for dispute {}", info.getEmail(), request.getDisputeReference());
    }

    /**
     * Determine if initiator should be notified based on update type
     */
    private boolean shouldNotifyInitiator(String updateType) {
        if (updateType == null) return true;
        // Initiator should be notified for all updates except when they submit evidence themselves
        return !updateType.equals("EVIDENCE_SUBMITTED_BY_INITIATOR");
    }

    /**
     * Determine if respondent should be notified based on update type
     */
    private boolean shouldNotifyRespondent(String updateType) {
        if (updateType == null) return true;
        // Respondent should be notified for: STATUS_CHANGE, EVIDENCE_SUBMITTED, ESCALATED, RESOLVED
        return updateType.equals("STATUS_CHANGE") || 
               updateType.equals("EVIDENCE_SUBMITTED") || 
               updateType.equals("ESCALATED") || 
               updateType.equals("RESOLVED");
    }

    /**
     * Get human-readable label for update type
     */
    private String getUpdateTypeLabel(String updateType) {
        if (updateType == null) return "Mise à jour";
        return switch (updateType) {
            case "STATUS_CHANGE" -> "Changement de statut";
            case "EVIDENCE_SUBMITTED" -> "Preuve soumise";
            case "RESPONSE_RECEIVED" -> "Réponse reçue";
            case "ESCALATED" -> "Escalade";
            case "RESOLVED" -> "Résolution";
            default -> updateType;
        };
    }

    /**
     * Get human-readable label for resolution
     */
    private String getResolutionLabel(String resolution) {
        if (resolution == null) return "";
        return switch (resolution) {
            case "REFUNDED" -> "Remboursement complet";
            case "REJECTED" -> "Litige rejeté";
            case "PARTIAL_REFUND" -> "Remboursement partiel";
            default -> resolution;
        };
    }
}
