# 🧪 PHASE 2 - TESTING & VALIDATION PLAN

## Test 1: Kafka Topics Verification

### Prerequisites
- Kafka broker running on 192.168.0.122:9092
- kafka-topics CLI available

### Steps
```bash
# Verify topics exist
kafka-topics.sh --list --bootstrap-server 192.168.0.122:9092 | grep "transaction.validation"

# Expected output:
# transaction.validation.request
# transaction.validation.result

# Verify topic configuration
kafka-topics.sh --describe --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.request

# Expected:
# - Partitions: 6
# - Replication Factor: 3
# - Leader: distributed across brokers
# - ISR (in-sync replicas): all 3 replicas should be listed
```

### Validation Criteria
- ✅ Both topics exist
- ✅ Both have 6 partitions
- ✅ Both have 3 replication factor
- ✅ All replicas are in-sync

---

## Test 2: Database Migration Verification

### Prerequisites
- PostgreSQL running on 192.168.0.122:5432
- psql CLI available
- DB: wallet_db, User: postgres

### Steps
```bash
# Check table exists
psql -h 192.168.0.122 -U postgres -d wallet_db -c "
  SELECT table_name FROM information_schema.tables 
  WHERE table_schema='public' AND table_name='validation_requests';"

# Expected: validation_requests (one row)

# Check table structure
psql -h 192.168.0.122 -U postgres -d wallet_db -c "\d validation_requests"

# Expected columns:
# - id (BIGSERIAL PRIMARY KEY)
# - correlation_id (VARCHAR 36, UNIQUE, NOT NULL)
# - transaction_id (BIGINT, NOT NULL)
# - status (VARCHAR 20, NOT NULL)
# - requested_at (TIMESTAMP)
# - processed_at (TIMESTAMP nullable)
# - expires_at (TIMESTAMP)
# - created_at (TIMESTAMP)
# - updated_at (TIMESTAMP)

# Check indexes
psql -h 192.168.0.122 -U postgres -d wallet_db -c "
  SELECT indexname FROM pg_indexes 
  WHERE tablename='validation_requests';"

# Expected 5 indexes:
# - validation_requests_pkey
# - idx_validation_requests_correlation_id
# - idx_validation_requests_transaction_id
# - idx_validation_requests_status
# - idx_validation_requests_expires_at
# - idx_validation_requests_pending
```

### Validation Criteria
- ✅ Table validation_requests exists
- ✅ All 8 columns present with correct types
- ✅ Primary key on id
- ✅ Unique constraint on correlation_id
- ✅ 5+ indexes created
- ✅ Default values set correctly (status='PENDING', timestamps)

---

## Test 3: Application Startup & Consumer Registration

### Prerequisites
- All Kafka topics created
- Database migration executed
- application.properties configured
- Spring Boot 3.x + Kafka dependencies available

### Steps
```bash
# Build application
cd transaction-service
mvn clean package

# Start application
java -jar target/transaction-service.jar

# Monitor logs for Consumer startup
# Look for these patterns:

# Expected log outputs:
# 1. "Started TransactionService"
# 2. "Kafka cluster version: 7.5.0"
# 3. "Consumer config: group.id=transaction-service-validation-result"
# 4. "Subscribing to topic: transaction.validation.result"
# 5. "Successfully subscribed to topic transaction.validation.result"
# 6. "Fetching committed offsets for partitions: [...]"
# 7. "Seeking to END offsets for partitions: [...]"
```

### Validation Criteria
- ✅ Application starts without errors
- ✅ Kafka consumer registered with correct group ID
- ✅ Consumer subscribed to transaction.validation.result topic
- ✅ Consumer group shows in Kafka broker
- ✅ No connection errors or timeouts

### Verify Consumer Group
```bash
kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --list | grep "transaction-service-validation-result"

# Expected: transaction-service-validation-result

kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result \
  --describe

# Expected:
# GROUP: transaction-service-validation-result
# TOPIC: transaction.validation.result
# PARTITION: [0-5]
# CURRENT-OFFSET: 0
# LOG-END-OFFSET: 0
# LAG: 0
```

---

## Test 4: Producer Configuration Test

### Prerequisites
- Application running
- Kafka topics exist

### Steps
```java
// Unit Test: ValidationRequestProducer
@Test
public void testProducerConfiguration() {
    // Given: A validation request
    TransactionValidationRequest request = TransactionValidationRequest.builder()
        .transactionId(123L)
        .senderWalletNumber("W001")
        .receiverWalletNumber("W002")
        .amount(BigDecimal.valueOf(1000))
        .currency("XOF")
        .build();
    
    // When: Publish to Kafka
    producer.publishValidationRequest(request);
    
    // Then: Verify in logs
    // - "Publishing validation request for transaction: 123"
    // - "Successfully published validation request"
    // - correlationId generated (UUID format)
}

// Integration Test: Check Kafka topics receive messages
@Test
public void testMessagePublishedToKafka() throws Exception {
    // Given: Kafka consumer subscribed to topic
    // When: Producer publishes request
    // Then: Message received by consumer within 5 seconds
    
    ConsumerRecords<String, TransactionValidationRequest> records = 
        consumer.poll(Duration.ofSeconds(5));
    
    assertThat(records).isNotEmpty();
    assertThat(records.iterator().next().value().getTransactionId()).isEqualTo(123L);
}
```

### Validation Criteria
- ✅ Producer publishes without errors
- ✅ correlationId generated (UUID format)
- ✅ Message arrives in Kafka topic within 5 seconds
- ✅ Message format is JSON
- ✅ All required fields present in message

---

## Test 5: Consumer & Idempotence Test

### Prerequisites
- Application running
- ValidationResultConsumer active
- ValidationRequest table created

### Steps

#### Test 5A: Happy Path
```bash
# 1. Create transaction that requires authorization
curl -X POST http://localhost:8083/api/transactions \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderWalletNumber": "W001",
    "receiverWalletNumber": "W002",
    "amount": 1000,
    "currency": "XOF",
    "type": "P2P_TRANSFER",
    "channel": "MOBILE",
    "requestedBy": "test@example.com"
  }'

# Expected response: Transaction created with status=PENDING

# 2. Observe logs
# "Publishing validation request for transaction: XXX"
# "correlationId: UUID-STRING"

# 3. Check database
psql -h 192.168.0.122 -U postgres -d wallet_db -c "
  SELECT id, correlation_id, transaction_id, status 
  FROM validation_requests 
  ORDER BY created_at DESC LIMIT 1;"

# Expected: 1 row with status=PENDING

# 4. Simulate external validator publishing result
kafka-console-producer.sh --broker-list 192.168.0.122:9092 \
  --topic transaction.validation.result \
  --property "parse.key=true" \
  --property "key.separator=:" << EOF
123:{"transactionId": 123, "correlationId": "UUID-STRING", "validationStatus": "APPROVED", "riskScoreFinal": 25, "complianceStatus": "CLEAR", "validatorId": "external-validator"}
EOF

# 5. Observe consumer processing
# Look for logs:
# - "Received validation result for transaction: 123"
# - "Successfully processed validation result"

# 6. Verify transaction state updated
psql -h 192.168.0.122 -U postgres -d wallet_db -c "
  SELECT id, status FROM transactions WHERE id=123;"

# Expected: status=AUTHORIZED (or FAILED if validationStatus=REJECTED)

# 7. Verify idempotence flag
psql -h 192.168.0.122 -U postgres -d wallet_db -c "
  SELECT status FROM validation_requests WHERE correlation_id='UUID-STRING';"

# Expected: status=PROCESSED
```

#### Test 5B: Idempotence Test (Duplicate Messages)
```bash
# 1. Publish SAME validation result message 3 times
for i in {1..3}; do
  kafka-console-producer.sh --broker-list 192.168.0.122:9092 \
    --topic transaction.validation.result \
    --property "parse.key=true" \
    --property "key.separator=:" << EOF
123:{"transactionId": 123, "correlationId": "UUID-STRING", "validationStatus": "APPROVED", "riskScoreFinal": 25, "complianceStatus": "CLEAR", "validatorId": "external-validator"}
EOF
  sleep 1
done

# 2. Observe logs
# First message:
# - "Successfully processed validation result"

# Messages 2-3:
# - "Validation result already processed for correlationId"
# - "Skipping" (no further processing)

# 3. Verify database
psql -h 192.168.0.122 -U postgres -d wallet_db -c "
  SELECT id, correlation_id, status FROM validation_requests 
  WHERE correlation_id='UUID-STRING';"

# Expected: 1 row (not 3) with status=PROCESSED

# 4. Verify transaction status unchanged
psql -h 192.168.0.122 -U postgres -d wallet_db -c "
  SELECT id, status FROM transactions WHERE id=123;"

# Expected: status=AUTHORIZED (same as after message 1)
```

#### Test 5C: Error Handling - Transaction Not Found
```bash
# 1. Publish validation result for non-existent transaction
kafka-console-producer.sh --broker-list 192.168.0.122:9092 \
  --topic transaction.validation.result \
  --property "parse.key=true" \
  --property "key.separator=:" << EOF
999999:{"transactionId": 999999, "correlationId": "ERROR-UUID", "validationStatus": "APPROVED", "riskScoreFinal": 25, "complianceStatus": "CLEAR", "validatorId": "external-validator"}
EOF

# 2. Observe logs
# Expected:
# - "Business error processing validation result"
# - "Transaction not found: 999999"
# - No retry (message acknowledged, offset advanced)

# 3. Verify validation_requests table
# - No row created (business error, not tracked)

# Or if it exists:
psql -h 192.168.0.122 -U postgres -d wallet_db -c "
  SELECT status FROM validation_requests WHERE correlation_id='ERROR-UUID';"

# Expected: No rows
```

### Validation Criteria
- ✅ Happy path: Transaction status updated after result received
- ✅ Idempotence: Duplicate messages processed only once
- ✅ No duplicate rows in validation_requests table
- ✅ Error handling: Business errors don't cause retries
- ✅ Logs show correct flow for each scenario

---

## Test 6: Feature Flag Testing (When Implemented)

### Prerequisites
- Application with feature flag support
- Feature flag: feature.async-validation.enabled

### Steps
```properties
# Test Case 1: Feature flag OFF
feature.async-validation.enabled=false

# - Create transaction with authorizationRequired=true
# - Expected: Uses OLD synchronous validation (not Kafka)
# - Check logs: NO "Publishing validation request"
# - Check DB: NO rows in validation_requests

# Test Case 2: Feature flag ON
feature.async-validation.enabled=true

# - Create transaction with authorizationRequired=true
# - Expected: Uses NEW async Kafka validation
# - Check logs: YES "Publishing validation request"
# - Check DB: YES rows in validation_requests
```

### Validation Criteria
- ✅ Feature flag OFF: No Kafka messages published
- ✅ Feature flag OFF: No rows in validation_requests table
- ✅ Feature flag ON: Kafka messages published
- ✅ Feature flag ON: Rows created in validation_requests table
- ✅ Can toggle without restarting (if using ConfigServer)

---

## Test 7: Load Testing

### Prerequisites
- Load testing tool: Apache JMeter or k6
- Transaction service running
- Kafka broker healthy

### Scenario: 100 concurrent transactions
```bash
# Using Apache JMeter
jmeter -t load_test_transactions.jmx -l results.jtl -j logs.txt

# Expected metrics:
# - Response time: < 500ms (async, so fast)
# - Error rate: < 0.1%
# - Throughput: 100+ transactions/sec
# - Kafka lag: 0 (all messages processed quickly)
```

### Validation Criteria
- ✅ Application handles 100 concurrent requests
- ✅ Response time < 500ms (async)
- ✅ Error rate < 0.1%
- ✅ Kafka lag remains 0
- ✅ Database inserts successful for all validation_requests

---

## Test 8: Monitoring & Metrics

### Kafka Consumer Lag
```bash
# Monitor lag continuously
watch -n 5 'kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result \
  --describe'

# Expected: LAG column should be 0 or very small
# If LAG grows: Consumer might be slow or stopped
```

### Application Metrics (if using Spring Actuator)
```bash
# Get metrics
curl http://localhost:8083/actuator/metrics | jq

# Key metrics to monitor:
# - kafka.consumer.bytes-consumed-total
# - kafka.consumer.records-consumed-total
# - kafka.consumer.records-lag
# - kafka.producer.records-sent-total
# - kafka.producer.records-failed-total

# Check Kafka-specific metrics:
curl http://localhost:8083/actuator/metrics/kafka.consumer.records-lag | jq
```

### Database Monitoring
```sql
-- Check table growth
SELECT status, COUNT(*) as count 
FROM validation_requests 
GROUP BY status;

-- Expected:
-- PENDING: Low count (should be processed quickly)
-- PROCESSED: Growing as tests run
-- EXPIRED: 0 (unless 5+ min passed)

-- Check no duplicates
SELECT correlation_id, COUNT(*) as count 
FROM validation_requests 
GROUP BY correlation_id 
HAVING COUNT(*) > 1;

-- Expected: 0 rows (idempotence working)
```

---

## Summary: Test Execution Order

| Order | Test | Duration | Status |
|-------|------|----------|--------|
| 1 | Kafka Topics | 5 min | Must PASS |
| 2 | DB Migration | 5 min | Must PASS |
| 3 | App Startup | 5 min | Must PASS |
| 4 | Producer Config | 5 min | Must PASS |
| 5 | Consumer & Idempotence | 15 min | Must PASS |
| 6 | Feature Flag | 10 min | Nice to have |
| 7 | Load Testing | 30 min | Nice to have |
| 8 | Monitoring | 10 min | Continuous |

---

## Exit Criteria for Phase 2

- ✅ All 8+ tests PASSED
- ✅ No errors in application logs
- ✅ No errors in Kafka broker logs
- ✅ Idempotence verified (duplicates handled correctly)
- ✅ Database table growing as expected
- ✅ Consumer lag at 0
- ✅ Response times acceptable (< 500ms)
- ✅ Team trained and ready for Phase 3 (Canary Deployment)

---

**Next Phase:** Phase 3 - Canary Deployment (10% → 50% → 100% traffic)
