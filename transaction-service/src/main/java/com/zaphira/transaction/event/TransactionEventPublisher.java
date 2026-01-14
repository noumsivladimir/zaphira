package com.zaphira.transaction.event;

import com.zaphira.common.event.TransactionCompletedEvent;
import com.zaphira.common.event.TransactionCreatedEvent;
import com.zaphira.transaction.model.entities.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Service pour publier les événements de transaction sur Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_CREATED_TOPIC = "transaction-created";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction-completed";
    private static final String TRANSACTION_FAILED_TOPIC = "transaction-failed";
    private static final String TRANSACTION_CANCELLED_TOPIC = "transaction-cancelled";

    public void publishTransactionCreated(Transaction transaction) {
        log.info("Publishing TRANSACTION_CREATED event for: {}", transaction.getReference());

        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(transaction.getId())
                .reference(transaction.getReference())
                .senderWalletNumber(transaction.getSenderWalletNumber())
                .receiverWalletNumber(transaction.getReceiverWalletNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency().name())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();

        kafkaTemplate.send(TRANSACTION_CREATED_TOPIC,
                transaction.getReference(), event);
    }

    public void publishTransactionCompleted(Transaction transaction) {
        log.info("Publishing TRANSACTION_COMPLETED event for: {}", transaction.getReference());

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .reference(transaction.getReference())
                .senderWalletNumber(transaction.getSenderWalletNumber())
                .receiverWalletNumber(transaction.getReceiverWalletNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency().name())
                .status("COMPLETED")
                .completedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC,
                transaction.getReference(), event);
    }

    public void publishTransactionFailed(Transaction transaction) {
        log.info("Publishing TRANSACTION_FAILED event for: {}", transaction.getReference());

        TransactionEvent event = buildEvent(transaction, "FAILED");
        event.setFailureReason(transaction.getFailureReason());

        kafkaTemplate.send(TRANSACTION_FAILED_TOPIC,
                transaction.getReference(), event);
    }

    public void publishTransactionCancelled(Transaction transaction) {
        log.info("Publishing TRANSACTION_CANCELLED event for: {}", transaction.getReference());

        TransactionEvent event = buildEvent(transaction, "CANCELLED");

        kafkaTemplate.send(TRANSACTION_CANCELLED_TOPIC,
                transaction.getReference(), event);
    }

    private TransactionEvent buildEvent(Transaction transaction, String eventType) {
        return TransactionEvent.builder()
                .eventType(eventType)
                .transactionReference(transaction.getReference())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .senderWalletId(transaction.getSenderWalletId())
                .receiverWalletId(transaction.getReceiverWalletId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .feeAmount(transaction.getFeeAmount())
                .totalAmount(transaction.getAmount())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
