package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.TransactionAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour l'entité TransactionAuditLog.
 * Permet de récupérer et de filtrer les logs d'audit.
 */
@Repository
public interface TransactionAuditLogRepository extends JpaRepository<TransactionAuditLog, Long> {
    
    /**
     * Récupère tous les logs pour une transaction donnée
     */
    List<TransactionAuditLog> findByTransactionIdOrderByTimestampDesc(Long transactionId);
    
    /**
     * Récupère les logs paginés pour une transaction
     */
    Page<TransactionAuditLog> findByTransactionId(Long transactionId, Pageable pageable);
    
    /**
     * Récupère les logs pour un utilisateur acteur
     */
    Page<TransactionAuditLog> findByActorUserIdOrderByTimestampDesc(Long actorUserId, Pageable pageable);
    
    /**
     * Récupère les logs par type d'action
     */
    Page<TransactionAuditLog> findByActionTypeOrderByTimestampDesc(String actionType, Pageable pageable);
    
    /**
     * Récupère les logs avec filtrage complexe
     */
    @Query("SELECT a FROM TransactionAuditLog a WHERE " +
           "(:transactionId IS NULL OR a.transactionId = :transactionId) AND " +
           "(:actorUserId IS NULL OR a.actorUserId = :actorUserId) AND " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:result IS NULL OR a.result = :result) AND " +
           "(:from IS NULL OR a.timestamp >= :from) AND " +
           "(:to IS NULL OR a.timestamp <= :to) " +
           "ORDER BY a.timestamp DESC")
    Page<TransactionAuditLog> searchAuditLogs(
        @Param("transactionId") Long transactionId,
        @Param("actorUserId") Long actorUserId,
        @Param("actionType") String actionType,
        @Param("result") String result,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
    
    /**
     * Récupère les logs échoués
     */
    List<TransactionAuditLog> findByResultAndTimestampAfter(String result, LocalDateTime timestamp);
    
    /**
     * Compte les opérations par acteur et par action
     */
    Long countByActorUserIdAndActionType(Long actorUserId, String actionType);
}
