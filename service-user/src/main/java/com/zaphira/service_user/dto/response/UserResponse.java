package com.zaphira.service_user.dto.response;

import com.zaphira.service_user.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long userId;
    private String emaxil;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String country;
    private String neighbourhood;
    private String city;
    private String region;
    private AccountStatus accountStatus;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean twoFactorEnabled;
    private RoleType roleType;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Regular user specific
    private String profilePicture;
    private String preferredLanguage;
    private Boolean notificationsEnabled;
    private Boolean kycVerified;

    // Admin user specific
    private String employeeId;
    private AdminLevel adminLevel;

    // Merchant user specific
    private String businessName;
    private String businessType;
    private Boolean isVerifiedMerchant;
}