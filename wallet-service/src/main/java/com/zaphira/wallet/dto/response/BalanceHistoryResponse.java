package com.zaphira.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for wallet balance history over time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceHistoryResponse {
    private String walletNumber;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<BalanceSnapshot> balanceSnapshots;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceSnapshot {
        private LocalDate date;
        private BigDecimal availableBalance;
        private BigDecimal blockedBalance;
        private BigDecimal totalBalance;
        private int transactionCount;
    }
}
