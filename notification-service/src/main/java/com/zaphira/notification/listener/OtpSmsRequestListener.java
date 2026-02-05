package com.zaphira.notification.listener;

import com.zaphira.common.event.OtpSmsRequestEvent;
import com.zaphira.common.event.OtpSmsResultEvent;
import com.zaphira.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka consumer for OTP SMS requests.
 * Listens to 'otp-sms-request' topic and sends SMS via Twilio.
 * Publishes result to 'otp-sms-result' topic for tracking.
 * 
 * Producer: User Service
 * Topic: otp-sms-request
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class OtpSmsRequestListener {

    private final SmsService smsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String OTP_SMS_RESULT_TOPIC = "otp-sms-result";

    @KafkaListener(
            topics = "otp-sms-request",
            groupId = "notification-otp-sms-group",
            containerFactory = "otpSmsRequestKafkaListenerContainerFactory"
    )
    public void handleOtpSmsRequest(OtpSmsRequestEvent event, Acknowledgment ack) {
        log.info("[OTP_SMS_LISTENER] 📩 Received OTP SMS request - userId: {}, phone: {}, purpose: {}, correlationId: {}",
                event.getUserId(),
                maskPhoneNumber(event.getPhoneNumber()),
                event.getPurpose(),
                event.getCorrelationId());

        String twilioMessageSid = null;
        boolean success = false;
        String errorMessage = null;

        try {
            // Build SMS message based on purpose
            String message = buildOtpMessage(event.getOtpCode(), event.getPurpose(), event.getExpiresInMinutes());

            // Send SMS via Twilio
            log.info("[OTP_SMS_LISTENER] 📤 Sending SMS via Twilio to: {}", maskPhoneNumber(event.getPhoneNumber()));
            smsService.sendSms(event.getPhoneNumber(), message);

            success = true;
            log.info("[OTP_SMS_LISTENER] ✅ OTP SMS sent successfully - correlationId: {}", event.getCorrelationId());

        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("[OTP_SMS_LISTENER] ❌ Failed to send OTP SMS - correlationId: {}, error: {}",
                    event.getCorrelationId(), e.getMessage(), e);
        }

        // Publish result event for tracking
        try {
            OtpSmsResultEvent resultEvent = OtpSmsResultEvent.builder()
                    .userId(event.getUserId())
                    .phoneNumber(event.getPhoneNumber())
                    .correlationId(event.getCorrelationId())
                    .success(success)
                    .twilioMessageSid(twilioMessageSid)
                    .errorMessage(errorMessage)
                    .sentAt(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(OTP_SMS_RESULT_TOPIC, event.getCorrelationId(), resultEvent);
            log.info("[OTP_SMS_LISTENER] 📨 Published OTP SMS result - success: {}, correlationId: {}",
                    success, event.getCorrelationId());

        } catch (Exception e) {
            log.warn("[OTP_SMS_LISTENER] ⚠️ Failed to publish result event - correlationId: {}",
                    event.getCorrelationId(), e);
        }

        // Acknowledge message
        if (ack != null) {
            ack.acknowledge();
        }
    }

    /**
     * Builds the OTP message based on purpose.
     */
    private String buildOtpMessage(String otpCode, String purpose, int expiresInMinutes) {
        String purposeText;
        switch (purpose.toUpperCase()) {
            case "REGISTRATION":
                purposeText = "inscription";
                break;
            case "PIN_RESET":
                purposeText = "réinitialisation de PIN";
                break;
            case "LOGIN":
                purposeText = "connexion";
                break;
            case "TRANSACTION":
                purposeText = "transaction";
                break;
            default:
                purposeText = "vérification";
        }

        return String.format(
                "Zaphira - Votre code de %s est: %s. Ce code expire dans %d minutes. Ne le partagez jamais.",
                purposeText,
                otpCode,
                expiresInMinutes
        );
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "*".repeat(phoneNumber.length() - 4) + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
