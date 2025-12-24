package com.zaphira.notification.controller;

import com.zaphira.notification.dto.NotificationDTO;
import com.zaphira.notification.model.entity.Notification;
import com.zaphira.notification.service.NotificationManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * NotificationController - REST API for notification management
 * Endpoints for retrieving, reading, and managing notifications
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationManagementService managementService;
    
    /**
     * GET /api/notifications/user/{userId}
     * Retrieve all notifications for a user (paginated)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationDTO>> getUserNotifications(
            @PathVariable String userId,
            Pageable pageable) {
        
        log.info("Fetching notifications for user: {}", userId);
        
        Page<Notification> notifications = managementService.getUserNotifications(userId, pageable);
        Page<NotificationDTO> dtoPage = notifications.map(this::toDTO);
        
        return ResponseEntity.ok(dtoPage);
    }
    
    /**
     * GET /api/notifications/user/{userId}/unread
     * Get unread notifications only
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<Page<NotificationDTO>> getUnreadNotifications(
            @PathVariable String userId,
            Pageable pageable) {
        
        log.info("Fetching unread notifications for user: {}", userId);
        
        Page<Notification> notifications = managementService.getUnreadNotifications(userId, pageable);
        Page<NotificationDTO> dtoPage = notifications.map(this::toDTO);
        
        return ResponseEntity.ok(dtoPage);
    }
    
    /**
     * GET /api/notifications/user/{userId}/unread-count
     * Get count of unread notifications
     */
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable String userId) {
        Long count = managementService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }
    
    /**
     * PUT /api/notifications/{id}/read
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable String id) {
        log.info("Marking notification as read: {}", id);
        
        Notification notification = managementService.markAsRead(id);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(toDTO(notification));
    }
    
    /**
     * PUT /api/notifications/user/{userId}/mark-all-read
     * Mark all notifications as read for a user
     */
    @PutMapping("/user/{userId}/mark-all-read")
    public ResponseEntity<String> markAllAsRead(@PathVariable String userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        managementService.markAllAsRead(userId);
        return ResponseEntity.ok("All notifications marked as read");
    }
    
    /**
     * DELETE /api/notifications/{id}
     * Archive/delete a notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> archiveNotification(@PathVariable String id) {
        log.info("Archiving notification: {}", id);
        managementService.archiveNotification(id);
        return ResponseEntity.ok("Notification archived");
    }
    
    /**
     * Convert Notification entity to DTO
     */
    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
            .id(notification.getId())
            .userId(notification.getUserId())
            .eventType(notification.getEventType())
            .sourceService(notification.getSourceService())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .status(notification.getStatus())
            .priority(notification.getPriority())
            .channel(notification.getChannel())
            .createdAt(notification.getCreatedAt())
            .sentAt(notification.getSentAt())
            .readAt(notification.getReadAt())
            .build();
    }
}
