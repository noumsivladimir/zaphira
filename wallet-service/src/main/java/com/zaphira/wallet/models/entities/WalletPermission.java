package com.zaphira.wallet.models.entities;

import com.zaphira.wallet.models.enums.PermissionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "user_id"}),
        indexes = {
                @Index(name = "idx_wallet_permission_wallet", columnList = "wallet_id"),
                @Index(name = "idx_wallet_permission_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false)
    private PermissionType permissionType;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "max_amount")
    private BigDecimal maxAmount; // Limite pour cette opération

    @Column(name = "daily_limit")
    private BigDecimal dailyLimit;

    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}