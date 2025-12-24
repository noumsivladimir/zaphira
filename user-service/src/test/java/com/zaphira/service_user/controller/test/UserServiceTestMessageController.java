package com.zaphira.service_user.controller.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.common.model.events.UserCreatedEvent;
import com.zaphira.service_user.dto.request.CreateWalletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test Synchronous Message Controller for user-service
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
public class UserServiceTestMessageController {

    private final ObjectMapper objectMapper;

    /**
     * Receive and process messages from other microservices.
     * 
     * @param topic The topic name (e.g., "wallet-created-topic")
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
                case "wallet-created-topic":
                    handleWalletCreatedEvent(payload);
                    break;
                case "transaction-confirmed-topic":
                    handleTransactionConfirmedEvent(payload);
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
     * Handle wallet created events from wallet-service
     */
    private void handleWalletCreatedEvent(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        log.debug("Processing wallet-created event: {}", json);
        
        // TODO: Update user record with wallet information
        // This would typically update the user entity with walletId
    }

    /**
     * Handle transaction confirmed events
     */
    private void handleTransactionConfirmedEvent(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        log.debug("Processing transaction-confirmed event: {}", json);
        
        // TODO: Update transaction status in user's transaction history
    }

    /**
     * Health check endpoint for tests
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("{\"status\":\"UP\", \"service\":\"user-service\"}");
    }
}
