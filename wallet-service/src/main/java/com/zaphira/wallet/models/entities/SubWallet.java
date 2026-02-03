package com.zaphira.wallet.models.entities;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.wallet.models.enums.SubWalletType;
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

@Entity
@Table(name = "sub_wallets", indexes = {
        @Index(name = "idx_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_account_number", columnList = "account_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

//    @ManyToOne
//    @JoinColumn(name = "wallet_id") // <-- clé étrangère réelle dans la table sub_wallet
//    private Wallet wallet;

    @ManyToMany(mappedBy = "subWallets")
    @Builder.Default
    private List<Wallet> managingWallets = new ArrayList<>();

    @OneToMany(mappedBy = "subWallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WalletSubWallet> walletSubWallets = new ArrayList<>();


    @Column(name = "account_number", unique = true, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubWalletType type; // CHECKING, SAVINGS, BUSINESS, INVESTMENT, ESCROW

    @Column(name = "sub_wallet_name", length = 100)
    private String subWalletName; // Nom personnalisé par l'utilisateur

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Currency currency = Currency.XAF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    // Soldes
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "blocked_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal blockedBalance = BigDecimal.ZERO;

    @Column(name = "total_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalBalance = BigDecimal.ZERO;

    // Limites spécifiques au compte
    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    private BigDecimal monthlyLimit;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    // Version pour optimistic locking
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    // Méthodes utilitaires
    public void calculateTotalBalance() {
        this.totalBalance = this.availableBalance.add(this.blockedBalance);
    }

    public boolean hasAvailableBalance(BigDecimal amount) {
        return this.availableBalance.compareTo(amount) >= 0;
    }

    public void credit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
        calculateTotalBalance();
    }

    public void debit(BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Solde insuffisant");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        calculateTotalBalance();
    }

    public void blockAmount(BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Solde disponible insuffisant");
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

    public List<Wallet> getManagingWallets() {
        if (managingWallets == null) {
            managingWallets = new ArrayList<>();
        }
        return managingWallets;
    }

    public void setManagingWallets(List<Wallet> managingWallets) {
        this.managingWallets = managingWallets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletSummary {
        private Long id;
        private String walletNumber;
        private WalletType type;
    }
}