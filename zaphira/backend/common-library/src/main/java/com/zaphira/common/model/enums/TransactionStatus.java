package com.zaphira.common.model.enums;

/**
 * Transaction status enumeration
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public enum TransactionStatus {
    /**
     * Transaction has been initiated but not yet processed
     */
    PENDING,
    
    /**
     * Transaction requires authorization (2FA, OTP, etc.)
     */
    AWAITING_AUTHORIZATION,
    
    /**
     * Transaction has been authorized and is being processed
     */
    PROCESSING,
    
    /**
     * Transaction has been successfully completed
     */
    COMPLETED,
    
    /**
     * Transaction failed due to an error
     */
    FAILED,
    
    /**
     * Transaction was cancelled by user
     */
    CANCELLED,
    
    /**
     * Transaction was declined (insufficient funds, limits, etc.)
     */
    DECLINED,
    
    /**
     * Transaction is being reversed
     */
    REVERSING,
    
    /**
     * Transaction has been reversed
     */
    REVERSED,
    
    /**
     * Transaction has been partially refunded
     */
    PARTIALLY_REFUNDED,
    
    /**
     * Transaction has been fully refunded
     */
    FULLY_REFUNDED,
    
    /**
     * Transaction is expired (timeout)
     */
    EXPIRED
}
