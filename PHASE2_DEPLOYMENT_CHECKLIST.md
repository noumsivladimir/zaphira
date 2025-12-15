# ✅ PHASE 2 - INTEGRATION CHECKLIST

## Infrastructure (À faire manuellement)

- [ ] **Kafka Topics Created**
  - [ ] `transaction.validation.request` (6 partitions, 3 replica)
  - [ ] `transaction.validation.result` (6 partitions, 3 replica)
  - [ ] Verify: `kafka-topics.sh --list --bootstrap-server 192.168.0.122:9092`

- [ ] **Database Migration**
  - [ ] Run migration script: `V20251215__create_validation_requests_table.sql`
  - [ ] Verify table exists: `SELECT * FROM validation_requests;`
  - [ ] Verify indexes created (5 indexes)
  - [ ] Verify comments added

- [ ] **Application Configuration**
  - [ ] `application.properties` updated with Kafka properties ✅ DONE
  - [ ] KafkaConfig.java with all properties ✅ DONE
  - [ ] ValidationResultConsumer configured ✅ DONE
  - [ ] ValidationRequestProducer configured ✅ DONE

---

## Code Integration (Already Implemented)

### ✅ Java Files (8 files)
- [x] TransactionValidationRequest.java (Event)
- [x] TransactionValidationResult.java (Event + Enums)
- [x] ValidationRequestProducer.java (Producer)
- [x] ValidationResultConsumer.java (Consumer)
- [x] ValidationCoordinatorService.java (Coordinator)
- [x] ValidationOrchestrationService.java (Orchestration)
- [x] ValidationRequest.java (JPA Entity)
- [x] ValidationRequestRepository.java (Repository)

### ✅ Configuration
- [x] KafkaConfig.java (Producer + Consumer factories)
- [x] application.properties (Kafka + Validation properties)
- [x] TransactionService.java (ValidationOrchestrationService injected)

### ✅ Database
- [x] Migration file created: `V20251215__create_validation_requests_table.sql`

---

## Deployment Steps

### Step 1: Create Kafka Topics
```bash
# SSH to Kafka broker or run from any machine with kafka-topics CLI
kafka-topics.sh --create \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.request \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=86400000

kafka-topics.sh --create \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.result \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=86400000

# Verify
kafka-topics.sh --list --bootstrap-server 192.168.0.122:9092
```

### Step 2: Deploy Application
```bash
# Stop current transaction-service
docker stop transaction-service

# Pull latest code (Phase 1 changes)
git pull origin services/updates

# Build application
mvn clean package

# Start with feature flag DISABLED (safe mode)
# In application.properties, ensure: feature.async-validation.enabled=false

# Start transaction-service
docker start transaction-service

# Verify logs
docker logs -f transaction-service
```

### Step 3: Run Database Migration
```bash
# Migration runs automatically on application startup (Flyway)
# Or manually:
psql -h 192.168.0.122 -U postgres -d wallet_db -f V20251215__create_validation_requests_table.sql

# Verify table created
psql -h 192.168.0.122 -U postgres -d wallet_db -c "SELECT * FROM validation_requests LIMIT 0;"
```

### Step 4: Verify All Components
```bash
# 1. Check topics
kafka-topics.sh --describe --bootstrap-server 192.168.0.122:9092 --topic transaction.validation.request
kafka-topics.sh --describe --bootstrap-server 192.168.0.122:9092 --topic transaction.validation.result

# 2. Check table
psql -h 192.168.0.122 -U postgres -d wallet_db -c "\d validation_requests"

# 3. Check logs for Consumer startup
docker logs transaction-service | grep "ValidationResultConsumer\|@KafkaListener"

# 4. Test transaction creation (should NOT use async yet)
curl -X POST http://localhost:8083/api/transactions \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "senderWalletNumber": "W123",
    "receiverWalletNumber": "W456",
    "amount": 1000,
    "currency": "XOF",
    "type": "P2P_TRANSFER",
    "channel": "MOBILE",
    "requestedBy": "user@example.com"
  }'
```

---

## Feature Flag Strategy (Progressive Rollout)

**Phase 2.1: Testing (feature flag = OFF)**
```properties
feature.async-validation.enabled=false
# Behavior: Transactions use OLD synchronous validation
# Risk: ZERO - no change
# Duration: 24-48 hours
```

**Phase 2.2: Canary (feature flag = ON, 10% traffic)**
```properties
feature.async-validation.enabled=true
feature.async-validation.traffic-percentage=10
# Behavior: 10% of transactions use async Kafka validation
# Monitoring: Watch logs for errors, validation success rate
# Duration: 24 hours
```

**Phase 2.3: Gradual Rollout (feature flag = ON, 50% traffic)**
```properties
feature.async-validation.traffic-percentage=50
# Duration: 24 hours, increase monitoring
```

**Phase 2.4: Full Deployment (feature flag = ON, 100% traffic)**
```properties
feature.async-validation.traffic-percentage=100
# All transactions use async Kafka validation
```

**Rollback (any time):**
```properties
feature.async-validation.enabled=false
# Instant: All transactions revert to synchronous validation
# No data loss - transactions continue normally
```

---

## Monitoring & Alerts

### Kafka Metrics to Monitor
```bash
# Consumer lag (should be minimal)
kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result \
  --describe

# Expected: LAG near 0 if processing is fast

# Producer metrics
# Monitor: messages-sent, bytes-sent, send-errors
```

### Application Logs to Monitor
```bash
# Look for patterns in logs:
docker logs -f transaction-service | grep -i "validation\|kafka"

# Expected patterns (success):
# - "Publishing validation request for transaction"
# - "Received validation result for transaction"
# - "Successfully processed validation result"

# Warning patterns (investigate):
# - "Failed to publish validation request"
# - "Business error processing validation result"
# - "Technical error processing validation result"
```

### Database Monitoring
```bash
# Check validation_requests table growth
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "SELECT status, COUNT(*) as count FROM validation_requests GROUP BY status;"

# Check for expired validations
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "SELECT COUNT(*) as expired_count FROM validation_requests 
   WHERE status = 'PENDING' AND expires_at < NOW();"
```

---

## Testing Scenarios

### Scenario 1: Happy Path
1. Create transaction with authorizationRequired = true
2. Observe: TransactionCreatedEvent + ValidationRequest published to Kafka
3. External validator processes and publishes result
4. Observe: Transaction status updated to AUTHORIZED or FAILED
5. Check: validation_requests table shows PROCESSED status

### Scenario 2: Idempotence Test
1. Create transaction (generates correlationId)
2. Publish same ValidationResult message 3 times (simulate retry)
3. Expected: Only first result processed, rest ignored
4. Check: validation_requests table has 1 row with status=PROCESSED

### Scenario 3: Kafka Broker Failure
1. Stop Kafka broker
2. Try to create transaction with authorizationRequired = true
3. Expected: Transaction created successfully (async publish fails silently)
4. Check: Log shows "Failed to initiate async validation" but transaction continues
5. Restart Kafka broker
6. Expected: No impact - transaction already created

### Scenario 4: Timeout Scenario
1. Create validation request
2. Wait > 5 minutes without result
3. Run cleanup job: ValidationRequest.markExpiredValidations()
4. Check: validation_requests table shows status=EXPIRED
5. Monitor: transaction still has original status (no reversal)

---

## Rollback Plan (If Issues)

### Immediate Rollback (< 5 minutes)
```properties
# Option 1: Disable feature flag
feature.async-validation.enabled=false

# Option 2: Scale down validation consumer
docker scale transaction-service=0
```

### Data Cleanup (if needed)
```sql
-- Truncate validation requests table (restart)
TRUNCATE TABLE validation_requests;

-- Reset consumer offset
kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result \
  --reset-offsets --to-latest --execute
```

### Full Rollback (code revert)
```bash
git revert <commit-hash>
mvn clean package
# Restart with previous code
```

---

## Sign-Off Checklist

- [ ] All Kafka topics created and verified
- [ ] Database migration executed successfully
- [ ] Application compiled without errors
- [ ] Feature flag default = FALSE (safe)
- [ ] Logging configured correctly
- [ ] Monitoring dashboards set up
- [ ] Team trained on deployment steps
- [ ] Rollback plan documented and tested
- [ ] Stakeholders notified

---

**Status:** ✅ PHASE 2 READY FOR DEPLOYMENT

**Next Steps:**
1. Execute infrastructure steps (topics + migration)
2. Deploy application with feature flag = OFF
3. Monitor for 24 hours
4. Gradually enable feature flag (10% → 50% → 100%)
5. Celebrate! 🎉
