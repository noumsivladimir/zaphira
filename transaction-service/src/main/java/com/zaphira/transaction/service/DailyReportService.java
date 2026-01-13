package com.zaphira.transaction.service;

import com.zaphira.transaction.model.*;
import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.repository.DailyReportRepository;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.DisputeRepository;
import com.zaphira.transaction.repository.TransactionSettlementRepository;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.DisputeStatus;
import com.zaphira.transaction.model.enums.SettlementStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DailyReportService - Generates daily transaction report summaries.
 * 
 * Purpose:
 * - Aggregate daily transaction metrics
 * - Calculate success rates and volume metrics
 * - Track dispute and reversal patterns
 * - Monitor settlement performance
 * 
 * Pattern (from Phase 2-4):
 * - @Transactional for data consistency
 * - @Scheduled for automatic execution
 * - Custom repository queries for performance
 * - Comprehensive error handling and logging
 * 
 * Scheduled: Daily at 23:59:59 UTC (end of business day)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DailyReportService {
    
    private final DailyReportRepository dailyReportRepository;
    private final TransactionRepository transactionRepository;
    private final DisputeRepository disputeRepository;
    private final TransactionSettlementRepository settlementRepository;
    
    /**
     * Generate daily report for previous day.
     * 
     * Scheduled to run at 23:59:59 UTC (end of day)
     */
    @Scheduled(cron = "59 59 23 * * *", zone = "UTC")
    public void generateDailyReport() {
        log.info("[DAILY_REPORT] Starting daily report generation");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            
            // Check if report already exists
            if (dailyReportRepository.existsByReportDate(yesterday)) {
                log.warn("[DAILY_REPORT] Report already exists for {}", yesterday);
                return;
            }
            
            // Fetch all data for the date
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);
            
            // Build report from aggregated data
            DailyReport report = buildDailyReport(yesterday, startOfDay, endOfDay);
            
            // Mark as finalized
            report.setIsFinalized(true);
            report.setFinalizedAt(LocalDateTime.now());
            
            // Save report
            dailyReportRepository.save(report);
            log.info("[DAILY_REPORT] Daily report generated successfully for {}", yesterday);
            
        } catch (Exception e) {
            log.error("[DAILY_REPORT] Error generating daily report", e);
            throw new RuntimeException("Daily report generation failed", e);
        }
    }
    
    /**
     * Get daily report for specific date
     */
    @Transactional(readOnly = true)
    public DailyReport getDailyReport(LocalDate reportDate) {
        log.debug("[DAILY_REPORT] Fetching report for {}", reportDate);
        return dailyReportRepository.findByReportDate(reportDate)
            .orElseThrow(() -> {
                log.warn("[DAILY_REPORT] Report not found for {}", reportDate);
                return new RuntimeException("Daily report not found for date: " + reportDate);
            });
    }
    
    /**
     * Get daily reports for date range
     */
    @Transactional(readOnly = true)
    public List<DailyReport> getDailyReports(LocalDate startDate, LocalDate endDate) {
        log.debug("[DAILY_REPORT] Fetching reports between {} and {}", startDate, endDate);
        return dailyReportRepository.findReportsByDateRange(startDate, endDate);
    }
    
    /**
     * Get latest N reports
     */
    @Transactional(readOnly = true)
    public List<DailyReport> getLatestReports(int limit) {
        log.debug("[DAILY_REPORT] Fetching latest {} reports", limit);
        return dailyReportRepository.findLatestReports(limit);
    }
    
    /**
     * Build comprehensive daily report
     */
    private DailyReport buildDailyReport(LocalDate reportDate, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        log.debug("[DAILY_REPORT] Building report for {}", reportDate);
        
        // Fetch all transactions for the day
        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        List<Transaction> successfulTxns = transactions.stream()
            .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
            .toList();
        List<Transaction> failedTxns = transactions.stream()
            .filter(t -> t.getStatus() == TransactionStatus.FAILED)
            .toList();
        List<Transaction> pendingTxns = transactions.stream()
            .filter(t -> t.getStatus() == TransactionStatus.PENDING)
            .toList();
        
        // Calculate transaction metrics
        BigDecimal totalVolume = transactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal successRate = transactions.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(successfulTxns.size())
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Fetch reversals
        List<Transaction> reversals = transactions.stream()
            .filter(t -> t.getType().name().equals("REVERSAL"))
            .toList();
        BigDecimal reversalVolume = reversals.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reversalRate = transactions.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(reversals.size())
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Fetch refunds
        List<Transaction> refunds = transactions.stream()
            .filter(t -> t.getType().name().equals("REFUND"))
            .toList();
        BigDecimal refundVolume = refunds.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundRate = transactions.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(refunds.size())
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Fetch disputes
        List<Dispute> disputes = disputeRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        List<Dispute> resolvedDisputes = disputes.stream()
            .filter(d -> d.getStatus() == DisputeStatus.RESOLVED)
            .toList();
        BigDecimal totalDisputedAmount = disputes.stream()
            .map(Dispute::getClaimedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disputeRate = transactions.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(disputes.size())
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Fetch settlements
        List<TransactionSettlement> settlements = settlementRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        List<TransactionSettlement> completedSettlements = settlements.stream()
            .filter(s -> s.getStatus() == SettlementStatus.COMPLETED)
            .toList();
        List<TransactionSettlement> failedSettlements = settlements.stream()
            .filter(s -> s.getStatus() == SettlementStatus.FAILED)
            .toList();
        BigDecimal settlementVolume = settlements.stream()
            .map(TransactionSettlement::getSettledAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settlementSuccessRate = settlements.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(completedSettlements.size())
                .divide(BigDecimal.valueOf(settlements.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Calculate fees
        BigDecimal totalFees = transactions.stream()
            .map(t -> t.getFeeAmount() != null ? t.getFeeAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Build and return report
        return DailyReport.builder()
            .reportDate(reportDate)
            .totalTransactions((long) transactions.size())
            .totalTransactionVolume(totalVolume)
            .successfulTransactions((long) successfulTxns.size())
            .failedTransactions((long) failedTxns.size())
            .pendingTransactions((long) pendingTxns.size())
            .successRate(successRate)
            .totalReversals((long) reversals.size())
            .totalReversalVolume(reversalVolume)
            .reversalRate(reversalRate)
            .totalRefunds((long) refunds.size())
            .totalRefundVolume(refundVolume)
            .refundRate(refundRate)
            .totalDisputes((long) disputes.size())
            .newDisputes((long) disputes.stream()
                .filter(d -> d.getStatus() == DisputeStatus.INITIATED)
                .count())
            .resolvedDisputes((long) resolvedDisputes.size())
            .disputeRate(disputeRate)
            .totalDisputedAmount(totalDisputedAmount)
            .totalSettlements((long) settlements.size())
            .totalSettlementVolume(settlementVolume)
            .completedSettlements((long) completedSettlements.size())
            .failedSettlements((long) failedSettlements.size())
            .settlementSuccessRate(settlementSuccessRate)
            .totalFeesCollected(totalFees)
            .build();
    }
}
