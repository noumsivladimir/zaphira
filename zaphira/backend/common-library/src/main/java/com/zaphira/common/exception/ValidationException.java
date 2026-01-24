package com.zaphira.common.exception;

/**
 * Exception thrown when a validation fails
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", cause);
    }
}
