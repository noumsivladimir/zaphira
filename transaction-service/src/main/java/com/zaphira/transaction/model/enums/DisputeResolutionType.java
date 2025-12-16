package com.zaphira.transaction.model.enums;

/**
 * Enum representing the type of resolution for a dispute.
 * 
 * Determines how the disputed amount will be handled:
 * - APPROVED: Full refund to customer
 * - DENIED: Full refund to merchant
 * - PARTIAL_APPROVAL: Split decision
 * - SETTLEMENT: Agreed settlement between parties
 * - WITHDRAWN: Customer withdrew the dispute
 * - EXPIRED: Deadline passed without action
 * - ESCALATED_TO_BANK: Escalated to banking authority
 */
public enum DisputeResolutionType {
    
    /**
     * Dispute approved - refund customer
     */
    APPROVED("Approved"),
    
    /**
     * Dispute denied - refund merchant
     */
    DENIED("Denied"),
    
    /**
     * Partial approval - split decision
     */
    PARTIAL_APPROVAL("Partial approval"),
    
    /**
     * Agreed settlement between parties
     */
    SETTLEMENT("Settlement"),
    
    /**
     * Dispute withdrawn by customer
     */
    WITHDRAWN("Withdrawn"),
    
    /**
     * Expired without action
     */
    EXPIRED("Expired"),
    
    /**
     * Escalated to banking authority
     */
    ESCALATED_TO_BANK("Escalated to bank");
    
    private final String description;
    
    DisputeResolutionType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
