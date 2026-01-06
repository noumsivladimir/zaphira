package com.zaphira.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        // Validation des paramètres
        if (!StringUtils.hasText(to)) {
            throw new IllegalArgumentException("Email recipient cannot be null or empty");
        }
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Email subject cannot be null or empty");
        }
        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("Email body cannot be null or empty");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            // Ajouter l'expéditeur si configuré
            if (StringUtils.hasText(fromEmail)) {
                message.setFrom(fromEmail);
            }
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send email to: {} - MailException: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to: {}", to, e);
            throw new RuntimeException("Unexpected error while sending email", e);
        }
    }

    public void sendTransactionNotification(String to, String transactionReference, String amount, String status) {
        String subject = "Transaction Notification - " + transactionReference;
        String body = String.format(
            "Your transaction %s for amount %s has been %s.\n\nThank you for using Zaphira.",
            transactionReference, amount, status
        );
        sendEmail(to, subject, body);
    }

    /**
     * Envoie un email HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(htmlBody)) {
            throw new IllegalArgumentException("Email parameters cannot be null or empty");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            
            if (StringUtils.hasText(fromEmail)) {
                helper.setFrom(fromEmail);
            }
            
            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    /**
     * Envoie une notification de bienvenue avec format HTML
     */
    public void sendWelcomeEmail(String to, String firstName, String lastName) {
        String subject = "Welcome to Zaphira - Account Created Successfully";
        String htmlBody = String.format(
            "<html><body>" +
            "<h2>Welcome to Zaphira, %s %s!</h2>" +
            "<p>Your account has been successfully created.</p>" +
            "<p>You can now start using our platform for secure transactions.</p>" +
            "<br>" +
            "<p>Best regards,<br>The Zaphira Team</p>" +
            "</body></html>",
            firstName, lastName
        );
        sendHtmlEmail(to, subject, htmlBody);
    }
}

