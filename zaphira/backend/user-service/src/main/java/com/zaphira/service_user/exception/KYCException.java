package com.zaphira.service_user.exception;

/**
 * Exception thrown for KYC-related business logic errors
 */
public class KYCException extends RuntimeException {
    
    public KYCException(String message) {
        super(message);
    }
    
    public KYCException(String message, Throwable cause) {
        super(message, cause);
    }
}
