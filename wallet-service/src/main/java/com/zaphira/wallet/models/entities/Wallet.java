package com.zaphira.wallet.models.entities;

import com.zaphira.wallet.models.enums.WalletStatus;
import com.zaphira.wallet.models.enums.WalletType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_wallet_number", columnList = "wallet_number", unique = true),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_number", unique = true, nullable = false, length = 8)
    private String walletNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

//    @Column(name = "wallet_name", length = 100)
//    private String walletName;

    // Solde disponible
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance ;

    //List de subWallet

    @ManyToMany
    @JoinTable(
            name = "wallet_subwallet",
            joinColumns = @JoinColumn(
                    name = "wallet_id",
                    referencedColumnName = "id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "subwallet_id",
                    referencedColumnName = "id"
            )
    )
    private List<SubWallet> subWallets = new ArrayList<>();

    // Solde bloqué pour (transactions en attente)
    @Column(name = "blocked_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal blockedBalance = BigDecimal.ZERO;

    // Solde total (calculé)
    @Column(name = "total_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalBalance = BigDecimal.ZERO;

    // Limite journalière
    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    // Limite mensuelle
    @Column(name = "monthly_limit", precision = 19, scale = 4)
    private BigDecimal monthlyLimit;

    // Montant dépensé aujourd'hui
    @Column(name = "daily_spent", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailySpent = BigDecimal.ZERO;

    // Montant dépensé ce mois
    @Column(name = "monthly_spent", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlySpent = BigDecimal.ZERO;

    // Date de réinitialisation des limites
    @Column(name = "last_limit_reset")
    private LocalDateTime lastLimitReset;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = true;

    @Version
    private Long version;

    @Column(name = "frozen_reason")
    private String frozenReason;

    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    @Column(name = "frozen_by")
    private String frozenBy;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WalletStatusHistory> statusHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Métadonnées supplémentaires (JSON)
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // Méthodes utilitaires

    public void addStatusHistory(WalletStatusHistory history) {
        statusHistory.add(history);
        history.setWallet(this);
    }

    public void calculateTotalBalance() {
        this.totalBalance = this.availableBalance.add(this.blockedBalance);
    }

    public boolean hasAvailableBalance(BigDecimal amount) {
        return this.availableBalance.compareTo(amount) >= 0;
    }

    public boolean isActive() {
        return WalletStatus.ACTIVE.equals(this.status);
    }

    public boolean canTransact() {
        return WalletStatus.ACTIVE.equals(this.status);
    }

    public void debit(BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Solde insuffisant");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        calculateTotalBalance();
    }

    public void credit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
        calculateTotalBalance();
    }

    public void blockAmount(BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Solde disponible insuffisant pour bloquer le montant");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.blockedBalance = this.blockedBalance.add(amount);
        calculateTotalBalance();
    }

    public void unblockAmount(BigDecimal amount) {
        if (this.blockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Montant bloqué insuffisant");
        }
        this.blockedBalance = this.blockedBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        calculateTotalBalance();
    }

    public void releaseBlockedAmount(BigDecimal amount) {
        if (this.blockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Montant bloqué insuffisant");
        }
        this.blockedBalance = this.blockedBalance.subtract(amount);
        calculateTotalBalance();
    }
}
