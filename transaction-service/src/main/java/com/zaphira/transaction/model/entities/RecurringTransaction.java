package com.zaphira.transaction.model.entities;

import com.zaphira.transaction.model.enums.RecurrenceFrequency;
import com.zaphira.transaction.model.enums.RecurringStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RecurringTransaction - Represents a recurring payment/transfer.
 * 
 * Use cases:
 * - Subscription payments (monthly Netflix, Spotify)
 * - Salary payouts (bi-weekly, monthly)
 * - Rent payments (monthly)
 * - Savings auto-transfer (weekly)
 * 
 * Workflow:
 * 1. PENDING → scheduled for next execution
 * 2. ACTIVE → currently executing
 * 3. PAUSED → temporarily stopped by user
 * 4. COMPLETED → end date reached or max executions
 * 5. CANCELLED → user cancelled
 * 6. FAILED → too many consecutive failures
 */
@Entity
@Table(name = "recurring_transactions", indexes = {
        @Index(name = "idx_recurring_user", columnList = "user_id"),
        @Index(name = "idx_recurring_status", columnList = "status"),
        @Index(name = "idx_recurring_next_run", columnList = "next_run_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who created this recurring transaction.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Status: PENDING, ACTIVE, PAUSED, COMPLETED, CANCELLED, FAILED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RecurringStatus status = RecurringStatus.PENDING;

    /**
     * Frequency: DAILY, WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY, YEARLY.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    /**
     * Sender wallet number.
     */
    @Column(name = "sender_wallet_number", nullable = false, length = 50)
    private String senderWalletNumber;

    /**
     * Receiver wallet number.
     */
    @Column(name = "receiver_wallet_number", nullable = false, length = 50)
    private String receiverWalletNumber;

    /**
     * Amount to transfer each time.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Currency code (XOF, EUR, etc).
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * Optional description for each transaction.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Start date - first execution.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * Optional end date - recurring stops after this date.
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * Next scheduled execution time.
     */
    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    /**
     * Last execution time.
     */
    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    /**
     * Total number of successful executions.
     */
    @Column(name = "execution_count", nullable = false)
    @Builder.Default
    private Integer executionCount = 0;

    /**
     * Optional max executions - recurring stops after this many successes.
     */
    @Column(name = "max_executions")
    private Integer maxExecutions;

    /**
     * Number of consecutive failures.
     */
    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private Integer consecutiveFailures = 0;

    /**
     * Max consecutive failures before marking as FAILED.
     */
    @Column(name = "max_consecutive_failures", nullable = false)
    @Builder.Default
    private Integer maxConsecutiveFailures = 3;

    /**
     * Last error message.
     */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /**
     * Reference to last executed transaction.
     */
    @Column(name = "last_transaction_reference", length = 100)
    private String lastTransactionReference;

    /**
     * Notification email (optional).
     */
    @Column(name = "notification_email", length = 255)
    private String notificationEmail;

    /**
     * Send notification on success.
     */
    @Column(name = "notify_on_success", nullable = false)
    @Builder.Default
    private Boolean notifyOnSuccess = true;

    /**
     * Send notification on failure.
     */
    @Column(name = "notify_on_failure", nullable = false)
    @Builder.Default
    private Boolean notifyOnFailure = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.nextRunAt == null) {
            this.nextRunAt = this.startDate;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if recurring is active and ready to execute.
     */
    public boolean isReadyToExecute() {
        return (status == RecurringStatus.PENDING || status == RecurringStatus.ACTIVE)
                && nextRunAt != null
                && nextRunAt.isBefore(LocalDateTime.now())
                && (endDate == null || nextRunAt.isBefore(endDate))
                && (maxExecutions == null || executionCount < maxExecutions);
    }

    /**
     * Check if recurring should be marked as completed.
     */
    public boolean shouldComplete() {
        return (endDate != null && LocalDateTime.now().isAfter(endDate))
                || (maxExecutions != null && executionCount >= maxExecutions);
    }

    /**
     * Check if recurring should be marked as failed due to too many consecutive failures.
     */
    public boolean shouldMarkAsFailed() {
        return consecutiveFailures >= maxConsecutiveFailures;
    }
}
