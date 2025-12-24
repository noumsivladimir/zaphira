package com.zaphira.wallet.exception;

public class DuplicateWalletException extends WalletException {
    public DuplicateWalletException(String message) {
        super(message, "DUPLICATE_WALLET");
    }
}