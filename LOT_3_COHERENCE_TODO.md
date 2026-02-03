# État de Cohérence et Todo - LOT 3 Scheduling & Batch
**Date**: 3 Février 2026  
**Status**: 🔄 EN COURS

---

## ✅ CE QUI EXISTE DÉJÀ

### ScheduledTransaction - Déjà implémenté ✅
**Fichiers existants:**
- ✅ `model/ScheduledTransaction.java` - Entity principale
- ✅ `model/entities/ScheduledTransaction.java` - Duplicate? À clarifier
- ✅ `ScheduledTransactionRepository.java` - Repository avec queries
- ✅ `ScheduledTransactionService.java` - Service complet
- ✅ `ScheduledTransactionController.java` - Controller avec 4 endpoints
- ✅ `ScheduledTransactionRequest.java` - DTO request
- ✅ `ScheduledTransactionResponse.java` - DTO response
- ✅ `ScheduledTransactionStatus.java` - Enum (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)

**Endpoints existants:**
```
POST   /api/transactions/scheduled         ✅ Créer
GET    /api/transactions/scheduled         ✅ Lister tous
GET    /api/transactions/scheduled/{id}    ✅ Détails
DELETE /api/transactions/scheduled/{id}    ✅ Annuler
```

**⚠️ PROBLÈMES IDENTIFIÉS:**

1. **Security Guards inadéquats:**
   ```java
   @PreAuthorize("hasAnyRole('USER','MERCHANT','ADMIN')")  // ❌ Mauvais rôles
   ```
   - Devrait être: `'REGULAR', 'MERCHANT', 'ADMIN'`
   - Pas de vérification de ownership sur GET/DELETE

2. **Manque DELETE endpoint:**
   - ❌ Params `cancelledBy` et `reason` dans URL au lieu du body
   - ❌ Devrait utiliser custom security: `@scheduledTxSecurity.isOwner(#id)`

3. **Manque GET avec filtres:**
   - ❌ GET /scheduled sans pagination
   - ❌ Pas de filtre par status
   - ❌ Pas de filtre par date

4. **Duplication ScheduledTransaction:**
   - ⚠️ 2 fichiers: `model/ScheduledTransaction.java` vs `model/entities/ScheduledTransaction.java`
   - À vérifier: lequel est utilisé?

---

## ✅ CE QUI VIENT D'ÊTRE CRÉÉ (Phase 2B)

### RecurringTransaction - Nouvellement créé ✅
**Fichiers créés:**
1. ✅ `RecurringTransaction.java` (214 lignes) - Entity complète
2. ✅ `RecurringTransactionExecution.java` (95 lignes) - Audit trail
3. ✅ `RecurringStatus.java` - Enum
4. ✅ `RecurrenceFrequency.java` - Enum avec calcul auto
5. ✅ `RecurringTransactionRepository.java` - 8 queries
6. ✅ `RecurringTransactionExecutionRepository.java` - 7 queries

**Features:**
- ✅ Support DAILY, WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY, YEARLY
- ✅ Auto-calculation de nextRunAt via `calculateNextRun()`
- ✅ Tracking: executionCount, consecutiveFailures
- ✅ Auto-completion: endDate ou maxExecutions
- ✅ Auto-failure: maxConsecutiveFailures
- ✅ Notifications: email, success/failure flags
- ✅ Audit trail complet des exécutions

---

## ❌ CE QUI MANQUE (À FAIRE)

### 1. Corriger ScheduledTransaction (PRIORITÉ HAUTE) 🔴

#### Corrections Security:
```java
// AVANT (incorrect):
@PreAuthorize("hasAnyRole('USER','MERCHANT','ADMIN')")

// APRÈS (correct):
@PreAuthorize("hasAnyRole('REGULAR','MERCHANT','ADMIN')")
```

#### Améliorer endpoints:
- [ ] **GET /scheduled** - Ajouter pagination + filtres (status, date range)
- [ ] **DELETE /scheduled/{id}** - Ajouter `@PreAuthorize("@scheduledTxSecurity.isOwner(#id)")`
- [ ] **PUT /scheduled/{id}** - Créer endpoint pour modifier (si PENDING)
- [ ] **POST /scheduled/{id}/execute-now** - Forcer exécution (ADMIN only)

#### Créer custom security:
- [ ] `ScheduledTransactionSecurityService.java`
- [ ] Méthode: `isOwner(Long id)` - Vérifie requesterUserId == currentUserId

---

### 2. Implémenter RecurringTransactionController (PRIORITÉ HAUTE) 🔴

**À créer:**
- [ ] `RecurringTransactionController.java` avec 7 endpoints:

```java
@RestController
@RequestMapping("/api/transactions/recurring")
public class RecurringTransactionController {
    
    // CRUD
    POST   /api/transactions/recurring                    - Créer récurrence (REGULAR/MERCHANT)
    GET    /api/transactions/recurring                    - Lister mes récurrences (paginated)
    GET    /api/transactions/recurring/{id}               - Détails récurrence
    PUT    /api/transactions/recurring/{id}               - Modifier (si status permet)
    DELETE /api/transactions/recurring/{id}               - Annuler récurrence
    
    // Actions
    POST   /api/transactions/recurring/{id}/pause         - Pause récurrence
    POST   /api/transactions/recurring/{id}/resume        - Resume récurrence
    
    // History
    GET    /api/transactions/recurring/{id}/history       - Historique exécutions (paginated)
    
    // Admin
    GET    /api/transactions/recurring/all                - Vue globale (ADMIN)
    POST   /api/transactions/recurring/{id}/execute-now   - Forcer exécution (ADMIN)
}
```

**Guards:**
```java
@PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")           // Create/list
@PreAuthorize("@recurringTxSecurity.isOwner(#id)")           // View/modify/delete own
@PreAuthorize("hasRole('ADMIN')")                            // View all, force execute
```

---

### 3. Créer RecurringTransactionService (PRIORITÉ HAUTE) 🔴

**À créer:**
- [ ] `RecurringTransactionService.java` avec méthodes:

```java
public interface RecurringTransactionService {
    // CRUD
    RecurringTransactionDTO create(RecurringTransactionRequest request);
    RecurringTransactionDTO findById(Long id);
    Page<RecurringTransactionDTO> findByUserId(Long userId, Pageable pageable);
    RecurringTransactionDTO update(Long id, RecurringTransactionRequest request);
    void cancel(Long id, String reason);
    
    // Actions
    void pause(Long id);
    void resume(Long id);
    
    // Execution
    RecurringTransactionExecutionDTO executeNow(Long id); // Admin force
    Page<RecurringTransactionExecutionDTO> getHistory(Long id, Pageable pageable);
    
    // Background job
    void processReadyRecurrences(); // Called by @Scheduled job
    void autoCompleteExpired();     // Called by @Scheduled job
    void autoMarkAsFailed();        // Called by @Scheduled job
}
```

**Business Logic:**
- [ ] `create()` - Valider dates, calculer nextRunAt initial
- [ ] `update()` - Uniquement si PENDING ou PAUSED
- [ ] `cancel()` - Marquer CANCELLED, ne pas supprimer (audit)
- [ ] `pause()/resume()` - Toggle PAUSED ↔ ACTIVE
- [ ] `processReadyRecurrences()` - Execute + update nextRunAt + track execution
- [ ] Auto-complete si endDate atteint ou maxExecutions
- [ ] Auto-fail si consecutiveFailures >= maxConsecutiveFailures

---

### 4. Créer TransactionSchedulerJob (PRIORITÉ HAUTE) 🔴

**À créer:**
- [ ] `TransactionSchedulerJob.java` - Background scheduler

```java
@Component
@EnableScheduling
public class TransactionSchedulerJob {
    
    private final ScheduledTransactionService scheduledService;
    private final RecurringTransactionService recurringService;
    
    // Execute scheduled transactions (every minute)
    @Scheduled(cron = "0 * * * * *")  // Every minute
    public void processScheduledTransactions() {
        log.info("[SCHEDULER] Processing scheduled transactions...");
        scheduledService.processDueScheduled();
    }
    
    // Execute recurring transactions (every minute)
    @Scheduled(cron = "0 * * * * *")  // Every minute
    public void processRecurringTransactions() {
        log.info("[SCHEDULER] Processing recurring transactions...");
        recurringService.processReadyRecurrences();
    }
    
    // Auto-complete expired recurring (every hour)
    @Scheduled(cron = "0 0 * * * *")  // Every hour
    public void autoCompleteExpired() {
        log.info("[SCHEDULER] Auto-completing expired recurrences...");
        recurringService.autoCompleteExpired();
    }
    
    // Auto-fail recurring with too many failures (every hour)
    @Scheduled(cron = "0 0 * * * *")  // Every hour
    public void autoMarkAsFailed() {
        log.info("[SCHEDULER] Auto-marking failed recurrences...");
        recurringService.autoMarkAsFailed();
    }
    
    // Cleanup old executions (daily at 2am)
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldExecutions() {
        log.info("[SCHEDULER] Cleaning up old executions...");
        recurringService.cleanupOldExecutions(90); // Keep 90 days
    }
}
```

**Configuration:**
- [ ] Enable @EnableScheduling in Application main class
- [ ] Configure thread pool in application.yml:
```yaml
spring:
  task:
    scheduling:
      pool:
        size: 5
      thread-name-prefix: scheduler-
```

---

### 5. Créer Custom Security Services (PRIORITÉ HAUTE) 🔴

#### ScheduledTransactionSecurityService:
```java
@Service("scheduledTxSecurity")
public class ScheduledTransactionSecurityService {
    
    public boolean isOwner(Long scheduledTransactionId) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        ScheduledTransaction scheduled = repository.findById(scheduledTransactionId)
            .orElseThrow(() -> new ScheduledTransactionNotFoundException(scheduledTransactionId));
        return scheduled.getRequesterUserId().equals(currentUserId);
    }
}
```

#### RecurringTransactionSecurityService:
```java
@Service("recurringTxSecurity")
public class RecurringTransactionSecurityService {
    
    public boolean isOwner(Long recurringTransactionId) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        RecurringTransaction recurring = repository.findById(recurringTransactionId)
            .orElseThrow(() -> new RecurringTransactionNotFoundException(recurringTransactionId));
        return recurring.getUserId().equals(currentUserId);
    }
}
```

---

### 6. Créer DTOs manquants (PRIORITÉ MOYENNE) 🟡

**RecurringTransaction DTOs:**
- [ ] `RecurringTransactionRequest.java` - Create/update request
- [ ] `RecurringTransactionDTO.java` - Response
- [ ] `RecurringTransactionExecutionDTO.java` - Execution history response

**ScheduledTransaction DTOs:**
- [ ] Vérifier si les DTOs existants sont complets
- [ ] Ajouter champs manquants si nécessaire

---

### 7. Notifications (PRIORITÉ BASSE) 🟢

**À implémenter:**
- [ ] Envoyer email sur scheduled transaction success/failure
- [ ] Envoyer email sur recurring transaction success/failure (si configuré)
- [ ] Intégrer avec NotificationService existant

---

## 🔍 PROBLÈMES DE COHÉRENCE IDENTIFIÉS

### 1. Duplication ScheduledTransaction
**Fichiers:**
- `transaction-service/src/main/java/com/zaphira/transaction/model/ScheduledTransaction.java`
- `transaction-service/src/main/java/com/zaphira/transaction/model/entities/ScheduledTransaction.java`

**Action requise:**
- ⚠️ Vérifier lequel est référencé dans Repository/Service
- ⚠️ Supprimer le duplicate ou merger les 2
- ⚠️ S'assurer cohérence avec convention: entities dans `model/entities/`

### 2. Rôles incorrects
**Dans ScheduledTransactionController:**
```java
@PreAuthorize("hasAnyRole('USER','MERCHANT','ADMIN')")  // ❌ 'USER' n'existe pas
```

**Rôles corrects:**
- REGULAR (pas USER)
- MERCHANT
- ADMIN

### 3. Manque SecurityContextHolder helper
**Besoin d'ajouter dans `SecurityContextHolder`:**
```java
public class SecurityContextHolder {
    public static Long getCurrentUserId() {
        // Extract from JWT claims
    }
    
    public static String getCurrentUserEmail() {
        // Extract from JWT claims
    }
    
    public static List<String> getCurrentUserRoles() {
        // Extract from JWT claims
    }
}
```

---

## 📊 RÉSUMÉ TODO LIST

### Phase 1: Corrections (URGENT) 🔴
- [ ] Corriger rôles dans ScheduledTransactionController ('USER' → 'REGULAR')
- [ ] Résoudre duplication ScheduledTransaction (model/ vs model/entities/)
- [ ] Créer ScheduledTransactionSecurityService
- [ ] Ajouter custom security guards sur endpoints scheduled

### Phase 2: RecurringTransaction (PRIORITÉ HAUTE) 🔴
- [ ] Créer RecurringTransactionController (7 endpoints)
- [ ] Créer RecurringTransactionService (business logic)
- [ ] Créer RecurringTransactionSecurityService
- [ ] Créer DTOs (Request, DTO, ExecutionDTO)

### Phase 3: Background Jobs (PRIORITÉ HAUTE) 🔴
- [ ] Créer TransactionSchedulerJob avec @Scheduled methods
- [ ] Implémenter processDueScheduled() dans ScheduledTransactionService
- [ ] Implémenter processReadyRecurrences() dans RecurringTransactionService
- [ ] Implémenter auto-complete et auto-fail logic
- [ ] Configurer thread pool

### Phase 4: Améliorations (PRIORITÉ MOYENNE) 🟡
- [ ] Ajouter pagination + filtres sur GET /scheduled
- [ ] Créer PUT /scheduled/{id} pour modification
- [ ] Créer POST /scheduled/{id}/execute-now (ADMIN)
- [ ] Améliorer error handling et logging

### Phase 5: Notifications (PRIORITÉ BASSE) 🟢
- [ ] Intégrer avec NotificationService
- [ ] Envoyer emails success/failure
- [ ] Support SMS notifications (optionnel)

---

## 🎯 NEXT STEPS

### Immédiat (maintenant):
1. Corriger rôles dans ScheduledTransactionController
2. Résoudre duplication ScheduledTransaction
3. Créer custom security services

### Court terme (aujourd'hui):
1. Créer RecurringTransactionController
2. Créer RecurringTransactionService
3. Créer TransactionSchedulerJob

### Moyen terme (cette semaine):
1. Tests complets
2. Documentation API
3. Déploiement

---

## 📝 NOTES

### Cron Expressions:
- `0 * * * * *` - Every minute
- `0 0 * * * *` - Every hour
- `0 0 2 * * *` - Every day at 2am
- `0 0 0 * * MON` - Every Monday at midnight

### Thread Pool Configuration:
- Default: 1 thread
- Recommandé: 5-10 threads pour production
- Max: Dépend de charge attendue

### Retry Strategy:
- Max retries: 3
- Delay: Exponential backoff (1min, 5min, 15min)
- After max retries: Mark as FAILED

### Cleanup Policy:
- Keep executions: 90 days
- Keep failed scheduled: 30 days
- Keep completed recurring: Forever (audit)

---

## ✅ VALIDATION CHECKLIST

Avant de marquer LOT 3 comme complet:
- [ ] Tous les endpoints implémentés avec guards
- [ ] Custom security services fonctionnels
- [ ] Background jobs exécutent correctement
- [ ] Notifications envoyées
- [ ] Tests passent (unit + integration)
- [ ] Documentation API complète
- [ ] Pas d'erreurs de compilation
- [ ] Cohérence rôles/permissions
- [ ] Audit trail complet

**Status actuel: 40% complet**
- ✅ Entities & Repositories créés
- ⚠️ Controllers partiellement créés
- ❌ Services incomplets
- ❌ Background jobs manquants
- ❌ Custom security manquant
