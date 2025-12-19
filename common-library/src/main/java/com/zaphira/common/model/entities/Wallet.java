package com.zaphira.common.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_number", nullable = false, length = 8, unique = true)
    private String walletNumber;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "blocked_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal blockedBalance = BigDecimal.ZERO;

    @Column(name = "total_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Column(nullable = true)
    private String currency;

    @Column(name = "status", nullable = true)
    private String status;

    @Column(name = "type", nullable = true)
    private String type;

    @Column(name = "is_primary", nullable = true)
    private Boolean isPrimary;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Column(name = "closed_at", nullable = true)
    private LocalDateTime closedAt;

    @Column(name = "frozen_at", nullable = true)
    private LocalDateTime frozenAt;

    @Column(name = "frozen_by", nullable = true)
    private Long frozenBy;

    @Column(name = "frozen_reason", nullable = true)
    private String frozenReason;

    @Column(name = "daily_limit", nullable = true, precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "daily_spent", nullable = true, precision = 19, scale = 4)
    private BigDecimal dailySpent;

    @Column(name = "monthly_limit", nullable = true, precision = 19, scale = 4)
    private BigDecimal monthlyLimit;

    @Column(name = "monthly_spent", nullable = true, precision = 19, scale = 4)
    private BigDecimal monthlySpent;

    @Column(name = "last_limit_reset", nullable = true)
    private LocalDateTime lastLimitReset;

    @Column(name = "metadata", nullable = true)
    private String metadata;

    @Column(name = "version", nullable = true)
    private Long version;

    private static final SecureRandom RANDOM = new SecureRandom();

    @PostPersist
    private void generateWalletNumber() {
        if (this.walletNumber == null) {
            this.walletNumber = generateSecureWalletNumber(this.id);
        }
    }

    /**
     * Getter compatible pour accéder au balance (pour les services existants)
     * Retourne available_balance ou total_balance selon le contexte
     */
    @Transient
    public BigDecimal getBalance() {
        return this.availableBalance != null ? this.availableBalance : BigDecimal.ZERO;
    }

    /**
     * Setter compatible pour mettre à jour le balance
     */
    public void setBalance(BigDecimal balance) {
        this.availableBalance = balance != null ? balance : BigDecimal.ZERO;
    }

    /**
     * Getter compatible pour accéder à l'état actif (basé sur frozenAt)
     */
    @Transient
    public Boolean getActive() {
        return this.frozenAt == null;
    }

    private String generateSecureWalletNumber(Long id) {
        String idStr = String.valueOf(id);
        int remainingLength = 8 - idStr.length();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < remainingLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        sb.append(idStr);

        return sb.toString();
    }
}

