package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.TransactionRefund;
import com.zaphira.transaction.model.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TransactionRefund entity
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
@Repository
public interface TransactionRefundRepository extends JpaRepository<TransactionRefund, Long> {
    
    /**
     * Find refund by reference number
     */
    Optional<TransactionRefund> findByRefundReference(String refundReference);
    
    /**
     * Find all refunds for a specific transaction
     */
    List<TransactionRefund> findByTransactionId(Long transactionId);
    
    /**
     * Find all refunds for a transaction with specific status
     */
    List<TransactionRefund> findByTransactionIdAndStatus(Long transactionId, RefundStatus status);
    
    /**
     * Find refunds by status
     */
    Page<TransactionRefund> findByStatus(RefundStatus status, Pageable pageable);
    
    /**
     * Find refunds initiated by specific user
     */
    Page<TransactionRefund> findByInitiatedBy(String initiatedBy, Pageable pageable);
    
    /**
     * Find pending refunds created before specific date
     */
    @Query("SELECT r FROM TransactionRefund r WHERE r.status = 'PENDING' AND r.createdAt < :beforeDate")
    List<TransactionRefund> findPendingRefundsBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Calculate total refunded amount for a transaction
     */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM TransactionRefund r " +
           "WHERE r.transaction.id = :transactionId AND r.status = 'COMPLETED'")
    BigDecimal calculateTotalRefundedAmount(@Param("transactionId") Long transactionId);
    
    /**
     * Check if transaction has pending refunds
     */
    boolean existsByTransactionIdAndStatusIn(Long transactionId, List<RefundStatus> statuses);
    
    /**
     * Count refunds by status
     */
    long countByStatus(RefundStatus status);
    
    /**
     * Find refunds created within date range
     */
    @Query("SELECT r FROM TransactionRefund r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    Page<TransactionRefund> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Find refunds for a specific transaction reference
     */
    @Query("SELECT r FROM TransactionRefund r WHERE r.transaction.transactionReference = :transactionReference")
    List<TransactionRefund> findByTransactionReference(@Param("transactionReference") String transactionReference);
}
