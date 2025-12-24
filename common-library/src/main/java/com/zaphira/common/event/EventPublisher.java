package com.zaphira.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * EventPublisher - utility for publishing NotificationEvents to Kafka
 * Used by Transaction, Wallet, and User services to publish domain events
 * 
 * Ensures standard event format across all microservices
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Publish notification event to appropriate Kafka topic
     */
    public void publishNotificationEvent(String eventType, String sourceService, 
                                        String userId, String aggregateId, 
                                        Map<String, Object> payload) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .sourceService(sourceService)
                .occurredAt(LocalDateTime.now())
                .userId(userId)
                .aggregateId(aggregateId)
                .payload(objectMapper.valueToTree(payload))
                .build();
            
            // Determine topic based on source service
            String topic = getTopic(sourceService);
            
            log.info("Publishing event: {} to topic: {} for user: {}", 
                eventType, topic, userId);
            
            kafkaTemplate.send(topic, userId, event);
            
        } catch (Exception e) {
            log.error("Error publishing notification event: {}", eventType, e);
            // Don't rethrow - event publishing should not block business logic
        }
    }
    
    /**
     * Determine Kafka topic based on source service
     */
    private String getTopic(String sourceService) {
        return switch (sourceService) {
            case "transaction-service" -> "notification.transaction.events";
            case "wallet-service" -> "notification.wallet.events";
            case "user-service" -> "notification.user.events";
            default -> "notification.default.events";
        };
    }
}
