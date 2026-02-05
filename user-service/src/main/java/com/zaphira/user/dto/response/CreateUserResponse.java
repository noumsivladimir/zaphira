package com.zaphira.user.dto.response;

import com.zaphira.user.model.enums.AccountStatus;
import com.zaphira.user.model.enums.AdminLevel;
import com.zaphira.user.model.enums.RoleType;
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
public class CreateUserResponse {

    private Long userId;

    private String walletId;
    private String email;

    private String phoneNumber;
    private String firstName;
    private String lastName;


    // Questions de sécurité
    private LocalDate dateOfBirth;
    private String country;
    private String neighborhood;
    private String registrationDate;
    private AccountStatus accountStatus;
    private RoleType roleType;
    private LocalDateTime createdAt;

    // Regular user specific
    private String preferredLanguage;
    private Boolean notificationsEnabled;

    // Admin user specific
    private String employeeId;
    private AdminLevel adminLevel;

    // Merchant user specific
    private String businessName;
    private String businessType;
    private Boolean isVerifiedMerchant;
}
