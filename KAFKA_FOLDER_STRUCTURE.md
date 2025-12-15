# STRUCTURE DES DOSSIERS - KAFKA IMPLEMENTATION

## Arborescence complète à créer

```
transaction-service/src/main/java/com/zaphira/transaction/
│
├── config/
│   └── KafkaConfig.java (MODIFIER - ajouter consumer config)
│
├── controller/
│   └── TransactionController.java (INCHANGÉ)
│
├── dto/
│   ├── TransactionRequest.java (INCHANGÉ)
│   ├── AuthorizationValidationRequest.java (INCHANGÉ)
│   └── ... (autres DTOs)
│
├── event/
│   └── TransactionEventPublisher.java (INCHANGÉ)
│
├── exception/
│   ├── GlobalExceptionHandler.java (MODIFIER - ajouter Kafka handler)
│   └── RestExceptionHandler.java (INCHANGÉ)
│
├── integration/
│   └── wallet/
│       ├── WalletClient.java (INCHANGÉ)
│       └── ...
│
├── kafka/ (NOUVEAU DOSSIER)
│   ├── event/ (NOUVEAU)
│   │   ├── TransactionValidationRequest.java (CRÉER)
│   │   └── TransactionValidationResult.java (CRÉER)
│   │
│   ├── producer/ (NOUVEAU)
│   │   └── ValidationRequestProducer.java (CRÉER)
│   │
│   └── consumer/ (NOUVEAU)
│       └── ValidationResultConsumer.java (CRÉER)
│
├── model/
│   ├── Transaction.java (INCHANGÉ)
│   ├── TransactionStateHistory.java (INCHANGÉ)
│   ├── enums/
│   │   ├── TransactionStatus.java (INCHANGÉ)
│   │   └── ...
│   │
│   └── kafka/ (NOUVEAU DOSSIER)
│       └── ValidationRequest.java (CRÉER)
│
├── repository/
│   ├── TransactionRepository.java (INCHANGÉ)
│   ├── TransactionStateHistoryRepository.java (INCHANGÉ)
│   │
│   └── kafka/ (NOUVEAU DOSSIER)
│       └── ValidationRequestRepository.java (CRÉER)
│
├── security/
│   ├── JwtAuthenticationFilter.java (INCHANGÉ)
│   └── ...
│
├── service/
│   ├── TransactionService.java (MODIFIER - 1 injection + 1 appel)
│   ├── TransactionValidationService.java (INCHANGÉ)
│   ├── ScheduledTransactionService.java (INCHANGÉ)
│   │
│   ├── authorization/
│   │   └── TransactionAuthorizationService.java (INCHANGÉ)
│   │
│   ├── compliance/
│   │   └── ComplianceService.java (INCHANGÉ)
│   │
│   ├── fee/
│   │   └── FeeService.java (INCHANGÉ)
│   │
│   ├── limit/
│   │   └── TransactionLimitService.java (INCHANGÉ)
│   │
│   ├── routing/
│   │   └── RoutingService.java (INCHANGÉ)
│   │
│   └── kafka/ (NOUVEAU DOSSIER)
│       ├── ValidationCoordinatorService.java (CRÉER)
│       └── ValidationOrchestrationService.java (CRÉER)
│
└── TransactionServiceApplication.java (INCHANGÉ)

transaction-service/src/main/resources/
│
├── application.yml (MODIFIER - ajouter config Kafka)
│   ou
├── application.properties (MODIFIER)
│
└── ...
```

## Fichiers CRÉER (7 nouveaux)

```
NEW FILES:
1. kafka/event/TransactionValidationRequest.java
2. kafka/event/TransactionValidationResult.java
3. kafka/producer/ValidationRequestProducer.java
4. kafka/consumer/ValidationResultConsumer.java
5. model/kafka/ValidationRequest.java
6. repository/kafka/ValidationRequestRepository.java
7. service/kafka/ValidationCoordinatorService.java
8. service/kafka/ValidationOrchestrationService.java
```

## Fichiers MODIFIER (3)

```
MODIFY FILES:
1. config/KafkaConfig.java
   - Ajouter consumer factory
   - Ajouter listener container factory
   - (Producer existant conservé)

2. service/TransactionService.java
   - Ajouter injection ValidationOrchestrationService
   - Ajouter appel initiateAsyncValidation()
   - Localisation: fin de createTransaction()

3. exception/GlobalExceptionHandler.java
   - Ajouter @ExceptionHandler pour KafkaException
   - (Optionnel mais recommandé)

4. src/main/resources/application.yml (CONFIG)
   - Ajouter consumer group config
   - Ajouter offset reset strategy
```

## Migration de la base de données

```sql
-- Créer table pour idempotence
CREATE TABLE validation_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    correlation_id VARCHAR(36) NOT NULL UNIQUE,
    transaction_id BIGINT NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_processed_at (processed_at),
    INDEX idx_correlation_id (correlation_id)
);

-- Optionnel: trigger pour nettoyer les anciennes requêtes
CREATE EVENT cleanup_validation_requests
ON SCHEDULE EVERY 1 DAY
DO
  DELETE FROM validation_requests 
  WHERE requested_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

## Configuration Kafka Topics

```bash
# À créer manuellement ou via infra-as-code

Topic 1: transaction.validation.request
- Partitions: 6
- Replication Factor: 3
- Retention: 24 hours (86400000 ms)
- Key: transactionId
- Format: JSON

Topic 2: transaction.validation.result
- Partitions: 6
- Replication Factor: 3
- Retention: 24 hours (86400000 ms)
- Key: transactionId
- Format: JSON

Consumer Group: transaction-service-validation-result
- Members: 1-3 instances
- Auto-offset-reset: earliest
```

## Commandes à exécuter

```bash
# 1. Créer les topics
kafka-topics --create \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.request \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=86400000

kafka-topics --create \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.result \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=86400000

# 2. Vérifier les topics
kafka-topics --list --bootstrap-server 192.168.0.122:9092

# 3. Décrire les topics
kafka-topics --describe \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.request

kafka-topics --describe \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.result

# 4. Monitoring (optionnel)
kafka-consumer-groups --list --bootstrap-server 192.168.0.122:9092
kafka-consumer-groups --describe \
  --bootstrap-server 192.168.0.122:9092 \
  --group transaction-service-validation-result
```

## Timeline d'implémentation

```
Phase 1: Code Review & Tests (1 jour)
├── Relire documentation
├── Tests unitaires locaux
├── Vérifier Kafka broker accessible
└── ✅ Go/No-go

Phase 2: Création topics & Configuration (0.5 jour)
├── Créer topics Kafka
├── Configurer application.yml
├── Ajouter DB migration
└── ✅ Prêt déploiement

Phase 3: Modifications code (0.5 jour)
├── Créer 8 fichiers Java
├── Modifier 2-3 fichiers existants
├── Tests intégration
└── ✅ Build success

Phase 4: Déploiement (2 jours)
├── Deploy avec feature flag = false
├── Validation 24h
├── Activate feature flag = true (10%)
├── Canary 10% → 50% → 100%
└── ✅ Production stable

Total: ~3-4 jours du code au production
```

## Checklist points critiques

- [ ] Tous les 8 fichiers créés compiling
- [ ] KafkaConfig compilant avec consumer factory
- [ ] TransactionService compilant avec injection + appel
- [ ] Table `validation_requests` créée en BD
- [ ] Topics Kafka créés et accessibles
- [ ] application.yml avec config Kafka
- [ ] Feature flag = false (sûreté)
- [ ] Tests e2e passent
- [ ] Monitoring actif (consumer lag, messages)
- [ ] Rollback plan documenté et testé

---

**Status**: ✅ Structure validée et documentée
**Fichiers à créer**: 8 Java + 1 config + 1 DB migration
**Fichiers à modifier**: 3 Java
**Complexité**: MOYENNE (interactions Kafka/DB/Service)
**Risque**: 🟢 MINIMAL (feature flag protection)
