package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TransactionTimeline - Complete timeline of transaction lifecycle events
 * Normalized table to separate timestamp tracking from core transaction
 */
@Entity
@Table(name = "transaction_timeline", indexes = {
    @Index(name = "idx_transaction_timeline_completed_at", columnList = "completed_at"),
    @Index(name = "idx_transaction_timeline_failed_at", columnList = "failed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTimeline {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /** Lifecycle timestamps */
    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @Column(name = "pending_at")
    private LocalDateTime pendingAt;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "under_review_at")
    private LocalDateTime underReviewAt;

    @Column(name = "on_hold_at")
    private LocalDateTime onHoldAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /** Audit */
    @Column(name = "last_updated_by", length = 100)
    private String lastUpdatedBy;

    /**
     * Set timestamp based on status
     */
    public void recordStatusChange(String status, String updatedBy) {
        LocalDateTime now = LocalDateTime.now();
        this.lastUpdatedBy = updatedBy;

        switch (status.toUpperCase()) {
            case "PENDING" -> this.pendingAt = now;
            case "PROCESSING" -> this.processingAt = now;
            case "COMPLETED" -> this.completedAt = now;
            case "FAILED" -> this.failedAt = now;
            case "CANCELLED" -> this.cancelledAt = now;
            case "REVERSED" -> this.reversedAt = now;
            case "REFUNDED" -> this.refundedAt = now;
            case "ON_HOLD" -> this.onHoldAt = now;
            case "UNDER_REVIEW" -> this.underReviewAt = now;
            case "EXPIRED" -> this.expiredAt = now;
        }
    }
}
