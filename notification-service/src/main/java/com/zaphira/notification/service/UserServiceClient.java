package com.zaphira.notification.service;

import com.zaphira.common.dto.WalletDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    
    private static final String USER_SERVICE_URL = "http://localhost:8082";
    private static final String WALLET_SERVICE_URL = "http://localhost:8084";

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
        try {
            UserNotificationInfo info = getUserNotificationInfo(userId);
            return info != null ? info.getTelegramChatId() : null;
        } catch (Exception e) {
            log.warn("User service not available, cannot get Telegram chat ID for user: {}", userId);
            return null; // Retourner null pour utiliser le chat par défaut
        }
    }

    public Long getUserIdFromWalletNumber(String walletNumber) {
        try {
            String url = WALLET_SERVICE_URL + "/api/wallets/" + walletNumber;
            WalletDTO wallet = restTemplate.getForObject(url, WalletDTO.class);
            return wallet != null ? wallet.getUserId() : null;
        } catch (Exception e) {
            log.warn("Wallet service not available, cannot get userId for wallet: {}", walletNumber);
            return null;
        }
    }

    public static class UserNotificationInfo {
        private String phoneNumber;
        private String telegramChatId;
        private String email;
        private String firstName;
        private String lastName;

        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getTelegramChatId() { return telegramChatId; }
        public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
}