package com.zaphira.notification.service;

import com.zaphira.notification.model.entity.Notification;
import com.zaphira.notification.model.entity.NotificationLog;
import com.zaphira.notification.model.entity.NotificationStatus;
import com.zaphira.notification.repository.NotificationLogRepository;
import com.zaphira.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NotificationDispatchService - dispatches notifications to configured channels
 * Currently supports IN_APP channel; can be extended for EMAIL, SMS, PUSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    
    @Transactional
    public void dispatchNotification(Notification notification) {
        try {
            log.info("Dispatching notification: {} to user: {} via {}", 
                notification.getId(), notification.getUserId(), notification.getChannel());
            
            // For IN_APP: just mark as sent
            if (notification.getChannel().name().equals("IN_APP")) {
                dispatchToInApp(notification);
            } else {
                // Placeholder for other channels
                log.debug("Channel {} not yet implemented", notification.getChannel());
                markAsUnsupported(notification);
            }
            
        } catch (Exception e) {
            log.error("Error dispatching notification: {}", notification.getId(), e);
            logDispatchAttempt(notification, NotificationStatus.FAILED, e.getMessage());
        }
    }
    
    /**
     * Dispatch to in-app channel (database storage + API retrieval)
     * This is synchronous and always succeeds
     */
    private void dispatchToInApp(Notification notification) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
        logDispatchAttempt(notification, NotificationStatus.SENT, null);
        log.info("Notification {} dispatched to IN_APP", notification.getId());
    }
    
    /**
     * Mark notification as unsupported (channel not yet implemented)
     */
    private void markAsUnsupported(Notification notification) {
        notification.setStatus(NotificationStatus.FAILED);
        notificationRepository.save(notification);
        logDispatchAttempt(notification, NotificationStatus.FAILED, 
            "Channel not supported: " + notification.getChannel());
    }
    
    /**
     * Log dispatch attempt for audit trail
     */
    @Transactional
    private void logDispatchAttempt(Notification notification, NotificationStatus status, String errorMessage) {
        long attemptCount = notificationLogRepository.countByNotificationId(notification.getId());
        
        NotificationLog log = NotificationLog.builder()
            .notificationId(notification.getId())
            .userId(notification.getUserId())
            .channel(notification.getChannel())
            .status(status)
            .attemptNumber((int) (attemptCount + 1))
            .errorMessage(errorMessage)
            .build();
        
        notificationLogRepository.save(log);
    }
    
    /**
     * Retry failed notification
     */
    @Transactional
    public void retryNotification(Notification notification) {
        log.info("Retrying notification: {}", notification.getId());
        dispatchNotification(notification);
    }
}
