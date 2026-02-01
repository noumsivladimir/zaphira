package com.zaphira.common.model.entities;


import com.zaphira.common.model.enums.RoleType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@DiscriminatorValue("REGULAR_USER")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder

public class RegularUser extends User {

    @Column(length = 500)
    private String profilePicture;

    @Column(length = 10)
    @Builder.Default
    private String preferredLanguage = "fr";

    @Column(length = 10)
    @Builder.Default
    private String preferredCurrency = "FCFA";

    @Column(nullable = false)
    @Builder.Default
    private Boolean notificationsEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean smsNotificationsEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNotificationsEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean marketingEmailsEnabled = false;

//    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private KYC kyc;

    @Column
    private LocalDateTime kycSubmittedAt;

    @Column
    private LocalDateTime kycVerifiedAt;

    @Column
    private LocalDateTime lastTransactionAt;

    @Override
    public RoleType getRoleType() {
        return RoleType.USER;
    }

//    @Override
//    public Set<PermissionType> getPermissions() {
//        return Set.of(
//                PermissionType.VIEW_OWN_PROFILE,
//                PermissionType.UPDATE_OWN_PROFILE,
//                PermissionType.DELETE_OWN_ACCOUNT,
//                PermissionType.VIEW_OWN_WALLET,
//                PermissionType.CREATE_WALLET,
//            //    PermissionType.TOPUP_WALLET,
//                PermissionType.CREATE_TRANSACTION,
//                PermissionType.VIEW_OWN_TRANSACTIONS,
//         //       PermissionType.CANCEL_OWN_TRANSACTION,
//                PermissionType.UPLOAD_KYC_DOCUMENTS,
//                PermissionType.VIEW_OWN_KYC_STATUS
//        );
//    }

    public Boolean isKycVerified() {
        return Boolean.TRUE;
    }

//    public boolean isKycVerified() {
//        return kyc != null && kyc.getKycStatus() == KYCStatus.VERIFIED;
//    }
//
//    public boolean isKycPending() {
//        return kyc != null && (kyc.getKycStatus() == KYCStatus.PENDING ||
//                kyc.getKycStatus() == KYCStatus.UNDER_REVIEW);
//    }
//
//    public boolean canPerformTransactions() {
//        return isAccountActive() && isKycVerified();
//    }
}