package com.zaphira.transaction.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MerchantAnalyticsResponse - Merchant-level transaction analytics response DTO
 * 
 * Provides comprehensive merchant performance metrics and revenue tracking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantAnalyticsResponse {
    
    private Long id;
    private Long merchantId;
    private LocalDate analyticsDate;
    
    // Business Metrics
    private Long totalTransactionCount;
    private BigDecimal totalVolume;
    private BigDecimal netRevenue;
    private BigDecimal grossRevenue;
    private BigDecimal totalFeesPaid;
    private BigDecimal averageTransactionValue;
    
    // Settlement Metrics
    private Long settlementCount;
    private BigDecimal settlementVolume;
    private Long successfulSettlements;
    private Long failedSettlements;
    private BigDecimal settlementSuccessRate;
    private Long settlementDelayAverageHours;
    
    // Dispute & Risk Metrics
    private Long disputeCount;
    private BigDecimal disputeVolume;
    private BigDecimal disputeRate;
    private Long resolvedDisputes;
    private Long merchantWonDisputes;
    private BigDecimal chargebackRate;
    
    // Performance Metrics
    private BigDecimal transactionSuccessRate;
    private BigDecimal reversalRate;
    private Long averageResponseTimeMs;
    
    // Merchant Tier & Classification
    private String merchantTier; // BRONZE, SILVER, GOLD, PLATINUM
    private BigDecimal riskScore;
    private String complianceStatus;
    
    // Currency & Market
    private String primaryCurrency;
    private Integer currencyCount;
    private Integer countryCount;
}
