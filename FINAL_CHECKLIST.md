# ✅ CHECKLIST - Mise en Place des Tests SQLite + Feign

## 📋 Étape 1 : Préparation (5 minutes)

- [ ] Lire [QUICK_START_TESTS.md](QUICK_START_TESTS.md)
- [ ] Consulter [IMPLEMENTATION_SUMMARY.txt](IMPLEMENTATION_SUMMARY.txt)
- [ ] Vérifier Java 17+ : `java -version`
- [ ] Vérifier Maven 3.8+ : `mvn -v`

## 📦 Étape 2 : Dépendances Maven (10 minutes)

### Pour chaque service avec tests (user-service, wallet-service)

**Localisation du fichier:** `<service>/pom.xml`

**À ajouter dans la section `<dependencies>`:**

```xml
<!-- H2 Database (SQLite replacement) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- Rest Assured API Testing -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito for Mocking -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ Assertions -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

**Vérification:**
```bash
mvn dependency:resolve
# ✅ Vérifier que les dépendances se téléchargent
```

## 🔧 Étape 3 : Configuration de Test (5 minutes)

**Vérifier que ces fichiers existent :**

### User-Service
- [ ] `user-service/src/test/resources/application-test-sync.properties` ✅
- [ ] `user-service/src/test/java/com/zaphira/service_user/controller/test/UserServiceTestMessageController.java` ✅

### Wallet-Service
- [ ] `wallet-service/src/test/resources/application-test-sync.properties` ✅
- [ ] `wallet-service/src/test/java/com/zaphira/wallet/controller/test/WalletServiceTestMessageController.java` ✅

**Si manquants:** Voir [GUIDE_EXECUTION_TESTS.md](GUIDE_EXECUTION_TESTS.md)

## 🧪 Étape 4 : Tests Implémentés (5 minutes)

**Vérifier que ces tests existent :**

### User-Service
- [ ] `user-service/src/test/java/...integration/test/UserRegistrationE2ETest.java` ✅
- [ ] `user-service/src/test/java/...integration/test/SynchronousMicroserviceCommunicationTest.java` ✅

### Wallet-Service
- [ ] `wallet-service/src/test/java/...integration/test/WalletCreationE2ETest.java` ✅

## 🚀 Étape 5 : Vérification de l'Environnement (5 minutes)

```bash
make verify
```

**Ou manuellement:**

```bash
# Java
java -version
# ✅ Doit être 17 ou plus

# Maven
mvn -v
# ✅ Doit être 3.8 ou plus

# Dépendances
mvn dependency:resolve
# ✅ Pas d'erreurs

# Configuration
ls user-service/src/test/resources/application-test-sync.properties
ls wallet-service/src/test/resources/application-test-sync.properties
# ✅ Les deux doivent exister
```

## 🏗️ Étape 6 : Compilation (5 minutes)

```bash
mvn clean compile
```

**Résultat attendu:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX seconds
```

## 🧪 Étape 7 : Exécution des Tests (30 secondes)

### Option 1: Makefile (Recommandé)
```bash
make test
```

### Option 2: Script
```bash
# Linux/Mac
./run-tests.sh all

# Windows
run-tests.bat all
```

### Option 3: Maven Direct
```bash
mvn clean test -Dspring.profiles.active=test-sync
```

## ✅ Étape 8 : Vérification des Résultats

**Résultat attendu:**
```
[INFO] BUILD SUCCESS
[INFO] Tests run: 10, Failures: 0, Errors: 0
```

**Sortie console:**
```
✅ UserRegistrationE2ETest
  ✅ testAdminRegistrationSuccess
  ✅ testAdminUserPersistence
  ✅ testDuplicateEmailRejection
  ✅ testValidationOfRequiredFields

✅ SynchronousMicroserviceCommunicationTest
  ✅ testWalletServiceCommunication
  ✅ testWalletServiceTimeout
  ✅ testRetryOnTransientFailure
  ✅ testRequestValidation

✅ WalletCreationE2ETest
  ✅ testWalletCreationSuccess
  ✅ testWalletCreationValidation
```

## 📊 Étape 9 : Rapports (Optionnel)

```bash
make report
```

**Ou manuellement:**
```bash
mvn surefire-report:report
mvn jacoco:report
```

**Accéder aux rapports:**
- Surefire: `target/site/surefire-report.html`
- Jacoco: `target/site/jacoco/index.html`

## 🔄 Étape 10 : Configuration CI/CD (Optionnel)

### GitHub Actions

**Fichier:** `.github/workflows/tests.yml`

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: mvn clean test -Dspring.profiles.active=test-sync
```

### Jenkins

```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh 'mvn clean test -Dspring.profiles.active=test-sync'
            }
        }
    }
}
```

## 📚 Étape 11 : Documentation (Optionnel)

- [ ] Lire [QUICK_START_TESTS.md](QUICK_START_TESTS.md)
- [ ] Consulter [GUIDE_EXECUTION_TESTS.md](GUIDE_EXECUTION_TESTS.md)
- [ ] Ajouter tests aux autres services avec [TEMPLATE_ADD_TESTS_NEW_SERVICE.md](TEMPLATE_ADD_TESTS_NEW_SERVICE.md)

## 🎯 Étape 12 : Validation Finale

**Checklist de vérification:**

- [ ] ✅ Java 17+ installé
- [ ] ✅ Maven 3.8+ installé
- [ ] ✅ Dépendances H2, Rest-Assured, Mockito ajoutées
- [ ] ✅ Fichiers de configuration créés
- [ ] ✅ Contrôleurs de test créés
- [ ] ✅ Tests E2E créés
- [ ] ✅ `mvn clean compile` réussi
- [ ] ✅ `mvn test -Dspring.profiles.active=test-sync` réussi
- [ ] ✅ 10 tests passants
- [ ] ✅ 0 erreurs

## 🎉 Status

**Quand tout est ✅ :**

```
🎉 Vous êtes prêt !
   Vos tests SQLite + Feign sont opérationnels.
   
   Exécutez régulièrement:
   $ make test
   ou
   $ run-tests.bat all
```

---

## 🐛 Dépannage Rapide

| Problème | Solution |
|----------|----------|
| H2 class not found | Ajouter dépendance H2 dans pom.xml |
| test-sync profile not found | Vérifier application-test-sync.properties existe |
| Connection refused | Normal : services communiquent via Feign (local) |
| Tests not running | `mvn clean` puis relancer |
| BUILD FAILURE | Consulter logs détaillés : `make test-debug` |

---

## 📝 Notes Importantes

1. **Pas de PostgreSQL requis** - H2 SQLite suffit
2. **Pas de Kafka requis** - Feign REST synchrone
3. **Pas de Docker requis** - Exécutable localement
4. **Pas de configuration externe** - Tout inclus
5. **Tests isolés** - BD recréée à chaque test

---

## 🔗 Ressources Utiles

| Ressource | Lien |
|-----------|------|
| Quick Start | [QUICK_START_TESTS.md](QUICK_START_TESTS.md) |
| Guide Complet | [GUIDE_EXECUTION_TESTS.md](GUIDE_EXECUTION_TESTS.md) |
| Dépendances | [DEPENDENCIES_TEST_CONFIGURATION.md](DEPENDENCIES_TEST_CONFIGURATION.md) |
| Index Complet | [TEST_IMPLEMENTATION_INDEX.md](TEST_IMPLEMENTATION_INDEX.md) |
| Résumé | [IMPLEMENTATION_SUMMARY.txt](IMPLEMENTATION_SUMMARY.txt) |
| Template | [TEMPLATE_ADD_TESTS_NEW_SERVICE.md](TEMPLATE_ADD_TESTS_NEW_SERVICE.md) |

---

**Version:** 1.0.0  
**Date:** 2025-12-24  
**Status:** ✅ Prêt pour utilisation
