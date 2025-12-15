package com.zaphira.transaction.event;

import com.zaphira.common.event.TransactionCreatedEvent;
import com.zaphira.common.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service pour publier les événements de transaction sur Kafka.
 * Publie deux types d'événements:
 * - TransactionCreatedEvent: quand une transaction est créée
 * - TransactionCompletedEvent: quand une transaction est complétée
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private static final String TRANSACTION_CREATED_TOPIC = "transaction-created";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction-completed";

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;
    private final KafkaTemplate<String, TransactionCompletedEvent> completedTransactionKafkaTemplate;

    /**
     * Publie un événement TransactionCreatedEvent sur Kafka.
     *
     * @param event L'événement à publier
     */
    public void publishTransactionCreated(TransactionCreatedEvent event) {
        try {
            log.info("Publishing TransactionCreatedEvent for transaction {}", event.getTransactionId());
            
            kafkaTemplate.send(TRANSACTION_CREATED_TOPIC, event.getTransactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TransactionCreatedEvent for transaction {}: {}",
                                event.getTransactionId(), ex.getMessage(), ex);
                    } else {
                        log.info("Successfully published TransactionCreatedEvent for transaction {} to offset {}",
                                event.getTransactionId(),
                                result.getRecordMetadata().offset());
                    }
                });
            
        } catch (Exception e) {
            log.error("Error publishing TransactionCreatedEvent for transaction {}: {}",
                    event.getTransactionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish TransactionCreatedEvent", e);
        }
    }

    /**
     * Publie un événement TransactionCompletedEvent sur Kafka.
     * Cet événement est consommé par user-service pour mettre à jour le solde utilisateur.
     *
     * @param event L'événement à publier contenant les détails de la transaction complétée
     */
    public void publishTransactionCompleted(TransactionCompletedEvent event) {
        try {
            log.info("Publishing TransactionCompletedEvent for transaction {} with status {}",
                    event.getTransactionId(), event.getStatus());
            
            completedTransactionKafkaTemplate.send(
                    TRANSACTION_COMPLETED_TOPIC,
                    event.getTransactionId().toString(),
                    event
                ).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TransactionCompletedEvent for transaction {}: {}",
                                event.getTransactionId(), ex.getMessage(), ex);
                    } else {
                        log.info("Successfully published TransactionCompletedEvent for transaction {} to offset {}",
                                event.getTransactionId(),
                                result.getRecordMetadata().offset());
                    }
                });
            
        } catch (Exception e) {
            log.error("Error publishing TransactionCompletedEvent for transaction {}: {}",
                    event.getTransactionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish TransactionCompletedEvent", e);
        }
    }
}
