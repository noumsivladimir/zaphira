package com.zaphira.transaction.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EventPublisherTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    @Test
    public void testPublish() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
            UUID.randomUUID().toString(),
            "txn123",
            "wallet1",
            "wallet2",
            BigDecimal.valueOf(100.00),
            "USD",
            "ref123",
            LocalDateTime.now()
        );

        eventPublisher.publish("transaction.events", event);
        verify(kafkaTemplate, times(1)).send("transaction.events", event);
    }
}