package com.zaphira.notification.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NotificationTemplate entity - stores message templates for each event type
 * Templates support placeholders for dynamic content injection
 */
@Entity
@Table(name = "notification_templates", uniqueConstraints = {
    @UniqueConstraint(name = "uk_event_type", columnNames = "event_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {
    
    @Id
    private String id;
    
    @Column(name = "event_type", nullable = false, unique = true)
    private String eventType;
    
    @Column(name = "title_template", nullable = false)
    private String titleTemplate;
    
    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    /**
     * Resolve template with provided variables
     * Replaces ${variable} placeholders with actual values
     */
    public String resolveTitle(java.util.Map<String, Object> variables) {
        return resolveTemplate(titleTemplate, variables);
    }
    
    /**
     * Resolve message template with provided variables
     */
    public String resolveMessage(java.util.Map<String, Object> variables) {
        return resolveTemplate(messageTemplate, variables);
    }
    
    private String resolveTemplate(String template, java.util.Map<String, Object> variables) {
        String result = template;
        if (variables != null) {
            for (java.util.Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return result;
    }
}
