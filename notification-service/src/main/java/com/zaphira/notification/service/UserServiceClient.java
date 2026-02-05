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

    
    private static final String USER_SERVICE_URL = "http://user-service";
    private static final String WALLET_SERVICE_URL = "http://wallet-service";

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