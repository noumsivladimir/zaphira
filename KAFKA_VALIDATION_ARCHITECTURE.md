# Architecture Kafka - Validation Asynchrone Transactionnelle

## 1. ANALYSE DU CODE EXISTANT

### 1.1 Entités Principales
- **Transaction** (JPA Entity)
  - Statuts: INITIATED, PENDING, AUTHORIZED, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED, REFUNDED, EXPIRED, ON_HOLD, UNDER_REVIEW
  - Relations: senderWallet, receiverWallet (ManyToOne LAZY)
  - Montants: amount, feeAmount
  - Auditoria: initiatedAt, authorizedAt, processingAt, completedAt, failedAt, etc.

### 1.2 Services Métier Clés
- **TransactionService**: Orchestration complète (create, authorize, update, cancel)
- **TransactionAuthorizationService**: Gestion des demandes d'autorisation
- **TransactionValidationService**: Validations métier (montants, wallets)
- **ComplianceService**: Vérifications de conformité
- **FeeService**: Calcul des frais
- **TransactionLimitService**: Vérification des limites par utilisateur
- **TransactionEventPublisher**: Publication d'événements sur Kafka (déjà en place)

### 1.3 Endpoints REST Actuels (IMMUABLES)
```
POST   /api/transactions                          # Création
GET    /api/transactions                          # Liste
GET    /api/transactions/{id}                    # Détail
GET    /api/transactions/{id}/history            # Historique
GET    /api/transactions/{id}/authorization      # Info autorisation
POST   /api/transactions/{id}/authorize           # Autorisation
PUT    /api/transactions/{id}/status             # Mise à jour statut
PUT    /api/transactions/{id}/cancel             # Annulation
```

### 1.4 Infrastructure Kafka Existante
- **KafkaConfig.java** configurée
- **TransactionEventPublisher** publie sur topic `transaction-created`
- Producer avec idempotence activée ✅
- Sérialisation JSON ✅

### 1.5 Gestion d'Erreurs
- **GlobalExceptionHandler** centralisé
- Exceptions spécialisées: TransactionNotFoundException, AuthorizationException, ComplianceException, WalletOperationException

---

## 2. DESIGN KAFKA PROPOSÉ

### 2.1 Workflow de Validation Asynchrone

```
┌─────────────────────────────────────────────────────────────────┐
│ Transaction crée → PENDING → Événement Kafka                    │
│                                      ↓                           │
│                    transaction.validation.request                │
│                           (transactionId)                        │
│                                      ↓                           │
│                     Service de Validation (externe)             │
│                    - Compliance check                           │
│                    - Risk scoring                               │
│                    - 3rd party verification                     │
│                                      ↓                           │
│                     transaction.validation.result               │
│                      (transactionId, status, ...)               │
│                                      ↓                           │
│        TransactionService consume et met à jour                 │
│         AUTHORIZED ou FAILED ou EXPIRED                         │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Topics Kafka

| Topic | Clé | Partition | Rétention | Objectif |
|-------|-----|-----------|-----------|----------|
| `transaction.validation.request` | transactionId | 6 | 24h | Demande de validation |
| `transaction.validation.result` | transactionId | 6 | 24h | Résultat de validation |

### 2.3 Idempotence et Robustesse

- **Clé**: `transactionId` (garantit l'ordre par transaction)
- **CorrelationId**: UUID unique par demande (évite doubles traitements)
- **Versioning**: Version timestamp dans le message
- **Retry**: Kafka retry policy + Dead Letter Topic en cas d'erreur
- **Timeout**: Expiration configurable de l'autorisation

---

## 3. DTOs KAFKA

### 3.1 TransactionValidationRequest
```
{
  "correlationId": "uuid-abc123",
  "transactionId": 42,
  "reference": "7f381432-3d2d-410f-8d46-...",
  "senderWalletNumber": "2",
  "receiverWalletNumber": "1",
  "amount": 5000.0000,
  "currency": "XOF",
  "feeAmount": 25.0000,
  "type": "P2P_TRANSFER",
  "channel": "MOBILE",
  "riskScore": 0,
  "initiatedAt": "2025-12-15T17:40:00Z"
}
```

### 3.2 TransactionValidationResult
```
{
  "correlationId": "uuid-abc123",
  "transactionId": 42,
  "validationStatus": "APPROVED", // ou REJECTED, EXPIRED
  "reason": "Compliance check passed",
  "validatorId": "compliance-service-1",
  "validatedAt": "2025-12-15T17:40:30Z",
  "riskScoreFinal": 15,
  "complianceStatus": "CLEAR", // ou FLAGGED, BLOCKED
  "metadata": {
    "verificationMethod": "auto_check",
    "amlStatus": "PASS"
  }
}
```

---

## 4. STRATÉGIE D'INTÉGRATION

### 4.1 Points d'Extension (Sans Casser l'Existant)

**Point 1**: Après création transaction
- Status: INITIATED → (optionnel) PENDING_AUTHORIZATION
- Événement publié automatiquement
- Flux existant non impacté

**Point 2**: Réception du résultat
- Consumer Kafka indépendant
- Appel safe à `updateTransactionFromValidationResult()`
- Idempotence garantie via correlationId + BD

**Point 3**: Timeout gestion
- Scheduled task vérifiant expirations
- Marque comme EXPIRED si pas de réponse

### 4.2 Garanties Transactionnelles

```
Consumer reçoit event
    ↓
Vérifier correlationId (idempotence)
    ↓
Vérifier état actuel (évite inversion)
    ↓
Mise à jour JPA en @Transactional
    ↓
Commit success / Rollback erreur
```

---

## 5. FICHIERS À CRÉER / MODIFIER

### À Créer:
1. `kafka/event/TransactionValidationRequest.java` (DTO)
2. `kafka/event/TransactionValidationResult.java` (DTO)
3. `kafka/producer/ValidationRequestProducer.java`
4. `kafka/consumer/ValidationResultConsumer.java`
5. `service/kafka/ValidationCoordinatorService.java`
6. `repository/ValidationRequestRepository.java` (pour idempotence)
7. `model/ValidationRequest.java` (JPA Entity pour tracer)

### À Modifier:
1. `config/KafkaConfig.java` (ajouter consumer/producer configs)
2. `service/TransactionService.java` (ajouter appel producer après création)
3. `exception/GlobalExceptionHandler.java` (ajouter gestion Kafka exceptions)

---

## 6. SÉCURITÉ

- ❌ AUCUN JWT dans les messages Kafka
- ✅ CorrelationId pour traçabilité
- ✅ Validation stricte de l'état avant mise à jour
- ✅ Logs de toutes les transitions
- ✅ No sensitive data in messages

---

## 7. COMPATIBILITÉ

- ✅ Endpoints REST inchangés
- ✅ Logique métier conservée
- ✅ Transactions existantes non affectées
- ✅ Migration progressive possible (feature flag)
