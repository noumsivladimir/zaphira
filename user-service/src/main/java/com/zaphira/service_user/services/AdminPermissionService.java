package com.zaphira.service_user.services;


import com.zaphira.service_user.model.entities.AdminUser;
import com.zaphira.service_user.model.enums.PermissionType;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AdminPermissionService {

    public Set<PermissionType> resolvePermissions(AdminUser admin) {

        Set<PermissionType> permissions = new HashSet<>();

        // permissions de base
        permissions.add(PermissionType.VIEW_OWN_PROFILE);
        permissions.add(PermissionType.UPDATE_OWN_PROFILE);

        // règles par flags
        if (admin.getCanManageUsers()) {
            permissions.addAll(Set.of(
                    PermissionType.VIEW_ALL_USERS,
                    PermissionType.SUSPEND_USER,
                    PermissionType.ACTIVATE_USER
            ));
        }

        if (admin.getCanApproveTransactions()) {
            permissions.addAll(Set.of(
                    PermissionType.APPROVE_TRANSACTION,
                    PermissionType.REJECT_TRANSACTION
            ));
        }

        if (admin.getCanManageKYC()) {
            permissions.add(PermissionType.MANAGE_KYC);
        }

        // règles par niveau
        switch (admin.getAdminLevel()) {
            case SUPER_ADMIN -> permissions.addAll(superAdminPermissions());
            case SENIOR -> permissions.addAll(seniorPermissions());
            case STANDARD -> {
                // Standard admins only have base permissions
            }
        }

        return permissions;
    }

    private Set<PermissionType> superAdminPermissions() {
        return Set.of(
                PermissionType.MANAGE_ADMINS,
                PermissionType.MANAGE_SYSTEM_CONFIG,
                PermissionType.VIEW_AUDIT_LOGS
        );
    }

    private Set<PermissionType> seniorPermissions() {
        return Set.of(
                PermissionType.VIEW_AUDIT_LOGS
        );
    }
}