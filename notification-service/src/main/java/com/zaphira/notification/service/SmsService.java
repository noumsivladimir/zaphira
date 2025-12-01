package com.zaphira.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    public void sendSms(String phoneNumber, String message) {
        // TODO: Integrate with SMS provider (Twilio, AWS SNS, etc.)
        log.info("SMS sent to {}: {}", phoneNumber, message);
    }

    public void sendTransactionSms(String phoneNumber, String transactionReference, String amount, String status) {
        String message = String.format(
            "Transaction %s: %s %s. Status: %s",
            transactionReference, amount, status
        );
        sendSms(phoneNumber, message);
    }
}

