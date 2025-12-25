package com.zaphira.notification.listener;

import com.zaphira.common.event.TransactionCreatedEvent;
//import com.zaphira.notification.service.EmailService;
import com.zaphira.notification.service.SmsService;
import com.zaphira.notification.service.TelegramService;
import com.zaphira.notification.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    //private final EmailService emailService;
    private final SmsService smsService;
    private final TelegramService telegramService;
    private final UserServiceClient userServiceClient;

    @KafkaListener(
            topics = "transaction-created",
            groupId = "notification-service",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Received transaction event: {}", event.getReference());

        try {
            // Récupérer les informations de l'utilisateur
            Long userId = getTransactionUserId(event);
            String chatId = getUserTelegramChatId(userId);
            String phoneNumber = getUserPhoneNumber(userId);

            // Send Telegram notification
            if (chatId != null) {
                // Déterminer le type de transaction
                String transactionType = determineTransactionType(event);
                telegramService.sendTransactionMessage(chatId,
                    event.getReference(),
                    event.getAmount().toString() + " " + event.getCurrency(),
                    transactionType,
                    event.getStatus());
            }

            // Send SMS notification (if phone number available)
            if (phoneNumber != null) {
                smsService.sendTransactionSms(
                    phoneNumber,
                    event.getReference(),
                    event.getAmount().toString(),
                    event.getStatus()
                );
            }

            log.info("Transaction notification processed for: {}", event.getReference());
        } catch (Exception e) {
            log.error("Failed to process transaction notification for: {}", event.getReference(), e);
        }
    }

    // TODO: Implémenter ces méthodes
    private Long getTransactionUserId(TransactionCreatedEvent event) {
        // Pour l'instant, extraire depuis la référence ou ajouter userId à l'événement
        // Supposons que la référence contient l'userId ou faire un appel au transaction-service
        return null; // Temporaire
    }

    private String getUserTelegramChatId(Long userId) {
        if (userId == null) return null;
        return userServiceClient.getUserTelegramChatId(userId);
    }

    private String getUserPhoneNumber(Long userId) {
        if (userId == null) return null;
        return userServiceClient.getUserPhoneNumber(userId);
    }

    private String determineTransactionType(TransactionCreatedEvent event) {
        if (event.getSenderWalletNumber() != null && event.getReceiverWalletNumber() != null) {
            return "TRANSFER";
        } else if (event.getSenderWalletNumber() != null) {
            return "DEBIT";
        } else if (event.getReceiverWalletNumber() != null) {
            return "CREDIT";
        }
        return "TRANSACTION";
    }
}

