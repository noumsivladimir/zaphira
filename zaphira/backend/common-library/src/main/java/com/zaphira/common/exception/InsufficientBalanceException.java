package com.zaphira.common.exception;

/**
 * Exception thrown when insufficient balance for an operation
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public class InsufficientBalanceException extends BusinessException {
    
    public InsufficientBalanceException(String message) {
        super(message, "INSUFFICIENT_BALANCE");
    }
    
    public InsufficientBalanceException(String walletId, double required, double available) {
        super(String.format("Insufficient balance in wallet %s. Required: %.2f, Available: %.2f", 
            walletId, required, available), "INSUFFICIENT_BALANCE");
    }
}
