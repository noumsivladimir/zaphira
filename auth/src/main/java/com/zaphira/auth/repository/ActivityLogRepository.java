package com.zaphira.auth.repository;

import com.zaphira.auth.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Corrigé : Spring Data va suivre la relation ActivityLog.user.userId
    List<ActivityLog> findByUserUserIdOrderByTimestampDesc(Long userId);
}
