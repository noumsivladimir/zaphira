package com.zaphira.transaction.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DailyReportResponse - Daily summary metrics response DTO
 * 
 * Pattern (from Phase 1-4):
 * - Consistent DTO naming (Entity + Response suffix)
 * - BigDecimal for monetary values
 * - LocalDate for dates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReportResponse {
    
    private Long id;
    private LocalDate reportDate;
    
    // Transaction Metrics
    private Long totalTransactions;
    private BigDecimal totalTransactionVolume;
    private Long successfulTransactions;
    private Long failedTransactions;
    private Long pendingTransactions;
    private BigDecimal successRate;
    
    // Reversal Metrics
    private Long totalReversals;
    private BigDecimal totalReversalVolume;
    private BigDecimal reversalRate;
    
    // Refund Metrics
    private Long totalRefunds;
    private BigDecimal totalRefundVolume;
    private BigDecimal refundRate;
    
    // Dispute Metrics
    private Long totalDisputes;
    private Long newDisputes;
    private Long resolvedDisputes;
    private BigDecimal disputeRate;
    private BigDecimal totalDisputedAmount;
    
    // Settlement Metrics
    private Long totalSettlements;
    private BigDecimal totalSettlementVolume;
    private Long completedSettlements;
    private Long failedSettlements;
    private BigDecimal settlementSuccessRate;
    
    // Fee Metrics
    private BigDecimal totalFeesCollected;
    private BigDecimal fxFeesCollected;
    private BigDecimal serviceFeesCollected;
    private BigDecimal averageFeePercentage;
    
    // Currency Metrics
    private Integer uniqueCurrencies;
    private String topCurrency;
    
    // Status
    private Boolean isFinalized;
}
