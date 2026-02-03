package com.zaphira.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * LOT 4: Merchant Report DTO
 * Summary of merchant sales over a period
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantReportDTO {
    
    private Long merchantId;
    private String merchantName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    // Transaction counts
    private Long totalTransactions;
    private Long completedTransactions;
    private Long pendingTransactions;
    private Long failedTransactions;
    private Long refundedTransactions;
    
    // Financial summary
    private BigDecimal totalSalesAmount;
    private BigDecimal completedSalesAmount;
    private BigDecimal pendingSalesAmount;
    private BigDecimal refundedAmount;
    private BigDecimal netAmount; // totalSales - refunds
    
    // Fees
    private BigDecimal totalFees;
    private BigDecimal averageFeePerTransaction;
    
    // Averages
    private BigDecimal averageTransactionAmount;
    private BigDecimal medianTransactionAmount;
    
    // Top customers (optional)
    private Integer uniqueCustomers;
}
