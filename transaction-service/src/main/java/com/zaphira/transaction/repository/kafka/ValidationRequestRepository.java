package com.zaphira.transaction.repository.kafka;

import com.zaphira.transaction.model.kafka.ValidationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour gérer les entités ValidationRequest
 * Utilisé pour garantir l'idempotence des messages Kafka
 */
@Repository
public interface ValidationRequestRepository extends JpaRepository<ValidationRequest, Long> {

    /**
     * Cherche une requête de validation par correlationId
     */
    Optional<ValidationRequest> findByCorrelationId(String correlationId);

    /**
     * Vérifie si une requête avec ce correlationId existe déjà
     */
    boolean existsByCorrelationId(String correlationId);

    /**
     * Cherche toutes les requêtes pour une transaction
     */
    List<ValidationRequest> findByTransactionId(Long transactionId);

    /**
     * Cherche les requêtes non expirées et non traitées
     */
    @Query("SELECT vr FROM ValidationRequest vr WHERE vr.status = 'PENDING' AND vr.expiresAt > :now")
    List<ValidationRequest> findPendingValidations(@Param("now") LocalDateTime now);

    /**
     * Marque une requête comme traitée
     */
    @Transactional
    @Modifying
    @Query("UPDATE ValidationRequest vr SET vr.status = 'PROCESSED', vr.processedAt = CURRENT_TIMESTAMP WHERE vr.correlationId = :correlationId")
    void markAsProcessed(
        @Param("correlationId") String correlationId
    );

    /**
     * Marque les requêtes expirées
     */
    @Transactional
    @Modifying
    @Query("UPDATE ValidationRequest vr SET vr.status = 'EXPIRED' WHERE vr.status = 'PENDING' AND vr.expiresAt <= :now")
    int markExpiredValidations(@Param("now") LocalDateTime now);

    /**
     * Supprime les requêtes anciennes (plus de 30 jours)
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM ValidationRequest vr WHERE vr.requestedAt < :cutoffDate")
    int deleteOldValidations(@Param("cutoffDate") LocalDateTime cutoffDate);
}
