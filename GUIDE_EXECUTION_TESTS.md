# 🧪 Guide d'Exécution des Tests - SQLite + Feign

## 📋 Prérequis

- Java 17+
- Maven 3.8+
- Aucune dépendance externe (PostgreSQL, Kafka) requise ! ✅

## 🏗️ Architecture des Tests

```
┌─────────────────────────────────────────────┐
│   Test Profile: test-sync                   │
├─────────────────────────────────────────────┤
│ ✅ Base de données: H2 SQLite (en mémoire)  │
│ ✅ Messaging: Feign REST calls              │
│ ✅ Pas de dépendances externes              │
│ ✅ Isolation complète                       │
└─────────────────────────────────────────────┘
```

## 🚀 Commandes d'Exécution

### 1. **Tests du User-Service**

```bash
# Tests avec profil test-sync
cd user-service
mvn clean test -Dspring.profiles.active=test-sync

# Test spécifique
mvn test -Dspring.profiles.active=test-sync \
  -Dtest=UserRegistrationE2ETest

# Avec logs détaillés
mvn clean test -Dspring.profiles.active=test-sync \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

### 2. **Tests du Wallet-Service**

```bash
# Tests avec profil test-sync
cd wallet-service
mvn clean test -Dspring.profiles.active=test-sync

# Test spécifique
mvn test -Dspring.profiles.active=test-sync \
  -Dtest=WalletCreationE2ETest
```

### 3. **Tous les Tests (du répertoire racine)**

```bash
# Depuis la racine du projet
mvn clean test -Dspring.profiles.active=test-sync

# Avec rapport de couverture
mvn clean test jacoco:report \
  -Dspring.profiles.active=test-sync
```

### 4. **Tests en Mode Debug**

```bash
# Avec inspection des logs
mvn clean test -Dspring.profiles.active=test-sync \
  -X 2>&1 | tee test-output.log

# Avec point d'arrêt (dans l'IDE)
mvn -Dmaven.surefire.debug test \
  -Dspring.profiles.active=test-sync
```

## 📊 Structure des Tests

### User Service

```
user-service/src/test/
├── java/
│   └── com/zaphira/service_user/
│       ├── integration/test/
│       │   └── UserRegistrationE2ETest.java
│       └── controller/test/
│           └── UserServiceTestMessageController.java
└── resources/
    └── application-test-sync.properties
```

**Tests Disponibles:**
- ✅ `testAdminRegistrationSuccess()` - Enregistrement admin complet
- ✅ `testAdminUserPersistence()` - Persistance en SQLite
- ✅ `testDuplicateEmailRejection()` - Validation des doublons
- ✅ `testValidationOfRequiredFields()` - Validation des champs

### Wallet Service

```
wallet-service/src/test/
├── java/
│   └── com/zaphira/wallet/
│       ├── integration/test/
│       │   └── WalletCreationE2ETest.java
│       └── controller/test/
│           └── WalletServiceTestMessageController.java
└── resources/
    └── application-test-sync.properties
```

**Tests Disponibles:**
- ✅ `testWalletCreationSuccess()` - Création du portefeuille
- ✅ `testWalletCreationValidation()` - Validation des données

## 🔄 Flux de Test E2E

```
1. Démarrage du contexte Spring
   ↓
2. Initialisation de H2 SQLite
   ↓
3. Création des schémas BD
   ↓
4. POST /api/users/register/admin
   ↓
5. Sauvegarde utilisateur en BD
   ↓
6. Appel Feign → wallet-service
   ↓
7. Création du portefeuille
   ↓
8. Retour du walletId
   ↓
9. Mise à jour utilisateur avec walletId
   ↓
10. Assertions de validation
```

## ✅ Points de Vérification

### Base de Données (H2)
- Tables créées automatiquement ✅
- Données persistées pendant le test ✅
- Nettoyage après le test ✅
- Accès via H2 Console: `http://localhost:8082/h2-console`

### Messaging (Feign)
- Calls synchrones (pas d'async) ✅
- Aucun Kafka requis ✅
- Fallback en cas d'erreur ✅
- Logs détaillés disponibles ✅

### Assertions
- Status HTTP vérifiés ✅
- JSON parsing validé ✅
- BD queries vérifiées ✅
- Erreurs capturées ✅

## 📈 Rapports de Test

### Rapport Maven Surefire
```bash
mvn surefire-report:report
# Résultat: target/site/surefire-report.html
```

### Couverture de Code (Jacoco)
```bash
mvn clean test jacoco:report
# Résultat: target/site/jacoco/index.html
```

### Logs Détaillés
Tous les logs sont disponibles dans:
- Console (sortie Maven)
- Fichiers dans `target/surefire-reports/`

## 🐛 Dépannage

### Erreur: "Connection refused"
```
✅ Solution: Les services ne communiquent que via Feign
   Vérifiez les URLs dans application-test-sync.properties
```

### Erreur: "H2 dialect not found"
```
✅ Solution: Ajoutez la dépendance H2 au pom.xml
   <dependency>
       <groupId>com.h2database</groupId>
       <artifactId>h2</artifactId>
       <scope>test</scope>
   </dependency>
```

### Erreur: "Profile test-sync not active"
```
✅ Solution: Assurez-vous que @ActiveProfiles("test-sync") 
   est présent dans la classe de test
```

## 🎯 Prochaines Étapes

1. **Ajouter tests unitaires** pour les services
2. **Ajouter tests contrats** (Pact)
3. **Ajouter performance tests**
4. **Intégrer à CI/CD**

## 📝 Notes Importantes

- Les tests s'exécutent **sans dépendances externes** ✅
- La BD H2 est **recréée à chaque test** (isolation) ✅
- Les tests **tournent sur une machine unique** ✅
- **Pas de configuration Docker** requise pour les tests ✅

## 🔗 Fichiers de Configuration

| Fichier | Localisation | Rôle |
|---------|-------------|------|
| `application-test-sync.properties` | `src/test/resources/` | Config H2 + Feign |
| `UserRegistrationE2ETest.java` | `src/test/java/` | Tests E2E user-service |
| `WalletCreationE2ETest.java` | `src/test/java/` | Tests E2E wallet-service |
| Test Controllers | `src/test/java/controller/test/` | Message handlers Feign |

---

**Besoin d'aide ?** Consultez les logs ou ajustez les configurations dans `application-test-sync.properties`
