package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.core.TransactionFees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * TransactionFeesRepository - LOT 2
 */
@Repository
public interface TransactionFeesRepository extends JpaRepository<TransactionFees, Long> {

    /**
     * Find fees by transaction ID
     */
    Optional<TransactionFees> findByTransactionId(Long transactionId);

    /**
     * Check if transaction has fees
     */
    boolean existsByTransactionId(Long transactionId);

    /**
     * Calculate total platform fees for a period
     */
    @Query("SELECT COALESCE(SUM(f.platformFee), 0) FROM TransactionFees f WHERE f.transaction.id IN " +
           "(SELECT t.id FROM TransactionCore t WHERE t.completedAt BETWEEN :startDate AND :endDate AND t.status = 'COMPLETED')")
    BigDecimal sumPlatformFeesByPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                                       @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Calculate total merchant fees for a period
     */
    @Query("SELECT COALESCE(SUM(f.merchantFee), 0) FROM TransactionFees f WHERE f.transaction.id IN " +
           "(SELECT t.id FROM TransactionCore t WHERE t.completedAt BETWEEN :startDate AND :endDate AND t.status = 'COMPLETED')")
    BigDecimal sumMerchantFeesByPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                                       @Param("endDate") java.time.LocalDateTime endDate);
}
