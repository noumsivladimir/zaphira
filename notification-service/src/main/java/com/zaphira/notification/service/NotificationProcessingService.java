package com.zaphira.notification.service;

import com.zaphira.notification.event.NotificationEvent;
import com.zaphira.notification.engine.NotificationRule;
import com.zaphira.notification.engine.NotificationRuleEngine;
import com.zaphira.notification.model.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NotificationProcessingService - orchestrates the notification pipeline
 * Coordinates event validation, rule application, creation, and dispatch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingService {
    
    private final NotificationRuleEngine ruleEngine;
    private final NotificationCreationService creationService;
    private final NotificationDispatchService dispatchService;
    
    @Transactional
    public void processEvent(NotificationEvent event) {
        try {
            // Step 1: Validate event
            if (!event.isValid()) {
                log.warn("Invalid notification event: missing required fields");
                return;
            }
            
            event.setEventIdIfNull();
            
            // Step 2: Check if notifications are enabled for this event type
            NotificationRule rule = ruleEngine.getRule(event.getEventType());
            if (!rule.isApplicable()) {
                log.debug("Notifications disabled for event type: {}", event.getEventType());
                return;
            }
            
            // Step 3: Create notification from event
            Notification notification = creationService.createNotification(
                event,
                rule.getChannels().get(0),  // Use first channel from rule
                rule.getPriority(),
                rule.getTemplate()
            );
            
            // Step 4: Dispatch notification
            dispatchService.dispatchNotification(notification);
            
            log.info("Successfully processed event: {} for user: {}", 
                event.getEventType(), event.getUserId());
            
        } catch (Exception e) {
            log.error("Error processing notification event: {}", event.getEventType(), e);
            // Don't rethrow - events should be processed independently
        }
    }
}
