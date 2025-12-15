# Phase 3: Rollout Execution Checklist

## Pre-Rollout Validation (Day 0)

### Code Integration
- [ ] `ValidationFeatureProperties.java` created ✅
- [ ] `TransactionService.java` modified with `shouldInitiateAsyncValidation()` ✅
- [ ] `application.properties` updated with feature flag configs ✅
- [ ] All imports correct (no compilation errors)
- [ ] Maven build succeeds: `mvn clean package -DskipTests`

### Configuration Validation
```bash
# Verify application.properties
grep -A4 "FEATURE FLAG" transaction-service/src/main/resources/application.properties

# Expected output:
# validation.feature.enabled=false
# validation.feature.traffic-percentage=0
# validation.feature.validation-timeout-seconds=300
# validation.feature.enable-auto-cleanup=true
```

- [ ] Feature flag disabled by default
- [ ] Traffic percentage = 0
- [ ] Timeout = 300 seconds
- [ ] Auto cleanup enabled

### Dependency Check
- [ ] Phase 2 components exist:
  - [ ] `ValidationOrchestrationService.java`
  - [ ] `ValidationRequestProducer.java`
  - [ ] `ValidationResultConsumer.java`
  - [ ] `ValidationCoordinatorService.java`
  - [ ] `ValidationRequest.java` (JPA entity)
  - [ ] `ValidationRequestRepository.java`
  - [ ] `KafkaConfig.java`

- [ ] Database setup complete:
  - [ ] `validation_requests` table created
  - [ ] 5 indexes created
  - [ ] Migration file: `V20251215__create_validation_requests_table.sql`

- [ ] Kafka setup complete:
  - [ ] Topic: `transaction.validation.request` (6 partitions, 3 replication)
  - [ ] Topic: `transaction.validation.result` (6 partitions, 3 replication)

### Local Testing
```bash
# Terminal 1: Start Kafka & DB (if needed)
docker-compose -f docker/docker-compose.yml up -d

# Terminal 2: Build & start transaction-service
cd transaction-service
mvn clean package -DskipTests
java -jar target/transaction-service-0.0.1-SNAPSHOT.jar

# Terminal 3: Monitor logs
tail -f transaction-service/logs/application.log | grep -i validation
```

- [ ] Application starts without errors
- [ ] No "unsatisfied dependency" exceptions
- [ ] Kafka consumer registered successfully
- [ ] Database connection successful
- [ ] Logs show: "AsyncValidation disabled - processing synchronously"

---

## Phase 3a: Safe Deployment (Production Day 1)

### Pre-Deployment

- [ ] Last code review passed
- [ ] All tests pass locally
- [ ] Backup database: `pg_dump wallet_db > backup_pre_phase3.sql`
- [ ] Team notified in Slack

### Deployment Steps

```bash
# Step 1: Deploy new application version
cd transaction-service
git pull origin services/updates
mvn clean package -DskipTests

# Step 2: Push to registry (adjust for your registry)
docker build -t zaphira/transaction-service:v1.0.3 .
docker push zaphira/transaction-service:v1.0.3

# Step 3: Update deployment (Kubernetes or Docker)
# Option A: Kubernetes
kubectl set image deployment/transaction-service \
  transaction-service=zaphira/transaction-service:v1.0.3 --record

# Option B: Docker Compose
# Edit docker-compose.yml image tag and run:
docker-compose -f docker/docker-compose.yml up -d --force-recreate transaction-service

# Step 4: Wait for health checks
sleep 30
```

- [ ] Deployment succeeded
- [ ] Health check: `curl http://localhost:8083/actuator/health`
- [ ] Response: `{"status":"UP"}`

### Post-Deployment Validation

```bash
# Check 1: Application started
curl http://localhost:8083/actuator/health
# Expected: {"status":"UP"}

# Check 2: Kafka consumer registered
kubectl logs deployment/transaction-service --tail=50 | grep "ListenerContainer"
# Expected: "startListeners" and "stopped = false"

# Check 3: Feature flag disabled (safe mode)
curl -X POST http://localhost:8083/api/transactions \
  -H "Content-Type: application/json" \
  -d '{...transaction request...}'

# Monitor logs for sync processing
kubectl logs deployment/transaction-service --tail=100 | grep -i async
# Expected: NO logs about async validation (feature disabled)

# Check 4: Transactions succeed
curl http://localhost:8083/api/transactions/{id}
# Expected: Status 200, transaction data returned

# Check 5: Kafka topics not consumed
kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result \
  --describe
# Expected: CURRENT-OFFSET = 0, LOG-END-OFFSET = 0 (no messages consumed)
```

- [ ] Application healthy
- [ ] Kafka consumer healthy but inactive
- [ ] Transactions processing (synchronously)
- [ ] Error rate = baseline (verify in APM)
- [ ] No exception logs related to validation

### Monitoring (24 hours)

```bash
# Set up continuous monitoring
watch -n 30 'kubectl logs deployment/transaction-service --tail=20 | grep -i "validation\|error"'
```

- [ ] Monitor dashboard: Error rate stable
- [ ] Monitor dashboard: Response time stable
- [ ] Monitor dashboard: Transaction volume normal
- [ ] No alerts triggered
- [ ] Database backups completed

**Decision:** All green? → Proceed to Phase 3b

---

## Phase 3b: Canary Testing (Production Day 2)

### Enable Feature Flag (10% Traffic)

```bash
# Edit application.properties
vim transaction-service/src/main/resources/application.properties

# Change:
#  validation.feature.enabled=false        → true
#  validation.feature.traffic-percentage=0 → 10

# Rebuild
mvn clean package -DskipTests

# Deploy
docker build -t zaphira/transaction-service:v1.0.3-canary .
docker-compose -f docker/docker-compose.yml up -d --force-recreate transaction-service

# Verify restart
sleep 20
curl http://localhost:8083/actuator/health
```

- [ ] Feature flag enabled
- [ ] Traffic percentage = 10
- [ ] Deployment successful
- [ ] Application healthy

### Canary Monitoring (0-30 minutes)

```bash
# Monitor Kafka producer (should send ~10% of transactions)
kafka-console-consumer.sh --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.request \
  --from-beginning \
  --max-messages 10

# Monitor Kafka consumer
kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result \
  --describe
# Expected: LAG > 0 (messages arriving)

# Monitor database growth
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "SELECT COUNT(*) as validation_count FROM validation_requests WHERE created_at > NOW() - INTERVAL '30 minutes';"
# Expected: Count > 0 (growing with time)

# Monitor transaction success rate
# Check APM/dashboard for error rate
# Expected: IDENTICAL to baseline ± 0.1%

# Monitor logs
kubectl logs deployment/transaction-service --tail=100 | grep -i "AsyncValidation"
# Expected: Mix of "processing asynchronously" and "random decision: skip"
```

- [ ] Kafka topics receiving messages (10% of traffic)
- [ ] Consumer lag: < 100 messages
- [ ] Database growing: validation_requests count increasing
- [ ] Error rate: IDENTICAL to baseline
- [ ] No timeouts or exceptions

### Canary Monitoring (30-60 minutes)

```bash
# Validate idempotence (duplicate message handling)
# Manually send duplicate message to Kafka topic
kafka-console-producer.sh --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.result \
  --property "key.serializer=org.apache.kafka.common.serialization.StringSerializer" \
  --property "value.serializer=org.apache.kafka.common.serialization.JsonSerializer"
# Send same validation result twice with same correlationId

# Check database (should be idempotent)
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "SELECT * FROM validation_requests WHERE correlation_id='DUPLICATE_ID' ORDER BY created_at;"
# Expected: Only ONE entry marked as PROCESSED (idempotence works)

# Check validation results
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "SELECT COUNT(*) as total, COUNT(DISTINCT correlation_id) as unique_ids FROM validation_requests WHERE status='PROCESSED';"
# Expected: total = unique_ids (no duplicates)

# Check transaction statuses
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "SELECT status, COUNT(*) FROM transactions WHERE created_at > NOW() - INTERVAL '1 hour' GROUP BY status;"
# Expected: AUTHORIZED count stable, no unexpected FAILED/EXPIRED
```

- [ ] Idempotence validation passed
- [ ] Duplicate messages handled correctly
- [ ] Transaction status distribution: Expected ✓

### Canary Monitoring (60-120 minutes)

```bash
# Check sustained performance
kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result \
  --describe
# Expected: LAG consistently < 100

# Check database query performance
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "EXPLAIN ANALYZE SELECT * FROM validation_requests WHERE correlation_id='test-id';"
# Expected: Index scan (not sequential), < 10ms

# Check expired validations (should be minimal)
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "SELECT COUNT(*) FROM validation_requests WHERE status='EXPIRED' AND created_at > NOW() - INTERVAL '2 hours';"
# Expected: < 5 (minimal timeouts)

# Check error logs
kubectl logs deployment/transaction-service --tail=1000 | grep -i "error\|exception\|warn" | wc -l
# Expected: No NEW errors related to validation (should match baseline)
```

- [ ] Consumer lag stable: < 100
- [ ] Database queries performing well
- [ ] Timeouts: < 1% (normal)
- [ ] Error rate: IDENTICAL to baseline
- [ ] All metrics green for 120+ minutes

**Decision:** All metrics stable for 2+ hours? → Proceed to Phase 3c

---

## Phase 3c: Progressive Rollout (Production Day 3-4)

### 10% → 25% Traffic Transition (Hour 1)

```bash
# Edit application.properties
vim transaction-service/src/main/resources/application.properties
# Change: validation.feature.traffic-percentage=10 → 25

# Rebuild & deploy
mvn clean package -DskipTests
docker build -t zaphira/transaction-service:v1.0.3-phase3c .
docker-compose -f docker/docker-compose.yml up -d --force-recreate transaction-service

# Verify
sleep 20
```

- [ ] Configuration updated (25%)
- [ ] Deployment successful
- [ ] Logs show mix of async and skip decisions
- [ ] No new errors

### Monitor 25% Traffic (30 minutes)

```bash
# Sample transaction processing rate
for i in {1..10}; do
  COUNT=$(psql -h 192.168.0.122 -U postgres -d wallet_db -t -c \
    "SELECT COUNT(*) FROM validation_requests WHERE created_at > NOW() - INTERVAL '5 minutes';")
  echo "Validation count (last 5 min): $COUNT"
  sleep 30
done

# Expected: Linear growth (25% of transactions)
```

- [ ] Traffic increase to ~25%: ✓
- [ ] Consumer lag: < 100 ✓
- [ ] Error rate: baseline ± 0.1% ✓

### 25% → 50% Traffic Transition (Hour 3)

```bash
# Same process
vim transaction-service/src/main/resources/application.properties
# Change: validation.feature.traffic-percentage=25 → 50

# Rebuild & deploy
mvn clean package -DskipTests
docker-compose -f docker/docker-compose.yml up -d --force-recreate transaction-service
```

- [ ] Configuration updated (50%)
- [ ] Deployment successful
- [ ] Error rate: baseline ± 0.1% ✓
- [ ] Consumer lag: < 100 ✓

### 50% → 100% Traffic Transition (Hour 5)

```bash
# Final step
vim transaction-service/src/main/resources/application.properties
# Change: validation.feature.traffic-percentage=50 → 100

# Rebuild & deploy
mvn clean package -DskipTests
docker-compose -f docker/docker-compose.yml up -d --force-recreate transaction-service
```

- [ ] Configuration updated (100%)
- [ ] Deployment successful
- [ ] 100% of new transactions use async validation
- [ ] All metrics stable

### Post-Rollout Validation (1 hour)

```bash
# Verify 100% traffic
COUNT=$(psql -h 192.168.0.122 -U postgres -d wallet_db -t -c \
  "SELECT COUNT(*) FROM validation_requests WHERE created_at > NOW() - INTERVAL '60 minutes' AND status='PROCESSED';")

TOTAL=$(psql -h 192.168.0.122 -U postgres -d wallet_db -t -c \
  "SELECT COUNT(*) FROM transactions WHERE created_at > NOW() - INTERVAL '60 minutes' AND status IN ('AUTHORIZED', 'FAILED', 'EXPIRED');")

PERCENTAGE=$((COUNT * 100 / TOTAL))
echo "Async validation coverage: $PERCENTAGE%"
# Expected: > 95% (accounting for failed auth checks)
```

- [ ] Async validation coverage: > 95%
- [ ] Error rate: baseline
- [ ] Consumer lag: stable
- [ ] Database healthy

---

## Emergency Rollback Procedure

### Scenario: Issues Detected During Canary/Rollout

```bash
# IMMEDIATE: Set traffic to 0%
vim transaction-service/src/main/resources/application.properties
# Change: validation.feature.traffic-percentage=0
# Keep: validation.feature.enabled=false (or true, doesn't matter at 0%)

# Quick rebuild & deploy (< 5 minutes)
mvn clean package -DskipTests
docker-compose -f docker/docker-compose.yml up -d --force-recreate transaction-service

# Verify rollback
sleep 20
curl http://localhost:8083/actuator/health

# Check: All transactions now process synchronously
kubectl logs deployment/transaction-service --tail=50 | grep -i "async"
# Expected: NO logs about async validation (traffic at 0%)
```

- [ ] Traffic set to 0%
- [ ] Deployment successful
- [ ] All transactions: synchronous mode
- [ ] Error rate: reverted to baseline

### Post-Rollback Analysis

```bash
# 1. Collect logs from rollout period
kubectl logs deployment/transaction-service \
  --since=1h \
  > transaction-service-logs-rollout.log

# 2. Query metrics from that period
psql -h 192.168.0.122 -U postgres -d wallet_db -c \
  "SELECT status, COUNT(*) FROM validation_requests WHERE created_at > NOW() - INTERVAL '1 hour' GROUP BY status;" \
  > validation_metrics.sql

# 3. Share with team for analysis
git add transaction-service-logs-rollout.log validation_metrics.sql
git commit -m "Phase 3 Rollback: Incident analysis"

# 4. Root cause analysis
# - Check if issue was: Code? Kafka? Database? Network?
# - Document fix required
# - Retest locally before re-enabling
```

- [ ] Logs collected
- [ ] Metrics captured
- [ ] Root cause identified
- [ ] Fix developed & tested
- [ ] Team consensus before re-enabling

---

## Sign-Off Checklist

### Pre-Deployment

- [ ] Tech Lead: Code review completed
- [ ] QA: Local testing passed
- [ ] DevOps: Infrastructure verified
- [ ] Security: No sensitive data in logs
- [ ] Product: Feature flag strategy understood

### Post-Canary (Before 25% Traffic)

- [ ] Monitoring: No new error patterns
- [ ] Performance: No degradation
- [ ] Database: Query performance acceptable
- [ ] Kafka: Consumer lag consistently < 100
- [ ] Team: Consensus to proceed

### Pre-Full Rollout (Before 100%)

- [ ] All metrics stable for 4+ hours
- [ ] Error rate: ± 0.1% of baseline
- [ ] Database: Cleanup successful
- [ ] Idempotence: Validated
- [ ] Rollback procedure: Tested
- [ ] On-call engineer: Notified & on alert

### Post-Full Rollout (24 hours after 100%)

- [ ] All transactions: Using async validation
- [ ] Error rate: Identical to baseline
- [ ] Consumer lag: Consistently low
- [ ] Database: Growing normally
- [ ] No anomalies detected
- [ ] Feature flag: Mark as "production-ready"

---

## Success Criteria

✅ **Phase 3 Complete When:**

1. **Functional**
   - ✅ Feature flag working as designed
   - ✅ Traffic percentage controls routing correctly
   - ✅ 100% of transactions using async validation

2. **Stable**
   - ✅ Error rate: baseline ± 0.1%
   - ✅ Response time: baseline + 10-50ms (async overhead)
   - ✅ No timeouts > 1%

3. **Scalable**
   - ✅ Kafka consumer lag: < 100 consistently
   - ✅ Database queries: < 10ms with indexes
   - ✅ Handle 10x traffic without issues

4. **Safe**
   - ✅ Idempotence: 100% validated
   - ✅ Rollback: Tested & working
   - ✅ Zero breaking changes
   - ✅ Can disable in 30 seconds

5. **Observable**
   - ✅ Metrics dashboard: All KPIs visible
   - ✅ Logs: Clear trace of async validation flow
   - ✅ Alerts: Set up for anomalies

---

## Timeline Summary

| Phase | Date | Config | Duration | Owner |
|-------|------|--------|----------|-------|
| 3a | Day 1 | enabled=false, 0% | 24h | DevOps |
| 3b | Day 2 | enabled=true, 10% | 2h | Engineering + On-Call |
| 3c | Day 3 | 25% → 50% → 100% | 4-6h | Engineering + On-Call |
| 3d | Day 4+ | enabled=true, 100% | Ongoing | Production |

---

## Support & Escalation

**During Rollout:**
- 📞 **On-Call:** [Phone/Slack]
- 📊 **Metrics:** [Dashboard URL]
- 📝 **Logs:** `kubectl logs deployment/transaction-service`
- 🔧 **Rollback:** `validation.feature.traffic-percentage=0` + restart

**Issue Escalation:**
1. Error rate > 1% above baseline → Reduce traffic by 50%
2. Consumer lag > 500 → Reduce traffic to 0%
3. Database issues → Disable feature flag entirely
4. Unresolved after 15 min → Rollback to previous version
