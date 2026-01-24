package com.zaphira.common.model.enums;

/**
 * Dispute status enumeration
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public enum DisputeStatus {
    /**
     * Dispute has been opened
     */
    OPEN,
    
    /**
     * Dispute is under investigation
     */
    INVESTIGATING,
    
    /**
     * Awaiting response from merchant
     */
    AWAITING_MERCHANT_RESPONSE,
    
    /**
     * Awaiting response from customer
     */
    AWAITING_CUSTOMER_RESPONSE,
    
    /**
     * Escalated to higher authority
     */
    ESCALATED,
    
    /**
     * Dispute resolved in favor of customer
     */
    RESOLVED_CUSTOMER_FAVOR,
    
    /**
     * Dispute resolved in favor of merchant
     */
    RESOLVED_MERCHANT_FAVOR,
    
    /**
     * Dispute closed without resolution
     */
    CLOSED,
    
    /**
     * Dispute withdrawn by customer
     */
    WITHDRAWN
}
