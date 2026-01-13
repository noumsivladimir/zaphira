package com.zaphira.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryDTO {

    private Long userId;

    private LocalDate period;

    private Long totalTransactions;

    private BigDecimal totalAmount;

    private BigDecimal totalDebits;

    private BigDecimal totalCredits;

    private BigDecimal totalFees;

    @Builder.Default
    private Map<String, Long> transactionsByType = new HashMap<>();

    @Builder.Default
    private Map<String, Long> transactionsByStatus = new HashMap<>();

    @Builder.Default
    private Map<String, BigDecimal> amountByCategory = new HashMap<>();
}