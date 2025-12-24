# 🧪 Guide Rapide - Tests SQLite + Feign

## 🎯 En 5 Minutes

### 1. **Vérifier l'environnement**
```bash
make verify
```

### 2. **Exécuter tous les tests**
```bash
# Option 1: Makefile (Linux/Mac)
make test

# Option 2: Script (Windows)
run-tests.bat all

# Option 3: Maven direct
mvn clean test -Dspring.profiles.active=test-sync
```

### 3. **Vérifier les résultats**
```
✅ BUILD SUCCESS
Tests run: XX, Failures: 0, Errors: 0
```

---

## 📚 Documentation Complète

| Document | Contenu |
|----------|---------|
| [TEST_SYNC_STRATEGY.md](TEST_SYNC_STRATEGY.md) | Architecture et stratégie |
| [GUIDE_EXECUTION_TESTS.md](GUIDE_EXECUTION_TESTS.md) | Guide complet d'exécution |
| [DEPENDENCIES_TEST_CONFIGURATION.md](DEPENDENCIES_TEST_CONFIGURATION.md) | Dépendances Maven |
| [TEST_IMPLEMENTATION_INDEX.md](TEST_IMPLEMENTATION_INDEX.md) | Index des fichiers créés |

---

## 🚀 Commandes Principales

### Makefile (Recommandé)
```bash
make test              # Tous les tests
make test-user         # User-service seulement
make test-wallet       # Wallet-service seulement
make test-debug        # Mode debug
make test-coverage     # Avec couverture
make clean             # Nettoyer
make help              # Voir toutes les commandes
```

### Scripts
```bash
# Linux/Mac
./run-tests.sh all
./run-tests.sh user-service debug

# Windows
run-tests.bat all
run-tests.bat user-service debug
```

### Maven Direct
```bash
# Tous les tests
mvn clean test -Dspring.profiles.active=test-sync

# Service spécifique
mvn test -f user-service/pom.xml -Dspring.profiles.active=test-sync

# Avec debug
mvn test -Dspring.profiles.active=test-sync -X
```

---

## 📊 Architecture

```
┌──────────────────────────────────────────────┐
│    SQLite + Feign Test Architecture          │
├──────────────────────────────────────────────┤
│                                              │
│  User-Service                Wallet-Service  │
│  │                                  │        │
│  ├─ H2 SQLite (in-memory)   H2 SQLite       │
│  ├─ UserRegistrationE2ETest WalletCreationE2ETest
│  └─ Feign ◄──────────────────► Feign        │
│                                              │
│  ✅ Pas de PostgreSQL                       │
│  ✅ Pas de Kafka                            │
│  ✅ Autonome sur une machine                │
│                                              │
└──────────────────────────────────────────────┘
```

---

## ✅ Checklist Initial

- [ ] Consulter DEPENDENCIES_TEST_CONFIGURATION.md
- [ ] Ajouter les dépendances Maven (H2, Rest-Assured, Mockito)
- [ ] Vérifier que `application-test-sync.properties` existe
- [ ] Exécuter `make verify`
- [ ] Lancer `make test`
- [ ] Vérifier le succès des tests

---

## 🧪 Tests Disponibles

### User-Service
✅ `testAdminRegistrationSuccess()` - Enregistrement complet  
✅ `testAdminUserPersistence()` - Persistance en BD  
✅ `testDuplicateEmailRejection()` - Validation doublons  
✅ `testValidationOfRequiredFields()` - Validation champs  
✅ `testWalletServiceCommunication()` - Communication Feign  
✅ `testWalletServiceTimeout()` - Gestion timeout  

### Wallet-Service
✅ `testWalletCreationSuccess()` - Création portefeuille  
✅ `testWalletCreationValidation()` - Validation données  

---

## 📈 Rapports

Après les tests :
```
test-reports-YYYYMMDD_HHMMSS/
├── all-tests.log
├── user-service-tests.log
├── wallet-service-tests.log
└── ...
```

Pour les rapports Surefire/Jacoco :
```bash
make report
```

---

## 🐛 Dépannage Rapide

| Problème | Solution |
|----------|----------|
| "H2 class not found" | Ajouter dépendance H2 |
| "test-sync profile not found" | Vérifier application-test-sync.properties |
| "Connection refused" | Services utilisent Feign (pas de portées requises) |
| "Tests not running" | `mvn clean` puis relancer |

---

## 📖 Lire Ensuite

1. **Pour configuration détaillée:** [GUIDE_EXECUTION_TESTS.md](GUIDE_EXECUTION_TESTS.md)
2. **Pour dépendances:** [DEPENDENCIES_TEST_CONFIGURATION.md](DEPENDENCIES_TEST_CONFIGURATION.md)
3. **Pour architecture:** [TEST_SYNC_STRATEGY.md](TEST_SYNC_STRATEGY.md)
4. **Pour fichiers créés:** [TEST_IMPLEMENTATION_INDEX.md](TEST_IMPLEMENTATION_INDEX.md)

---

## 💡 Astuces

**Exécution plus rapide:**
```bash
make quick-test        # Minimal output, faster
```

**Mode debug complet:**
```bash
make test-debug        # Avec logs détaillés
```

**Couverture de code:**
```bash
make test-coverage     # Avec Jacoco report
```

---

## 🎉 C'est Tout !

Vous avez maintenant une suite de tests complète qui :
- ✅ Fonctionne sans dépendances externes
- ✅ S'exécute rapidement
- ✅ Est isolée et reproductible
- ✅ Teste l'intégration entre services

**Bon testing ! 🚀**
