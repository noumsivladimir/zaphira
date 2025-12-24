# 🔧 KAFKA DIAGNOSTIC COMMANDS

**Zaphira Kafka Analysis - Verification & Diagnostic Commands**  
**Date:** 21 Décembre 2025

---

## 🚀 QUICK VERIFICATION COMMANDS

Run these commands from your workspace root to verify the analysis:

### 1. **Verify All Kafka Files Found**

```bash
# Find all Kafka-related Java files
find . -name "*.java" -type f | xargs grep -l "KafkaTemplate\|@KafkaListener" | sort

# Expected: Should find 15+ files (producers, consumers, configs)
```

### 2. **Verify Microservices Locations**

```bash
# Check all pom.xml files
find . -name "pom.xml" -path "*/*/pom.xml" -type f | head -11

# Expected output:
# ./pom.xml (parent)
# ./auth/pom.xml
# ./user-service/pom.xml
# ./wallet-service/pom.xml
# ./transaction-service/pom.xml
# ./notification-service/pom.xml
# ./common-library/pom.xml
# (+ api-gateway, config-server, service-registry)
```

### 3. **Count Producers vs Consumers**

```bash
# Find all KafkaTemplate declarations (Producers)
find . -name "*.java" -type f | xargs grep -c "KafkaTemplate" | grep -v ":0" | wc -l

# Find all @KafkaListener annotations (Consumers)
find . -name "*.java" -type f | xargs grep -c "@KafkaListener" | grep -v ":0" | wc -l

# Expected:
# Producers: 4 files/locations
# Consumers: 8+ files/locations
```

---

## 📊 KAFKA CONFIGURATION VERIFICATION

### 4. **Find All Kafka Bootstrap Servers**

```bash
# Find all bootstrap server configurations
grep -r "bootstrap-servers\|bootstrap.servers" . \
    --include="*.properties" \
    --include="*.yml" \
    --include="*.yaml" 2>/dev/null

# Expected:
# localhost:9092 (auth-service, wallet-service)
# 192.168.0.122:9092 (user-service, transaction-service)
```

### 5. **Find All Kafka Topics**

```bash
# Find topic definitions in properties files
grep -r "kafka.topics\|spring.kafka" . \
    --include="*.properties" \
    --include="*.yml" \
    --include="*.yaml" 2>/dev/null | grep -i "topic"

# Expected to find:
# user-registered, user-created-topic, wallet-created-topic
# transaction.validation.request
# notification.*, wallet.updated
```

### 6. **Verify Database Configurations**

```bash
# Find all PostgreSQL connections
grep -r "jdbc:postgresql" . \
    --include="*.properties" \
    --include="*.yml" \
    --include="*.java" 2>/dev/null

# Expected:
# localhost:5432/wallet_db (wallet-service) ⚠️
# 192.168.0.122:5432/auth_db (auth-service)
# 192.168.0.122:5432/wallet_db (user-service, transaction-service)
# localhost:5432/notification_db (notification-service)
```

### 7. **Check DDL Strategies**

```bash
# Find all hibernate DDL configurations
grep -r "ddl-auto\|hibernate.ddl-auto" . \
    --include="*.properties" \
    --include="*.yml" \
    --include="*.yaml" 2>/dev/null

# Expected:
# auth-service: create-drop ⚠️ (should be update)
# user-service: update ✓
# wallet-service: create-drop ⚠️ (should be update)
# transaction-service: update ✓
# notification-service: validate ✓
```

---

## 🔐 SECURITY VERIFICATION

### 8. **Check TRUSTED_PACKAGES Security**

```bash
# Find TRUSTED_PACKAGES configuration
grep -r "TRUSTED_PACKAGES\|trusted.packages" . \
    --include="*.java" \
    --include="*.properties" \
    --include="*.yml" 2>/dev/null

# Look for:
grep -r "TRUSTED_PACKAGES.*\*" . --include="*.java"

# Expected: Will show services using "*" (security risk)
# FIX: Should be whitelist like "com.zaphira.common.event"
```

### 9. **Check Kafka Security Protocol**

```bash
# Look for SSL/SASL configuration
grep -r "security.protocol\|sasl.mechanism" . \
    --include="*.properties" \
    --include="*.yml" \
    --include="*.java" 2>/dev/null

# Expected: EMPTY (means no SSL/SASL configured - development only)
# Should be: SASL_SSL for production
```

---

## 📁 SPECIFIC SERVICE VERIFICATION

### 10. **Auth Service - Verify Producer**

```bash
# Check auth-service KafkaConfig
grep -A 5 "kafkaTemplate" auth/src/main/java/com/zaphira/auth/config/KafkaConfig.java

# Should show: KafkaTemplate<String, UserRegisteredEvent>
```

### 11. **User Service - Verify Producer & Consumer**

```bash
# Producer
grep -A 10 "publishUserCreatedEvent" \
    user-service/src/main/java/com/zaphira/service_user/kafka/UserEventProducer.java

# Consumer
grep -A 10 "handleWalletCreatedEvent" \
    user-service/src/main/java/com/zaphira/service_user/kafka/WalletResponseListener.java

# Expected: Both should show topic names and event types
```

### 12. **Wallet Service - Verify Producer & Consumers**

```bash
# Producer
grep -A 10 "walletCreatedTopic" \
    wallet-service/src/main/java/com/zaphira/wallet/consumer/UserEventConsumer.java

# Consumer 1
grep -A 10 "handleUserCreatedEvent" \
    wallet-service/src/main/java/com/zaphira/wallet/consumer/UserEventConsumer.java

# Consumer 2
grep -A 10 "handleUserRegistered" \
    wallet-service/src/main/java/com/zaphira/wallet/listener/UserEventListener.java
```

### 13. **Transaction Service - Find Orphaned Producer**

```bash
# Check for validation producer
find transaction-service -name "*.java" -type f | \
    xargs grep -l "ValidationRequestProducer\|transaction.validation"

# Should find:
# - ValidationRequestProducer.java (producer found ✓)
# - No consumers for this topic (problem 🔴)
```

### 14. **Notification Service - Check Consumers**

```bash
# Find all @KafkaListener in notification-service
grep -r "@KafkaListener" \
    notification-service/src/main/java/ \
    -A 2 -B 2

# Should show 5 listeners:
# 1. UserEventListener (user-registered)
# 2. TransactionEventListener (transaction-created)
# 3. UserEventConsumer (notification.user.events)
# 4. TransactionEventConsumer (notification.transaction.events)
# 5. WalletEventConsumer (notification.wallet.events)

# Problem: No producer defined ⚠️
```

---

## 🔍 INVESTIGATION COMMANDS

### 15. **Find Unknown Producers**

```bash
# Search for "transaction-created" producer
find . -name "*.java" -type f | \
    xargs grep -l "transaction.created\|transaction-created" | \
    grep -i producer

# Search for "notification.user.events" producer
find . -name "*.java" -type f | \
    xargs grep -l "notification.user.events" | \
    grep -i producer

# If empty = Producer not found (problem to investigate)
```

### 16. **Find transaction.validation.request Consumer**

```bash
# Search for consumer of validation requests
find . -name "*.java" -type f | \
    xargs grep -l "transaction.validation.request" | \
    grep -i consumer

# Search in all topics/listeners
grep -r "transaction.validation" . \
    --include="*.java" \
    --include="*.properties"

# If only ValidationRequestProducer = Consumer missing (problem 🔴)
```

### 17. **Verify Database Host Mismatch**

```bash
# Check wallet_db hosts
grep -r "wallet_db" . \
    --include="*.properties" \
    --include="*.yml" \
    --include="*.yaml" 2>/dev/null

# Expected output will show:
# localhost:5432/wallet_db (wallet-service) ⚠️
# 192.168.0.122:5432/wallet_db (user-service, transaction-service)
# These are DIFFERENT instances!
```

---

## 🧪 KAFKA BROKER TESTING

### 18. **Test Kafka Connection (if kafkacat installed)**

```bash
# List topics
kafkacat -b localhost:9092 -L 2>/dev/null | grep topics

# OR using Docker
docker exec zaphira-kafka kafka-topics --list --bootstrap-server localhost:9092

# Expected topics:
# user-registered
# user-created-topic
# wallet-created-topic
# transaction.validation.request
# notification.* (if they exist)
```

### 19. **Check Topic Consumer Groups**

```bash
# List consumer groups
kafkacat -b localhost:9092 -L 2>/dev/null | grep "group"

# OR using Docker
docker exec zaphira-kafka kafka-consumer-groups \
    --list --bootstrap-server localhost:9092

# Expected groups:
# auth-service-group
# user-service-group
# wallet-service-group
# notification-service
# notification-service-user-group
# notification-service-transaction-group
# notification-service-wallet-group
# transaction-service-validation-result
```

### 20. **Check Topic Lag**

```bash
# For each consumer group, check lag
docker exec zaphira-kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 \
    --group wallet-service-group \
    --describe

# Look for:
# - LAG column should be close to 0 for healthy consumers
# - Large LAG = messages not being processed 🔴
```

---

## 📊 BUILD & COMPILATION VERIFICATION

### 21. **Verify All Services Compile**

```bash
# Run Maven clean compile for each service
cd auth && mvn clean compile
cd ../user-service && mvn clean compile
cd ../wallet-service && mvn clean compile
cd ../transaction-service && mvn clean compile
cd ../notification-service && mvn clean compile
cd ../common-library && mvn clean compile

# Expected: All should show "BUILD SUCCESS"
```

### 22. **Check Dependencies**

```bash
# List all Kafka dependencies
find . -name "pom.xml" -type f | \
    xargs grep -l "spring-kafka" | \
    xargs grep -A 2 "spring-kafka"

# Expected: spring-kafka in most services
```

---

## 🎯 QUICK TEST SUITE

### Run All Verifications

```bash
#!/bin/bash
echo "=== KAFKA ANALYSIS VERIFICATION ==="
echo ""

echo "1. Counting Kafka files..."
find . -name "*.java" | xargs grep -l "KafkaTemplate\|@KafkaListener" | wc -l
echo "   Expected: 15+ files"
echo ""

echo "2. Finding microservices..."
find . -name "pom.xml" -path "*/*/pom.xml" | wc -l
echo "   Expected: 9 pom.xml files"
echo ""

echo "3. Finding topics..."
grep -r "kafka.topics\|topics:" . --include="*.yml" --include="*.properties" 2>/dev/null | \
    grep -o "[a-z\-\.]*topic[a-z\-\.]*" | sort -u | wc -l
echo "   Expected: 9+ topics"
echo ""

echo "4. Finding PostgreSQL databases..."
grep -r "jdbc:postgresql" . --include="*.properties" --include="*.yml" 2>/dev/null | \
    grep -o "[a-z_]*_db" | sort -u | wc -l
echo "   Expected: 4-5 databases"
echo ""

echo "5. Finding security issues..."
grep -r "TRUSTED_PACKAGES.*\*" . --include="*.java" 2>/dev/null | wc -l
echo "   Expected: 0 (all should be whitelisted)"
echo ""

echo "=== END VERIFICATION ==="
```

---

## 🐛 TROUBLESHOOTING

### If commands don't work:

```bash
# Make sure you're in the workspace root
pwd
# Should show: /path/to/zaphira-15-12-2025

# If grep fails, try with absolute paths
find /full/path/to/zaphira -name "*.java" | xargs grep "KafkaTemplate"

# On Windows (PowerShell):
Get-ChildItem -Path "." -Filter "*.java" -Recurse | \
    Select-String "KafkaTemplate" | wc -l
```

---

## 📝 COMMAND REFERENCE

| Command | Purpose |
|---------|---------|
| `grep -r "pattern" .` | Search all files for pattern |
| `find . -name "*.java"` | Find all Java files |
| `xargs grep -l "pattern"` | Find files containing pattern |
| `grep -c "pattern"` | Count lines with pattern |
| `grep -A 5 "pattern"` | Show pattern + 5 lines after |
| `grep -B 2 "pattern"` | Show pattern + 2 lines before |

---

## ✅ VERIFICATION CHECKLIST

After running these commands, verify:

- [ ] Found 4 microservices with KafkaTemplate (producers)
- [ ] Found 8+ locations with @KafkaListener (consumers)
- [ ] Found 9 Kafka topics in configuration
- [ ] Found 4 PostgreSQL databases
- [ ] Found 2 bootstrap server addresses
- [ ] Found DDL strategies (some using create-drop)
- [ ] Found TRUSTED_PACKAGES with "*" (security issue)
- [ ] Found transaction.validation.request producer (no consumer)
- [ ] Found notification-service with 5 consumers (no producer)
- [ ] Verified all services compile successfully

**If all checkmarks:** ✅ Analysis is validated!

---

## 🚀 NEXT STEPS

After verification, implement:

1. Fix P0 issues (2-3 hours)
   - Find transaction.validation consumer
   - Align wallet_db hosts
   - Update DDL strategies

2. Fix P1 issues (3-4 hours)
   - Find missing producers
   - Add notification producer
   - Secure TRUSTED_PACKAGES

3. Plan P2 improvements (backlog)
   - Monitoring
   - SSL/SASL
   - Documentation

---

**Generated:** 21 Décembre 2025  
**Version:** 1.0  
**Part of:** Zaphira Kafka Analysis Package
