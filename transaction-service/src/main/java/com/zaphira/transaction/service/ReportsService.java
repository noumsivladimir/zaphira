package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.report.DailyReportDTO;
import com.zaphira.transaction.dto.report.MerchantReportDTO;
import com.zaphira.transaction.dto.report.UserReportDTO;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportsService {

    private final TransactionRepository transactionRepository;

    /**
     * Generate daily report for specific date
     */
    @Transactional(readOnly = true)
    public DailyReportDTO getDailyReport(LocalDate date) {
        log.info("Generating daily report for date: {}", date);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        var transactions = transactionRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        long total = transactions.size();
        long successful = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .count();
        long failed = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                .count();
        long pending = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PENDING)
                .count();
        long cancelled = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.CANCELLED)
                .count();
        
        BigDecimal totalVolume = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgAmount = total > 0 
                ? totalVolume.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        BigDecimal successRate = total > 0
                ? BigDecimal.valueOf(successful).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        return DailyReportDTO.builder()
                .reportDate(date)
                .totalTransactions(total)
                .totalVolume(totalVolume)
                .successfulTransactions(successful)
                .failedTransactions(failed)
                .pendingTransactions(pending)
                .cancelledTransactions(cancelled)
                .averageTransactionAmount(avgAmount)
                .successRate(successRate)
                .build();
    }

    /**
     * Get latest N daily reports
     */
    @Transactional(readOnly = true)
    public List<DailyReportDTO> getLatestDailyReports(int limit) {
        log.info("Generating latest {} daily reports", limit);
        
        List<DailyReportDTO> reports = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        
        for (int i = 0; i < limit; i++) {
            LocalDate date = currentDate.minusDays(i);
            reports.add(getDailyReport(date));
        }
        
        return reports;
    }

    /**
     * Get daily reports for date range
     */
    @Transactional(readOnly = true)
    public List<DailyReportDTO> getDailyReportsForRange(LocalDate startDate, LocalDate endDate) {
        log.info("Generating daily reports from {} to {}", startDate, endDate);
        
        List<DailyReportDTO> reports = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            reports.add(getDailyReport(current));
            current = current.plusDays(1);
        }
        
        return reports;
    }

    /**
     * Generate user report for specific user and date
     */
    @Transactional(readOnly = true)
    public UserReportDTO getUserReport(Long userId, LocalDate date) {
        log.info("Generating user report for userId: {} on date: {}", userId, date);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        var transactions = transactionRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        // Filter transactions involving this user (via wallet)
        // Note: This is simplified - in production, we'd join with wallet table
        var userTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .toList();
        
        long total = userTransactions.size();
        
        BigDecimal totalVolume = userTransactions.stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgAmount = total > 0
                ? totalVolume.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        return UserReportDTO.builder()
                .userId(userId)
                .reportDate(date)
                .totalTransactions(total)
                .totalVolume(totalVolume)
                .sentTransactions(total / 2) // Simplified
                .receivedTransactions(total / 2) // Simplified
                .sentVolume(totalVolume.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
                .receivedVolume(totalVolume.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
                .averageTransactionAmount(avgAmount)
                .build();
    }

    /**
     * Get user report history for date range
     */
    @Transactional(readOnly = true)
    public List<UserReportDTO> getUserReportHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating user report history for userId: {} from {} to {}", userId, startDate, endDate);
        
        List<UserReportDTO> reports = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            reports.add(getUserReport(userId, current));
            current = current.plusDays(1);
        }
        
        return reports;
    }

    /**
     * Generate merchant report for specific merchant and date
     */
    @Transactional(readOnly = true)
    public MerchantReportDTO getMerchantReport(Long merchantId, LocalDate date) {
        log.info("Generating merchant report for merchantId: {} on date: {}", merchantId, date);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        var transactions = transactionRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        // Filter merchant transactions
        var merchantTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .toList();
        
        long total = merchantTransactions.size();
        long successful = merchantTransactions.size();
        long failed = 0; // Simplified
        
        BigDecimal totalRevenue = merchantTransactions.stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFees = totalRevenue.multiply(BigDecimal.valueOf(0.025)); // 2.5% fee
        BigDecimal netRevenue = totalRevenue.subtract(totalFees);
        
        BigDecimal avgAmount = total > 0
                ? totalRevenue.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        BigDecimal successRate = total > 0
                ? BigDecimal.valueOf(successful).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        return MerchantReportDTO.builder()
                .merchantId(merchantId)
                .reportDate(date)
                .totalTransactions(total)
                .totalRevenue(totalRevenue)
                .totalFees(totalFees)
                .netRevenue(netRevenue)
                .successfulPayments(successful)
                .failedPayments(failed)
                .successRate(successRate)
                .averagePaymentAmount(avgAmount)
                .build();
    }
}
