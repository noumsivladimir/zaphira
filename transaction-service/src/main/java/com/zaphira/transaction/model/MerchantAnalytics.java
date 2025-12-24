package com.zaphira.transaction.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MerchantAnalytics - Aggregated merchant-level transaction analytics.
 * 
 * Purpose:
 * - Track merchant revenue and settlement performance
 * - Monitor dispute rates and chargeback risk
 * - Support merchant tier classification
 * - Enable merchant performance insights
 * 
 * Updated daily via scheduled task
 */
@Entity
@Table(name = "merchant_analytics", indexes = {
    @Index(name = "idx_merchant_analytics_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_merchant_analytics_date", columnList = "analytics_date DESC"),
    @Index(name = "idx_merchant_analytics_tier", columnList = "merchant_tier")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;
    
    @Column(name = "analytics_date", nullable = false)
    private LocalDate analyticsDate;
    
    // ============================================================
    // BUSINESS METRICS
    // ============================================================
    
    @Column(name = "total_transaction_count", nullable = false)
    private Long totalTransactionCount = 0L;
    
    @Column(name = "total_volume", precision = 19, scale = 2)
    private BigDecimal totalVolume = BigDecimal.ZERO;
    
    @Column(name = "net_revenue", precision = 19, scale = 2)
    private BigDecimal netRevenue = BigDecimal.ZERO;
    
    @Column(name = "gross_revenue", precision = 19, scale = 2)
    private BigDecimal grossRevenue = BigDecimal.ZERO;
    
    @Column(name = "total_fees_paid", precision = 19, scale = 2)
    private BigDecimal totalFeesPaid = BigDecimal.ZERO;
    
    @Column(name = "average_transaction_value", precision = 19, scale = 2)
    private BigDecimal averageTransactionValue = BigDecimal.ZERO;
    
    // ============================================================
    // SETTLEMENT METRICS
    // ============================================================
    
    @Column(name = "settlement_count", nullable = false)
    private Long settlementCount = 0L;
    
    @Column(name = "settlement_volume", precision = 19, scale = 2)
    private BigDecimal settlementVolume = BigDecimal.ZERO;
    
    @Column(name = "successful_settlements", nullable = false)
    private Long successfulSettlements = 0L;
    
    @Column(name = "failed_settlements", nullable = false)
    private Long failedSettlements = 0L;
    
    @Column(name = "settlement_success_rate", precision = 5, scale = 2)
    private BigDecimal settlementSuccessRate = BigDecimal.ZERO;
    
    @Column(name = "settlement_delay_average", nullable = false)
    private Long settlementDelayAverageHours = 0L;
    
    // ============================================================
    // DISPUTE & RISK METRICS
    // ============================================================
    
    @Column(name = "dispute_count", nullable = false)
    private Long disputeCount = 0L;
    
    @Column(name = "dispute_volume", precision = 19, scale = 2)
    private BigDecimal disputeVolume = BigDecimal.ZERO;
    
    @Column(name = "dispute_rate", precision = 5, scale = 2)
    private BigDecimal disputeRate = BigDecimal.ZERO;
    
    @Column(name = "resolved_disputes", nullable = false)
    private Long resolvedDisputes = 0L;
    
    @Column(name = "merchant_won_disputes", nullable = false)
    private Long merchantWonDisputes = 0L;
    
    @Column(name = "chargeback_rate", precision = 5, scale = 2)
    private BigDecimal chargebackRate = BigDecimal.ZERO;
    
    // ============================================================
    // PERFORMANCE METRICS
    // ============================================================
    
    @Column(name = "transaction_success_rate", precision = 5, scale = 2)
    private BigDecimal transactionSuccessRate = BigDecimal.ZERO;
    
    @Column(name = "reversal_rate", precision = 5, scale = 2)
    private BigDecimal reversalRate = BigDecimal.ZERO;
    
    @Column(name = "average_response_time_ms", nullable = false)
    private Long averageResponseTimeMs = 0L;
    
    // ============================================================
    // MERCHANT TIER & CLASSIFICATION
    // ============================================================
    
    @Column(name = "merchant_tier", length = 20)
    private String merchantTier; // BRONZE, SILVER, GOLD, PLATINUM
    
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore = BigDecimal.ZERO;
    
    @Column(name = "compliance_status", length = 20)
    private String complianceStatus;
    
    // ============================================================
    // CURRENCY & MARKET
    // ============================================================
    
    @Column(name = "primary_currency", length = 3)
    private String primaryCurrency;
    
    @Column(name = "currency_count", nullable = false)
    private Integer currencyCount = 0;
    
    @Column(name = "country_count", nullable = false)
    private Integer countryCount = 0;
    
    // ============================================================
    // STATUS & TIMESTAMPS
    // ============================================================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
