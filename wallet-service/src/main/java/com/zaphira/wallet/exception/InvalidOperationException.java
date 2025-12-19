package com.zaphira.wallet.exception;

public class InvalidOperationException extends WalletException {
    public InvalidOperationException(String message) {
        super(message, "INVALID_OPERATION");
    }
}