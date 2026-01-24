package com.zaphira.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when an email needs to be sent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendEvent {
    private String to;
    private String subject;
    private String body;
    private String htmlBody; // Optional, for HTML emails
    private String emailType; // "verification", "welcome", "notification", etc.
    private Long userId; // Optional, reference to user
    private LocalDateTime requestedAt;
}