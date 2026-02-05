package com.zaphira.transaction.service;

import com.zaphira.common.security.SecurityContextHolder;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.response.MerchantAnalyticsDTO;
import com.zaphira.transaction.dto.response.MerchantReportDTO;
import com.zaphira.transaction.mapper.TransactionMapper;
import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LOT 4: Merchant Reports & Analytics Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantReportService {
    
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    
    /**
     * Get merchant sales (transactions where merchant is receiver)
     */
    public Page<TransactionDTO> getMerchantSales(TransactionStatus status, LocalDate from, 
                                                   LocalDate to, int page, int size) {
        Long merchantId = SecurityContextHolder.getCurrentMerchantId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : null;
        
        Page<Transaction> sales;
        
        if (status != null && fromDateTime != null && toDateTime != null) {
            sales = transactionRepository.findByReceiverWalletIdAndStatusAndCreatedAtBetween(
                    merchantId, status, fromDateTime, toDateTime, pageable);
        } else if (status != null) {
            sales = transactionRepository.findByReceiverWalletIdAndStatus(merchantId, status, pageable);
        } else if (fromDateTime != null && toDateTime != null) {
            sales = transactionRepository.findByReceiverWalletIdAndCreatedAtBetween(
                    merchantId, fromDateTime, toDateTime, pageable);
        } else {
            sales = transactionRepository.findByReceiverWalletId(merchantId, pageable);
        }
        
        return sales.map(transactionMapper::toDTO);
    }
    
    /**
     * Generate merchant report
     */
    public MerchantReportDTO getMerchantReport(LocalDate from, LocalDate to) {
        Long merchantId = SecurityContextHolder.getCurrentMerchantId();
        return generateReport(merchantId, from, to);
    }
    
    /**
     * Get merchant analytics
     */
    public MerchantAnalyticsDTO getMerchantAnalytics(LocalDate from, LocalDate to) {
        Long merchantId = SecurityContextHolder.getCurrentMerchantId();
        
        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : LocalDateTime.now();
        
        List<Transaction> transactions = transactionRepository.findByReceiverWalletIdAndCreatedAtBetween(
                merchantId, fromDateTime, toDateTime);
        
        // Calculate metrics
        BigDecimal todaysSales = calculateTodaysSales(merchantId);
        Long todaysCount = countTodaysTransactions(merchantId);
        BigDecimal weekSales = calculateWeekSales(merchantId);
        BigDecimal monthSales = calculateMonthSales(merchantId);
        
        // Group by status
        Map<String, Long> byStatus = new HashMap<>();
        for (TransactionStatus status : TransactionStatus.values()) {
            long count = transactions.stream()
                    .filter(t -> t.getStatus() == status)
                    .count();
            if (count > 0) {
                byStatus.put(status.name(), count);
            }
        }
        
        // Group by type
        Map<String, Long> byType = new HashMap<>();
        for (TransactionType type : TransactionType.values()) {
            long count = transactions.stream()
                    .filter(t -> t.getType() == type)
                    .count();
            if (count > 0) {
                byType.put(type.name(), count);
            }
        }
        
        // Pending settlements
        List<Transaction> pendingSettlements = transactionRepository
                .findByReceiverWalletIdAndStatusIn(merchantId, 
                        List.of(TransactionStatus.COMPLETED, TransactionStatus.PROCESSING));
        
        BigDecimal pendingSettlementAmount = pendingSettlements.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Refund metrics
        long refundCount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.REFUND)
                .count();
        
        BigDecimal refundRate = transactions.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(refundCount)
                        .divide(BigDecimal.valueOf(transactions.size()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
        
        BigDecimal avgRefundAmount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(refundCount > 0 ? BigDecimal.valueOf(refundCount) : BigDecimal.ONE, 
                        2, RoundingMode.HALF_UP);
        
        return MerchantAnalyticsDTO.builder()
                .merchantId(merchantId)
                .todaysSales(todaysSales)
                .todaysTransactionCount(todaysCount)
                .weekSales(weekSales)
                .monthSales(monthSales)
                .transactionsByStatus(byStatus)
                .transactionsByType(byType)
                .pendingSettlementAmount(pendingSettlementAmount)
                .pendingSettlementCount((long) pendingSettlements.size())
                .refundRate(refundRate)
                .averageRefundAmount(avgRefundAmount)
                .build();
    }
    
    /**
     * Get merchant settlements
     */
    public Page<TransactionDTO> getMerchantSettlements(TransactionStatus status, int page, int size) {
        Long merchantId = SecurityContextHolder.getCurrentMerchantId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        List<TransactionStatus> settlementStatuses = (status != null) 
                ? List.of(status) 
                : List.of(TransactionStatus.COMPLETED, TransactionStatus.PROCESSING);
        
        Page<Transaction> settlements = transactionRepository.findByReceiverWalletIdAndStatusIn(
                merchantId, settlementStatuses, pageable);
        
        return settlements.map(transactionMapper::toDTO);
    }
    
    /**
     * Get merchant refunds
     */
    public Page<TransactionDTO> getMerchantRefunds(LocalDate from, LocalDate to, int page, int size) {
        Long merchantId = SecurityContextHolder.getCurrentMerchantId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : LocalDateTime.now().minusMonths(3);
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : LocalDateTime.now();
        
        Page<Transaction> refunds = transactionRepository.findBySenderWalletIdAndTypeAndCreatedAtBetween(
                merchantId, TransactionType.REFUND, fromDateTime, toDateTime, pageable);
        
        return refunds.map(transactionMapper::toDTO);
    }
    
    /**
     * ADMIN: Get merchant report by ID
     */
    public MerchantReportDTO getMerchantReportById(Long merchantId, LocalDate from, LocalDate to) {
        return generateReport(merchantId, from, to);
    }
    
    // ========== Private Helper Methods ==========
    
    private MerchantReportDTO generateReport(Long merchantId, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : LocalDateTime.now();
        
        List<Transaction> transactions = transactionRepository.findByReceiverWalletIdAndCreatedAtBetween(
                merchantId, fromDateTime, toDateTime);
        
        long total = transactions.size();
        long completed = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();
        long pending = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();
        long failed = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count();
        long refunded = transactions.stream().filter(t -> t.getType() == TransactionType.REFUND).count();
        
        BigDecimal totalSales = transactions.stream()
                .filter(t -> t.getType() != TransactionType.REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal completedSales = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED && t.getType() != TransactionType.REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal pendingSales = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PENDING)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal refundedAmount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netAmount = totalSales.subtract(refundedAmount);
        
        BigDecimal avgAmount = total > 0 
                ? totalSales.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        return MerchantReportDTO.builder()
                .merchantId(merchantId)
                .periodStart(from)
                .periodEnd(to)
                .totalTransactions(total)
                .completedTransactions(completed)
                .pendingTransactions(pending)
                .failedTransactions(failed)
                .refundedTransactions(refunded)
                .totalSalesAmount(totalSales)
                .completedSalesAmount(completedSales)
                .pendingSalesAmount(pendingSales)
                .refundedAmount(refundedAmount)
                .netAmount(netAmount)
                .averageTransactionAmount(avgAmount)
                .uniqueCustomers((int) transactions.stream().map(Transaction::getUserId).distinct().count())
                .build();
    }
    
    private BigDecimal calculateTodaysSales(Long merchantId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        
        List<Transaction> todaysTx = transactionRepository.findByReceiverWalletIdAndCreatedAtBetween(
                merchantId, startOfDay, endOfDay);
        
        return todaysTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .filter(t -> t.getType() != TransactionType.REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private Long countTodaysTransactions(Long merchantId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        
        return transactionRepository.countByReceiverWalletIdAndCreatedAtBetween(
                merchantId, startOfDay, endOfDay);
    }
    
    private BigDecimal calculateWeekSales(Long merchantId) {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime now = LocalDateTime.now();
        
        List<Transaction> weekTx = transactionRepository.findByReceiverWalletIdAndCreatedAtBetween(
                merchantId, weekAgo, now);
        
        return weekTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .filter(t -> t.getType() != TransactionType.REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateMonthSales(Long merchantId) {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        LocalDateTime now = LocalDateTime.now();
        
        List<Transaction> monthTx = transactionRepository.findByReceiverWalletIdAndCreatedAtBetween(
                merchantId, monthAgo, now);
        
        return monthTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .filter(t -> t.getType() != TransactionType.REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
