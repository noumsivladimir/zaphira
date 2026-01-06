package com.zaphira.service_user.repository;

import com.zaphira.service_user.model.entities.OtpToken;
import com.zaphira.service_user.model.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findByPhoneNumberAndPurposeAndUsedFalse(String phoneNumber, OtpPurpose purpose);

    void deleteByPhoneNumberAndPurpose(String phoneNumber, OtpPurpose purpose);

    Optional<OtpToken> findByEmailAndPurposeAndUsedFalse(String email, OtpPurpose purpose);

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);
}