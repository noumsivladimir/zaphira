package com.zaphira.notification.repository;

import com.zaphira.notification.model.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
    
    Optional<NotificationTemplate> findByEventTypeAndIsActiveTrue(String eventType);
    
    Optional<NotificationTemplate> findByEventType(String eventType);
}
