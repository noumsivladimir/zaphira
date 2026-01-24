package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DisputeResponse DTO - Response object containing full dispute details.
 * 
 * Returned after creating a dispute or retrieving an existing one.
 * Contains all relevant information about the dispute state, including
 * timeline events and evidence count.
 * 
 * Example Response:
 * {
 *   "disputeId": "DSP-1234567890",
 *   "transactionId": "TXN-1234567890",
 *   "status": "UNDER_INVESTIGATION",
 *   "category": "FRAUDULENT_TRANSACTION",
 *   "reason": "I did not authorize this transaction",
 *   "claimedAmount": "250.00",
 *   "currency": "USD",
 *   "initiatedBy": "customer@example.com",
 *   "initiatorRole": "CUSTOMER",
 *   "evidenceCount": 2,
 *   "timelineEventsCount": 5,
 *   "createdAt": "2024-12-15T10:30:00Z",
 *   "deadlineAt": "2024-12-29T10:30:00Z"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing dispute details")
public class DisputeResponse {
    
    /**
     * Unique identifier for this dispute
     * Format: DSP-XXXXXXXXX
     */
    @JsonProperty("dispute_id")
    @Schema(
        description = "Unique identifier for the dispute",
        example = "DSP-1234567890",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String disputeId;
    
    /**
     * ID of the disputed transaction
     */
    @JsonProperty("transaction_id")
    @Schema(
        description = "ID of the disputed transaction",
        example = "TXN-1234567890",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String transactionId;
    
    /**
     * Current status of the dispute
     * Possible values: INITIATED, UNDER_INVESTIGATION, AWAITING_EVIDENCE,
     * AWAITING_RESPONSE, RESOLVED, CLOSED, APPEAL_REQUESTED, ESCALATED, EXPIRED
     */
    @JsonProperty("status")
    @Schema(
        description = "Current status of the dispute",
        example = "UNDER_INVESTIGATION",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String status;
    
    /**
     * Category of the dispute
     */
    @JsonProperty("category")
    @Schema(
        description = "Category of the dispute",
        example = "FRAUDULENT_TRANSACTION",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String category;
    
    /**
     * Brief reason provided by initiator
     */
    @JsonProperty("reason")
    @Schema(
        description = "Reason for initiating the dispute",
        example = "I did not authorize this transaction",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String reason;
    
    /**
     * Amount claimed in the dispute
     */
    @JsonProperty("claimed_amount")
    @Schema(
        description = "Amount claimed in the dispute",
        example = "250.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal claimedAmount;
    
    /**
     * Currency of the dispute
     */
    @JsonProperty("currency")
    @Schema(
        description = "Currency code (ISO 4217)",
        example = "USD",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String currency;
    
    /**
     * Email/ID of who initiated the dispute
     */
    @JsonProperty("initiated_by")
    @Schema(
        description = "Email/ID of who initiated the dispute",
        example = "customer@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String initiatedBy;
    
    /**
     * Role of the dispute initiator
     * Possible values: CUSTOMER, MERCHANT, ADMIN, SYSTEM
     */
    @JsonProperty("initiator_role")
    @Schema(
        description = "Role of the person initiating the dispute",
        example = "CUSTOMER",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String initiatorRole;
    
    /**
     * Whether dispute is open for submission of evidence
     */
    @JsonProperty("open_for_input")
    @Schema(
        description = "Whether the dispute is still open for evidence submission",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean openForInput;
    
    /**
     * Number of evidence items submitted so far
     */
    @JsonProperty("evidence_count")
    @Schema(
        description = "Number of evidence items submitted",
        example = "2",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer evidenceCount;
    
    /**
     * Number of timeline events (activity history)
     */
    @JsonProperty("timeline_events_count")
    @Schema(
        description = "Number of timeline events recorded",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer timelineEventsCount;
    
    /**
     * Resolution type (if dispute is resolved)
     * Possible values: APPROVED, DENIED, PARTIAL_APPROVAL, SETTLEMENT, WITHDRAWN, EXPIRED, ESCALATED_TO_BANK
     */
    @JsonProperty("resolution_type")
    @Schema(
        description = "Type of resolution (null if not resolved)",
        example = "APPROVED",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String resolutionType;
    
    /**
     * Amount resolved (if dispute is resolved)
     */
    @JsonProperty("resolution_amount")
    @Schema(
        description = "Amount resolved (null if not resolved)",
        example = "250.00",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private BigDecimal resolutionAmount;
    
    /**
     * Who resolved the dispute (if resolved)
     */
    @JsonProperty("resolved_by")
    @Schema(
        description = "Who resolved the dispute (null if not resolved)",
        example = "admin@company.com",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String resolvedBy;
    
    /**
     * Timestamp when dispute was created
     */
    @JsonProperty("created_at")
    @Schema(
        description = "Timestamp when dispute was created",
        example = "2024-12-15T10:30:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime createdAt;
    
    /**
     * Deadline for evidence submission
     * Usually 14 days from creation
     */
    @JsonProperty("deadline_at")
    @Schema(
        description = "Deadline for evidence submission",
        example = "2024-12-29T10:30:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime deadlineAt;
    
    /**
     * Whether deadline has passed
     */
    @JsonProperty("deadline_passed")
    @Schema(
        description = "Whether the evidence submission deadline has passed",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean deadlinePassed;
    
    /**
     * Timestamp when dispute was resolved (if applicable)
     */
    @JsonProperty("resolved_at")
    @Schema(
        description = "Timestamp when dispute was resolved (null if not resolved)",
        example = "2024-12-20T15:45:00Z",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDateTime resolvedAt;
}
