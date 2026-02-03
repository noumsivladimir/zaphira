package com.zaphira.wallet.exception;

public class UnauthorizedWalletAccessException extends WalletException {
    public UnauthorizedWalletAccessException(String message) {
        super(message, "FORBIDDEN");
    }
}
