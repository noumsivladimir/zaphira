package com.zaphira.wallet.controller.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test Synchronous Message Controller for wallet-service
 * 
 * Simulates Kafka message consumption via Feign for testing purposes.
 * This controller receives messages from other services via REST calls
 * instead of Kafka topics.
 * 
 * Activated only with "test-sync" profile.
 */
@RestController
@RequestMapping("/test-sync")
@ConditionalOnProfile("test-sync")
@RequiredArgsConstructor
@Slf4j
public class WalletServiceTestMessageController {

    private final ObjectMapper objectMapper;

    /**
     * Receive and process messages from other microservices.
     * 
     * @param topic The topic name (e.g., "user-created-topic")
     * @param payload The message payload
     * @return Response status
     */
    @PostMapping("/messages/{topic}")
    public ResponseEntity<?> handleMessage(
            @PathVariable("topic") String topic,
            @RequestBody Object payload) {

        log.info("📨 Test Message received - Topic: {}, Payload: {}", topic, payload);

        try {
            switch (topic) {
                case "user-created-topic":
                    handleUserCreatedEvent(payload);
                    break;
                case "transaction-initiated-topic":
                    handleTransactionInitiatedEvent(payload);
                    break;
                default:
                    log.warn("Unknown topic: {}", topic);
            }
            return ResponseEntity.ok().body("{\"status\":\"processed\", \"topic\":\"" + topic + "\"}");
        } catch (Exception e) {
            log.error("❌ Error processing message from topic: {}", topic, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Handle user created events from user-service
     */
    private void handleUserCreatedEvent(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        log.debug("Processing user-created event: {}", json);
        
        // Extract userId from event and create wallet
        // This is handled synchronously via Feign in test mode
    }

    /**
     * Handle transaction initiated events
     */
    private void handleTransactionInitiatedEvent(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        log.debug("Processing transaction-initiated event: {}", json);
        
        // TODO: Update wallet balance, process transaction
    }

    /**
     * Health check endpoint for tests
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("{\"status\":\"UP\", \"service\":\"wallet-service\"}");
    }
}
