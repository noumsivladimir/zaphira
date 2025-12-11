package com.zaphira.transaction.service;

public class ScheduledTransactionNotFoundException extends RuntimeException {

    public ScheduledTransactionNotFoundException(Long id) {
        super("Scheduled transaction not found with id=" + id);
    }
}


