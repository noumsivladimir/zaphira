package com.zaphira.notification.dto;

import com.zaphira.notification.model.entity.NotificationChannel;
import com.zaphira.notification.model.entity.NotificationPriority;
import com.zaphira.notification.model.entity.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NotificationDTO - response DTO for notification API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    
    private String id;
    private String userId;
    private String eventType;
    private String sourceService;
    private String title;
    private String message;
    private NotificationStatus status;
    private NotificationPriority priority;
    private NotificationChannel channel;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
