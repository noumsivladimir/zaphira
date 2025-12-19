# Wallet Structure Coherence - Integration Testing Guide

**Status:** ✅ CODE COHERENCE ACHIEVED  
**Compilation:** ✅ ALL MODULES BUILDING  
**Next Phase:** Integration Testing

---

## What Has Been Fixed

### 1. Entity-Database Mismatch (CRITICAL - RESOLVED)

**Problem:** Database had different schema than JPA entity expected
- DB had `available_balance`, `blocked_balance`, `total_balance` (3 fields)
- Entity had single `balance` field
- DB had `frozen_at` for status, entity had `active` column

**Solution Implemented:**
```java
// Wallet.java - Now matches database exactly
@Column(name = "available_balance")
private BigDecimal availableBalance;

@Column(name = "blocked_balance")
private BigDecimal blockedBalance;

@Column(name = "total_balance")
private BigDecimal totalBalance;

@Column(name = "frozen_at")
private LocalDateTime frozenAt;

// Backward compatibility methods for existing code
@Transient
public BigDecimal getBalance() {
    return this.availableBalance != null ? this.availableBalance : BigDecimal.ZERO;
}

@Transient
public Boolean getActive() {
    return this.frozenAt == null;
}
```

---

### 2. Field Mapping Across Microservices (RESOLVED)

**Problem:** WalletDTO and Wallet entity had inconsistent field sets

**Solution Implemented:**
- ✅ WalletDTO now has 23 fields matching Wallet entity
- ✅ TransactionService.mapWalletDtoToWallet() maps ALL 20+ fields
- ✅ WalletService.createWallet() initializes ALL fields
- ✅ WalletService.toDTO() exports ALL fields

---

### 3. Balance Operations Coherence (RESOLVED)

**Problem:** Different services handled balance differently

**Solution Implemented:**
- ✅ All services use `getBalance()` / `setBalance()` for compatibility
- ✅ getBalance() returns `availableBalance`
- ✅ setBalance() updates `availableBalance`
- ✅ All balance checks use backward-compatible methods

---

### 4. Frozen Status Handling (RESOLVED)

**Problem:** No consistent way to check if wallet is frozen

**Solution Implemented:**
- ✅ getActive() returns `frozenAt == null`
- ✅ WalletRoutingStrategy uses getActive() correctly
- ✅ CardRoutingStrategy uses getActive() correctly
- ✅ Transactions properly validate frozen status before processing

---

## Integration Testing Scenarios

### Scenario 1: User Registration & Wallet Creation

**Flow:**
1. User registers in Auth Service
2. Auth Service creates wallet via WalletServiceClient
3. Wallet Service creates wallet with all fields initialized
4. Wallet Service returns WalletDTO
5. Auth Service stores wallet reference

**Tests to Run:**
```bash
# Test wallet creation with all fields
mvn test -Dtest=WalletServiceTest#testCreateWallet

# Test Auth Service integration
mvn test -Dtest=AuthServiceTest#testUserRegistrationWithWallet
```

**Expected Results:**
- ✅ Wallet created with availableBalance = 0.0
- ✅ Wallet currency = "XOF"
- ✅ Wallet type = "REGULAR"
- ✅ Wallet isPrimary = false
- ✅ All timestamps initialized
- ✅ frozenAt = null (wallet active)

---

### Scenario 2: Transaction Validation

**Flow:**
1. Transaction Service receives transaction request
2. Fetches sender/receiver wallets via Feign client
3. Maps WalletDTO to Wallet entity
4. Validates both wallets are active
5. Checks sender balance is sufficient
6. Proceeds with transaction

**Tests to Run:**
```bash
# Test wallet retrieval and mapping
mvn test -Dtest=TransactionServiceTest#testCreateTransaction

# Test routing strategy
mvn test -Dtest=WalletRoutingStrategyTest
```

**Expected Results:**
- ✅ WalletDTO properly mapped to Wallet
- ✅ All 23 fields populated correctly
- ✅ getActive() returns correct value
- ✅ getBalance() returns availableBalance
- ✅ Frozen wallets properly rejected

---

### Scenario 3: Transaction with Frozen Wallet

**Flow:**
1. Set receiver wallet frozenAt = now()
2. Attempt to create transaction
3. WalletRoutingStrategy.isBothWalletsActive() called
4. receiver.getActive() returns false (frozenAt != null)
5. Transaction rejected

**Tests to Run:**
```bash
# Test frozen wallet rejection
mvn test -Dtest=WalletRoutingStrategyTest#testFrozenWalletRejection
```

**Expected Results:**
- ✅ getActive() returns false when frozenAt is set
- ✅ Transaction routing fails for frozen wallets
- ✅ Proper error message returned to user

---

### Scenario 4: Balance Operations

**Flow:**
1. Create transaction with debit
2. WalletService.debit() called
3. Uses getBalance() / setBalance()
4. availableBalance reduced
5. Wallet saved

**Tests to Run:**
```bash
# Test balance debit
mvn test -Dtest=WalletServiceTest#testDebitWallet

# Test balance credit
mvn test -Dtest=WalletServiceTest#testCreditWallet
```

**Expected Results:**
- ✅ availableBalance reduced by debit amount
- ✅ getBalance() reflects change
- ✅ Database row updated
- ✅ No null pointer exceptions

---

### Scenario 5: Transaction Reversal

**Flow:**
1. Original transaction fetched
2. Reversal service accesses wallets
3. Uses getBalance() / setBalance()
4. Reverses balance updates
5. Wallets saved

**Tests to Run:**
```bash
# Test transaction reversal
mvn test -Dtest=TransactionReversalServiceTest#testReverseTransaction
```

**Expected Results:**
- ✅ Sender balance restored
- ✅ Receiver balance reversed
- ✅ Uses backward-compatible methods
- ✅ Transaction status updated to REVERSED

---

## Full Integration Test Command

Run all integration tests:

```bash
# From project root
mvn clean test

# Or run specific test suites
mvn test -Dtest=WalletServiceTest,TransactionServiceTest,WalletRoutingStrategyTest
```

---

## Database Connection Test

Before running integration tests, verify database connection:

```sql
-- Connect to PostgreSQL and verify wallet table
SELECT * FROM wallets LIMIT 1;

-- Expected columns:
-- id, wallet_number, user_id, available_balance, blocked_balance, total_balance,
-- status, currency, type, is_primary, daily_limit, daily_spent, monthly_limit,
-- monthly_spent, frozen_at, frozen_by, frozen_reason, created_at, updated_at,
-- closed_at, last_limit_reset, metadata, version
```

---

## Validation Checklist

Before marking as complete:

- [ ] Run: `mvn clean test` - All tests pass
- [ ] Verify: Database schema matches Wallet.java @Column mappings
- [ ] Check: Wallet creation initializes all fields
- [ ] Confirm: getBalance() / setBalance() work with availableBalance
- [ ] Verify: getActive() works based on frozenAt
- [ ] Test: Frozen wallet rejection in routing strategy
- [ ] Check: TransactionService maps all 23 fields from DTO
- [ ] Verify: WalletService returns complete WalletDTO
- [ ] Test: Inter-service communication (Auth → Wallet Service)
- [ ] Confirm: No null pointer exceptions in balance operations
- [ ] Check: No SQL column not found errors
- [ ] Verify: Transaction reversal/refund use correct balance fields
- [ ] Test: All backward-compatible methods work
- [ ] Confirm: Compilation produces 0 errors

---

## Common Issues & Solutions

### Issue 1: Column not found error
**Cause:** Entity field not mapped correctly  
**Solution:** Check @Column(name="...") annotation matches database column name

### Issue 2: NullPointerException on balance
**Cause:** Code using .balance instead of .availableBalance  
**Solution:** Use getBalance() method which handles null and maps to availableBalance

### Issue 3: Active field always false
**Cause:** getActive() not checking frozenAt correctly  
**Solution:** Verify getActive() returns `frozenAt == null`

### Issue 4: DTO missing fields
**Cause:** TransactionService not mapping all fields  
**Solution:** Use mapWalletDtoToWallet() which maps all 23 fields

### Issue 5: Frozen wallets not rejected
**Cause:** Routing strategy not using getActive()  
**Solution:** Verify WalletRoutingStrategy calls getActive() on both wallets

---

## Performance Considerations

### Batch Wallet Lookups
If retrieving multiple wallets, consider batch loading:
```java
// Instead of individual calls
List<Wallet> wallets = walletRepository.findByIds(walletIds);
```

### DTO Mapping Optimization
For high-volume scenarios, use MapStruct for faster mapping:
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

### Database Indexes
Ensure these columns are indexed for performance:
- wallet_number (UNIQUE)
- user_id (FOREIGN KEY)
- status / frozen_at (for filtering)

---

## Documentation Updates Made

Files created/updated:
- ✅ [WALLET_COHERENCE_VERIFICATION.md](WALLET_COHERENCE_VERIFICATION.md) - Complete coherence report
- ✅ This file - Integration testing guide

---

## Next Steps

1. **Run Integration Tests:**
   ```bash
   mvn clean test
   ```

2. **Verify Database Connectivity:**
   - Check PostgreSQL is running
   - Verify schema matches entity mappings
   - Run sample queries on wallets table

3. **Start Services:**
   ```bash
   # Terminal 1: Wallet Service
   java -jar wallet-service/target/wallet-service.jar
   
   # Terminal 2: Transaction Service
   java -jar transaction-service/target/transaction-service.jar
   
   # Terminal 3: Auth Service
   java -jar auth/target/auth.jar
   ```

4. **Manual Testing:**
   - Create user via Auth Service
   - Verify wallet created in Wallet Service
   - Create transaction via Transaction Service
   - Verify balance updated
   - Freeze wallet and test rejection

5. **Load Testing:**
   - Test with concurrent transactions
   - Verify balance consistency
   - Check database connections under load

---

## Success Criteria

✅ Integration tests pass  
✅ No SQL errors  
✅ No null pointer exceptions  
✅ Wallets created with all fields  
✅ Transactions route correctly  
✅ Frozen wallets rejected  
✅ Balance operations consistent  
✅ Cross-service communication works  
✅ Database matches entity  
✅ All backward-compatible methods work

---

## Deployment Checklist

- [ ] All tests passing
- [ ] Code reviewed for consistency
- [ ] Database migrations complete
- [ ] Services configured for production
- [ ] Monitoring configured
- [ ] Logging configured
- [ ] Error handling validated
- [ ] Performance tested
- [ ] Security validated
- [ ] Documentation complete

---

**Status:** Ready for Integration Testing  
**Confidence Level:** ✅ HIGH  
**Recommendation:** Proceed with full test suite execution
