package com.zaphira.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DisputeMessageDTO - Represents a message/event in dispute history.
 * 
 * Used for GET /api/disputes/{disputeId}/messages endpoint.
 * Maps DisputeTimeline entity to a user-friendly message format.
 * 
 * Timeline events include:
 * - Dispute creation
 * - Evidence submissions
 * - Status changes
 * - Merchant responses
 * - Admin actions
 * - Resolution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeMessageDTO {
    
    /**
     * Timeline event ID
     */
    private Long id;
    
    /**
     * Type of event (CREATED, EVIDENCE_ADDED, STATUS_CHANGED, RESOLVED, etc.)
     */
    private String eventType;
    
    /**
     * Human-readable description of what happened
     */
    private String message;
    
    /**
     * Who performed this action (email or "SYSTEM")
     */
    private String actor;
    
    /**
     * Role of actor (CUSTOMER, MERCHANT, ADMIN, SYSTEM)
     */
    private String actorRole;
    
    /**
     * Previous status (if status change event)
     */
    private String oldStatus;
    
    /**
     * New status (if status change event)
     */
    private String newStatus;
    
    /**
     * When this event occurred
     */
    private LocalDateTime timestamp;
}
