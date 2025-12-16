package com.zaphira.transaction.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * UserAnalytics - Aggregated user-level transaction analytics.
 * 
 * Purpose:
 * - Track individual user transaction activity
 * - Monitor user spending patterns
 * - Support user behavioral analytics
 * - Enable personalized insights
 * 
 * Updated daily via scheduled task
 */
@Entity
@Table(name = "user_analytics", indexes = {
    @Index(name = "idx_user_analytics_user_id", columnList = "user_id"),
    @Index(name = "idx_user_analytics_date", columnList = "analytics_date DESC"),
    @Index(name = "idx_user_analytics_type", columnList = "user_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "analytics_date", nullable = false)
    private LocalDate analyticsDate;
    
    // CUSTOMER or MERCHANT
    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;
    
    // ============================================================
    // TRANSACTION ACTIVITY
    // ============================================================
    
    @Column(name = "transaction_count", nullable = false)
    private Long transactionCount = 0L;
    
    @Column(name = "transaction_volume", precision = 19, scale = 2)
    private BigDecimal transactionVolume = BigDecimal.ZERO;
    
    @Column(name = "successful_transactions", nullable = false)
    private Long successfulTransactions = 0L;
    
    @Column(name = "failed_transactions", nullable = false)
    private Long failedTransactions = 0L;
    
    @Column(name = "average_transaction_amount", precision = 19, scale = 2)
    private BigDecimal averageTransactionAmount = BigDecimal.ZERO;
    
    @Column(name = "max_transaction_amount", precision = 19, scale = 2)
    private BigDecimal maxTransactionAmount = BigDecimal.ZERO;
    
    @Column(name = "min_transaction_amount", precision = 19, scale = 2)
    private BigDecimal minTransactionAmount = BigDecimal.ZERO;
    
    // ============================================================
    // REVERSAL ACTIVITY
    // ============================================================
    
    @Column(name = "reversal_count", nullable = false)
    private Long reversalCount = 0L;
    
    @Column(name = "reversal_volume", precision = 19, scale = 2)
    private BigDecimal reversalVolume = BigDecimal.ZERO;
    
    @Column(name = "reversal_rate", precision = 5, scale = 2)
    private BigDecimal reversalRate = BigDecimal.ZERO;
    
    // ============================================================
    // DISPUTE ACTIVITY
    // ============================================================
    
    @Column(name = "dispute_count", nullable = false)
    private Long disputeCount = 0L;
    
    @Column(name = "dispute_volume", precision = 19, scale = 2)
    private BigDecimal disputeVolume = BigDecimal.ZERO;
    
    @Column(name = "dispute_rate", precision = 5, scale = 2)
    private BigDecimal disputeRate = BigDecimal.ZERO;
    
    // ============================================================
    // FEE ANALYTICS
    // ============================================================
    
    @Column(name = "total_fees_paid", precision = 19, scale = 2)
    private BigDecimal totalFeesPaid = BigDecimal.ZERO;
    
    @Column(name = "average_fee_percentage", precision = 5, scale = 2)
    private BigDecimal averageFeePercentage = BigDecimal.ZERO;
    
    // ============================================================
    // CURRENCY USAGE
    // ============================================================
    
    @Column(name = "primary_currency", length = 3)
    private String primaryCurrency;
    
    @Column(name = "currency_count", nullable = false)
    private Integer currencyCount = 0;
    
    // ============================================================
    // COMPLIANCE METRICS
    // ============================================================
    
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore = BigDecimal.ZERO;
    
    @Column(name = "compliance_status", length = 20)
    private String complianceStatus;
    
    // ============================================================
    // STATUS & TIMESTAMPS
    // ============================================================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
