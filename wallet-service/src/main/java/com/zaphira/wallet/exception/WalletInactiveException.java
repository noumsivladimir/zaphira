package com.zaphira.wallet.exception;

public class WalletInactiveException extends WalletException {
    public WalletInactiveException(String message) {
        super(message, "WALLET_INACTIVE");
    }
}