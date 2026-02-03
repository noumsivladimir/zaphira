package com.zaphira.transaction.model.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TransactionMetadata - LOT 2
 * 
 * Stores additional transaction metadata:
 * - Failure reason
 * - Retry count
 * - Client IP
 * - User agent
 * - Custom metadata (JSON)
 * 
 * Used for:
 * - Error tracking
 * - Audit trails
 * - Analytics
 * - Debugging
 */
@Entity
@Table(name = "transaction_metadata", indexes = {
        @Index(name = "idx_metadata_transaction", columnList = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionMetadata {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "transaction_id")
    private TransactionCore transaction;

    /** Failure reason (if transaction failed) */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Number of retry attempts */
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /** Maximum retry attempts allowed */
    @Column(name = "max_retry_attempts")
    @Builder.Default
    private Integer maxRetryAttempts = 3;

    /** Client IP address */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /** User agent string */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** Device information */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /** Additional custom metadata (JSON format) */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /** Notes or comments */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if retry is allowed
     */
    public boolean canRetry() {
        return retryCount < maxRetryAttempts;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Check if max retries reached
     */
    public boolean maxRetriesReached() {
        return retryCount >= maxRetryAttempts;
    }
}
