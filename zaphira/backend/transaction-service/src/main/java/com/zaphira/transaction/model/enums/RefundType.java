package com.zaphira.transaction.model.enums;

/**
 * Refund type enumeration
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public enum RefundType {
    /**
     * Full refund of the original transaction amount
     */
    FULL,
    
    /**
     * Partial refund (less than original amount)
     */
    PARTIAL
}
