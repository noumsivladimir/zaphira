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
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka pour transaction-service.
 * 
 * Configuré pour:
 * - Producer: TransactionCreatedEvent et TransactionValidationRequest
 * - Consumer: TransactionValidationResult
 * 
 * Garanties:
 * - Idempotence Producer activée
 * - Auto-offset reset: earliest
 * - Manual commit après traitement réussi
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

    // ==================== PRODUCER CONFIGURATION ====================

    /**
     * Producer Factory pour TransactionCreatedEvent (existant)
     */
    @Bean
    public ProducerFactory<String, TransactionCreatedEvent> transactionProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TransactionCreatedEvent> transactionKafkaTemplate() {
        return new KafkaTemplate<>(transactionProducerFactory());
    }

    /**
     * Producer Factory pour TransactionValidationRequest
     */
    @Bean
    public ProducerFactory<String, TransactionValidationRequest> validationRequestProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TransactionValidationRequest> validationRequestKafkaTemplate() {
        return new KafkaTemplate<>(validationRequestProducerFactory());
    }

    // ==================== CONSUMER CONFIGURATION ====================

    /**
     * Consumer Factory pour TransactionValidationResult
     */
    @Bean
    public ConsumerFactory<String, TransactionValidationResult> validationResultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionValidationResult.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, validationConsumerGroupId);  // From property
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, validationConsumerEnableAutoCommit);  // Manual commit
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, validationConsumerMaxPollRecords);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, validationConsumerSessionTimeoutMs);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(props);
    }
}
