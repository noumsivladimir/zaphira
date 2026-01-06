package com.zaphira.notification.service;

import com.zaphira.common.event.VerificationEmailSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final KafkaTemplate<String, Object> notificationKafkaTemplate;

    private static final String VERIFICATION_EMAIL_TOPIC = "verification-email-events";

    public void publishVerificationEmailSentEvent(Long userId, String email, String verificationCode, boolean isResend) {
        try {
            VerificationEmailSentEvent event = VerificationEmailSentEvent.builder()
                    .userId(userId)
                    .email(email)
                    .verificationCode(verificationCode)
                    .sentAt(LocalDateTime.now())
                    .isResend(isResend)
                    .build();

            notificationKafkaTemplate.send(VERIFICATION_EMAIL_TOPIC, userId.toString(), event);
            log.info("Published VerificationEmailSentEvent for user ID: {} (resend: {})", userId, isResend);
        } catch (Exception e) {
            log.error("Failed to publish VerificationEmailSentEvent for user ID: {}", userId, e);
        }
    }
}