package com.zaphira.common.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing OTP tokens for various purposes.
 * Used for transaction authorization, PIN reset, etc.
 */
@Entity
@Table(name = "otp_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The OTP code sent to the user
     */
    @Column(nullable = false, length = 6)
    private String code;

    /**
     * Phone number associated with this OTP
     */
    @Column(nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Purpose of the OTP (e.g., PIN_RESET, TRANSACTION_AUTH)
     */
    @Column(nullable = false, length = 50)
    private String purpose;

    /**
     * Number of failed attempts to verify this OTP
     */
    @Column(nullable = false)
    private Integer attempts;

    /**
     * Whether this OTP has been used
     */
    @Column(nullable = false)
    private Boolean used;

    /**
     * Timestamp when OTP expires
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Timestamp when OTP was verified
     */
    @Column
    private LocalDateTime verifiedAt;

    /**
     * Additional context data (e.g., transaction ID, user ID)
     */
    @Column(length = 255)
    private String context;

    /**
     * Check if OTP is still valid (not expired and not used)
     */
    public boolean isValid() {
        return !this.used && LocalDateTime.now().isBefore(this.expiresAt);
    }

    /**
     * Check if OTP has exceeded maximum attempts
     */
    public boolean isMaxAttemptsExceeded(int maxAttempts) {
        return this.attempts >= maxAttempts;
    }

    /**
     * Mark OTP as verified
     */
    public void markAsVerified() {
        this.used = true;
        this.verifiedAt = LocalDateTime.now();
    }
}
