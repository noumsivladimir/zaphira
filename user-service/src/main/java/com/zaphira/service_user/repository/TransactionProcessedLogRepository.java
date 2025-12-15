package com.zaphira.service_user.repository;

import com.zaphira.service_user.model.entities.TransactionProcessedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionProcessedLogRepository extends JpaRepository<TransactionProcessedLog, Long> {
    
    /**
     * Vérifie si une transaction a déjà été traitée pour un utilisateur donné.
     * Utilisé pour garantir l'idempotence.
     *
     * @param userId L'ID de l'utilisateur
     * @param transactionId L'ID de la transaction
     * @return Optional contenant le log si la transaction a déjà été traitée
     */
    Optional<TransactionProcessedLog> findByUserIdAndTransactionId(Long userId, Long transactionId);
}
