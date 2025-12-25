package com.zaphira.notification.listener;

import com.zaphira.common.event.UserRegisteredEvent;
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
public class UserEventListener {

    //private final EmailService emailService;
    private final SmsService smsService;
    private final TelegramService telegramService;
    private final UserServiceClient userServiceClient;

    @KafkaListener(
            topics = "user-registered",
            groupId = "notification-service",
            containerFactory = "userKafkaListenerContainerFactory"
    )
    public void handleUserRegistered(UserRegisteredEvent event) {
        // Générer le nom complet
        String fullName = event.getFirstName() + " " + event.getLastName();

        log.info("Received user registered event for phone number: {}", event.getPhoneNumber());

        try {
            // Envoyer notification Telegram si chatId disponible
            String chatId = getUserTelegramChatId(event.getUserId());
            if (chatId != null) {
                telegramService.sendUserWelcomeMessage(chatId, event.getFirstName(), event.getLastName());
                log.info("Welcome Telegram message sent to user: {}", event.getUserId());
            }

            // Si vous voulez toujours envoyer un email et que l'adresse existe
            if (event.getPhoneNumber() != null) {
                // Exemple : on envoie le SMS
                smsService.sendSms(event.getPhoneNumber(),
                        "Hello " + fullName + "! Welcome to Zaphira! Your account has been created.");

                log.info("Welcome SMS sent to: {}", event.getPhoneNumber());
            }
        } catch (Exception e) {
            log.error("Failed to send welcome notification to: {}", event.getPhoneNumber(), e);
        }
    }

    // TODO: Implémenter cette méthode pour récupérer le chatId Telegram de l'utilisateur
    private String getUserTelegramChatId(Long userId) {
        return userServiceClient.getUserTelegramChatId(userId);
    }
}
