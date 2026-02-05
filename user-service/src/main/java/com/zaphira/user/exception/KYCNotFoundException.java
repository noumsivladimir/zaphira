package com.zaphira.user.exception;

public class KYCNotFoundException extends RuntimeException {
    public KYCNotFoundException(String message) {
        super(message);
    }
}
