package com.zaphira.notification.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NotificationPreference entity - stores per-user notification preferences
 * Allows users to customize which notifications they receive and via which channels
 */
@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_event", columnNames = {"user_id", "event_type"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;
    
    @Column(name = "channels")
    private String channels;  // JSON array: ["IN_APP", "EMAIL"]
    
    @Column(name = "quiet_start")
    private String quietStart;  // HH:mm format for quiet hours start
    
    @Column(name = "quiet_end")
    private String quietEnd;    // HH:mm format for quiet hours end
}
