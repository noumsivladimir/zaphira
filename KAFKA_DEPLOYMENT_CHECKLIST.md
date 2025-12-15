# CHECKLIST DÉPLOIEMENT KAFKA - VALIDATION ASYNCHRONE

## PHASE 1: CODE REVIEW & TESTS UNITAIRES

- [ ] Relire la documentation architecture: `KAFKA_VALIDATION_ARCHITECTURE.md`
- [ ] Relire le guide intégration: `KAFKA_INTEGRATION_GUIDE.md`
- [ ] Générer et tester les DTOs Kafka:
  - `TransactionValidationRequest.java`
  - `TransactionValidationResult.java`
- [ ] Tester le Producer Kafka: `ValidationRequestProducer`
- [ ] Tester le Consumer Kafka: `ValidationResultConsumer`
- [ ] Tester le Coordinator: `ValidationCoordinatorService`
- [ ] Tester l'idempotence: `ValidationRequestRepository`

## PHASE 2: CONFIGURATION & INFRASTRUCTURE

### Kafka Broker
- [ ] Vérifier que broker 192.168.0.122:9092 est accessible
- [ ] Créer les topics:
  ```bash
  # Topic pour requêtes
  kafka-topics --create \
    --bootstrap-server 192.168.0.122:9092 \
    --topic transaction.validation.request \
    --partitions 6 \
    --replication-factor 3 \
    --config retention.ms=86400000

  # Topic pour résultats  
  kafka-topics --create \
    --bootstrap-server 192.168.0.122:9092 \
    --topic transaction.validation.result \
    --partitions 6 \
    --replication-factor 3 \
    --config retention.ms=86400000
  ```

### Application Configuration
- [ ] Vérifier `KafkaConfig.java` avec producer + consumer
- [ ] Ajouter configuration application.yml:
  ```yaml
  spring:
    kafka:
      bootstrap-servers: 192.168.0.122:9092
      producer:
        acks: all
        retries: 3
      consumer:
        group-id: transaction-service-validation-result
        auto-offset-reset: earliest
        enable-auto-commit: false
  ```

## PHASE 3: MODIFICATIONS CODE MINIMAL

- [ ] Ajouter `ValidationOrchestrationService` à `TransactionService` (injection)
- [ ] Ajouter appel `validationOrchestrationService.initiateAsyncValidation(saved)` 
  - Localisation: Fin de `createTransaction()` si `evaluation.isAuthorizationRequired()`
  - **IMPORTANT**: Ne pas modifier d'autres fonctionnalités
- [ ] Ajouter exception handler pour Kafka dans `GlobalExceptionHandler`

## PHASE 4: MIGRATION PROGRESSIVE (SANS RISQUE)

### 4.1 Déployer avec feature flag désactivé
```yaml
feature:
  async-validation:
    enabled: false
```

### 4.2 Vérifier que tout fonctionne
- [ ] POST /api/transactions → OK
- [ ] GET /api/transactions/{id} → OK
- [ ] Les transactions se créent normalement
- [ ] Aucun message d'erreur Kafka

### 4.3 Activer graduellement
```yaml
feature:
  async-validation:
    enabled: true  # À 10% d'abord, via canary deployment
```

### 4.4 Monitoring
- [ ] Monitorer le topic `transaction.validation.request` (messages publiés)
- [ ] Monitorer le topic `transaction.validation.result` (messages reçus)
- [ ] Vérifier que transactions se mettent à jour (status AUTHORIZED/FAILED)
- [ ] Vérifier aucun message en DLQ

## PHASE 5: VALIDATION & MONITORING

### Métriques à surveiller
- [ ] Publications/sec sur validation.request
- [ ] Consommations/sec sur validation.result
- [ ] Lag du consumer group
- [ ] Erreurs de sérialisation JSON
- [ ] Timeouts (transactions restent PENDING > 5min)

### Tests en prod (optionnel)
```bash
# Vérifier que les topics reçoivent des messages
kafka-console-consumer \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.request \
  --from-beginning \
  --max-messages 5

kafka-console-consumer \
  --bootstrap-server 192.168.0.122:9092 \
  --topic transaction.validation.result \
  --from-beginning \
  --max-messages 5
```

## PHASE 6: ROLLBACK PLAN

Si problème détecté:

### Option 1: Feature flag (RECOMMANDÉ)
```yaml
feature:
  async-validation:
    enabled: false
```
Redéployer → Validation asynchrone arrêtée immédiatement

### Option 2: Supprimer injection (si besoin d'urgence)
- Commenter la ligne `validationOrchestrationService.initiateAsyncValidation()`
- Redéployer

### Garantie: Les transactions continuent de fonctionner normalement en sync

## FICHIERS À CRÉER / MODIFIER

### À CRÉER:
```
transaction-service/src/main/java/com/zaphira/transaction/
├── kafka/
│   ├── event/
│   │   ├── TransactionValidationRequest.java ✅
│   │   └── TransactionValidationResult.java ✅
│   ├── producer/
│   │   └── ValidationRequestProducer.java ✅
│   └── consumer/
│       └── ValidationResultConsumer.java ✅
├── model/
│   └── kafka/
│       └── ValidationRequest.java (JPA Entity) ✅
├── repository/
│   └── kafka/
│       └── ValidationRequestRepository.java ✅
└── service/
    └── kafka/
        ├── ValidationCoordinatorService.java ✅
        └── ValidationOrchestrationService.java ✅
```

### À MODIFIER:
```
transaction-service/src/main/java/com/zaphira/transaction/
├── config/
│   └── KafkaConfig.java ✅ (ajouter consumer config)
├── service/
│   └── TransactionService.java ⚠️ (ajouter 1 injection + 1 appel)
└── exception/
    └── GlobalExceptionHandler.java (optionnel - ajouter handler Kafka)
```

## VÉRIFICATION FINALE

```java
// Test d'intégration simple
@Test
void kafkaIntegration() {
    // 1. Créer transaction
    Transaction tx = transactionService.createTransaction(request);
    assertEquals(TransactionStatus.PENDING, tx.getStatus());
    
    // 2. Vérifier que correlationId a été généré
    ValidationRequest vr = validationRequestRepository.findByTransactionId(tx.getId()).get(0);
    assertNotNull(vr.getCorrelationId());
    
    // 3. Simuler réception de résultat
    TransactionValidationResult result = new TransactionValidationResult(
        vr.getCorrelationId(),
        tx.getId(),
        ValidationStatus.APPROVED,
        "OK",
        "validator-1",
        LocalDateTime.now(),
        20,
        ComplianceStatus.CLEAR,
        null
    );
    
    // 4. Vérifier que transaction est mise à jour
    coordinatorService.processValidationResult(result);
    Transaction updated = transactionRepository.findById(tx.getId()).get();
    assertEquals(TransactionStatus.AUTHORIZED, updated.getStatus());
}
```

## SUPPORT & TROUBLESHOOTING

### Message pas reçu?
1. Vérifier que Kafka broker est actif
2. Vérifier les logs du consumer
3. Vérifier les topics Kafka existent
4. Vérifier feature flag = true

### Double consommation?
1. Vérifier la DB: table `validation_requests` à `PROCESSED`
2. Vérifier que correlationId n'existe qu'une fois

### Transaction reste PENDING?
1. Vérifier que `transaction.validation.result` reçoit des messages
2. Vérifier consumer lag
3. Vérifier les logs de `ValidationResultConsumer`
4. Vérifier schema JSON match

---

**Status**: ✅ Prêt pour déploiement progressif
**Risque**: 🟢 MINIMAL (feature flag, rollback facile, pas de breaking change)
**Date**: 2025-12-15
