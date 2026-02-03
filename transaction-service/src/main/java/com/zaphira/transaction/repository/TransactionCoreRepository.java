package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.core.TransactionCore;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
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
 * TransactionCoreRepository - LOT 1 Repository
 * 
 * Basic queries for minimal transaction operations
 */
@Repository
public interface TransactionCoreRepository extends JpaRepository<TransactionCore, Long> {

    /* =========================
       BASIC LOOKUPS
       ========================= */

    /**
     * Find transaction by unique reference
     */
    Optional<TransactionCore> findByReference(String reference);

    /**
     * Check if reference exists
     */
    boolean existsByReference(String reference);

    /* =========================
       WALLET-BASED QUERIES
       ========================= */

    /**
     * Get all transactions for a specific wallet (sender or receiver)
     */
    @Query("SELECT t FROM TransactionCore t WHERE t.senderWalletId = :walletId OR t.receiverWalletId = :walletId ORDER BY t.createdAt DESC")
    Page<TransactionCore> findByWalletId(@Param("walletId") Long walletId, Pageable pageable);

    /**
     * Get transactions sent from a wallet
     */
    Page<TransactionCore> findBySenderWalletIdOrderByCreatedAtDesc(Long senderWalletId, Pageable pageable);

    /**
     * Get transactions received by a wallet
     */
    Page<TransactionCore> findByReceiverWalletIdOrderByCreatedAtDesc(Long receiverWalletId, Pageable pageable);

    /* =========================
       STATUS-BASED QUERIES
       ========================= */

    /**
     * Find all transactions with specific status
     */
    List<TransactionCore> findByStatus(TransactionStatus status);

    /**
     * Find pending transactions for a wallet
     */
    @Query("SELECT t FROM TransactionCore t WHERE (t.senderWalletId = :walletId OR t.receiverWalletId = :walletId) AND t.status = :status")
    List<TransactionCore> findByWalletIdAndStatus(@Param("walletId") Long walletId, @Param("status") TransactionStatus status);

    /**
     * Count pending transactions for a wallet
     */
    @Query("SELECT COUNT(t) FROM TransactionCore t WHERE t.senderWalletId = :walletId AND t.status = 'PENDING'")
    long countPendingBySenderWallet(@Param("walletId") Long walletId);

    /* =========================
       TYPE-BASED QUERIES
       ========================= */

    /**
     * Find transactions by type
     */
    Page<TransactionCore> findByTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);

    /**
     * Find transactions by wallet and type
     */
    @Query("SELECT t FROM TransactionCore t WHERE (t.senderWalletId = :walletId OR t.receiverWalletId = :walletId) AND t.type = :type ORDER BY t.createdAt DESC")
    Page<TransactionCore> findByWalletIdAndType(@Param("walletId") Long walletId, @Param("type") TransactionType type, Pageable pageable);

    /* =========================
       DATE RANGE QUERIES
       ========================= */

    /**
     * Find transactions within date range
     */
    @Query("SELECT t FROM TransactionCore t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<TransactionCore> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Find wallet transactions within date range
     */
    @Query("SELECT t FROM TransactionCore t WHERE (t.senderWalletId = :walletId OR t.receiverWalletId = :walletId) AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<TransactionCore> findByWalletIdAndDateRange(@Param("walletId") Long walletId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /* =========================
       AMOUNT-BASED QUERIES
       ========================= */

    /**
     * Find transactions above a certain amount
     */
    @Query("SELECT t FROM TransactionCore t WHERE t.amount >= :minAmount ORDER BY t.amount DESC")
    Page<TransactionCore> findByMinAmount(@Param("minAmount") BigDecimal minAmount, Pageable pageable);

    /**
     * Find transactions within amount range
     */
    @Query("SELECT t FROM TransactionCore t WHERE t.amount BETWEEN :minAmount AND :maxAmount ORDER BY t.createdAt DESC")
    Page<TransactionCore> findByAmountRange(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount, Pageable pageable);

    /* =========================
       STATISTICS QUERIES
       ========================= */

    /**
     * Calculate total amount sent by wallet
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionCore t WHERE t.senderWalletId = :walletId AND t.status = 'COMPLETED'")
    BigDecimal sumCompletedAmountBySenderWallet(@Param("walletId") Long walletId);

    /**
     * Calculate total amount received by wallet
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionCore t WHERE t.receiverWalletId = :walletId AND t.status = 'COMPLETED'")
    BigDecimal sumCompletedAmountByReceiverWallet(@Param("walletId") Long walletId);

    /**
     * Count completed transactions for wallet
     */
    @Query("SELECT COUNT(t) FROM TransactionCore t WHERE (t.senderWalletId = :walletId OR t.receiverWalletId = :walletId) AND t.status = 'COMPLETED'")
    long countCompletedByWallet(@Param("walletId") Long walletId);

    /**
     * Count failed transactions for wallet
     */
    @Query("SELECT COUNT(t) FROM TransactionCore t WHERE t.senderWalletId = :walletId AND t.status = 'FAILED'")
    long countFailedBySenderWallet(@Param("walletId") Long walletId);
}
