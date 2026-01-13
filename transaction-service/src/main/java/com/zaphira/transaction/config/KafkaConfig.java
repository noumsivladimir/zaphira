package com.zaphira.transaction.config;

import com.zaphira.common.event.TransactionCreatedEvent;
import com.zaphira.transaction.kafka.event.TransactionValidationRequest;
import com.zaphira.transaction.kafka.event.TransactionValidationResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka optimisée pour transaction-service.
 *
 * Supports:
 * - Producers typés : TransactionCreatedEvent, TransactionValidationRequest
 * - Consumer typé : TransactionValidationResult
 *
 * Garanties :
 * - Idempotence Producer activée
 * - Auto-offset reset : earliest
 * - Manual commit pour consumer
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:192.168.0.122:9092}")
    private String bootstrapServers;

    @Value("${kafka.validation.consumer.group-id:transaction-service-validation-result}")
    private String validationConsumerGroupId;

    @Value("${kafka.validation.consumer.max-poll-records:10}")
    private Integer validationConsumerMaxPollRecords;

    @Value("${kafka.validation.consumer.session-timeout-ms:30000}")
    private Integer validationConsumerSessionTimeoutMs;

    @Value("${kafka.validation.consumer.enable-auto-commit:false}")
    private Boolean validationConsumerEnableAutoCommit;

    // ==================== GENERIC PRODUCER FACTORY ====================

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return props;
    }

    @Bean
    public ProducerFactory<String, Object> genericProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    @Bean
    public KafkaTemplate<String, Object> genericKafkaTemplate() {
        return new KafkaTemplate<>(genericProducerFactory());
    }

    // ==================== TYPED PRODUCERS ====================

    @Bean
    public ProducerFactory<String, TransactionCreatedEvent> transactionCreatedEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    @Bean
    public KafkaTemplate<String, TransactionCreatedEvent> transactionCreatedEventKafkaTemplate() {
        return new KafkaTemplate<>(transactionCreatedEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, TransactionValidationRequest> transactionValidationRequestProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    @Bean
    public KafkaTemplate<String, TransactionValidationRequest> transactionValidationRequestKafkaTemplate() {
        return new KafkaTemplate<>(transactionValidationRequestProducerFactory());
    }

    // ==================== CONSUMER CONFIGURATION ====================

    @Bean
    public ConsumerFactory<String, TransactionValidationResult> transactionValidationResultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionValidationResult.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, validationConsumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, validationConsumerEnableAutoCommit);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, validationConsumerMaxPollRecords);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, validationConsumerSessionTimeoutMs);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(props);
    }
}
