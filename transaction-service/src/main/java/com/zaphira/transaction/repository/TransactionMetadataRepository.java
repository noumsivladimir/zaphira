package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.core.TransactionMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TransactionMetadataRepository - LOT 2
 */
@Repository
public interface TransactionMetadataRepository extends JpaRepository<TransactionMetadata, Long> {

    /**
     * Find metadata by transaction ID
     */
    Optional<TransactionMetadata> findByTransactionId(Long transactionId);

    /**
     * Find transactions with max retries reached
     */
    @Query("SELECT m FROM TransactionMetadata m WHERE m.retryCount >= m.maxRetryAttempts")
    List<TransactionMetadata> findTransactionsWithMaxRetriesReached();

    /**
     * Count failed transactions with specific reason
     */
    @Query("SELECT COUNT(m) FROM TransactionMetadata m WHERE m.failureReason LIKE %:reason%")
    long countByFailureReasonContaining(@Param("reason") String reason);
}
