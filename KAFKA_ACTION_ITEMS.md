# ⚡ ACTION ITEMS - Priorisation & Quick Wins

**Généré:** 21 Décembre 2025

---

## 🎯 TRIAGE PAR PRIORITÉ

### 🔴 P0 - CRITICAL (Cette semaine)

#### ❌ [P0-1] Topic Orphan: `transaction.validation.request`
**Impact:** Transactions ne peuvent pas être validées  
**Effort:** 2-3 heures

```bash
# DIAGNOSTIQUE
kafkacat -b localhost:9092 -t transaction.validation.request \
         -f '%p:%o:%k\t%s\n' | head -20

# SOLUTION OPTIONS:
# Option A: Trouver le consommateur existant mais non trouvé dans le scan
#   → Chercher dans: payment-service, validation-service, middleware
#   → Grep: "transaction.validation.request" --include="*.java"
#
# Option B: Implémenter le consommateur (ex: dans transaction-service lui-même)
#   → Classe: TransactionValidationResultConsumer
#   → Method: handleValidationResult(@KafkaListener)
#   → Action: Update transaction status in DB
```

**Commande Recherche:**
```bash
find . -name "*.java" -type f | xargs grep -l "transaction.validation.request"
find . -name "*.properties" -type f | xargs grep -l "transaction.validation"
find . -name "*.yml" -type f | xargs grep -l "transaction.validation"
```

**Checklist:**
- [ ] Identifier consommateur manquant OU
- [ ] Créer nouveau consommateur pour ce topic
- [ ] Tester avec message de test
- [ ] Valider que messages sont consommés

---

#### ❌ [P0-2] Incohérence PostgreSQL: wallet_db sur 2 hosts
**Impact:** Données fragmentées - services n'accèdent pas même base  
**Effort:** 30 minutes configuration + validation

```yaml
# WALLET-SERVICE: src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wallet_db  # ❌ ACTUELLEMENT
    # CHANGER À:
    url: jdbc:postgresql://192.168.0.122:5432/wallet_db  # ✅ CIBLE

# Après changement:
# 1. Arrêter wallet-service
# 2. Vérifier données existantes sur localhost
# 3. Migrer données si nécessaire vers 192.168.0.122
# 4. Redémarrer service
# 5. Tester accès aux wallets
```

**Validation Post-Changement:**
```sql
-- Sur 192.168.0.122:
SELECT COUNT(*) FROM wallet_db.wallets;

-- Vérifier que wallet-service voit les données
curl http://localhost:8086/api/wallets

-- Vérifier synchronisation user-service ↔ wallet-service
curl http://localhost:8082/api/users/123/wallet
```

**Checklist:**
- [ ] Backup wallet_db sur localhost
- [ ] Actualiser URL dans application.yml
- [ ] Tester connexion à nouvelle BD
- [ ] Vérifier accès aux données existantes
- [ ] Valider Kafka event flow user-service → wallet-service

---

#### ❌ [P0-3] Problème Database Incohérence: DDL Strategies
**Impact:** Data loss on restart (create-drop), difficile debug  
**Effort:** 1-2 heures

```properties
# auth-service/application.properties
spring.jpa.hibernate.ddl-auto=create-drop  # ❌ DANGEREUX
# CHANGER À:
spring.jpa.hibernate.ddl-auto=update        # ✅ SAFE

# wallet-service/application.yml
spring.jpa.hibernate.ddl-auto: create-drop  # ❌ DANGEREUX  
# CHANGER À:
spring.jpa.hibernate.ddl-auto: update       # ✅ SAFE
```

**Validation:**
```bash
# Vérifier changements:
grep -r "ddl-auto" . --include="*.properties" --include="*.yml"

# Attendu:
# - auth-service: update
# - user-service: update
# - wallet-service: update
# - transaction-service: update
# - notification-service: validate ✓ (c'est OK, utilise Flyway)
```

**Checklist:**
- [ ] Mettre à jour auth-service
- [ ] Mettre à jour wallet-service
- [ ] Backup BD avant changement
- [ ] Tester redémarrage du service
- [ ] Vérifier données toujours présentes après restart

---

### 🟡 P1 - HIGH (Prochaines 2 semaines)

#### ❌ [P1-1] Topics sans Producteur Identifié
**Topics Affectés:** transaction-created, notification.*.events (4 au total)  
**Impact:** Unclear data flow, potential dead code  
**Effort:** 3-4 heures

```bash
# SEARCH: Localiser producteurs manquants
find . -name "*.java" -type f | xargs grep -l "transaction-created" | grep -i producer
find . -name "*.java" -type f | xargs grep -l "notification.user.events" | grep -i producer

# OPTION 1: Topics ne sont pas utilisés → NETTOYER
#   → Supprimer listeners pour ces topics dans notification-service
#   → Supprimer configuration topics inutilisés
#
# OPTION 2: Producteurs existent ailleurs → AJOUTER À SCAN
#   → Chercher dans payment-service, analytics-service, etc
#   → Remettre à jour cet inventaire
#
# OPTION 3: À implémenter → PLANIFIER FEATURE
#   → Exemple: wallet-service produit notification.wallet.events
#     quand solde change
```

**Pour notification-service:**
```java
// OPTION: Implémenter producteur
@Service
public class NotificationEventProducer {
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    
    public void publishNotificationSuccess(NotificationEvent event) {
        kafkaTemplate.send("notification.results", 
                          event.getId(), 
                          event);
    }
}
```

**Checklist:**
- [ ] Décider: Nettoyer, Chercher, ou Implémenter
- [ ] Si Implémenter: Créer producteur avec tests
- [ ] Si Nettoyer: Supprimer listeners inutilisés
- [ ] Mettre à jour cet inventaire

---

#### ⚠️ [P1-2] Notification-Service n'a pas de Producteur
**Impact:** Service est "sink" - accepte événements mais ne peut pas en émettre  
**Effort:** 2-3 heures si nécessaire

```java
// Si besoin de feedback sur notifications envoyées:
@Service
public class NotificationResultProducer {
    private final KafkaTemplate<String, NotificationResultEvent> kafkaTemplate;
    
    public void publishNotificationResult(
        String notificationId, 
        NotificationStatus status,
        String error) {
        
        NotificationResultEvent event = 
            NotificationResultEvent.builder()
                .notificationId(notificationId)
                .status(status)
                .errorMessage(error)
                .timestamp(Instant.now())
                .build();
                
        kafkaTemplate.send("notification.results", 
                          notificationId, 
                          event);
    }
}
```

**Cas d'Usage:**
- Notification envoyée avec succès → Autres services en notification
- Notification échouée après retries → Alerter administrateur
- Notification livrée → Mettre à jour stats

**Checklist:**
- [ ] Évaluer besoin: Y a-t-il un cas métier?
- [ ] Si OUI: Implémenter producteur + topic notification.results
- [ ] Si NON: Documenter que c'est intentionnel
- [ ] Ajouter tests

---

#### 🔒 [P1-3] Sécurité Kafka: Trusted Packages = "*"
**Impact:** RCE potentiel - peut désérialiser n'importe quelle classe  
**Effort:** 30 minutes

```java
// ACTUELLEMENT (DANGEREUX):
props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

// SÉCURISÉ:
props.put(JsonDeserializer.TRUSTED_PACKAGES, 
    "com.zaphira.common.event," +
    "com.zaphira.notification.event");

// À FAIRE POUR CHAQUE SERVICE:
// auth-service:
props.put(JsonDeserializer.TRUSTED_PACKAGES, 
    "com.zaphira.common.event");

// user-service:
props.put(JsonDeserializer.TRUSTED_PACKAGES, 
    "com.zaphira.common.event");

// wallet-service:
props.put(JsonDeserializer.TRUSTED_PACKAGES, 
    "com.zaphira.common.event");

// notification-service:
props.put(JsonDeserializer.TRUSTED_PACKAGES, 
    "com.zaphira.common.event," +
    "com.zaphira.notification.event");

// transaction-service:
props.put(JsonDeserializer.TRUSTED_PACKAGES, 
    "com.zaphira.common.event," +
    "com.zaphira.transaction.kafka.event");
```

**Validation:**
```bash
# Vérifier tous les KafkaConfig:
grep -r "TRUSTED_PACKAGES" . --include="*.java"

# Attendu: Whitelist spécifique pour chaque service
```

**Checklist:**
- [ ] Mettre à jour tous les KafkaConfig.java
- [ ] Tester que serialization/deserialization fonctionne
- [ ] Valider que messages connus sont acceptés
- [ ] Valider que classes inconnues sont rejetées

---

### 🟢 P2 - MEDIUM (Prochains 3 mois)

#### 📊 [P2-1] Monitoring Kafka & Metrics
**Impact:** Visibility sur santé système  
**Effort:** 8-12 heures setup + 4 heures by service

```yaml
# Ajouter à parent pom.xml ou chaque service:
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

# Ajouter Micrometer Kafka:
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

# application.properties:
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

**Dashboard Grafana à créer:**
- [ ] Kafka broker health
- [ ] Topic lag par consumer group
- [ ] Message throughput par topic
- [ ] Consumer lag alerts
- [ ] Producer error rate
- [ ] Deserialization failures

**Checklist:**
- [ ] Ajouter actuator & prometheus à tous les services
- [ ] Configurer Prometheus scrape configs
- [ ] Créer Grafana dashboards
- [ ] Configurer alertes PagerDuty/Slack

---

#### 🔐 [P2-2] Sécurité Kafka: SSL/SASL
**Impact:** Communication chiffrée & authentifiée  
**Effort:** 6-8 heures

```properties
# Dans application.properties:
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=\
    org.apache.kafka.common.security.plain.PlainLoginModule required \
    username="zaphira-app" \
    password="${KAFKA_PASSWORD}";

spring.kafka.ssl.trust-store-location=classpath:kafka-truststore.jks
spring.kafka.ssl.trust-store-password=${KAFKA_TRUSTSTORE_PASSWORD}
```

**Checklist:**
- [ ] Générer certificates/keystores Kafka broker
- [ ] Configurer broker SASL/SSL
- [ ] Créer credentials pour application
- [ ] Tester connexion chiffrée
- [ ] Documenter rotation credentials

---

#### 📚 [P2-3] Documentation: Event Contracts (AsyncAPI)
**Impact:** Clarté pour futurs développeurs  
**Effort:** 4-6 heures

```yaml
# asyncapi.yaml
asyncapi: '3.0.0'
info:
  title: Zaphira Event API
  version: '1.0.0'

channels:
  user-registered:
    address: 'user-registered'
    messages:
      userRegistered:
        contentType: application/json
        payload:
          $ref: '#/components/schemas/UserRegisteredEvent'

components:
  schemas:
    UserRegisteredEvent:
      type: object
      properties:
        userId:
          type: string
        firstName:
          type: string
        lastName:
          type: string
        phoneNumber:
          type: string
        correlationId:
          type: string
      required:
        - userId
        - phoneNumber
        - correlationId
```

**Checklist:**
- [ ] Créer asyncapi.yaml pour tous les topics
- [ ] Documenter payload de chaque événement
- [ ] Documenter garanties (at-least-once, etc)
- [ ] Documenter retry policies
- [ ] Publier sur docs.zaphira.io

---

### 🟢 P3 - NICE-TO-HAVE (À long terme)

#### 🔗 [P3-1] Distributed Tracing (Jaeger/Zipkin)
**Impact:** Debug problèmes multi-services  
**Effort:** 10-12 heures

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

---

#### 🧪 [P3-2] Contract Testing (Pact)
**Impact:** Garantir compatibilité événements entre services  
**Effort:** 12-16 heures

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "WalletService", port = "9090")
public class WalletCreatedEventPactTest {
    
    @Pact(consumer = "UserService")
    public V4Pact walletCreatedEvent(PactBuilder builder) {
        return builder
            .expectStringRegexInteraction(
                "wallet created event",
                Pattern.compile("wallet-created-topic"),
                ".*")
            .build();
    }
}
```

---

## 📋 SUMMARY TABLE

| ID | Issue | P | Effort | Owner | Status |
|----|-------|---|--------|-------|--------|
| P0-1 | Consumer manquant: transaction.validation.request | 🔴 | 2-3h | ❓ TBD | ⏳ TODO |
| P0-2 | wallet_db sur 2 hosts différents | 🔴 | 30m | ❓ TBD | ⏳ TODO |
| P0-3 | DDL Strategy: create-drop dangereux | 🔴 | 1-2h | ❓ TBD | ⏳ TODO |
| P1-1 | Producteurs manquants pour 4 topics | 🟡 | 3-4h | ❓ TBD | ⏳ TODO |
| P1-2 | Notification-service sans producteur | 🟡 | 2-3h | ❓ TBD | ⏳ TODO |
| P1-3 | Sécurité: TRUSTED_PACKAGES = "*" | 🟡 | 30m | ❓ TBD | ⏳ TODO |
| P2-1 | Monitoring Kafka & Metrics | 🟢 | 8-12h | ❓ TBD | ⏳ PLAN |
| P2-2 | Sécurité: SSL/SASL Kafka | 🟢 | 6-8h | ❓ TBD | ⏳ PLAN |
| P2-3 | Documentation: Event Contracts | 🟢 | 4-6h | ❓ TBD | ⏳ PLAN |

---

## ⏱️ PLANNING ESTIMATE

### Semaine 1 (P0s)
```
Monday:   P0-1 (Consumer manquant) + P0-2 (DB hosts) = 3 heures
Tuesday:  P0-3 (DDL Strategy) + Testing = 2 heures
Wednesday: Validation + Fix de regression = 2 heures
Thursday: Reserves pour issues découvertes = 2 heures
Friday:   Retrospective + Documentation = 1 heure
──────────────────────────────────────────────────────
TOTAL SEMAINE 1: 10 heures
```

### Semaine 2-3 (P1s)
```
P1-1: Topics manquants (3-4 heures)
P1-2: Notification producer (2-3 heures)
P1-3: Sécurité TRUSTED_PACKAGES (30 minutes)
──────────────────────────────────────────────────────
TOTAL: 6-8 heures
```

### Semaine 4+ (P2s & P3s)
```
P2-1: Monitoring Kafka (12-16 heures)
P2-2: SSL/SASL Kafka (8 heures)
P2-3: AsyncAPI Documentation (6 heures)
P3-*: Distributed Tracing, Contract Testing (backlog)
──────────────────────────────────────────────────────
TOTAL: 26-30 heures
```

---

## 📞 ESCALATION

### Si P0-1 n'est pas résolu dans 24h
→ Chercher dans les services suivants (candidates):
- payment-service
- validation-service
- middleware/gateway service
- Vérifier anciennes branches git

### Si incapacité à trouver consumer P0-1
→ Options:
1. Mettre topic en DLQ temporaire
2. Implémenter dummy consumer avec logging
3. Bloquer publication jusqu'à résolution

### Si problème P0-2 (DB hosts) non solvable
→ Chercher:
1. Raison de 2 instances (data isolation intentionnelle?)
2. Historique git pour savoir quand changement
3. Validation données côté wallet-service vs user-service

---

**Généré:** 21 Décembre 2025  
**Priorité:** IMMÉDIATE pour P0s
