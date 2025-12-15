# Phase 3: Feature Flag & Canary Rollout Strategy

## Overview

Phase 3 implements a **production-safe feature flag system** for progressive rollout of async Kafka validation.

**Key Principle:** Zero impact in production until explicitly enabled.

---

## 1. Architecture

### ValidationFeatureProperties Component

**File:** `ValidationFeatureProperties.java`

Configuration class that controls all async validation behavior:

```java
@Component
@ConfigurationProperties(prefix = "validation.feature")
public class ValidationFeatureProperties {
    private boolean enabled = false;                    // Global on/off
    private int trafficPercentage = 0;                  // Canary percentage
    private int validationTimeoutSeconds = 300;         // Result timeout
    private boolean enableAutoCleanup = true;           // Cleanup strategy
}
```

### shouldInitiateAsyncValidation() Method

**File:** `TransactionService.java` (new method)

Decision logic with 3-level checks:

```
1. Feature Flag Check
   ├─ enabled = false? → Return false (SAFE MODE)
   └─ enabled = true? → Continue

2. Service Availability Check
   ├─ ValidationOrchestrationService = null? → Return false
   └─ Service available? → Continue

3. Traffic Percentage Check
   ├─ trafficPercentage = 0? → Return false (NO TRAFFIC)
   ├─ trafficPercentage = 100? → Return true (FULL ROLLOUT)
   └─ 0 < percentage < 100? → Random decision
       └─ Math.random() * 100 < trafficPercentage? → Return true/false
```

---

## 2. Configuration (application.properties)

### Default (Safe) Configuration

```properties
# ❌ DISABLED (DEFAULT - SAFE MODE)
validation.feature.enabled=false
validation.feature.traffic-percentage=0

# Supporting configs (always set)
validation.feature.validation-timeout-seconds=300
validation.feature.enable-auto-cleanup=true
```

**Impact:** 
- ✅ All transactions processed synchronously (existing behavior)
- ✅ Kafka validation infra deployed but **NOT USED**
- ✅ Zero breaking changes
- ✅ Production-safe

---

## 3. Rollout Strategy

### Phase 3a: Safe Deployment (Day 1)

```yaml
Configuration:
  enabled: false
  traffic-percentage: 0
  
Result:
  ✅ Deploy with all Kafka infra
  ✅ Create Kafka topics & DB migration
  ✅ Restart transaction-service
  ✅ Monitor: Zero async validation calls
  ✅ Tests pass: Synchronous path unchanged
```

### Phase 3b: Canary Testing (Day 2-3)

```yaml
Configuration:
  enabled: true
  traffic-percentage: 10  # 10% traffic
  
Rollout Plan:
  Step 1: Enable flag, set 10% traffic
  Step 2: Monitor Kafka metrics:
         - Topic throughput
         - Consumer lag
         - Validation time p95/p99
  Step 3: Monitor transaction success rate:
         - Should be identical to synchronous
         - No new failures
  Step 4: If metrics good → 25% traffic
```

### Phase 3c: Progressive Rollout (Day 4-5)

```yaml
Timeline:
  Hour 1-2:   10%  traffic  (1-2 hrs)
  Hour 3-4:   25%  traffic  (1-2 hrs)
  Hour 5-6:   50%  traffic  (2-3 hrs)
  Hour 7+:   100%  traffic  (full rollout)
  
Monitoring:
  Every 30 minutes:
    - Consumer lag < 100 messages
    - Transaction success rate = baseline
    - Error rate = baseline
    - No timeout exceptions
    
Rollback Trigger:
  Set traffic-percentage = 0
  Restart service (30 seconds)
  Transactions revert to synchronous
```

### Phase 3d: Full Rollout (Day 6+)

```yaml
Configuration:
  enabled: true
  traffic-percentage: 100

Result:
  ✅ All new transactions use Kafka validation
  ✅ Hybrid mode: Both sync + async processing
  ✅ Full idempotence guarantee
  ✅ Ready for production scale
```

---

## 4. Implementation Details

### Decision Flow in TransactionService

```java
public TransactionService(..., ValidationFeatureProperties featureProps) {
    this.validationFeatureProperties = featureProps;
    this.validationOrchestrationService = orchestrationService;
}

private void createTransaction(TransactionRequest request) {
    Transaction saved = repository.save(transaction);
    
    // Line 207-213 (MODIFIED):
    if (evaluation.isAuthorizationRequired()) {
        saved.applyStatus(TransactionStatus.PENDING);
        
        // FEATURE FLAG CHECK HERE ⭐
        if (shouldInitiateAsyncValidation()) {
            try {
                validationOrchestrationService.initiateAsyncValidation(saved);
            } catch (Exception e) {
                log.warn("Async validation failed (non-blocking)", e);
            }
        }
        // If flag disabled: Skip entire Kafka flow
    }
}

private boolean shouldInitiateAsyncValidation() {
    // Check 1: Feature enabled?
    if (!validationFeatureProperties.isEnabled()) {
        return false;  // ← SAFE MODE: All transactions sync
    }
    
    // Check 2: Service available?
    if (validationOrchestrationService == null) {
        return false;
    }
    
    // Check 3: Traffic percentage?
    int percentage = validationFeatureProperties.getTrafficPercentage();
    
    if (percentage <= 0) return false;          // 0% = no traffic
    if (percentage >= 100) return true;         // 100% = all traffic
    
    // Canary: Random decision
    return (int)(Math.random() * 100) < percentage;
}
```

---

## 5. Configuration Files Modified

### TransactionService.java

```diff
+ import com.zaphira.transaction.config.ValidationFeatureProperties;

  private ValidationOrchestrationService validationOrchestrationService;
+ private ValidationFeatureProperties validationFeatureProperties;

  @Autowired
  public TransactionService(
    ...
    ValidationOrchestrationService validationOrchestrationService,
+   ValidationFeatureProperties validationFeatureProperties
  ) {
    ...
+   this.validationFeatureProperties = validationFeatureProperties;
  }

- if (shouldInitiateAsyncValidation()) {
+ // Feature flag check - ADDED
+ if (validationOrchestrationService != null) {
    validationOrchestrationService.initiateAsyncValidation(saved);
  }

+ // NEW METHOD (35 lines)
+ private boolean shouldInitiateAsyncValidation() {
+   // 3-level check logic here
+ }
```

### ValidationFeatureProperties.java (NEW)

```
40 lines of configuration class
- enabled: boolean
- trafficPercentage: int (0-100)
- validationTimeoutSeconds: int
- enableAutoCleanup: boolean
```

### application.properties

```diff
+ # ============================================================================
+ # FEATURE FLAG: Async Kafka Validation (Phase 3)
+ # ============================================================================
+ validation.feature.enabled=false                    # Safe default
+ validation.feature.traffic-percentage=0             # No traffic
+ validation.feature.validation-timeout-seconds=300   # 5 min timeout
+ validation.feature.enable-auto-cleanup=true         # Cleanup strategy
```

---

## 6. Rollout Commands

### Enable Canary (10% Traffic)

```bash
# Update application.properties
sed -i 's/validation.feature.enabled=false/validation.feature.enabled=true/' \
    transaction-service/src/main/resources/application.properties

sed -i 's/validation.feature.traffic-percentage=0/validation.feature.traffic-percentage=10/' \
    transaction-service/src/main/resources/application.properties

# Rebuild & restart
cd transaction-service
mvn clean package -DskipTests
docker-compose restart transaction-service

# Monitor
kubectl logs -f deployment/transaction-service --tail=100 | grep "validation"
```

### Scale Up Traffic (50%)

```bash
# Edit application.properties
validation.feature.traffic-percentage=50

# Hot reload via actuator (if enabled) OR restart
docker-compose restart transaction-service
```

### Full Rollout (100%)

```bash
# Edit application.properties
validation.feature.traffic-percentage=100

# Restart
docker-compose restart transaction-service
```

### Emergency Rollback

```bash
# IMMEDIATE - Set to 0%
validation.feature.traffic-percentage=0

# Restart (all transactions revert to sync)
docker-compose restart transaction-service

# Monitor:
# - Should see zero async validation calls
# - Consumer lag increases (no consumption)
# - All transactions succeed synchronously
```

---

## 7. Monitoring & Metrics

### Key Metrics During Rollout

```
1. Traffic Percentage Match
   ├─ Expected: X% of transactions use Kafka
   └─ Monitor: Count async validations / total transactions
   
2. Kafka Consumer Health
   ├─ Consumer Lag: target < 100 messages
   ├─ Throughput: should match request rate at X%
   └─ Error Rate: should be 0
   
3. Validation Success Rate
   ├─ APPROVED: X%
   ├─ REJECTED: Y%
   ├─ FLAGGED→UNDER_REVIEW: Z%
   ├─ EXPIRED: < 0.1%
   └─ Technical Errors: should be 0
   
4. Transaction Metrics
   ├─ Success Rate: should be identical to sync
   ├─ Response Time: async slightly higher (background)
   ├─ Error Rate: should be identical to sync
   └─ No new failure modes
   
5. Idempotence Validation
   ├─ Duplicate correlationId: correctly skipped
   ├─ Database conflicts: none detected
   └─ Message redelivery: handled correctly
```

### Queries for Monitoring

```sql
-- Validation requests processed in last hour
SELECT COUNT(*) as validation_count
FROM validation_requests
WHERE created_at > NOW() - INTERVAL '1 hour'
  AND status = 'PROCESSED';

-- Expired validations (investigate timeout)
SELECT COUNT(*) as expired_count
FROM validation_requests
WHERE status = 'EXPIRED'
  AND created_at > NOW() - INTERVAL '1 hour';

-- Average processing time
SELECT 
  AVG(EXTRACT(EPOCH FROM (processed_at - created_at))) as avg_seconds
FROM validation_requests
WHERE processed_at IS NOT NULL
  AND created_at > NOW() - INTERVAL '1 hour';
```

### Log Patterns

```
# Safe mode (flag disabled)
[INFO] AsyncValidation disabled - processing transaction synchronously (txn_id=123)

# Canary active (10% traffic)
[INFO] AsyncValidation enabled (10% traffic) - processing asynchronously (txn_id=456)
[INFO] AsyncValidation enabled but random decision: skip (txn_id=789)

# Error handling
[WARN] Failed to initiate async validation for transaction: 999 (non-blocking)
```

---

## 8. Decision Tree: Should Enable?

```
┌─ Production stability acceptable?
│  ├─ NO  → Stay in Safe Mode (enabled=false)
│  └─ YES → Continue
│
├─ Infrastructure tested?
│  ├─ NO  → Run Phase 2 testing plan first
│  └─ YES → Continue
│
├─ Monitoring ready?
│  ├─ NO  → Set up Kafka metrics + DB queries
│  └─ YES → Continue
│
├─ Rollback plan tested?
│  ├─ NO  → Test: enabled=true → enabled=false (should succeed)
│  └─ YES → Continue
│
└─ Ready to enable canary (10%)?
   ├─ YES → Set enabled=true, traffic-percentage=10
   └─ NO  → Wait, re-evaluate
```

---

## 9. Troubleshooting

### Scenario 1: AsyncValidation Not Running

```bash
# Check feature flag
grep validation.feature.enabled application.properties
# Expected: true

# Check traffic percentage
grep validation.feature.traffic-percentage application.properties
# Expected: > 0

# Check logs for decision
kubectl logs deployment/transaction-service | grep "shouldInitiateAsyncValidation"

# Check Kafka topics exist
kafka-topics.sh --describe --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.request
```

### Scenario 2: Kafka Consumer Lag Too High

```bash
# Check consumer offset
kafka-consumer-groups.sh --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result \
  --describe

# If lag > 1000, reduce traffic:
validation.feature.traffic-percentage=5

# Restart
docker-compose restart transaction-service
```

### Scenario 3: Expired Validations Increasing

```sql
-- Check expiration rate
SELECT 
  COUNT(*) as expired,
  COUNT(*) * 100.0 / 
    (SELECT COUNT(*) FROM validation_requests 
     WHERE created_at > NOW() - INTERVAL '1 hour') as expired_percentage
FROM validation_requests
WHERE status = 'EXPIRED'
  AND created_at > NOW() - INTERVAL '1 hour';

-- If > 1%: Increase timeout
validation.feature.validation-timeout-seconds=600  # 10 minutes

# Restart
docker-compose restart transaction-service
```

---

## 10. Checklist

### Before Enabling Canary

- [ ] Kafka topics created (transaction.validation.request, .result)
- [ ] Database migration executed (validation_requests table)
- [ ] Transaction-service restarted with new code
- [ ] Monitoring dashboard set up
- [ ] Rollback procedure tested
- [ ] Team on-call notified
- [ ] Feature flag documentation shared

### During Canary (10%)

- [ ] 30 min: Monitor Kafka lag (should be < 100)
- [ ] 30 min: Monitor transaction success rate (should = baseline)
- [ ] 60 min: Check database (validation_requests growing normally)
- [ ] 120 min: Increase to 25% if no issues

### Before Full Rollout

- [ ] All metrics stable for 2+ hours at 50%
- [ ] Error rate = baseline ± 0.1%
- [ ] No timeout exceptions
- [ ] Consumer lag < 100 consistently
- [ ] Idempotence validation passed
- [ ] Team consensus to proceed

---

## Summary

| Phase | Flag | Traffic | Duration | Status |
|-------|------|---------|----------|--------|
| 3a | false | 0% | 24h | Safe Deployment |
| 3b | true | 10% | 2-4h | Canary Testing |
| 3c | true | 10→50→100% | 4-6h | Progressive Rollout |
| 3d | true | 100% | Ongoing | Full Production |

**Control:** Simple property change → Instant traffic routing
**Safety:** Disabled by default → Zero production risk
**Reversibility:** Set to 0% → Immediate rollback to sync
