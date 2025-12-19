package com.zaphira.wallet.exception;

public class LimitExceededException extends WalletException {
    public LimitExceededException(String message) {
        super(message, "LIMIT_EXCEEDED");
    }
}