package com.zaphira.notification.service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.zaphira.notification.config.TwilioProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioSmsService implements SmsService {

    private final TwilioProperties twilioProperties;

    private void initTwilio() {
        if (twilioProperties.getAccountSid() != null && !twilioProperties.getAccountSid().isBlank() 
            && twilioProperties.getAuthToken() != null && !twilioProperties.getAuthToken().isBlank()) {
            try {
                Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
            } catch (Exception e) {
                log.warn("Failed to initialize Twilio: {}", e.getMessage());
            }
        }
    }

    @Override
    public void sendOtpSms(String phoneNumber, String code) {
        String body = String.format("Zaphira - Votre code de vérification est: %s. Ce code expire dans 5 minutes.", code);
        sendSms(phoneNumber, body);
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("Tentative d'envoi SMS - To: {}, From: {}, AccountSid: {}..., Message length: {}",
                phoneNumber,
                twilioProperties.getFromNumber(),
                twilioProperties.getAccountSid() != null ? twilioProperties.getAccountSid().substring(0, 8) : "null",
                message.length());

        if (twilioProperties.getAccountSid() == null || twilioProperties.getAccountSid().isBlank()
            || twilioProperties.getAuthToken() == null || twilioProperties.getAuthToken().isBlank()
            || twilioProperties.getFromNumber() == null || twilioProperties.getFromNumber().isBlank()) {
            log.error("❌ CONFIGURATION TWILIO MANQUANTE:");
            log.error("  - AccountSid: {}", twilioProperties.getAccountSid() != null ? "présent" : "MANQUANT");
            log.error("  - AuthToken: {}", twilioProperties.getAuthToken() != null ? "présent" : "MANQUANT");
            log.error("  - FromNumber: {}", twilioProperties.getFromNumber() != null ? "présent" : "MANQUANT");
            log.error("SMS NON ENVOYÉ - To: {} Message: {}", phoneNumber, message);
            return;
        }

        initTwilio();

        try {
            log.info("📤 Envoi du SMS via Twilio en cours...");
            Message smsMessage = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioProperties.getFromNumber()),
                    message
            ).create();

            log.info("✅ SMS envoyé avec succès à {} - SID: {}", phoneNumber, smsMessage.getSid());
        } catch (ApiException e) {
            log.error("❌ ERREUR TWILIO API - Code: {}, Message: {}", e.getCode(), e.getMessage());
            log.error("Détails Twilio - To: {}, From: {}", phoneNumber, twilioProperties.getFromNumber());
        } catch (Exception e) {
            log.error("❌ ERREUR INATTENDUE lors de l'envoi SMS: {}", e.getMessage(), e);
        }
    }
}
