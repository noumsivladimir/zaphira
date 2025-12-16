package com.zaphira.transaction.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UserAnalyticsResponse - User-level transaction analytics response DTO
 * 
 * Provides granular insights into individual user transaction patterns
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnalyticsResponse {
    
    private Long id;
    private Long userId;
    private LocalDate analyticsDate;
    private String userType; // CUSTOMER or MERCHANT
    
    // Transaction Activity
    private Long transactionCount;
    private BigDecimal transactionVolume;
    private Long successfulTransactions;
    private Long failedTransactions;
    private BigDecimal averageTransactionAmount;
    private BigDecimal maxTransactionAmount;
    private BigDecimal minTransactionAmount;
    
    // Reversal Activity
    private Long reversalCount;
    private BigDecimal reversalVolume;
    private BigDecimal reversalRate;
    
    // Dispute Activity
    private Long disputeCount;
    private BigDecimal disputeVolume;
    private BigDecimal disputeRate;
    
    // Fee Analytics
    private BigDecimal totalFeesPaid;
    private BigDecimal averageFeePercentage;
    
    // Currency Usage
    private String primaryCurrency;
    private Integer currencyCount;
    
    // Compliance Metrics
    private BigDecimal riskScore;
    private String complianceStatus;
}
