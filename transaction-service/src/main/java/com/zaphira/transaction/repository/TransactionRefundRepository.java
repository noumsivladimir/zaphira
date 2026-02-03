package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.entities.TransactionRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TransactionRefund entity
 */
@Repository
public interface TransactionRefundRepository extends JpaRepository<TransactionRefund, Long> {

    /**
     * Find refund by reference
     */
    Optional<TransactionRefund> findByRefundReference(String refundReference);

    /**
     * Find all refunds for an original transaction
     */
    List<TransactionRefund> findByOriginalTransactionId(Long originalTransactionId);

    /**
     * Find refund by the refund transaction ID
     */
    Optional<TransactionRefund> findByRefundTransactionId(Long refundTransactionId);

    /**
     * Check if transaction has been refunded
     */
    boolean existsByOriginalTransactionId(Long originalTransactionId);

    /**
     * Get total refunded amount for a transaction
     */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM TransactionRefund r " +
           "WHERE r.originalTransactionId = :transactionId AND r.status = 'COMPLETED'")
    java.math.BigDecimal getTotalRefundedAmount(@Param("transactionId") Long transactionId);

    /**
     * Find pending refunds
     */
    List<TransactionRefund> findByStatus(String status);

    /**
     * Find refunds performed by a specific user
     */
    List<TransactionRefund> findByPerformedByUserId(Long userId);
}
