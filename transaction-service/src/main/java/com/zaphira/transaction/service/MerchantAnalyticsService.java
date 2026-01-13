//package com.zaphira.transaction.service;
//
//import com.zaphira.common.model.enums.Currency;
//import com.zaphira.transaction.model.MerchantAnalytics;
//import com.zaphira.transaction.model.entities.Transaction;
//import com.zaphira.transaction.model.Dispute;
//import com.zaphira.transaction.model.TransactionSettlement;
//import com.zaphira.transaction.repository.MerchantAnalyticsRepository;
//import com.zaphira.transaction.repository.TransactionRepository;
//import com.zaphira.transaction.repository.DisputeRepository;
//import com.zaphira.transaction.repository.TransactionSettlementRepository;
//import com.zaphira.transaction.model.enums.TransactionStatus;
//import com.zaphira.transaction.model.enums.DisputeStatus;
//import com.zaphira.transaction.model.enums.SettlementStatus;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * MerchantAnalyticsService - Generates merchant-level transaction analytics.
// *
// * Purpose:
// * - Track merchant revenue and settlement performance
// * - Monitor dispute rates and chargeback risk
// * - Calculate merchant performance tiers
// * - Support merchant insights and risk management
// *
// * Pattern (from Phase 2-4):
// * - Daily batch processing of merchant metrics
// * - Automatic tier classification
// * - Risk scoring based on disputes/chargebacks
// * - Settlement performance tracking
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class MerchantAnalyticsService {
//
//    private final MerchantAnalyticsRepository merchantAnalyticsRepository;
//    private final TransactionRepository transactionRepository;
//    private final DisputeRepository disputeRepository;
//    private final TransactionSettlementRepository settlementRepository;
//
//    /**
//     * Generate merchant analytics for all active merchants.
//     *
//     * Scheduled daily at 00:30 UTC (30 minutes after day start)
//     */
//    @Scheduled(cron = "0 30 0 * * *", zone = "UTC")
//    public void generateMerchantAnalytics() {
//        log.info("[MERCHANT_ANALYTICS] Starting merchant analytics generation");
//        try {
//            LocalDate yesterday = LocalDate.now().minusDays(1);
//
//            // Get all unique merchants from yesterday's transactions
//            LocalDateTime startOfDay = yesterday.atStartOfDay();
//            LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);
//
//            List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(startOfDay, endOfDay);
//
//            // Get unique merchant IDs (receiver in transaction)
//            Set<Long> merchantIds = transactions.stream()
//                .map(t -> t.getReceiverWallet().getUserId())
//                .collect(Collectors.toSet());
//
//            // Generate analytics for each merchant
//            merchantIds.forEach(merchantId ->
//                generateMerchantAnalyticsForMerchant(merchantId, yesterday)
//            );
//
//            log.info("[MERCHANT_ANALYTICS] Merchant analytics generated for {} merchants", merchantIds.size());
//
//        } catch (Exception e) {
//            log.error("[MERCHANT_ANALYTICS] Error generating merchant analytics", e);
//            throw new RuntimeException("Merchant analytics generation failed", e);
//        }
//    }
//
//    /**
//     * Generate analytics for specific merchant
//     */
//    private void generateMerchantAnalyticsForMerchant(Long merchantId, LocalDate analyticsDate) {
//        log.debug("[MERCHANT_ANALYTICS] Generating analytics for merchant {}", merchantId);
//
//        LocalDateTime startOfDay = analyticsDate.atStartOfDay();
//        LocalDateTime endOfDay = analyticsDate.atTime(LocalTime.MAX);
//
//        // Fetch merchant's transactions for the day
//        List<Transaction> merchantTransactions = transactionRepository
//            .findByReceiverWallet_UserIdAndCreatedAtBetween(merchantId, startOfDay, endOfDay);
//
//        if (merchantTransactions.isEmpty()) {
//            log.debug("[MERCHANT_ANALYTICS] No transactions found for merchant {}", merchantId);
//            return;
//        }
//
//        // Calculate transaction metrics
//        List<Transaction> successfulTxns = merchantTransactions.stream()
//            .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
//            .toList();
//
//        List<Transaction> reversals = merchantTransactions.stream()
//            .filter(t -> t.getType().name().equals("REVERSAL"))
//            .toList();
//
//        BigDecimal totalVolume = merchantTransactions.stream()
//            .map(Transaction::getAmount)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal avgTransactionValue = merchantTransactions.isEmpty() ? BigDecimal.ZERO :
//            totalVolume.divide(BigDecimal.valueOf(merchantTransactions.size()), 2, RoundingMode.HALF_UP);
//
//        BigDecimal transactionSuccessRate = merchantTransactions.isEmpty() ? BigDecimal.ZERO :
//            BigDecimal.valueOf(successfulTxns.size())
//                .divide(BigDecimal.valueOf(merchantTransactions.size()), 2, RoundingMode.HALF_UP)
//                .multiply(BigDecimal.valueOf(100));
//
//        BigDecimal reversalRate = merchantTransactions.isEmpty() ? BigDecimal.ZERO :
//            BigDecimal.valueOf(reversals.size())
//                .divide(BigDecimal.valueOf(merchantTransactions.size()), 2, RoundingMode.HALF_UP)
//                .multiply(BigDecimal.valueOf(100));
//
//        // Fetch settlements for merchant
//        List<TransactionSettlement> merchantSettlements = settlementRepository
//            .findByCreatedAtBetween(startOfDay, endOfDay);
//
//        List<TransactionSettlement> completedSettlements = merchantSettlements.stream()
//            .filter(s -> s.getStatus() == SettlementStatus.COMPLETED)
//            .toList();
//
//        List<TransactionSettlement> failedSettlements = merchantSettlements.stream()
//            .filter(s -> s.getStatus() == SettlementStatus.FAILED)
//            .toList();
//
//        BigDecimal settlementVolume = merchantSettlements.stream()
//            .map(TransactionSettlement::getSettledAmount)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal settlementSuccessRate = merchantSettlements.isEmpty() ? BigDecimal.ZERO :
//            BigDecimal.valueOf(completedSettlements.size())
//                .divide(BigDecimal.valueOf(merchantSettlements.size()), 2, RoundingMode.HALF_UP)
//                .multiply(BigDecimal.valueOf(100));
//
//        // Fetch disputes for merchant
//        List<Dispute> merchantDisputes = disputeRepository.findByCreatedAtBetween(startOfDay, endOfDay);
//        List<Dispute> resolvedDisputes = merchantDisputes.stream()
//            .filter(d -> d.getStatus() == DisputeStatus.RESOLVED)
//            .toList();
//
//        BigDecimal disputeRate = merchantTransactions.isEmpty() ? BigDecimal.ZERO :
//            BigDecimal.valueOf(merchantDisputes.size())
//                .divide(BigDecimal.valueOf(merchantTransactions.size()), 2, RoundingMode.HALF_UP)
//                .multiply(BigDecimal.valueOf(100));
//
//        BigDecimal chargebackRate = merchantDisputes.isEmpty() ? BigDecimal.ZERO :
//            BigDecimal.valueOf(merchantDisputes.size())
//                .divide(BigDecimal.valueOf(merchantTransactions.size()), 2, RoundingMode.HALF_UP)
//                .multiply(BigDecimal.valueOf(100));
//
//        // Calculate fees
//        BigDecimal totalFeesPaid = merchantTransactions.stream()
//            .map(t -> t.getFeeAmount() != null ? t.getFeeAmount() : BigDecimal.ZERO)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // Calculate gross and net revenue
//        BigDecimal grossRevenue = totalVolume;
//        BigDecimal netRevenue = grossRevenue.subtract(totalFeesPaid);
//
////        // Get currencies and countries
////        Set<String> currencies = merchantTransactions.stream()
////            .map(Transaction::getCurrency)
////            .collect(Collectors.toSet());
////
//        // Determine merchant tier based on volume
//        String merchantTier = determineMerchantTier(totalVolume);
//
//        // Calculate risk score
//        BigDecimal riskScore = calculateMerchantRiskScore(disputeRate, reversalRate, settlementSuccessRate);
//
//        // Build and save analytics
//        MerchantAnalytics analytics = MerchantAnalytics.builder()
//            .merchantId(merchantId)
//            .analyticsDate(analyticsDate)
//            .totalTransactionCount((long) merchantTransactions.size())
//            .totalVolume(totalVolume)
//            .netRevenue(netRevenue)
//            .grossRevenue(grossRevenue)
//            .totalFeesPaid(totalFeesPaid)
//            .averageTransactionValue(avgTransactionValue)
//            .settlementCount((long) merchantSettlements.size())
//            .settlementVolume(settlementVolume)
//            .successfulSettlements((long) completedSettlements.size())
//            .failedSettlements((long) failedSettlements.size())
//            .settlementSuccessRate(settlementSuccessRate)
//            .disputeCount((long) merchantDisputes.size())
//            .disputeRate(disputeRate)
//            .resolvedDisputes((long) resolvedDisputes.size())
//            .chargebackRate(chargebackRate)
//            .transactionSuccessRate(transactionSuccessRate)
//            .reversalRate(reversalRate)
//            .merchantTier(merchantTier)
//            .riskScore(riskScore)
//            .complianceStatus("COMPLIANT")
//            .primaryCurrency(Currency.XAF.getName())
//            .currencyCount(1)
//            .build();
//
//        merchantAnalyticsRepository.save(analytics);
//        log.debug("[MERCHANT_ANALYTICS] Analytics saved for merchant {} (tier: {})", merchantId, merchantTier);
//    }
//
//    /**
//     * Determine merchant tier based on transaction volume
//     *
//     * BRONZE: < 10,000
//     * SILVER: 10,000 - 100,000
//     * GOLD: 100,000 - 1,000,000
//     * PLATINUM: > 1,000,000
//     */
//    private String determineMerchantTier(BigDecimal volume) {
//        if (volume.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
//            return "PLATINUM";
//        } else if (volume.compareTo(BigDecimal.valueOf(100_000)) > 0) {
//            return "GOLD";
//        } else if (volume.compareTo(BigDecimal.valueOf(10_000)) > 0) {
//            return "SILVER";
//        }
//        return "BRONZE";
//    }
//
//    /**
//     * Calculate merchant risk score
//     *
//     * Factors:
//     * - Chargeback/dispute rate (>2% = risk)
//     * - Reversal rate (>5% = risk)
//     * - Settlement failure rate (>10% = risk)
//     */
//    private BigDecimal calculateMerchantRiskScore(BigDecimal disputeRate, BigDecimal reversalRate, BigDecimal settlementSuccessRate) {
//        BigDecimal riskScore = BigDecimal.ZERO;
//
//        // Dispute rate impact
//        if (disputeRate.compareTo(BigDecimal.valueOf(2)) > 0) {
//            BigDecimal disputeRisk = disputeRate.subtract(BigDecimal.valueOf(2))
//                .multiply(BigDecimal.valueOf(5)); // 5 points per 1% above threshold
//            riskScore = riskScore.add(disputeRisk);
//        }
//
//        // Reversal rate impact
//        if (reversalRate.compareTo(BigDecimal.valueOf(5)) > 0) {
//            BigDecimal reversalRisk = reversalRate.subtract(BigDecimal.valueOf(5))
//                .multiply(BigDecimal.valueOf(3)); // 3 points per 1% above threshold
//            riskScore = riskScore.add(reversalRisk);
//        }
//
//        // Settlement failure impact
//        BigDecimal settlementFailureRate = BigDecimal.valueOf(100).subtract(settlementSuccessRate);
//        if (settlementFailureRate.compareTo(BigDecimal.valueOf(10)) > 0) {
//            BigDecimal settlementRisk = settlementFailureRate.subtract(BigDecimal.valueOf(10))
//                .multiply(BigDecimal.valueOf(2)); // 2 points per 1% above threshold
//            riskScore = riskScore.add(settlementRisk);
//        }
//
//        // Cap at 100
//        return riskScore.min(BigDecimal.valueOf(100));
//    }
//
//    /**
//     * Get merchant analytics for specific date
//     */
//    @Transactional(readOnly = true)
//    public MerchantAnalytics getMerchantAnalytics(Long merchantId, LocalDate analyticsDate) {
//        log.debug("[MERCHANT_ANALYTICS] Fetching analytics for merchant {} on {}", merchantId, analyticsDate);
//        return merchantAnalyticsRepository.findByMerchantIdAndAnalyticsDate(merchantId, analyticsDate)
//            .orElseThrow(() -> new RuntimeException("Merchant analytics not found"));
//    }
//}
