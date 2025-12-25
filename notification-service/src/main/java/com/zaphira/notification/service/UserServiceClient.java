package com.zaphira.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    // TODO: Remplacer par les vraies URLs des services
    private static final String USER_SERVICE_URL = "http://user-service:8082";
    private static final String WALLET_SERVICE_URL = "http://wallet-service:8086";

    public UserNotificationInfo getUserNotificationInfo(Long userId) {
        try {
            String url = USER_SERVICE_URL + "/api/users/" + userId + "/notification-info";
            return restTemplate.getForObject(url, UserNotificationInfo.class);
        } catch (Exception e) {
            log.error("Failed to get user notification info for userId: {}", userId, e);
            return null;
        }
    }

    public String getUserPhoneNumber(Long userId) {
        UserNotificationInfo info = getUserNotificationInfo(userId);
        return info != null ? info.getPhoneNumber() : null;
    }

    public String getUserTelegramChatId(Long userId) {
        UserNotificationInfo info = getUserNotificationInfo(userId);
        return info != null ? info.getTelegramChatId() : null;
    }

    public static class UserNotificationInfo {
        private String phoneNumber;
        private String telegramChatId;
        private String email;

        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getTelegramChatId() { return telegramChatId; }
        public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}