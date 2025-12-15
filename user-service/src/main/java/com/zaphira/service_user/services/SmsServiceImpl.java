package com.zaphira.service_user.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Override
    public void sendSms(String phoneNumber, String message) {
        // TODO: Intégrer avec un provider SMS (Twilio, Orange SMS API, etc.)
        log.info("Sending SMS to {}: {}", phoneNumber, message);

        // Exemple avec Twilio (décommenter et configurer)
        /*
        Twilio.init(accountSid, authToken);
        Message.creator(
            new PhoneNumber(phoneNumber),
            new PhoneNumber(twilioPhoneNumber),
            message
        ).create();
        */
    }
}