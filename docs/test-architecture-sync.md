# Test Architecture: Synchronous Feign + SQLite (test-sync Profile)

## Overview

This document describes the **test-sync** profile architecture that enables:
- **Synchronous messaging**: Kafka replaced by Feign REST calls for microservice communication
- **In-memory SQLite**: No PostgreSQL or external databases required
- **Local CI**: Tests run on a single machine without Docker or Testcontainers
- **Fast execution**: Target execution time: **2-3 minutes** for full test suite

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Test-Sync Mode                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Transaction Service      Wallet Service    User Service    │
│  ┌──────────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ SQLite (memory)  │  │SQLite(memory)│  │SQLite(memory)│  │
│  └────────┬─────────┘  └──────┬───────┘  └──────┬───────┘  │
│           │                   │                 │           │
│  ┌────────▼─────────────────────────────────────▼────────┐  │
│  │           Feign Synchronous Messaging                 │  │
│  │  (REST calls, no Kafka, no message queue)            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │    SynchronousMessagingTestConfig                    │  │
│  │    (Mock KafkaTemplate, Feign clients setup)         │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Kafka → Feign Mapping

| Component | Kafka (Prod) | Test-Sync Mode | Endpoint |
|-----------|------|------|----------|
| **ValidationRequestProducer** | KafkaTemplate.send("validation-requests", event) | TransactionSyncClient.sendMessage("validation-requests", event) | POST /test-sync/messages/validation-requests |
| **ValidationResultConsumer** | @KafkaListener(topic="validation-results") | SynchronousMessageController.handleValidationResults() | POST /test-sync/messages/validation-results |
| **UserEventConsumer** (Wallet) | @KafkaListener(topic="user-events") | SynchronousMessageController.handleUserEvents() | POST /test-sync/messages/user-events |
| **TransactionEventConsumer** (Notification) | @KafkaListener(topic="transaction-events") | SynchronousMessageController.handleTransactionEvents() | POST /test-sync/messages/transaction-events |
| **WalletEventConsumer** (Notification) | @KafkaListener(topic="wallet-events") | SynchronousMessageController.handleWalletEvents() | POST /test-sync/messages/wallet-events |

## Configuration Files

### 1. `application-test-sync.yml` (Per Service)

Located in `src/test/resources/` for each microservice.

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
```

### 2. `SynchronousMessagingTestConfig.java`

Located in `common-library/src/test/java/com/zaphira/common/test/messaging/`.

Provides:
- Mock `KafkaTemplate` bean (no-op, prevents errors)
- Feign client registration
- Profile-specific bean initialization

```java
@Configuration
@ConditionalOnProfile("test-sync")
public class SynchronousMessagingTestConfig {
    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        // Returns mock template - Kafka not used in test-sync
        return new KafkaTemplate<>(...);
    }
}
```

### 3. `SynchronousMessageController.java` (Per Service)

Located in `src/test/java/com/zaphira/[service]/test/messaging/`.

Implements HTTP endpoints that map Kafka topics:

```java
@RestController
@RequestMapping("/test-sync/messages")
@ConditionalOnProfile("test-sync")
public class SynchronousMessageController {
    
    @PostMapping("/validation-results")
    public ResponseEntity<String> handleValidationResults(@RequestBody Object message) {
        // Route to ValidationResultConsumer.consume(message)
    }
    
    @PostMapping("/{topic}")
    public ResponseEntity<String> handleGenericMessage(
            @PathVariable String topic,
            @RequestBody Object message) {
        // Route by topic name
    }
}
```

### 4. Feign Clients (Test-Only)

Located in `common-library/src/test/java/com/zaphira/common/test/feign/`.

```java
@FeignClient(
    name = "transaction-service-sync-test",
    url = "${test.sync.transaction-service.url:http://localhost:8081}",
    fallback = TransactionSyncClient.Fallback.class
)
public interface TransactionSyncClient {
    @PostMapping("/test-sync/messages/{topic}")
    void sendMessage(@PathVariable String topic, @RequestBody Object payload);
}
```

## Dependencies Required

### POM Configuration

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.43.0.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <!-- Already present in prod, reused for tests -->
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Running Tests

### Local Test Execution (test-sync Profile)

```bash
# Single service
cd transaction-service
mvn clean test -Dspring.profiles.active=test-sync

# All services (assumes all are in same project)
mvn clean test -Dspring.profiles.active=test-sync

# Specific test class
mvn test -Dspring.profiles.active=test-sync \
         -Dtest=CrossMicroserviceIntegrationTest
```

### With Custom Feign URLs

```bash
mvn test -Dspring.profiles.active=test-sync \
         -Dtest.sync.transaction-service.url=http://localhost:8081 \
         -Dtest.sync.wallet-service.url=http://localhost:8082
```

## Integration Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-sync")
class CrossMicroserviceIntegrationTest {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private WalletSyncClient walletSyncClient;
    
    @Test
    void testSynchronousFeign() {
        // 1. Create transaction locally with SQLite
        Transaction tx = transactionRepository.save(...);
        
        // 2. Call wallet service synchronously via Feign
        walletSyncClient.sendMessage("wallet-updated", event);
        
        // 3. Assert results
        assertThat(tx).isNotNull();
    }
}
```

## Limitations & Known Issues

| Limitation | Impact | Workaround |
|-----------|--------|-----------|
| **Synchronous only** | Cannot test async race conditions, message ordering issues | Use Testcontainers + Kafka for e2e tests (separate profile) |
| **No message persistence** | Cannot test message replay, offset management | Messages are request-response only |
| **SQLite schema differences** | Some PostgreSQL features (JSON, arrays) not supported | Keep SQLite tests simple, use e2e tests for complex queries |
| **Network timeouts** | No real network delays, test environment unrealistic | Acceptable for unit/integration tests, e2e tests should use real network |
| **Cross-service startup** | All services must be running and healthy | CI/CD scripts must start services in dependency order |

## When to Use test-sync vs. e2e (Testcontainers)

### Use test-sync for:
- Unit tests of business logic
- Repository/JPA integration tests
- Simple cross-service flow tests (request-response)
- Local development & CI without Docker
- Fast feedback loop (2-3 minutes)
- Test isolation (in-memory DB, no state)

### Use e2e (Testcontainers) for:
- Complex async message flows
- Message ordering & delivery guarantees
- Kafka topic retention & replay scenarios
- Real PostgreSQL schema & features
- Production-like network conditions
- Load testing

## Checklist for Implementation

- [ ] Add `sqlite-jdbc` test dependency to all service pom.xml files
- [ ] Create `application-test-sync.yml` in each service's `src/test/resources/`
- [ ] Create `SynchronousMessageController` in each service's `src/test/java/`
- [ ] Create Feign client interfaces in `common-library/src/test/java/com/zaphira/common/test/feign/`
- [ ] Create `SynchronousMessagingTestConfig` in `common-library/src/test/java/`
- [ ] Create test dialect: `TestSQLiteDialect.java`
- [ ] Update integration tests to use `@ActiveProfiles("test-sync")`
- [ ] Document cross-service test flows
- [ ] Create CI/CD script to run tests with test-sync profile
- [ ] Verify no production files modified (only src/test/**

)
- [ ] Test execution time: target 2-3 minutes for full suite

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Test (test-sync)

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Run tests (test-sync profile)
        run: |
          mvn clean test \
            -Dspring.profiles.active=test-sync \
            -DskipIntegrationTests=false
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

## Troubleshooting

### "Cannot connect to Feign client"

**Cause**: Service endpoint not reachable
**Fix**: Ensure all services are running or start them in order:
```bash
# Terminal 1
cd transaction-service
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test-sync"

# Terminal 2
cd wallet-service
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test-sync"

# Terminal 3 - Run tests
cd transaction-service
mvn test -Dspring.profiles.active=test-sync
```

### "SQLite dialect not found"

**Cause**: `TestSQLiteDialect` not in classpath
**Fix**: Ensure `common-library` is a dependency and compiled first:
```bash
mvn clean install -DskipTests
mvn test
```

### "KafkaTemplate bean not found"

**Cause**: `SynchronousMessagingTestConfig` not loaded
**Fix**: Verify `@ComponentScan` includes test packages or use explicit `@Import`:
```java
@SpringBootTest
@ActiveProfiles("test-sync")
@Import(SynchronousMessagingTestConfig.class)
class TestClass { ... }
```

## References

- [Hibernate SQLite Dialect Docs](https://hibernate.org/orm/releases/)
- [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign)
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc)
- Zaphira Kafka Integration: `KAFKA_INTEGRATION_GUIDE.md`
- Zaphira SQLite Integration: `SQLITE_INTEGRATION_SUMMARY.md`

---

**Last Updated**: December 21, 2025
**Status**: Implementation in progress
**Revision**: 1.0
