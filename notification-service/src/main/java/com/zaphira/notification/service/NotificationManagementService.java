package com.zaphira.notification.service;

import com.zaphira.notification.model.entity.Notification;
import com.zaphira.notification.model.entity.NotificationStatus;
import com.zaphira.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * NotificationManagementService - handles user-facing notification operations
 * Allows users to retrieve, read, and manage their notifications
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationManagementService {
    
    private final NotificationRepository notificationRepository;
    
    /**
     * Get all notifications for a user, paginated
     */
    public Page<Notification> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * Get unread notifications for a user
     */
    public Page<Notification> getUnreadNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            userId, NotificationStatus.PENDING, pageable);
    }
    
    /**
     * Get unread notification count
     */
    public Long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.PENDING);
    }
    
    /**
     * Mark notification as read
     */
    @Transactional
    public Notification markAsRead(String notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        
        if (notification.isPresent()) {
            Notification n = notification.get();
            n.setStatus(NotificationStatus.READ);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
            log.info("Marked notification {} as read", notificationId);
            return n;
        }
        
        log.warn("Notification not found: {}", notificationId);
        return null;
    }
    
    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(String userId) {
        Page<Notification> unread = getUnreadNotifications(userId, 
            org.springframework.data.domain.PageRequest.of(0, 100));
        
        for (Notification notification : unread) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        
        log.info("Marked all notifications as read for user: {}", userId);
    }
    
    /**
     * Delete/archive notification
     */
    @Transactional
    public void archiveNotification(String notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        
        if (notification.isPresent()) {
            Notification n = notification.get();
            n.setStatus(NotificationStatus.ARCHIVED);
            notificationRepository.save(n);
            log.info("Archived notification: {}", notificationId);
        }
    }
}
