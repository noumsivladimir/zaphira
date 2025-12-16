package com.zaphira.transaction.model.enums;

/**
 * Enum representing different categories of disputes.
 * 
 * Used to categorize the nature of the dispute for routing and resolution rules.
 */
public enum DisputeCategory {
    // Customer claims against merchant/service provider
    TRANSACTION_NOT_RECOGNIZED("Transaction not recognized"),
    FRAUDULENT_TRANSACTION("Fraudulent transaction"),
    DUPLICATE_CHARGE("Duplicate charge"),
    INCORRECT_AMOUNT("Incorrect amount"),
    SERVICE_NOT_PROVIDED("Service not provided"),
    SERVICE_QUALITY_ISSUE("Service quality issue"),
    
    // Merchant claims against customer
    PAYMENT_NOT_RECEIVED("Payment not received"),
    PARTIAL_PAYMENT("Partial payment"),
    NON_DELIVERY("Non-delivery by customer"),
    CUSTOMER_NO_SHOW("Customer no-show"),
    CHARGEBACK_THREAT("Chargeback threat"),
    
    // Operational issues
    TECHNICAL_ERROR("Technical error"),
    SYSTEM_MALFUNCTION("System malfunction"),
    PROCESSING_ERROR("Processing error"),
    TIMEOUT_ERROR("Timeout error"),
    
    // Other
    OTHER("Other");
    
    private final String description;
    
    DisputeCategory(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
