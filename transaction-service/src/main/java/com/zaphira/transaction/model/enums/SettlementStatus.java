package com.zaphira.transaction.model.enums;

/**
 * SettlementStatus - Status of transaction settlement.
 * 
 * Flow:
 * PENDING → PROCESSING → COMPLETED
 *                     ↘ FAILED (retry)
 */
public enum SettlementStatus {
    PENDING("Awaiting settlement processing"),
    PROCESSING("Settlement in progress"),
    COMPLETED("Settlement completed successfully"),
    FAILED("Settlement failed, awaiting retry"),
    CANCELLED("Settlement cancelled");
    
    private final String description;
    
    SettlementStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
