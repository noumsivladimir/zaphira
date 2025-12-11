package com.zaphira.transaction.event;

import com.zaphira.common.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

/**
 * Service pour publier les événements de transaction sur Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private static final String TRANSACTION_CREATED_TOPIC = "transaction-created";

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

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
}
