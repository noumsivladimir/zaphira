package com.zaphira.common.model.enums;

/**
 * Transaction type enumeration
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public enum TransactionType {
    /**
     * Money sent from one wallet to another
     */
    TRANSFER,
    
    /**
     * Payment to merchant
     */
    PAYMENT,
    
    /**
     * Deposit to wallet from external source
     */
    DEPOSIT,
    
    /**
     * Withdrawal from wallet to external destination
     */
    WITHDRAWAL,
    
    /**
     * Refund to customer
     */
    REFUND,
    
    /**
     * Adjustment by admin
     */
    ADJUSTMENT,
    
    /**
     * Fee charged
     */
    FEE,
    
    /**
     * Reversal of a previous transaction
     */
    REVERSAL,
    
    /**
     * Currency exchange
     */
    EXCHANGE
}
