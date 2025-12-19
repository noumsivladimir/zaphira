# ⚠️ PHASE 2 - IMPORTANT WARNINGS & BEST PRACTICES

**Document:** Security & Implementation Guidelines  
**Status:** 🔴 **READ THIS BEFORE DEPLOYMENT**  
**Severity:** HIGH - Critical Financial Operations

---

## 🚨 COMMON MISTAKES TO AVOID

### ❌ NEVER DO THIS

#### 1. Don't Rely on Controller Security Only

**WRONG:**
```java
@PostMapping("/{id}/reverse")
public TransactionReversalResponse reverse(@RequestBody TransactionReversalRequest request) {
    // DANGEROUS: No authorization checks!
    return service.reverse(request);
}
```

**CORRECT:**
```java
@PostMapping("/{id}/reverse")
@PreAuthorize("hasAuthority('TRANSACTION_REVERSE')")
public TransactionReversalResponse reverse(
    @RequestBody TransactionReversalRequest request,
    @AuthenticationPrincipal AuthenticatedUser user) {
    // Controller layer checks permission
    // Service layer checks business rules
    List<String> roles = extractRolesFromJWT();
    List<String> permissions = extractPermissionsFromJWT();
    
    authorizationService.authorizeReversal(user, roles, permissions, ...);
    return service.reverse(...);
}
```

---

#### 2. Don't Skip Audit Logging on Error

**WRONG:**
```java
public TransactionReversalResponse reverse(...) {
    try {
        // ... reversal logic
        return response;
    } catch (Exception e) {
        log.error("Error: {}", e.getMessage());
        throw e;  // No audit log recorded!
    }
}
```

**CORRECT:**
```java
public TransactionReversalResponse reverse(...) {
    try {
        // ... reversal logic
        auditLogRepository.save(successLog);
        return response;
    } catch (Exception e) {
        log.error("Error: {}", e.getMessage());
        // Record FAILED operation in audit
        auditLogRepository.save(failureLog);
        throw e;
    }
}
```

---

#### 3. Don't Update Wallets Without Atomicity

**WRONG:**
```java
// DANGEROUS: If second update fails, wallets are inconsistent!
walletService.updateBalance(senderWalletId, newBalance1);
walletService.updateBalance(receiverWalletId, newBalance2);
```

**CORRECT:**
```java
@Transactional  // Entire method is atomic
public TransactionReversalResponse reverse(...) {
    // All updates in one transaction
    // If ANY fails, ALL are rolled back
    walletService.updateBalance(senderWalletId, newBalance1);
    walletService.updateBalance(receiverWalletId, newBalance2);
    auditLogRepository.save(auditLog);
    transactionRepository.save(transaction);
}
// All committed together, or none at all
```

---

#### 4. Don't Expose Sensitive Information in Responses

**WRONG:**
```json
{
  "message": "Database error: unique constraint violation on transaction_reversals.reference",
  "errorDetails": "SQL State: 23505",
  "dbErrorCode": 1234
}
```

**CORRECT:**
```json
{
  "error": "Operation Failed",
  "message": "An error occurred while processing your request",
  "code": "REVERSAL_FAILED",
  "requestId": "req-abc-123"
  // Technical details go to logs, not response!
}
```

---

#### 5. Don't Store Passwords or Sensitive Data in Audit Log

**WRONG:**
```java
auditLog.setDetails(new String(requestBytes)); // Contains password!
```

**CORRECT:**
```java
auditLog.setDetails("{\"transactionId\": 1, \"amount\": 100.00}");
// Only business data, never credentials or sensitive info
```

---

#### 6. Don't Allow Reversals/Refunds Without Time Limits

**WRONG:**
```java
// Transaction from 2023 can be refunded anytime!
public void authorizeRefund(...) {
    // No date check
}
```

**CORRECT:**
```java
public void authorizeRefund(...) {
    LocalDateTime maxRefundDate = transaction.getCreatedAt().plusDays(90);
    if (LocalDateTime.now().isAfter(maxRefundDate)) {
        throw new AccessDeniedException(
            "Transaction can only be refunded within 90 days of creation"
        );
    }
}
```

---

#### 7. Don't Allow SUPPORT to Refund Unlimited Amounts

**WRONG:**
```java
public void authorizeRefund(...) {
    // SUPPORT can refund anything!
}
```

**CORRECT:**
```java
public void authorizeRefund(...) {
    if (ROLE_SUPPORT.equals(userRole)) {
        if (refundAmount.compareTo(supportMaxRefund) > 0) {
            throw new AccessDeniedException(
                "SUPPORT can only refund up to " + supportMaxRefund
            );
        }
    }
}
```

---

#### 8. Don't Process Reversals on Already Reversed Transactions

**WRONG:**
```java
public TransactionReversalResponse reverse(...) {
    // No check if already reversed!
    return createCompensatoryTransaction(...);
}
```

**CORRECT:**
```java
public void authorizeReversal(...) {
    if (TransactionStatus.REVERSED.equals(transaction.getStatus()) ||
        TransactionStatus.REFUNDED.equals(transaction.getStatus())) {
        throw new AccessDeniedException(
            "Transaction has already been " + 
            transaction.getStatus().toString().toLowerCase()
        );
    }
}
```

---

### ⚠️ WHAT REQUIRES TESTING

#### 1. Edge Cases

```
✓ Test: Reverse exactly at 30-day boundary
✓ Test: Refund exactly at 90-day boundary
✓ Test: Reverse with partial fees
✓ Test: Refund partial amounts (10%, 50%, 99%)
✓ Test: Refund with fees
✓ Test: Support limit exactly at $5,000
✓ Test: MERCHANT on own vs. another's transaction
```

#### 2. Concurrent Operations

```
✓ Test: Two reversals simultaneously
✓ Test: Reverse + refund simultaneously
✓ Test: Multiple partial refunds concurrently
✓ Verify wallet balance consistency
✓ Verify audit log integrity
```

#### 3. Error Scenarios

```
✓ Test: Reverse non-existent transaction (404)
✓ Test: Reverse with invalid JWT (401)
✓ Test: Reverse without permission (403)
✓ Test: Reverse outside time limit (403)
✓ Test: Refund with amount > original (400)
✓ Test: Database connection failure
✓ Test: Kafka publisher failure
✓ Verify: Operation rolled back on error
✓ Verify: Audit log shows FAILED
```

---

### 🔐 SECURITY CHECKLIST

#### Before Going to Production

- [ ] **Authentication**
  - [ ] All endpoints require valid JWT
  - [ ] JWT expiration enforced
  - [ ] Token signature validation works
  - [ ] No expired tokens accepted

- [ ] **Authorization**
  - [ ] TRANSACTION_REVERSE permission required
  - [ ] TRANSACTION_REFUND permission required
  - [ ] TRANSACTION_REFUND_PARTIAL checked for partial
  - [ ] Role checks working (ADMIN, SUPPORT, MERCHANT)
  - [ ] Merchant ownership verified

- [ ] **Business Rules**
  - [ ] Reversal time limit (30 days) enforced
  - [ ] Refund time limit (90 days) enforced
  - [ ] Amount limits enforced
  - [ ] SUPPORT max amount ($5,000) enforced
  - [ ] Status validation working
  - [ ] Already reversed/refunded check working

- [ ] **Audit & Compliance**
  - [ ] All operations logged
  - [ ] Failed operations logged
  - [ ] Immutable audit table (no DELETEs)
  - [ ] IP and user-agent captured
  - [ ] Reason stored
  - [ ] Actor identified
  - [ ] Request ID for tracing

- [ ] **Data Integrity**
  - [ ] Transactions atomic
  - [ ] Wallet balances consistent
  - [ ] Compensatory transactions created
  - [ ] No orphaned records
  - [ ] Database constraints enforced

---

### 🧪 CRITICAL TEST SCENARIOS

#### Scenario 1: Authorized Reversal

```
GIVEN: SUPPORT user with TRANSACTION_REVERSE permission
  AND: Transaction status = COMPLETED
  AND: Transaction created < 30 days ago
WHEN: POST /api/transactions/{id}/reverse
THEN: 
  ✓ Response 200 OK
  ✓ Original status = REVERSED
  ✓ Compensatory transaction type = REVERSAL
  ✓ Audit log result = SUCCESS
  ✓ Kafka event published
```

#### Scenario 2: Unauthorized Reversal (Missing Permission)

```
GIVEN: USER without TRANSACTION_REVERSE permission
WHEN: POST /api/transactions/{id}/reverse
THEN:
  ✓ Response 403 FORBIDDEN
  ✓ Message: "You do not have permission to reverse..."
  ✓ Transaction unchanged
  ✓ Audit log result = FAILED (if logged at service level)
```

#### Scenario 3: Authorized Full Refund

```
GIVEN: MERCHANT user
  AND: Permission TRANSACTION_REFUND
  AND: Transaction owner (senderWallet.userId = currentUser.id)
  AND: Transaction status = COMPLETED
  AND: refundAmount = originalAmount
WHEN: POST /api/transactions/{id}/refund with FULL type
THEN:
  ✓ Response 200 OK
  ✓ Original status = REFUNDED
  ✓ Compensatory transaction created
  ✓ Wallet balances updated
  ✓ remainingRefundable = 0.00
  ✓ Audit log recorded
```

#### Scenario 4: Authorized Partial Refund

```
GIVEN: MERCHANT user
  AND: Permission TRANSACTION_REFUND_PARTIAL
  AND: Transaction owner
  AND: refundAmount < originalAmount
WHEN: POST /api/transactions/{id}/refund with PARTIAL type
THEN:
  ✓ Response 200 OK
  ✓ Original status = COMPLETED (unchanged)
  ✓ Metadata updated (partial_refund = true)
  ✓ remainingRefundable > 0
  ✓ Second partial refund allowed
```

#### Scenario 5: Support Limit Exceeded

```
GIVEN: SUPPORT user
  AND: Permission TRANSACTION_REFUND
  AND: refundAmount = 5001.00 (> $5000 limit)
WHEN: POST /api/transactions/{id}/refund
THEN:
  ✓ Response 403 FORBIDDEN
  ✓ Message mentions SUPPORT limit
  ✓ Transaction unchanged
```

#### Scenario 6: Reverse Outside Time Limit

```
GIVEN: Transaction created 31 days ago
  AND: SUPPORT user with TRANSACTION_REVERSE
WHEN: POST /api/transactions/{id}/reverse
THEN:
  ✓ Response 403 FORBIDDEN
  ✓ Message: "can only be reversed within 30 days"
```

---

### 📊 MONITORING & ALERTING

#### Critical Alerts to Configure

1. **Failed Reversal/Refund Attempts**
   ```
   Alert if: failures > 5 per hour
   Severity: HIGH
   Action: Investigate immediately
   ```

2. **Audit Log Write Failures**
   ```
   Alert if: audit write fails > 0
   Severity: CRITICAL
   Action: Stop processing, investigate database
   ```

3. **Kafka Event Publishing Failures**
   ```
   Alert if: Kafka send fails > 10 per hour
   Severity: HIGH
   Action: Check Kafka cluster status
   ```

4. **Unauthorized Access Attempts**
   ```
   Alert if: 403 errors > 20 per hour from same IP
   Severity: MEDIUM
   Action: Check for attack patterns
   ```

5. **Unusual Reversal/Refund Volume**
   ```
   Alert if: volume > 3x average
   Severity: MEDIUM
   Action: Investigate patterns
   ```

---

### 🔍 AUDIT LOG QUERIES

#### Find All Reversals for a User

```sql
SELECT * FROM transaction_audit_log
WHERE actor_user_id = 123
  AND action_type = 'REVERSE'
ORDER BY timestamp DESC;
```

#### Find Failed Operations

```sql
SELECT * FROM transaction_audit_log
WHERE result = 'FAILED'
  AND timestamp > NOW() - INTERVAL '24 hours'
ORDER BY timestamp DESC;
```

#### Find Suspicious Activity (Multiple Refunds)

```sql
SELECT 
    actor_user_id,
    COUNT(*) as action_count,
    SUM(amount) as total_amount
FROM transaction_audit_log
WHERE action_type IN ('REFUND', 'REFUND_PARTIAL')
  AND timestamp > NOW() - INTERVAL '1 hour'
GROUP BY actor_user_id
HAVING COUNT(*) > 10
ORDER BY COUNT(*) DESC;
```

#### Audit Trail for Transaction

```sql
SELECT * FROM transaction_audit_log
WHERE transaction_id = 1
ORDER BY timestamp ASC;
```

---

### 📝 COMPLIANCE REQUIREMENTS

**These are MANDATORY for financial services:**

- [ ] **PCI DSS:** If card payments involved
  - [ ] No card data in logs
  - [ ] No card data in audit trail
  - [ ] Encryption for cardholder data

- [ ] **SOX (Sarbanes-Oxley):** If public company
  - [ ] Complete audit trails required
  - [ ] Immutable logs required
  - [ ] Separation of duties (who can do what)

- [ ] **AML/KYC:** Anti-Money Laundering
  - [ ] Large refunds might trigger alerts
  - [ ] Unusual patterns need investigation
  - [ ] Audit trail for compliance checks

- [ ] **GDPR:** If EU customers
  - [ ] Right to access audit data
  - [ ] Right to be forgotten (archive after period)
  - [ ] Privacy by design

---

### 🎓 FINAL REMINDERS

**Remember:**
1. 🔐 **Trust Nothing**: Verify everything (auth, permission, status, amounts)
2. 📝 **Log Everything**: Every operation, every failure, every attempt
3. 🔄 **Be Atomic**: All or nothing, no partial states
4. ⏰ **Respect Limits**: Time, amount, rate limits
5. 🛡️ **Layer Security**: Controller + Service + Database
6. 🧪 **Test Edge Cases**: Boundaries, errors, concurrency
7. 📊 **Monitor Constantly**: Alerts, dashboards, investigations
8. 📚 **Document Thoroughly**: Code, architecture, processes

---

## ✅ SIGN-OFF

This document must be reviewed and understood before any deployment to production.

| Role | Date | Signature |
|------|------|-----------|
| Development Lead | _______ | __________ |
| Security Review | _______ | __________ |
| DevOps/Deployment | _______ | __________ |
| Project Manager | _______ | __________ |

---

**Status:** 🟢 **IMPORTANT - REVIEW BEFORE DEPLOYMENT**

**Last Updated:** 16 Décembre 2025
