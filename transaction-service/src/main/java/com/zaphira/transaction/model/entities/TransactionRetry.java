package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * TransactionRetry - Retry logic and failure tracking
 * Normalized table to separate retry mechanism from core transaction
 */
@Entity
@Table(name = "transaction_retry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRetry {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retry")
    @Builder.Default
    private Integer maxRetry = 3;

    @Column(name = "last_failure_reason", columnDefinition = "TEXT")
    private String lastFailureReason;

    /** Audit */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Increment retry count and record failure reason
     */
    public void recordFailure(String reason) {
        this.retryCount++;
        this.lastFailureReason = reason;
    }

    /**
     * Check if max retries exceeded
     */
    public boolean isMaxRetriesExceeded() {
        return this.retryCount >= this.maxRetry;
    }
}