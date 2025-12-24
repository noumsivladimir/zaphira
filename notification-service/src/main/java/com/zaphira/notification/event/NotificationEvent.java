package com.zaphira.notification.event;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Standard event contract for all microservices publishing notifications
 * Ensures traceability, idempotence, audit trail, and Kafka replay capability
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    
    private String eventId;                    // UUID - unique event identifier
    private String eventType;                  // WalletBalanceUpdated, TransactionFailed, etc.
    private String sourceService;              // wallet-service, transaction-service, user-service
    private LocalDateTime occurredAt;          // ISO-8601 timestamp
    private String userId;                     // Recipient user ID
    private String aggregateId;                // walletId | transactionId | userId
    private JsonNode payload;                  // Dynamic payload per event type
    
    /**
     * Initialize with UUID if not provided
     */
    public void setEventIdIfNull() {
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID().toString();
        }
    }
    
    /**
     * Check if event has all required fields
     */
    public boolean isValid() {
        return eventId != null && eventType != null && sourceService != null && 
               occurredAt != null && userId != null && aggregateId != null;
    }
}
