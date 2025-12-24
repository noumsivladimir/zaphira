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
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	List<Transaction> findBySenderWallet(Wallet senderWallet);

	List<Transaction> findByReceiverWallet(Wallet receiverWallet);

	List<Transaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

	List<Transaction> findBySenderWallet_UserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);

	List<Transaction> findByReceiverWallet_UserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);

	Page<Transaction> findBySenderWalletNumber(String senderWalletNumber, Pageable pageable);

	Page<Transaction> findByReceiverWalletNumber(String receiverWalletNumber, Pageable pageable);

	Page<Transaction> findByAmountBetween(BigDecimal amountMin, BigDecimal amountMax, Pageable pageable);

	Page<Transaction> findByStatusAndCreatedAtBetween(TransactionStatus status, LocalDateTime createdFrom, LocalDateTime createdTo, Pageable pageable);

	Page<Transaction> findByTypeAndCurrency(TransactionType type, String currency, Pageable pageable);

	@Query("SELECT t FROM Transaction t WHERE " +
		"(:senderWalletNumber IS NULL OR t.senderWalletNumber = :senderWalletNumber) AND " +
		"(:receiverWalletNumber IS NULL OR t.receiverWalletNumber = :receiverWalletNumber) AND " +
		"(:status IS NULL OR t.status = :status) AND " +
		"(:type IS NULL OR t.type = :type) AND " +
		"(:amountMin IS NULL OR t.amount >= :amountMin) AND " +
		"(:amountMax IS NULL OR t.amount <= :amountMax) AND " +
		"(:createdFrom IS NULL OR t.createdAt >= :createdFrom) AND " +
		"(:createdTo IS NULL OR t.createdAt <= :createdTo) AND " +
		"(:reference IS NULL OR t.reference LIKE CONCAT('%', :reference, '%')) AND " +
		"(:currency IS NULL OR t.currency = :currency) AND " +
		"(:route IS NULL OR t.route = :route) AND " +
		"(:scheduled IS NULL OR t.scheduled = :scheduled)")
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
		Pageable pageable
	);

	// Test helper methods
	List<Transaction> findByStatus(TransactionStatus status);

	List<Transaction> findByType(TransactionType type);
}


