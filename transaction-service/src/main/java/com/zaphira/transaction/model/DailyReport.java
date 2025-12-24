package com.zaphira.transaction.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DailyReport - Daily summary metrics for transaction service.
 * 
 * Purpose:
 * - Aggregate daily transaction metrics
 * - Track settlement volumes and success rates
 * - Monitor dispute patterns
 * - Support business analytics
 * 
 * Generated daily via scheduled task at end of day (23:59 UTC)
 * Pre-calculated for performance in reporting queries
 */
@Entity
@Table(name = "daily_reports", indexes = {
    @Index(name = "idx_daily_reports_date", columnList = "report_date DESC"),
    @Index(name = "idx_daily_reports_status", columnList = "is_finalized")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Report date (YYYY-MM-DD)
    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;
    
    // ============================================================
    // TRANSACTION METRICS
    // ============================================================
    
    @Builder.Default
    @Column(name = "total_transactions", nullable = false)
    private Long totalTransactions = 0L;
    
    @Builder.Default
    @Column(name = "total_transaction_volume", precision = 19, scale = 2)
    private BigDecimal totalTransactionVolume = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "successful_transactions", nullable = false)
    private Long successfulTransactions = 0L;
    
    @Builder.Default
    @Column(name = "failed_transactions", nullable = false)
    private Long failedTransactions = 0L;
    
    @Builder.Default
    @Column(name = "pending_transactions", nullable = false)
    private Long pendingTransactions = 0L;
    
    @Builder.Default
    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate = BigDecimal.ZERO;
    
    // ============================================================
    // REVERSAL METRICS
    // ============================================================
    
    @Builder.Default
    @Column(name = "total_reversals", nullable = false)
    private Long totalReversals = 0L;
    
    @Builder.Default
    @Column(name = "total_reversal_volume", precision = 19, scale = 2)
    private BigDecimal totalReversalVolume = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "reversal_rate", precision = 5, scale = 2)
    private BigDecimal reversalRate = BigDecimal.ZERO;
    
    // ============================================================
    // REFUND METRICS
    // ============================================================
    
    @Builder.Default
    @Column(name = "total_refunds", nullable = false)
    private Long totalRefunds = 0L;
    
    @Builder.Default
    @Column(name = "total_refund_volume", precision = 19, scale = 2)
    private BigDecimal totalRefundVolume = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "refund_rate", precision = 5, scale = 2)
    private BigDecimal refundRate = BigDecimal.ZERO;
    
    // ============================================================
    // DISPUTE METRICS
    // ============================================================
    
    @Builder.Default
    @Column(name = "total_disputes", nullable = false)
    private Long totalDisputes = 0L;
    
    @Builder.Default
    @Column(name = "new_disputes", nullable = false)
    private Long newDisputes = 0L;
    
    @Builder.Default
    @Column(name = "resolved_disputes", nullable = false)
    private Long resolvedDisputes = 0L;
    
    @Builder.Default
    @Column(name = "dispute_rate", precision = 5, scale = 2)
    private BigDecimal disputeRate = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "total_disputed_amount", precision = 19, scale = 2)
    private BigDecimal totalDisputedAmount = BigDecimal.ZERO;
    
    // ============================================================
    // SETTLEMENT METRICS
    // ============================================================
    
    @Builder.Default
    @Column(name = "total_settlements", nullable = false)
    private Long totalSettlements = 0L;
    
    @Builder.Default
    @Column(name = "total_settlement_volume", precision = 19, scale = 2)
    private BigDecimal totalSettlementVolume = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "completed_settlements", nullable = false)
    private Long completedSettlements = 0L;
    
    @Builder.Default
    @Column(name = "failed_settlements", nullable = false)
    private Long failedSettlements = 0L;
    
    @Builder.Default
    @Column(name = "settlement_success_rate", precision = 5, scale = 2)
    private BigDecimal settlementSuccessRate = BigDecimal.ZERO;
    
    // ============================================================
    // FEE METRICS
    // ============================================================
    
    @Builder.Default
    @Column(name = "total_fees_collected", precision = 19, scale = 2)
    private BigDecimal totalFeesCollected = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "fx_fees_collected", precision = 19, scale = 2)
    private BigDecimal fxFeesCollected = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "service_fees_collected", precision = 19, scale = 2)
    private BigDecimal serviceFeesCollected = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "average_fee_percentage", precision = 5, scale = 2)
    private BigDecimal averageFeePercentage = BigDecimal.ZERO;
    
    // ============================================================
    // CURRENCY METRICS
    // ============================================================
    
    @Builder.Default
    @Column(name = "unique_currencies", nullable = false)
    private Integer uniqueCurrencies = 0;
    
    @Column(name = "top_currency", length = 3)
    private String topCurrency;
    
    // ============================================================
    // STATUS & TIMESTAMPS
    // ============================================================
    
    @Builder.Default
    @Column(name = "is_finalized", nullable = false)
    private Boolean isFinalized = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;
}
