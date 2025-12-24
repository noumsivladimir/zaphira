package com.zaphira.transaction.model.enums;

/**
 * Enum representing the role of the person who initiated the dispute.
 * 
 * Determines authorization level and processing rules.
 */
public enum DisputeInitiatorRole {
    
    /**
     * Customer (transaction sender) initiated dispute
     */
    CUSTOMER("Customer"),
    
    /**
     * Merchant (transaction receiver) initiated dispute
     */
    MERCHANT("Merchant"),
    
    /**
     * Admin/Support staff initiated dispute
     */
    ADMIN("Admin"),
    
    /**
     * System automatically escalated to dispute
     */
    SYSTEM("System");
    
    private final String description;
    
    DisputeInitiatorRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
