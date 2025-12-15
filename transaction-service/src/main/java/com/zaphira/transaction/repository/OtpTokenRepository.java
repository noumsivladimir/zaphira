package com.zaphira.transaction.repository;

import com.zaphira.common.model.entities.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing OTP tokens for transaction authorization
 */
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /**
     * Find the most recent unused OTP for a given phone number and purpose
     */
    @Query("SELECT o FROM OtpToken o WHERE o.phoneNumber = :phoneNumber AND o.purpose = :purpose AND o.used = false ORDER BY o.id DESC LIMIT 1")
    Optional<OtpToken> findActiveOtpByPhoneAndPurpose(@Param("phoneNumber") String phoneNumber, @Param("purpose") String purpose);

    /**
     * Find an OTP token by phone number and code
     */
    @Query("SELECT o FROM OtpToken o WHERE o.phoneNumber = :phoneNumber AND o.code = :code AND o.used = false")
    Optional<OtpToken> findByPhoneAndCode(@Param("phoneNumber") String phoneNumber, @Param("code") String code);

    /**
     * Find an OTP token by phone number, code, and purpose
     */
    @Query("SELECT o FROM OtpToken o WHERE o.phoneNumber = :phoneNumber AND o.code = :code AND o.purpose = :purpose AND o.used = false")
    Optional<OtpToken> findByPhoneCodeAndPurpose(@Param("phoneNumber") String phoneNumber, @Param("code") String code, @Param("purpose") String purpose);

    /**
     * Find an OTP by ID and phone number (for security validation)
     */
    @Query("SELECT o FROM OtpToken o WHERE o.id = :id AND o.phoneNumber = :phoneNumber AND o.used = false")
    Optional<OtpToken> findByIdAndPhone(@Param("id") Long id, @Param("phoneNumber") String phoneNumber);
}
