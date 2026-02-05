package com.zaphira.transaction.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantReportDTO {
    private Long merchantId;
    private LocalDate reportDate;
    private Long totalTransactions;
    private BigDecimal totalRevenue;
    private BigDecimal totalFees;
    private BigDecimal netRevenue;
    private Long successfulPayments;
    private Long failedPayments;
    private BigDecimal successRate;
    private BigDecimal averagePaymentAmount;
}
