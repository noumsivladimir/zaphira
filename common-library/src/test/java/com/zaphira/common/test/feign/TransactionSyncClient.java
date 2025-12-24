package com.zaphira.common.test.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for synchronous test messaging between microservices.
 * 
 * Replaces Kafka for the 'test-sync' profile. Allows microservices to communicate
 * synchronously via REST/Feign instead of asynchronously via Kafka.
 * 
 * Each microservice implements a POST endpoint at /test-sync/messages/{topic}
 * that processes messages synchronously.
 * 
 * Usage:
 * @Autowired
 * private TransactionSyncClient txClient;
 * 
 * txClient.sendMessage("transaction-validation-topic", myEvent);
 */
@Profile("test-sync")
@FeignClient(
    name = "transaction-service-sync-test",
    url = "${test.sync.transaction-service.url:http://localhost:8081}",
    fallback = TransactionSyncClient.Fallback.class
)
public interface TransactionSyncClient {
    
    /**
     * Send a message to a specific topic synchronously.
     * @param topic the Kafka topic name (e.g., "validation-requests")
     * @param payload the message payload
     */
    @PostMapping("/test-sync/messages/{topic}")
    void sendMessage(
        @PathVariable(name = "topic") String topic,
        @RequestBody Object payload
    );

    /**
     * Fallback implementation for when the service is unavailable.
     */
    class Fallback implements TransactionSyncClient {
        @Override
        public void sendMessage(String topic, Object payload) {
            throw new RuntimeException(
                "transaction-service-sync-test is not available. " +
                "Ensure the service is running with @ActiveProfiles(\"test-sync\") " +
                "and test.sync.transaction-service.url is configured."
            );
        }
    }
}
