package com.zaphira.notification.listener;

import com.zaphira.common.event.WalletBalanceUpdatedEvent;
import com.zaphira.common.event.WalletCreatedEvent;
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
public class WalletEventListener {

    private final EmailService emailService;
    private final SmsService smsService;
    //private final TelegramService telegramService;
    private final UserServiceClient userServiceClient;

    @KafkaListener(
            topics = "wallet-created",
            groupId = "notification-service",
            containerFactory = "walletKafkaListenerContainerFactory"
    )
    public void handleWalletCreated(WalletCreatedEvent event) {
        log.info("Received wallet created event for user: {}", event.getUserId());

        try {
            // Récupérer les informations de notification de l'utilisateur
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(event.getUserId());
            String email = (info != null) ? info.getEmail() : null;
            String phoneNumber = getUserPhoneNumber(event.getUserId());

            if (email != null) {
                // Envoyer email de création de portefeuille
                String subject = "Portefeuille Zaphira créé";
                String body = String.format(
                    "Bonjour,\n\n" +
                    "Votre portefeuille Zaphira a été créé avec succès.\n" +
                    "Numéro de portefeuille: WALLET-%s\n\n" +
                    "Vous pouvez maintenant effectuer des transactions.\n\n" +
                    "L'équipe Zaphira",
                    event.getUserId()
                );
                emailService.sendEmail(email, subject, body);
                log.info("Wallet creation email sent to user: {}", event.getUserId());
            }

            // SMS de confirmation
            if (phoneNumber != null) {
                smsService.sendSms(phoneNumber,
                    "Votre portefeuille Zaphira a été créé avec succès!");
                log.info("Wallet creation SMS sent to: {}", phoneNumber);
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
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(event.getUserId());
            String email = (info != null) ? info.getEmail() : null;
            String phoneNumber = getUserPhoneNumber(event.getUserId());

            if (email != null) {
                // Envoyer email de mise à jour du solde
                String subject = "Mise à jour du solde Zaphira";
                String body = String.format(
                    "Bonjour,\n\n" +
                    "Votre solde a été mis à jour.\n\n" +
                    "Portefeuille: %s\n" +
                    "Nouveau solde: %s XAF\n\n" +
                    "L'équipe Zaphira",
                    event.getWalletNumber(),
                    event.getNewBalance()
                );
                emailService.sendEmail(email, subject, body);
                log.info("Balance update email sent to user: {}", event.getUserId());
            }

            if (phoneNumber != null) {
                smsService.sendSms(phoneNumber,
                    String.format("Solde mis à jour: %s XAF (Portefeuille: %s)",
                        event.getNewBalance(), event.getWalletNumber()));
                log.info("Balance update SMS sent to: {}", phoneNumber);
            }

        } catch (Exception e) {
            log.error("Failed to send balance update notification for wallet: {}", event.getWalletNumber(), e);
        }
    }

    private String getUserPhoneNumber(Long userId) {
        return userServiceClient.getUserPhoneNumber(userId);
    }
}