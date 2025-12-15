package com.zaphira.auth.event;

import com.zaphira.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher pour publier des événements utilisateur vers Kafka.
 * Utilisé pour la communication asynchrone avec wallet-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    
    private static final String USER_REGISTERED_TOPIC = "user-registered";

    /**
     * Publie un événement UserRegisteredEvent vers Kafka.
     * 
     * @param event L'événement à publier
     */
    public void publishUserRegistered(UserRegisteredEvent event) {
        try {
            String userId = event.getUserId() != null ? event.getUserId().toString() : "unknown";
            kafkaTemplate.send(USER_REGISTERED_TOPIC, userId, event);
            log.info("Published UserRegisteredEvent for user {} to topic {}", 
                    userId, USER_REGISTERED_TOPIC);
        } catch (Exception e) {
            log.error("Failed to publish UserRegisteredEvent for user {}", 
                    event.getUserId(), e);
            // Optionnel: Enregistrer dans une table de retry ou dead letter queue
        }
    }
}

