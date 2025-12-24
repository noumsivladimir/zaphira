package com.zaphira.notification.consumer;

import com.zaphira.notification.event.NotificationEvent;
import com.zaphira.notification.service.NotificationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * TransactionEventConsumer - listens to transaction domain events
 * Processes 8 transaction event types for notification generation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {
    
    private final NotificationProcessingService processingService;
    
    @KafkaListener(
        topics = "notification.transaction.events",
        groupId = "notification-service-transaction-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(@Payload NotificationEvent event) {
        try {
            log.info("Received transaction event: {} from user: {}", 
                event.getEventType(), event.getUserId());
            
            // Process the event - will apply rules, create, and dispatch notification
            processingService.processEvent(event);
            
        } catch (Exception e) {
            log.error("Error consuming transaction event: {}", event.getEventType(), e);
            // Event goes to DLQ on repeated failure via Spring Kafka retry config
        }
    }
}
