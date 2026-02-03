package com.zaphira.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * LOT 4: Merchant Analytics DTO
 * Aggregated statistics for merchant dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAnalyticsDTO {
    
    private Long merchantId;
    
    // Real-time metrics
    private BigDecimal todaysSales;
    private Long todaysTransactionCount;
    private BigDecimal weekSales;
    private BigDecimal monthSales;
    
    // Growth metrics
    private BigDecimal salesGrowthPercent; // vs previous period
    private BigDecimal transactionGrowthPercent;
    
    // Transaction breakdown by status
    private Map<String, Long> transactionsByStatus; // {COMPLETED: 150, PENDING: 5, ...}
    
    // Transaction breakdown by type
    private Map<String, Long> transactionsByType; // {MERCHANT_PAYMENT: 120, REFUND: 10, ...}
    
    // Top performing periods
    private String bestDayOfWeek;
    private String bestHourOfDay;
    
    // Settlement info
    private BigDecimal pendingSettlementAmount;
    private Long pendingSettlementCount;
    
    // Refund metrics
    private BigDecimal refundRate; // % of transactions refunded
    private BigDecimal averageRefundAmount;
}
