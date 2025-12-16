package com.zaphira.transaction.service;

import com.zaphira.transaction.model.UserAnalytics;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.Dispute;
import com.zaphira.transaction.repository.UserAnalyticsRepository;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.DisputeRepository;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.DisputeStatus;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserAnalyticsService - Generates user-level transaction analytics.
 * 
 * Purpose:
 * - Track individual user transaction activity
 * - Monitor user spending patterns and behavior
 * - Calculate user-specific metrics (volume, disputes, fees)
 * - Support personalized analytics and insights
 * 
 * Pattern (from Phase 2-4):
 * - Daily batch processing of user analytics
 * - Aggregation from transaction repository
 * - Risk score calculation
 * - Compliance status tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAnalyticsService {
    
    private final UserAnalyticsRepository userAnalyticsRepository;
    private final TransactionRepository transactionRepository;
    private final DisputeRepository disputeRepository;
    
    /**
     * Generate user analytics for all active users.
     * 
     * Scheduled daily at 00:15 UTC (15 minutes after day start)
     */
    @Scheduled(cron = "0 15 0 * * *", zone = "UTC")
    public void generateUserAnalytics() {
        log.info("[USER_ANALYTICS] Starting user analytics generation");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            
            // Get all unique users from yesterday's transactions
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);
            
            List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(startOfDay, endOfDay);
            
            // Get unique customer IDs and merchant IDs
            Set<Long> customerIds = transactions.stream()
                .map(t -> t.getSenderWallet().getUserId())
                .collect(Collectors.toSet());
            
            Set<Long> merchantIds = transactions.stream()
                .map(t -> t.getReceiverWallet().getUserId())
                .collect(Collectors.toSet());
            
            // Generate analytics for each customer
            customerIds.forEach(customerId -> 
                generateUserAnalyticsForUser(customerId, "CUSTOMER", yesterday)
            );
            
            // Generate analytics for each merchant
            merchantIds.forEach(merchantId ->
                generateUserAnalyticsForUser(merchantId, "MERCHANT", yesterday)
            );
            
            log.info("[USER_ANALYTICS] User analytics generated for {} customers and {} merchants",
                customerIds.size(), merchantIds.size());
            
        } catch (Exception e) {
            log.error("[USER_ANALYTICS] Error generating user analytics", e);
            throw new RuntimeException("User analytics generation failed", e);
        }
    }
    
    /**
     * Generate analytics for specific user
     */
    private void generateUserAnalyticsForUser(Long userId, String userType, LocalDate analyticsDate) {
        log.debug("[USER_ANALYTICS] Generating analytics for user {} ({})", userId, userType);
        
        LocalDateTime startOfDay = analyticsDate.atStartOfDay();
        LocalDateTime endOfDay = analyticsDate.atTime(LocalTime.MAX);
        
        // Fetch user's transactions for the day
        List<Transaction> userTransactions = "CUSTOMER".equals(userType) ?
            transactionRepository.findBySenderWallet_UserIdAndCreatedAtBetween(userId, startOfDay, endOfDay) :
            transactionRepository.findByReceiverWallet_UserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);
        
        if (userTransactions.isEmpty()) {
            log.debug("[USER_ANALYTICS] No transactions found for user {}", userId);
            return;
        }
        
        // Calculate metrics
        List<Transaction> successfulTxns = userTransactions.stream()
            .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
            .toList();
        
        List<Transaction> reversals = userTransactions.stream()
            .filter(t -> t.getType().name().equals("REVERSAL"))
            .toList();
        
        BigDecimal totalVolume = userTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgAmount = userTransactions.isEmpty() ? BigDecimal.ZERO :
            totalVolume.divide(BigDecimal.valueOf(userTransactions.size()), 2, RoundingMode.HALF_UP);
        
        BigDecimal maxAmount = userTransactions.stream()
            .map(Transaction::getAmount)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal minAmount = userTransactions.stream()
            .map(Transaction::getAmount)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal reversalRate = userTransactions.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(reversals.size())
                .divide(BigDecimal.valueOf(userTransactions.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Fetch disputes for user
        List<Dispute> userDisputes = "CUSTOMER".equals(userType) ?
            disputeRepository.findByCreatedAtBetween(startOfDay, endOfDay) :
            disputeRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        BigDecimal disputeRate = userTransactions.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(userDisputes.size())
                .divide(BigDecimal.valueOf(userTransactions.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Calculate total fees paid
        BigDecimal totalFeesPaid = userTransactions.stream()
            .map(t -> t.getFeeAmount() != null ? t.getFeeAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgFeePercentage = totalVolume.equals(BigDecimal.ZERO) ? BigDecimal.ZERO :
            totalFeesPaid.divide(totalVolume, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Get currencies used
        Set<String> currencies = userTransactions.stream()
            .map(Transaction::getCurrency)
            .collect(Collectors.toSet());
        
        // Build and save analytics
        UserAnalytics analytics = UserAnalytics.builder()
            .userId(userId)
            .analyticsDate(analyticsDate)
            .userType(userType)
            .transactionCount((long) userTransactions.size())
            .transactionVolume(totalVolume)
            .successfulTransactions((long) successfulTxns.size())
            .failedTransactions((long) (userTransactions.size() - successfulTxns.size()))
            .averageTransactionAmount(avgAmount)
            .maxTransactionAmount(maxAmount)
            .minTransactionAmount(minAmount)
            .reversalCount((long) reversals.size())
            .reversalRate(reversalRate)
            .disputeCount((long) userDisputes.size())
            .disputeRate(disputeRate)
            .totalFeesPaid(totalFeesPaid)
            .averageFeePercentage(avgFeePercentage)
            .primaryCurrency(currencies.isEmpty() ? null : currencies.iterator().next())
            .currencyCount(currencies.size())
            .riskScore(calculateRiskScore(userTransactions, userDisputes, reversals))
            .complianceStatus("COMPLIANT")
            .build();
        
        userAnalyticsRepository.save(analytics);
        log.debug("[USER_ANALYTICS] Analytics saved for user {}", userId);
    }
    
    /**
     * Get user analytics for specific date
     */
    @Transactional(readOnly = true)
    public UserAnalytics getUserAnalytics(Long userId, LocalDate analyticsDate) {
        log.debug("[USER_ANALYTICS] Fetching analytics for user {} on {}", userId, analyticsDate);
        return userAnalyticsRepository.findByUserIdAndAnalyticsDate(userId, analyticsDate)
            .orElseThrow(() -> new RuntimeException("User analytics not found"));
    }
    
    /**
     * Get user analytics for date range
     */
    @Transactional(readOnly = true)
    public List<UserAnalytics> getUserAnalyticsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.debug("[USER_ANALYTICS] Fetching analytics for user {} between {} and {}", userId, startDate, endDate);
        return userAnalyticsRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }
    
    /**
     * Calculate risk score based on user behavior
     * 
     * Risk factors:
     * - High dispute rate (>5%)
     * - High reversal rate (>10%)
     * - Unusual transaction patterns
     */
    private BigDecimal calculateRiskScore(List<Transaction> transactions, List<Dispute> disputes, List<Transaction> reversals) {
        BigDecimal riskScore = BigDecimal.ZERO;
        
        // Dispute rate impact
        if (!transactions.isEmpty()) {
            BigDecimal disputeRate = BigDecimal.valueOf(disputes.size())
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
            if (disputeRate.compareTo(BigDecimal.valueOf(0.05)) > 0) {
                riskScore = riskScore.add(BigDecimal.valueOf(30));
            }
            
            // Reversal rate impact
            BigDecimal reversalRate = BigDecimal.valueOf(reversals.size())
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
            if (reversalRate.compareTo(BigDecimal.valueOf(0.10)) > 0) {
                riskScore = riskScore.add(BigDecimal.valueOf(25));
            }
        }
        
        // Cap at 100
        return riskScore.min(BigDecimal.valueOf(100));
    }
}
