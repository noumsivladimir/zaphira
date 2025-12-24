package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DailyReportRepository - Repository for daily transaction reports
 * 
 * Pattern (from Phase 2-4):
 * - Custom @Query methods for complex queries
 * - Named parameter patterns (@Param)
 * - Proper indexing for performance
 */
@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    
    /**
     * Find report for specific date
     */
    Optional<DailyReport> findByReportDate(LocalDate reportDate);
    
    /**
     * Find reports within date range (for dashboard)
     */
    @Query("SELECT r FROM DailyReport r WHERE r.reportDate BETWEEN :startDate AND :endDate ORDER BY r.reportDate DESC")
    List<DailyReport> findReportsByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find latest N reports
     */
    @Query(value = "SELECT * FROM daily_reports ORDER BY report_date DESC LIMIT :limit", nativeQuery = true)
    List<DailyReport> findLatestReports(int limit);
    
    /**
     * Find finalized reports only
     */
    @Query("SELECT r FROM DailyReport r WHERE r.isFinalized = true AND r.reportDate BETWEEN :startDate AND :endDate ORDER BY r.reportDate DESC")
    List<DailyReport> findFinalizedReports(LocalDate startDate, LocalDate endDate);
    
    /**
     * Check if report exists for date
     */
    boolean existsByReportDate(LocalDate reportDate);
    
    /**
     * Find reports with high dispute rates (for monitoring)
     */
    @Query("SELECT r FROM DailyReport r WHERE r.disputeRate > :disputeThreshold AND r.reportDate BETWEEN :startDate AND :endDate")
    List<DailyReport> findHighDisputeRateReports(BigDecimal disputeThreshold, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find reports with low settlement success rates (for alerts)
     */
    @Query("SELECT r FROM DailyReport r WHERE r.settlementSuccessRate < :successThreshold AND r.reportDate BETWEEN :startDate AND :endDate")
    List<DailyReport> findLowSettlementSuccessReports(BigDecimal successThreshold, LocalDate startDate, LocalDate endDate);
}
