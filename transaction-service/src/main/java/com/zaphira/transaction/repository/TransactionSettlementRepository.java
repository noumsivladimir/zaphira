package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.TransactionSettlement;
import com.zaphira.transaction.model.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TransactionSettlementRepository - Settlement records data access.
 * 
 * Queries:
 * 1. findByTransactionId - Get settlement for transaction
 * 2. findByStatus - Find settlements by status (for batch processing)
 * 3. findFailedSettlements - For retry processing
 */
@Repository
public interface TransactionSettlementRepository extends JpaRepository<TransactionSettlement, Long> {
    
    /**
     * Find settlement by transaction ID.
     * 
     * @param transactionId Transaction ID
     * @return Optional containing settlement
     */
    Optional<TransactionSettlement> findByTransactionId(Long transactionId);
    
    /**
     * Find all settlements by status.
     * 
     * @param status Settlement status
     * @return List of settlements with given status
     */
    List<TransactionSettlement> findByStatus(SettlementStatus status);
    
    /**
     * Find failed settlements for retry.
     * 
     * @return List of FAILED settlements
     */
    @Query("SELECT s FROM TransactionSettlement s WHERE s.status = 'FAILED' ORDER BY s.createdAt ASC")
    List<TransactionSettlement> findFailedSettlements();
    
    /**
     * Find failed settlements that haven't been retried too many times.
     * 
     * @param maxRetries Maximum retry count
     * @return List of retryable settlements
     */
    @Query("SELECT s FROM TransactionSettlement s " +
           "WHERE s.status = 'FAILED' " +
           "AND s.retryCount < :maxRetries " +
           "ORDER BY s.updatedAt ASC")
    List<TransactionSettlement> findRetryableSettlements(@Param("maxRetries") Integer maxRetries);
    
    /**
     * Find settlements created within date range.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return List of settlements
     */
    @Query("SELECT s FROM TransactionSettlement s " +
           "WHERE s.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY s.createdAt DESC")
    List<TransactionSettlement> findSettlementsByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Count settlements by status.
     * 
     * @param status Settlement status
     * @return Count of settlements
     */
    Long countByStatus(SettlementStatus status);
    
    /**
     * Find settlements created within a date range (simplified method).
     * 
     * @param from Start date
     * @param to End date
     * @return List of settlements
     */
    List<TransactionSettlement> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
