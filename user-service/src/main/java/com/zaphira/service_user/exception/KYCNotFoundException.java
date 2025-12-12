package com.zaphira.service_user.exception;

public class KYCNotFoundException extends RuntimeException {
    public KYCNotFoundException(String message) {
        super(message);
    }
}