package com.zaphira.transaction.service.otp;

import com.zaphira.common.model.entities.OtpToken;
import com.zaphira.transaction.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing OTP tokens for transaction authorization.
 * Handles validation, verification, and tracking of OTP attempts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;

    @Value("${transaction.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${transaction.otp.purpose:TRANSACTION_AUTH}")
    private String otpPurpose;

    /**
     * Verify an OTP code for a given phone number and transaction context
     * 
     * @param phoneNumber The user's phone number
     * @param code The OTP code provided by the user
     * @return The verified OtpToken
     * @throws OtpVerificationException if verification fails
     */
    @Transactional
    public OtpToken verifyOtp(String phoneNumber, String code) {
        return verifyOtp(phoneNumber, code, otpPurpose);
    }

    /**
     * Verify an OTP code for a given phone number, code, and purpose
     * 
     * @param phoneNumber The user's phone number
     * @param code The OTP code provided by the user
     * @param purpose The purpose of the OTP
     * @return The verified OtpToken
     * @throws OtpVerificationException if verification fails
     */
    @Transactional
    public OtpToken verifyOtp(String phoneNumber, String code, String purpose) {
        var otpOptional = otpTokenRepository.findByPhoneCodeAndPurpose(phoneNumber, code, purpose);
        
        if (otpOptional.isEmpty()) {
            log.warn("OTP not found for phone: {} and purpose: {}", phoneNumber, purpose);
            throw new OtpVerificationException("Invalid OTP code");
        }

        OtpToken otp = otpOptional.get();

        // Check if OTP is expired
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for phone: {}", phoneNumber);
            throw new OtpVerificationException("OTP has expired");
        }

        // Check if OTP has been used
        if (otp.getUsed()) {
            log.warn("OTP already used for phone: {}", phoneNumber);
            throw new OtpVerificationException("OTP has already been used");
        }

        // Check if max attempts exceeded
        if (otp.getAttempts() >= maxAttempts) {
            log.warn("Max OTP attempts exceeded for phone: {}", phoneNumber);
            otp.setUsed(true);
            otpTokenRepository.save(otp);
            throw new OtpVerificationException("Maximum OTP verification attempts exceeded");
        }

        // Mark as verified
        otp.markAsVerified();
        otpTokenRepository.save(otp);

        log.info("OTP verified successfully for phone: {}", phoneNumber);
        return otp;
    }

    /**
     * Record a failed OTP verification attempt
     * 
     * @param phoneNumber The user's phone number
     * @param code The OTP code that was attempted
     */
    @Transactional
    public void recordFailedAttempt(String phoneNumber, String code) {
        var otpOptional = otpTokenRepository.findByPhoneAndCode(phoneNumber, code);
        
        if (otpOptional.isPresent()) {
            OtpToken otp = otpOptional.get();
            otp.setAttempts(otp.getAttempts() + 1);
            
            // Mark as used if max attempts reached
            if (otp.getAttempts() >= maxAttempts) {
                otp.setUsed(true);
                log.warn("OTP marked as used due to max attempts for phone: {}", phoneNumber);
            }
            
            otpTokenRepository.save(otp);
            log.debug("Recorded failed OTP attempt for phone: {}, attempts: {}", phoneNumber, otp.getAttempts());
        }
    }

    /**
     * Get the most recent active OTP for a phone number
     * 
     * @param phoneNumber The user's phone number
     * @return The active OtpToken or empty if none found
     */
    public java.util.Optional<OtpToken> getActiveOtp(String phoneNumber) {
        return getActiveOtp(phoneNumber, otpPurpose);
    }

    /**
     * Get the most recent active OTP for a phone number and purpose
     * 
     * @param phoneNumber The user's phone number
     * @param purpose The purpose of the OTP
     * @return The active OtpToken or empty if none found
     */
    public java.util.Optional<OtpToken> getActiveOtp(String phoneNumber, String purpose) {
        return otpTokenRepository.findActiveOtpByPhoneAndPurpose(phoneNumber, purpose);
    }

    /**
     * Validate that an OTP token is still active
     * 
     * @param otp The OTP token to validate
     * @return true if valid, false otherwise
     */
    public boolean isOtpValid(OtpToken otp) {
        return otp.isValid() && !otp.isMaxAttemptsExceeded(maxAttempts);
    }

    /**
     * Exception thrown when OTP verification fails
     */
    public static class OtpVerificationException extends RuntimeException {
        public OtpVerificationException(String message) {
            super(message);
        }

        public OtpVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
