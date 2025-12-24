package com.zaphira.notification.repository;

import com.zaphira.notification.model.entity.NotificationLog;
import com.zaphira.notification.model.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {
    
    List<NotificationLog> findByNotificationIdOrderByCreatedAtDesc(String notificationId);
    
    List<NotificationLog> findByNotificationIdAndStatus(String notificationId, NotificationStatus status);
    
    Long countByNotificationId(String notificationId);
}
