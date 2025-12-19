# **PRIORITY 1 - VERIFICATION COMMANDS**

**Purpose:** Verify implementation and compilation status  
**Commands:** Maven/Git commands to validate changes

---

## **🔍 VERIFICATION CHECKLIST**

### **Step 1: Check Compilation**
```bash
# Compile the transaction-service
cd transaction-service
mvn clean compile

# Expected: BUILD SUCCESS with 0 errors
```

### **Step 2: Verify New Files Exist**
```bash
# Check if all routing files exist
ls -la src/main/java/com/zaphira/transaction/model/enums/PaymentMethod.java
ls -la src/main/java/com/zaphira/transaction/service/routing/

# Expected: All files present
```

### **Step 3: Check Transaction Model Changes**
```bash
# Verify @Version field exists
grep -n "@Version" src/main/java/com/zaphira/transaction/model/Transaction.java

# Verify authorizationLevel field exists
grep -n "authorizationLevel" src/main/java/com/zaphira/transaction/model/Transaction.java

# Verify paymentMethod field exists
grep -n "paymentMethod" src/main/java/com/zaphira/transaction/model/Transaction.java

# Expected: All three fields present
```

### **Step 4: Verify PaymentMethod Enum**
```bash
# Check PaymentMethod enum values
grep -E "^[[:space:]]*[A-Z_]+\(" src/main/java/com/zaphira/transaction/model/enums/PaymentMethod.java | wc -l

# Expected: 14 payment methods
```

### **Step 5: Run Tests**
```bash
# Run all tests to ensure no regressions
mvn test

# Expected: All tests pass
```

### **Step 6: Check Git Status**
```bash
# See what files changed
git status

# See detailed changes
git diff src/main/java/com/zaphira/transaction/model/Transaction.java

# Expected: New files + 1 modified file
```

---

## **📋 FILE VERIFICATION SCRIPT**

### **For Bash/Linux/Mac:**
```bash
#!/bin/bash

echo "=== Priority 1 Implementation Verification ==="
echo ""

echo "1. Checking PaymentMethod enum..."
if [ -f "transaction-service/src/main/java/com/zaphira/transaction/model/enums/PaymentMethod.java" ]; then
    echo "   ✓ PaymentMethod.java exists"
    count=$(grep -c "^[[:space:]]*[A-Z_]*," "transaction-service/src/main/java/com/zaphira/transaction/model/enums/PaymentMethod.java")
    echo "   → Found $count payment methods"
else
    echo "   ✗ PaymentMethod.java NOT FOUND"
fi
echo ""

echo "2. Checking Routing Strategy interface..."
if [ -f "transaction-service/src/main/java/com/zaphira/transaction/service/routing/RoutingStrategy.java" ]; then
    echo "   ✓ RoutingStrategy.java exists"
else
    echo "   ✗ RoutingStrategy.java NOT FOUND"
fi
echo ""

echo "3. Checking Transaction modifications..."
if grep -q "@Version" "transaction-service/src/main/java/com/zaphira/transaction/model/Transaction.java"; then
    echo "   ✓ @Version field found"
else
    echo "   ✗ @Version field NOT FOUND"
fi

if grep -q "authorizationLevel" "transaction-service/src/main/java/com/zaphira/transaction/model/Transaction.java"; then
    echo "   ✓ authorizationLevel field found"
else
    echo "   ✗ authorizationLevel field NOT FOUND"
fi

if grep -q "paymentMethod" "transaction-service/src/main/java/com/zaphira/transaction/model/Transaction.java"; then
    echo "   ✓ paymentMethod field found"
else
    echo "   ✗ paymentMethod field NOT FOUND"
fi
echo ""

echo "4. Checking routing strategy implementations..."
strategies=("WalletRoutingStrategy" "BankRoutingStrategy" "CardRoutingStrategy" "TransactionRoutingService" "RoutingException" "RoutingValidationResult")
for strategy in "${strategies[@]}"; do
    if [ -f "transaction-service/src/main/java/com/zaphira/transaction/service/routing/${strategy}.java" ]; then
        echo "   ✓ ${strategy}.java exists"
    else
        echo "   ✗ ${strategy}.java NOT FOUND"
    fi
done
echo ""

echo "=== Verification Complete ==="
```

### **For Windows (PowerShell):**
```powershell
# Navigate to project root
cd transaction-service

# Check PaymentMethod enum exists
if (Test-Path "src/main/java/com/zaphira/transaction/model/enums/PaymentMethod.java") {
    Write-Host "✓ PaymentMethod.java exists" -ForegroundColor Green
} else {
    Write-Host "✗ PaymentMethod.java NOT FOUND" -ForegroundColor Red
}

# Check Transaction modifications
$content = Get-Content "src/main/java/com/zaphira/transaction/model/Transaction.java"

if ($content -match "@Version") {
    Write-Host "✓ @Version field found" -ForegroundColor Green
} else {
    Write-Host "✗ @Version field NOT FOUND" -ForegroundColor Red
}

# Check all routing files
$routingFiles = @(
    "WalletRoutingStrategy",
    "BankRoutingStrategy", 
    "CardRoutingStrategy",
    "TransactionRoutingService",
    "RoutingStrategy",
    "RoutingException",
    "RoutingValidationResult"
)

foreach ($file in $routingFiles) {
    $path = "src/main/java/com/zaphira/transaction/service/routing/${file}.java"
    if (Test-Path $path) {
        Write-Host "✓ ${file}.java exists" -ForegroundColor Green
    } else {
        Write-Host "✗ ${file}.java NOT FOUND" -ForegroundColor Red
    }
}
```

---

## **🔨 BUILD VERIFICATION**

### **Full Build**
```bash
# From project root
mvn clean install -DskipTests

# Expected: BUILD SUCCESS
```

### **Quick Compile Check**
```bash
# Just compile, no tests
mvn clean compile

# Expected: 0 errors
```

### **Specific Module**
```bash
# Compile transaction-service only
mvn clean compile -pl transaction-service

# Expected: 0 errors
```

---

## **📊 EXPECTED RESULTS**

### **File Count**
```
New Files:    9
Modified Files: 1
Total: 10 files
```

### **Compilation**
```
Errors:    0 ✅
Warnings:  Minimal
Status:    BUILD SUCCESS
```

### **Test Status**
```
All tests should pass
No regressions introduced
```

### **Git Status**
```
New files:
  - src/main/java/com/zaphira/transaction/model/enums/PaymentMethod.java
  - src/main/java/com/zaphira/transaction/service/routing/RoutingStrategy.java
  - src/main/java/com/zaphira/transaction/service/routing/WalletRoutingStrategy.java
  - src/main/java/com/zaphira/transaction/service/routing/BankRoutingStrategy.java
  - src/main/java/com/zaphira/transaction/service/routing/CardRoutingStrategy.java
  - src/main/java/com/zaphira/transaction/service/routing/TransactionRoutingService.java
  - src/main/java/com/zaphira/transaction/service/routing/RoutingValidationResult.java
  - src/main/java/com/zaphira/transaction/service/routing/RoutingException.java

Modified files:
  - src/main/java/com/zaphira/transaction/model/Transaction.java (3 additions)
```

---

## **✅ SIGN-OFF VERIFICATION**

Before declaring implementation complete, verify:

```
□ All 9 new files exist
□ Transaction.java has 3 new fields
□ mvn clean compile returns BUILD SUCCESS with 0 errors
□ All tests pass (mvn test)
□ No import errors in IDE
□ PaymentMethod enum has 14 values
□ RoutingStrategy interface compiles
□ All 3 routing strategies compile
□ TransactionRoutingService compiles
□ Git shows correct files modified
```

---

## **🚀 QUICK VERIFICATION CHECKLIST**

**For Quick Verification (2 minutes):**

1. Open TransactionService.java
   - ✓ No red squiggly lines in IDE
   
2. Check PaymentMethod.java
   - ✓ File exists in enums folder
   - ✓ 14 payment method constants visible
   
3. Check routing folder
   - ✓ 8 files present
   - ✓ No compilation errors
   
4. Run compile
   ```bash
   mvn clean compile
   ```
   - ✓ BUILD SUCCESS

---

## **📞 TROUBLESHOOTING**

### **If Compilation Fails**

**Check 1: Missing Imports**
```bash
# Search for missing imports
grep -r "import.*PaymentMethod" transaction-service/src
```

**Check 2: Syntax Errors**
```bash
# Check for Java syntax
javac -cp . src/main/java/com/zaphira/transaction/model/Transaction.java
```

**Check 3: Spring Boot Version**
- Ensure Spring Boot 3.x
- Check pom.xml spring-boot.version

**Check 4: Lombok**
- Verify @Builder.Default works
- Update Lombok if needed

### **If Tests Fail**

**Check 1: Recompile**
```bash
mvn clean compile test
```

**Check 2: Check Dependencies**
```bash
mvn dependency:tree
```

**Check 3: Run Specific Test**
```bash
mvn test -Dtest=TransactionTest
```

---

## **📝 VERIFICATION LOG TEMPLATE**

```
PRIORITY 1 VERIFICATION LOG
Date: ______________
Verifier: ______________

Item 1: Optimistic Locking
  □ @Version field added to Transaction
  □ Compilation successful
  □ Tests pass
  Status: ___________

Item 2: Authorization Level
  □ authorizationLevel field added
  □ Compilation successful
  □ Tests pass
  Status: ___________

Item 3: Payment Routing
  □ PaymentMethod enum created (14 values)
  □ RoutingStrategy interface created
  □ 3 strategy implementations created
  □ TransactionRoutingService created
  □ Compilation successful (0 errors)
  □ Tests pass
  Status: ___________

OVERALL STATUS: ___________

Notes:
_________________________________
_________________________________
```

---

**When all checks pass: ✅ IMPLEMENTATION VERIFIED & READY FOR DEPLOYMENT**

