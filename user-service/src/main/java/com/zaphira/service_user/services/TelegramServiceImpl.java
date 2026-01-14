package com.zaphira.service_user.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class TelegramServiceImpl implements TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String defaultChatId;

    private final RestTemplate restTemplate;

    public TelegramServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendMessage(String chatId, String message) {
        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            String requestBody = String.format("{\"chat_id\":\"%s\",\"text\":\"%s\",\"parse_mode\":\"HTML\"}",
                    chatId, message.replace("\"", "\\\""));

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Message sent successfully to Telegram chat: {}", chatId);
            } else {
                log.error("Failed to send message to Telegram. Status: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Error sending message to Telegram chat {}: {}", chatId, e.getMessage());
        }
    }

    @Override
    public void sendOtpMessage(String chatId, String otpCode) {
        String message = String.format(
            "🔐 <b>Zaphira - Code de vérification</b>\n\n" +
            "Votre code OTP : <code>%s</code>\n\n" +
            "⚠️ Ce code expire dans 5 minutes.\n" +
            "🔒 Ne partagez ce code avec personne.",
            otpCode
        );

        sendMessage(chatId, message);
    }
}