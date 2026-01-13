//package com.zaphira.transaction.controller;
//
//import com.zaphira.transaction.dto.report.DailyReportResponse;
//import com.zaphira.transaction.dto.report.UserAnalyticsResponse;
//import com.zaphira.transaction.dto.report.MerchantAnalyticsResponse;
//import com.zaphira.transaction.model.DailyReport;
//import com.zaphira.transaction.model.UserAnalytics;
//import com.zaphira.transaction.model.MerchantAnalytics;
//import com.zaphira.transaction.service.DailyReportService;
////import com.zaphira.transaction.service.UserAnalyticsService;
////import com.zaphira.transaction.service.MerchantAnalyticsService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * ReportsController - REST endpoints for transaction analytics and reports.
// *
// * Purpose:
// * - Provide access to daily transaction summaries
// * - Enable user-level analytics queries
// * - Support merchant performance monitoring
// * - Offer business intelligence endpoints
// *
// * Security:
// * - ADMIN: Access to all reports
// * - MERCHANT: Access to own merchant analytics
// * - CUSTOMER: Access to own user analytics (limited)
// *
// * Pattern (from Phase 1-4):
// * - @PreAuthorize for role-based access control
// * - DTOs for response transformation
// * - Comprehensive error handling
// * - Detailed logging with [ACTION_SCOPE] format
// * - Date range filtering support
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/reports")
//@RequiredArgsConstructor
//public class ReportsController {
//
//    private final DailyReportService dailyReportService;
//    private final UserAnalyticsService userAnalyticsService;
//    private final MerchantAnalyticsService merchantAnalyticsService;
//
//    // ============================================================
//    // DAILY REPORTS ENDPOINTS
//    // ============================================================
//
//    /**
//     * GET /api/reports/daily/{date}
//     *
//     * Retrieve daily transaction summary for specific date.
//     *
//     * Access: ADMIN, SUPPORT
//     *
//     * @param date Report date (YYYY-MM-DD)
//     * @return Daily report with aggregate metrics
//     */
//    @GetMapping("/daily/{date}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
//    public ResponseEntity<DailyReportResponse> getDailyReport(
//        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
//    ) {
//        log.info("[REPORTS] GET /daily/{} - Fetching daily report", date);
//        try {
//            DailyReport report = dailyReportService.getDailyReport(date);
//            return ResponseEntity.ok(mapDailyReportToResponse(report));
//        } catch (Exception e) {
//            log.error("[REPORTS] Error fetching daily report for {}", date, e);
//            throw new RuntimeException("Failed to fetch daily report", e);
//        }
//    }
//
//    /**
//     * GET /api/reports/daily
//     *
//     * Retrieve daily reports for date range.
//     *
//     * Access: ADMIN, SUPPORT
//     *
//     * Query Parameters:
//     * - startDate: Start date (YYYY-MM-DD)
//     * - endDate: End date (YYYY-MM-DD)
//     * - limit: Maximum number of results (default: 30)
//     *
//     * @return List of daily reports
//     */
//    @GetMapping("/daily")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
//    public ResponseEntity<List<DailyReportResponse>> getDailyReports(
//        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
//        @RequestParam(defaultValue = "30") int limit
//    ) {
//        log.info("[REPORTS] GET /daily - Fetching reports from {} to {}", startDate, endDate);
//        try {
//            List<DailyReport> reports = dailyReportService.getDailyReports(startDate, endDate);
//            List<DailyReportResponse> responses = reports.stream()
//                .limit(limit)
//                .map(this::mapDailyReportToResponse)
//                .collect(Collectors.toList());
//            return ResponseEntity.ok(responses);
//        } catch (Exception e) {
//            log.error("[REPORTS] Error fetching daily reports", e);
//            throw new RuntimeException("Failed to fetch daily reports", e);
//        }
//    }
//
//    /**
//     * GET /api/reports/daily/latest
//     *
//     * Retrieve latest N daily reports.
//     *
//     * Access: ADMIN, SUPPORT
//     *
//     * Query Parameters:
//     * - limit: Number of latest reports to fetch (default: 30)
//     *
//     * @return List of latest daily reports
//     */
//    @GetMapping("/daily/latest")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
//    public ResponseEntity<List<DailyReportResponse>> getLatestDailyReports(
//        @RequestParam(defaultValue = "30") int limit
//    ) {
//        log.info("[REPORTS] GET /daily/latest - Fetching {} latest reports", limit);
//        try {
//            List<DailyReport> reports = dailyReportService.getLatestReports(limit);
//            List<DailyReportResponse> responses = reports.stream()
//                .map(this::mapDailyReportToResponse)
//                .collect(Collectors.toList());
//            return ResponseEntity.ok(responses);
//        } catch (Exception e) {
//            log.error("[REPORTS] Error fetching latest reports", e);
//            throw new RuntimeException("Failed to fetch latest reports", e);
//        }
//    }
//
//    // ============================================================
//    // USER ANALYTICS ENDPOINTS
//    // ============================================================
//
//    /**
//     * GET /api/reports/user/{userId}
//     *
//     * Retrieve user analytics for specific date.
//     *
//     * Access: ADMIN, SUPPORT, CUSTOMER (own data only)
//     *
//     * @param userId User ID
//     * @param date Analytics date (YYYY-MM-DD)
//     * @return User analytics with transaction metrics
//     */
//    @GetMapping("/user/{userId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT') or @securityService.isCurrentUser(#userId)")
//    public ResponseEntity<UserAnalyticsResponse> getUserAnalytics(
//        @PathVariable Long userId,
//        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
//    ) {
//        log.info("[REPORTS] GET /user/{} - Fetching analytics for date {}", userId, date);
//        try {
//            UserAnalytics analytics = userAnalyticsService.getUserAnalytics(userId, date);
//            return ResponseEntity.ok(mapUserAnalyticsToResponse(analytics));
//        } catch (Exception e) {
//            log.error("[REPORTS] Error fetching user analytics for user {}", userId, e);
//            throw new RuntimeException("Failed to fetch user analytics", e);
//        }
//    }
//
//    /**
//     * GET /api/reports/user/{userId}/history
//     *
//     * Retrieve user analytics for date range.
//     *
//     * Access: ADMIN, SUPPORT, CUSTOMER (own data only)
//     *
//     * @param userId User ID
//     * @param startDate Start date (YYYY-MM-DD)
//     * @param endDate End date (YYYY-MM-DD)
//     * @return List of user analytics
//     */
//    @GetMapping("/user/{userId}/history")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT') or @securityService.isCurrentUser(#userId)")
//    public ResponseEntity<List<UserAnalyticsResponse>> getUserAnalyticsHistory(
//        @PathVariable Long userId,
//        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        log.info("[REPORTS] GET /user/{}/history - Fetching analytics from {} to {}", userId, startDate, endDate);
//        try {
//            List<UserAnalytics> analytics = userAnalyticsService.getUserAnalyticsByDateRange(userId, startDate, endDate);
//            List<UserAnalyticsResponse> responses = analytics.stream()
//                .map(this::mapUserAnalyticsToResponse)
//                .collect(Collectors.toList());
//            return ResponseEntity.ok(responses);
//        } catch (Exception e) {
//            log.error("[REPORTS] Error fetching user analytics history", e);
//            throw new RuntimeException("Failed to fetch user analytics history", e);
//        }
//    }
//
//    // ============================================================
//    // MERCHANT ANALYTICS ENDPOINTS
//    // ============================================================
//
//    /**
//     * GET /api/reports/merchant/{merchantId}
//     *
//     * Retrieve merchant analytics for specific date.
//     *
//     * Access: ADMIN, SUPPORT, MERCHANT (own data only)
//     *
//     * @param merchantId Merchant ID
//     * @param date Analytics date (YYYY-MM-DD)
//     * @return Merchant analytics with performance metrics
//     */
//    @GetMapping("/merchant/{merchantId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT') or @securityService.isCurrentMerchant(#merchantId)")
//    public ResponseEntity<MerchantAnalyticsResponse> getMerchantAnalytics(
//        @PathVariable Long merchantId,
//        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
//    ) {
//        log.info("[REPORTS] GET /merchant/{} - Fetching analytics for date {}", merchantId, date);
//        try {
//            MerchantAnalytics analytics = merchantAnalyticsService.getMerchantAnalytics(merchantId, date);
//            return ResponseEntity.ok(mapMerchantAnalyticsToResponse(analytics));
//        } catch (Exception e) {
//            log.error("[REPORTS] Error fetching merchant analytics for merchant {}", merchantId, e);
//            throw new RuntimeException("Failed to fetch merchant analytics", e);
//        }
//    }
//
//    // ============================================================
//    // MAPPING METHODS
//    // ============================================================
//
//    /**
//     * Map DailyReport entity to response DTO
//     */
//    private DailyReportResponse mapDailyReportToResponse(DailyReport report) {
//        return DailyReportResponse.builder()
//            .id(report.getId())
//            .reportDate(report.getReportDate())
//            .totalTransactions(report.getTotalTransactions())
//            .totalTransactionVolume(report.getTotalTransactionVolume())
//            .successfulTransactions(report.getSuccessfulTransactions())
//            .failedTransactions(report.getFailedTransactions())
//            .pendingTransactions(report.getPendingTransactions())
//            .successRate(report.getSuccessRate())
//            .totalReversals(report.getTotalReversals())
//            .totalReversalVolume(report.getTotalReversalVolume())
//            .reversalRate(report.getReversalRate())
//            .totalRefunds(report.getTotalRefunds())
//            .totalRefundVolume(report.getTotalRefundVolume())
//            .refundRate(report.getRefundRate())
//            .totalDisputes(report.getTotalDisputes())
//            .newDisputes(report.getNewDisputes())
//            .resolvedDisputes(report.getResolvedDisputes())
//            .disputeRate(report.getDisputeRate())
//            .totalDisputedAmount(report.getTotalDisputedAmount())
//            .totalSettlements(report.getTotalSettlements())
//            .totalSettlementVolume(report.getTotalSettlementVolume())
//            .completedSettlements(report.getCompletedSettlements())
//            .failedSettlements(report.getFailedSettlements())
//            .settlementSuccessRate(report.getSettlementSuccessRate())
//            .totalFeesCollected(report.getTotalFeesCollected())
//            .fxFeesCollected(report.getFxFeesCollected())
//            .serviceFeesCollected(report.getServiceFeesCollected())
//            .averageFeePercentage(report.getAverageFeePercentage())
//            .uniqueCurrencies(report.getUniqueCurrencies())
//            .topCurrency(report.getTopCurrency())
//            .isFinalized(report.getIsFinalized())
//            .build();
//    }
//
//    /**
//     * Map UserAnalytics entity to response DTO
//     */
//    private UserAnalyticsResponse mapUserAnalyticsToResponse(UserAnalytics analytics) {
//        return UserAnalyticsResponse.builder()
//            .id(analytics.getId())
//            .userId(analytics.getUserId())
//            .analyticsDate(analytics.getAnalyticsDate())
//            .userType(analytics.getUserType())
//            .transactionCount(analytics.getTransactionCount())
//            .transactionVolume(analytics.getTransactionVolume())
//            .successfulTransactions(analytics.getSuccessfulTransactions())
//            .failedTransactions(analytics.getFailedTransactions())
//            .averageTransactionAmount(analytics.getAverageTransactionAmount())
//            .maxTransactionAmount(analytics.getMaxTransactionAmount())
//            .minTransactionAmount(analytics.getMinTransactionAmount())
//            .reversalCount(analytics.getReversalCount())
//            .reversalVolume(analytics.getReversalVolume())
//            .reversalRate(analytics.getReversalRate())
//            .disputeCount(analytics.getDisputeCount())
//            .disputeVolume(analytics.getDisputeVolume())
//            .disputeRate(analytics.getDisputeRate())
//            .totalFeesPaid(analytics.getTotalFeesPaid())
//            .averageFeePercentage(analytics.getAverageFeePercentage())
//            .primaryCurrency(analytics.getPrimaryCurrency())
//            .currencyCount(analytics.getCurrencyCount())
//            .riskScore(analytics.getRiskScore())
//            .complianceStatus(analytics.getComplianceStatus())
//            .build();
//    }
//
//    /**
//     * Map MerchantAnalytics entity to response DTO
//     */
//    private MerchantAnalyticsResponse mapMerchantAnalyticsToResponse(MerchantAnalytics analytics) {
//        return MerchantAnalyticsResponse.builder()
//            .id(analytics.getId())
//            .merchantId(analytics.getMerchantId())
//            .analyticsDate(analytics.getAnalyticsDate())
//            .totalTransactionCount(analytics.getTotalTransactionCount())
//            .totalVolume(analytics.getTotalVolume())
//            .netRevenue(analytics.getNetRevenue())
//            .grossRevenue(analytics.getGrossRevenue())
//            .totalFeesPaid(analytics.getTotalFeesPaid())
//            .averageTransactionValue(analytics.getAverageTransactionValue())
//            .settlementCount(analytics.getSettlementCount())
//            .settlementVolume(analytics.getSettlementVolume())
//            .successfulSettlements(analytics.getSuccessfulSettlements())
//            .failedSettlements(analytics.getFailedSettlements())
//            .settlementSuccessRate(analytics.getSettlementSuccessRate())
//            .settlementDelayAverageHours(analytics.getSettlementDelayAverageHours())
//            .disputeCount(analytics.getDisputeCount())
//            .disputeVolume(analytics.getDisputeVolume())
//            .disputeRate(analytics.getDisputeRate())
//            .resolvedDisputes(analytics.getResolvedDisputes())
//            .merchantWonDisputes(analytics.getMerchantWonDisputes())
//            .chargebackRate(analytics.getChargebackRate())
//            .transactionSuccessRate(analytics.getTransactionSuccessRate())
//            .reversalRate(analytics.getReversalRate())
//            .averageResponseTimeMs(analytics.getAverageResponseTimeMs())
//            .merchantTier(analytics.getMerchantTier())
//            .riskScore(analytics.getRiskScore())
//            .complianceStatus(analytics.getComplianceStatus())
//            .primaryCurrency(analytics.getPrimaryCurrency())
//            .currencyCount(analytics.getCurrencyCount())
//            .countryCount(analytics.getCountryCount())
//            .build();
//    }
//}
