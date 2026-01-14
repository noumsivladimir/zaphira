package com.zaphira.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a verification email is sent to a user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationEmailSentEvent {
    private Long userId;
    private String email;
    private String verificationCode;
    private LocalDateTime sentAt;
    private boolean isResend; // true if this is a resend, false for initial send
}