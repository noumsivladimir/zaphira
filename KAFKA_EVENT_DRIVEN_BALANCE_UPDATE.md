# Kafka Event-Driven Balance Update - Implementation Guide

## 📋 Vue d'ensemble

Ce document décrit l'implémentation d'un système event-driven avec Kafka pour mettre à jour le solde utilisateur lorsqu'une transaction passe au statut `COMPLETED`.

### Architecture

```
Transaction-Service                      User-Service
      │                                        │
      │  ① Transaction COMPLETED              │
      │  (status = COMPLETED)                 │
      │                                        │
      ├─→ PublishEvent                        │
      │   TransactionCompletedEvent          │
      │                                        │
      ├─→ Kafka Topic: transaction-completed  │
      │                                        │
      │                                        ← ② Consumer consomme l'événement
      │                                        ← ③ Verify idempotence
      │                                        ← ④ Update balance
      │                                        ← ⑤ Record processing
      │                                        ← ⑥ Acknowledge message
```

## 🔧 Composants Implémentés

### 1. **Événement Kafka** - `TransactionCompletedEvent`

**Fichier:** `common-library/src/main/java/com/zaphira/common/event/TransactionCompletedEvent.java`

**Contenu:**
- `transactionId` - ID unique de la transaction
- `reference` - UUID de la transaction
- `initiatorUserId` - ID de l'utilisateur initiateur (sender)
- `senderWalletNumber` - Numéro de portefeuille de l'expéditeur
- `receiverWalletNumber` - Numéro de portefeuille du destinataire
- `amount` - Montant de la transaction
- `currency` - Devise (XOF, USD, etc.)
- `feeAmount` - Frais appliqués
- `status` - Statut final (COMPLETED, FAILED, etc.)
- `completedAt` - Timestamp de complétion
- `eventTimestamp` - Timestamp de création de l'événement

### 2. **Producer Kafka** - `TransactionEventPublisher`

**Fichier:** `transaction-service/src/main/java/com/zaphira/transaction/event/TransactionEventPublisher.java`

**Méthodes:**
- `publishTransactionCreated(TransactionCreatedEvent)` - Publie l'événement de création
- `publishTransactionCompleted(TransactionCompletedEvent)` - ✅ NOUVELLE - Publie l'événement de complétion

### 3. **Configuration Kafka - Transaction Service**

**Fichier:** `transaction-service/src/main/java/com/zaphira/transaction/config/KafkaConfig.java`

**Ajouts:**
- `completedTransactionProducerFactory()` - Factory pour TransactionCompletedEvent
- `completedTransactionKafkaTemplate()` - Template pour publier TransactionCompletedEvent

### 4. **Consumer Kafka** - `TransactionEventListener`

**Fichier:** `user-service/src/main/java/com/zaphira/service_user/kafka/listener/TransactionEventListener.java`

**Responsabilités:**
- Écoute le topic `transaction-completed`
- Reçoit les événements `TransactionCompletedEvent`
- Vérifie l'idempotence
- Déclenche la mise à jour du solde
- Acknowledges le message après succès

**Méthode principale:**
```java
@KafkaListener(
    topics = "transaction-completed",
    groupId = "user-service-group",
    containerFactory = "transactionCompletedKafkaListenerContainerFactory"
)
public void onTransactionCompleted(TransactionCompletedEvent event, ...)
```

### 5. **Configuration Kafka - User Service**

**Fichier:** `user-service/src/main/java/com/zaphira/service_user/config/KafkaConfig.java`

**Ajouts:**
- `transactionCompletedConsumerFactory()` - ConsumerFactory pour TransactionCompletedEvent
- `transactionCompletedKafkaListenerContainerFactory()` - Factory avec manual acknowledgment

### 6. **Service de Logique Métier** - `UserService`

**Interface:** `user-service/src/main/java/com/zaphira/service_user/services/UserService.java`

**Nouvelle méthode:**
```java
void updateBalanceFromTransaction(TransactionCompletedEvent event);
```

### 7. **Implémentation du Service** - `UserServiceImpl`

**Fichier:** `user-service/src/main/java/com/zaphira/service_user/services/UserServiceImpl.java`

**Méthode `updateBalanceFromTransaction`:**
- Vérifie que l'utilisateur existe
- Vérifie l'idempotence (transaction pas déjà traitée)
- Calcule le débit total (montant + frais)
- Enregistre le traitement
- Gère les erreurs sans relancer d'exception (pour éviter les retries infinis)

**Méthodes auxiliaires:**
- `isTransactionAlreadyProcessed()` - Vérifie l'idempotence
- `recordTransactionProcessed()` - Enregistre le succès
- `recordTransactionFailed()` - Enregistre l'échec

### 8. **Log d'Idempotence** - `TransactionProcessedLog`

**Fichier:** `user-service/src/main/java/com/zaphira/service_user/model/entities/TransactionProcessedLog.java`

**Table:** `transaction_processed_log`

**Colonnes:**
- `id` - PK
- `user_id` - FK vers users
- `transaction_id` - ID de la transaction (clé d'idempotence)
- `transaction_reference` - UUID de la transaction
- `amount` - Montant traité
- `processing_status` - SUCCESS | FAILED | PENDING
- `error_message` - Message d'erreur si applicable
- `processed_at` - Timestamp du traitement
- `created_at` - Timestamp de création du log

**Index:**
- Unique compound key: `(user_id, transaction_id)` - Garantit l'idempotence

### 9. **Repository** - `TransactionProcessedLogRepository`

**Fichier:** `user-service/src/main/java/com/zaphira/service_user/repository/TransactionProcessedLogRepository.java`

**Méthode clé:**
```java
Optional<TransactionProcessedLog> findByUserIdAndTransactionId(Long userId, Long transactionId);
```

## 🚀 Flux d'Exécution

### Scénario: Transaction Complétée

#### Étape 1: Transaction-Service (COMPLETED)
```
1. TransactionService.authorizeTransaction() 
2. processImmediateTransaction()
3. changeStatus(COMPLETED)
4. publishTransactionCompletedEvent()
   └─→ TransactionEventPublisher.publishTransactionCompleted()
       └─→ KafkaTemplate.send("transaction-completed", event)
```

#### Étape 2: Kafka Topic
```
Message envoyé:
{
  "transactionId": 123,
  "reference": "TXN-2024-12-13-ABC",
  "initiatorUserId": 456,
  "senderWalletNumber": "WALLET-001",
  "amount": "1000.00",
  "feeAmount": "5.00",
  "currency": "XOF",
  "status": "COMPLETED",
  "completedAt": "2024-12-13T10:30:00Z",
  "eventTimestamp": "2024-12-13T10:30:01Z"
}
```

#### Étape 3: User-Service Consumer
```
1. TransactionEventListener.onTransactionCompleted()
2. Vérifier: status == "COMPLETED" ✓
3. Vérifier: initiatorUserId != null ✓
4. isTransactionAlreadyProcessed(456, 123) → NO
5. UserService.updateBalanceFromTransaction()
   a. Chercher User ID 456
   b. Calculer totalDebit = 1000.00 + 5.00 = 1005.00
   c. Enregistrer dans TransactionProcessedLog (SUCCESS)
   d. Update balance du wallet (via wallet-service ou direct DB)
6. acknowledgment.acknowledge()
```

#### Étape 4: Retry - Déjà Traitée
```
Si message est retraité (topic compaction, broker failover, etc.):
1. isTransactionAlreadyProcessed(456, 123) → YES
2. Log: "Transaction 123 for user 456 has already been processed"
3. acknowledgment.acknowledge()
4. Fin (aucune mise à jour du balance)
```

## 📊 Configuration Kafka

### Topics

```
Topic: transaction-completed
├─ Brokers: 192.168.0.122:9092
├─ Partitions: 1 (ou plus selon volume)
├─ Replication Factor: 1 (développement) ou 3 (production)
├─ Retention: 7 jours (par défaut)
└─ Producer: transaction-service
   Consumers: user-service, notification-service

Topic: transaction-created (existant)
├─ Producer: transaction-service
└─ Consumers: notification-service, ...
```

### Consumer Group

```
Group: user-service-group
├─ Service: user-service
├─ Topics: [transaction-created, transaction-completed]
├─ Auto Offset Reset: earliest
├─ Enable Auto Commit: false (Manual Acknowledgment)
└─ Ack Mode: MANUAL_IMMEDIATE
```

### Sérialisation

```
Producer (transaction-service):
├─ Key Serializer: StringSerializer
└─ Value Serializer: JsonSerializer

Consumer (user-service):
├─ Key Deserializer: StringDeserializer
├─ Value Deserializer: JsonDeserializer
├─ Trusted Packages: "*" (à restreindre en prod)
└─ Auto Offset Reset: earliest
```

## 🛡️ Idempotence - Garantie

### Mécanisme

```
Clé d'Idempotence: (userId + transactionId)

Unique Index: transaction_processed_log (user_id, transaction_id)

Vérification:
1. À la réception du message Kafka
2. SELECT * FROM transaction_processed_log 
   WHERE user_id = ? AND transaction_id = ?
3. Si existe → SKIP (déjà traitée)
4. Si n'existe pas → PROCESS + INSERT log
```

### Scenarios Gérés

| Scénario | Idempotence | Action |
|----------|------------|--------|
| Premier traitement | ✅ | Traiter + Enregistrer |
| Retry après succès | ✅ | Détecter + SKIP |
| Retry après échec | ⚠️ | Réessayer (manuelle via logs) |
| Duplicate message | ✅ | Détecter + SKIP |
| Repartition Kafka | ✅ | Détecter + SKIP |
| Topic compaction | ✅ | Détecter + SKIP |

## 📈 Monitoring et Logs

### Logs Transaction-Service

```
[INFO] Publishing TransactionCompletedEvent for transaction 123
[INFO] Successfully published TransactionCompletedEvent for transaction 123 to offset 45
```

### Logs User-Service

```
[INFO] Received TransactionCompletedEvent: transactionId=123, initiatorUserId=456, amount=1000.00, offset=45
[INFO] Successfully processed TransactionCompletedEvent: transactionId=123, initiatorUserId=456
[WARN] Ignoring transaction event with status FAILED, expected COMPLETED. transactionId=123
[WARN] Transaction 123 for user 456 has already been processed. Skipping.
[ERROR] Failed to process TransactionCompletedEvent: transactionId=123, initiatorUserId=456. Error: User not found
```

### Query d'Audit

```sql
-- Voir toutes les transactions traitées
SELECT * FROM transaction_processed_log 
WHERE user_id = 456 
ORDER BY processed_at DESC;

-- Vérifier les échecs
SELECT * FROM transaction_processed_log 
WHERE processing_status = 'FAILED' 
ORDER BY processed_at DESC;

-- Vérifier une transaction spécifique
SELECT * FROM transaction_processed_log 
WHERE user_id = 456 AND transaction_id = 123;
```

## 🔒 Gestion des Erreurs

### Cas 1: Utilisateur Non Trouvé
```
Exception: UserNotFoundException
Action: 
├─ Log ERROR
├─ Record FAILED in log
├─ Acknowledge message (évite retry infini)
└─ Message supprimé du topic
```

### Cas 2: Transaction Déjà Traitée
```
Détection: findByUserIdAndTransactionId returns Optional.present()
Action:
├─ Log WARN
├─ Skip processing
└─ Acknowledge message
```

### Cas 3: Erreur Inattendue
```
Exception: Any other exception
Action:
├─ Log ERROR with stack trace
├─ Record FAILED in log
├─ Acknowledge message (évite retry infini)
└─ Administrateur review les logs pour correction manuelle
```

### Cas 4: Producer Echec
```
Kafka down ou timeout dans transaction-service
Action:
├─ Log ERROR dans TransactionEventPublisher
├─ Transaction est complétée (COMPLETED status en DB)
├─ Le balance ne sera PAS mis à jour
└─ Alerter l'administrateur pour retry manuel
```

## 📦 Migration Database

### Créer la table de log

```sql
CREATE TABLE transaction_processed_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    transaction_reference VARCHAR(100),
    amount DECIMAL(19, 4),
    processing_status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500),
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, transaction_id),
    FOREIGN KEY(user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_user_transaction ON transaction_processed_log(user_id, transaction_id);
CREATE INDEX idx_user_id ON transaction_processed_log(user_id);
CREATE INDEX idx_transaction_id ON transaction_processed_log(transaction_id);
CREATE INDEX idx_processed_at ON transaction_processed_log(processed_at);
```

## ✅ Checklist de Déploiement

- [ ] Créer le topic Kafka `transaction-completed`
- [ ] Exécuter les migrations de database (TransactionProcessedLog)
- [ ] Compiler common-library avec TransactionCompletedEvent
- [ ] Déployer transaction-service avec le publisher
- [ ] Déployer user-service avec le consumer et service
- [ ] Vérifier les logs du consumer
- [ ] Tester avec une transaction test
- [ ] Vérifier que le balance a été mis à jour
- [ ] Vérifier les logs dans transaction_processed_log
- [ ] Tester l'idempotence (envoyer le même message 2x)
- [ ] Configurer les alertes sur les erreurs (processing_status = FAILED)

## 🧪 Tests Manuels

### Test 1: Transaction Simple

```powershell
# 1. Créer une transaction
POST http://localhost:8083/api/transactions
{
  "senderWalletNumber": "WALLET-001",
  "receiverWalletNumber": "WALLET-002",
  "amount": 1000.00,
  "currency": "XOF",
  "type": "TRANSFER"
}
# Response: { "data": { "id": 123 }, ... }

# 2. Autoriser la transaction
POST http://localhost:8083/api/transactions/123/authorize
{
  "method": "OTP",
  "phoneNumber": "+33612345678",
  "otpCode": "654321"
}
# Status change: COMPLETED
# Kafka event published

# 3. Vérifier le log dans user-service
SELECT * FROM transaction_processed_log WHERE transaction_id = 123;
# Result: processing_status = SUCCESS
```

### Test 2: Idempotence

```sql
-- Avant: 1 log
SELECT COUNT(*) FROM transaction_processed_log WHERE transaction_id = 123;
-- Result: 1

-- Publier le même message Kafka 2 fois manuellement
-- (ou utiliser un outil Kafka CLI)

-- Après: toujours 1 log (pas 2)
SELECT COUNT(*) FROM transaction_processed_log WHERE transaction_id = 123;
-- Result: 1 (toujours, pas de double processing)
```

### Test 3: Utilisateur Non Trouvé

```
Modifier le message Kafka:
{
  "initiatorUserId": 99999  # N'existe pas
}

Vérifier:
- Log: "User not found: 99999"
- DB: transaction_processed_log.processing_status = FAILED
- Message acknowledged (pas de retry)
```

## 📞 Support & Troubleshooting

### Q: Le balance n'est pas mis à jour
- [ ] Vérifier que kafka broker est accessible (192.168.0.122:9092)
- [ ] Vérifier les logs de transaction-service
- [ ] Vérifier les logs de user-service consumer
- [ ] Vérifier que le message est bien envoyé: kafka console consumer
- [ ] Vérifier que l'utilisateur existe: SELECT FROM users WHERE user_id = ?

### Q: Double débit observé
- [ ] Vérifier l'index unique sur (user_id, transaction_id)
- [ ] Vérifier que le log a bien été inséré après traitement
- [ ] Vérifier que le retry logic est bien en place

### Q: Consumer lag important
- [ ] Augmenter les partitions du topic
- [ ] Augmenter les consumer instances
- [ ] Vérifier les erreurs de traitement
- [ ] Vérifier les locks DB

### Commandes Kafka CLI

```bash
# Créer le topic
kafka-topics.sh --create \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction-completed \
  --partitions 1 \
  --replication-factor 1

# Lister les messages
kafka-console-consumer.sh \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction-completed \
  --from-beginning

# Consumer group info
kafka-consumer-groups.sh \
  --bootstrap-server 192.168.0.122:9092 \
  --group user-service-group \
  --describe
```

## 📚 Références

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Idempotent Consumers Pattern](https://kafka.apache.org/documentation/#consumerconfigs)
- [Manual Acknowledgment Best Practices](https://spring.io/blog/2022/10/13/apache-kafka-support-in-spring-boot-3-0)
