# Integration Checklist - Notification Service

## ✅ Service Implementation Complete

All 8 components of the Notification Service have been fully implemented and are ready for integration with the existing microservices.

---

## Phase 1: Database Setup

- [ ] Create PostgreSQL database: `notification_db`
- [ ] Update `application.yml` with correct database credentials
- [ ] Run Flyway migrations (automatic via Spring Boot)
- [ ] Verify tables created:
  ```sql
  SELECT table_name FROM information_schema.tables 
  WHERE table_schema = 'public' 
  AND table_name LIKE 'notification%';
  ```

---

## Phase 2: Kafka Setup

- [ ] Verify Kafka is running on `localhost:9092` (or configured address)
- [ ] Create Kafka topics (auto-created if not present):
  - `notification.transaction.events`
  - `notification.wallet.events`
  - `notification.user.events`
  - `notification.transaction.events.dlt` (DLQ)
  - `notification.wallet.events.dlt` (DLQ)
  - `notification.user.events.dlt` (DLQ)
- [ ] Verify topics:
  ```bash
  kafka-topics --list --bootstrap-server localhost:9092
  ```

---

## Phase 3: Notification Service Startup

- [ ] Update `notification-service/src/main/resources/application.yml`:
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/notification_db
      username: your_user
      password: your_password
    kafka:
      bootstrap-servers: localhost:9092
  ```

- [ ] Run Notification Service:
  ```bash
  mvn spring-boot:run -pl notification-service
  ```

- [ ] Verify startup:
  ```bash
  curl http://localhost:8007/actuator/health
  ```
  Expected response: `{"status":"UP"}`

- [ ] Check logs for consumer initialization:
  ```
  INFO ... TransactionEventConsumer : Consuming transaction events
  INFO ... WalletEventConsumer : Consuming wallet events
  INFO ... UserEventConsumer : Consuming user events
  ```

---

## Phase 4: Transaction Service Integration

- [ ] In `transaction-service/pom.xml`, verify common-library dependency exists
- [ ] In `TransactionService.java`, add field:
  ```java
  @RequiredArgsConstructor
  public class TransactionService {
      private final EventPublisher eventPublisher;
  ```

- [ ] Add event publishing to key methods:
  ```java
  public void completeTransaction(Transaction transaction) {
      // ... existing business logic ...
      
      // Publish notification event
      eventPublisher.publishNotificationEvent(
          "TransactionCompleted",
          "transaction-service",
          transaction.getUserId(),
          transaction.getId().toString(),
          Map.of(
              "amount", transaction.getAmount(),
              "currency", transaction.getCurrency(),
              "recipient", transaction.getRecipientEmail()
          )
      );
  }
  
  public void failTransaction(Transaction transaction, String reason) {
      // ... existing business logic ...
      
      eventPublisher.publishNotificationEvent(
          "TransactionFailed",
          "transaction-service",
          transaction.getUserId(),
          transaction.getId().toString(),
          Map.of("amount", transaction.getAmount(), "reason", reason)
      );
  }
  ```

- [ ] Add event publishing for other event types:
  - TransactionValidated
  - TransactionAuthorized
  - TransactionRefunded
  - TransactionCancelled
  - SuspiciousTransactionDetected
  - LargeTransactionDetected

- [ ] Test: Publish transaction event via `curl`:
  ```bash
  curl -X POST http://localhost:8007/api/notifications/test \
    -H "Content-Type: application/json" \
    -d '{
      "eventType":"TransactionCompleted",
      "sourceService":"transaction-service",
      "userId":"user-123",
      "aggregateId":"txn-456",
      "payload":{"amount":100,"currency":"USD"}
    }'
  ```

---

## Phase 5: Wallet Service Integration

- [ ] In `WalletService.java`, add `EventPublisher` field
- [ ] Add event publishing to methods:
  ```java
  public void createWallet(Wallet wallet) {
      // ... business logic ...
      eventPublisher.publishNotificationEvent(
          "WalletCreated",
          "wallet-service",
          wallet.getUserId(),
          wallet.getId().toString(),
          Map.of("currency", wallet.getCurrency(), "type", wallet.getType())
      );
  }
  
  public void updateBalance(Wallet wallet, BigDecimal newBalance) {
      wallet.setAvailableBalance(newBalance);
      walletRepository.save(wallet);
      
      eventPublisher.publishNotificationEvent(
          "WalletBalanceUpdated",
          "wallet-service",
          wallet.getUserId(),
          wallet.getId().toString(),
          Map.of("balance", newBalance, "currency", wallet.getCurrency())
      );
  }
  
  public void freezeWallet(Wallet wallet, String reason) {
      wallet.setFrozenAt(LocalDateTime.now());
      walletRepository.save(wallet);
      
      eventPublisher.publishNotificationEvent(
          "WalletFrozen",
          "wallet-service",
          wallet.getUserId(),
          wallet.getId().toString(),
          Map.of("reason", reason)
      );
  }
  ```

- [ ] Add events for:
  - WalletCreated
  - WalletBalanceUpdated
  - WalletLowBalance
  - WalletFrozen
  - WalletUnfrozen
  - WalletLimitsUpdated

- [ ] Test notifications via REST API:
  ```bash
  curl http://localhost:8007/api/notifications/user/user-123
  ```

---

## Phase 6: User Service Integration

- [ ] In `UserService.java`, add `EventPublisher` field
- [ ] Add event publishing:
  ```java
  public void registerUser(User user) {
      // ... business logic ...
      eventPublisher.publishNotificationEvent(
          "UserAccountCreated",
          "user-service",
          user.getId(),
          user.getId(),
          Map.of("email", user.getEmail(), "username", user.getUsername())
      );
  }
  
  public void changePassword(User user) {
      // ... business logic ...
      eventPublisher.publishNotificationEvent(
          "UserPasswordChanged",
          "user-service",
          user.getId(),
          user.getId(),
          Map.of()
      );
  }
  
  public void suspendAccount(User user, String reason) {
      // ... business logic ...
      eventPublisher.publishNotificationEvent(
          "UserAccountSuspended",
          "user-service",
          user.getId(),
          user.getId(),
          Map.of("reason", reason)
      );
  }
  ```

- [ ] Add events for:
  - UserAccountCreated
  - UserProfileUpdated
  - UserPasswordChanged
  - UserEmailChanged
  - UserPhoneChanged
  - UserAccountSuspended
  - UserAccountReactivated

---

## Phase 7: Verification & Testing

### 7.1 Event Publishing
- [ ] Trigger a transaction creation
- [ ] Verify Kafka topic `notification.transaction.events` has new message:
  ```bash
  kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic notification.transaction.events --from-beginning
  ```

### 7.2 Notification Creation
- [ ] Check database for new notifications:
  ```sql
  SELECT id, user_id, event_type, title, status FROM notifications 
  ORDER BY created_at DESC LIMIT 5;
  ```

### 7.3 REST API
- [ ] Get user notifications:
  ```bash
  curl http://localhost:8007/api/notifications/user/user-123?page=0&size=10
  ```

- [ ] Mark as read:
  ```bash
  curl -X PUT http://localhost:8007/api/notifications/{notification-id}/read
  ```

- [ ] Get unread count:
  ```bash
  curl http://localhost:8007/api/notifications/user/user-123/unread-count
  ```

### 7.4 Templates & Rules
- [ ] Verify templates in database:
  ```sql
  SELECT event_type, title_template FROM notification_templates;
  ```

- [ ] Check rule configuration in logs:
  ```
  Loaded X notification rules from configuration
  ```

---

## Phase 8: Extended Channels (Optional)

### Email Channel
- [ ] Configure SMTP settings in `application.yml`:
  ```yaml
  spring:
    mail:
      host: smtp.gmail.com
      port: 587
      username: your-email@gmail.com
      password: your-app-password
      properties:
        mail.smtp.auth: true
        mail.smtp.starttls.enable: true
  ```

- [ ] Extend `NotificationDispatchService`:
  ```java
  private void dispatchToEmail(Notification notification) {
      // Send via JavaMailSender
  }
  ```

- [ ] Update rule configuration:
  ```yaml
  TransactionCompleted:
    channels: [IN_APP, EMAIL]
  ```

### SMS Channel
- [ ] Add SMS provider (Twilio, AWS SNS, etc.)
- [ ] Implement in `NotificationDispatchService`

### PUSH Notifications
- [ ] Add Firebase Cloud Messaging
- [ ] Implement push notification dispatch

---

## Phase 9: Monitoring

- [ ] Set up Prometheus metrics:
  ```bash
  curl http://localhost:8007/actuator/metrics
  ```

- [ ] Monitor Kafka consumer lag:
  ```bash
  kafka-consumer-groups --bootstrap-server localhost:9092 \
    --group notification-service-transaction-group --describe
  ```

- [ ] Check application logs:
  ```bash
  tail -f logs/notification-service.log
  ```

- [ ] View user notification statistics:
  ```sql
  SELECT * FROM user_notification_stats WHERE user_id = 'user-123';
  ```

---

## Phase 10: Production Checklist

- [ ] Database backups configured
- [ ] Kafka cluster configured with replication
- [ ] SSL/TLS enabled for Kafka
- [ ] Application deployed with proper resource limits
- [ ] Alerts configured for:
  - High error rates in notification consumers
  - Kafka consumer lag exceeding threshold
  - Database connection pool exhaustion
  - Disk space on notification database
- [ ] Logging centralized (ELK, Splunk, etc.)
- [ ] Health checks configured in load balancer
- [ ] Graceful shutdown implemented
- [ ] Rate limiting on REST API endpoints

---

## Troubleshooting

### Issue: Events not being consumed
**Check**:
1. Kafka is running: `kafka-broker-api-versions --bootstrap-server localhost:9092`
2. Topics exist: `kafka-topics --list --bootstrap-server localhost:9092`
3. Consumer groups exist: `kafka-consumer-groups --bootstrap-server localhost:9092 --list`
4. Check logs for deserialization errors

### Issue: Notifications not appearing in database
**Check**:
1. Database connection in logs
2. Flyway migrations ran successfully: `SELECT * FROM flyway_schema_history;`
3. EventPublisher is being called
4. Rule is enabled in configuration
5. Check application logs for pipeline errors

### Issue: Templates not resolving
**Check**:
1. Template exists in database: `SELECT * FROM notification_templates WHERE event_type = 'TransactionCompleted';`
2. Template variables match payload fields
3. Check `NotificationCreationService` logs

### Issue: Kafka consumer lag increasing
**Check**:
1. Consumer thread count: increase `factory.setConcurrency()`
2. Check for stuck messages in DLQ
3. Monitor processor CPU/memory usage
4. Check database performance (slow inserts)

---

## Support & Rollback

### If service needs to be rolled back:
1. Stop new event publishing in microservices
2. Keep Notification Service running to process pending events
3. Events in Kafka will be retained for replay
4. Once fixed, restart with `--spring.kafka.consumer.auto-offset-reset=earliest`

### DLQ Management:
```bash
# Check DLQ messages
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic notification.transaction.events.dlt

# Replay DLQ to main topic (after fix)
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic notification.transaction.events.dlt | \
  kafka-console-producer --bootstrap-server localhost:9092 \
  --topic notification.transaction.events
```

---

**Last Updated**: December 19, 2025  
**Implementation Status**: ✅ COMPLETE - Ready for Integration
