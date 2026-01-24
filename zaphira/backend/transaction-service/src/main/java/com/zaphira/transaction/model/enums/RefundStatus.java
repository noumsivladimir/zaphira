package com.zaphira.transaction.model.enums;

/**
 * Refund status enumeration
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public enum RefundStatus {
    /**
     * Refund request initiated, awaiting approval
     */
    PENDING,
    
    /**
     * Refund is being processed
     */
    PROCESSING,
    
    /**
     * Refund successfully completed
     */
    COMPLETED,
    
    /**
     * Refund failed
     */
    FAILED,
    
    /**
     * Refund was cancelled
     */
    CANCELLED
}
