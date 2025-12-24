package com.zaphira.notification.repository;

import com.zaphira.notification.model.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
    
    Optional<NotificationPreference> findByUserIdAndEventType(String userId, String eventType);
    
    List<NotificationPreference> findByUserId(String userId);
}
