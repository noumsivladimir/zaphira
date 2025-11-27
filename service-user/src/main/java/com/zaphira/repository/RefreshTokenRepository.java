package com.zaphira.zaphira;

import com.zaphira.zaphira.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Transactional
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeTokensByUserId(Long userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now OR r.revoked = true")
    void deleteExpiredTokens(Instant now);
}
