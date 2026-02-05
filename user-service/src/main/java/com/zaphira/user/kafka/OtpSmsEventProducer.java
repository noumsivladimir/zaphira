package com.zaphira.user.kafka;

import com.zaphira.common.event.OtpSmsRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for OTP SMS requests.
 * Publishes OtpSmsRequestEvent to Notification Service for Twilio SMS delivery.
 * 
 * Topic: otp-sms-request
 * Consumer: Notification Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class OtpSmsEventProducer implements OtpSmsPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.otp-sms-request:otp-sms-request}")
    private String otpSmsRequestTopic;

    /**
     * Publishes an OTP SMS request to Kafka for async delivery via Notification Service.
     *
     * @param userId          User ID requesting OTP
     * @param phoneNumber     Phone number in E.164 format
     * @param otpCode         The OTP code to send
     * @param purpose         Purpose (REGISTRATION, PIN_RESET, etc.)
     * @param expiresInMinutes OTP validity period
     * @param isResend        Whether this is a resend
     * @return Correlation ID for tracking
     */
    @Override
    public String publishOtpSmsRequest(Long userId, String phoneNumber, String otpCode, 
                                        String purpose, int expiresInMinutes, boolean isResend) {
        
        String correlationId = UUID.randomUUID().toString();
        
        OtpSmsRequestEvent event = OtpSmsRequestEvent.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresInMinutes(expiresInMinutes)
                .requestedAt(LocalDateTime.now())
                .isResend(isResend)
                .correlationId(correlationId)
                .build();

        log.info("[OTP_SMS_PRODUCER] Publishing OTP SMS request - userId: {}, phone: {}, purpose: {}, correlationId: {}", 
                userId, maskPhoneNumber(phoneNumber), purpose, correlationId);

        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    otpSmsRequestTopic, 
                    correlationId, 
                    event
            );

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[OTP_SMS_PRODUCER] ❌ Failed to publish OTP SMS request - correlationId: {}, error: {}", 
                            correlationId, ex.getMessage());
                } else {
                    log.info("[OTP_SMS_PRODUCER] ✅ OTP SMS request published - correlationId: {}, partition: {}, offset: {}", 
                            correlationId, 
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

            return correlationId;

        } catch (Exception e) {
            log.error("[OTP_SMS_PRODUCER] ❌ Exception publishing OTP SMS request - correlationId: {}", correlationId, e);
            throw new RuntimeException("Failed to publish OTP SMS request", e);
        }
    }

    /**
     * Convenience method for registration OTP.
     */
    @Override
    public String publishRegistrationOtp(Long userId, String phoneNumber, String otpCode) {
        return publishOtpSmsRequest(userId, phoneNumber, otpCode, "REGISTRATION", 5, false);
    }

    /**
     * Convenience method for PIN reset OTP.
     */
    @Override
    public String publishPinResetOtp(Long userId, String phoneNumber, String otpCode) {
        return publishOtpSmsRequest(userId, phoneNumber, otpCode, "PIN_RESET", 5, false);
    }

    /**
     * Convenience method for resending OTP.
     */
    @Override
    public String publishResendOtp(Long userId, String phoneNumber, String otpCode, String purpose) {
        return publishOtpSmsRequest(userId, phoneNumber, otpCode, purpose, 5, true);
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "*".repeat(phoneNumber.length() - 4) + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
