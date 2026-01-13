package com.zaphira.wallet.exception;

public class MerchantAlreadyExistsException extends WalletException {
    public MerchantAlreadyExistsException(String message, String errorCode) {
        super(message, errorCode);
    }

    public MerchantAlreadyExistsException(String message) {
        super(message);
    }

    public MerchantAlreadyExistsException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
