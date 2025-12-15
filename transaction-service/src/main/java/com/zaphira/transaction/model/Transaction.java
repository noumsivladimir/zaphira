package com.zaphira.transaction.model;

import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.ComplianceStatus;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.common.model.entities.Wallet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @Column(nullable = false)
    private String senderWalletNumber;

    @Column(nullable = false)
    private String receiverWalletNumber;

    // RELATIONSHIP: JPA @ManyToOne relationship to sender Wallet entity
    // Allows accessing sender wallet details and associated user via senderWallet.getUserId()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet_id", nullable = true, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Wallet senderWallet;

    // RELATIONSHIP: JPA @ManyToOne relationship to receiver Wallet entity
    // Allows accessing receiver wallet details and associated user via receiverWallet.getUserId()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet_id", nullable = true, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Wallet receiverWallet;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    // Fees
    @Column(precision = 19, scale = 4)
    private BigDecimal feeAmount;

    private String feeCurrency;

    private String feeType; // e.g. FIXED, PERCENTAGE, TIERED

    private String route;   // e.g. WALLET_INTERNAL, BANK_GATEWAY_X, CARD_NETWORK_Y

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private TransactionChannel channel;

    private String description;

    @Enumerated(EnumType.STRING)
    private AuthorizationMethod authorizationMethod;

    private Boolean authorizationRequired;

    private Integer retryCount;
    private Integer maxRetry;

    private Boolean scheduled;
    private LocalDateTime scheduledFor;

    /**
     * Phone number used for OTP-based transaction authorization
     * Stored for audit trail and verification purposes
     */
    @Column(length = 20)
    private String phoneNumberUsedForAuth;

    /**
     * Authorization method actually used (OTP, BIOMETRIC, PASSWORD, NONE)
     * Audit trail for compliance
     */
    @Column(length = 50)
    private String actualAuthorizationMethod;

    private String metadata;
    private String initiatedBy;
    private String lastUpdatedBy;

    // Compliance / risk
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus;

    private LocalDateTime initiatedAt;
    private LocalDateTime pendingAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime processingAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime reversedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime onHoldAt;
    private LocalDateTime underReviewAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = this.createdAt;
        if (this.reference == null) {
            this.reference = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = TransactionStatus.INITIATED;
        }
        if (this.complianceStatus == null) {
            this.complianceStatus = ComplianceStatus.CLEAR;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        if (this.maxRetry == null) {
            this.maxRetry = 3;
        }
        if (Boolean.TRUE.equals(this.scheduled) && this.scheduledFor == null) {
            this.scheduled = false;
        }
        if (this.initiatedAt == null) {
            this.initiatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void applyStatus(TransactionStatus newStatus) {
        this.status = newStatus;
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case INITIATED -> this.initiatedAt = now;
            case PENDING -> this.pendingAt = now;
            case AUTHORIZED -> this.authorizedAt = now;
            case PROCESSING -> this.processingAt = now;
            case COMPLETED -> this.completedAt = now;
            case FAILED -> this.failedAt = now;
            case CANCELLED -> this.cancelledAt = now;
            case REVERSED -> this.reversedAt = now;
            case REFUNDED -> this.refundedAt = now;
            case EXPIRED -> this.expiredAt = now;
            case ON_HOLD -> this.onHoldAt = now;
            case UNDER_REVIEW -> this.underReviewAt = now;
            default -> {
            }
        }
        this.lastUpdatedAt = now;
    }
}


