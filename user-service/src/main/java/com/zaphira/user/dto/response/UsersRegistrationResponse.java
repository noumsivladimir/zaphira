package com.zaphira.user.dto.response;

import com.zaphira.user.model.enums.AccountStatus;
import com.zaphira.user.model.enums.AdminLevel;
import com.zaphira.user.model.enums.RoleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
public class UsersRegistrationResponse {



    private Long userId;

    private String walletId;
    private String phoneNumber;
    private String firstName;
    private String lastName;

    private String email;

    private LocalDate dateOfBirth;
    private String country;

    private String preferredCurrency;
    private LocalDateTime registrationDate;
    private AccountStatus accountStatus;
    private RoleType roleType;
    private LocalDateTime createdAt;

    // Admin user specific
    private String employeeId;
    private AdminLevel adminLevel;

    // Merchant user specific
    private String businessName;
    private String businessType;
    private Boolean isVerifiedMerchant;
}
