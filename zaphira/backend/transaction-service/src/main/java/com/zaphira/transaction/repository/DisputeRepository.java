package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.Dispute;
import com.zaphira.transaction.model.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DisputeRepository - Data access layer for Dispute entities.
 * 
 * Provides custom query methods for common dispute lookups:
 * - Find by transaction ID
 * - Find by status (for filtering/reporting)
 * - Find by deadline (for expiration jobs)
 * - Find by initiator (for user's disputes list)
 * 
 * Used by DisputeService and DisputeResolutionService.
 */
@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    
    /**
     * Find dispute by transaction ID.
     * Used to check if dispute already exists and get dispute for transaction.
     */
    Optional<Dispute> findByTransactionId(Long transactionId);

    /**
     * Check if dispute exists for transaction.
     */
    boolean existsByTransactionId(Long transactionId);
    
    /**
     * Find all disputes with specific status.
     * Used for filtering and reporting.
     * 
     * Example: Find all UNDER_INVESTIGATION disputes
     */
    List<Dispute> findByStatus(DisputeStatus status);
    
    /**
     * Find all disputes initiated by specific user.
     * Used to show user their disputes list.
     */
    List<Dispute> findByInitiatedBy(String userEmail);
    
    /**
     * Find disputes with deadline approaching or passed.
     * Used by scheduled job to auto-expire disputes.
     */
    @Query("SELECT d FROM Dispute d WHERE d.deadlineAt < :deadline AND d.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Dispute> findWithApproachingDeadline(@Param("deadline") LocalDateTime deadline);
    
    /**
     * Find all unresolved disputes.
     * Used for dashboards and reports.
     */
    @Query("SELECT d FROM Dispute d WHERE d.status NOT IN ('RESOLVED', 'CLOSED', 'EXPIRED')")
    List<Dispute> findUnresolvedDisputes();
    
    /**
     * Count disputes by status.
     * Used for metrics and monitoring.
     */
    long countByStatus(DisputeStatus status);
    
    /**
     * Find disputes created within a date range.
     * Used for daily reporting and analytics.
     */
    List<Dispute> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
