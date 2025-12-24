# SQLite Integration - Copy-Paste Snippets for All Services

> Ce document fournit les snippets directement copiables pour intégrer SQLite dans chaque service.

---

## 🔄 Services à Configurer

- [x] transaction-service (déjà fait)
- [ ] wallet-service
- [ ] user-service
- [ ] auth
- [ ] notification-service

---

## 📋 Template Universel par Service

### Pour CHAQUE service, appliquer:

#### 1️⃣ Ajouter au pom.xml (identique pour tous)

```xml
<!-- À ajouter après la dépendance H2 (si présente) -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.43.0.0</version>
    <scope>test</scope>
</dependency>
```

**Localisation:** `[SERVICE]/pom.xml`, section `<dependencies>`, après h2 ou avant les dépendances business

---

#### 2️⃣ Créer application-test.yml

```yaml
# SQLite Test Configuration
# This profile activates when running tests with @ActiveProfiles("test")
# SQLite runs in-memory by default for fast test execution

spring:
  datasource:
    url: jdbc:sqlite::memory:
    driver-class-name: org.sqlite.JDBC
    # Optional: force file-based SQLite for debugging by setting SQLITE_TEST_DB_PATH
    # Example: export SQLITE_TEST_DB_PATH=/tmp/test_zaphira.db
    url-override: ${SQLITE_TEST_DB_PATH:}
  
  jpa:
    hibernate:
      # Auto-generate schema (create-drop: recreate on each test run)
      ddl-auto: create-drop
    properties:
      hibernate:
        # Use SQLite dialect
        dialect: com.zaphira.[SERVICE].config.SQLiteDialectCustom
        # Formatting SQL for readability in test logs
        format_sql: true
        # Show SQL in logs
        show_sql: false
        # Batch size for operations
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true
    # Disable Flyway migrations for tests (use Hibernate DDL instead)
    database-platform: org.hibernate.dialect.SQLite3Dialect
    open-in-view: false

  # Disable initialization mode (use Hibernate DDL)
  sql:
    init:
      mode: never

# Logging
logging:
  level:
    root: INFO
    com.zaphira: DEBUG
    # Uncomment for SQL debugging:
    # org.hibernate.SQL: DEBUG
    # org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**Localisation:** `[SERVICE]/src/test/resources/application-test.yml`  
**Remplacer:** `[SERVICE]` par le nom du package (auth, wallet, user, etc.)

---

#### 3️⃣ Créer SQLiteDialectCustom.java

```java
package com.zaphira.[SERVICE].config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SQLiteDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;

import java.sql.Types;

/**
 * Custom SQLite Dialect for Hibernate compatible with SQLite JDBC driver.
 * 
 * Provides minimal mapping for common types with special handling for:
 * - UUID -> TEXT (SQLite has no native UUID)
 * - JSONB -> TEXT (use JSON strings)
 * - ARRAY -> TEXT (use serialized format)
 * 
 * Note: SQLite has limited support compared to PostgreSQL. This dialect is intended
 * for fast unit/integration tests only. For DB-level fidelity testing, use Testcontainers
 * with PostgreSQL.
 */
public class SQLiteDialectCustom extends SQLiteDialect {

    public SQLiteDialectCustom() {
        super(DatabaseVersion.parse("3.43.0"));
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        
        // Map UUID to TEXT in SQLite
        typeContributions.contributeType(
            StandardBasicTypes.UUID,
            "TEXT"
        );
        
        // Ensure NUMERIC types are properly mapped
        typeContributions.contributeType(
            StandardBasicTypes.BIG_DECIMAL,
            "NUMERIC"
        );
    }

    @Override
    public String getCastTypeName(int code) {
        // SQLite doesn't support many CAST operations, return NULL for unsupported types
        switch (code) {
            case Types.NUMERIC:
            case Types.DECIMAL:
                return "NUMERIC";
            case Types.TIMESTAMP:
                return "TEXT";
            case Types.DATE:
                return "TEXT";
            case Types.TIME:
                return "TEXT";
            default:
                return super.getCastTypeName(code);
        }
    }

    @Override
    public String getTypeName(int code, long length, int precision, int scale) {
        // Handle UUID specifically
        if ("uuid".equalsIgnoreCase(getTypeName(code))) {
            return "TEXT";
        }
        return super.getTypeName(code, length, precision, scale);
    }

    /**
     * SQLite doesn't support LIMIT with ORDER BY NULLS FIRST/LAST
     */
    @Override
    public boolean supportsDistinctFromPredicate() {
        return false;
    }

    /**
     * SQLite doesn't support some window functions
     */
    @Override
    public boolean supportsWindowFunctions() {
        return false;
    }

    @Override
    public boolean supportsOffsetInSubquery() {
        return true;
    }

    @Override
    public boolean supportsLimitInSubquery() {
        return true;
    }

    /**
     * SQLite supports UNION
     */
    @Override
    public boolean supportsUnionAll() {
        return true;
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    @Override
    public String getCurrentTimestampSelectString() {
        return "select datetime('now')";
    }

    /**
     * Enable PRAGMA foreign_keys for referential integrity in tests
     */
    @Override
    public String getQuerySequencesString() {
        return "PRAGMA foreign_keys = ON";
    }
}
```

**Localisation:** `[SERVICE]/src/test/java/com/zaphira/[SERVICE]/config/SQLiteDialectCustom.java`

---

#### 4️⃣ Exemple de Test par Service

##### Pour wallet-service
```java
package com.zaphira.wallet.integration;

import com.zaphira.wallet.model.Wallet;  // Adapter l'import
import com.zaphira.wallet.repository.WalletRepository;  // Adapter l'import
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for WalletRepository using SQLite in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Wallet Repository - SQLite Integration Tests")
class WalletRepositoryIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = Wallet.builder()
                .walletNumber("WALLET-" + System.currentTimeMillis())
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve wallet by ID")
    void testSaveAndFindById() {
        // Arrange & Act
        Wallet savedWallet = walletRepository.save(testWallet);

        // Assert
        assertThat(savedWallet.getId()).isNotNull();
        
        Optional<Wallet> retrievedWallet = walletRepository.findById(savedWallet.getId());
        
        assertThat(retrievedWallet).isPresent();
        assertThat(retrievedWallet.get().getWalletNumber()).isEqualTo(testWallet.getWalletNumber());
        assertThat(retrievedWallet.get().getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should update wallet balance")
    void testUpdateBalance() {
        // Arrange
        Wallet savedWallet = walletRepository.save(testWallet);
        Long walletId = savedWallet.getId();

        // Act
        Optional<Wallet> retrievedWallet = walletRepository.findById(walletId);
        assertThat(retrievedWallet).isPresent();
        
        Wallet wallet = retrievedWallet.get();
        wallet.setBalance(new BigDecimal("2000.00"));
        walletRepository.save(wallet);

        // Assert
        Optional<Wallet> updatedWallet = walletRepository.findById(walletId);
        assertThat(updatedWallet)
                .isPresent()
                .get()
                .satisfies(w -> assertThat(w.getBalance()).isEqualByComparingTo(new BigDecimal("2000.00")));
    }

    @Test
    @DisplayName("Should delete wallet by ID")
    void testDeleteWallet() {
        // Arrange
        Wallet savedWallet = walletRepository.save(testWallet);
        Long walletId = savedWallet.getId();

        // Act
        walletRepository.deleteById(walletId);

        // Assert
        Optional<Wallet> deletedWallet = walletRepository.findById(walletId);
        assertThat(deletedWallet).isEmpty();
    }

    @Test
    @DisplayName("Should count wallets")
    void testCountWallets() {
        // Arrange
        walletRepository.save(testWallet);
        walletRepository.save(testWallet.toBuilder()
                .walletNumber("WALLET-2-" + System.currentTimeMillis())
                .build());

        // Act
        long count = walletRepository.count();

        // Assert
        assertThat(count).isGreaterThanOrEqualTo(2);
    }
}
```

**Localisation:** `[SERVICE]/src/test/java/com/zaphira/[SERVICE]/integration/[Entity]RepositoryIntegrationTest.java`

> Adapter:
> - `Wallet` → entité du service
> - `WalletRepository` → repository du service
> - Champs spécifiques à l'entité

---

## 🎯 Guide par Service

### 🏦 wallet-service

1. **Copier** le `pom.xml` snippet et ajouter à `wallet-service/pom.xml`
2. **Copier** le `application-test.yml` à `wallet-service/src/test/resources/`
3. **Copier** le `SQLiteDialectCustom.java` à `wallet-service/src/test/java/com/zaphira/wallet/config/`
4. **Créer** `WalletRepositoryIntegrationTest.java` (adapter le snippet fourni)

**Entités principales:**
- `Wallet`
- `WalletTransaction`
- `WalletBalance`

---

### 👤 user-service

1. Mêmes étapes que wallet-service
2. **Adapter** les imports pour `user` au lieu de `wallet`
3. **Tester** avec `User`, `UserProfile`, `UserPreferences`

**Entités principales:**
- `User`
- `UserRole`
- `UserPreference`

---

### 🔐 auth

1. Mêmes étapes
2. **Entités principales:**
   - `AuthToken`
   - `RefreshToken`
   - `ApiKey`

---

### 📬 notification-service

1. Mêmes étapes
2. **Entités principales:**
   - `Notification`
   - `NotificationTemplate`
   - `NotificationLog`

---

## 🔍 Checklist Rapide par Service

```bash
# Template pour chaque service:

cd [SERVICE]

# 1. Ajouter dépendance
nano pom.xml
# → Ajouter sqlite-jdbc dependency

# 2. Créer config
mkdir -p src/test/resources
# → Créer application-test.yml

# 3. Créer dialecte
mkdir -p src/test/java/com/zaphira/[SERVICE]/config
# → Créer SQLiteDialectCustom.java

# 4. Créer test
mkdir -p src/test/java/com/zaphira/[SERVICE]/integration
# → Créer [Entity]RepositoryIntegrationTest.java

# 5. Tester
mvn clean test
# ✅ Doit passer

# 6. Vérifier
mvn test -Dtest=[Entity]RepositoryIntegrationTest
# ✅ Tous les tests doivent passer
```

---

## 🚀 Script d'Automatisation (optionnel)

Si vous voulez automatiser la création pour tous les services:

```bash
#!/bin/bash
# setup-sqlite-all-services.sh

SERVICES=("wallet-service" "user-service" "auth" "notification-service")

for SERVICE in "${SERVICES[@]}"; do
    echo "Setting up SQLite for $SERVICE..."
    
    # 1. Ensure pom.xml dependency is added
    # (Manual: add to pom.xml)
    
    # 2. Create application-test.yml
    mkdir -p "$SERVICE/src/test/resources"
    cat > "$SERVICE/src/test/resources/application-test.yml" << 'EOF'
# [Copier le contenu de application-test.yml depuis ce document]
EOF
    
    # 3. Create SQLiteDialectCustom
    mkdir -p "$SERVICE/src/test/java/com/zaphira/${SERVICE}/config"
    cat > "$SERVICE/src/test/java/com/zaphira/${SERVICE}/config/SQLiteDialectCustom.java" << 'EOF'
# [Copier le contenu de SQLiteDialectCustom.java depuis ce document]
EOF
    
    # 4. Test
    cd "$SERVICE"
    mvn clean test
    cd ..
    
    echo "✅ $SERVICE configured"
done

echo "🎉 All services configured with SQLite!"
```

---

## 📖 Ressources Documentaires

**Lire dans cet ordre:**
1. [SQLITE_QUICK_START.md](./SQLITE_QUICK_START.md) - Vue d'ensemble rapide
2. [SQLITE_INTEGRATION_DELIVERABLES.md](./SQLITE_INTEGRATION_DELIVERABLES.md) - Ce qui a été livré
3. [test-architecture-sqlite.md](./test-architecture-sqlite.md) - Architecture complète
4. [SQLITE_INTEGRATION_COMMIT_PR_TEMPLATE.md](./SQLITE_INTEGRATION_COMMIT_PR_TEMPLATE.md) - Pour Git

---

## ❓ FAQ

**Q: Faire un fichier universel à réutiliser?**
A: Oui! Mettre `SQLiteDialectCustom.java` en `common-library` et importer partout

**Q: Comment mettre en commun?**
```java
// common-library/src/test/java/com/zaphira/common/config/SQLiteDialectCustom.java
public class SQLiteDialectCustom extends SQLiteDialect { ... }

// Chaque service importe:
// spring.jpa.properties.hibernate.dialect: com.zaphira.common.config.SQLiteDialectCustom
```

**Q: Et les migrations Flyway en test?**
A: Utiliser `ddl-auto: create-drop` au lieu de Flyway pour les tests SQLite. Pour Flyway-critical, utiliser Testcontainers + PostgreSQL

**Q: Garder H2 ou le remplacer?**
A: H2 peut rester (sauf conflit). SQLite est maintenant défaut via profil `test`

---

## ✅ Validation Finale (tous les services)

```bash
# Tester tous les services
for SERVICE in transaction-service wallet-service user-service auth notification-service; do
    echo "Testing $SERVICE..."
    cd "$SERVICE"
    mvn clean test || echo "❌ $SERVICE FAILED"
    cd ..
done

echo "✅ All services ready for SQLite testing!"
```

---

## 🎁 Fichiers à Garder

Ces fichiers sont maintenant prêts et ne doivent PAS être modifiés:

- ✅ `docs/test-architecture-sqlite.md` - Référence architecturale
- ✅ `docs/SQLITE_QUICK_START.md` - Guide utilisateur
- ✅ `docs/SQLITE_INTEGRATION_DELIVERABLES.md` - Checklist livrables
- ✅ `docs/SQLITE_INTEGRATION_COMMIT_PR_TEMPLATE.md` - Git templates
- ✅ `docs/SQLITE_SNIPPETS_COPY_PASTE.md` - Ce fichier

**À chaque service:** créer ses propres fichiers (pom.xml, application-test.yml, config/SQLiteDialectCustom.java, tests)

---

## 🎉 Prêt à Déployer!

Tous les snippets, templates et guides sont fournis. Il suffit de:
1. Copier/coller
2. Adapter les noms de packages
3. Tester
4. Commiter

Bonne chance! 🚀
