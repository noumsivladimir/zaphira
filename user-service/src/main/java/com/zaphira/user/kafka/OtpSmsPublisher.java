package com.zaphira.user.kafka;

/**
 * Interface for OTP SMS event publishing.
 * Has two implementations:
 * - OtpSmsEventProducer: Publishes to Kafka when kafka.enabled=true
 * - NoOpOtpSmsEventProducer: No-op when Kafka is disabled (uses HTTP fallback)
 */
public interface OtpSmsPublisher {
    
    /**
     * Publishes an OTP SMS request.
     *
     * @param userId          User ID
     * @param phoneNumber     Phone in E.164 format
     * @param otpCode         OTP code
     * @param purpose         Purpose (REGISTRATION, PIN_RESET, etc.)
     * @param expiresInMinutes Validity period
     * @param isResend        Whether resend
     * @return Correlation ID
     */
    String publishOtpSmsRequest(Long userId, String phoneNumber, String otpCode, 
                                String purpose, int expiresInMinutes, boolean isResend);

    String publishRegistrationOtp(Long userId, String phoneNumber, String otpCode);
    
    String publishPinResetOtp(Long userId, String phoneNumber, String otpCode);
    
    String publishResendOtp(Long userId, String phoneNumber, String otpCode, String purpose);
}
