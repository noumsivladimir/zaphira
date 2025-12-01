package com.zaphira.notification.listener;

import com.zaphira.common.event.UserRegisteredEvent;
import com.zaphira.notification.service.EmailService;
import com.zaphira.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {
    
    private final EmailService emailService;
    private final SmsService smsService;

    @KafkaListener(topics = "user-registered", groupId = "notification-service")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user registered event: {}", event.getEmail());
        
        try {
            // Send welcome email
            String subject = "Welcome to Zaphira!";
            String body = String.format(
                "Hello %s,\n\nWelcome to Zaphira! Your account has been successfully created.\n\nThank you for joining us!",
                event.getFullName()
            );
            emailService.sendEmail(event.getEmail(), subject, body);
            
            // Send welcome SMS
            smsService.sendSms(event.getPhoneNumber(), "Welcome to Zaphira! Your account has been created.");
            
            log.info("Welcome notification sent to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome notification to: {}", event.getEmail(), e);
        }
    }
}

