package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.core.TransactionTimeline;
import com.zaphira.transaction.model.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TransactionTimelineRepository - LOT 2
 */
@Repository
public interface TransactionTimelineRepository extends JpaRepository<TransactionTimeline, Long> {

    /**
     * Get timeline for a transaction
     */
    List<TransactionTimeline> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);

    /**
     * Get timeline by status change
     */
    List<TransactionTimeline> findByNewStatusOrderByCreatedAtDesc(TransactionStatus newStatus);

    /**
     * Count status transitions
     */
    @Query("SELECT COUNT(t) FROM TransactionTimeline t WHERE t.previousStatus = :previousStatus AND t.newStatus = :newStatus")
    long countStatusTransitions(@Param("previousStatus") TransactionStatus previousStatus,
                                @Param("newStatus") TransactionStatus newStatus);
}
