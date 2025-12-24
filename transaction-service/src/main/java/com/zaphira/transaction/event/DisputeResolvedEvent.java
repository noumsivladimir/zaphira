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
 * DisputeResolvedEvent - Kafka event published when dispute is resolved.
 * 
 * Event Flow:
 * 1. Admin makes resolution decision via DisputeController
 * 2. DisputeResolutionService resolves dispute and releases funds
 * 3. Dispute status updated → DisputeResolvedEvent published to Kafka
 * 4. Other services listen and react (release funds, notify parties, close case, update metrics, etc)
 * 
 * Resolution Types:
 * - APPROVED: Customer wins full refund
 * - PARTIAL_APPROVAL: Partial refund to customer, remainder to merchant
 * - DENIED: Merchant wins, customer gets no refund
 * - SETTLEMENT: Negotiated resolution
 * - WITHDRAWN: Customer withdraws dispute
 * - EXPIRED: Deadline passed
 * - ESCALATED_TO_BANK: Escalated to payment processor
 * 
 * Consumers:
 * - Wallet Service: Release funds to winner based on resolution type
 * - Notification Service: Send final decision email to customer and merchant
 * - Analytics Service: Update dispute resolution metrics
 * - Compliance Service: Log final decision for audit trail
 * - CRM Service: Update customer dispute history
 * 
 * Topic: "dispute-resolved"
 * Key: dispute.getReference() (e.g., "DSP-ABC123DEF456")
 * 
 * Payload includes resolution details for downstream processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResolvedEvent implements Serializable {
    
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
     * Type of resolution decision
     * Possible values: APPROVED, PARTIAL_APPROVAL, DENIED, SETTLEMENT, WITHDRAWN, EXPIRED, ESCALATED_TO_BANK
     */
    @JsonProperty("resolution_type")
    private String resolutionType;
    
    /**
     * Amount decided in the resolution
     */
    @JsonProperty("resolution_amount")
    private BigDecimal resolutionAmount;
    
    /**
     * Currency of the resolution amount
     */
    @JsonProperty("currency")
    private String currency;
    
    /**
     * Admin/User who made the resolution
     */
    @JsonProperty("resolved_by")
    private String resolvedBy;
    
    /**
     * When the dispute was resolved
     */
    @JsonProperty("resolved_at")
    private LocalDateTime resolvedAt;
    
    /**
     * Optional internal notes (not shared with customer)
     */
    @JsonProperty("internal_notes")
    private String internalNotes;
}
