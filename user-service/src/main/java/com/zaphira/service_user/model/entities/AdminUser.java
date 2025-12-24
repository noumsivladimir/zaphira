package com.zaphira.service_user.model.entities;

import com.zaphira.common.model.enums.CameroonRegion;
import com.zaphira.service_user.model.enums.AdminLevel;
import com.zaphira.service_user.model.enums.RoleType;
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


    @Column(unique = true, length = 50)
    private String employeeId;

    @Column(length = 100)
    @Enumerated(EnumType.STRING)
    private CameroonRegion regionCode;

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

    public boolean isSuperAdmin() {
        return adminLevel == AdminLevel.SUPER_ADMIN;
    }

}