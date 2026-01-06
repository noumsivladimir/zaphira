package com.zaphira.notification.listener;

import com.zaphira.common.event.TransactionCreatedEvent;
import com.zaphira.notification.service.EmailService;
import com.zaphira.notification.service.SmsService;
//import com.zaphira.notification.service.TelegramService;
import com.zaphira.notification.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
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
                }
            }

            // Send email notification to receiver if exists
            if (event.getReceiverWalletNumber() != null) {
                Long receiverUserId = userServiceClient.getUserIdFromWalletNumber(event.getReceiverWalletNumber());
                if (receiverUserId != null) {
                    UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(receiverUserId);
                    String email = (info != null) ? info.getEmail() : null;
                    if (email != null) {
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
                        emailService.sendEmail(email, subject, body);
                        log.info("Transaction email sent to receiver: {}", receiverUserId);
                    }
                }
            }

            log.info("Transaction notification processed for: {}", event.getReference());
        } catch (Exception e) {
            log.error("Failed to process transaction notification for: {}", event.getReference(), e);
        }
    }
}

