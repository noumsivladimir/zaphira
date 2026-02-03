package com.zaphira.service_user.model.entities;

import com.zaphira.service_user.model.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = true, length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OtpPurpose purpose;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean consumed = false;

    @Column(nullable = false, length = 255)
    private String code;

    @Column(nullable = true, length = 255)
    private String email;

    @Column(name = "phone_number", nullable = true, length = 255)
    private String phoneNumber;

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * Vérifie si l'OTP a expiré
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Vérifie si on peut encore faire une tentative
     */
    public boolean canAttempt() {
        return attempts < 3; // Maximum 3 tentatives
    }

  
}