# 📊 Liste des Lots Mis à Jour - Architecture Simplifiée

**Date:** 3 Février 2026  
**Version:** 2.0 - Architecture Core + Extensions

---

## 🎯 NOUVEAUX LOTS FONCTIONNELS

### ✅ LOT 0 - INFRASTRUCTURE (Déjà complet)
**Services actifs:** 6/6
- ✅ auth-service (JWT, Login, Roles)
- ✅ user-service (Users, KYC, Merchant)
- ✅ wallet-service (Wallets, Balance, SubWallets)
- ✅ api-gateway (Routing, JWT validation)
- ✅ config-server (Configuration centralisée)
- ✅ service-registry (Eureka)

**Technologies:**
- PostgreSQL (9 databases)
- Kafka (événements asynchrones)
- Spring Boot 3.x
- Spring Security avec JWT
- MapStruct pour mapping

**Fonctionnalités disponibles:**
- Authentification complète (OTP, Email)
- Gestion utilisateurs REGULAR/MERCHANT/ADMIN
- Wallets avec balance et permissions
- API Gateway sécurisé

---

### 🔥 LOT 1 - TRANSACTION CORE (Nouveau - À implémenter)
**Priorité:** CRITIQUE  
**Durée estimée:** 2-3 jours  
**Dépendances:** LOT 0

#### Objectif
Créer une base transactionnelle légère et fonctionnelle permettant:
- Transferts P2P
- Dépôts
- Retraits

#### Fichiers créés
```
✅ TransactionCore.java              # Entity minimale
✅ TransactionCoreDTO.java           # DTO response
✅ CreateTransactionCoreRequest.java # DTO request
⏳ TransactionCoreRepository.java    # Repository
⏳ TransactionCoreService.java       # Service métier
⏳ TransactionCoreController.java    # API endpoints
⏳ TransactionCoreMapper.java        # MapStruct mapper
```

#### Endpoints à créer
```http
POST   /api/v1/transactions/transfer       # Transfert P2P
POST   /api/v1/transactions/deposit        # Dépôt
POST   /api/v1/transactions/withdrawal     # Retrait
GET    /api/v1/transactions/{ref}          # Consulter une transaction
GET    /api/v1/transactions/wallet/{id}    # Historique d'un wallet
```

#### Workflow simplifié
```
1. Valider montant > 0
2. Vérifier balance suffisante (appel wallet-service)
3. Créer transaction status=PENDING
4. Appeler wallet-service pour débiter/créditer
5. Mettre à jour status=COMPLETED ou FAILED
6. Publier event Kafka
```

#### Tests requis
- [ ] Test: Transfer entre 2 wallets REGULAR
- [ ] Test: Deposit sur wallet MERCHANT
- [ ] Test: Withdrawal d'un wallet
- [ ] Test: Historique wallet avec pagination
- [ ] Test: Permissions (USER vs ADMIN)

---

### 🚀 LOT 2 - FEATURES ESSENTIELLES (À implémenter après LOT 1)
**Priorité:** HAUTE  
**Durée estimée:** 3-4 jours  
**Dépendances:** LOT 1

#### Fonctionnalités ajoutées

##### 2.1 Transaction Fees
```java
@Entity
@Table(name = "transaction_fees")
public class TransactionFees {
    private Long transactionId;
    private BigDecimal platformFee;
    private BigDecimal merchantFee;
    private BigDecimal totalFees;
}
```

**Calcul:**
- Transfer: 0.5% plateforme
- Merchant Payment: 2% marchand + 0.5% plateforme
- Deposit: 0€
- Withdrawal: 1€ fixe

##### 2.2 Merchant Payments
```http
POST /api/v1/transactions/merchant-payment
{
  "senderWalletId": 1,
  "merchantWalletId": 5,
  "amount": 100.00,
  "description": "Achat produit"
}
```

##### 2.3 Cancel & Retry
```http
POST /api/v1/transactions/{ref}/cancel
POST /api/v1/transactions/{ref}/retry
```

**Règles:**
- Cancel: Uniquement si status=PENDING
- Retry: Uniquement si status=FAILED et < 3 tentatives

#### Extensions base de données
```sql
-- Ajouter colonne fees dans transactions_core
ALTER TABLE transactions_core ADD COLUMN fee_amount DECIMAL(19,4);
ALTER TABLE transactions_core ADD COLUMN total_amount DECIMAL(19,4);

-- Table fees détaillées
CREATE TABLE transaction_fees (
    transaction_id BIGINT PRIMARY KEY REFERENCES transactions_core(id),
    platform_fee DECIMAL(19,4),
    merchant_fee DECIMAL(19,4),
    total_fees DECIMAL(19,4),
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

### ⚡ LOT 3 - SCHEDULING & BATCH (À refactorer)
**Priorité:** MOYENNE  
**Durée estimée:** 3-4 jours  
**Dépendances:** LOT 2

#### État actuel
⚠️ **Partiellement implémenté mais problématique:**
- ✅ Entity ScheduledTransaction existe
- ✅ Controller existe (4 endpoints)
- ✅ Service existe
- ❌ Security guards incorrects (`'USER'` au lieu de `'REGULAR'`)
- ❌ Duplication fichiers (model/ vs model/entities/)
- ❌ Pas de pagination
- ❌ Pas de filtres

#### À corriger

##### 3.1 Nettoyage ScheduledTransaction
```bash
# Supprimer duplication
rm transaction-service/src/.../model/ScheduledTransaction.java
# Garder uniquement model/entities/ScheduledTransaction.java
```

##### 3.2 Corriger Security Guards
```java
// AVANT (Mauvais)
@PreAuthorize("hasAnyRole('USER','MERCHANT','ADMIN')")

// APRÈS (Correct)
@PreAuthorize("hasAnyRole('REGULAR','MERCHANT','ADMIN')")
@PreAuthorize("@scheduledTxSecurity.canView(#id)")
```

##### 3.3 Ajouter pagination
```java
@GetMapping
public ResponseEntity<Page<ScheduledTransactionResponse>> getScheduled(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) ScheduledTransactionStatus status
) {
    // ...
}
```

##### 3.4 Spring Batch Job
```java
@Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
public void executeScheduledTransactions() {
    List<ScheduledTransaction> pending = repo.findPendingTransactions();
    for (ScheduledTransaction st : pending) {
        if (st.getScheduledTime().isBefore(LocalDateTime.now())) {
            executeTransaction(st);
        }
    }
}
```

#### 3.5 Recurring Transactions (Nouveau)
```http
POST /api/v1/recurring-transactions
{
  "senderWalletId": 1,
  "receiverWalletId": 2,
  "amount": 50.00,
  "pattern": "MONTHLY",
  "dayOfMonth": 1,
  "endDate": "2027-01-01"
}
```

---

### 📊 LOT 4 - ANALYTICS & REPORTING (Nouveau)
**Priorité:** MOYENNE  
**Durée estimée:** 2-3 jours  
**Dépendances:** LOT 2

#### Fonctionnalités

##### 4.1 Merchant Analytics
```http
GET /api/v1/merchants/{id}/analytics?from=2026-01-01&to=2026-01-31
```

**Réponse:**
```json
{
  "merchantId": 5,
  "period": { "from": "2026-01-01", "to": "2026-01-31" },
  "totalSales": 15000.00,
  "transactionCount": 145,
  "averageTransaction": 103.45,
  "totalFees": 300.00,
  "topProducts": [...]
}
```

##### 4.2 Transaction Search
```http
GET /api/v1/transactions/search?
    status=COMPLETED&
    from=2026-01-01&
    to=2026-01-31&
    minAmount=100&
    maxAmount=1000&
    page=0&
    size=20
```

##### 4.3 Wallet Statement
```http
GET /api/v1/wallets/{id}/statement?from=...&to=...
```

**Format:** PDF ou JSON
- Liste transactions
- Balance début/fin période
- Total crédits/débits

---

### 🛡️ LOT 5 - DISPUTES & ADVANCED (Existant - À finaliser)
**Priorité:** BASSE  
**Durée estimée:** 2-3 jours  
**Dépendances:** LOT 3

#### État actuel
✅ **Bien implémenté:**
- ✅ Dispute entity avec timeline
- ✅ DisputeService avec méthode createDispute
- ✅ DisputeController avec 5 endpoints
- ✅ Evidence upload support

#### À tester et finaliser

##### 5.1 Workflow complet
```
1. Customer crée dispute
2. Merchant répond avec evidence
3. Customer soumet counter-evidence
4. ADMIN review et résout
```

##### 5.2 Tests E2E
- [ ] Test: Customer crée dispute
- [ ] Test: Merchant répond
- [ ] Test: ADMIN résout
- [ ] Test: Permissions correctes

##### 5.3 Refunds & Reversals
```http
POST /api/v1/transactions/{ref}/refund
POST /api/v1/transactions/{ref}/reverse
```

---

### 💱 LOT 6 - MULTI-CURRENCY & FX (Futur)
**Priorité:** TRÈS BASSE  
**Durée estimée:** 4-5 jours  
**Dépendances:** LOT 2

#### Fonctionnalités futures

##### 6.1 Exchange Rates
```java
@Entity
public class ExchangeRate {
    private Currency from;
    private Currency to;
    private BigDecimal rate;
    private LocalDateTime effectiveAt;
}
```

##### 6.2 Cross-Currency Transaction
```http
POST /api/v1/transactions/exchange
{
  "fromWalletId": 1,
  "toWalletId": 2,
  "fromAmount": 100.00,
  "fromCurrency": "EUR",
  "toCurrency": "USD"
}
```

**Calcul:**
- Récupérer taux en temps réel
- Appliquer spread (ex: 1%)
- Créer 2 transactions liées

---

## 📋 ROADMAP D'IMPLÉMENTATION

### Sprint 1 - LOT 1 Core (Semaine 1)
- [x] Créer TransactionCore entity
- [x] Créer DTOs
- [ ] Créer Repository
- [ ] Créer Service
- [ ] Créer Controller
- [ ] Tests unitaires
- [ ] Tests intégration
- [ ] Documentation API

### Sprint 2 - LOT 2 Features (Semaine 2)
- [ ] Ajouter TransactionFees
- [ ] Implémenter Merchant Payment
- [ ] Ajouter Cancel/Retry
- [ ] Tests E2E
- [ ] Performance tests

### Sprint 3 - LOT 3 Scheduling (Semaine 3)
- [ ] Nettoyer ScheduledTransaction
- [ ] Corriger security
- [ ] Ajouter pagination
- [ ] Batch job
- [ ] RecurringTransaction
- [ ] Tests scheduler

### Sprint 4 - LOT 4 Analytics (Semaine 4)
- [ ] Merchant analytics
- [ ] Transaction search
- [ ] Wallet statements
- [ ] Dashboard API

### Sprint 5 - LOT 5 Disputes (Semaine 5)
- [ ] Tests complets Disputes
- [ ] Refunds
- [ ] Reversals
- [ ] Workflow complet

---

## 🎯 CRITÈRES DE SUCCÈS PAR LOT

### LOT 1 ✅
- [ ] P2P transfer fonctionne
- [ ] Deposit fonctionne
- [ ] Withdrawal fonctionne
- [ ] Balance mise à jour correctement
- [ ] Events Kafka publiés
- [ ] Temps réponse < 500ms
- [ ] Security guards OK

### LOT 2 ✅
- [ ] Fees calculés correctement
- [ ] Merchant payment OK
- [ ] Cancel transaction OK
- [ ] Retry transaction OK
- [ ] Commissions marchands correctes

### LOT 3 ✅
- [ ] Scheduled transaction créé
- [ ] Scheduled transaction exécuté
- [ ] Recurring transaction répété
- [ ] Batch job sans erreur
- [ ] Security guards corrects

### LOT 4 ✅
- [ ] Analytics précis
- [ ] Search performant (<1s)
- [ ] Pagination correcte
- [ ] Export PDF OK

### LOT 5 ✅
- [ ] Dispute créé
- [ ] Evidence soumis
- [ ] Résolution OK
- [ ] Refund OK
- [ ] Reversal OK

---

## 📊 MÉTRIQUES DE PERFORMANCE

### Objectifs par LOT

| LOT | Temps réponse | Throughput | Disponibilité |
|-----|---------------|------------|---------------|
| 1   | < 500ms       | 100 TPS    | 99.9%         |
| 2   | < 800ms       | 80 TPS     | 99.5%         |
| 3   | Batch 1000/min| N/A        | 99%           |
| 4   | < 2s          | 50 TPS     | 99%           |
| 5   | < 1s          | 20 TPS     | 99%           |

---

**Prochaine étape:** Implémenter LOT 1 - TransactionCore Repository et Service

