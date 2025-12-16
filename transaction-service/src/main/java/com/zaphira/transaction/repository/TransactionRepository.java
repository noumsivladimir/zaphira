package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.zaphira.common.model.entities.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	java.util.List<Transaction> findBySenderWallet(Wallet senderWallet);

	java.util.List<Transaction> findByReceiverWallet(Wallet receiverWallet);

	// ============================================================
	// ADVANCED SEARCH QUERIES
	// ============================================================

	/**
	 * Find transactions by sender wallet number with pagination
	 */
	Page<Transaction> findBySenderWalletNumber(String senderWalletNumber, Pageable pageable);

	/**
	 * Find transactions by receiver wallet number with pagination
	 */
	Page<Transaction> findByReceiverWalletNumber(String receiverWalletNumber, Pageable pageable);

	/**
	 * Find transactions by amount range
	 */
	Page<Transaction> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

	/**
	 * Find transactions by status and created date range
	 */
	Page<Transaction> findByStatusAndCreatedAtBetween(
			TransactionStatus status,
			LocalDateTime fromDate,
			LocalDateTime toDate,
			Pageable pageable);

	/**
	 * Find transactions by type and currency
	 */
	Page<Transaction> findByTypeAndCurrency(TransactionType type, String currency, Pageable pageable);

	/**
	 * Advanced search with multiple filter criteria using native JPQL query.
	 * Supports filtering by:
	 * - Sender/Receiver wallet numbers
	 * - Transaction status
	 * - Transaction type
	 * - Amount range
	 * - Date range
	 * - Reference
	 * - Currency
	 * - Route
	 * - Scheduled flag
	 *
	 * @param senderWalletNumber Sender wallet number (optional, null = no filter)
	 * @param receiverWalletNumber Receiver wallet number (optional, null = no filter)
	 * @param status Transaction status (optional, null = no filter)
	 * @param type Transaction type (optional, null = no filter)
	 * @param amountMin Minimum amount (optional, null = no filter)
	 * @param amountMax Maximum amount (optional, null = no filter)
	 * @param createdFrom Start date (optional, null = no filter)
	 * @param createdTo End date (optional, null = no filter)
	 * @param reference Transaction reference (optional, null = no filter)
	 * @param currency Currency code (optional, null = no filter)
	 * @param route Transaction route (optional, null = no filter)
	 * @param scheduled Scheduled flag (optional, null = no filter)
	 * @param pageable Pagination and sorting information
	 * @return Paginated search results
	 */
	@Query("SELECT t FROM Transaction t WHERE " +
			"(:senderWalletNumber IS NULL OR t.senderWalletNumber = :senderWalletNumber) AND " +
			"(:receiverWalletNumber IS NULL OR t.receiverWalletNumber = :receiverWalletNumber) AND " +
			"(:status IS NULL OR t.status = :status) AND " +
			"(:type IS NULL OR t.type = :type) AND " +
			"(:amountMin IS NULL OR t.amount >= :amountMin) AND " +
			"(:amountMax IS NULL OR t.amount <= :amountMax) AND " +
			"(:createdFrom IS NULL OR t.createdAt >= :createdFrom) AND " +
			"(:createdTo IS NULL OR t.createdAt <= :createdTo) AND " +
			"(:reference IS NULL OR UPPER(t.reference) LIKE UPPER(CONCAT('%', :reference, '%'))) AND " +
			"(:currency IS NULL OR t.currency = :currency) AND " +
			"(:route IS NULL OR t.route = :route) AND " +
			"(:scheduled IS NULL OR t.scheduled = :scheduled) " +
			"ORDER BY t.createdAt DESC")
	Page<Transaction> searchTransactions(
			@Param("senderWalletNumber") String senderWalletNumber,
			@Param("receiverWalletNumber") String receiverWalletNumber,
			@Param("status") TransactionStatus status,
			@Param("type") TransactionType type,
			@Param("amountMin") BigDecimal amountMin,
			@Param("amountMax") BigDecimal amountMax,
			@Param("createdFrom") LocalDateTime createdFrom,
			@Param("createdTo") LocalDateTime createdTo,
			@Param("reference") String reference,
			@Param("currency") String currency,
			@Param("route") String route,
			@Param("scheduled") Boolean scheduled,
			Pageable pageable);

	/**
	 * Count transactions by status
	 */
	long countByStatus(TransactionStatus status);

	/**
	 * Find transactions created within a date range
	 */
	java.util.List<Transaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
	
	/**
	 * Find transactions by sender wallet user ID
	 */
	java.util.List<Transaction> findBySenderWallet_UserId(Long userId);
	
	/**
	 * Find transactions by sender wallet user ID and date range
	 */
	java.util.List<Transaction> findBySenderWallet_UserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
	
	/**
	 * Find transactions by receiver wallet user ID
	 */
	java.util.List<Transaction> findByReceiverWallet_UserId(Long userId);
	
	/**
	 * Find transactions by receiver wallet user ID and date range
	 */
	java.util.List<Transaction> findByReceiverWallet_UserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
}


