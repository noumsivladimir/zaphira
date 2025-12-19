package com.zaphira.wallet.models.entities;

import com.zaphira.wallet.models.enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    // Utilisateur ayant la permission
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Niveau de permission
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionLevel level;

    // Permissions spécifiques
    @Column(name = "can_view")
    @Builder.Default
    private Boolean canView = true;

    @Column(name = "can_transact")
    @Builder.Default
    private Boolean canTransact = false;

    @Column(name = "can_manage_accounts")
    @Builder.Default
    private Boolean canManageAccounts = false;

    @Column(name = "can_manage_permissions")
    @Builder.Default
    private Boolean canManagePermissions = false;

    @Column(name = "can_manage_sub_wallets")
    @Builder.Default
    private Boolean canManageSubWallets = false;

    // Limites spécifiques pour ce gestionnaire
    @Column(name = "max_transaction_amount", precision = 19, scale = 4)
    private java.math.BigDecimal maxTransactionAmount;

    // Accordé par (user_id de l'accordeur)
    @Column(name = "granted_by")
    private Long grantedBy;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    // Date d'expiration (optionnel)
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Méthodes utilitaires

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    public boolean hasFullAccess() {
        return PermissionLevel.OWNER.equals(level) || PermissionLevel.FULL_MANAGER.equals(level);
    }
}