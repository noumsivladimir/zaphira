package com.zaphira.wallet.exception;

public class WalletNotFoundException extends WalletException {
    public WalletNotFoundException(String message) {
        super(message, "WALLET_NOT_FOUND");
    }
}
