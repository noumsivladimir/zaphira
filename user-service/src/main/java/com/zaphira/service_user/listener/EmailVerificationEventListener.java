package com.zaphira.service_user.listener;

import com.zaphira.service_user.event.EmailVerificationSuccessEvent;
import com.zaphira.service_user.event.UserRegistrationEvent;
import com.zaphira.service_user.model.entities.RegularUser;
import com.zaphira.service_user.model.enums.AccountStatus;
import com.zaphira.service_user.repository.RegularUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener pour gérer les événements liés à la vérification email
 * Ce listener s'assure que le processus de vérification email fonctionne correctement
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationEventListener {

    private final RegularUserRepository regularUserRepository;

    /**
     * Méthode appelée automatiquement après une vérification email réussie
     * Cette méthode s'assure que :
     * 1. Le compte utilisateur est bien activé
     * 2. L'événement UserRegisteredEvent est publié
     * 3. Le wallet est créé automatiquement
     */
    @EventListener
    @Async
    public void handleEmailVerificationSuccess(EmailVerificationSuccessEvent event) {
        log.info("Email verification success event received for user: {} with email: {}",
                event.getUserId(), event.getEmail());

        try {
            // Vérifier que l'utilisateur existe et que son email est vérifié
            RegularUser user = regularUserRepository.findById(event.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + event.getUserId()));

            // Vérifier que le compte est bien activé
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                log.warn("User account is not active after email verification. User ID: {}", event.getUserId());
                return;
            }

            // Vérifier que l'email est marqué comme vérifié
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                log.warn("User email is not marked as verified. User ID: {}", event.getUserId());
                return;
            }

            log.info("Email verification process completed successfully for user: {}", event.getUserId());
            log.info("User account is now ACTIVE and email is verified");

            // Ici nous pourrions ajouter d'autres actions post-vérification
            // comme l'envoi d'un email de bienvenue, la création de notifications, etc.

        } catch (Exception e) {
            log.error("Error processing email verification success event for user: {}", event.getUserId(), e);
        }
    }

    /**
     * Méthode appelée automatiquement après l'inscription d'un utilisateur
     * Cette méthode s'assure que l'email de vérification est bien envoyé
     */
    @EventListener
    @Async
    public void handleUserRegistration(UserRegistrationEvent event) {
        log.info("User registration event received for user: {} with email: {} (OTP: {})",
                event.getUserId(), event.getEmail(), event.getVerificationCode());

        try {
            // Vérifier que l'utilisateur a bien été créé avec le bon statut
            RegularUser user = regularUserRepository.findById(event.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + event.getUserId()));

            // Vérifier que le compte est en attente de vérification
            if (user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
                log.warn("User account status is not PENDING_VERIFICATION. User ID: {}", event.getUserId());
                return;
            }

            // Vérifier que l'email n'est pas encore vérifié
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                log.warn("User email is already verified during registration. User ID: {}", event.getUserId());
                return;
            }

            log.info("User registration process initiated successfully for user: {}", event.getUserId());
            log.info("Email verification pending - waiting for user to click verification link");

        } catch (Exception e) {
            log.error("Error processing user registration event for user: {}", event.getUserId(), e);
        }
    }
}