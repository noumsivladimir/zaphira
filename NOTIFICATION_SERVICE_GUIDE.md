# Notification Service - Implementation Guide

## Overview

The Notification Service is a **transverse, event-driven, fully decoupled** microservice responsible for:

✅ **Centralizing** all notification-related events  
✅ **Ordering** events via Kafka with partition by userId  
✅ **Determining** notification recipients  
✅ **Generating** notification content using templates  
✅ **Persisting** notification history for audit and retrieval  
✅ **Dispatching** notifications via configured channels  

**No business logic** resides in this service.

---

## Architecture

### Component Pipeline

```
Kafka Consumer (3 topics)
         ↓
Event Validator
         ↓
Notification Rule Engine (YAML-driven)
         ↓
Template Resolver (dynamic content)
         ↓
Notification Persistence (database)
         ↓
Notification Dispatcher (multi-channel)
         ↓
User API (REST endpoints)
```

### Event Flow

1. **Source Service** publishes `NotificationEvent` to Kafka topic
2. **Kafka** orders events by `userId` (partition key)
3. **Consumer** receives event with idempotency check
4. **Rule Engine** determines if notification is enabled
5. **Template Engine** generates title/message with variables
6. **Persistence** saves notification to database
7. **Dispatcher** sends to configured channels
8. **Users** retrieve via REST API

---

## Kafka Topics & Configuration

### Topics

| Topic | Source | Events |
|-------|--------|--------|
| `notification.transaction.events` | Transaction Service | TransactionCreated, TransactionCompleted, TransactionFailed, TransactionRefunded, etc. |
| `notification.wallet.events` | Wallet Service | WalletCreated, WalletBalanceUpdated, WalletLowBalance, WalletFrozen, etc. |
| `notification.user.events` | User Service | UserAccountCreated, UserProfileUpdated, UserPasswordChanged, UserAccountSuspended, etc. |

### Kafka Rules

- **Partition Key**: `userId` → Guarantees per-user ordering
- **Consumer Group**: `notification-service-{type}-group`
- **Offset Reset**: `earliest` (replay capability)
- **Acknowledgment**: Manual (after successful processing)
- **Retry**: Spring Kafka retry configuration with exponential backoff
- **DLQ**: Automatic dead-letter queue for persistent failures

---

## Event Contract (Standard Format)

All microservices **MUST** publish using this format:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "TransactionCompleted",
  "sourceService": "transaction-service",
  "occurredAt": "2025-12-19T14:30:00Z",
  "userId": "user-123",
  "aggregateId": "transaction-456",
  "payload": {
    "amount": 100.00,
    "currency": "USD",
    "recipient": "john@example.com",
    "status": "SUCCESS"
  }
}
```

**Fields**:
- `eventId` (UUID): Unique identifier for idempotence
- `eventType`: Event classification for rule matching
- `sourceService`: Origin service name
- `occurredAt`: ISO-8601 timestamp
- `userId`: Recipient user ID
- `aggregateId`: Resource identifier (walletId, transactionId, etc.)
- `payload`: Event-specific dynamic data

---

## Notification Rules (YAML Configuration)

Rules are **data-driven**, not hardcoded:

```yaml
notification:
  rules:
    TransactionCompleted:
      enabled: true
      channels: [IN_APP, EMAIL]
      template: TRANSACTION_COMPLETED
      priority: NORMAL
      maxRetries: 3
      retryDelayMs: 5000
      requiresPreference: true

    TransactionFailed:
      enabled: true
      channels: [IN_APP, PUSH, EMAIL]
      template: TRANSACTION_FAILED
      priority: HIGH
      maxRetries: 5
      retryDelayMs: 10000
      requiresPreference: false

    WalletLowBalance:
      enabled: true
      channels: [IN_APP, EMAIL]
      template: WALLET_LOW_BALANCE
      priority: NORMAL
      requiresPreference: true
```

**Rule Properties**:
- `enabled`: Enable/disable notifications
- `channels`: Delivery methods (IN_APP, EMAIL, SMS, PUSH)
- `template`: Template name for content generation
- `priority`: LOW, NORMAL, HIGH, CRITICAL
- `maxRetries`: Maximum retry attempts
- `retryDelayMs`: Delay between retries (ms)
- `requiresPreference`: Check user preferences before sending

---

## Database Schema

### notifications
Stores all user notifications

```sql
id          UUID PRIMARY KEY
user_id     VARCHAR(50)          -- Recipient
event_type  VARCHAR(100)         -- Event classification
source_service VARCHAR(50)       -- Origin service
aggregate_id VARCHAR(100)        -- Resource ID
title       VARCHAR(255)         -- Notification title
message     TEXT                 -- Notification content
status      ENUM(PENDING|SENT|FAILED|READ|ARCHIVED)
priority    ENUM(LOW|NORMAL|HIGH|CRITICAL)
channel     ENUM(IN_APP|EMAIL|SMS|PUSH)
created_at  TIMESTAMP            -- Creation time
sent_at     TIMESTAMP            -- Send time
read_at     TIMESTAMP            -- Read time
metadata    JSONB                -- Event payload
```

**Indexes**: user_id, event_type, status, created_at

### notification_templates
Message templates for events

```sql
id                   UUID PRIMARY KEY
event_type           VARCHAR(100) UNIQUE
title_template       VARCHAR(255)       -- ${variable} placeholders
message_template     TEXT               -- ${variable} placeholders
description          TEXT
is_active            BOOLEAN
```

**Example Template**:
```
Title: "Transaction Successful"
Message: "Your transaction of ${amount} ${currency} to ${recipient} has been completed."
```

Variables are extracted from event payload and injected.

### notification_preferences
User notification customization

```sql
id              UUID PRIMARY KEY
user_id         VARCHAR(50)
event_type      VARCHAR(100)
is_enabled      BOOLEAN
channels        TEXT (JSON array)  -- ["IN_APP", "EMAIL"]
quiet_start     TIME               -- HH:mm format
quiet_end       TIME               -- HH:mm format
```

### notification_logs
Audit trail for dispatch attempts

```sql
id              UUID PRIMARY KEY
notification_id UUID FOREIGN KEY
user_id         VARCHAR(50)
channel         VARCHAR(20)
status          ENUM(PENDING|SENT|FAILED|READ|ARCHIVED)
attempt_number  INTEGER            -- Retry count
error_message   TEXT               -- Failure reason
created_at      TIMESTAMP
```

---

## REST API Endpoints

### Get User Notifications

```
GET /api/notifications/user/{userId}?page=0&size=20

Response:
{
  "content": [
    {
      "id": "uuid",
      "userId": "user-123",
      "eventType": "TransactionCompleted",
      "title": "Transaction Successful",
      "message": "Your transaction...",
      "status": "PENDING",
      "priority": "NORMAL",
      "channel": "IN_APP",
      "createdAt": "2025-12-19T14:30:00Z",
      "sentAt": "2025-12-19T14:30:01Z",
      "readAt": null
    }
  ],
  "totalPages": 1,
  "totalElements": 20,
  "currentPage": 0
}
```

### Get Unread Notifications

```
GET /api/notifications/user/{userId}/unread?page=0&size=20

Returns only PENDING status notifications
```

### Get Unread Count

```
GET /api/notifications/user/{userId}/unread-count

Response:
42
```

### Mark Notification as Read

```
PUT /api/notifications/{notificationId}/read

Response:
{
  "id": "uuid",
  "status": "READ",
  "readAt": "2025-12-19T14:35:00Z",
  ...
}
```

### Mark All as Read

```
PUT /api/notifications/user/{userId}/mark-all-read

Response:
"All notifications marked as read"
```

### Archive Notification

```
DELETE /api/notifications/{notificationId}

Response:
"Notification archived"
```

---

## Using EventPublisher in Other Services

### 1. Inject EventPublisher in Service

```java
@Service
@RequiredArgsConstructor
public class TransactionService {
    
    private final EventPublisher eventPublisher;
    
    public void completeTransaction(Transaction transaction) {
        // ... business logic ...
        
        // Publish notification event
        eventPublisher.publishNotificationEvent(
            "TransactionCompleted",           // eventType
            "transaction-service",            // sourceService
            transaction.getUserId(),          // userId
            transaction.getId().toString(),   // aggregateId
            Map.of(
                "amount", transaction.getAmount(),
                "currency", transaction.getCurrency(),
                "recipient", transaction.getRecipientEmail(),
                "status", transaction.getStatus()
            )
        );
    }
}
```

### 2. Update application.yml with Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    topic:
      transaction-events: notification.transaction.events
      wallet-events: notification.wallet.events
      user-events: notification.user.events
```

---

## Notification Channels

### IN_APP (Implemented)
✅ Store in database  
✅ Retrieve via REST API  
✅ Mark as read  

### EMAIL (Framework Ready)
🔄 Requires email service implementation  
🔄 Use NotificationDispatchService extension  

### SMS (Framework Ready)
🔄 Requires SMS gateway integration  
🔄 Add SMS provider configuration  

### PUSH (Framework Ready)
🔄 Requires push notification service  
🔄 Firebase/OneSignal integration  

---

## Supported Event Types

### Transaction Events (8)
- TransactionCreated
- TransactionValidated
- TransactionAuthorized
- TransactionCompleted
- TransactionFailed
- TransactionCancelled
- TransactionRefunded
- SuspiciousTransactionDetected
- LargeTransactionDetected

### User Events (7)
- UserAccountCreated
- UserProfileUpdated
- UserPasswordChanged
- UserEmailChanged
- UserPhoneChanged
- UserAccountSuspended
- UserAccountReactivated

### Wallet Events (6)
- WalletCreated
- WalletBalanceUpdated
- WalletLowBalance
- WalletFrozen
- WalletUnfrozen
- WalletLimitsUpdated

---

## Configuration Properties

### Kafka
```yaml
spring.kafka.bootstrap-servers: localhost:9092
spring.kafka.consumer.group-id: notification-service
spring.kafka.consumer.auto-offset-reset: earliest
spring.kafka.consumer.enable-auto-commit: false
```

### Database
```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/notification_db
spring.datasource.username: postgres
spring.datasource.password: password
spring.jpa.hibernate.ddl-auto: validate
spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Flyway
```yaml
spring.flyway.enabled: true
spring.flyway.baseline-on-migrate: true
spring.flyway.locations: classpath:db/migration
```

---

## Error Handling & Retry

### Retry Strategy
1. Event consumed and processed
2. If fails: logged and retried up to `maxRetries`
3. Exponential backoff: `delay * 2^attempt`
4. After max retries: moved to DLQ

### DLQ (Dead Letter Queue)
- Topics: `notification.*.events.dlt`
- Manual intervention required
- Reprocess via admin API when fixed

### Idempotency
- `eventId` ensures no duplicate notifications
- `aggregateId` + `eventType` + `userId` prevents duplicates
- Safe to replay events from DLQ

---

## Monitoring & Observability

### Logs
- Consumer: Logs all received events
- Validator: Logs validation failures
- Rule Engine: Logs rule decisions
- Dispatch: Logs send success/failure
- Audit: All operations logged

### Metrics (via Spring Boot Actuator)
- `/actuator/metrics/kafka.consumer.records.consumed`
- `/actuator/metrics/kafka.consumer.lag`
- `/actuator/health` - includes Kafka, DB status

### Database Views
```sql
-- User notification statistics
SELECT * FROM user_notification_stats;

-- Unread count by user
SELECT user_id, unread_count FROM user_notification_stats;
```

---

## Running the Service

### Prerequisites
- Java 17+
- PostgreSQL 13+
- Kafka 2.8+

### Steps

1. **Create database**
   ```sql
   CREATE DATABASE notification_db;
   ```

2. **Update application.yml**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/notification_db
       username: postgres
       password: your-password
     kafka:
       bootstrap-servers: localhost:9092
   ```

3. **Run application**
   ```bash
   mvn spring-boot:run -pl notification-service
   ```

4. **Verify startup**
   ```bash
   curl http://localhost:8007/actuator/health
   ```

---

## Next Steps

1. ✅ Implement email channel with SMTP
2. ✅ Add SMS channel integration
3. ✅ Implement push notifications
4. ✅ Add notification preferences API
5. ✅ Build admin dashboard for rule management
6. ✅ Add observability (Prometheus metrics)
7. ✅ Implement notification scheduling
8. ✅ Add rate limiting per user/event

---

## Support

For issues or questions:
1. Check logs in `/logs/notification-service.log`
2. Query database views for statistics
3. Check Kafka topics for message backlog
4. Review DLQ for failed events
