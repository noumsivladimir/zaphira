package com.zaphira.common.exception;

/**
 * Exception thrown when access is denied
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public class AccessDeniedException extends BusinessException {
    
    public AccessDeniedException(String message) {
        super(message, "ACCESS_DENIED");
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(message, "ACCESS_DENIED", cause);
    }
}
