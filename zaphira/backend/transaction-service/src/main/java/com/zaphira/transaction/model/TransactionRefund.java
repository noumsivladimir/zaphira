package com.zaphira.transaction.model;

import com.zaphira.transaction.model.enums.RefundStatus;
import com.zaphira.transaction.model.enums.RefundType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionRefund - Record of transaction refunds.
 * 
 * Purpose:
 * - Track partial and full refunds
 * - Maintain refund history per transaction
 * - Support multiple refunds for same transaction
 * - Audit trail for refund process
 * 
 * Business Rules:
 * - A transaction can have multiple partial refunds
 * - Total refunded amount cannot exceed original transaction amount
 * - Refund can only be initiated for COMPLETED transactions
 * - Refund status: PENDING → PROCESSING → COMPLETED/FAILED
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
@Entity
@Table(name = "transaction_refunds", indexes = {
    @Index(name = "idx_refunds_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_refunds_status", columnList = "status"),
    @Index(name = "idx_refunds_created_at", columnList = "created_at"),
    @Index(name = "idx_refunds_reference", columnList = "refund_reference", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRefund {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique refund reference number
     * Format: REF{YYYYMMDD}{6-digit random}
     * Example: REF20260121123456
     */
    @Column(name = "refund_reference", nullable = false, unique = true, length = 20)
    private String refundReference;
    
    /**
     * Original transaction being refunded
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    /**
     * Refund type: FULL or PARTIAL
     */
    @Column(name = "refund_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RefundType refundType;
    
    /**
     * Refund amount (must be <= original transaction amount)
     */
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;
    
    /**
     * Refund status: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RefundStatus status;
    
    /**
     * Reason for refund
     */
    @Column(name = "reason", length = 500)
    private String reason;
    
    /**
     * User ID who initiated the refund
     */
    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;
    
    /**
     * User ID who approved the refund (if required)
     */
    @Column(name = "approved_by")
    private String approvedBy;
    
    /**
     * Timestamp when refund was initiated
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when refund was processed
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    /**
     * Timestamp when refund was approved
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    /**
     * Timestamp when refund was completed
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * Error message if refund failed
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    /**
     * Additional notes or comments
     */
    @Column(name = "notes", length = 1000)
    private String notes;
    
    /**
     * External reference (from payment processor, if applicable)
     */
    @Column(name = "external_reference", length = 100)
    private String externalReference;
    
    /**
     * Last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this is a full refund
     */
    @Transient
    public boolean isFullRefund() {
        return refundType == RefundType.FULL;
    }
    
    /**
     * Check if this is a partial refund
     */
    @Transient
    public boolean isPartialRefund() {
        return refundType == RefundType.PARTIAL;
    }
    
    /**
     * Check if refund is completed
     */
    @Transient
    public boolean isCompleted() {
        return status == RefundStatus.COMPLETED;
    }
    
    /**
     * Check if refund is pending
     */
    @Transient
    public boolean isPending() {
        return status == RefundStatus.PENDING || status == RefundStatus.PROCESSING;
    }
}
