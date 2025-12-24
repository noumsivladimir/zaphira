# Quick Start: Test-Sync Profile Implementation Guide

## 🎯 Objective
Implement test-sync profile for your microservice to enable:
- Local testing without Kafka or Docker
- In-memory SQLite database
- Synchronous Feign-based service communication
- Fast test execution (< 3 minutes)

## ⏱️ Time Required
- Per service: **15-20 minutes**
- All services: **2 hours**

## 📋 Prerequisites

- [x] Common-library test infrastructure deployed (TestSQLiteDialect, Feign clients, SynchronousMessagingTestConfig)
- [x] Review `docs/test-architecture-sync.md` to understand the architecture
- [x] All microservices have integration tests

## 🚀 Step-by-Step Implementation

### Step 1: Add SQLite Dependency (2 minutes)

**File**: `pom.xml`

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.43.0.0</version>
    <scope>test</scope>
</dependency>
```

✅ **Verify**: `mvn clean compile -T 1C` succeeds

### Step 2: Create Configuration File (3 minutes)

**File**: `src/test/resources/application-test-sync.yml`

```yaml
spring:
  profiles: test-sync
  
  datasource:
    url: jdbc:sqlite::memory:
    driver-class-name: org.sqlite.JDBC
    hikari:
      maximum-pool-size: 5
  
  jpa:
    database-platform: com.zaphira.common.test.TestSQLiteDialect
    hibernate:
      ddl-auto: create-drop
  
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 1000
            readTimeout: 2000

test:
  sync:
    transaction-service:
      url: http://localhost:8081
    wallet-service:
      url: http://localhost:8082
    user-service:
      url: http://localhost:8083
    notification-service:
      url: http://localhost:8085

logging:
  level:
    root: WARN
    com.zaphira: DEBUG
```

✅ **Verify**: File exists and has correct indentation (YAML sensitive)

### Step 3: Create Synchronous Message Controller (5 minutes)

**File**: `src/test/java/com/zaphira/[service]/test/messaging/SynchronousMessageController.java`

**Template**:
```java
package com.zaphira.[service].test.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for synchronous message handling in test-sync mode.
 * Available only with @ActiveProfiles("test-sync")
 */
@RestController
@RequestMapping("/test-sync/messages")
@ConditionalOnProfile("test-sync")
public class SynchronousMessageController {
    
    private static final Logger logger = LoggerFactory.getLogger(SynchronousMessageController.class);

    /**
     * Handle [topic-name] messages
     */
    @PostMapping("/[topic-name]")
    public ResponseEntity<String> handle[TopicName](@RequestBody Object message) {
        try {
            logger.info("Received [topic-name] message: {}", message);
            // Route to actual consumer/handler
            return ResponseEntity.ok("Message processed");
        } catch (Exception e) {
            logger.error("Error processing message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{topic}")
    public ResponseEntity<String> handleGenericMessage(
            @PathVariable String topic,
            @RequestBody Object message) {
        try {
            logger.info("Received message for topic '{}': {}", topic, message);
            switch (topic) {
                case "[topic-1]": return handle[TopicName1](message);
                case "[topic-2]": return handle[TopicName2](message);
                default:
                    logger.warn("No handler for topic: {}", topic);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No handler for topic: " + topic);
            }
        } catch (Exception e) {
            logger.error("Error processing message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
}
```

**Example for Wallet Service** (handles UserEventConsumer):
```java
@PostMapping("/user-events")
public ResponseEntity<String> handleUserEvents(@RequestBody Object message) {
    // Routes to UserEventConsumer.consume()
}
```

✅ **Verify**: Class compiles, endpoints match Kafka topics

### Step 4: Update Integration Tests (5 minutes)

**File**: Each integration test class

Change:
```java
// BEFORE
@SpringBootTest
class MyIntegrationTest {
```

To:
```java
// AFTER
@SpringBootTest
@ActiveProfiles("test-sync")
class MyIntegrationTest {
```

✅ **Verify**: `mvn test -Dspring.profiles.active=test-sync` runs without "KafkaTemplate not found" errors

### Step 5: Verify Feign Clients (2 minutes)

**File**: Check `common-library/src/test/java/com/zaphira/common/test/feign/`

Feign clients should already exist:
- ✅ `TransactionSyncClient.java`
- ✅ `WalletSyncClient.java`
- ✅ `UserSyncClient.java`
- ✅ `NotificationSyncClient.java`

If not, add them to common-library's test package.

✅ **Verify**: All Feign clients available in your project

### Step 6: Test Execution (3 minutes)

```bash
# Single service
cd [your-service]
mvn clean test -Dspring.profiles.active=test-sync

# Specific test
mvn test -Dspring.profiles.active=test-sync -Dtest=YourIntegrationTest

# With custom URLs
mvn test -Dspring.profiles.active=test-sync \
         -Dtest.sync.wallet-service.url=http://localhost:8082
```

✅ **Verify**: All tests pass, execution < 3 minutes

## 🔍 Checklist

- [ ] Step 1: SQLite dependency added to pom.xml
- [ ] Step 2: application-test-sync.yml created (src/test/resources/)
- [ ] Step 3: SynchronousMessageController created (src/test/java/)
- [ ] Step 4: @ActiveProfiles("test-sync") added to tests
- [ ] Step 5: Feign clients available in common-library
- [ ] Step 6: Tests pass with mvn test -Dspring.profiles.active=test-sync
- [ ] Step 7: No production files modified (only src/test/**, docs/)
- [ ] Step 8: Execution time < 3 minutes
- [ ] Step 9: Database isolation verified (fresh DB per test)

## 🐛 Troubleshooting

### Error: "Cannot find symbol: class TestSQLiteDialect"
```
mvn clean install -DskipTests
```

### Error: "FeignClient not found"
- Check that common-library is a dependency
- Verify @EnableFeignClients is present or auto-configured

### Error: "@ActiveProfiles annotation not recognized"
- Add missing import: `import org.springframework.test.context.ActiveProfiles;`

### Error: "Kafka connection timeout"
- Ensure @ActiveProfiles("test-sync") is applied to test class
- Check application-test-sync.yml is in src/test/resources/

### Error: "SQLite dialect not available"
- Verify common-library test classes are in classpath
- Run: `mvn clean install -DskipTests` first

## 📚 Documentation

- **Full Architecture**: `docs/test-architecture-sync.md`
- **Commit/PR Template**: `COMMIT_PR_TEST_SYNC.md`
- **Status & Roadmap**: `TEST_SYNC_IMPLEMENTATION_STATUS.md`
- **Kafka Integration**: `KAFKA_INTEGRATION_GUIDE.md`

## ⏱️ Performance Targets

| Metric | Target | Your Service |
|--------|--------|--------------|
| Setup time | < 1 second | _______ |
| Test execution | < 3 minutes | _______ |
| Database ops | < 100ms | _______ |
| Feign calls | < 1 second | _______ |

## 🎓 Learning Resources

1. **Understand the mapping**: Review Kafka consumers/producers in your service
2. **Map to HTTP**: Each Kafka topic → POST /test-sync/messages/{topic}
3. **Route messages**: In SynchronousMessageController, route by topic name
4. **Test locally**: Run tests without Docker

## ✅ Success Criteria

- [x] SQLite in-memory database works
- [x] Feign clients can call other services (if running)
- [x] Tests use @ActiveProfiles("test-sync")
- [x] No external services required
- [x] Execution completes in < 3 minutes
- [x] All tests pass
- [x] Zero production code changes (only src/test/**)

## 🚢 Deployment

Once implementation complete for all services:

```bash
# CI/CD integration
mvn clean test -Dspring.profiles.active=test-sync

# Or with specific profile
mvn clean test -P test-sync
```

## 📞 Support

For questions or issues:
1. Check `docs/test-architecture-sync.md` for detailed architecture
2. Review transaction-service implementation as reference
3. Check COMMIT_PR_TEST_SYNC.md for code review points

---

**Estimated Time**: 15-20 minutes per service  
**Difficulty**: Easy (mostly copy-paste with customization)  
**Impact**: Significant (3x faster tests, no Docker needed)
