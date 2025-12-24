# 🎨 DIAGRAMMES VISUELS - Architecture Kafka Zaphira

## 📊 Matrice de Communication Inter-Services

```
                    ┌─────────────────────────────────────────────────────┐
                    │           MICROSERVICES - KAFKA MATRIX               │
                    └─────────────────────────────────────────────────────┘

                    AUTH    USER    WALLET  TRANS   NOTIF   COMMON
                    ────    ────    ──────  ─────   ─────   ──────
           
  AUTH              ✓       →       →       ✗       →       ✓
  (8081)            │       │       │       │       │       │
                    │       │       ▼       │       ▼       │
  
  USER              ✗       ✓       ←→      ✗       ✓       ✓
  (8082)                    │       ▲       │       │       │
                            │       │       │       │       │
                            ▼       │       │       ▼       │
  
  WALLET            ←       ←→      ✓       ✗       ✗       ✓
  (8086)                    │       │       │               │
                            │       ▼       │               │
  
  TRANS             ✗       ✗       ✗       ✓       →       ✓
  (8083)                                    │       │       │
                                            ▼       ▼       │
  
  NOTIF             ✗       ✗       ✗       ✗       ✓       ✓
  (8007)                                                    │
  
  COMMON            ✗       ✗       ✗       ✗       ✗       ✓
                    
  Légende:
  ✓   = Cible (DB, Dépendances)
  →   = Producteur (publie événements)
  ←   = Consommateur (consomme événements)
  ←→  = Bi-directionnel (producteur ET consommateur)
  ✗   = Pas de relation
```

---

## 🔄 Flux d'Événements Détaillé

### Vue 1: Enregistrement Utilisateur (USER-REGISTERED)

```
┌──────────────────────────────────────────────────────────────────┐
│                    FLUX D'ENREGISTREMENT UTILISATEUR              │
└──────────────────────────────────────────────────────────────────┘

 1. AUTH-SERVICE
    ├─ Endpoint: POST /api/auth/login
    ├─ Action: Authentification utilisateur
    ├─ Output: UserRegisteredEvent
    └─ Publish: Topic "user-registered"
                │
                ├─────────────────────────────┬─────────────────────────────┐
                │                             │                             │
                ▼                             ▼                             ▼
 2. WALLET-SERVICE              3. NOTIFICATION-SERVICE      (Other Subscribers)
    ├─ Consumer Group: wallet-service-group
    ├─ Class: UserEventListener
    ├─ Method: handleUserRegistered()
    ├─ Action: Idempotent wallet creation
    │   (Check if exists → Create if not)
    └─ DB: PostgreSQL localhost:5432
                                  ├─ Consumer Group: notification-service
                                  ├─ Class: UserEventListener
                                  ├─ Method: handleUserRegistered()
                                  ├─ Action: Send Welcome SMS
                                  ├─ Service: SmsService
                                  └─ DB: PostgreSQL localhost:5432
```

### Vue 2: Création Compte Utilisateur (USER-CREATED ↔ WALLET-CREATED)

```
┌──────────────────────────────────────────────────────────────────┐
│              FLUX DE CRÉATION DE COMPTE & SYNCHRONISATION         │
└──────────────────────────────────────────────────────────────────┘

 1. USER-SERVICE (Initiateur)
    ├─ Endpoint: POST /api/users/register
    ├─ Action: Créer utilisateur
    ├─ Class: UserEventProducer
    ├─ Method: publishUserCreatedEvent()
    ├─ Output: UserCreatedEvent
    ├─ Publish: Topic "user-created-topic"
    ├─ Await Response: CompletableFuture<WalletCreatedEvent>
    ├─ Timeout: 30 seconds
    └─ On Success: Retour confirmation avec walletId
                │
                ▼
 2. WALLET-SERVICE (Réactif)
    ├─ Consumer Group: wallet-service-group
    ├─ Class: UserEventConsumer
    ├─ Method: handleUserCreatedEvent()
    ├─ Action: 
    │  1. Create wallet
    │  2. Publish response
    ├─ Output: WalletCreatedEvent
    ├─ Publish: Topic "wallet-created-topic"
    └─ Correlation ID: event.getCorrelationId()
                │
                ▼
 3. USER-SERVICE (Reçoit Réponse)
    ├─ Consumer Group: user-service-group
    ├─ Class: WalletResponseListener
    ├─ Method: handleWalletCreatedEvent()
    ├─ Action: Complete CompletableFuture
    └─ Result: Retour succès utilisateur avec portefeuille
    
 Pattern: Synchronous over Asynchronous (orchestration)
 Latency: P99 < 30 secondes
 Failure Handling: Timeout → Exception → User notification
```

### Vue 3: Validation Transaction (ORPHANED)

```
┌──────────────────────────────────────────────────────────────────┐
│            FLUX DE VALIDATION TRANSACTION (🔴 INCOMPLET)          │
└──────────────────────────────────────────────────────────────────┘

 1. TRANSACTION-SERVICE (Producteur)
    ├─ Class: ValidationRequestProducer
    ├─ Method: publishValidationRequest()
    ├─ Output: TransactionValidationRequest
    ├─ Publish: Topic "transaction.validation.request"
    ├─ Idempotence: Enabled (correlationId)
    ├─ Retries: 3
    ├─ Acks: all
    └─ Status: ✓ FONCTIONNEL
                │
                ▼
 ❓ UNKNOWN CONSUMER
    ├─ Expected Topic: "transaction.validation.request"
    ├─ Status: 🔴 NOT FOUND
    ├─ Messages Accumulation: YES
    ├─ Data Loss: NO (persisted in Kafka)
    ├─ Impact: Transaction validation blocked
    └─ Action Required: IMPLEMENT CONSUMER
```

### Vue 4: Notifications (MIXED)

```
┌──────────────────────────────────────────────────────────────────┐
│                    FLUX DE NOTIFICATIONS (PARTIEL)                │
└──────────────────────────────────────────────────────────────────┘

 A. ACTIFS IDENTIFIÉS:

 User-Registered → NOTIFICATION-SERVICE
                   └─ SMS Welcome Message ✓
                   
 Transaction-Created → NOTIFICATION-SERVICE
                       └─ Confirmation Email/SMS ✓

 B. TOPICS SANS PRODUCTEUR IDENTIFIÉ:

 notification.user.events → NOTIFICATION-SERVICE
                           ├─ Expected: User domain events
                           ├─ Producer: ❓ UNKNOWN
                           └─ Status: 🟡 ORPHANED INPUT

 notification.transaction.events → NOTIFICATION-SERVICE
                                  ├─ Expected: Transaction events
                                  ├─ Producer: ❓ UNKNOWN
                                  └─ Status: 🟡 ORPHANED INPUT

 notification.wallet.events → NOTIFICATION-SERVICE
                             ├─ Expected: Wallet events
                             ├─ Producer: ❓ UNKNOWN
                             └─ Status: 🟡 ORPHANED INPUT

 wallet.updated → NOTIFICATION-SERVICE
                 ├─ Expected: Wallet update events
                 ├─ Producer: ❓ UNKNOWN
                 └─ Status: 🟡 ORPHANED INPUT
```

---

## 🗄️ Architecture Base de Données

```
┌──────────────────────────────────────────────────────────────────┐
│                   ARCHITECTURE BASE DE DONNÉES                    │
└──────────────────────────────────────────────────────────────────┘

PostgreSQL Instance 1: 192.168.0.122:5432
├─ Database: auth_db
│  ├─ Owner: auth-service (8081)
│  ├─ DDL Strategy: create-drop ⚠️
│  ├─ Recreated: On every startup
│  └─ Tables: User, Token, RefreshToken, ActivityLog
│
├─ Database: wallet_db
│  ├─ Owners: user-service (8082), transaction-service (8083)
│  ├─ DDL Strategy: update ✓
│  ├─ Schema Evolution: Progressive
│  └─ Tables: User, Wallet, Transaction, Account
│
└─ Status: PRIMARY

PostgreSQL Instance 2: localhost:5432
├─ Database: wallet_db
│  ├─ Owner: wallet-service (8086)
│  ├─ DDL Strategy: create-drop ⚠️
│  ├─ Recreated: On every startup
│  ├─ Tables: Wallet, Balance, Transaction
│  └─ Issue: 🔴 DIFFERENT INSTANCE (data fragmentation)
│
├─ Database: notification_db
│  ├─ Owner: notification-service (8007)
│  ├─ DDL Strategy: validate
│  ├─ Migrations: Flyway
│  ├─ Schema Requirement: Must exist before startup
│  └─ Tables: Notification, NotificationTemplate, NotificationLog
│
└─ Status: SECONDARY (development only?)

⚠️ PROBLEM: wallet_db exists in TWO instances!
   - Instance 1: User-Service, Transaction-Service write here
   - Instance 2: Wallet-Service write here
   - RESULT: Data fragmentation - services don't share wallet data!
```

---

## 📡 Topologie Kafka

```
┌──────────────────────────────────────────────────────────────────┐
│                      KAFKA BROKER TOPOLOGY                        │
└──────────────────────────────────────────────────────────────────┘

Bootstrap Servers:
├─ Development: localhost:9092 (auth-service, wallet-service)
└─ Network: 192.168.0.122:9092 (user-service, transaction-service)

Topics & Partitions:
│
├─ user-registered (ACTIVE)
│  ├─ Partitions: ? (default)
│  ├─ Replication Factor: ? (default)
│  ├─ Producer: auth-service
│  ├─ Consumers: wallet-service, notification-service
│  └─ Status: ✅ HEALTHY
│
├─ user-created-topic (ACTIVE)
│  ├─ Producer: user-service
│  ├─ Consumers: wallet-service
│  └─ Status: ✅ HEALTHY
│
├─ wallet-created-topic (ACTIVE)
│  ├─ Producer: wallet-service
│  ├─ Consumers: user-service
│  └─ Status: ✅ HEALTHY
│
├─ transaction.validation.request (ORPHANED)
│  ├─ Producer: transaction-service ✓
│  ├─ Consumers: NONE 🔴
│  ├─ Message Accumulation: YES
│  └─ Status: 🔴 CRITICAL
│
├─ transaction-created (NO PRODUCER)
│  ├─ Producer: ? UNKNOWN 🟡
│  ├─ Consumers: notification-service
│  └─ Status: 🟡 INCOMPLETE
│
├─ notification.user.events (NO PRODUCER)
│  ├─ Producer: ? UNKNOWN 🟡
│  ├─ Consumers: notification-service
│  └─ Status: 🟡 INCOMPLETE
│
├─ notification.transaction.events (NO PRODUCER)
│  ├─ Producer: ? UNKNOWN 🟡
│  ├─ Consumers: notification-service
│  └─ Status: 🟡 INCOMPLETE
│
├─ notification.wallet.events (NO PRODUCER)
│  ├─ Producer: ? UNKNOWN 🟡
│  ├─ Consumers: notification-service
│  └─ Status: 🟡 INCOMPLETE
│
└─ wallet.updated (NO PRODUCER)
   ├─ Producer: ? UNKNOWN 🟡
   ├─ Consumers: notification-service
   └─ Status: 🟡 INCOMPLETE

Consumer Groups:
│
├─ auth-service-group
│  └─ Members: 0 (auth-service doesn't consume)
│
├─ user-service-group
│  └─ Members: WalletResponseListener
│
├─ wallet-service-group
│  └─ Members: UserEventListener
│
├─ transaction-service-validation-result
│  └─ Members: 0 (no consumers)
│
├─ notification-service
│  └─ Members: UserEventListener, TransactionEventListener
│
├─ notification-service-user-group
│  └─ Members: UserEventConsumer
│
├─ notification-service-transaction-group
│  └─ Members: TransactionEventConsumer
│
└─ notification-service-wallet-group
   └─ Members: WalletEventConsumer
```

---

## 🔍 Sérialisation & Configuration

```
┌──────────────────────────────────────────────────────────────────┐
│              KAFKA SERIALIZATION & SECURITY CONFIG                │
└──────────────────────────────────────────────────────────────────┘

PRODUCER CONFIGURATION:
│
├─ Key Serializer: org.apache.kafka.common.serialization.StringSerializer
├─ Value Serializer: org.springframework.kafka.support.serializer.JsonSerializer
├─ Acks: all (wait for leader + all in-sync replicas) ✓
├─ Retries: 3
├─ Enable Idempotence: true ✓
├─ Compression Type: (default = none)
└─ Batch Settings:
   ├─ Linger MS: (default)
   └─ Batch Size: (default)

CONSUMER CONFIGURATION:
│
├─ Key Deserializer: org.apache.kafka.common.serialization.StringDeserializer
├─ Value Deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
├─ Auto Offset Reset: earliest
├─ Enable Auto Commit: false (manual acknowledged where specified) ✓
├─ Trusted Packages: "*" (SECURITY RISK - all classes accepted) 🔴
├─ Group ID: (specific per service)
├─ Max Poll Records: (default or configured)
├─ Session Timeout MS: 30000 (30 seconds)
└─ Isolation Level: (default)

SECURITY:
│
├─ SSL/TLS: NOT CONFIGURED (development only) ⚠️
├─ SASL Authentication: NOT CONFIGURED ⚠️
├─ Zookeeper Security: NOT CONFIGURED ⚠️
├─ ACLs: NOT CONFIGURED ⚠️
│
└─ Recommendation: Implement for production use

SERIALIZATION RISK:
│
├─ Issue: Trusted Packages = "*"
├─ Risk: Arbitrary class deserialization (RCE)
├─ Recommendation: Whitelist specific event classes only
├─ Fix: 
│  props.put(JsonDeserializer.TRUSTED_PACKAGES, 
│            "com.zaphira.common.event");
└─ Priority: MEDIUM (only internal traffic)
```

---

## 📈 Statistiques Visuelles

```
┌──────────────────────────────────────────────────────────────────┐
│                      MICROSERVICES DISTRIBUTION                   │
└──────────────────────────────────────────────────────────────────┘

Producers by Service:
│
├─ auth-service:           ████░░░░░░ 25% (1/4)
├─ user-service:           ████░░░░░░ 25% (1/4)
├─ wallet-service:         ████░░░░░░ 25% (1/4)
├─ transaction-service:    ████░░░░░░ 25% (1/4)
├─ notification-service:   ░░░░░░░░░░ 0% (0/4)
│
└─ Total: 4 Producers


Consumers by Service:
│
├─ auth-service:           ░░░░░░░░░░ 0% (0/8)
├─ user-service:           ██░░░░░░░░ 12.5% (1/8)
├─ wallet-service:         ████░░░░░░ 25% (2/8)
├─ transaction-service:    ░░░░░░░░░░ 0% (0/8)
├─ notification-service:   ██████████ 62.5% (5/8)
│
└─ Total: 8 Consumers


Topic Health:
│
├─ Healthy (Both Sides):   ████████░░ 33% (3/9)
├─ Orphaned (No Consumer): ██░░░░░░░░ 22% (2/9)
├─ Missing Producer:       ████░░░░░░ 45% (4/9)
│
└─ Overall Health: 37% ⚠️


Database Instances:
│
├─ 192.168.0.122:5432:    ██████░░░░ 67% (2/3 databases)
├─ localhost:5432:         ████░░░░░░ 67% (2/3 databases)
│
└─ Fragmentation Risk: HIGH 🔴
```

---

**Légende des Symboles**

| Symbole | Signification | Couleur |
|---------|---------------|---------|
| ✓ | OK / Fonctionnel | 🟢 Green |
| ⚠️ | Avertissement / À surveiller | 🟡 Yellow |
| ❌ | Non trouvé / Manquant | 🔴 Red |
| ← | Consomme | Blue |
| → | Produit | Green |
| ↔ | Bidirectionnel | Purple |

---

*Généré: 21 Décembre 2025*
