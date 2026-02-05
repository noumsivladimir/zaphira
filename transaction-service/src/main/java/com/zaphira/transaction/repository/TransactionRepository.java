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

    /* =========================
       LOT 4: USER-SCOPED QUERIES
       ========================= */

    // Find all transactions where user is sender or receiver
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.senderWalletId = :userId OR t.receiverWalletId = :userId
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // Find user transactions by status
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE (t.senderWalletId = :userId OR t.receiverWalletId = :userId)
          AND t.status = :status
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            Pageable pageable
    );

    // Find user transactions by type
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE (t.senderWalletId = :userId OR t.receiverWalletId = :userId)
          AND t.type = :type
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            Pageable pageable
    );

    // Find user transactions by status and type
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE (t.senderWalletId = :userId OR t.receiverWalletId = :userId)
          AND t.status = :status
          AND t.type = :type
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByUserIdAndStatusAndType(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            Pageable pageable
    );

    // Find user transactions by date range, status, and type
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE (t.senderWalletId = :userId OR t.receiverWalletId = :userId)
          AND t.status = :status
          AND t.type = :type
          AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByUserIdAndStatusAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // Find transactions sent by user
    Page<Transaction> findBySenderWalletId(Long senderWalletId, Pageable pageable);

    // Find transactions sent by user with status
    Page<Transaction> findBySenderWalletIdAndStatus(Long senderWalletId, TransactionStatus status, Pageable pageable);

    // Find transactions received by user
    Page<Transaction> findByReceiverWalletId(Long receiverWalletId, Pageable pageable);

    // Find transactions received by user with status
    Page<Transaction> findByReceiverWalletIdAndStatus(Long receiverWalletId, TransactionStatus status, Pageable pageable);

    /* =========================
       LOT 4: MERCHANT REPORT QUERIES
       ========================= */

    // Find merchant transactions by date range
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.receiverWalletId = :receiverWalletId
          AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findByReceiverWalletIdAndCreatedAtBetween(
            @Param("receiverWalletId") Long receiverWalletId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // Find merchant transactions by date range paginated
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.receiverWalletId = :receiverWalletId
          AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByReceiverWalletIdAndCreatedAtBetween(
            @Param("receiverWalletId") Long receiverWalletId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // Find merchant transactions by status and date range
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.receiverWalletId = :receiverWalletId
          AND t.status = :status
          AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByReceiverWalletIdAndStatusAndCreatedAtBetween(
            @Param("receiverWalletId") Long receiverWalletId,
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // Find merchant transactions by status list
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.receiverWalletId = :receiverWalletId
          AND t.status IN :statuses
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findByReceiverWalletIdAndStatusIn(
            @Param("receiverWalletId") Long receiverWalletId,
            @Param("statuses") List<TransactionStatus> statuses,
            Pageable pageable
    );

    // Find merchant transactions by status list (non-paginated)
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.receiverWalletId = :receiverWalletId
          AND t.status IN :statuses
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findByReceiverWalletIdAndStatusIn(
            @Param("receiverWalletId") Long receiverWalletId,
            @Param("statuses") List<TransactionStatus> statuses
    );

    // Find refunds issued by merchant
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.senderWalletId = :senderWalletId
          AND t.type = :type
          AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findBySenderWalletIdAndTypeAndCreatedAtBetween(
            @Param("senderWalletId") Long senderWalletId,
            @Param("type") TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // Count transactions by receiver and date range
    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.receiverWalletId = :receiverWalletId
          AND t.createdAt BETWEEN :from AND :to
    """)
    Long countByReceiverWalletIdAndCreatedAtBetween(
            @Param("receiverWalletId") Long receiverWalletId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
