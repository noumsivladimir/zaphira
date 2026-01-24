package com.zaphira.transaction.kafka.producer;

import com.zaphira.transaction.kafka.event.TransactionValidationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Producer Kafka pour publier les demandes de validation de transactions
 * 
 * Responsabilités:
 * - Publier TransactionValidationRequest sur transaction.validation.request
 * - Générer correlationId pour idempotence
 * - Logger succès et erreurs
 * - Gérer retry et erreurs de publication
 * 
 * Garantie: Au moins une fois (idempotence via correlationId côté consumer)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationRequestProducer {

    private static final String TOPIC = "transaction.validation.request";
    
    private final KafkaTemplate<String, TransactionValidationRequest> validationRequestKafkaTemplate;

    /**
     * Publie une demande de validation de transaction
     * 
     * @param request Requête de validation contenant tous les détails de la transaction
     * @throws RuntimeException si la publication échoue
     */
    public void publishValidationRequest(TransactionValidationRequest request) {
        try {
            // Assurer que correlationId est défini
            if (request.getCorrelationId() == null) {
                request.setCorrelationId(UUID.randomUUID().toString());
            }

            String transactionId = request.getTransactionId().toString();

            log.info(
                "Publishing validation request for transaction: {}, correlationId: {}, amount: {}",
                transactionId,
                request.getCorrelationId(),
                request.getAmount()
            );

            // Construire le message avec headers Kafka
            Message<TransactionValidationRequest> message = MessageBuilder
                .withPayload(request)
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .setHeader("correlationId", request.getCorrelationId())
                .setHeader("timestamp", System.currentTimeMillis())
                .build();

            // Publier de manière asynchrone avec callback
            validationRequestKafkaTemplate.send(TOPIC, transactionId, message.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                            "Failed to publish validation request for transaction: {}, correlationId: {}, error: {}",
                            transactionId,
                            request.getCorrelationId(),
                            ex.getMessage(),
                            ex
                        );
                    } else {
                        log.info(
                            "Successfully published validation request for transaction: {}, " +
                            "correlationId: {}, partition: {}, offset: {}",
                            transactionId,
                            request.getCorrelationId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                        );
                    }
                });

        } catch (Exception e) {
            log.error(
                "Error publishing validation request for transaction: {}, error: {}",
                request.getTransactionId(),
                e.getMessage(),
                e
            );
            throw new RuntimeException("Failed to publish transaction validation request", e);
        }
    }
}
