package com.zaphira.notification.repository;

import com.zaphira.notification.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
}