package com.zaphira.transaction.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DisputeResolutionRequest DTO - Request object for resolving a dispute.
 * 
 * Admins/dispute investigators use this to make final decisions on disputes.
 * The resolution type and amount determine the outcome for customers and merchants.
 * 
 * Validation Rules:
 * - disputeId: Required
 * - resolutionType: Required, must be one of predefined types
 * - resolutionAmount: Required for some resolutions, must be <= claimedAmount
 * - resolutionReason: Required, minimum 20 characters
 * 
 * Resolution Types:
 * - APPROVED: Full refund to customer, amount = claimedAmount
 * - PARTIAL_APPROVAL: Partial refund, amount < claimedAmount
 * - DENIED: No refund, amount = 0
 * - SETTLEMENT: Negotiated resolution between parties
 * - WITHDRAWN: Customer withdrew their dispute
 * - EXPIRED: Dispute deadline passed without resolution
 * - ESCALATED_TO_BANK: Escalated to payment processor/bank
 * 
 * Example:
 * {
 *   "dispute_id": "DSP-1234567890",
 *   "resolution_type": "APPROVED",
 *   "resolution_amount": "250.00",
 *   "resolution_reason": "Evidence clearly shows unauthorized transaction. Full refund approved."
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to resolve a dispute with final decision")
public class DisputeResolutionRequest {
    
    /**
     * ID of the dispute being resolved
     * Format: DSP-XXXXXXXXX
     */
    @NotBlank(message = "Dispute ID is required")
    @JsonProperty("dispute_id")
    @Schema(
        description = "ID of the dispute to resolve",
        example = "DSP-1234567890",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String disputeId;
    
    /**
     * Type of resolution decision
     * Possible values: APPROVED, PARTIAL_APPROVAL, DENIED, SETTLEMENT, WITHDRAWN, EXPIRED, ESCALATED_TO_BANK
     */
    @NotBlank(message = "Resolution type is required")
    @JsonProperty("resolution_type")
    @Schema(
        description = "Type of resolution decision",
        example = "APPROVED",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"APPROVED", "PARTIAL_APPROVAL", "DENIED", "SETTLEMENT", 
                          "WITHDRAWN", "EXPIRED", "ESCALATED_TO_BANK"}
    )
    private String resolutionType;
    
    /**
     * Amount to be refunded/settled
     * Requirements:
     * - For APPROVED: Must equal claimed amount
     * - For PARTIAL_APPROVAL: Must be > 0 and < claimed amount
     * - For DENIED: Must be 0
     * - For SETTLEMENT: Any amount agreed upon
     * - For WITHDRAWN/EXPIRED/ESCALATED: Typically 0 unless agreed otherwise
     */
    @NotNull(message = "Resolution amount is required")
    @DecimalMin(value = "0.00", message = "Resolution amount cannot be negative")
    @JsonProperty("resolution_amount")
    @Schema(
        description = "Amount to be refunded/settled",
        example = "250.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal resolutionAmount;
    
    /**
     * Detailed reason for the resolution decision
     * Must be at least 20 characters
     * Explains the decision to both customer and merchant
     */
    @NotBlank(message = "Resolution reason is required and must be at least 20 characters")
    @JsonProperty("resolution_reason")
    @Schema(
        description = "Detailed reason for the resolution decision",
        example = "Evidence clearly shows unauthorized transaction. Customer did not authorize this purchase.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 20
    )
    private String resolutionReason;
    
    /**
     * Optional internal notes for case history
     * Not shared with customer/merchant
     */
    @JsonProperty("internal_notes")
    @Schema(
        description = "Internal notes for case history (not shared)",
        example = "All evidence reviewed by senior team member. Unusual pattern detected on account.",
        maxLength = 2000,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String internalNotes;
}
