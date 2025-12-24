# 📈 Tableau Détaillé - Format Simple

## Inventaire Rapide des Producteurs et Consommateurs Kafka

### 🎯 Vue d'Ensemble par Service

---

## **1. AUTH-SERVICE** (Port: 8081)

**Base de Données**: PostgreSQL auth_db (192.168.0.122:5432)

| Type | Fichier | Classe | Méthode | Topic | Event |
|---|---|---|---|---|---|
| 📤 PRODUCTEUR | `config/KafkaConfig.java` | - | - | `user-registered` | `UserRegisteredEvent` |
| 📥 CONSOMMATEUR | - | - | - | ❌ Aucun | - |

**Configuration Kafka**:
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=StringSerializer
spring.kafka.producer.value-serializer=JsonSerializer
spring.kafka.consumer.group-id=auth-service-group
```

---

## **2. USER-SERVICE** (Port: 8082)

**Base de Données**: PostgreSQL wallet_db (192.168.0.122:5432)

| Type | Fichier | Classe | Méthode | Topic | Event |
|---|---|---|---|---|---|
| 📤 PRODUCTEUR | `kafka/UserEventProducer.java` | UserEventProducer | publishUserCreatedEvent() | `user-created-topic` | `UserCreatedEvent` |
| 📥 CONSOMMATEUR | `kafka/WalletResponseListener.java` | WalletResponseListener | handleWalletCreatedEvent() | `wallet-created-topic` | `WalletCreatedEvent` |

**Topics Config**:
```properties
kafka.topics.user-created=user-created-topic
kafka.topics.wallet-created=wallet-created-topic
spring.kafka.bootstrap-servers=192.168.0.122:9092
```

**Pattern**: Synchrone avec CompletableFuture (attend réponse du wallet-service)

---

## **3. WALLET-SERVICE** (Port: 8086)

**Base de Données**: PostgreSQL wallet_db (localhost:5432)

| Type | Fichier | Classe | Méthode | Topic | Event |
|---|---|---|---|---|---|
| 📤 PRODUCTEUR | `consumer/UserEventConsumer.java` | UserEventConsumer | handleUserCreatedEvent() | `wallet-created-topic` | `WalletCreatedEvent` |
| 📥 CONSOMMATEUR #1 | `consumer/UserEventConsumer.java` | UserEventConsumer | handleUserCreatedEvent() | `user-created-topic` | `UserCreatedEvent` |
| 📥 CONSOMMATEUR #2 | `listener/UserEventListener.java` | UserEventListener | handleUserRegistered() | `user-registered` | `UserRegisteredEvent` |

**Topics Config**:
```yaml
kafka:
  topics:
    user-created: user-created-topic
    wallet-created: wallet-created-topic
  bootstrap-servers: localhost:9092
```

**Consumer Groups**:
- `wallet-service-group` (UserEventListener sur user-registered)
- `${spring.kafka.consumer.group-id}` (UserEventConsumer sur user-created)

---

## **4. TRANSACTION-SERVICE** (Port: 8083)

**Base de Données**: PostgreSQL wallet_db (192.168.0.122:5432)

| Type | Fichier | Classe | Méthode | Topic | Event |
|---|---|---|---|---|---|
| 📤 PRODUCTEUR | `kafka/producer/ValidationRequestProducer.java` | ValidationRequestProducer | publishValidationRequest() | `transaction.validation.request` | `TransactionValidationRequest` |
| 📥 CONSOMMATEUR | - | - | - | ❌ Aucun trouvé | - |
| 📤 PRODUCTEUR (Potentiel) | `config/KafkaConfig.java` | - | - | `transaction.created` | `TransactionCreatedEvent` |

**Observations**:
- Configuration pour `TransactionCreatedEvent` présente mais non utilisée en code
- `ValidationRequestProducer` envoie vers `transaction.validation.request`
- ❌ **Aucun consommateur trouvé** pour ces deux topics

---

## **5. NOTIFICATION-SERVICE** (Port: 8007)

**Base de Données**: PostgreSQL notification_db (localhost:5432)

| Type | Fichier | Classe | Méthode | Topic | Event |
|---|---|---|---|---|---|
| 📥 CONSOMMATEUR #1 | `listener/UserEventListener.java` | UserEventListener | handleUserRegistered() | `user-registered` | `UserRegisteredEvent` |
| 📥 CONSOMMATEUR #2 | `listener/TransactionEventListener.java` | TransactionEventListener | handleTransactionCreated() | `transaction-created` | `TransactionCreatedEvent` |
| 📥 CONSOMMATEUR #3 | `consumer/UserEventConsumer.java` | UserEventConsumer | consumeUserEvent() | `notification.user.events` | `NotificationEvent` |
| 📥 CONSOMMATEUR #4 | `consumer/TransactionEventConsumer.java` | TransactionEventConsumer | consumeTransactionEvent() | `notification.transaction.events` | `NotificationEvent` |
| 📥 CONSOMMATEUR #5 | `consumer/WalletEventConsumer.java` | WalletEventConsumer | consumeWalletEvent() | `notification.wallet.events` | `NotificationEvent` |
| 📤 PRODUCTEUR | - | - | - | ❌ Aucun | - |

**Topics Config** (application.yml):
```yaml
kafka-topics:
  notifications-in: notification-events
  notifications-out: notification-results
  user-registered: user.registered
  transaction-created: transaction.created
  wallet-updated: wallet.updated
```

**Consumer Groups**:
- `notification-service` (UserEventListener, TransactionEventListener)
- `notification-service-user-group` (UserEventConsumer)
- `notification-service-transaction-group` (TransactionEventConsumer)
- `notification-service-wallet-group` (WalletEventConsumer)

**Observations**:
- ✅ 5 consommateurs actifs
- ❌ Aucun producteur
- Topics `notification.*.events` ne semblent pas être produits

---

## **6. COMMON-LIBRARY**

**Rôle**: Librairie partagée (DTOs, Events, Exceptions)

| Aspect | Valeur |
|---|---|
| 📤 PRODUCTEUR | ❌ Non |
| 📥 CONSOMMATEUR | ❌ Non |
| **Events Définies** | `UserRegisteredEvent` |
| | `UserCreatedEvent` |
| | `WalletCreatedEvent` |
| | `TransactionCreatedEvent` |
| | `NotificationEvent` |
| **Dépendances Kafka** | ✅ spring-kafka (optional) |

---

---

# 📊 TABLEAU SYNTHÉTIQUE COMPACT

```
┌─────────────────────┬──────────────────┬──────────────────┬────────────────┐
│ MICROSERVICE        │ PRODUCTEUR(S)    │ CONSOMMATEUR(S)  │ BASE DONNÉES   │
├─────────────────────┼──────────────────┼──────────────────┼────────────────┤
│ auth-service        │ ✅ 1             │ ❌ 0             │ PostgreSQL     │
│ 8081                │ UserRegistered   │                  │ auth_db        │
│                     │ Topic: user-*    │                  │ 192.168.0.122  │
├─────────────────────┼──────────────────┼──────────────────┼────────────────┤
│ user-service        │ ✅ 1             │ ✅ 1             │ PostgreSQL     │
│ 8082                │ UserCreated      │ WalletCreated    │ wallet_db      │
│                     │ Topic: user-*    │ Topic: wallet-*  │ 192.168.0.122  │
├─────────────────────┼──────────────────┼──────────────────┼────────────────┤
│ wallet-service      │ ✅ 1             │ ✅ 2             │ PostgreSQL     │
│ 8086                │ WalletCreated    │ UserCreated      │ wallet_db      │
│                     │ Topic: wallet-*  │ UserRegistered   │ localhost      │
│                     │                  │ Topic: user-*    │                │
├─────────────────────┼──────────────────┼──────────────────┼────────────────┤
│ transaction-service │ ✅ 1             │ ❌ 0             │ PostgreSQL     │
│ 8083                │ Validation Req   │                  │ wallet_db      │
│                     │ Topic: trans-*   │                  │ 192.168.0.122  │
├─────────────────────┼──────────────────┼──────────────────┼────────────────┤
│ notification-svc    │ ❌ 0             │ ✅ 5             │ PostgreSQL     │
│ 8007                │                  │ UserRegistered   │ notification   │
│                     │                  │ TransactionCreated│ localhost      │
│                     │                  │ NotificationEvent│                │
│                     │                  │ (3 variantes)    │                │
├─────────────────────┼──────────────────┼──────────────────┼────────────────┤
│ TOTAUX              │ ✅ 4             │ ✅ 8             │ 5 instances    │
│                     │ producteurs      │ consommateurs    │ PostgreSQL     │
└─────────────────────┴──────────────────┴──────────────────┴────────────────┘
```

---

# 🔍 ANALYSE DES DÉPENDANCES KAFKA

## Versions et Dépendances

### Spring Kafka Version
```
Tous les services: spring-kafka (version du parent pom.xml)
Spring Boot Version: 3.3.4 (common-library)
```

### PostgreSQL JDBC Driver
```
auth-service: postgresql:42.7.3
transaction-service: postgresql:42.7.3
user-service: postgresql (parent pom)
wallet-service: postgresql (parent pom)
notification-service: postgresql (parent pom)
```

### Configuration Sérialisation
```
Producteur:
  - Key: org.apache.kafka.common.serialization.StringSerializer
  - Value: org.springframework.kafka.support.serializer.JsonSerializer

Consommateur:
  - Key: org.apache.kafka.common.serialization.StringDeserializer
  - Value: org.springframework.kafka.support.serializer.JsonDeserializer
  - Trusted Packages: "*"
```

---

# ⚡ MATRIX DE COMMUNICATION

```
USER-SERVICE (8082)
     ↓ publie UserCreatedEvent
     ↓ topic: user-created-topic
     └→ WALLET-SERVICE (8086) consomme
         ↓ publie WalletCreatedEvent
         ↓ topic: wallet-created-topic
         └→ USER-SERVICE reçoit response

AUTH-SERVICE (8081)
     ↓ publie UserRegisteredEvent  
     ↓ topic: user-registered
     ├→ WALLET-SERVICE (8086) consomme
     └→ NOTIFICATION-SERVICE (8007) consomme

TRANSACTION-SERVICE (8083)
     ↓ publie TransactionValidationRequest
     ↓ topic: transaction.validation.request
     └→ ❌ AUCUN CONSOMMATEUR TROUVÉ

NOTIFICATION-SERVICE (8007)
     ✅ Consomme 5 topics différents
     ❌ Ne produit aucun événement
```

---

**Fichier généré:** 21 Décembre 2025
