package com.zaphira.notification.repository;

import com.zaphira.notification.model.entity.Notification;
import com.zaphira.notification.model.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    // Fetch notifications by user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Fetch unread notifications
    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, NotificationStatus status, Pageable pageable);
    
    // Find notifications by event type for audit
    List<Notification> findByEventTypeAndCreatedAtBetween(String eventType, LocalDateTime from, LocalDateTime to);
    
    // Find pending notifications for retry
    List<Notification> findByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime before);
    
    // Idempotency check - prevent duplicate notifications for same event
    @Query("SELECT n FROM Notification n WHERE n.aggregateId = :aggregateId AND n.eventType = :eventType AND n.userId = :userId")
    Optional<Notification> findExistingNotification(@Param("aggregateId") String aggregateId, 
                                                     @Param("eventType") String eventType,
                                                     @Param("userId") String userId);
    
    // Count unread notifications for user
    Long countByUserIdAndStatus(String userId, NotificationStatus status);
}
