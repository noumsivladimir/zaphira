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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@DiscriminatorValue("MERCHANT")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class MerchantUser extends User {

    @Column(nullable = false, length = 200)
    private String businessName;

    @Column(unique = true, length = 50)
    private String businessRegistrationNumber;


    @Column(length = 500)
    private String businessAddress;

    @Column(nullable = false)
    private Boolean isVerifiedMerchant = false;

    @Column
    private LocalDateTime merchantVerifiedAt;

    @Column(nullable = false)
    private String verifiedBy;

    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.valueOf(2.5);

    @Column(precision = 15, scale = 2)
    private BigDecimal totalTransactionVolume = BigDecimal.ZERO;

    @Column
    private Integer totalTransactions = 0;

    @Column(nullable = false)
    private Boolean canAcceptPayments = false;

    @Override
    public RoleType getRoleType() {
        return RoleType.MERCHANT;
    }

//    @Override
    public Set<PermissionType> getPermissions() {
        return Set.of(
                PermissionType.VIEW_OWN_PROFILE,
                PermissionType.UPDATE_OWN_PROFILE,
                PermissionType.VIEW_OWN_WALLET,
                PermissionType.ACCEPT_PAYMENTS,
                PermissionType.DEPOSIT_FUNDS,
                PermissionType.WITHDRAW_FUNDS,
                PermissionType.VIEW_MERCHANT_DASHBOARD,
                PermissionType.MANAGE_MERCHANT_SETTINGS,
                PermissionType.VIEW_MERCHANT_TRANSACTIONS,
                PermissionType.REQUEST_SETTLEMENT,
                PermissionType.VIEW_OWN_TRANSACTIONS,
                PermissionType.UPLOAD_KYC_DOCUMENTS,
                PermissionType.VIEW_OWN_KYC_STATUS
        );
    }

    public boolean canAcceptPayment() {
        return isAccountActive() && isVerifiedMerchant && canAcceptPayments;
    }

    public void incrementTransactionVolume(BigDecimal amount) {
        this.totalTransactionVolume = this.totalTransactionVolume.add(amount);
        this.totalTransactions++;
    }


}
