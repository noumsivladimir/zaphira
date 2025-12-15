package com.zaphira.service_user.repository;

import com.zaphira.service_user.model.entities.UserSession;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByToken(String token);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    List<UserSession> findByUserId(Long userId);

    List<UserSession> findByUserIdAndIsActive(Long userId, Boolean isActive);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.isActive = true")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM UserSession s WHERE s.expiryTime < :now AND s.isActive = true")
    List<UserSession> findExpiredActiveSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.logoutTime = :logoutTime WHERE s.userId = :userId AND s.isActive = true")
    void invalidateAllUserSessions(@Param("userId") Long userId, @Param("logoutTime") LocalDateTime logoutTime);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.logoutTime = :logoutTime WHERE s.sessionId = :sessionId")
    void invalidateSession(@Param("sessionId") Long sessionId, @Param("logoutTime") LocalDateTime logoutTime);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiryTime < :date")
    void deleteExpiredSessions(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.isActive = true")
    Long countActiveSessionsByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM UserSession s WHERE s.ipAddress = :ipAddress AND s.isActive = true")
    List<UserSession> findByIpAddress(@Param("ipAddress") String ipAddress);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.loginTime >= :startDate AND s.loginTime <= :endDate")
    Long countLoginsBetween(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);
}