package com.zaphira.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published by User Service to request OTP SMS delivery via Notification Service.
 * Consumed by Notification Service to send SMS via Twilio.
 * 
 * Kafka Topic: otp-sms-request
 * Producer: User Service
 * Consumer: Notification Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSmsRequestEvent {
    
    /**
     * User ID associated with the OTP request
     */
    private Long userId;
    
    /**
     * Phone number in E.164 format (e.g., +237690000000)
     */
    private String phoneNumber;
    
    /**
     * The OTP code to send
     */
    private String otpCode;
    
    /**
     * Purpose of the OTP (REGISTRATION, PIN_RESET, LOGIN, etc.)
     */
    private String purpose;
    
    /**
     * OTP expiration time in minutes
     */
    private int expiresInMinutes;
    
    /**
     * Timestamp when the request was created
     */
    private LocalDateTime requestedAt;
    
    /**
     * Whether this is a resend of a previous OTP
     */
    private boolean isResend;
    
    /**
     * Correlation ID for tracing across services
     */
    private String correlationId;
}
