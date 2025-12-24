package com.zaphira.notification.service;

import com.zaphira.notification.event.NotificationEvent;
import com.zaphira.notification.model.entity.Notification;
import com.zaphira.notification.model.entity.NotificationChannel;
import com.zaphira.notification.model.entity.NotificationPriority;
import com.zaphira.notification.model.entity.NotificationStatus;
import com.zaphira.notification.repository.NotificationRepository;
import com.zaphira.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * NotificationCreationService - creates notifications from events
 * Responsible for template resolution and content generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    
    @Transactional
    public Notification createNotification(NotificationEvent event, 
                                          NotificationChannel channel,
                                          NotificationPriority priority,
                                          String templateName) {
        
        // Check for duplicate (idempotency)
        Optional<Notification> existing = notificationRepository.findExistingNotification(
            event.getAggregateId(), 
            event.getEventType(),
            event.getUserId()
        );
        
        if (existing.isPresent()) {
            log.info("Notification already exists for event: {} - {}", 
                event.getEventType(), event.getAggregateId());
            return existing.get();
        }
        
        // Load template and resolve content
        var template = templateRepository.findByEventType(templateName);
        
        String title = "Notification";
        String message = "New notification";
        
        if (template.isPresent()) {
            var templateEntity = template.get();
            Map<String, Object> variables = extractEventVariables(event);
            title = templateEntity.resolveTitle(variables);
            message = templateEntity.resolveMessage(variables);
        } else {
            log.warn("No template found for event type: {}", templateName);
        }
        
        // Create notification entity
        Notification notification = Notification.builder()
            .id(UUID.randomUUID().toString())
            .userId(event.getUserId())
            .eventType(event.getEventType())
            .sourceService(event.getSourceService())
            .aggregateId(event.getAggregateId())
            .title(title)
            .message(message)
            .status(NotificationStatus.PENDING)
            .priority(priority)
            .channel(channel)
            .metadata(event.getPayload() != null ? event.getPayload().toString() : null)
            .build();
        
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification: {} for user: {} from event: {}", 
            saved.getId(), saved.getUserId(), event.getEventType());
        
        return saved;
    }
    
    /**
     * Extract variables from event payload for template resolution
     */
    private Map<String, Object> extractEventVariables(NotificationEvent event) {
        Map<String, Object> variables = new java.util.HashMap<>();
        
        if (event.getPayload() != null) {
            try {
                // Convert JSON payload to map
                event.getPayload().fields().forEachRemaining(entry -> 
                    variables.put(entry.getKey(), entry.getValue().asText())
                );
            } catch (Exception e) {
                log.warn("Failed to extract variables from payload", e);
            }
        }
        
        // Add standard variables
        variables.put("eventType", event.getEventType());
        variables.put("sourceService", event.getSourceService());
        variables.put("aggregateId", event.getAggregateId());
        
        return variables;
    }
}
