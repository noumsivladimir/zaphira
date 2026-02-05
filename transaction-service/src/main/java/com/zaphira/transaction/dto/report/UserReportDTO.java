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
public class UserReportDTO {
    private Long userId;
    private LocalDate reportDate;
    private Long totalTransactions;
    private BigDecimal totalVolume;
    private Long sentTransactions;
    private Long receivedTransactions;
    private BigDecimal sentVolume;
    private BigDecimal receivedVolume;
    private BigDecimal averageTransactionAmount;
}
