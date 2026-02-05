package com.zaphira.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published by Notification Service after OTP SMS delivery attempt.
 * Used for tracking and audit purposes.
 * 
 * Kafka Topic: otp-sms-result
 * Producer: Notification Service
 * Consumer: User Service (optional, for tracking)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSmsResultEvent {
    
    /**
     * User ID associated with the OTP
     */
    private Long userId;
    
    /**
     * Phone number the SMS was sent to
     */
    private String phoneNumber;
    
    /**
     * Correlation ID for tracing
     */
    private String correlationId;
    
    /**
     * Whether the SMS was sent successfully
     */
    private boolean success;
    
    /**
     * Twilio message SID if successful
     */
    private String twilioMessageSid;
    
    /**
     * Error message if failed
     */
    private String errorMessage;
    
    /**
     * Timestamp of the delivery attempt
     */
    private LocalDateTime sentAt;
}
