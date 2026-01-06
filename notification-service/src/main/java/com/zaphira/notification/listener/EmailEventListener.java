package com.zaphira.notification.listener;

import com.zaphira.common.event.EmailSendEvent;
import com.zaphira.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {

    private final EmailService emailService;

    @KafkaListener(
            topics = "email-send-events",
            groupId = "notification-service",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void handleEmailSendEvent(com.zaphira.common.event.EmailSendEvent event) {
        log.info("Received email send event for: {} | Type: {}", event.getTo(), event.getEmailType());

        try {
            if (event.getHtmlBody() != null && !event.getHtmlBody().isEmpty()) {
                // Envoyer un email HTML
                emailService.sendHtmlEmail(event.getTo(), event.getSubject(), event.getHtmlBody());
            } else {
                // Envoyer un email texte simple
                emailService.sendEmail(event.getTo(), event.getSubject(), event.getBody());
            }

            log.info("Email sent successfully to: {} | Type: {}", event.getTo(), event.getEmailType());

        } catch (Exception e) {
            log.error("Failed to send email to: {} | Type: {}", event.getTo(), event.getEmailType(), e);
            // TODO: Implement retry mechanism or dead letter queue
        }
    }
}