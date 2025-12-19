package com.zaphira.wallet.exception;

public class WalletFrozenException extends WalletException {
    public WalletFrozenException(String message) {
        super(message, "WALLET_FROZEN");
    }
}
