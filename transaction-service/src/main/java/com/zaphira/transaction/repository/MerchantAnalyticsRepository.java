package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.MerchantAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * MerchantAnalyticsRepository - Repository for merchant-level analytics
 * 
 * Supports merchant performance tracking and tier classification
 */
@Repository
public interface MerchantAnalyticsRepository extends JpaRepository<MerchantAnalytics, Long> {
    
    /**
     * Find analytics for specific merchant on specific date
     */
    Optional<MerchantAnalytics> findByMerchantIdAndAnalyticsDate(Long merchantId, LocalDate analyticsDate);
    
    /**
     * Find latest analytics for merchant (most recent date)
     */
    @Query("SELECT m FROM MerchantAnalytics m WHERE m.merchantId = :merchantId ORDER BY m.analyticsDate DESC LIMIT 1")
    Optional<MerchantAnalytics> findLatestByMerchantId(@Param("merchantId") Long merchantId);
    
    /**
     * Find analytics for merchant within date range
     */
    @Query("SELECT m FROM MerchantAnalytics m WHERE m.merchantId = :merchantId AND m.analyticsDate BETWEEN :startDate AND :endDate ORDER BY m.analyticsDate DESC")
    List<MerchantAnalytics> findByMerchantIdAndDateRange(
        @Param("merchantId") Long merchantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Find all analytics for specific date
     */
    @Query("SELECT m FROM MerchantAnalytics m WHERE m.analyticsDate = :analyticsDate ORDER BY m.totalVolume DESC")
    List<MerchantAnalytics> findByAnalyticsDate(@Param("analyticsDate") LocalDate analyticsDate);
    
    /**
     * Find top merchants by volume on specific date
     */
    @Query("SELECT m FROM MerchantAnalytics m WHERE m.analyticsDate = :analyticsDate ORDER BY m.totalVolume DESC LIMIT :limit")
    List<MerchantAnalytics> findTopMerchantsByVolume(
        @Param("analyticsDate") LocalDate analyticsDate,
        @Param("limit") int limit
    );
    
    /**
     * Find merchants by tier
     */
    @Query("SELECT m FROM MerchantAnalytics m WHERE m.merchantTier = :tier AND m.analyticsDate = :analyticsDate ORDER BY m.totalVolume DESC")
    List<MerchantAnalytics> findByMerchantTier(
        @Param("tier") String tier,
        @Param("analyticsDate") LocalDate analyticsDate
    );
    
    /**
     * Find high-risk merchants (for monitoring)
     */
    @Query("SELECT m FROM MerchantAnalytics m WHERE m.riskScore > :riskThreshold AND m.analyticsDate = :analyticsDate ORDER BY m.riskScore DESC")
    List<MerchantAnalytics> findHighRiskMerchants(
        @Param("analyticsDate") LocalDate analyticsDate,
        @Param("riskThreshold") java.math.BigDecimal riskThreshold
    );
    
    /**
     * Find merchants with high chargeback rates (for investigation)
     */
    @Query("SELECT m FROM MerchantAnalytics m WHERE m.chargebackRate > :chargebackThreshold AND m.analyticsDate BETWEEN :startDate AND :endDate ORDER BY m.chargebackRate DESC")
    List<MerchantAnalytics> findHighChargebackMerchants(
        @Param("chargebackThreshold") java.math.BigDecimal chargebackThreshold,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Find merchants with low settlement success rates
     */
    @Query("SELECT m FROM MerchantAnalytics m WHERE m.settlementSuccessRate < :successThreshold AND m.analyticsDate = :analyticsDate ORDER BY m.settlementSuccessRate ASC")
    List<MerchantAnalytics> findLowSettlementSuccessMerchants(
        @Param("analyticsDate") LocalDate analyticsDate,
        @Param("successThreshold") java.math.BigDecimal successThreshold
    );
}
