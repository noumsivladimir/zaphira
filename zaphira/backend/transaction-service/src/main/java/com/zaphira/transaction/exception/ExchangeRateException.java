package com.zaphira.transaction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ExchangeRateException - FX rate or currency conversion errors.
 * 
 * HTTP 503 Service Unavailable - FX provider issues
 * or HTTP 400 Bad Request - Unsupported currency
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ExchangeRateException extends RuntimeException {
    
    public ExchangeRateException(String message) {
        super(message);
    }
    
    public ExchangeRateException(String message, Throwable cause) {
        super(message, cause);
    }
}
