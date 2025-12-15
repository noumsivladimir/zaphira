package com.zaphira.auth.config;

import com.zaphira.common.event.UserRegisteredEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka pour auth-service.
 * Configure le producer pour publier des événements UserRegisteredEvent.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:192.168.0.122:9092}")
    private String bootstrapServers;

    @Bean
    public KafkaTemplate<String, UserRegisteredEvent> userRegisteredKafkaTemplate() {
        return new KafkaTemplate<>(userRegisteredProducerFactory());
    }

    @Bean
    public ProducerFactory<String, UserRegisteredEvent> userRegisteredProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // Use the injected bootstrapServers
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
}

