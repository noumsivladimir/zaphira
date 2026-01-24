package com.zaphira.notification.service;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.common.model.enums.AccountStatus;
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
    private static final String WALLET_SERVICE_URL = "http://localhost:8083";

    public UserNotificationInfo getUserNotificationInfo(Long userId) {
        try {
            String url = USER_SERVICE_URL + "/api/users/" + userId + "/notification-info";
            log.debug("Fetching user notification info from: {}", url);
            UserNotificationInfo info = restTemplate.getForObject(url, UserNotificationInfo.class);
            if (info != null) {
                log.debug("Successfully retrieved notification info for userId: {}", userId);
            }
            return info;
        } catch (Exception e) {
            log.warn("User service not available, cannot get notification info for userId: {}. Reason: {}", userId, e.getMessage());
            return null;
        }
    }

    public String getUserPhoneNumber(Long userId) {
        try {
            UserNotificationInfo info = getUserNotificationInfo(userId);
            if (info != null && info.getPhoneNumber() != null) {
                log.debug("Phone number found for userId: {}", userId);
                return info.getPhoneNumber();
            }
            log.debug("No phone number available for userId: {}", userId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to get phone number for userId: {}. Reason: {}", userId, e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromWalletNumber(String walletNumber) {
        try {
            String url = WALLET_SERVICE_URL + "/api/wallets/" + walletNumber;
            log.debug("Calling wallet service at: {}", url);
            WalletDTO wallet = restTemplate.getForObject(url, WalletDTO.class);
            if (wallet != null && wallet.getUserId() != null) {
                log.debug("Successfully retrieved userId {} for wallet {}", wallet.getUserId(), walletNumber);
                return wallet.getUserId();
            } else {
                log.warn("Wallet found but userId is null for wallet: {}", walletNumber);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to get userId from wallet service for wallet: {}. Error: {}", walletNumber, e.getMessage());
            return null;
        }
    }

    public static class UserNotificationInfo {
        private String phoneNumber;
        private String email;
        private String firstName;
        private String lastName;
        private AccountStatus accountStatus;

        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public AccountStatus getAccountStatus() { return accountStatus; }
        public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    }
}