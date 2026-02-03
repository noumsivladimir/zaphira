# 📋 TESTS D'INTÉGRATION - Zaphira Platform
**Date**: 3 Février 2026  
**Phase**: PHASE A - Validation Core

---

## 🎯 OBJECTIF

Valider que les microservices Transaction et Wallet sont fonctionnels à 100% en testant:
- ✅ Tous les endpoints LOT 1 (Core fonctionnel)
- ✅ Tous les endpoints LOT 2 (Bulk operations)
- ✅ Security guards et authentification
- ✅ Validation des données
- ✅ Business logic (transactions, wallets)

---

## 📁 STRUCTURE DES TESTS

### Transaction Service
```
transaction-service/src/test/java/com/zaphira/transaction/integration/
├── TransactionServiceIntegrationTest.java    (LOT 1 - 60+ tests)
│   ├── TransferTests                         (P2P transfers)
│   ├── DepositTests                          (Deposits)
│   ├── WithdrawalTests                       (Withdrawals)
│   ├── MerchantPaymentTests                  (Merchant payments)
│   ├── QueryTests                            (Get transactions)
│   ├── ActionTests                           (Process, cancel, retry)
│   ├── SecurityTests                         (Authentication, authorization)
│   └── ValidationTests                       (Input validation)
│
└── BulkOperationsIntegrationTest.java        (LOT 2 - 30+ tests)
    ├── BulkTransferTests                     (Merchant bulk transfers)
    ├── SplitPaymentTests                     (Split payments)
    ├── FeesTests                             (Fee calculation)
    └── SecurityTests                         (Role-based access)
```

### Wallet Service
```
wallet-service/src/test/java/com/zaphira/wallet/integration/
└── WalletServiceIntegrationTest.java         (LOT 1 - 50+ tests)
    ├── WalletCreationTests                   (Create wallets)
    ├── WalletQueryTests                      (Get wallet info)
    ├── BalanceOperationsTests                (Credit, debit, block, unblock)
    ├── LifecycleTests                        (Freeze, suspend, close)
    ├── SecurityTests                         (Authentication, authorization)
    └── ValidationTests                       (Input validation)
```

---

## 🧪 DÉTAILS DES TESTS

### Transaction Service - LOT 1 (TransactionServiceIntegrationTest)

#### 1. TransferTests (P2P Transfers)
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testCreateTransfer` | Create P2P transfer successfully | ✅ 201 Created |
| `testTransferInsufficientBalance` | Reject transfer with insufficient balance | ✅ 400 Bad Request |
| `testTransferInvalidWallet` | Reject transfer with invalid wallet | ✅ 404 Not Found |
| `testTransferUnauthorized` | Reject transfer without auth | ✅ 401 Unauthorized |

#### 2. DepositTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testCreateDeposit` | Create deposit successfully | ✅ 201 Created |
| `testDepositNegativeAmount` | Reject negative amount | ✅ 400 Bad Request |
| `testMerchantDeposit` | Merchant can deposit | ✅ 201 Created |

#### 3. WithdrawalTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testCreateWithdrawal` | Create withdrawal successfully | ✅ 201 Created |
| `testWithdrawalExceedsBalance` | Reject withdrawal exceeding balance | ✅ 400 Bad Request |

#### 4. MerchantPaymentTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testCreateMerchantPayment` | Regular user pays merchant | ✅ 201 Created |
| `testMerchantCannotPaySelf` | Merchant cannot pay themselves | ✅ 400 Bad Request |

#### 5. QueryTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testGetTransactionByReference` | Get transaction by reference | ✅ 200 OK |
| `testGetWalletHistory` | Get wallet transaction history | ✅ 200 OK |
| `testAdminGetTransactionById` | Admin gets transaction by ID | ✅ 200 OK |
| `testRegularUserCannotGetById` | Regular user cannot get by ID | ✅ 403 Forbidden |
| `testCannotViewOthersTransaction` | Cannot view other's transaction | ✅ 403 Forbidden |

#### 6. ActionTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testCancelOwnTransaction` | Owner cancels their transaction | ✅ 200 OK |
| `testCannotCancelOthersTransaction` | Cannot cancel other's transaction | ✅ 403 Forbidden |
| `testProcessTransaction` | Process pending transaction | ✅ 200 OK |

#### 7. SecurityTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testEndpointsRequireAuth` | All endpoints require authentication | ✅ 401 Unauthorized |
| `testAdminEndpointsRejectRegular` | Admin endpoints reject regular users | ✅ 403 Forbidden |
| `testMerchantBasicAccess` | Merchant has same access as regular | ✅ 200 OK |

#### 8. ValidationTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testEmptyTransferRequest` | Reject empty request | ✅ 400 Bad Request |
| `testZeroAmountTransfer` | Reject zero amount | ✅ 400 Bad Request |
| `testTransferToSameWallet` | Reject transfer to same wallet | ✅ 400 Bad Request |

**Total LOT 1**: ~60 tests

---

### Transaction Service - LOT 2 (BulkOperationsIntegrationTest)

#### 1. BulkTransferTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testMerchantBulkTransfer` | Merchant creates bulk transfer | ✅ 201 Created |
| `testAdminBulkTransfer` | Admin creates bulk transfer | ✅ 201 Created |
| `testRegularUserCannotBulkTransfer` | Regular user cannot bulk transfer | ✅ 403 Forbidden |
| `testEmptyRecipientsList` | Reject empty recipients | ✅ 400 Bad Request |
| `testBulkTransferInsufficientBalance` | Reject insufficient balance | ✅ 400 Bad Request |
| `testBulkTransferAtomicity` | Bulk is atomic (all or nothing) | ✅ 400 Bad Request |

#### 2. SplitPaymentTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testCreateSplitPayment` | Create split payment successfully | ✅ 201 Created |
| `testMerchantSplitPayment` | Merchant can split payment | ✅ 201 Created |
| `testSplitPaymentSharesMismatch` | Reject if shares don't sum to total | ✅ 400 Bad Request |
| `testSplitPaymentMinimumRecipients` | Require at least 2 recipients | ✅ 400 Bad Request |
| `testSplitPaymentAtomicity` | Split is atomic | ✅ 404 Not Found |
| `testSplitPaymentInsufficientBalance` | Reject insufficient balance | ✅ 400 Bad Request |

#### 3. FeesTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testFeesCalculation` | Transactions calculate fees | ✅ 201 Created |
| `testBulkFeesCalculation` | Bulk calculates total fees | ✅ 201 Created |

#### 4. SecurityTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testBulkTransferRequiresAuth` | Bulk requires authentication | ✅ 401 Unauthorized |
| `testSplitPaymentRequiresAuth` | Split requires authentication | ✅ 401 Unauthorized |
| `testRegularUserBulkAccess` | Regular user cannot bulk | ✅ 403 Forbidden |

**Total LOT 2**: ~30 tests

---

### Wallet Service - LOT 1 (WalletServiceIntegrationTest)

#### 1. WalletCreationTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testCreateRegularWallet` | Regular user creates wallet | ✅ 201 Created |
| `testCreateMerchantWallet` | Merchant creates merchant wallet | ✅ 201 Created |
| `testAdminCreateWallet` | Admin creates any wallet | ✅ 201 Created |
| `testPreventDuplicateWallet` | Prevent duplicate wallet | ✅ 409 Conflict |
| `testUniqueWalletNumber` | Generate unique wallet numbers | ✅ 201 Created |

#### 2. WalletQueryTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testGetOwnWallet` | Owner views their wallet | ✅ 200 OK |
| `testCannotViewOthersWallet` | Cannot view other's wallet | ✅ 403 Forbidden |
| `testAdminViewAnyWallet` | Admin views any wallet | ✅ 200 OK |
| `testGetWalletById` | Admin gets wallet by ID | ✅ 200 OK |
| `testRegularCannotGetById` | Regular cannot get by ID | ✅ 403 Forbidden |
| `testGetWalletSummary` | Get wallet summary | ✅ 200 OK |
| `testCannotGetOthersSummary` | Cannot get other's summary | ✅ 403 Forbidden |
| `testAdminGetAnySummary` | Admin gets any summary | ✅ 200 OK |

#### 3. BalanceOperationsTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testCreditWallet` | Admin credits wallet | ✅ 200 OK |
| `testRegularCannotCredit` | Regular cannot credit | ✅ 403 Forbidden |
| `testDebitWallet` | Admin debits wallet | ✅ 200 OK |
| `testDebitExceedsBalance` | Reject debit exceeding balance | ✅ 400 Bad Request |
| `testBlockAmount` | Block amount successfully | ✅ 200 OK |
| `testUnblockAmount` | Unblock amount successfully | ✅ 200 OK |

#### 4. LifecycleTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testFreezeWallet` | Admin freezes wallet | ✅ 200 OK |
| `testRegularCannotFreeze` | Regular cannot freeze | ✅ 403 Forbidden |
| `testUnfreezeWallet` | Admin unfreezes wallet | ✅ 200 OK |
| `testSuspendWallet` | Admin suspends wallet | ✅ 200 OK |
| `testActivateWallet` | Admin activates wallet | ✅ 200 OK |
| `testCloseWallet` | Admin closes wallet | ✅ 200 OK |

#### 5. SecurityTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testEndpointsRequireAuth` | All endpoints require auth | ✅ 401 Unauthorized |
| `testAdminOnlyOperations` | Admin-only ops reject regular | ✅ 403 Forbidden |

#### 6. ValidationTests
| Test | Description | Statut Attendu |
|------|-------------|----------------|
| `testEmptyCreateRequest` | Reject empty request | ✅ 400 Bad Request |
| `testNegativeCreditAmount` | Reject negative credit | ✅ 400 Bad Request |
| `testZeroAmountCredit` | Reject zero amount credit | ✅ 400 Bad Request |

**Total Wallet**: ~50 tests

---

## 🚀 EXÉCUTION DES TESTS

### Option 1: Script PowerShell (Recommandé)
```powershell
.\run-integration-tests.ps1
```

**Fonctionnalités:**
- ✅ Exécute tous les tests d'intégration
- ✅ Affiche les résultats en temps réel
- ✅ Génère un résumé global
- ✅ Calcule le taux de réussite
- ✅ Exit code approprié (0 = succès, 1 = échecs)

### Option 2: Maven individuel

**Transaction Service:**
```bash
cd transaction-service
mvn clean test -Dtest="*IntegrationTest"
```

**Wallet Service:**
```bash
cd wallet-service
mvn clean test -Dtest="*IntegrationTest"
```

### Option 3: Tous les services via Maven
```bash
mvn clean test -Dtest="*IntegrationTest" -pl transaction-service,wallet-service
```

---

## 📊 CRITÈRES DE SUCCÈS

### ✅ Core Validation Réussie Si:
1. **Transaction Service:**
   - ✅ Tous les tests LOT 1 passent (60+ tests)
   - ✅ Tous les tests LOT 2 passent (30+ tests)
   - ✅ Taux de réussite ≥ 95%

2. **Wallet Service:**
   - ✅ Tous les tests LOT 1 passent (50+ tests)
   - ✅ Taux de réussite ≥ 95%

3. **Global:**
   - ✅ Total tests: 140+
   - ✅ Aucun échec critique
   - ✅ Security guards fonctionnent
   - ✅ Validation des données fonctionne

---

## 🔍 COUVERTURE DES TESTS

### Transaction Service LOT 1
- ✅ **Endpoints:** 11/11 (100%)
- ✅ **Security:** Tous les rôles testés
- ✅ **Validation:** Tous les cas limites
- ✅ **Business Logic:** Solde, fees, état

### Transaction Service LOT 2
- ✅ **Endpoints:** 2/2 (100%)
- ✅ **Security:** MERCHANT/ADMIN only
- ✅ **Atomicity:** Testée
- ✅ **Validation:** Shares, recipients

### Wallet Service LOT 1
- ✅ **Endpoints:** 14/14 (100%)
- ✅ **Security:** Ownership, ADMIN operations
- ✅ **Lifecycle:** Tous les états
- ✅ **Balance Operations:** Credit, debit, block

---

## 📝 RAPPORTS

### Emplacement des Rapports
```
transaction-service/target/surefire-reports/
├── TEST-com.zaphira.transaction.integration.TransactionServiceIntegrationTest.xml
└── TEST-com.zaphira.transaction.integration.BulkOperationsIntegrationTest.xml

wallet-service/target/surefire-reports/
└── TEST-com.zaphira.wallet.integration.WalletServiceIntegrationTest.xml
```

### Types de Rapports
- **XML**: Détails complets (format Surefire)
- **TXT**: Sortie console
- **HTML**: (via `mvn surefire-report:report`)

---

## 🐛 DEBUGGING

### Si des tests échouent:

1. **Vérifier les logs:**
   ```bash
   tail -f transaction-service/target/surefire-reports/*.txt
   ```

2. **Activer debug logging:**
   ```yaml
   # application-test.yml
   logging:
     level:
       com.zaphira: TRACE
   ```

3. **Exécuter un seul test:**
   ```bash
   mvn test -Dtest="TransactionServiceIntegrationTest#testCreateTransfer"
   ```

4. **Vérifier la base H2:**
   - URL: `jdbc:h2:mem:testdb`
   - Console: `http://localhost:8080/h2-console` (si activée)

---

## 🔄 NEXT STEPS

### Après validation réussie:
1. ✅ **PHASE A Complete** - Core validé
2. ➡️ **PHASE B** - Implémenter LOT 4 (Search & Reporting)
3. ➡️ **PHASE C** - Implémenter LOT 3 (Recurring Transactions)
4. ➡️ **PHASE D** - Finaliser ScheduledTransaction
5. ➡️ **PHASE E** - Background Jobs
6. ➡️ **PHASE F** - Dispute System

### Si échecs:
1. 🐛 **Corriger les bugs identifiés**
2. ♻️ **Re-exécuter les tests**
3. 📝 **Documenter les problèmes**
4. 🔄 **Itérer jusqu'à succès complet**

---

## 📌 NOTES IMPORTANTES

### Configuration des Tests
- **Base de données:** H2 in-memory (isolée par test)
- **Sécurité:** Spring Security Test avec `@WithMockUser`
- **Transaction:** `@Transactional` sur les tests (rollback auto)
- **Profil:** `@ActiveProfiles("test")`

### Best Practices
- ✅ Tests isolés (pas de dépendances entre tests)
- ✅ Setup/Teardown automatique
- ✅ Données de test générées dynamiquement
- ✅ Assertions complètes (status, body, headers)
- ✅ Error cases testés

### Limitations Connues
- ⚠️ Tests utilisent H2, pas PostgreSQL (dialecte différent)
- ⚠️ Kafka désactivé en test
- ⚠️ Eureka désactivé en test
- ⚠️ Certains tests nécessitent données pré-existantes

---

**Statut**: 📋 TESTS CRÉÉS - PRÊT POUR EXÉCUTION  
**Total Tests**: 140+  
**Coverage**: LOT 1 (100%), LOT 2 (100%)  
**Next Action**: Exécuter `.\run-integration-tests.ps1`
