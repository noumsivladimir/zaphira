package com.zaphira.transaction.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

@Service
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(@NonNull KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(@NonNull String topic, @NonNull Object event) {
        kafkaTemplate.send(topic, event);
    }
}