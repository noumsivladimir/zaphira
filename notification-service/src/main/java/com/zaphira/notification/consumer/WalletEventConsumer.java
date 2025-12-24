package com.zaphira.notification.consumer;

import com.zaphira.notification.event.NotificationEvent;
import com.zaphira.notification.service.NotificationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * WalletEventConsumer - listens to wallet domain events
 * Processes 6 wallet event types for notification generation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventConsumer {
    
    private final NotificationProcessingService processingService;
    
    @KafkaListener(
        topics = "notification.wallet.events",
        groupId = "notification-service-wallet-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeWalletEvent(@Payload NotificationEvent event) {
        try {
            log.info("Received wallet event: {} from user: {}", 
                event.getEventType(), event.getUserId());
            
            // Process the event - will apply rules, create, and dispatch notification
            processingService.processEvent(event);
            
        } catch (Exception e) {
            log.error("Error consuming wallet event: {}", event.getEventType(), e);
            // Event goes to DLQ on repeated failure via Spring Kafka retry config
        }
    }
}
