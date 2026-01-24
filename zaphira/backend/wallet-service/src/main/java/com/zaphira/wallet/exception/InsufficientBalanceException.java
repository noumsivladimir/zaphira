package com.zaphira.wallet.exception;

public class InsufficientBalanceException extends WalletException {
    public InsufficientBalanceException(String message) {
        super(message, "INSUFFICIENT_BALANCE");
    }
}