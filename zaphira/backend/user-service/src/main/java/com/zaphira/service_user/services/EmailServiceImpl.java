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
    
    @Override
    public void sendKYCSubmissionConfirmation(String to, String firstName) {
        String subject = "Zaphira - KYC Documents Received";
        String body = String.format(
            "Hello %s,\n\n" +
            "We have received your KYC documents and they are currently under review.\n\n" +
            "Our compliance team will review your documents within 24-48 hours. " +
            "You will receive an email notification once the verification is complete.\n\n" +
            "If you have any questions, please contact our support team.\n\n" +
            "Best regards,\n" +
            "Zaphira Team",
            firstName
        );
        sendEmail(to, subject, body);
    }
    
    @Override
    public void sendKYCResubmissionConfirmation(String to, String firstName) {
        String subject = "Zaphira - KYC Documents Resubmitted";
        String body = String.format(
            "Hello %s,\n\n" +
            "We have received your updated KYC documents.\n\n" +
            "Our compliance team will review your documents as soon as possible. " +
            "You will be notified once the review is complete.\n\n" +
            "Thank you for your patience.\n\n" +
            "Best regards,\n" +
            "Zaphira Team",
            firstName
        );
        sendEmail(to, subject, body);
    }
    
    @Override
    public void sendKYCApprovalEmail(String to, String firstName) {
        String subject = "Zaphira - KYC Verification Approved ✓";
        String body = String.format(
            "Hello %s,\n\n" +
            "Great news! Your KYC verification has been approved.\n\n" +
            "You now have full access to all Zaphira features including:\n" +
            "- Higher transaction limits\n" +
            "- International transfers\n" +
            "- Withdrawal capabilities\n" +
            "- Enhanced security features\n\n" +
            "Thank you for completing the verification process.\n\n" +
            "Best regards,\n" +
            "Zaphira Team",
            firstName
        );
        sendEmail(to, subject, body);
    }
    
    @Override
    public void sendKYCRejectionEmail(String to, String firstName, String reason, boolean canResubmit) {
        String subject = "Zaphira - KYC Verification Update";
        String resubmitText = canResubmit 
                ? "You can resubmit your documents through your account dashboard." 
                : "Please contact support for assistance.";
        
        String body = String.format(
            "Hello %s,\n\n" +
            "Unfortunately, we were unable to verify your KYC documents.\n\n" +
            "Reason: %s\n\n" +
            "%s\n\n" +
            "If you have any questions, please don't hesitate to contact our support team.\n\n" +
            "Best regards,\n" +
            "Zaphira Team",
            firstName, reason, resubmitText
        );
        sendEmail(to, subject, body);
    }
    
    @Override
    public void sendKYCExpiryNotification(String to, String firstName) {
        String subject = "Zaphira - KYC Document Expired";
        String body = String.format(
            "Hello %s,\n\n" +
            "Your KYC verification document has expired.\n\n" +
            "To continue using full features of your Zaphira account, " +
            "please resubmit your updated identification documents.\n\n" +
            "You can update your documents through your account dashboard.\n\n" +
            "Best regards,\n" +
            "Zaphira Team",
            firstName
        );
        sendEmail(to, subject, body);
    }
}