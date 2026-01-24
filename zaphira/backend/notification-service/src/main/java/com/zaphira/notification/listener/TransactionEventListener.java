package com.zaphira.notification.listener;

import com.zaphira.common.event.TransactionCompletedEvent;
import com.zaphira.common.event.TransactionCreatedEvent;
import com.zaphira.notification.service.EmailService;
import com.zaphira.notification.service.SmsService;
//import com.zaphira.notification.service.TelegramService;
import com.zaphira.notification.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class TransactionEventListener {

    private final EmailService emailService;
    private final SmsService smsService;
    //private final TelegramService telegramService;
    private final UserServiceClient userServiceClient;

    @KafkaListener(
            topics = "transaction-created",
            groupId = "notification-service",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Received transaction event: {}", event.getReference());

        try {
            // Send email notification to sender if exists
            if (event.getSenderWalletNumber() != null) {
                Long senderUserId = userServiceClient.getUserIdFromWalletNumber(event.getSenderWalletNumber());
                if (senderUserId != null) {
                    UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(senderUserId);
                    String email = (info != null) ? info.getEmail() : null;
                    if (email != null) {
                        String type = event.getReceiverWalletNumber() != null ? "Transfert" : "Débit";
                        String subject = "Notification de transaction - " + event.getReference();
                        String body = String.format(
                            "Bonjour,\n\n" +
                            "Votre transaction %s a été %s.\n\n" +
                            "Référence: %s\n" +
                            "Montant: %s %s\n" +
                            "Statut: %s\n\n" +
                            "Merci d'utiliser Zaphira.\n\n" +
                            "L'équipe Zaphira",
                            type, event.getStatus().toLowerCase(),
                            event.getReference(),
                            event.getAmount(), event.getCurrency(),
                            event.getStatus()
                        );
                        emailService.sendEmail(email, subject, body);
                        log.info("Transaction email sent to sender: {}", senderUserId);
                    }
                    // Send SMS to sender
                    String phoneNumber = userServiceClient.getUserPhoneNumber(senderUserId);
                    if (phoneNumber != null) {
                        String smsMessage = String.format(
                            "Bonjour,\n\n" +
                            "Votre transaction de débit a été initiée.\n\n" +
                            "Référence: %s\n" +
                            "Montant: %s %s\n" +
                            "Statut: %s\n\n" +
                            "Merci d'utiliser Zaphira.",
                            event.getReference(),
                            event.getAmount(), event.getCurrency(),
                            event.getStatus()
                        );
                        smsService.sendSms(phoneNumber, smsMessage);
                        log.info("Transaction SMS sent to sender: {}", senderUserId);
                    }
                }
            }

            // Send email notification to receiver if exists
            if (event.getReceiverWalletNumber() != null) {
                Long receiverUserId = userServiceClient.getUserIdFromWalletNumber(event.getReceiverWalletNumber());
                if (receiverUserId != null) {
                    UserServiceClient.UserNotificationInfo receiverInfo = userServiceClient.getUserNotificationInfo(receiverUserId);
                    String receiverEmail = (receiverInfo != null) ? receiverInfo.getEmail() : null;
                    if (receiverEmail != null) {
                        String type = event.getSenderWalletNumber() != null ? "Transfert" : "Crédit";
                        String subject = "Notification de transaction - " + event.getReference();
                        String body = String.format(
                            "Bonjour,\n\n" +
                            "Votre transaction %s a été %s.\n\n" +
                            "Référence: %s\n" +
                            "Montant: %s %s\n" +
                            "Statut: %s\n\n" +
                            "Merci d'utiliser Zaphira.\n\n" +
                            "L'équipe Zaphira",
                            type, event.getStatus().toLowerCase(),
                            event.getReference(),
                            event.getAmount(), event.getCurrency(),
                            event.getStatus()
                        );
                        emailService.sendEmail(receiverEmail, subject, body);
                        log.info("Transaction email sent to receiver: {}", receiverUserId);
                    }
                    // Send SMS to receiver
                    String receiverPhoneNumber = userServiceClient.getUserPhoneNumber(receiverUserId);
                    if (receiverPhoneNumber != null) {
                        String smsMessage = String.format(
                            "Bonjour,\n\n" +
                            "Votre transaction de crédit a été initiée.\n\n" +
                            "Référence: %s\n" +
                            "Montant: %s %s\n" +
                            "Statut: %s\n\n" +
                            "Merci d'utiliser Zaphira.",
                            event.getReference(),
                            event.getAmount(), event.getCurrency(),
                            event.getStatus()
                        );
                        smsService.sendSms(receiverPhoneNumber, smsMessage);
                        log.info("Transaction SMS sent to receiver: {}", receiverUserId);
                    }
                }
            }

            log.info("Transaction notification processed for: {}", event.getReference());
        } catch (Exception e) {
            log.error("Failed to process transaction notification for: {}", event.getReference(), e);
        }
    }

    @KafkaListener(
            topics = "transaction-completed",
            groupId = "notification-service",
            containerFactory = "transactionCompletedKafkaListenerContainerFactory"
    )
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Received transaction completed event: {}", event.getReference());

        try {
            // Send SMS notification to sender if exists
            if (event.getSenderWalletNumber() != null) {
                Long senderUserId = userServiceClient.getUserIdFromWalletNumber(event.getSenderWalletNumber());
                if (senderUserId != null) {
                    String phoneNumber = userServiceClient.getUserPhoneNumber(senderUserId);
                    if (phoneNumber != null) {
                        String message = String.format(
                            "Bonjour,\n\n" +
                            "Votre transaction de débit a été complétée.\n\n" +
                            "Référence: %s\n" +
                            "Montant: %s %s\n" +
                            "Statut: %s\n\n" +
                            "Merci d'utiliser Zaphira.",
                            event.getReference(),
                            event.getAmount(), event.getCurrency(),
                            event.getStatus()
                        );
                        smsService.sendSms(phoneNumber, message);
                        log.info("Transaction SMS sent to sender: {}", senderUserId);
                    }
                }
            }

            // Send SMS notification to receiver if exists
            if (event.getReceiverWalletNumber() != null) {
                Long receiverUserId = userServiceClient.getUserIdFromWalletNumber(event.getReceiverWalletNumber());
                if (receiverUserId != null) {
                    String phoneNumber = userServiceClient.getUserPhoneNumber(receiverUserId);
                    if (phoneNumber != null) {
                        String message = String.format(
                            "Bonjour,\n\n" +
                            "Votre transaction de crédit a été complétée.\n\n" +
                            "Référence: %s\n" +
                            "Montant: %s %s\n" +
                            "Statut: %s\n\n" +
                            "Merci d'utiliser Zaphira.",
                            event.getReference(),
                            event.getAmount(), event.getCurrency(),
                            event.getStatus()
                        );
                        smsService.sendSms(phoneNumber, message);
                        log.info("Transaction SMS sent to receiver: {}", receiverUserId);
                    }
                }
            }

            log.info("Transaction completed SMS notification processed for: {}", event.getReference());
        } catch (Exception e) {
            log.error("Failed to process transaction completed SMS notification for: {}", event.getReference(), e);
        }
    }
}

