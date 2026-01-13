package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository
		extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /* =========================
       BASIC LOOKUPS
       ========================= */

	Optional<Transaction> findByReference(String reference);

	boolean existsByReference(String reference);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
        SELECT t
        FROM Transaction t
        WHERE t.reference = :reference
    """)
	Optional<Transaction> findByReferenceWithLock(@Param("reference") String reference);

    /* =========================
       WALLET BASED (ID ONLY)
       ========================= */

	List<Transaction> findBySenderWalletId(Long senderWalletId);

	List<Transaction> findByReceiverWalletId(Long receiverWalletId);

	@Query("""
        SELECT t
        FROM Transaction t
        WHERE t.senderWalletId = :walletId
           OR t.receiverWalletId = :walletId
    """)
	Page<Transaction> findByWalletId(
			@Param("walletId") Long walletId,
			Pageable pageable
	);

    /* =========================
       USER BASED
       ========================= */

//	List<Transaction> findByUserId(Long userId);
//
//	Page<Transaction> findByUserId(Long userId, Pageable pageable);
//
//	List<Transaction> findByUserIdAndStatus(Long userId, TransactionStatus status);
//
//	List<Transaction> findByUserIdAndCreatedAtBetween(
//			Long userId,
//			LocalDateTime startDate,
//			LocalDateTime endDate
//	);

    /* =========================
       DATE & STATUS
       ========================= */

	List<Transaction> findByStatus(TransactionStatus status);

	List<Transaction> findByCreatedAtBetween(
			LocalDateTime from,
			LocalDateTime to
	);

	Page<Transaction> findByStatusAndCreatedAtBetween(
			TransactionStatus status,
			LocalDateTime from,
			LocalDateTime to,
			Pageable pageable
	);

    /* =========================
       ANALYTICS / LIMITS
       ========================= */

	@Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM Transaction t
    WHERE t.type = :type
      AND t.status = 'COMPLETED'
      AND t.createdAt BETWEEN :start AND :end
""")
	BigDecimal sumCompletedAmountByType(
			@Param("type") TransactionType type,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);


	@Query("""
    SELECT COUNT(t)
    FROM Transaction t
    WHERE t.status = 'COMPLETED'
      AND t.createdAt >= :since
""")
	long countCompletedSince(
			@Param("since") LocalDateTime since
	);


    /* =========================
       SEARCH (BACKOFFICE)
       ========================= */

	@Query("""
        SELECT t
        FROM Transaction t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:type IS NULL OR t.type = :type)
          AND (:currency IS NULL OR t.currency = :currency)

          AND (:minAmount IS NULL OR t.amount >= :minAmount)
          AND (:maxAmount IS NULL OR t.amount <= :maxAmount)
          AND (:from IS NULL OR t.createdAt >= :from)
          AND (:to IS NULL OR t.createdAt <= :to)
          AND (:reference IS NULL OR t.reference LIKE CONCAT('%', :reference, '%'))
    """)
	Page<Transaction> search(
			@Param("status") TransactionStatus status,
			@Param("type") TransactionType type,
			@Param("currency") String currency,

			@Param("minAmount") BigDecimal minAmount,
			@Param("maxAmount") BigDecimal maxAmount,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			@Param("reference") String reference,
			Pageable pageable
	);

    /* =========================
       REVERSAL / LINKED TX
       ========================= */

	List<Transaction> findByRelatedTransactionId(Long relatedTransactionId);

    /* =========================
       TYPE BASED
       ========================= */

	List<Transaction> findByType(TransactionType type);
}
