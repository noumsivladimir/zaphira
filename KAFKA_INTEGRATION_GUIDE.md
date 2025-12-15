# INTÉGRATION KAFKA DANS TRANSACTIONSERVICE

## Points de modification minimal et sûr

### 1. Ajouter injection du ValidationOrchestrationService

```java
// Dans TransactionService.java - Ajouter à l'injection existante

@Service
public class TransactionService {

    // ... services existants ...
    private ValidationOrchestrationService validationOrchestrationService;

    public TransactionService(
        // ... paramètres existants ...
        ValidationOrchestrationService validationOrchestrationService  // AJOUTER
    ) {
        // ... assignations existantes ...
        this.validationOrchestrationService = validationOrchestrationService;
    }
}
```

### 2. Appel à la validation asynchrone (après création)

**Localisation**: Fin de la méthode `createTransaction()`

```java
// Dans createTransaction(), AVANT le return final

// AJOUTER: Publier demande de validation asynchrone si nécessaire
if (evaluation.isAuthorizationRequired()) {
    validationOrchestrationService.initiateAsyncValidation(saved);
}

return saved;
```

**Contexte complet** (extrait existant à modifier):

```java
if (evaluation.isAuthorizationRequired()) {
    saved.applyStatus(TransactionStatus.PENDING);
    repository.save(saved);
    recordState(saved, TransactionStatus.PENDING, request.getRequestedBy(), evaluation.getReason());
    authorizationService.createAuthorization(saved, evaluation.getMethod(), request.getRequestedBy());
    
    // AJOUTER CES 2 LIGNES:
    validationOrchestrationService.initiateAsyncValidation(saved);
    
    return saved;
}
```

### 3. Exception handling (optionnel mais recommandé)

Ajouter à `GlobalExceptionHandler.java`:

```java
@ExceptionHandler(org.springframework.kafka.KafkaException.class)
public ResponseEntity<Map<String, Object>> handleKafkaError(org.springframework.kafka.KafkaException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("message", "Validation service unavailable: " + ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
}
```

## Migration progressive (FEATURE FLAG)

Pour tester sans impacter le produit:

```java
@Value("${feature.async-validation.enabled:false}")
private boolean asyncValidationEnabled;

if (evaluation.isAuthorizationRequired() && asyncValidationEnabled) {
    validationOrchestrationService.initiateAsyncValidation(saved);
}
```

Configuration:
```yaml
feature:
  async-validation:
    enabled: false  # À mettre à true après tests
```

## Garanties de compatibilité

✅ **Endpoints REST**: Inchangés (pas de breaking change)
✅ **TransactionController**: Pas de modification
✅ **Logique métier**: Même comportement (sync validation existante + async optionnel)
✅ **DTOs**: TransactionRequest/Response identiques
✅ **Statuts**: PENDING toujours utilisé
✅ **Sécurité**: Aucun JWT dans Kafka

## Flux après intégration

```
POST /api/transactions
    ↓
TransactionService.createTransaction()
    ↓
Création en BD, status INITIATED
    ↓
Vérification des limites/autorisation nécessaire?
    ↓ Si OUI
    Status PENDING
    ↓
    ValidationOrchestrationService.initiateAsyncValidation()  [ASYNC]
        ↓
        Crée ValidationRequest en BD
        Publie TransactionValidationRequest vers Kafka
    ↓
    Return transaction au client (HTTP 201)
    
[Parallèlement - Consumer Kafka]
Consumer reçoit TransactionValidationResult
    ↓
ValidationCoordinatorService.processValidationResult()
    ↓
Met à jour transaction.status = AUTHORIZED/FAILED/EXPIRED
    ↓
Client peut interroger GET /api/transactions/{id} pour voir le statut final
```

## Rollback si problème

Pour désactiver complètement Kafka:

1. **Ajouter feature flag**:
```java
@Value("${feature.async-validation.enabled:false}")
private boolean asyncValidationEnabled;

if (evaluation.isAuthorizationRequired() && asyncValidationEnabled) {
    validationOrchestrationService.initiateAsyncValidation(saved);
}
```

2. **En prod**, passer `feature.async-validation.enabled=false`
3. Les transactions continueront d'être créées normalement
4. Les messages Kafka en attente seront DLQ et ne bloqueront rien

## Tests unitaires à ajouter

```java
@Test
void testAsyncValidationInitiated() {
    // Given: transaction nécessite autorisation
    // When: createTransaction appelée
    // Then: validationOrchestrationService.initiateAsyncValidation() invoqué
    verify(validationOrchestrationService).initiateAsyncValidation(any());
}

@Test
void testValidationResultProcessed() {
    // Given: ValidationResult reçu du Kafka
    // When: consumer appelle handleValidationResult
    // Then: transaction.status = AUTHORIZED et saved en BD
    assertEquals(TransactionStatus.AUTHORIZED, updated.getStatus());
}
```
