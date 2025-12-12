package com.zaphira.service_user.model.entities;


import com.zaphira.service_user.model.enums.PermissionType;
import com.zaphira.service_user.model.enums.RoleType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Set;


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
    private String preferredLanguage = "fr";

    @Column(length = 10)
    private String preferredCurrency = "FCFA";

    @Column(nullable = false)
    private boolean notificationsEnabled ;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private Boolean smsNotificationsEnabled = false;

    @Column(nullable = false)
    private Boolean emailNotificationsEnabled = false;

    @Column(nullable = false)
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




    @Override
    public Set<PermissionType> getPermissions() {
        return Set.of(
                PermissionType.VIEW_OWN_PROFILE,
                PermissionType.UPDATE_OWN_PROFILE,
                PermissionType.DELETE_OWN_ACCOUNT,
                PermissionType.VIEW_OWN_WALLET,
                PermissionType.CREATE_WALLET,
            //    PermissionType.TOPUP_WALLET,
                PermissionType.CREATE_TRANSACTION,
                PermissionType.VIEW_OWN_TRANSACTIONS,
         //       PermissionType.CANCEL_OWN_TRANSACTION,
                PermissionType.UPLOAD_KYC_DOCUMENTS,
                PermissionType.VIEW_OWN_KYC_STATUS
        );
    }

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