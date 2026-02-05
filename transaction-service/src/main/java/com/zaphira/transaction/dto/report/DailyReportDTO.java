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
public class DailyReportDTO {
    private LocalDate reportDate;
    private Long totalTransactions;
    private BigDecimal totalVolume;
    private Long successfulTransactions;
    private Long failedTransactions;
    private Long pendingTransactions;
    private Long cancelledTransactions;
    private BigDecimal averageTransactionAmount;
    private BigDecimal successRate;
}
