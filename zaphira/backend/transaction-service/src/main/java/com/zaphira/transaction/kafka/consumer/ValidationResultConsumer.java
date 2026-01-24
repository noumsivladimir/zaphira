package com.zaphira.transaction.kafka.consumer;

import com.zaphira.transaction.kafka.event.TransactionValidationResult;
import com.zaphira.transaction.service.kafka.ValidationCoordinatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Consumer Kafka pour traiter les résultats de validation de transactions
 * 
 * Responsabilités:
 * - Consommer TransactionValidationResult du topic transaction.validation.result
 * - Mettre à jour l'état de la transaction basé sur le résultat
 * - Gérer l'idempotence via correlationId
 * - Logger et tracer tous les traitements
 * 
 * Garantie: Au moins une fois (idempotence garantie par DB + correlationId)
 * Offset: Auto-commit après succès du traitement
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationResultConsumer {

    private static final String TOPIC = "transaction.validation.result";
    private static final String GROUP_ID = "transaction-service-validation-result";

    private final ValidationCoordinatorService validationCoordinatorService;

    /**
     * Consomme un événement de résultat de validation
     * 
     * @param payload L'événement de résultat de validation
     * @param transactionId La clé du message (transactionId)
     */
    @KafkaListener(
        topics = TOPIC,
        groupId = GROUP_ID
    )
    public void handleValidationResult(
        @Payload TransactionValidationResult payload,
        @Header(KafkaHeaders.RECEIVED_KEY) String transactionId
    ) {
        try {
            log.info(
                "Received validation result for transaction: {}, correlationId: {}, " +
                "status: {}",
                transactionId,
                payload.getCorrelationId(),
                payload.getValidationStatus()
            );

            // Déléguer le traitement au coordinateur qui gère l'idempotence et la logique métier
            validationCoordinatorService.processValidationResult(payload);

            log.info(
                "Successfully processed validation result for transaction: {}, " +
                "correlationId: {}, newStatus: {}",
                transactionId,
                payload.getCorrelationId(),
                payload.getValidationStatus()
            );

        } catch (IllegalArgumentException e) {
            // Erreur métier (ex: transaction introuvable, état invalide)
            // Log comme warning car ce n'est pas une erreur Kafka
            log.warn(
                "Business error processing validation result for transaction: {}, " +
                "correlationId: {}, error: {}",
                transactionId,
                payload.getCorrelationId(),
                e.getMessage()
            );
            // Ne pas relancer -> offset avancé pour éviter retry infini

        } catch (Exception e) {
            // Erreur technique (DB, etc.)
            log.error(
                "Technical error processing validation result for transaction: {}, " +
                "correlationId: {}, error: {}",
                transactionId,
                payload.getCorrelationId(),
                e.getMessage(),
                e
            );
            // Relancer l'exception pour que Spring Kafka retry le message
            // Après max.poll.records retries, envoyer vers Dead Letter Topic
            throw new RuntimeException("Failed to process validation result", e);
        }
    }
}
