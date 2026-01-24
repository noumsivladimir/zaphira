package com.zaphira.notification.listener;

import com.zaphira.common.event.UserRegisteredEvent;
import com.zaphira.notification.service.EmailService;
import com.zaphira.notification.service.NotificationEventPublisher;
import com.zaphira.notification.service.SmsService;
import com.zaphira.notification.service.UserServiceClient;
import com.zaphira.notification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;
    private final SmsService smsService;
    private final UserServiceClient userServiceClient;
    private final VerificationService verificationService;
    private final NotificationEventPublisher notificationEventPublisher;

    @KafkaListener(
            topics = "user-registered",
            groupId = "notification-service",
            containerFactory = "userKafkaListenerContainerFactory"
    )
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user registered event for user: {} ({})", event.getUserId(), event.getEmail());

        try {
            // Envoyer email de bienvenue (l'utilisateur a déjà vérifié son email)
            String subject = "Bienvenue sur Zaphira - Votre compte est activé !";
            String body = String.format(
                "Bonjour %s %s,\n\n" +
                "Félicitations ! Votre compte Zaphira a été activé avec succès.\n\n" +
                "Vous pouvez maintenant profiter de tous nos services :\n" +
                "- Transferts d'argent sécurisés\n" +
                "- Gestion de votre portefeuille\n" +
                "- Historique des transactions\n" +
                "- Et bien plus encore...\n\n" +
                "Votre numéro de téléphone : %s\n" +
                "Date d'inscription : %s\n\n" +
                "Pour commencer, connectez-vous à votre application mobile ou web.\n\n" +
                "Si vous avez des questions, n'hésitez pas à nous contacter.\n\n" +
                "Bienvenue dans la communauté Zaphira !\n\n" +
                "L'équipe Zaphira",
                event.getFirstName(), event.getLastName(), 
                event.getPhoneNumber() != null ? event.getPhoneNumber() : "Non spécifié",
                event.getRegisteredAt() != null ? event.getRegisteredAt().toString() : "N/A"
            );

            emailService.sendEmail(event.getEmail(), subject, body);
            log.info("Welcome email sent to activated user: {} at {}", event.getUserId(), event.getEmail());

            // Publier l'événement de notification envoyée (sans code de vérification)
            notificationEventPublisher.publishVerificationEmailSentEvent(
                event.getUserId(),
                event.getEmail(),
                null, // Pas de code de vérification
                false
            );

        } catch (Exception e) {
            log.error("Failed to send welcome email to user: {}", event.getUserId(), e);
            // TODO: Implement retry mechanism or dead letter queue
        }
    }
}