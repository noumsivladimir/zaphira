package com.zaphira.common.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;


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
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_number", nullable = false, length = 8, unique = true)
    private String walletNumber;

    // Balance fields (updated from single balance field)
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance;

    @Column(name = "blocked_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal blockedBalance = BigDecimal.ZERO;

    @Column(name = "total_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "type", length = 20)
    private String type;

    // RELATIONSHIP: Reference to User entity in auth-service
    // Cross-service relationship (User is in separate auth-service module)
    // This Long ID represents the userId from auth-service User entity
    // Can be used to fetch user details via inter-service Feign client call
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Frozen status
    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    @Column(name = "frozen_by")
    private Long frozenBy;

    @Column(name = "frozen_reason")
    private String frozenReason;

    // Limits
    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "daily_spent", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailySpent = BigDecimal.ZERO;

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    private BigDecimal monthlyLimit;

    @Column(name = "monthly_spent", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlySpent = BigDecimal.ZERO;

    @Column(name = "last_limit_reset")
    private LocalDateTime lastLimitReset;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    private static final SecureRandom RANDOM = new SecureRandom();

    @PostPersist
    private void generateWalletNumber() {
        if (this.walletNumber == null) {
            this.walletNumber = generateSecureWalletNumber(this.id);
        }
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

    // Backward compatibility: balance property returns availableBalance
    public BigDecimal getBalance() {
        return this.availableBalance != null ? this.availableBalance : BigDecimal.ZERO;
    }

    public void setBalance(BigDecimal balance) {
        this.availableBalance = balance;
    }
}

