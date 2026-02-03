package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.entities.RecurringTransactionExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecurringTransactionExecutionRepository extends JpaRepository<RecurringTransactionExecution, Long> {

    /**
     * Find all executions for a recurring transaction (paginated).
     */
    Page<RecurringTransactionExecution> findByRecurringTransactionId(Long recurringTransactionId, Pageable pageable);

    /**
     * Find all executions for a recurring transaction (list).
     */
    List<RecurringTransactionExecution> findByRecurringTransactionIdOrderByExecutedAtDesc(Long recurringTransactionId);

    /**
     * Find executions by status.
     */
    Page<RecurringTransactionExecution> findByRecurringTransactionIdAndExecutionStatus(
            Long recurringTransactionId, 
            String executionStatus, 
            Pageable pageable);

    /**
     * Find recent failed executions for a recurring transaction.
     */
    @Query("SELECT e FROM RecurringTransactionExecution e WHERE " +
           "e.recurringTransactionId = :recurringTransactionId AND " +
           "e.executionStatus = 'FAILED' " +
           "ORDER BY e.executedAt DESC")
    List<RecurringTransactionExecution> findRecentFailedExecutions(
            @Param("recurringTransactionId") Long recurringTransactionId, 
            Pageable pageable);

    /**
     * Count successful executions.
     */
    Long countByRecurringTransactionIdAndExecutionStatus(Long recurringTransactionId, String executionStatus);

    /**
     * Find executions within date range.
     */
    Page<RecurringTransactionExecution> findByRecurringTransactionIdAndExecutedAtBetween(
            Long recurringTransactionId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    /**
     * Delete old executions (cleanup job).
     */
    void deleteByExecutedAtBefore(LocalDateTime date);
}
