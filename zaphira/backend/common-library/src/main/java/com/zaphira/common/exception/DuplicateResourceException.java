package com.zaphira.common.exception;

/**
 * Exception thrown when a duplicate resource is detected
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public class DuplicateResourceException extends BusinessException {
    
    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE");
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, "DUPLICATE_RESOURCE", cause);
    }
}
