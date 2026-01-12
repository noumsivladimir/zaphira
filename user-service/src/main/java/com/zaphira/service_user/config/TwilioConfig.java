package com.zaphira.service_user.config;

import com.twilio.Twilio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class TwilioConfig {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @PostConstruct
    public void initTwilio() {
        // Ne pas initialiser Twilio si les credentials ne sont pas fournis
        if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken)) {
            log.warn("Twilio credentials not provided. SMS functionality will be disabled. OTP will be returned in JSON responses only.");
            return;
        }

        try {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully with account SID: {}", accountSid.substring(0, 8) + "...");
        } catch (Exception e) {
            log.error("Failed to initialize Twilio: {}", e.getMessage());
            log.warn("SMS functionality will be disabled. OTP will be returned in JSON responses only.");
        }
    }
}