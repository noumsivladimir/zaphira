package com.zaphira.transaction.service.routing;

/**
 * Exception thrown when transaction routing fails.
 */
public class RoutingException extends RuntimeException {
    private final String errorCode;

    public RoutingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RoutingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
