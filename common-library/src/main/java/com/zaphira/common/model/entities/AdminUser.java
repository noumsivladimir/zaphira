package com.zaphira.common.model.entities;

import com.zaphira.common.model.enums.AdminLevel;
import com.zaphira.common.model.enums.RoleType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("ADMIN")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AdminUser extends User {


    @Column(unique = true, length = 20)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AdminLevel adminLevel = AdminLevel.STANDARD;

    @Column
    private LocalDateTime lastAdminActionAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canApproveTransactions = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canManageUsers = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canAccessReports = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canManageRoles = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canManageKYC = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canViewAuditLogs = false;

    @Column
    private Long supervisorId;

    @Override
    public RoleType getRoleType() {
        return RoleType.ADMIN;
    }

//    @Override
//    public Set<PermissionType> getPermissions() {
//        Set<PermissionType> permissions = new HashSet<>();
//
//        permissions.addAll(Set.of(
//                PermissionType.VIEW_OWN_PROFILE,
//                PermissionType.UPDATE_OWN_PROFILE
//        ));
//
//        if (canManageUsers) {
//            permissions.addAll(Set.of(
//                    PermissionType.VIEW_ALL_USERS,
//                    PermissionType.VIEW_USER_DETAILS,
//                    PermissionType.SUSPEND_USER,
//                    PermissionType.ACTIVATE_USER
//            ));
//        }
//
//        if (canApproveTransactions) {
//            permissions.addAll(Set.of(
//                    PermissionType.VIEW_ALL_TRANSACTIONS,
//                    PermissionType.VIEW_TRANSACTION_DETAILS,
//                    PermissionType.APPROVE_TRANSACTION,
//                    PermissionType.REJECT_TRANSACTION,
//                    PermissionType.REFUND_TRANSACTION
//            ));
//        }
//
//        if (canManageKYC) {
//            permissions.addAll(Set.of(
//                    PermissionType.MANAGE_KYC,
//                    PermissionType.APPROVE_KYC,
//                    PermissionType.REJECT_KYC,
//                    PermissionType.VIEW_ALL_KYC
//            ));
//        }
//
//        if (canAccessReports) {
//            permissions.addAll(Set.of(
//                    PermissionType.GENERATE_REPORTS,
//                    PermissionType.VIEW_ANALYTICS
//            ));
//        }
//
//        if (adminLevel == AdminLevel.SUPER_ADMIN) {
//            permissions.addAll(Set.of(
//                    PermissionType.DELETE_USER,
//                    PermissionType.CREATE_USER,
//                    PermissionType.UPDATE_USER,
//                    PermissionType.MANAGE_ADMINS,
//                    PermissionType.MANAGE_ROLES,
//                    PermissionType.MANAGE_PERMISSIONS,
//                    PermissionType.VIEW_AUDIT_LOGS,
//                    PermissionType.MANAGE_SYSTEM_CONFIG
//            ));
//        } else if (adminLevel == AdminLevel.SENIOR) {
//            permissions.addAll(Set.of(
//                    PermissionType.UPDATE_USER,
//                    PermissionType.VIEW_AUDIT_LOGS
//            ));
//        }
//
//        if (canViewAuditLogs) {
//            permissions.add(PermissionType.VIEW_AUDIT_LOGS);
//        }
//
//        if (canManageRoles) {
//            permissions.addAll(Set.of(
//                    PermissionType.MANAGE_ROLES,
//                    PermissionType.MANAGE_PERMISSIONS
//            ));
//        }
//
//        return permissions;
//    }

    public boolean isSuperAdmin() {
        return adminLevel == AdminLevel.SUPER_ADMIN;
    }

//    public boolean canPerformAction(PermissionType permission) {
//        return getPermissions().contains(permission);
//    }
}