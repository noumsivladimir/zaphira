package com.zaphira.transaction.model.core;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionCore - Transaction entity (LOT 1 + LOT 2)
 * 
 * LOT 1: Basic operations
 * - P2P Transfer, Deposit, Withdrawal
 * 
 * LOT 2 Extensions:
 * ✅ TransactionFees (OneToOne)
 * ✅ TransactionMetadata (OneToOne)
 * ✅ TransactionTimeline (OneToMany)
 * ✅ Merchant Payment support
 * ✅ Cancel & Retry functionality
 */
@Entity
@Table(name = "transactions_core", indexes = {
        @Index(name = "idx_core_reference", columnList = "reference", unique = true),
        @Index(name = "idx_core_sender", columnList = "sender_wallet_id"),
        @Index(name = "idx_core_receiver", columnList = "receiver_wallet_id"),
        @Index(name = "idx_core_status", columnList = "status"),
        @Index(name = "idx_core_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence unique de transaction */
    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    /** Portefeuilles impliqués */
    @Column(name = "sender_wallet_id")
    private Long senderWalletId;

    @Column(name = "receiver_wallet_id")
    private Long receiverWalletId;

    /** Montant */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** Type de transaction */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;  // TRANSFER, DEPOSIT, WITHDRAWAL

    /** Statut */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;  // PENDING, COMPLETED, FAILED

    /** Description optionnelle */
    @Column(length = 255)
    private String description;

    /** LOT 2: Fee amount (for quick access without join) */
    @Column(name = "fee_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    /** LOT 2: Total amount (amount + fees) */
    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    /** LOT 2: Relationships to extended entities */
    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TransactionFees fees;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TransactionMetadata metadata;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<TransactionTimeline> timeline;

    /** Timestamps */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == TransactionStatus.CANCELLED;
    }

    public void markCancelled() {
        this.status = TransactionStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Calculate total amount (amount + fees)
     */
    public void calculateTotalAmount() {
        BigDecimal amt = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal fee = feeAmount != null ? feeAmount : BigDecimal.ZERO;
        this.totalAmount = amt.add(fee);
    }

    /**
     * Check if transaction can be cancelled
     */
    public boolean canBeCancelled() {
        return status == TransactionStatus.PENDING;
    }

    /**
     * Check if transaction can be retried
     */
    public boolean canBeRetried() {
        return status == TransactionStatus.FAILED &&
               metadata != null &&
               metadata.canRetry();
    }

    /**
     * Get failure reason from metadata
     */
    public String getFailureReason() {
        return metadata != null ? metadata.getFailureReason() : null;
    }

    /**
     * Set failure reason in metadata
     */
    public void setFailureReason(String reason) {
        if (metadata == null) {
            metadata = TransactionMetadata.builder()
                    .transaction(this)
                    .build();
        }
        metadata.setFailureReason(reason);
    }

    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    public boolean isProcessing() {
        return status == TransactionStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = TransactionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}
