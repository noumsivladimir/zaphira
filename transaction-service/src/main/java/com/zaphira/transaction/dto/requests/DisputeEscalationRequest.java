package com.zaphira.transaction.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DisputeEscalationRequest - DTO for escalating a dispute.
 * 
 * Used for POST /api/disputes/{disputeId}/escalate endpoint.
 * 
 * Escalation moves dispute to higher authority when:
 * - Customer unsatisfied with initial resolution
 * - Merchant disputes the resolution
 * - Complex case requiring senior review
 * - Escalated to legal/compliance team
 * 
 * Validation:
 * - Reason required (min 10 chars for meaningful explanation)
 * - Max 2000 chars to prevent abuse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeEscalationRequest {
    
    /**
     * Reason for escalation
     * Required, minimum 10 characters
     */
    @NotBlank(message = "Escalation reason is required")
    @Size(min = 10, max = 2000, message = "Escalation reason must be between 10 and 2000 characters")
    private String reason;
    
    /**
     * Priority level for escalation
     * Optional: LOW, MEDIUM, HIGH, URGENT
     */
    private String priority;
    
    /**
     * Additional notes for escalation team
     * Optional
     */
    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    private String additionalNotes;
}
