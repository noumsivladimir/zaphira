package com.zaphira.notification.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NotificationLog entity - audit trail for notification dispatch attempts
 * Tracks success/failure, retry attempts, and delivery details
 */
@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_notification_id", columnList = "notification_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {
    
    @Id
    private String id;
    
    @Column(name = "notification_id", nullable = false)
    private String notificationId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "response_code")
    private String responseCode;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
