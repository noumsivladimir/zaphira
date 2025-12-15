# Phase 3: Visual Rollout Guide

## 1. Feature Flag Control Flow

```
┌─────────────────────────────────────────────────────────────┐
│          Transaction Creation Flow (createTransaction)      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    Save Transaction
                            │
                            ▼
            Is Authorization Required?
                     │         │
                    NO        YES
                     │         │
                     ▼         ▼
                  Skip   Set Status = PENDING
                        (Save Transaction)
                            │
                            ▼
            ┌───────────────────────────────────┐
            │ NEW: Check Feature Flag           │
            │ shouldInitiateAsyncValidation()   │  ⭐
            └───────────────────────────────────┘
                     │         │         │
          DISABLED   │     0%  │  1-99%  │  100%
                     │         │         │
         NO ASYNC ◀──┘         │         │
                               │         │
        RANDOM DECISION ◀───────┘         │
                               │         │
        ASYNC ◀────────────────┴─────────┘
                     │
            (existing code)
                     │
                     ▼
         Return Created Transaction
```

---

## 2. Traffic Distribution Timeline

### Safe Deployment (Phase 3a - Day 1)

```
enabled=false, traffic=0%

Time: 0h ─ 24h
│
├─ 00:00 ──── Deploy with flag DISABLED
├─ 01:00 ──── Health check ✓
├─ 02:00 ──── Baseline metrics ✓
├─ 04:00 ──── Error rate normal ✓
├─ 08:00 ──── All systems stable ✓
├─ 12:00 ──── 24h monitoring passed ✓
└─ 24:00 ──── Ready for canary ✓

Async Traffic Distribution:
│
100% │ ─────────────────────────────────
     │ │ ALL SYNCHRONOUS
 0%  │ ─────────────────────────────────
     └─────────────────────────────────────► Time (hours)
       0    6    12    18    24
       
Consumer Status: IDLE (no messages consumed)
Database Growth: validation_requests = 0
Risk Level: ZERO ✅
```

### Canary Phase (Phase 3b - Day 2)

```
enabled=true, traffic=10%

Time: 0h ─ 4h
│
├─ 00:00 ──── Enable flag, set 10% traffic
├─ 00:15 ──── Kafka consumer active ✓
├─ 00:30 ──── First validations in DB ✓
├─ 01:00 ──── Metrics stable ✓
├─ 02:00 ──── Error rate check ✓
└─ 04:00 ──── Ready to scale ✓

Async Traffic Distribution:
│
100% │ ─────────────────────────────────
     │ │
 50% │ │ ┌───────── 90% SYNC
     │ │ │ ┌─ 10% ASYNC
 10% │ │ │ │
  0% │ ─┼─┴─────────────────────────────
     └─────────────────────────────────────► Time (hours)
       0    1    2    3    4

Consumer Status: ACTIVE (lag < 100)
Database Growth: validation_requests growing
Risk Level: LIMITED (10% blast radius) ✅
```

### Progressive Rollout (Phase 3c - Day 3-4)

```
enabled=true, traffic: 10% → 25% → 50% → 100%

Time: 0h ─ 6h
│
├─ 00:00 ──── 10% (proven from canary) ✓
├─ 01:00 ──── Scale to 25% ✓
├─ 02:00 ──── Monitor 25% ✓
├─ 03:00 ──── Scale to 50% ✓
├─ 04:00 ──── Monitor 50% ✓
├─ 05:00 ──── Scale to 100% ✓
└─ 06:00 ──── Full production ✓

Async Traffic Distribution:
│
100% │ ───┐
     │    │         ┌─── 100% ASYNC
 80% │    │    ┌────┘
     │    │    │
 60% │    │ ┌──┘
     │    │ │
 40% │    │ │  ┌─ 50% ASYNC
     │    │ │  │
 20% │ ┌──┘ │  │
     │ │    │  │
  0% │ │    │  │ 25% ASYNC 10% ASYNC
     └─┴────┴──┴─────────────────────────────► Time (hours)
      0  1  2  3  4  5  6
      
    CANARY  SCALE   FULL
             UP    ROLLOUT

Consumer Status: ACTIVE & SCALING (lag < 100)
Database Growth: Linear with traffic
Risk Level: PROGRESSIVE (4 steps, rollback at each) ✅
```

### Full Production (Phase 3d - Day 4+)

```
enabled=true, traffic=100%

Time: 6h+ (ongoing)
│
├─ 06:00 ──── 100% reached ✓
├─ 12:00 ──── 6h stable ✓
├─ 24:00 ──── 24h stable ✓
└─ ... ────── Continuous production

Async Traffic Distribution:
│
100% │ ═════════════════════════════════
     │ │ ALL TRANSACTIONS ASYNC
  0% │ ═════════════════════════════════
     └─────────────────────────────────────► Time
       0    12h   24h   48h   72h  ...

Consumer Status: FULLY ACTIVE (steady state)
Database Growth: Continuous, linear
Risk Level: ZERO (proven, all metrics stable) ✅
```

---

## 3. Error Rate Monitoring

### Target Metrics During Rollout

```
Error Rate vs Traffic Percentage
│
3%  │      ╭─ ALERT THRESHOLD
    │     ╱│
2%  │    ╱ │ Rollback if > 2x baseline
    │   ╱  │
1%  │  ╱   ├─ Baseline (typical)
    │ ╱    │
0%  │─────┴────────────────────────────────
    └──────────────────────────────────────► Traffic %
      0   10   25   50   75   100
      
    ✓ SAFE  ✓ SAFE  ✓ SAFE  ✓ SAFE
    (all increasing linearly with traffic)

If Error Rate Spikes:
│
    ╭──── Alert triggered
    │     Set traffic = 0%
3%  │  ╱╲  Error rate drops
    │ ╱  ╲
2%  ├─────╲──── Back to baseline
    │      ╲
1%  │       ╲═════════ (steady after rollback)
    │
0%  └───────────────────────────────────────► Time
      0   1h  2h  3h
```

---

## 4. Consumer Lag Monitoring

### Healthy Consumer Metrics

```
Consumer Lag (messages behind)
│
500 │      ╱╲          ALERT if > 500
    │     ╱  ╲
250 │    ╱    ╲
    │   ╱      ╲     Healthy range: < 100
100 │  ╱        ╲
    │ ╱          ╲
  0 │─────────────╲─────────────────
    └──────────────────────────────────────► Time (hours)
      0    1    2    3    4    5    6
      
    Canary   Scale   Full
    (10%)    (50%)   (100%)

Interpretation:
- Lag increases gradually: ✅ Normal (more traffic)
- Lag stays < 100: ✅ Consumer keeping up
- Lag spikes > 500: ❌ Problem detected
  └─ Action: Reduce traffic 50%, investigate
```

---

## 5. Transaction Success Rate

### Expected Distribution

```
Transaction Status Distribution Over Time
│
100%│ ┌─────────────────────────────────
    │ │ AUTHORIZED (traffic %)
 80%│ │  ╱╲
    │ │ ╱  ╲
 60%│ ├─    ╲
    │ │      ╲─── FAILED/REJECTED
 40%│ │
    │ │
 20%│ │ PENDING ──┐
    │ │           ├─── Should decrease
  0%│ └───────────┘    with scale-up
    └─────────────────────────────────────► Traffic %
      0   10   25   50   100

Metric: AUTHORIZED % should be identical
        across all traffic percentages
        (+ or - 1%)
        
✓ If identical: Feature working correctly
❌ If different: Validation logic issue
```

---

## 6. Database Growth Pattern

### Validation Requests Table

```
Row Count in validation_requests Table
│
10K │           ┌─── Full Production
    │          ╱│     (100% traffic)
 7K │         ╱ │
    │        ╱  │
 5K │       ╱   ├─── Progressive Rollout
    │      ╱    │     (10%→25%→50%→100%)
 2K │     ╱     │
    │    ╱      │
 0K │───┴───────┴─────────────────────────
    └────────────────────────────────────────► Time
      0  24h  2d  3d  4d  5d  6d+
      
   SAFE  CANARY  SCALE   FULL
   (0%)  (10%)  (25-50%) (100%)

Expected Growth Rate:
- Day 0: 0 rows (flag disabled)
- Day 1: ~500 rows/hour at 10% traffic
- Day 2: ~1250 rows/hour at 25% traffic
- Day 3: ~2500 rows/hour at 50% traffic
- Day 4+: ~5000 rows/hour at 100% traffic

Pattern: Should be LINEAR with traffic %
         (doubling every time we double traffic)
```

---

## 7. Rollback Scenario

### Emergency Response Timeline

```
Normal Operation
│
 0h │ Transactions flowing ✓
    │ Error rate: 0.5%
    │ Consumer lag: 50
    │ 100% traffic
    │
 1h │ ⚠️ ERROR RATE SPIKE TO 2.5% ❌
    │
 2h │ DECISION: Rollback
    │ Set traffic = 0%
    │ Restart service
    │
 3h │ ✅ Restored to sync
    │ Error rate: 0.5%
    │ All green
    │
 4h │ Root cause analysis
    │ Fix developed
    │ Prepare retry
    │
Analysis & Retry
│
 5h │ Fixed and retested
    │ Enable 5% traffic (lower)
    │ Monitor closely
    │
 6h │ Stable at 5%
    │ Increase to 10%
    │
 7h │ Continue scale-up
    │ Back on track
```

---

## 8. Feature Flag State Machine

```
                    ┌─────────────────────────────────┐
                    │  FEATURE FLAG DECISION ENGINE    │
                    └─────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │ enabled = false?    │  (Default)
                    │                     │
                    └──────────┬──────────┘
                         YES  │  NO
                             │
                    ┌────────┴─────────┐
                    │                  │
                    ▼                  ▼
            SYNC MODE (100%)    Check Traffic %
            (All transactions)   │
                    │            ├─ 0% ──→ SYNC MODE
                    │            ├─ 1-99% ──→ RANDOM
                    │            │         (Random pick)
                    │            └─ 100% ──→ ASYNC MODE
                    │                      (All async)
                    ▼
            ┌──────────────────┐
            │ Route to Sync    │
            │ (Traditional)    │
            └──────────────────┘
                    │
                    ▼
            ┌──────────────────────────────┐
            │ Return Sync Result           │
            │ (No Kafka involvement)       │
            └──────────────────────────────┘
            
            
EXAMPLE STATE TRANSITIONS:

Deployment: enabled=false → All SYNC (safe)
Canary: enabled=true, 10% → 90% SYNC, 10% ASYNC
Scale: enabled=true, 100% → All ASYNC
Rollback: enabled=true, 0% → All SYNC (emergency)
```

---

## 9. Monitoring Dashboard Layout

```
┌────────────────────────────────────────────────────────────┐
│              PHASE 3 ROLLOUT MONITORING DASHBOARD           │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─ Feature Flag Status ─────────────────────────────────┐ │
│  │ enabled: true  │  traffic-percentage: 10%             │ │
│  │ Current Time: 2025-12-16 14:30:00                     │ │
│  └─────────────────────────────────────────────────────┬─┘ │
│                                                         │   │
│  ┌─ Kafka Consumer Health ────────────────────────────┐   │
│  │ Group: transaction-service-validation-result       │   │
│  │ Current Lag: 45 messages ✓ (target < 100)         │   │
│  │ Throughput: 150 msg/sec ✓                         │   │
│  │ Error Rate: 0% ✓                                   │   │
│  └─────────────────────────────────────────────────┬──┘   │
│                                                    │       │
│  ┌─ Transaction Metrics ─────────────────────────┐       │
│  │ Total (last 1h): 1,500 transactions           │       │
│  │ Async: 150 (10.0%) ✓ (expected 10%)          │       │
│  │ Success Rate: 99.5% ✓ (baseline: 99.4%)      │       │
│  │ Error Rate: 0.5% ✓ (baseline: 0.6%)          │       │
│  └─────────────────────────────────────────────┬─┘       │
│                                                  │        │
│  ┌─ Database Health ─────────────────────────┐         │
│  │ validation_requests rows: 15,000          │         │
│  │ Growth rate: +250 rows/min ✓              │         │
│  │ Query time (p95): 8ms ✓ (target < 10ms)  │         │
│  │ Index usage: 100% ✓                       │         │
│  └─────────────────────────────────────────┬─┘         │
│                                              │          │
│  ┌─ Alert Status ────────────────────────┐            │
│  │ 🟢 All systems healthy                │            │
│  │ 🟢 No alerts triggered                │            │
│  │ Last update: 30 seconds ago            │            │
│  └───────────────────────────────────────┘            │
│                                                        │
│  [Next Action: Monitor for 30 min, then scale to 25%] │
└────────────────────────────────────────────────────────┘
```

---

## 10. Decision Tree Flowchart

```
┌─ START: New Transaction Created ─────────────┐
│                                              │
├─ Authorization required?                    │
│  ├─ NO  → Skip, return ✓                   │
│  └─ YES → Continue                         │
│                                              │
├─ validationFeatureProperties != null?       │
│  ├─ NO  → Sync mode ✓                      │
│  └─ YES → Continue                         │
│                                              │
├─ enabled = true?                           │
│  ├─ NO  → Sync mode ✓                      │
│  └─ YES → Continue                         │
│                                              │
├─ validationOrchestrationService != null?   │
│  ├─ NO  → Sync mode ✓                      │
│  └─ YES → Continue                         │
│                                              │
├─ traffic-percentage = ?                    │
│  ├─ 0 → Sync mode ✓                        │
│  ├─ 100 → Async mode (Kafka) ✓             │
│  └─ 1-99 → Random decision                 │
│      ├─ Random() < percentage → Async ✓    │
│      └─ Random() >= percentage → Sync ✓    │
│                                              │
└─ END: Transaction Processed ───────────────┘
```

---

## Summary Table

| Metric | Day 1 | Day 2 | Day 3 | Day 4+ |
|--------|-------|-------|-------|--------|
| **Flag Status** | Disabled | Enabled | Enabled | Enabled |
| **Traffic %** | 0% | 10% | 25→50→100% | 100% |
| **Async Calls** | 0 | ~10% | Progressive | 100% |
| **Sync Calls** | 100% | ~90% | Progressive | 0% |
| **Risk Level** | 🟢 ZERO | 🟡 LOW | 🟡 MEDIUM | 🟢 SAFE |
| **Blast Radius** | N/A | 10% | Increasing | N/A |
| **Rollback Time** | N/A | 30sec | 30sec | 30sec |
| **Monitoring** | Baseline | Intensive | Normal | Normal |

---

**All visualizations are production-ready reference guides for the rollout team!**
