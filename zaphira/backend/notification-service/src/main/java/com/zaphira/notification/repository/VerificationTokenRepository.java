package com.zaphira.notification.repository;

import com.zaphira.notification.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByUserIdAndCodeAndUsedFalseAndExpiresAtAfter(Long userId, String code, LocalDateTime now);

    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.used = true WHERE vt.userId = :userId AND vt.code = :code AND vt.used = false")
    int markAsUsed(@Param("userId") Long userId, @Param("code") String code);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.used = true AND vt.createdAt < :cutoffDate")
    int deleteOldUsedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Nouvelles méthodes pour la gestion avancée
    boolean existsByUserIdAndCodeAndUsedFalse(Long userId, String code);

    long countByUserIdAndUsedFalse(Long userId);

    List<VerificationToken> findByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.userId = :userId AND vt.used = false")
    int deleteByUserIdAndUsedFalse(@Param("userId") Long userId);

    // Méthodes pour les statistiques
    long countByUsedFalse();
    long countByUsedTrue();

    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE vt.expiresAt < :dateTime")
    long countByExpiresAtBefore(@Param("dateTime") LocalDateTime dateTime);
}