# 🎯 PLAN D'IMPLÉMENTATION CORE - Zaphira Platform
**Date**: 3 Février 2026  
**Objectif**: Implémenter et intégrer les fonctionnalités principales qui font tourner le core de l'API

---

## 📊 ANALYSE DE L'ÉTAT ACTUEL

### ✅ CE QUI EST FONCTIONNEL (80%)

#### 🔐 PHASE 1 - Infrastructure de Sécurité: 100% ✅
- JWT avec rôles (REGULAR, MERCHANT, ADMIN)
- Spring Security sur tous les services
- Custom security expressions (@txSecurity, @walletSecurity)
- API Gateway avec validation JWT

#### 💰 Transaction Service - LOT 1: 100% ✅
**Core transactionnel complet:**
- ✅ Transfer P2P, Deposit, Withdrawal
- ✅ Merchant payments
- ✅ Transaction processing/cancellation
- ✅ Wallet history
- ✅ Security guards sur tous les endpoints
- ✅ TransactionStateHistory (audit trail)
- ✅ Transaction fees calculation

#### 💼 Wallet Service - LOT 1: 100% ✅
**Gestion wallet de base:**
- ✅ Create wallet (REGULAR/MERCHANT/ADMIN)
- ✅ Wallet lifecycle (freeze, suspend, activate, close)
- ✅ Balance operations (credit, debit, block, unblock)
- ✅ Security guards sur tous les endpoints
- ✅ Multi-wallet support (via SubWallet)

#### 🔄 LOT 2 - Bulk Operations: 100% ✅
- ✅ Bulk transfers (MERCHANT/ADMIN)
- ✅ Split payments
- ✅ Atomic transactions

### ⚠️ CE QUI EST PARTIEL (40-60%)

#### 🔍 LOT 4 - Search & Reporting: 60% ⚠️
**Ce qui existe:**
- ✅ Global search (ADMIN only)
- ✅ Wallet history
- ✅ Repositories analytics (MerchantAnalyticsRepository, etc.)

**Ce qui manque:**
- ❌ GET /api/transactions/my-transactions (REGULAR - ses propres transactions)
- ❌ GET /api/transactions/merchant/reports (MERCHANT - ses ventes)
- ❌ GET /api/transactions/merchant/analytics (MERCHANT - agrégats)
- ❌ Pagination et filtres avancés

#### 📅 LOT 3 - Scheduling: 50% ⚠️
**ScheduledTransaction:**
- ✅ Entity + Repository + Service + Controller
- ✅ Security guards (corrigés récemment)
- ❌ Background job pour exécution
- ❌ PUT endpoint pour modification
- ❌ Pagination et filtres

**RecurringTransaction:**
- ✅ Entity + Execution entity + Repositories
- ✅ Business logic dans entity (calculateNextRun, etc.)
- ❌ Service layer (0%)
- ❌ Controller (0%)
- ❌ Security service (0%)
- ❌ Background job pour exécution

#### ⚖️ LOT 5 - Disputes: 40% ⚠️
**Ce qui existe:**
- ✅ Dispute entity + DisputeEvidence entity
- ✅ Repositories
- ✅ Refund transaction endpoint
- ✅ Reverse transaction endpoint
- ✅ Security expression @txSecurity.canRefund

**Ce qui manque:**
- ❌ DisputeController complet (0%)
- ❌ DisputeService avec workflow (0%)
- ❌ Guards pour disputes (REGULAR crée, MERCHANT répond, ADMIN arbitre)

### ❌ CE QUI N'EST PAS IMPLÉMENTÉ (0%)

#### 💱 LOT 6 - FX & Multi-devise: 0% ❌
- ✅ ExchangeRate entity
- ✅ TransactionFees with FX fields
- ❌ ExchangeRateController
- ❌ Service pour fetch/refresh rates
- ❌ Conversion logic

---

## 🎯 STRATÉGIE D'IMPLÉMENTATION PAR PRIORITÉ

### PRIORITÉ 1 (CRITIQUE) 🔴 - Core Fonctionnel
**Objectif**: Assurer que le core transactionnel/wallet est 100% opérationnel et testable

#### 1.1 Vérifier et Valider LOT 1 & LOT 2
- [ ] **Tests d'intégration Transaction Service**
  - Tester tous les endpoints LOT 1
  - Tester bulk transfer et split payment
  - Valider security guards
  - Vérifier audit trail (TransactionStateHistory)
  
- [ ] **Tests d'intégration Wallet Service**
  - Tester création wallet (REGULAR/MERCHANT)
  - Tester lifecycle operations
  - Tester balance operations
  - Valider security guards

- [ ] **Tests end-to-end**
  - Scénario: Registration → Wallet creation → Transaction
  - Scénario: Merchant payment flow complet
  - Scénario: Refund flow

**Temps estimé:** 2-3 jours  
**Blockers:** Aucun - tout est implémenté

---

### PRIORITÉ 2 (HAUTE) 🟠 - Fonctionnalités Essentielles

#### 2.1 Compléter Search & Reporting (LOT 4)
**Pourquoi prioritaire:** Les utilisateurs ont besoin de voir leur historique

**Implémentation:**

##### A. User-scoped transactions (REGULAR/MERCHANT)
```java
// TransactionController - 3 nouveaux endpoints
@GetMapping("/my-transactions")
@PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
Page<TransactionDTO> getMyTransactions(
    @RequestParam(required = false) TransactionStatus status,
    @RequestParam(required = false) TransactionType type,
    @RequestParam(required = false) LocalDateTime startDate,
    @RequestParam(required = false) LocalDateTime endDate,
    Pageable pageable
)

@GetMapping("/my-transactions/sent")
@PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
Page<TransactionDTO> getMySentTransactions(Pageable pageable)

@GetMapping("/my-transactions/received")
@PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
Page<TransactionDTO> getMyReceivedTransactions(Pageable pageable)
```

##### B. Merchant reports (MERCHANT only)
```java
// MerchantReportController - nouveau controller
@RestController
@RequestMapping("/api/transactions/merchant")
public class MerchantReportController {
    
    @GetMapping("/sales")
    @PreAuthorize("hasRole('MERCHANT')")
    Page<TransactionDTO> getMySales(
        @RequestParam(required = false) LocalDateTime startDate,
        @RequestParam(required = false) LocalDateTime endDate,
        Pageable pageable
    )
    
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('MERCHANT')")
    MerchantAnalyticsDTO getMyAnalytics(
        @RequestParam(required = false) LocalDateTime startDate,
        @RequestParam(required = false) LocalDateTime endDate
    )
    
    @GetMapping("/report")
    @PreAuthorize("hasRole('MERCHANT')")
    MerchantReportDTO getMyReport(
        @RequestParam LocalDateTime startDate,
        @RequestParam LocalDateTime endDate
    )
}
```

**Fichiers à créer:**
- `MerchantReportController.java`
- `MerchantReportService.java`
- `MerchantReportDTO.java`
- `MerchantAnalyticsDTO.java`

**Fichiers à modifier:**
- `TransactionController.java` (ajouter 3 endpoints)
- `TransactionService.java` (ajouter méthodes)
- `TransactionServiceImpl.java` (implémentation)

**Temps estimé:** 2 jours  
**Dépendances:** Aucune - repositories existent déjà

---

#### 2.2 Implémenter RecurringTransaction Service & Controller (LOT 3)
**Pourquoi prioritaire:** Fonctionnalité demandée par utilisateurs (paiements récurrents)

**Implémentation:**

##### A. RecurringTransactionService
```java
@Service
public class RecurringTransactionService {
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
    void processRecurringTransaction(Long id);
    void processDueRecurringTransactions(); // Called by scheduler
    
    // History
    Page<RecurringTransactionExecutionDTO> getExecutionHistory(Long id, Pageable pageable);
}
```

##### B. RecurringTransactionController
```java
@RestController
@RequestMapping("/api/transactions/recurring")
public class RecurringTransactionController {
    @PostMapping
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    RecurringTransactionDTO create(@RequestBody RecurringTransactionRequest request);
    
    @GetMapping
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    Page<RecurringTransactionDTO> findMine(Pageable pageable);
    
    @GetMapping("/{id}")
    @PreAuthorize("@recurringTxSecurity.isOwner(#id) or hasRole('ADMIN')")
    RecurringTransactionDTO findById(@PathVariable Long id);
    
    @PutMapping("/{id}")
    @PreAuthorize("@recurringTxSecurity.isOwner(#id)")
    RecurringTransactionDTO update(@PathVariable Long id, @RequestBody RecurringTransactionRequest request);
    
    @DeleteMapping("/{id}")
    @PreAuthorize("@recurringTxSecurity.isOwner(#id)")
    void cancel(@PathVariable Long id, @RequestBody CancelRequest request);
    
    @PostMapping("/{id}/pause")
    @PreAuthorize("@recurringTxSecurity.isOwner(#id)")
    void pause(@PathVariable Long id);
    
    @PostMapping("/{id}/resume")
    @PreAuthorize("@recurringTxSecurity.isOwner(#id)")
    void resume(@PathVariable Long id);
    
    @GetMapping("/{id}/history")
    @PreAuthorize("@recurringTxSecurity.isOwner(#id) or hasRole('ADMIN')")
    Page<RecurringTransactionExecutionDTO> getHistory(@PathVariable Long id, Pageable pageable);
    
    // Admin endpoints
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    Page<RecurringTransactionDTO> findAll(Pageable pageable);
    
    @PostMapping("/{id}/execute-now")
    @PreAuthorize("hasRole('ADMIN')")
    void executeNow(@PathVariable Long id);
}
```

##### C. RecurringTransactionSecurityService
```java
@Service
public class RecurringTransactionSecurityService {
    public boolean isOwner(Long recurringTransactionId) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        RecurringTransaction transaction = repository.findById(recurringTransactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));
        return transaction.getRequesterUserId().equals(currentUserId);
    }
}
```

**Fichiers à créer:**
- `RecurringTransactionService.java` + `Impl`
- `RecurringTransactionController.java`
- `RecurringTransactionSecurityService.java`
- `RecurringTransactionRequest.java`
- `RecurringTransactionDTO.java`
- `RecurringTransactionExecutionDTO.java`

**Temps estimé:** 3 jours  
**Dépendances:** Entity + Repository existent déjà

---

#### 2.3 Finaliser ScheduledTransaction (LOT 3)
**Pourquoi prioritaire:** Infrastructure similaire à RecurringTransaction

**Corrections et ajouts:**

##### A. Corriger endpoints existants
- ✅ Rôles corrigés (REGULAR, MERCHANT, ADMIN)
- ✅ Custom security ajouté (@scheduledTxSecurity.isOwner)
- [ ] Ajouter pagination sur GET /scheduled
- [ ] Ajouter filtres (status, date range)

##### B. Ajouter endpoints manquants
```java
// ScheduledTransactionController - ajouts
@PutMapping("/{id}")
@PreAuthorize("@scheduledTxSecurity.isOwner(#id)")
ScheduledTransactionResponse update(@PathVariable Long id, @RequestBody ScheduledTransactionRequest request);

@PostMapping("/{id}/execute-now")
@PreAuthorize("hasRole('ADMIN')")
void executeNow(@PathVariable Long id);
```

##### C. Implémenter execution logic dans service
```java
// ScheduledTransactionService - ajouts
void processDueScheduledTransactions(); // Called by scheduler
void executeScheduledTransaction(Long id);
```

**Fichiers à modifier:**
- `ScheduledTransactionController.java`
- `ScheduledTransactionService.java`

**Temps estimé:** 1 jour  
**Dépendances:** Tout existe déjà

---

### PRIORITÉ 3 (MOYENNE) 🟡 - Fonctionnalités Importantes

#### 3.1 Implémenter Background Jobs (LOT 3)
**Pourquoi important:** Scheduled/Recurring transactions ne fonctionnent pas sans jobs

**Implémentation:**

##### A. ScheduledTransactionJob
```java
@Component
public class ScheduledTransactionJob {
    
    @Scheduled(cron = "0 * * * * *") // Every minute
    public void processScheduledTransactions() {
        log.info("Processing due scheduled transactions...");
        scheduledTransactionService.processDueScheduledTransactions();
    }
}
```

##### B. RecurringTransactionJob
```java
@Component
public class RecurringTransactionJob {
    
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void processRecurringTransactions() {
        log.info("Processing due recurring transactions...");
        recurringTransactionService.processDueRecurringTransactions();
    }
}
```

**Fichiers à créer:**
- `ScheduledTransactionJob.java`
- `RecurringTransactionJob.java`

**Configuration:**
```yaml
# application.yml
spring:
  task:
    scheduling:
      pool:
        size: 5
```

**Temps estimé:** 1 jour  
**Dépendances:** Services doivent être implémentés d'abord

---

#### 3.2 Implémenter Dispute System (LOT 5)
**Pourquoi important:** Gestion des litiges essentielle pour confiance utilisateurs

**Implémentation:**

##### A. DisputeService
```java
@Service
public class DisputeService {
    // CRUD
    DisputeDTO createDispute(CreateDisputeRequest request);
    DisputeDTO findById(Long id);
    Page<DisputeDTO> findByUserId(Long userId, Pageable pageable);
    
    // Actions
    void addEvidence(Long disputeId, AddEvidenceRequest request);
    void respondToDispute(Long disputeId, RespondDisputeRequest request);
    void resolveDispute(Long disputeId, ResolveDisputeRequest request); // ADMIN only
    
    // Workflow
    void updateDisputeStatus(Long disputeId, DisputeStatus newStatus);
}
```

##### B. DisputeController
```java
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {
    @PostMapping
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    DisputeDTO createDispute(@RequestBody CreateDisputeRequest request);
    
    @GetMapping("/{id}")
    @PreAuthorize("@disputeSecurity.canView(#id)")
    DisputeDTO findById(@PathVariable Long id);
    
    @GetMapping
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    Page<DisputeDTO> findMine(Pageable pageable);
    
    @PostMapping("/{id}/evidence")
    @PreAuthorize("@disputeSecurity.canAddEvidence(#id)")
    void addEvidence(@PathVariable Long id, @RequestBody AddEvidenceRequest request);
    
    @PostMapping("/{id}/respond")
    @PreAuthorize("@disputeSecurity.canRespond(#id)")
    void respond(@PathVariable Long id, @RequestBody RespondDisputeRequest request);
    
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    void resolve(@PathVariable Long id, @RequestBody ResolveDisputeRequest request);
}
```

##### C. DisputeSecurityService
```java
@Service
public class DisputeSecurityService {
    public boolean canView(Long disputeId) {
        // ADMIN or participant (complainant or merchant)
    }
    
    public boolean canAddEvidence(Long disputeId) {
        // Complainant or merchant (not resolved yet)
    }
    
    public boolean canRespond(Long disputeId) {
        // Merchant only (not resolved yet)
    }
}
```

**Fichiers à créer:**
- `DisputeService.java` + `Impl`
- `DisputeController.java`
- `DisputeSecurityService.java`
- `CreateDisputeRequest.java`
- `DisputeDTO.java`
- `AddEvidenceRequest.java`
- `RespondDisputeRequest.java`
- `ResolveDisputeRequest.java`

**Temps estimé:** 3 jours  
**Dépendances:** Entities existent déjà

---

### PRIORITÉ 4 (BASSE) 🟢 - Fonctionnalités Avancées

#### 4.1 FX & Multi-devise (LOT 6)
**Pourquoi plus tard:** Non critique pour MVP, complexe

**À implémenter:**
- ExchangeRateController
- ExchangeRateService
- Provider integration (external API)
- Conversion logic dans transactions

**Temps estimé:** 5 jours  
**Report:** Après LOTs 3, 4, 5 complets

---

## 📋 PLAN D'EXÉCUTION DÉTAILLÉ

### PHASE A - Validation Core (Semaine 1)
**Durée:** 3 jours

1. **Jour 1:** Tests d'intégration LOT 1
   - Transaction Service: tous endpoints LOT 1
   - Wallet Service: tous endpoints LOT 1
   - Validation security guards

2. **Jour 2:** Tests d'intégration LOT 2
   - Bulk transfers
   - Split payments
   - Tests atomicité

3. **Jour 3:** Tests end-to-end
   - Registration → Wallet → Transaction
   - Merchant payment flow
   - Refund flow
   - Documentation des problèmes trouvés

**Livrables:**
- ✅ Rapport de tests
- ✅ Liste des bugs trouvés
- ✅ Core validé à 100%

---

### PHASE B - Search & Reporting (Semaine 1-2)
**Durée:** 2 jours

1. **Jour 4:** User-scoped transactions
   - Créer 3 endpoints dans TransactionController
   - Implémenter dans TransactionService
   - Tester avec Postman

2. **Jour 5:** Merchant reports
   - Créer MerchantReportController
   - Créer MerchantReportService
   - Implémenter analytics
   - Tester avec Postman

**Livrables:**
- ✅ Endpoints my-transactions fonctionnels
- ✅ Merchant reports/analytics fonctionnels
- ✅ LOT 4 à 100%

---

### PHASE C - Recurring Transactions (Semaine 2)
**Durée:** 3 jours

1. **Jour 6:** Service layer
   - Créer RecurringTransactionService
   - Implémenter CRUD + actions
   - Implémenter execution logic

2. **Jour 7:** Controller + Security
   - Créer RecurringTransactionController
   - Créer RecurringTransactionSecurityService
   - Implémenter 10 endpoints
   - Tester avec Postman

3. **Jour 8:** DTOs + Tests
   - Créer tous les DTOs
   - Tests unitaires service
   - Tests intégration controller

**Livrables:**
- ✅ RecurringTransaction API complet
- ✅ Tests passent
- ✅ Documentation Postman

---

### PHASE D - Scheduled Transactions Finalization (Semaine 2)
**Durée:** 1 jour

1. **Jour 9:** Corrections et ajouts
   - Ajouter pagination/filtres
   - Créer PUT endpoint
   - Créer POST execute-now endpoint
   - Implémenter execution logic
   - Tests

**Livrables:**
- ✅ ScheduledTransaction à 100%
- ✅ Tests passent

---

### PHASE E - Background Jobs (Semaine 3)
**Durée:** 1 jour

1. **Jour 10:** Schedulers
   - Créer ScheduledTransactionJob
   - Créer RecurringTransactionJob
   - Configuration Spring Task Scheduler
   - Tests execution automatique
   - Logging et monitoring

**Livrables:**
- ✅ Jobs fonctionnels
- ✅ Transactions exécutées automatiquement
- ✅ LOT 3 à 100%

---

### PHASE F - Dispute System (Semaine 3)
**Durée:** 3 jours

1. **Jour 11:** Service layer
   - Créer DisputeService
   - Implémenter workflow
   - Implémenter status transitions

2. **Jour 12:** Controller + Security
   - Créer DisputeController
   - Créer DisputeSecurityService
   - Implémenter 6 endpoints

3. **Jour 13:** DTOs + Tests
   - Créer tous les DTOs
   - Tests unitaires
   - Tests intégration
   - Tests workflow complet

**Livrables:**
- ✅ Dispute system complet
- ✅ LOT 5 à 100%
- ✅ Tests passent

---

### PHASE G - Tests Finaux (Semaine 4)
**Durée:** 2 jours

1. **Jour 14:** Tests d'intégration globaux
   - Tous les LOTs ensemble
   - Scénarios complexes
   - Performance tests

2. **Jour 15:** Documentation
   - Postman collection complète
   - README mis à jour
   - API documentation (Swagger)

**Livrables:**
- ✅ Tous les LOTs (1-5) à 100%
- ✅ Documentation complète
- ✅ Prêt pour production

---

## 📊 RÉSUMÉ DES PRIORITÉS

| LOT | Status Actuel | Priorité | Durée | Semaine |
|-----|---------------|----------|-------|---------|
| LOT 1 & 2 Validation | 100% | 🔴 P1 | 3j | 1 |
| LOT 4 - Search/Reporting | 60% | 🟠 P2 | 2j | 1-2 |
| LOT 3 - RecurringTransaction | 50% | 🟠 P2 | 3j | 2 |
| LOT 3 - ScheduledTransaction | 80% | 🟠 P2 | 1j | 2 |
| LOT 3 - Background Jobs | 0% | 🟡 P3 | 1j | 3 |
| LOT 5 - Disputes | 40% | 🟡 P3 | 3j | 3 |
| LOT 6 - FX | 0% | 🟢 P4 | 5j | Future |

**TOTAL:** ~15 jours de travail effectif (3-4 semaines calendaires)

---

## 🎯 SUCCESS CRITERIA

### Core Fonctionnel ✅
- [ ] Tous les endpoints LOT 1 & 2 testés et validés
- [ ] Tests d'intégration passent à 100%
- [ ] Security guards fonctionnent correctement
- [ ] Audit trail complet (TransactionStateHistory)

### Fonctionnalités Essentielles ✅
- [ ] Users peuvent voir leurs transactions (LOT 4)
- [ ] Merchants ont des rapports de ventes (LOT 4)
- [ ] Recurring transactions fonctionnent (LOT 3)
- [ ] Scheduled transactions fonctionnent (LOT 3)
- [ ] Background jobs exécutent automatiquement (LOT 3)

### Dispute System ✅
- [ ] Users peuvent créer des disputes
- [ ] Merchants peuvent répondre
- [ ] Admins peuvent arbitrer
- [ ] Workflow complet fonctionne

### Documentation ✅
- [ ] Postman collection complète
- [ ] README à jour
- [ ] Swagger/OpenAPI documentation
- [ ] Tests documentation

---

## 🚀 PROCHAINES ÉTAPES IMMÉDIATES

### ACTION IMMÉDIATE (Aujourd'hui)
1. **Valider le plan avec l'équipe**
2. **Commencer PHASE A - Tests d'intégration LOT 1**
   - Setup environnement de test
   - Créer collection Postman
   - Tester tous les endpoints Transaction Service LOT 1

### DEMAIN
3. **Continuer PHASE A - Tests LOT 2**
4. **Documenter les résultats**

---

## 📝 NOTES

### Décisions Architecturales
- Background jobs: Spring @Scheduled (simple, pas besoin de Quartz pour MVP)
- Security: Custom expressions pour ownership (@txSecurity, @walletSecurity, @scheduledTxSecurity, @recurringTxSecurity, @disputeSecurity)
- Pagination: Spring Data Page<T> partout
- DTOs: Mapper pattern (manuel ou MapStruct)

### Risques Identifiés
- **Duplication ScheduledTransaction entity** (model/ vs model/entities/) → À résoudre en PHASE D
- **Background jobs performance** → Monitoring en PHASE E
- **Dispute workflow complexité** → Tests exhaustifs en PHASE F

### Dépendances Externes
- Kafka: déjà setup pour notifications
- Database: PostgreSQL - schemas existent
- JWT: auth-service opérationnel

---

**Statut**: 📋 PLAN APPROUVÉ - EN ATTENTE D'EXÉCUTION  
**Next Action**: PHASE A - Validation Core (Tests d'intégration LOT 1)
