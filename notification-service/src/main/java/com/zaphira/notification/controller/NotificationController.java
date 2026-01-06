package com.zaphira.notification.controller;

import com.zaphira.common.dto.TransactionDTO;
import com.zaphira.notification.service.EmailService;
import com.zaphira.notification.service.NotificationEventPublisher;
import com.zaphira.notification.service.TransactionServiceClient;
import com.zaphira.notification.service.UserServiceClient;
import com.zaphira.notification.service.VerificationService;
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
    private final NotificationEventPublisher notificationEventPublisher;

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
            notificationEventPublisher.publishVerificationEmailSentEvent(userId, email, code, false);

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
            notificationEventPublisher.publishVerificationEmailSentEvent(userId, info.getEmail(), code, true);

            return ResponseEntity.ok("{\"status\":\"verification_email_resent\"}");

        } catch (Exception e) {
            log.error("Erreur renvoi code vérification user {}", userId, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
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
}
