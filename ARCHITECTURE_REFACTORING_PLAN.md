# 🏗️ Plan de Refactoring Architecture - Zaphira Platform

**Date:** 3 Février 2026  
**Objectif:** Architecture légère et modulaire avec Transaction Core  
**Principe:** Fonctionnalités basiques d'abord, extensions progressives

---

## 🎯 Vision Architecturale

### Principe CORE + EXTENSIONS
```
┌─────────────────────────────────────────┐
│         TRANSACTION CORE (Léger)        │
│  - Transfert P2P basique                │
│  - Dépôt/Retrait simple                 │
│  - États: PENDING → COMPLETED/FAILED    │
│  - Validation minimale                  │
└─────────────────────────────────────────┘
              ▲
              │ extends
    ┌─────────┴─────────┐
    │                   │
┌───▼────────┐  ┌───────▼──────┐
│ ADVANCED   │  │  MERCHANT    │
│ Features   │  │  Features    │
│            │  │              │
│- Scheduled │  │- Payment     │
│- Recurring │  │- Settlement  │
│- Bulk      │  │- Analytics   │
└────────────┘  └──────────────┘
```

---

## 📦 NOUVELLE STRUCTURE DE LOTS

### ✅ LOT 0 - INFRASTRUCTURE CORE (Obligatoire - Déjà fait)
**Services:** auth, user-service, wallet-service, api-gateway, config-server, service-registry  
**Technologies:** PostgreSQL, Kafka, JWT, Spring Security  
**Status:** ✅ COMPLET

#### Fonctionnalités Actives:
- ✅ Authentification JWT avec rôles (USER, MERCHANT, ADMIN)
- ✅ Gestion utilisateurs (REGULAR/MERCHANT)
- ✅ Wallets de base avec balance
- ✅ API Gateway avec routing
- ✅ Kafka pour événements

---

### 🔥 LOT 1 - TRANSACTION CORE (Minimal Viable)
**Priorité:** CRITIQUE  
**Dépendances:** LOT 0  
**Cible:** Permettre les transactions de base sans complexité

#### 1.1 Transaction Entity Core
```java
@Entity
@Table(name = "transactions")
public class TransactionCore {
    // IDs & Référence
    private Long id;
    private String reference;
    
    // Acteurs (simplifié)
    private Long senderWalletId;
    private Long receiverWalletId;
    
    // Montant
    private BigDecimal amount;
    private Currency currency;
    
    // Type & Statut
    private TransactionType type;  // TRANSFER, DEPOSIT, WITHDRAWAL
    private TransactionStatus status;  // PENDING, COMPLETED, FAILED
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
```

**Supprimé du Core:**
- ❌ TransactionCategory (trop complexe)
- ❌ TransactionChannel (pas nécessaire au début)
- ❌ Fees (ajouté en LOT 2)
- ❌ Metadata, Risk, Timeline (ajoutés en LOT 3+)
- ❌ Scheduled, Refunded, Reversed flags

#### 1.2 API Endpoints Core
```
POST   /api/v1/transactions/transfer       # P2P Transfer
POST   /api/v1/transactions/deposit        # Dépôt
POST   /api/v1/transactions/withdrawal     # Retrait
GET    /api/v1/transactions/{ref}          # Consulter
GET    /api/v1/transactions/wallet/{id}    # Historique wallet
```

**Permissions:**
- REGULAR: Peut faire transfer/deposit/withdrawal sur ses wallets
- MERCHANT: Idem REGULAR
- ADMIN: Lecture de toutes transactions

#### 1.3 Services Core
- ✅ TransactionServiceCore (créer, lister, consulter)
- ✅ TransactionValidationService (validations basiques)
- ✅ WalletClient (appel sync au wallet-service)

**Workflow simplifié:**
```
1. Valider montant > 0
2. Vérifier balance sender
3. Bloquer fonds (wallet-service)
4. Créer transaction PENDING
5. Débiter sender / Créditer receiver
6. Marquer COMPLETED
7. Publier event Kafka
```

---

### 🚀 LOT 2 - FEATURES ESSENTIELLES
**Priorité:** HAUTE  
**Dépendances:** LOT 1  
**Ajouts:** Fees, Merchant Payments, Cancel/Retry

#### 2.1 Extensions Transaction
```java
@Entity
@Table(name = "transaction_fees")
public class TransactionFees {
    private Long transactionId;
    private BigDecimal feeAmount;
    private BigDecimal platformFee;
    private BigDecimal merchantFee;
}
```

#### 2.2 Nouveaux Endpoints
```
POST   /api/v1/transactions/merchant-payment   # Paiement marchand
POST   /api/v1/transactions/{ref}/cancel       # Annuler
POST   /api/v1/transactions/{ref}/retry        # Réessayer
```

#### 2.3 Merchant Features
- ✅ Accepter paiements
- ✅ Calcul commissions
- ✅ Balance marchande séparée

---

### ⚡ LOT 3 - SCHEDULING & AUTOMATION
**Priorité:** MOYENNE  
**Dépendances:** LOT 2  
**Note:** Déjà partiellement implémenté mais à refactorer

#### 3.1 Scheduled Transactions (À refactorer)
**Fichiers existants à nettoyer:**
- ⚠️ Duplicate: `model/ScheduledTransaction.java` vs `model/entities/ScheduledTransaction.java`
- ✅ Service, Controller, Repository existent

**À corriger:**
```java
// AVANT (Mauvais)
@PreAuthorize("hasAnyRole('USER','MERCHANT','ADMIN')")

// APRÈS (Correct)
@PreAuthorize("hasAnyRole('REGULAR','MERCHANT','ADMIN')")
@PreAuthorize("@scheduledTxSecurity.isOwner(#id)")
```

#### 3.2 Nouveaux Endpoints
```
POST   /api/v1/scheduled-transactions           # Créer planifié
GET    /api/v1/scheduled-transactions           # Lister (avec pagination)
GET    /api/v1/scheduled-transactions/{id}      # Détails
PUT    /api/v1/scheduled-transactions/{id}      # Modifier
DELETE /api/v1/scheduled-transactions/{id}      # Annuler
POST   /api/v1/scheduled-transactions/{id}/run  # Exécuter manuellement
```

#### 3.3 Recurring Transactions (Nouveau)
```java
@Entity
public class RecurringTransaction {
    private Long id;
    private Long userId;
    private TransactionType type;
    private BigDecimal amount;
    private RecurrencePattern pattern;  // DAILY, WEEKLY, MONTHLY
    private LocalDateTime nextExecution;
    private LocalDateTime endDate;
    private RecurringStatus status;
}
```

#### 3.4 Batch Processing
- ✅ Spring Batch job pour scheduled transactions
- ✅ Cron job pour recurring
- ✅ Retry logic pour failed

---

### 📊 LOT 4 - ANALYTICS & REPORTING LIGHT
**Priorité:** MOYENNE  
**Dépendances:** LOT 2

#### 4.1 Merchant Analytics
```
GET /api/v1/merchants/{id}/analytics      # Stats marchand
GET /api/v1/merchants/{id}/sales          # Ventes
GET /api/v1/merchants/{id}/settlements    # Règlements
```

#### 4.2 Transaction Search
```
GET /api/v1/transactions/search?status=...&from=...&to=...
```

**Implémentation:**
- ✅ Query basique avec filtres
- ❌ Pas de ElasticSearch au début
- ❌ Pagination obligatoire

---

### 🛡️ LOT 5 - DISPUTES & ADVANCED
**Priorité:** BASSE  
**Dépendances:** LOT 3  
**Note:** Partiellement implémenté

#### 5.1 Disputes (Déjà en place)
```
POST   /api/v1/disputes                    # Créer dispute
POST   /api/v1/disputes/{id}/evidence      # Soumettre preuve
GET    /api/v1/disputes/{id}               # Consulter
PUT    /api/v1/disputes/{id}/resolve       # Résoudre (ADMIN)
```

**À vérifier:**
- ✅ DisputeService.createDispute existe
- ✅ DisputeController existe
- ⚠️ Tester workflow complet

#### 5.2 Refunds & Reversals
```
POST   /api/v1/transactions/{ref}/refund   # Remboursement
POST   /api/v1/transactions/{ref}/reverse  # Inversion
```

---

### 💱 LOT 6 - MULTI-CURRENCY & FX
**Priorité:** BASSE  
**Dépendances:** LOT 2

#### 6.1 Exchange Rates
```java
@Entity
public class ExchangeRate {
    private Currency fromCurrency;
    private Currency toCurrency;
    private BigDecimal rate;
    private LocalDateTime effectiveAt;
}
```

#### 6.2 Cross-Currency Transactions
- Conversion automatique
- Calcul taux en temps réel
- Historique taux

---

## 🗂️ NOUVELLE STRUCTURE FICHIERS

### Transaction Service Refactoré
```
transaction-service/
├── model/
│   ├── core/
│   │   └── TransactionCore.java              # LOT 1
│   ├── entities/
│   │   ├── TransactionFees.java              # LOT 2
│   │   ├── TransactionMetadata.java          # LOT 3
│   │   ├── TransactionTimeline.java          # LOT 5
│   │   ├── TransactionRisk.java              # LOT 5
│   │   └── TransactionRetry.java             # LOT 2
│   ├── scheduled/
│   │   ├── ScheduledTransaction.java         # LOT 3
│   │   └── RecurringTransaction.java         # LOT 3
│   └── dispute/
│       ├── Dispute.java                      # LOT 5
│       └── DisputeEvidence.java              # LOT 5
│
├── service/
│   ├── core/
│   │   ├── TransactionCoreService.java       # LOT 1
│   │   └── TransactionValidationService.java # LOT 1
│   ├── advanced/
│   │   ├── ScheduledTransactionService.java  # LOT 3
│   │   ├── RecurringTransactionService.java  # LOT 3
│   │   └── BulkTransactionService.java       # LOT 2
│   ├── merchant/
│   │   ├── MerchantPaymentService.java       # LOT 2
│   │   └── MerchantAnalyticsService.java     # LOT 4
│   └── dispute/
│       └── DisputeService.java               # LOT 5
│
└── controller/
    ├── TransactionCoreController.java        # LOT 1
    ├── ScheduledTransactionController.java   # LOT 3
    ├── MerchantController.java               # LOT 2
    └── DisputeController.java                # LOT 5
```

---

## 🚦 ROADMAP D'IMPLÉMENTATION

### Phase 1 - Foundation (2-3 jours)
1. ✅ Refactorer Transaction en TransactionCore
2. ✅ Simplifier endpoints au strict minimum
3. ✅ Tester workflows: transfer, deposit, withdrawal
4. ✅ Vérifier intégration wallet-service

### Phase 2 - Essential Features (3-4 jours)
1. ✅ Ajouter TransactionFees
2. ✅ Implémenter merchant-payment
3. ✅ Ajouter cancel/retry
4. ✅ Tests E2E complets

### Phase 3 - Automation (3-4 jours)
1. ⚠️ Nettoyer duplication ScheduledTransaction
2. ⚠️ Corriger security guards
3. ✅ Implémenter RecurringTransaction
4. ✅ Spring Batch jobs

### Phase 4 - Analytics (2-3 jours)
1. ✅ Merchant analytics endpoints
2. ✅ Transaction search
3. ✅ Basic reporting

### Phase 5 - Advanced (À prioriser)
1. ⚠️ Finaliser Disputes
2. ❌ Refunds & Reversals
3. ❌ Multi-currency

---

## 📋 CHECKLIST DE DÉMARRAGE

### Étape 1: Audit du code existant
- [x] Identifier toutes les entités Transaction*
- [x] Lister tous les services Transaction*
- [x] Mapper tous les endpoints
- [x] Vérifier les duplications

### Étape 2: Créer Transaction Core
- [ ] Créer `TransactionCore.java` (champs minimaux)
- [ ] Créer `TransactionCoreService.java`
- [ ] Créer `TransactionCoreController.java`
- [ ] Migrer logique essentielle

### Étape 3: Tester LOT 1
- [ ] Test: P2P Transfer
- [ ] Test: Deposit
- [ ] Test: Withdrawal
- [ ] Test: Get by reference
- [ ] Test: Wallet history

### Étape 4: Documenter
- [ ] API documentation (Swagger)
- [ ] Diagrammes de séquence
- [ ] Guide d'utilisation

---

## 🎯 CRITÈRES DE SUCCÈS

### LOT 1 (Core)
✅ Les 3 types de transactions fonctionnent  
✅ Balance correcte après chaque transaction  
✅ Security guards appliqués  
✅ Events Kafka publiés  
✅ Temps de réponse < 500ms  

### LOT 2 (Features)
✅ Merchant peut accepter paiements  
✅ Fees calculés correctement  
✅ Cancel/Retry fonctionnels  

### LOT 3 (Scheduling)
✅ Scheduled transaction créé et exécuté  
✅ Recurring transaction répété correctement  
✅ Batch job sans erreur  

---

## 📌 NOTES IMPORTANTES

### Ce qui EXISTE déjà et qu'on GARDE:
- ✅ User Service complet
- ✅ Wallet Service complet
- ✅ Auth Service avec JWT
- ✅ API Gateway avec routing
- ✅ Kafka infrastructure
- ✅ PostgreSQL schemas

### Ce qu'on SIMPLIFIE:
- 🔄 Transaction (trop de champs)
- 🔄 ScheduledTransaction (duplic + security)
- 🔄 Services trop couplés

### Ce qu'on GARDE tel quel:
- ✅ Dispute (bien implémenté)
- ✅ TransactionFees
- ✅ Validation services

---

**Prochaine étape:** Créer `TransactionCore.java` et migrer la logique essentielle

