package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.entities.RecurringTransaction;
import com.zaphira.transaction.model.enums.RecurringStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    /**
     * Find all recurring transactions for a user.
     */
    Page<RecurringTransaction> findByUserId(Long userId, Pageable pageable);

    /**
     * Find recurring transactions by user and status.
     */
    Page<RecurringTransaction> findByUserIdAndStatus(Long userId, RecurringStatus status, Pageable pageable);

    /**
     * Find single recurring transaction by ID and user (for ownership check).
     */
    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);

    /**
     * Find all active/pending recurring transactions ready to execute.
     */
    @Query("SELECT r FROM RecurringTransaction r WHERE " +
           "(r.status = 'PENDING' OR r.status = 'ACTIVE') AND " +
           "r.nextRunAt <= :now AND " +
           "(r.endDate IS NULL OR r.nextRunAt < r.endDate) AND " +
           "(r.maxExecutions IS NULL OR r.executionCount < r.maxExecutions)")
    List<RecurringTransaction> findReadyToExecute(@Param("now") LocalDateTime now);

    /**
     * Find recurring transactions that should be completed.
     */
    @Query("SELECT r FROM RecurringTransaction r WHERE " +
           "(r.status = 'PENDING' OR r.status = 'ACTIVE') AND " +
           "((r.endDate IS NOT NULL AND :now > r.endDate) OR " +
           "(r.maxExecutions IS NOT NULL AND r.executionCount >= r.maxExecutions))")
    List<RecurringTransaction> findShouldComplete(@Param("now") LocalDateTime now);

    /**
     * Find recurring transactions that should be marked as failed.
     */
    @Query("SELECT r FROM RecurringTransaction r WHERE " +
           "(r.status = 'PENDING' OR r.status = 'ACTIVE') AND " +
           "r.consecutiveFailures >= r.maxConsecutiveFailures")
    List<RecurringTransaction> findShouldMarkAsFailed();

    /**
     * Count active recurring transactions for a user.
     */
    Long countByUserIdAndStatus(Long userId, RecurringStatus status);
}
