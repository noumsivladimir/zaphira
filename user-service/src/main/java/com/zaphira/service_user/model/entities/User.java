package com.zaphira.service_user.model.entities;

import com.zaphira.service_user.model.enums.AccountStatus;
import com.zaphira.service_user.model.enums.PermissionType;
import com.zaphira.service_user.model.enums.RoleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;


@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_phone_Number", columnList = "phone_number"),
        @Index(name = "idx_walletId", columnList = "walletId"),
        @Index(name = "idx_userId", columnList = "user_id"),
        @Index(name = "idx_user_type", columnList = "user_type"),
        @Index(name = "idx_account_status", columnList = "account_status")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

//    private Wallet walletId;

    @Column(unique = true)
    private String walletId;


    @Column(nullable = true, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;


    @Column(nullable = false, length = 255)
    private String pin;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column (nullable = false)
    @Past
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String neighborhood;

    @Column(nullable = true, length = 100)
    private String city;

    @Column(nullable = true, length = 100)
    private String region;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

    @Column(nullable = false)
    private Boolean emailVerified = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean phoneVerified = Boolean.TRUE;

    @Column(nullable = false)
    private Boolean twoFactorEnabled = Boolean.FALSE;

    @Column(length = 100)
    private String twoFactorSecret;

    @Column
    private LocalDateTime lastLoginAt;

    @Column(length = 50)
    private String lastLoginIp;

//    @Column(nullable = false)
    @Column
    private Integer failedLoginAttempts = 0;

    @Column
    private LocalDateTime accountLockedUntil;

    @CreatedDate
//    @Column(nullable = false, updatable = false)
    @Column
    private LocalDateTime registrationDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Abstract methods - to be implemented by subclasses
    public abstract RoleType getRoleType();
    public abstract Set<PermissionType> getPermissions();

    // Common business logic methods
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isAccountActive() {
        return accountStatus == AccountStatus.ACTIVE && !isAccountLocked();
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.accountLockedUntil = LocalDateTime.now().plusHours(1);
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasPermission(PermissionType permission) {
        return getPermissions().contains(permission);
    }
}