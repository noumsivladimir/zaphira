package com.zaphira.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Standard NotificationEvent contract - used by all microservices
 * Ensures traceability, idempotence, audit trail, and Kafka replay capability
 * 
 * All services should publish this format to notification topics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    
    private String eventId;           // UUID - unique event identifier for idempotence
    private String eventType;         // Event type: TransactionCompleted, WalletBalanceUpdated, etc.
    private String sourceService;     // Source: wallet-service, transaction-service, user-service
    private LocalDateTime occurredAt; // When the event occurred (ISO-8601)
    private String userId;            // The user who should be notified
    private String aggregateId;       // Resource ID: walletId, transactionId, userId, etc.
    private JsonNode payload;         // Event-specific data as JSON
    
    /**
     * Initialize event ID if not already set
     */
    public void setEventIdIfNull() {
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID().toString();
        }
    }
    
    /**
     * Validate required fields
     */
    public boolean isValid() {
        return eventId != null && eventType != null && sourceService != null && 
               occurredAt != null && userId != null && aggregateId != null;
    }
}
