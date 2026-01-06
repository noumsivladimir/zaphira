package com.zaphira.service_user.services;

import com.zaphira.common.event.EmailSendEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String EMAIL_EVENTS_TOPIC = "email-send-events";

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            EmailSendEvent event = EmailSendEvent.builder()
                    .to(to)
                    .subject(subject)
                    .body(body)
                    .emailType("notification")
                    .requestedAt(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(EMAIL_EVENTS_TOPIC, "email.send", event);
            log.info("Email send event published for: {} | Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to publish email send event for: {}", to, e);
            // Fallback: log the email content
            log.warn("Email not sent - To: {} | Subject: {} | Body: {}", to, subject, body);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            EmailSendEvent event = EmailSendEvent.builder()
                    .to(to)
                    .subject(subject)
                    .htmlBody(htmlBody)
                    .emailType("notification")
                    .requestedAt(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(EMAIL_EVENTS_TOPIC, "email.send.html", event);
            log.info("HTML email send event published for: {} | Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to publish HTML email send event for: {}", to, e);
            // Fallback: log the email content
            log.warn("HTML email not sent - To: {} | Subject: {} | HTML Body: {}", to, subject, htmlBody);
        }
    }

    public void sendVerificationEmail(String to, String verificationCode, Long userId) {
        String subject = "Zaphira - Code de vérification";
        String body = String.format(
            "Bonjour,\n\n" +
            "Votre code de vérification Zaphira est: %s\n\n" +
            "Ce code expirera dans 5 minutes.\n\n" +
            "Si vous n'avez pas demandé ce code, ignorez ce message.\n\n" +
            "L'équipe Zaphira",
            verificationCode
        );

        try {
            EmailSendEvent event = EmailSendEvent.builder()
                    .to(to)
                    .subject(subject)
                    .body(body)
                    .emailType("verification")
                    .userId(userId)
                    .requestedAt(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(EMAIL_EVENTS_TOPIC, "email.verification", event);
            log.info("Verification email event published for user: {} at {}", userId, to);
        } catch (Exception e) {
            log.error("Failed to publish verification email event for user: {}", userId, e);
            // Fallback: log the email content
            log.warn("Verification email not sent - To: {} | Code: {}", to, verificationCode);
        }
    }
}