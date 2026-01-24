package com.zaphira.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendEmail_ValidParameters_SendsSuccessfully() {
        // Given
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@zaphira.com");
        String to = "user@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        // When
        emailService.sendEmail(to, subject, body);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_NullRecipient_ThrowsException() {
        // Given
        String to = null;
        String subject = "Test Subject";
        String body = "Test Body";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> emailService.sendEmail(to, subject, body));
        assertEquals("Email recipient cannot be null or empty", exception.getMessage());
    }

    @Test
    void sendEmail_EmptySubject_ThrowsException() {
        // Given
        String to = "user@example.com";
        String subject = "";
        String body = "Test Body";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> emailService.sendEmail(to, subject, body));
        assertEquals("Email subject cannot be null or empty", exception.getMessage());
    }
}