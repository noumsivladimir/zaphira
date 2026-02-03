package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RecurringTransactionExecution - Tracks each execution of a recurring transaction.
 * 
 * Provides:
 * - Audit trail of all executions
 * - Success/failure tracking
 * - Link to actual transaction created
 * - Error messages for failed executions
 */
@Entity
@Table(name = "recurring_transaction_executions", indexes = {
        @Index(name = "idx_execution_recurring", columnList = "recurring_transaction_id"),
        @Index(name = "idx_execution_status", columnList = "execution_status"),
        @Index(name = "idx_execution_date", columnList = "executed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the recurring transaction.
     */
    @Column(name = "recurring_transaction_id", nullable = false)
    private Long recurringTransactionId;

    /**
     * Execution status: SUCCESS, FAILED, SKIPPED.
     */
    @Column(name = "execution_status", nullable = false, length = 20)
    private String executionStatus;

    /**
     * Transaction reference if successful.
     */
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    /**
     * Amount transferred (may differ from recurring if variable).
     */
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Scheduled execution time.
     */
    @Column(name = "scheduled_for", nullable = false)
    private LocalDateTime scheduledFor;

    /**
     * Actual execution time.
     */
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    /**
     * Execution duration in milliseconds.
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Error message if failed.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Retry count for this execution.
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Notification sent flag.
     */
    @Column(name = "notification_sent", nullable = false)
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.executedAt == null) {
            this.executedAt = LocalDateTime.now();
        }
    }
}
