package com.zaphira.transaction.exception;

/**
 * Exception thrown when a user attempts to perform an action
 * they do not have authorization for.
 * 
 * Used in authorization checks:
 * - Role validation (ADMIN, SUPPORT, MERCHANT)
 * - Permission validation (TRANSACTION_REVERSE, TRANSACTION_REFUND)
 * - Amount limits validation
 * - Time window validation
 * - Ownership validation
 */
public class AccessDeniedException extends RuntimeException {
    
    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
