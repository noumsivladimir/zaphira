package com.zaphira.transaction.exception;

/**
 * ValidationException - Thrown when request validation fails.
 * 
 * HTTP Status: 422 (Unprocessable Entity)
 * Used for: Invalid dispute data, missing required fields, invalid amounts, etc.
 * 
 * Example:
 * - Dispute reason too long
 * - Claimed amount is negative
 * - Invalid dispute category
 * - Missing required fields in request
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
