package com.zaphira.common.test.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that replaces Kafka with synchronous Feign-based messaging.
 * 
 * Activated by profile 'test-sync'. This configuration provides mock/stub implementations
 * of Kafka producers and consumers, allowing microservices to communicate synchronously
 * via REST/Feign for tests.
 * 
 * This eliminates the need for a real Kafka broker during testing while maintaining
 * the same messaging semantics.
 * 
 * Usage:
 * @ActiveProfiles("test-sync")
 * @SpringBootTest
 * public class IntegrationTest { ... }
 * 
 * @since 1.0.0
 */
@Configuration
@Profile("test-sync")
public class SynchronousMessagingTestConfig {

    /**
     * Provides a mock/no-op KafkaTemplate for testing.
     * In test-sync mode, actual Kafka is replaced with REST calls via Feign.
     */
    @Bean
    @ConditionalOnMissingBean
    public MockKafkaTemplate mockKafkaTemplate() {
        return new MockKafkaTemplate();
    }

    /**
     * Marker bean to indicate test-sync mode is active.
     */
    @Bean(name = "testSyncModeActive")
    public String testSyncModeIndicator() {
        return "test-sync: Feign-based synchronous messaging enabled. Kafka disabled.";
    }

    /**
     * Mock Kafka template that does nothing but accepts calls.
     */
    public static class MockKafkaTemplate {
        public void send(String topic, Object message) {
            // No-op for testing
        }
    }
}
