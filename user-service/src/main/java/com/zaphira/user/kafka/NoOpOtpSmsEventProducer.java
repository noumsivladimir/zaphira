package com.zaphira.user.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * No-op implementation of OTP SMS producer when Kafka is disabled.
 * Falls back to direct HTTP call via NotificationServiceClient.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOtpSmsEventProducer implements OtpSmsPublisher {

    @Override
    public String publishOtpSmsRequest(Long userId, String phoneNumber, String otpCode, 
                                        String purpose, int expiresInMinutes, boolean isResend) {
        String correlationId = UUID.randomUUID().toString();
        log.info("[OTP_SMS_NO_OP] Kafka disabled - OTP SMS request not published via Kafka. " +
                 "Using HTTP fallback. userId: {}, purpose: {}, correlationId: {}", 
                 userId, purpose, correlationId);
        return correlationId;
    }

    @Override
    public String publishRegistrationOtp(Long userId, String phoneNumber, String otpCode) {
        return publishOtpSmsRequest(userId, phoneNumber, otpCode, "REGISTRATION", 5, false);
    }

    @Override
    public String publishPinResetOtp(Long userId, String phoneNumber, String otpCode) {
        return publishOtpSmsRequest(userId, phoneNumber, otpCode, "PIN_RESET", 5, false);
    }

    @Override
    public String publishResendOtp(Long userId, String phoneNumber, String otpCode, String purpose) {
        return publishOtpSmsRequest(userId, phoneNumber, otpCode, purpose, 5, true);
    }
}
