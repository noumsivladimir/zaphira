package com.zaphira.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dispute update notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeUpdateNotificationRequest {
    
    private Long disputeId;
    private String disputeReference;
    private Long transactionId;
    private String transactionReference;
    private String previousStatus;
    private String newStatus;
    private Long initiatorUserId; // User who opened the dispute
    private Long respondentUserId; // User being disputed against
    private String updateType; // STATUS_CHANGE, EVIDENCE_SUBMITTED, RESPONSE_RECEIVED, ESCALATED, RESOLVED
    private String message; // Optional message to include in notification
    private String resolution; // For resolved disputes: REFUNDED, REJECTED, PARTIAL_REFUND
}
