package com.zaphira.transaction.dto;

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
 * DisputeRequest DTO - Request object for creating a new dispute.
 * 
 * Customers can initiate disputes for transactions they believe are incorrect,
 * fraudulent, or for which service was not provided.
 * 
 * Validation Rules:
 * - transactionId: Required, must be a valid transaction ID
 * - category: Required, must be one of predefined categories
 * - reason: Required, minimum 20 characters for context
 * - claimedAmount: Required, must be > 0 and <= transaction amount
 * - description: Optional, can be empty but recommended (max 2000 chars)
 * 
 * Example:
 * {
 *   "transactionId": "TXN-123456789",
 *   "category": "FRAUDULENT_TRANSACTION",
 *   "reason": "I did not authorize this transaction",
 *   "claimedAmount": 250.00,
 *   "description": "Charged to my card without permission, contacted merchant but no response"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new dispute for a transaction")
public class DisputeRequest {
    
    /**
     * ID of the transaction being disputed
     * Format: TXN-XXXXXXXXX or equivalent from transaction service
     */
    @NotBlank(message = "Transaction ID is required")
    @JsonProperty("transaction_id")
    @Schema(
        description = "ID of the transaction being disputed",
        example = "TXN-1234567890",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String transactionId;
    
    /**
     * Category of the dispute
     * Possible values: FRAUD, DUPLICATE_CHARGE, SERVICE_NOT_PROVIDED, 
     * TECHNICAL_ERROR, PARTIAL_REFUND, REFUND_NOT_RECEIVED, CURRENCY_MISMATCH,
     * BILLING_CYCLE_ERROR, MERCHANDISE_QUALITY, UNAUTHORIZED_TRANSACTION, etc
     */
    @NotBlank(message = "Dispute category is required")
    @JsonProperty("category")
    @Schema(
        description = "Category of the dispute",
        example = "FRAUDULENT_TRANSACTION",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"FRAUD", "DUPLICATE_CHARGE", "SERVICE_NOT_PROVIDED", 
                          "TECHNICAL_ERROR", "PARTIAL_REFUND", "REFUND_NOT_RECEIVED"}
    )
    private String category;
    
    /**
     * Brief reason for the dispute
     * Must be at least 20 characters to provide adequate context
     */
    @NotBlank(message = "Reason is required and must be at least 20 characters")
    @JsonProperty("reason")
    @Schema(
        description = "Brief reason for initiating the dispute",
        example = "I did not authorize this transaction",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 20
    )
    private String reason;
    
    /**
     * Amount claimed in dispute
     * Must be greater than 0 and not exceed transaction amount
     */
    @NotNull(message = "Claimed amount is required")
    @DecimalMin(value = "0.01", message = "Claimed amount must be greater than 0")
    @JsonProperty("claimed_amount")
    @Schema(
        description = "Amount claimed in the dispute",
        example = "250.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal claimedAmount;
    
    /**
     * Currency of the dispute amount
     * Should match transaction currency if empty
     */
    @JsonProperty("currency")
    @Schema(
        description = "Currency code (ISO 4217), defaults to transaction currency",
        example = "USD",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String currency;
    
    /**
     * Detailed description of the dispute
     * Optional but highly recommended for quicker resolution
     * Maximum 2000 characters
     */
    @JsonProperty("description")
    @Schema(
        description = "Detailed description of the dispute",
        example = "I was charged for a service I did not request. I have not received the service.",
        maxLength = 2000,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String description;
}
