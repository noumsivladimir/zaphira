<<<<<<< HEAD
package com.zaphira.wallet.listener;

import com.zaphira.common.event.UserRegisteredEvent;
import com.zaphira.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listener pour écouter les événements utilisateur depuis Kafka.
 * Crée automatiquement un wallet lorsqu'un utilisateur est enregistré.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final WalletService walletService;

    /**
     * Écoute les événements UserRegisteredEvent depuis Kafka.
     * Crée automatiquement un wallet avec un solde initial de 0.
     * 
     * @param event L'événement UserRegisteredEvent
     * @param acknowledgment Pour confirmer la réception du message
     */
    @KafkaListener(
            topics = "user-registered",
            groupId = "wallet-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserRegistered(
            @Payload UserRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received UserRegisteredEvent for user {}: {}", userId, event);
            
            // Vérifier si le wallet existe déjà (idempotence)
            try {
                walletService.getWalletByUserId(event.getUserId());
                log.warn("Wallet already exists for user {}, skipping creation", event.getUserId());
            } catch (Exception e) {
                // Wallet n'existe pas, on le crée
                log.info("Creating wallet for user {} with currency XOF", event.getUserId());
                walletService.createWallet(event.getUserId());
                log.info("Wallet created successfully for user {}", event.getUserId());
            }
            
            // Confirmer la réception du message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Failed to create wallet for user {}: {}", event.getUserId(), e.getMessage(), e);
            // Optionnel: Envoyer vers une dead letter queue ou retry
            // Pour l'instant, on log juste l'erreur
            // En production, implémenter un mécanisme de retry
        }
    }
}

=======
//package com.zaphira.wallet.listener;
//
//import com.zaphira.common.event.UserRegisteredEvent;
//import com.zaphira.wallet.service.WalletServiceDeFaible;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//
///**
// * Listener pour écouter les événements utilisateur depuis Kafka.
// * Crée automatiquement un wallet lorsqu'un utilisateur est enregistré.
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class UserEventListener {
//
//    private final WalletServiceDeFaible walletService;
//
//    /**
//     * Écoute les événements UserRegisteredEvent depuis Kafka.
//     * Crée automatiquement un wallet avec un solde initial de 0.
//     *
//     * @param event L'événement UserRegisteredEvent
//     * @param acknowledgment Pour confirmer la réception du message
//     */
//    @KafkaListener(
//            topics = "user-registered",
//            groupId = "wallet-service-group",
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void handleUserRegistered(
//            @Payload UserRegisteredEvent event,
//            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
//            Acknowledgment acknowledgment) {
//
//        try {
//            log.info("Received UserRegisteredEvent for user {}: {}", userId, event);
//
//            // Vérifier si le wallet existe déjà (idempotence)
//            try {
//                walletService.getWalletByUserId(event.getUserId());
//                log.warn("Wallet already exists for user {}, skipping creation", event.getUserId());
//            } catch (Exception e) {
//                // Wallet n'existe pas, on le crée
//                log.info("Creating wallet for user {} with currency XOF", event.getUserId());
//                walletService.createWallet(event.getUserId());
//                log.info("Wallet created successfully for user {}", event.getUserId());
//            }
//
//            // Confirmer la réception du message
//            if (acknowledgment != null) {
//                acknowledgment.acknowledge();
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to create wallet for user {}: {}", event.getUserId(), e.getMessage(), e);
//            // Optionnel: Envoyer vers une dead letter queue ou retry
//            // Pour l'instant, on log juste l'erreur
//            // En production, implémenter un mécanisme de retry
//        }
//    }
//}
//
>>>>>>> origin/services/wallet
