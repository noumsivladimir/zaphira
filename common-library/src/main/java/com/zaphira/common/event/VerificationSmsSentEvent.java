package com.zaphira.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a verification SMS is sent to a user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationSmsSentEvent {
    private Long userId;
    private String phoneNumber;
    private String verificationCode;
    private LocalDateTime sentAt;
    private boolean isResend; // true if this is a resend, false for initial send
}