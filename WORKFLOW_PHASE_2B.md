# WORKFLOW - Phase 2B: Fonctionnalités Avancées
**Date**: 3 Février 2026  
**Status**: 🔄 EN COURS  
**Précédent**: Phase 2A (LOTs critiques) - ✅ 100% COMPLET

---

## 🎯 OBJECTIF PHASE 2B
Implémenter les fonctionnalités avancées pour enrichir l'expérience utilisateur et les capacités business.

---

## 📋 LOTS À IMPLÉMENTER

### 🔄 Transaction LOT 3: Scheduling & Batch - PRIORITÉ HAUTE

#### Fonctionnalités
1. **Scheduled Transactions**
   - Créer une transaction planifiée (future date)
   - Modifier/annuler avant exécution
   - Exécution automatique par job background
   
2. **Recurring Transactions**
   - Créer paiement récurrent (daily/weekly/monthly)
   - Modifier/annuler récurrence
   - Historique des exécutions

3. **Batch Processing**
   - Job scheduler pour exécuter scheduled transactions
   - Status tracking et retry logic
   - Notifications d'échec/succès

#### Endpoints à créer
```
POST   /api/transactions/scheduled          - Créer transaction planifiée (REGULAR/MERCHANT)
GET    /api/transactions/scheduled          - Lister mes transactions planifiées
GET    /api/transactions/scheduled/{id}     - Détails transaction planifiée
PUT    /api/transactions/scheduled/{id}     - Modifier transaction planifiée
DELETE /api/transactions/scheduled/{id}     - Annuler transaction planifiée

POST   /api/transactions/recurring          - Créer paiement récurrent (REGULAR/MERCHANT)
GET    /api/transactions/recurring          - Lister mes récurrences
GET    /api/transactions/recurring/{id}     - Détails récurrence
PUT    /api/transactions/recurring/{id}     - Modifier récurrence
DELETE /api/transactions/recurring/{id}     - Annuler récurrence
GET    /api/transactions/recurring/{id}/history - Historique exécutions

GET    /api/transactions/scheduled/all      - Vue globale (ADMIN)
POST   /api/transactions/scheduled/{id}/execute-now - Forcer exécution (ADMIN)
```

#### Entities existantes
- ✅ ScheduledTransaction entity
- ✅ ScheduledTransactionRepository
- ❌ RecurringTransaction entity (à créer)
- ❌ RecurringTransactionExecution entity (à créer)

#### DTOs à créer
- ScheduledTransactionRequest
- ScheduledTransactionResponse
- RecurringTransactionRequest
- RecurringTransactionResponse
- RecurringExecutionDTO

#### Services à implémenter
- ScheduledTransactionService
- RecurringTransactionService
- TransactionSchedulerJob (Spring @Scheduled)

#### Guards
```java
@PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")  // Create/view own
@PreAuthorize("@scheduledTxSecurity.isOwner(#id)") // Modify/cancel
@PreAuthorize("hasRole('ADMIN')")                   // View all, force execute
```

---

### 💱 Transaction LOT 6: FX & Multi-devise - PRIORITÉ MOYENNE

#### Fonctionnalités
1. **Exchange Rate Management**
   - Fetch rates from providers (API externe)
   - Store et cache rates
   - Refresh automatique (TTL)

2. **Currency Conversion**
   - Appliquer FX rate aux transactions multi-devises
   - Calculer fees FX
   - Historique des conversions

3. **Provider Management**
   - Configure multiple FX providers
   - Fallback strategy
   - Rate comparison

#### Endpoints à créer
```
GET    /api/exchange-rates                  - Lister taux actuels (PUBLIC ou REGULAR/MERCHANT)
GET    /api/exchange-rates/{fromCurrency}/{toCurrency} - Taux spécifique
POST   /api/exchange-rates/refresh          - Refresh rates (ADMIN)
POST   /api/exchange-rates/calculate        - Calculer conversion (REGULAR/MERCHANT)

POST   /api/exchange-rates/providers        - Configure provider (ADMIN)
GET    /api/exchange-rates/providers        - Liste providers (ADMIN)
PUT    /api/exchange-rates/providers/{id}   - Update provider (ADMIN)
DELETE /api/exchange-rates/providers/{id}   - Delete provider (ADMIN)
```

#### Entities existantes
- ✅ ExchangeRate entity
- ✅ ExchangeRateRepository
- ✅ TransactionFees (fxRate, fxFee, fxRateProvider fields)
- ❌ ExchangeRateProvider entity (à créer)

#### DTOs à créer
- ExchangeRateDTO
- ConversionRequest
- ConversionResponse
- ExchangeRateProviderDTO

#### Services à implémenter
- ExchangeRateService
- ExchangeRateProviderService
- FXCalculationService
- ExchangeRateRefreshJob (Spring @Scheduled)

#### Guards
```java
@PreAuthorize("permitAll()")                        // GET rates (public)
@PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")  // Calculate conversion
@PreAuthorize("hasRole('ADMIN')")                   // Manage providers, refresh
```

---

### 📊 Wallet LOT 5: Historique & Statements - PRIORITÉ BASSE

#### Fonctionnalités
1. **Transaction History**
   - Historique complet des mouvements wallet
   - Filtres: date range, type, status
   - Pagination

2. **Statement Generation**
   - Générer statement PDF/HTML pour période
   - Inclure: balance début/fin, transactions, fees
   - Template customizable

3. **Export**
   - Export CSV des transactions
   - Export Excel avec pivot
   - Schedule automatic reports (merchants)

#### Endpoints à créer
```
GET    /api/wallets/{walletNumber}/history         - Historique transactions (Owner/ADMIN)
GET    /api/wallets/{walletNumber}/statement       - Générer statement (Owner/ADMIN)
GET    /api/wallets/{walletNumber}/export          - Export CSV/Excel (Owner/ADMIN)
POST   /api/wallets/{walletNumber}/statement/schedule - Schedule reports (MERCHANT)
```

#### DTOs à créer
- WalletHistoryRequest (filters)
- WalletHistoryResponse
- StatementRequest (date range, format)
- StatementResponse
- ExportRequest (format, filters)

#### Services à implémenter
- WalletHistoryService
- StatementGeneratorService (PDF/HTML)
- ExportService (CSV/Excel)
- ScheduledReportService

#### Guards
```java
@PreAuthorize("@walletSecurity.canView(#walletNumber)") // Owner or ADMIN
@PreAuthorize("hasRole('MERCHANT')")                     // Schedule reports
```

---

## 📊 PRIORISATION

### Sprint 1 (3-5 jours) - Transaction LOT 3
- ✅ Scheduled Transactions (CRUD)
- ✅ Background job execution
- ✅ Recurring Transactions
- ✅ Admin controls

### Sprint 2 (2-3 jours) - Transaction LOT 6
- ✅ Exchange Rate management
- ✅ FX calculation in transactions
- ✅ Provider configuration

### Sprint 3 (2-3 jours) - Wallet LOT 5
- ✅ History avec filtres
- ✅ Statement generation
- ✅ Export CSV/Excel

---

## 🎯 SUCCESS CRITERIA

### Transaction LOT 3
- [ ] User peut créer transaction planifiée
- [ ] Job background exécute transactions à l'heure
- [ ] Recurring transactions s'exécutent automatiquement
- [ ] Admin peut forcer exécution
- [ ] Notifications envoyées sur succès/échec

### Transaction LOT 6
- [ ] Rates refresh automatiquement
- [ ] Transactions multi-devises calculent FX correctement
- [ ] Admin peut configurer providers
- [ ] Fallback fonctionne si provider principal down

### Wallet LOT 5
- [ ] History paginé avec filtres
- [ ] Statement PDF généré correctement
- [ ] Export CSV complet
- [ ] Merchant peut schedule reports

---

## 🔧 TECHNICAL STACK

### Scheduled Jobs
- Spring @Scheduled
- Cron expressions
- Thread pool configuration
- Job status tracking

### PDF Generation
- iText ou Flying Saucer
- HTML → PDF conversion
- Template engine (Thymeleaf)

### CSV/Excel Export
- Apache POI (Excel)
- OpenCSV (CSV)
- Streaming pour large datasets

### FX Rates API
- Options: Fixer.io, ExchangeRate-API, Open Exchange Rates
- Fallback strategy
- Rate caching (Redis?)

---

## 📝 NOTES

### LOT 3 - Considerations
- **Timezone handling**: UTC storage, display in user timezone
- **Retry logic**: Max retries, exponential backoff
- **Concurrency**: Prevent duplicate execution
- **Notifications**: Email/SMS on execution

### LOT 6 - Considerations
- **Rate freshness**: TTL 1 hour typical
- **Provider SLA**: Timeout, retry, circuit breaker
- **Rate accuracy**: Decimal precision (6+ digits)
- **Historical rates**: Store for audit

### LOT 5 - Considerations
- **Performance**: Pagination essential for large datasets
- **Memory**: Stream large exports
- **Security**: Encrypt sensitive data in exports
- **Compliance**: Data retention policies

---

## 🚀 COMMENÇONS PAR LOT 3!

Prêt à implémenter Transaction LOT 3 (Scheduling & Batch)?
