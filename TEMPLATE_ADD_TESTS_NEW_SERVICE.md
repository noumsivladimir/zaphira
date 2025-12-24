# 📋 Template - Ajouter les Tests à un Nouveau Service

Utilisez ce template pour rapidement ajouter les tests SQLite + Feign à n'importe quel microservice.

## 🔄 Processus

### 1. **Créer le répertoire de configuration test**

```bash
mkdir -p <service-name>/src/test/resources
mkdir -p <service-name>/src/test/java/com/zaphira/<service-name>/controller/test
mkdir -p <service-name>/src/test/java/com/zaphira/<service-name>/integration/test
```

### 2. **Ajouter application-test-sync.properties**

Copier et adapter le template pour votre service :

**Fichier:** `<service-name>/src/test/resources/application-test-sync.properties`

```properties
# ===========================================
# SQLite Configuration for Test Environment
# ===========================================

spring.application.name=<service-name>
server.port=<PORT>

# =========================================
# SQLite (H2) Database Configuration
# =========================================
spring.datasource.url=jdbc:h2:mem:<service-short-name>test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=PostgreSQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA / Hibernate Configuration
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.defer-datasource-initialization=true

# =========================================
# Kafka Configuration (Mock/Disabled)
# =========================================
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=<service-name>-group
spring.kafka.consumer.auto-offset-reset=earliest
# ... autres propriétés Kafka ...

# =========================================
# Feign Services URLs
# =========================================
# Remplacer localhost par les URLs réelles des services
# Exemple:
# other-service.url=http://localhost:8082

# =========================================
# Logging Configuration
# =========================================
logging.level.root=INFO
logging.level.com.zaphira=DEBUG
logging.level.org.springframework.test=DEBUG
logging.level.org.springframework.web=DEBUG

# =========================================
# Eureka (Disabled for Tests)
# =========================================
eureka.client.enabled=false

# =========================================
# Actuator
# =========================================
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
```

### 3. **Créer le contrôleur de test**

**Fichier:** `<service-name>/src/test/java/com/zaphira/<service-name>/controller/test/<ServiceName>TestMessageController.java`

```java
package com.zaphira.<service-name>.controller.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test Synchronous Message Controller for <service-name>
 * 
 * Simulates Kafka message consumption via Feign for testing purposes.
 * Activated only with "test-sync" profile.
 */
@RestController
@RequestMapping("/test-sync")
@ConditionalOnProfile("test-sync")
@RequiredArgsConstructor
@Slf4j
public class <ServiceName>TestMessageController {

    private final ObjectMapper objectMapper;

    @PostMapping("/messages/{topic}")
    public ResponseEntity<?> handleMessage(
            @PathVariable("topic") String topic,
            @RequestBody Object payload) {

        log.info("📨 Test Message received - Topic: {}, Payload: {}", topic, payload);

        try {
            switch (topic) {
                case "topic-name-1":
                    handleEvent1(payload);
                    break;
                case "topic-name-2":
                    handleEvent2(payload);
                    break;
                default:
                    log.warn("Unknown topic: {}", topic);
            }
            return ResponseEntity.ok().body("{\"status\":\"processed\", \"topic\":\"" + topic + "\"}");
        } catch (Exception e) {
            log.error("❌ Error processing message from topic: {}", topic, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleEvent1(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        log.debug("Processing event 1: {}", json);
        // TODO: Process event
    }

    private void handleEvent2(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        log.debug("Processing event 2: {}", json);
        // TODO: Process event
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("{\"status\":\"UP\", \"service\":\"<service-name>\"}");
    }
}
```

### 4. **Créer les tests E2E**

**Fichier:** `<service-name>/src/test/java/com/zaphira/<service-name>/integration/test/<ServiceName>E2ETest.java`

```java
package com.zaphira.<service-name>.integration.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Test for <ServiceName>
 * 
 * Uses "test-sync" profile (SQLite + Feign)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-sync")
@Slf4j
@DisplayName("<ServiceName> Integration Test (SQLite + Feign)")
public class <ServiceName>E2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        log.info("✅ <ServiceName> test environment ready");
    }

    @Test
    @DisplayName("Should successfully <describe-functionality>")
    void testSuccessfulFlow() throws Exception {
        // Given: Valid request
        
        // When: POST request
        mockMvc.perform(post("/api/<endpoint>")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                // Then: Expect success
                .andExpect(status().isOk());

        log.info("✅ Test passed");
    }

    @Test
    @DisplayName("Should validate input")
    void testValidation() throws Exception {
        // Test validation logic
        
        mockMvc.perform(post("/api/<endpoint>")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        log.info("✅ Validation test passed");
    }
}
```

### 5. **Ajouter les dépendances pom.xml**

Vérifier que votre `pom.xml` inclut :

```xml
<!-- H2 Database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- Test Dependencies -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

### 6. **Tester**

```bash
# Vérifier la structure
mvn test -Dspring.profiles.active=test-sync

# Ou avec Makefile
make test
```

---

## 📋 Checklist Complet

- [ ] Créer répertoires test/java et test/resources
- [ ] Copier et adapter application-test-sync.properties
- [ ] Créer TestMessageController
- [ ] Créer E2E Test classes
- [ ] Ajouter dépendances H2, Rest-Assured
- [ ] Exécuter `mvn test -Dspring.profiles.active=test-sync`
- [ ] Vérifier succès des tests
- [ ] Ajouter au CI/CD pipeline

---

## 🎯 Variables à Remplacer

| Variable | Exemple |
|----------|---------|
| `<service-name>` | transaction-service |
| `<service-short-name>` | transaction |
| `<ServiceName>` | TransactionService |
| `<PORT>` | 8083 |
| `<endpoint>` | /api/transactions |

---

## 📚 Fichiers de Référence

Vous pouvez utiliser comme référence :
- User-Service: [user-service/src/test/](user-service/src/test/)
- Wallet-Service: [wallet-service/src/test/](wallet-service/src/test/)

---

## ❓ FAQ

**Q: Puis-je utiliser ce template pour mes propres services ?**  
A: Oui, adaptez simplement le nom du service et les endpoints.

**Q: Comment tester la communication avec d'autres services ?**  
A: Utilisez `@MockBean` pour mocker les clients Feign, voir `SynchronousMicroserviceCommunicationTest.java`

**Q: Comment ajouter plus de tests ?**  
A: Créez d'autres méthodes `@Test` dans vos classes de test.

---

**Version:** 1.0.0  
**Date:** 2025-12-24
