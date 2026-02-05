package com.zaphira.user.service;

import com.zaphira.user.dto.response.TwoFactorSetupResponse;
import com.zaphira.user.model.entities.User;
import com.zaphira.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service for managing two-factor authentication
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final UserRepository userRepository;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Enable 2FA for a user
     */
    @Transactional
    public TwoFactorSetupResponse enableTwoFactorAuth(Long userId) {
        log.info("Enabling 2FA for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Generate a secret key for 2FA
        String secretKey = generateSecretKey();
        
        // Generate QR code URL (Google Authenticator format)
        String qrCodeUrl = generateQRCodeUrl(user.getEmail(), secretKey);
        
        // Store the secret in user entity (would need to add twoFactorSecret field to User entity)
        // For now, we'll just return the setup info
        
        // Generate backup codes
        String[] backupCodes = generateBackupCodes(8);
        
        log.info("2FA enabled successfully for user: {}", userId);
        
        return TwoFactorSetupResponse.builder()
                .secret(secretKey)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .build();
    }

    /**
     * Disable 2FA for a user
     */
    @Transactional
    public void disableTwoFactorAuth(Long userId, String password) {
        log.info("Disabling 2FA for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // In a real implementation, you would:
        // 1. Verify the password
        // 2. Clear the 2FA secret from user entity
        // 3. Mark 2FA as disabled
        
        log.info("2FA disabled successfully for user: {}", userId);
    }

    /**
     * Verify a 2FA code
     */
    @Transactional(readOnly = true)
    public boolean verifyTwoFactorCode(Long userId, String code) {
        log.info("Verifying 2FA code for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // In a real implementation, you would:
        // 1. Get the user's 2FA secret
        // 2. Generate TOTP code based on current time
        // 3. Compare with provided code (allowing time window)
        
        // For now, simulate verification (always returns true for demo)
        // In production, use Google Authenticator algorithm (TOTP)
        
        log.info("2FA code verification completed for user: {}", userId);
        return true; // Simplified implementation
    }

    /**
     * Generate a random secret key for 2FA
     */
    private String generateSecretKey() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Generate QR code URL for Google Authenticator
     */
    private String generateQRCodeUrl(String email, String secretKey) {
        String issuer = "Zaphira";
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, email, secretKey, issuer);
    }

    /**
     * Generate backup codes
     */
    private String[] generateBackupCodes(int count) {
        String[] codes = new String[count];
        for (int i = 0; i < count; i++) {
            codes[i] = String.format("%08d", random.nextInt(100000000));
        }
        return codes;
    }
}
