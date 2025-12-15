package com.zaphira.transaction.service.exception;

public class WalletOperationException extends RuntimeException {
    public WalletOperationException(String message) {
        super(message);
    }

    public WalletOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}


