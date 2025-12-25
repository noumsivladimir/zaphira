package com.zaphira.notification.listener;

import com.zaphira.common.event.WalletBalanceUpdatedEvent;
import com.zaphira.common.event.WalletCreatedEvent;
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
public class WalletEventListener {

    private final SmsService smsService;
    private final TelegramService telegramService;
    private final UserServiceClient userServiceClient;

    @KafkaListener(
            topics = "wallet-created",
            groupId = "notification-service",
            containerFactory = "walletKafkaListenerContainerFactory"
    )
    public void handleWalletCreated(WalletCreatedEvent event) {
        log.info("Received wallet created event for user: {}", event.getUserId());

        try {
            // Pour l'instant, on suppose que le chatId est stocké quelque part
            // TODO: Récupérer le chatId Telegram de l'utilisateur depuis user-service
            String chatId = getUserTelegramChatId(event.getUserId());

            if (chatId != null) {
                telegramService.sendWalletCreatedMessage(chatId, "WALLET-" + event.getUserId());
            }

            // SMS de confirmation
            String phoneNumber = getUserPhoneNumber(event.getUserId());
            if (phoneNumber != null) {
                smsService.sendSms(phoneNumber,
                    "Votre portefeuille Zaphira a été créé avec succès!");
            }

        } catch (Exception e) {
            log.error("Failed to send wallet creation notification for user: {}", event.getUserId(), e);
        }
    }

    @KafkaListener(
            topics = "wallet-balance-updated",
            groupId = "notification-service",
            containerFactory = "walletBalanceKafkaListenerContainerFactory"
    )
    public void handleWalletBalanceUpdated(WalletBalanceUpdatedEvent event) {
        log.info("Received wallet balance updated event for wallet: {}", event.getWalletNumber());

        try {
            String chatId = getUserTelegramChatId(event.getUserId());
            String phoneNumber = getUserPhoneNumber(event.getUserId());

            if (chatId != null) {
                telegramService.sendBalanceUpdateMessage(chatId,
                    event.getWalletNumber(),
                    event.getNewBalance().toString() + " XAF");
            }

            if (phoneNumber != null) {
                smsService.sendSms(phoneNumber,
                    String.format("Solde mis à jour: %s XAF (Portefeuille: %s)",
                        event.getNewBalance(), event.getWalletNumber()));
            }

        } catch (Exception e) {
            log.error("Failed to send balance update notification for wallet: {}", event.getWalletNumber(), e);
        }
    }

    // TODO: Implémenter ces méthodes pour récupérer les informations utilisateur
    private String getUserTelegramChatId(Long userId) {
        return userServiceClient.getUserTelegramChatId(userId);
    }

    private String getUserPhoneNumber(Long userId) {
        return userServiceClient.getUserPhoneNumber(userId);
    }
}