package com.zaphira.user.repository;

import com.zaphira.user.model.entities.OtpCode;
import com.zaphira.user.model.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findByPhoneNumberAndPurposeAndUsedFalse(String phoneNumber, OtpPurpose purpose);

    Optional<OtpCode> findByPhoneNumberAndPurposeAndConsumedFalse(String phoneNumber, OtpPurpose purpose);

    Optional<OtpCode> findByEmailAndPurposeAndUsedFalse(String email, OtpPurpose purpose);

    Optional<OtpCode> findByEmailAndPurposeAndConsumedFalse(String email, OtpPurpose purpose);

    void deleteByPhoneNumberAndPurpose(String phoneNumber, OtpPurpose purpose);

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now")
    void deleteExpiredCodes(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.phoneNumber = :phoneNumber AND o.purpose = :purpose AND o.used = false AND o.consumed = false")
    long countActiveCodesByPhoneNumberAndPurpose(@Param("phoneNumber") String phoneNumber, @Param("purpose") OtpPurpose purpose);
}
