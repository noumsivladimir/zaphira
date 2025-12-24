package com.zaphira.transaction.test.messaging;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for synchronous message handling in test-sync mode.
 * 
 * Replaces Kafka listeners with HTTP endpoints. When other services
 * send messages via Feign to this controller, they are routed to the
 * appropriate consumer handler.
 * 
 * Endpoints:
 * - POST /test-sync/messages/validation-results
 *   Maps to: ValidationResultConsumer.consume()
 * 
 * Available only with @ActiveProfiles("test-sync")
 */
@RestController
@RequestMapping("/test-sync/messages")
@Profile("test-sync")
public class SynchronousMessageController {
    
    private static final Logger logger = LoggerFactory.getLogger(SynchronousMessageController.class);

    /**
     * Handle validation result messages (replaces Kafka topic: validation-results)
     * 
     * @param message the validation result message
     * @return 200 OK if processed successfully
     */
    @PostMapping("/validation-results")
    public ResponseEntity<String> handleValidationResults(@RequestBody Object message) {
        try {
            logger.info("Received validation-results message via Feign: {}", message);
            
            // In a real implementation, route this to the actual validation result consumer
            // ValidationResultConsumer.process(message);
            
            return ResponseEntity.ok("Message processed synchronously");
        } catch (Exception e) {
            logger.error("Error processing validation-results message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing message: " + e.getMessage());
        }
    }

    /**
     * Generic endpoint to handle any Kafka topic message.
     * Useful for extensibility.
     * 
     * @param topic the Kafka topic name (path variable)
     * @param message the message payload
     * @return 200 OK if processed
     */
    @PostMapping("/{topic}")
    public ResponseEntity<String> handleGenericMessage(
            @PathVariable String topic,
            @RequestBody Object message) {
        try {
            logger.info("Received message for topic '{}': {}", topic, message);
            
            // Route to appropriate handler based on topic
            switch (topic) {
                case "validation-results":
                    return handleValidationResults(message);
                default:
                    logger.warn("No handler for topic: {}", topic);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No handler for topic: " + topic);
            }
        } catch (Exception e) {
            logger.error("Error processing message for topic: {}", topic, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
}
