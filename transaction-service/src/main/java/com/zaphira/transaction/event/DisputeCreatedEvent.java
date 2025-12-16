package com.zaphira.transaction.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DisputeCreatedEvent - Kafka event published when dispute is created.
 * 
 * Event Flow:
 * 1. Customer/Admin creates dispute via DisputeController
 * 2. DisputeService creates Dispute entity and saves to database
 * 3. Dispute created successfully → DisputeCreatedEvent published to Kafka
 * 4. Other services listen and react (hold funds, notify parties, update metrics, etc)
 * 
 * Consumers:
 * - Wallet Service: Hold disputed amount in escrow
 * - Notification Service: Send email to customer and merchant
 * - Analytics Service: Track dispute metrics
 * - Compliance Service: Log for audit trail
 * 
 * Topic: "dispute-created"
 * Key: dispute.getReference() (e.g., "DSP-ABC123DEF456")
 * 
 * Payload includes all relevant context for downstream processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeCreatedEvent implements Serializable {
    
    /**
     * Unique ID of the dispute
     */
    @JsonProperty("dispute_id")
    private Long disputeId;
    
    /**
     * Dispute reference number (e.g., DSP-ABC123DEF456)
     * Used as Kafka message key for partitioning
     */
    @JsonProperty("reference")
    private String reference;
    
    /**
     * ID of the disputed transaction
     */
    @JsonProperty("transaction_id")
    private String transactionId;
    
    /**
     * Category of the dispute
     */
    @JsonProperty("category")
    private String category;
    
    /**
     * Reason provided by customer
     */
    @JsonProperty("reason")
    private String reason;
    
    /**
     * Amount claimed in the dispute
     */
    @JsonProperty("claimed_amount")
    private BigDecimal claimedAmount;
    
    /**
     * Currency of the claimed amount
     */
    @JsonProperty("currency")
    private String currency;
    
    /**
     * Email of who initiated the dispute
     */
    @JsonProperty("initiated_by")
    private String initiatedBy;
    
    /**
     * Role of the dispute initiator (CUSTOMER, MERCHANT, ADMIN, SYSTEM)
     */
    @JsonProperty("initiator_role")
    private String initiatorRole;
    
    /**
     * When the dispute was created
     */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    /**
     * Deadline for evidence submission
     */
    @JsonProperty("deadline_at")
    private LocalDateTime deadlineAt;
}
