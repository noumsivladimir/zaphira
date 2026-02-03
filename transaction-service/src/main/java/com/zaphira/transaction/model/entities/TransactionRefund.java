package com.zaphira.transaction.model.entities;

import com.zaphira.transaction.model.enums.RefundType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionRefund - Refund tracking and management
 * Links original transaction to its refund transaction
 */
@Entity
@Table(name = "transaction_refunds", indexes = {
    @Index(name = "idx_transaction_refunds_original_id", columnList = "original_transaction_id"),
    @Index(name = "idx_transaction_refunds_refund_id", columnList = "refund_transaction_id"),
    @Index(name = "idx_transaction_refunds_reference", columnList = "refund_reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Original transaction being refunded */
    @Column(name = "original_transaction_id", nullable = false)
    private Long originalTransactionId;

    /** The refund transaction (if created) */
    @Column(name = "refund_transaction_id")
    private Long refundTransactionId;

    @Column(name = "refund_reference", unique = true, length = 100)
    private String refundReference;

    /** Refund amounts */
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal refundAmount;

    @Column(name = "refund_fees", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal refundFees = BigDecimal.ZERO;

    @Column(name = "total_refund_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalRefundAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    /** Refund details */
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false, length = 20)
    private RefundType refundType;

    @Column(length = 500)
    private String reason;

    /** Who performed the refund */
    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "performed_by_role", length = 50)
    private String performedByRole;

    /** Status */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /** Audit */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (totalRefundAmount == null && refundAmount != null) {
            this.totalRefundAmount = refundAmount.add(
                refundFees != null ? refundFees : BigDecimal.ZERO
            );
        }
    }
}
