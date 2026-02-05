package com.zaphira.user.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String twilioPhoneNumber;

    private boolean twilioEnabled = false;

    @PostConstruct
    public void initTwilio() {
        if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken) || !StringUtils.hasText(twilioPhoneNumber)) {
            log.warn("Twilio credentials not provided. SMS functionality will be disabled.");
            twilioEnabled = false;
            return;
        }

        try {
            Twilio.init(accountSid, authToken);
            twilioEnabled = true;
            log.info("Twilio initialized successfully with account SID: {}", accountSid.substring(0, 8) + "...");
        } catch (Exception e) {
            log.error("Failed to initialize Twilio: {}", e.getMessage());
            twilioEnabled = false;
        }
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!twilioEnabled) {
            log.warn("SMS not sent - Twilio not configured. Phone: {}, Message: {}", phoneNumber, message);
            return;
        }

        try {
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                message
            ).create();

            log.info("SMS sent successfully to {} with SID: {}", phoneNumber, twilioMessage.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }
}
