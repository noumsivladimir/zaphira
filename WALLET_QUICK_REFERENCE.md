# Wallet Structure - Quick Reference Guide

**For Developers Working with Wallet Objects**

---

## TL;DR - What You Need to Know

### Use These Methods (Backward Compatible)
```java
// Getting balance
BigDecimal balance = wallet.getBalance();              // ✅ CORRECT
BigDecimal balance = wallet.availableBalance;          // ✅ OK but use method above

// Setting balance
wallet.setBalance(new BigDecimal("1000"));              // ✅ CORRECT

// Checking if wallet is frozen
Boolean isActive = wallet.getActive();                  // ✅ CORRECT
if (isActive) { /* process transaction */ }            // ✅ CORRECT

// Don't use these (they don't exist)
wallet.getBalance();           // ❌ WRONG - Entity doesn't map
wallet.isActive();             // ❌ WRONG - Column doesn't exist
```

---

## Field Reference

### Balance Fields (Use getBalance() for compatibility)
```
availableBalance     → Spendable balance
blockedBalance       → Blocked/Reserved balance
totalBalance         → availableBalance + blockedBalance
```

**Best Practice:** Use `getBalance()` for most operations (returns `availableBalance`)

### Frozen Status (Use getActive())
```
frozenAt    → Timestamp when wallet was frozen (null = active)
frozenBy    → User ID who froze wallet
frozenReason → Why wallet was frozen
```

**Best Practice:** Use `getActive()` which returns `frozenAt == null`

### Spending Limits
```
dailyLimit      → Daily spending limit
dailySpent      → Amount spent today
monthlyLimit    → Monthly spending limit  
monthlySpent    → Amount spent this month
```

### Wallet Properties
```
walletNumber   → 8-digit wallet number
currency       → Currency code (e.g., "XOF")
type           → Wallet type (e.g., "REGULAR")
isPrimary      → Is this the primary wallet?
```

### Timestamps
```
createdAt          → When wallet was created
updatedAt          → When wallet was last updated
closedAt           → When wallet was closed (if applicable)
lastLimitReset     → When daily/monthly limits were last reset
```

---

## Common Operations

### Creating a Wallet
```java
Wallet wallet = Wallet.builder()
    .walletNumber("12345678")
    .availableBalance(BigDecimal.ZERO)
    .blockedBalance(BigDecimal.ZERO)
    .totalBalance(BigDecimal.ZERO)
    .currency("XOF")
    .type("REGULAR")
    .status(BigDecimal.ZERO)
    .isPrimary(false)
    .dailySpent(BigDecimal.ZERO)
    .monthlySpent(BigDecimal.ZERO)
    .frozenAt(null)
    .createdAt(LocalDateTime.now())
    .updatedAt(LocalDateTime.now())
    .build();
```

### Checking Balance
```java
// Check if sufficient funds
if (wallet.getBalance().compareTo(amount) >= 0) {
    // Process transaction
}
```

### Debit (Withdraw)
```java
BigDecimal newBalance = wallet.getBalance().subtract(amount);
wallet.setBalance(newBalance);
walletRepository.save(wallet);
```

### Credit (Deposit)
```java
BigDecimal newBalance = wallet.getBalance().add(amount);
wallet.setBalance(newBalance);
walletRepository.save(wallet);
```

### Freezing a Wallet
```java
wallet.setFrozenAt(LocalDateTime.now());
wallet.setFrozenBy(userId);
wallet.setFrozenReason("Suspicious activity");
walletRepository.save(wallet);
```

### Checking if Wallet is Frozen
```java
if (wallet.getActive()) {
    // Wallet is not frozen - can use
} else {
    // Wallet is frozen - reject transaction
}
```

---

## DTO Mapping

### Creating WalletDTO from Wallet
```java
WalletDTO dto = WalletDTO.builder()
    .id(wallet.getId())
    .walletNumber(wallet.getWalletNumber())
    .availableBalance(wallet.getAvailableBalance())
    .blockedBalance(wallet.getBlockedBalance())
    .totalBalance(wallet.getTotalBalance())
    .currency(wallet.getCurrency())
    .type(wallet.getType())
    .status(wallet.getStatus())
    .userId(wallet.getUserId())
    .isPrimary(wallet.getIsPrimary())
    .dailyLimit(wallet.getDailyLimit())
    .dailySpent(wallet.getDailySpent())
    .monthlyLimit(wallet.getMonthlyLimit())
    .monthlySpent(wallet.getMonthlySpent())
    .frozenAt(wallet.getFrozenAt())
    .frozenBy(wallet.getFrozenBy())
    .frozenReason(wallet.getFrozenReason())
    .createdAt(wallet.getCreatedAt())
    .updatedAt(wallet.getUpdatedAt())
    .closedAt(wallet.getClosedAt())
    .lastLimitReset(wallet.getLastLimitReset())
    .metadata(wallet.getMetadata())
    .version(wallet.getVersion())
    .build();
```

### Creating Wallet from WalletDTO
```java
Wallet wallet = Wallet.builder()
    .id(dto.getId())
    .walletNumber(dto.getWalletNumber())
    .availableBalance(dto.getAvailableBalance())
    .blockedBalance(dto.getBlockedBalance())
    .totalBalance(dto.getTotalBalance())
    .currency(dto.getCurrency())
    .type(dto.getType())
    .status(dto.getStatus())
    .userId(dto.getUserId())
    .isPrimary(dto.getIsPrimary())
    .dailyLimit(dto.getDailyLimit())
    .dailySpent(dto.getDailySpent())
    .monthlyLimit(dto.getMonthlyLimit())
    .monthlySpent(dto.getMonthlySpent())
    .frozenAt(dto.getFrozenAt())
    .frozenBy(dto.getFrozenBy())
    .frozenReason(dto.getFrozenReason())
    .createdAt(dto.getCreatedAt())
    .updatedAt(dto.getUpdatedAt())
    .closedAt(dto.getClosedAt())
    .lastLimitReset(dto.getLastLimitReset())
    .metadata(dto.getMetadata())
    .version(dto.getVersion())
    .build();
```

---

## Testing Wallets

### Creating Test Wallet
```java
WalletDTO walletDto = WalletDTO.builder()
    .id(1L)
    .walletNumber("12345678")
    .availableBalance(new BigDecimal("10000"))
    .blockedBalance(BigDecimal.ZERO)
    .totalBalance(new BigDecimal("10000"))
    .currency("XOF")
    .type("REGULAR")
    .status(BigDecimal.ZERO)
    .isPrimary(false)
    .dailySpent(BigDecimal.ZERO)
    .monthlySpent(BigDecimal.ZERO)
    .frozenAt(null)              // Active wallet
    .createdAt(LocalDateTime.now())
    .updatedAt(LocalDateTime.now())
    .build();
```

### Testing Frozen Wallet
```java
WalletDTO frozenWallet = WalletDTO.builder()
    .id(2L)
    .walletNumber("87654321")
    .availableBalance(new BigDecimal("5000"))
    .frozenAt(LocalDateTime.now())  // Frozen!
    .frozenBy(1L)
    .frozenReason("Suspicious activity")
    // ... other fields ...
    .build();

// This will return false
Boolean isActive = frozenWallet.getActive();
```

---

## Common Mistakes

### ❌ Mistake 1: Using single balance field
```java
wallet.balance = amount;  // ❌ WRONG - field doesn't exist
wallet.setBalance(amount);  // ✅ CORRECT
```

### ❌ Mistake 2: Checking active column
```java
if (wallet.isActive()) { }           // ❌ WRONG - method doesn't exist
if (wallet.getActive()) { }          // ✅ CORRECT
if (wallet.getFrozenAt() == null) {} // ✅ ALSO OK
```

### ❌ Mistake 3: Not mapping all fields
```java
// ❌ WRONG - Missing frozen status
Wallet wallet = Wallet.builder()
    .id(dto.getId())
    .availableBalance(dto.getAvailableBalance())
    .build();

// ✅ CORRECT - All fields mapped
Wallet wallet = Wallet.builder()
    .id(dto.getId())
    .walletNumber(dto.getWalletNumber())
    .availableBalance(dto.getAvailableBalance())
    .blockedBalance(dto.getBlockedBalance())
    .totalBalance(dto.getTotalBalance())
    .frozenAt(dto.getFrozenAt())
    // ... all other fields ...
    .build();
```

### ❌ Mistake 4: Not checking frozen status
```java
// ❌ WRONG - Doesn't check if wallet is frozen
walletService.debit(wallet, amount);

// ✅ CORRECT - Check frozen status first
if (wallet.getActive()) {
    walletService.debit(wallet, amount);
} else {
    throw new WalletFrozenException();
}
```

---

## Service Endpoints

### Wallet Service
```
POST /api/wallets                 → Create wallet
GET /api/wallets/user/{userId}    → Get wallet by user
GET /api/wallets/{walletNumber}   → Get wallet by number
PUT /api/wallets/{id}             → Update wallet
```

### Transaction Service
```
POST /api/transactions            → Create transaction
GET /api/transactions/{id}        → Get transaction
PUT /api/transactions/{id}/reverse → Reverse transaction
PUT /api/transactions/{id}/refund  → Refund transaction
```

---

## Database Schema Reference

```sql
CREATE TABLE wallets (
    id BIGINT PRIMARY KEY,
    wallet_number VARCHAR(8) UNIQUE,
    user_id BIGINT,
    available_balance NUMERIC(19,4),
    blocked_balance NUMERIC(19,4),
    total_balance NUMERIC(19,4),
    currency VARCHAR,
    type VARCHAR,
    status NUMERIC(19,4),
    is_primary BOOLEAN,
    daily_limit NUMERIC(19,4),
    daily_spent NUMERIC(19,4),
    monthly_limit NUMERIC(19,4),
    monthly_spent NUMERIC(19,4),
    frozen_at TIMESTAMP,
    frozen_by BIGINT,
    frozen_reason VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    closed_at TIMESTAMP,
    last_limit_reset TIMESTAMP,
    metadata VARCHAR,
    version BIGINT
);
```

---

## Performance Tips

1. **Use getBalance()** instead of direct field access
2. **Batch wallet lookups** when processing multiple transactions
3. **Index wallet_number** for fast lookups
4. **Index user_id** for user-to-wallet queries
5. **Index frozen_at** for filtering active wallets
6. **Cache wallet lookups** within transaction context

---

## Troubleshooting

### Problem: NullPointerException on balance
**Solution:** Check that wallet exists and balance fields are initialized
```java
if (wallet != null && wallet.getBalance() != null) {
    // Safe to use
}
```

### Problem: Frozen wallet not being rejected
**Solution:** Make sure getActive() is being called
```java
// Check before routing
if (!wallet.getActive()) {
    throw new WalletFrozenException();
}
```

### Problem: DTO fields missing in response
**Solution:** Make sure WalletService.toDTO() includes all fields
```java
// Verify all 23 fields are being mapped
```

---

## Resources

- [WALLET_COHERENCE_VERIFICATION.md](WALLET_COHERENCE_VERIFICATION.md) - Complete coherence report
- [WALLET_COHERENCE_FINAL.md](WALLET_COHERENCE_FINAL.md) - Final validation report
- [INTEGRATION_TESTING_GUIDE.md](INTEGRATION_TESTING_GUIDE.md) - Testing guide
- `common-library/src/main/java/com/zaphira/common/model/entities/Wallet.java` - Entity code
- `common-library/src/main/java/com/zaphira/common/dto/WalletDTO.java` - DTO code

---

## Quick Summary

| What You Want | Use This | Why |
|---------------|----------|-----|
| Get balance | `wallet.getBalance()` | Backward compatible |
| Set balance | `wallet.setBalance(amount)` | Updates availableBalance |
| Check frozen | `wallet.getActive()` | Returns frozenAt == null |
| Map DTO to entity | Use builder with all 23 fields | Ensures data integrity |
| Create wallet in tests | Use WalletDTO.builder() | Complete initialization |
| Validate before debit | Check `getActive()` first | Prevents frozen wallet usage |

---

**Last Updated:** 2025-12-15  
**Status:** ✅ READY FOR DEVELOPERS  
**Questions?** Check the detailed guides above
