package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.UserAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * UserAnalyticsRepository - Repository for user-level analytics
 * 
 * Supports both customer and merchant analytics
 */
@Repository
public interface UserAnalyticsRepository extends JpaRepository<UserAnalytics, Long> {
    
    /**
     * Find analytics for specific user on specific date
     */
    Optional<UserAnalytics> findByUserIdAndAnalyticsDate(Long userId, LocalDate analyticsDate);
    
    /**
     * Find latest analytics for user (most recent date)
     */
    @Query("SELECT u FROM UserAnalytics u WHERE u.userId = :userId ORDER BY u.analyticsDate DESC LIMIT 1")
    Optional<UserAnalytics> findLatestByUserId(@Param("userId") Long userId);
    
    /**
     * Find analytics for user within date range
     */
    @Query("SELECT u FROM UserAnalytics u WHERE u.userId = :userId AND u.analyticsDate BETWEEN :startDate AND :endDate ORDER BY u.analyticsDate DESC")
    List<UserAnalytics> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Find all analytics for specific date (for reporting)
     */
    @Query("SELECT u FROM UserAnalytics u WHERE u.analyticsDate = :analyticsDate ORDER BY u.transactionVolume DESC")
    List<UserAnalytics> findByAnalyticsDate(@Param("analyticsDate") LocalDate analyticsDate);
    
    /**
     * Find top users by transaction volume for specific date
     */
    @Query("SELECT u FROM UserAnalytics u WHERE u.analyticsDate = :analyticsDate AND u.userType = :userType ORDER BY u.transactionVolume DESC LIMIT :limit")
    List<UserAnalytics> findTopUsersByVolume(
        @Param("analyticsDate") LocalDate analyticsDate,
        @Param("userType") String userType,
        @Param("limit") int limit
    );
    
    /**
     * Find high-risk users (for monitoring)
     */
    @Query("SELECT u FROM UserAnalytics u WHERE u.riskScore > :riskThreshold AND u.analyticsDate = :analyticsDate ORDER BY u.riskScore DESC")
    List<UserAnalytics> findHighRiskUsers(
        @Param("analyticsDate") LocalDate analyticsDate,
        @Param("riskThreshold") java.math.BigDecimal riskThreshold
    );
    
    /**
     * Find users with high dispute rates (for investigation)
     */
    @Query("SELECT u FROM UserAnalytics u WHERE u.disputeRate > :disputeThreshold AND u.analyticsDate BETWEEN :startDate AND :endDate ORDER BY u.disputeRate DESC")
    List<UserAnalytics> findHighDisputeRateUsers(
        @Param("disputeThreshold") java.math.BigDecimal disputeThreshold,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Count users by type on specific date
     */
    @Query("SELECT COUNT(DISTINCT u.userId) FROM UserAnalytics u WHERE u.analyticsDate = :analyticsDate AND u.userType = :userType")
    Long countUsersByType(@Param("analyticsDate") LocalDate analyticsDate, @Param("userType") String userType);
}
