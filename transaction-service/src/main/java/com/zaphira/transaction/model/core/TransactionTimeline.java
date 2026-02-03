package com.zaphira.transaction.model.core;

import com.zaphira.transaction.model.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TransactionTimeline - LOT 2
 * 
 * Tracks state changes in transaction lifecycle:
 * - PENDING → PROCESSING → COMPLETED
 * - PENDING → FAILED
 * - FAILED → PENDING (retry)
 * - PENDING → CANCELLED
 * 
 * Used for:
 * - Audit trail
 * - Timeline visualization
 * - Performance analysis
 * - Debugging
 */
@Entity
@Table(name = "transaction_timeline", indexes = {
        @Index(name = "idx_timeline_transaction", columnList = "transaction_id"),
        @Index(name = "idx_timeline_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionCore transaction;

    /** Previous status (before change) */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private TransactionStatus previousStatus;

    /** New status (after change) */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20, nullable = false)
    private TransactionStatus newStatus;

    /** Reason for status change */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /** User or system that triggered the change */
    @Column(name = "changed_by", length = 100)
    private String changedBy;

    /** Additional details about the change */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if status changed to completed
     */
    public boolean isCompletedTransition() {
        return newStatus == TransactionStatus.COMPLETED;
    }

    /**
     * Check if status changed to failed
     */
    public boolean isFailedTransition() {
        return newStatus == TransactionStatus.FAILED;
    }

    /**
     * Check if status changed to cancelled
     */
    public boolean isCancelledTransition() {
        return newStatus == TransactionStatus.CANCELLED;
    }

    /**
     * Get transition duration (if previous status exists)
     */
    public String getTransitionDescription() {
        if (previousStatus != null) {
            return previousStatus + " → " + newStatus;
        }
        return "Created as " + newStatus;
    }
}
