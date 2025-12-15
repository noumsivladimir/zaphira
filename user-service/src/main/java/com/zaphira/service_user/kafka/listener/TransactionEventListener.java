package com.zaphira.service_user.kafka.listener;

import com.zaphira.common.event.TransactionCompletedEvent;
import com.zaphira.service_user.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka listener pour consommer les événements TransactionCompletedEvent.
 * 
 * Responsabilités:
 * - Écoute le topic "transaction-completed"
 * - Reçoit les événements de completion de transaction
 * - Déclenche la mise à jour du solde utilisateur
 * - Garantit l'idempotence (pas de double débit)
 * 
 * Topic: transaction-completed
 * Groupe de consommation: user-service-group
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventListener {

    private final UserService userService;

    /**
     * Consomme les événements TransactionCompletedEvent et met à jour le solde utilisateur.
     *
     * @param event L'événement de transaction complétée
     * @param partition La partition Kafka du message
     * @param offset L'offset du message
     * @param acknowledgment Reconnaissance manuelle du message
     */
    @KafkaListener(
        topics = "transaction-completed",
        groupId = "user-service-group",
        containerFactory = "transactionCompletedKafkaListenerContainerFactory"
    )
    public void onTransactionCompleted(
            @Payload TransactionCompletedEvent event,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received TransactionCompletedEvent: transactionId={}, initiatorUserId={}, amount={}",
                    event.getTransactionId(), event.getInitiatorUserId(), event.getAmount());
            
            // Vérifier que c'est une transaction COMPLETED
            if (!"COMPLETED".equals(event.getStatus())) {
                log.warn("Ignoring transaction event with status {}, expected COMPLETED. transactionId={}",
                        event.getStatus(), event.getTransactionId());
                acknowledgeMessage(acknowledgment);
                return;
            }
            
            // Vérifier que nous avons un ID utilisateur valide
            if (event.getInitiatorUserId() == null) {
                log.warn("Received TransactionCompletedEvent with null initiatorUserId. transactionId={}",
                        event.getTransactionId());
                acknowledgeMessage(acknowledgment);
                return;
            }
            
            // Mettre à jour le solde utilisateur de manière idempotente
            // La clé d'idempotence est: userId + transactionId
            userService.updateBalanceFromTransaction(event);
            
            // Acknowledger le message après traitement réussi
            acknowledgeMessage(acknowledgment);
            
            log.info("Successfully processed TransactionCompletedEvent: transactionId={}, initiatorUserId={}",
                    event.getTransactionId(), event.getInitiatorUserId());
            
        } catch (Exception e) {
            log.error("Error processing TransactionCompletedEvent: transactionId={}, initiatorUserId={}. Error: {}",
                    event.getTransactionId(),
                    event.getInitiatorUserId(),
                    e.getMessage(),
                    e);
            
            // En cas d'erreur, ne pas acknowledger le message pour permettre une retry
            // Le message sera retraité plus tard
        }
    }

    /**
     * Acknowledger manuellement le message Kafka.
     *
     * @param acknowledgment L'objet d'acknowledgement
     */
    private void acknowledgeMessage(Acknowledgment acknowledgment) {
        if (acknowledgment != null) {
            try {
                acknowledgment.acknowledge();
                log.debug("Message acknowledged successfully");
            } catch (Exception e) {
                log.warn("Failed to acknowledge message: {}", e.getMessage());
            }
        }
    }
}
