# Architecture de Test SQLite - Zaphira Platform

## Vue d'ensemble

Ce document décrit l'intégration de **SQLite** comme base de données dédiée aux tests unitaires et d'intégration dans le projet Spring Boot Zaphira. L'approche combine les avantages de SQLite (vitesse, légèreté) pour les tests rapides avec Testcontainers/PostgreSQL pour les tests critiques nécessitant une fidélité complète avec la base de production.

## Stratégie de Test : Multi-niveaux

```
┌─────────────────────────────────────────────────────────────────┐
│                    STRATÉGIE DE TEST ZAPHIRA                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. TESTS UNITAIRES (Mockito)                                   │
│     └─ Pas de base de données                                   │
│     └─ Focus: logique métier isolée                             │
│     └─ Vitesse: < 100ms par test                                │
│                                                                   │
│  2. TESTS D'INTÉGRATON (SQLite in-memory)  ← NEW                │
│     └─ Rapides, pas de dépendances externes                     │
│     └─ Focus: interactions JPA/Hibernate                        │
│     └─ Vitesse: < 500ms par test                                │
│     └─ Limitations: Pas UUID natif, pas JSONB, etc.             │
│                                                                   │
│  3. TESTS DB-CRITICAUX (Testcontainers + PostgreSQL)            │
│     └─ Fidélité complète avec production                        │
│     └─ Focus: fonctionnalités PostgreSQL-spécifiques            │
│     └─ Vitesse: 2-5s par test (démarrage container)             │
│     └─ Utilisation: CI/CD, tests de migration, etc.             │
│                                                                   │
│  4. TESTS E2E (Docker Compose stack)                            │
│     └─ Intégration avec tous les services                       │
│     └─ Focus: workflows complets                                │
│     └─ Vitesse: 10-30s (warm containers)                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Configuration SQLite pour Tests

### 1. Dépendance Maven

```xml
<!-- pom.xml (scope: test) -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.43.0.0</version>
    <scope>test</scope>
</dependency>
```

### 2. Profil de Configuration (application-test.yml)

```yaml
spring:
  datasource:
    url: jdbc:sqlite::memory:          # Base en mémoire (rapide)
    driver-class-name: org.sqlite.JDBC
  jpa:
    hibernate:
      ddl-auto: create-drop            # Recréer le schéma pour chaque test
    properties:
      hibernate:
        dialect: com.zaphira.transaction.config.SQLiteDialectCustom
        format_sql: true
        show_sql: false
```

### 3. Activation du Profil en Test

```java
@SpringBootTest
@ActiveProfiles("test")
class MyIntegrationTest {
    // SQLite sera utilisé automatiquement
}
```

### 4. Modes de Fonctionnement

#### a) Mode Par Défaut (In-Memory, Rapide)
```bash
# Aucune configuration supplémentaire requise
mvn test  # SQLite en mémoire
```

#### b) Mode Debug (Fichier SQLite Persistent)
```bash
# Force un fichier SQLite pour inspecter après les tests
export SQLITE_TEST_DB_PATH=/tmp/test_zaphira.db
mvn test

# Inspecter la base:
sqlite3 /tmp/test_zaphira.db "SELECT COUNT(*) FROM transactions;"
```

## Dialecte Hibernate Personnalisé

### Fichier: `SQLiteDialectCustom.java`

Le dialecte personnalisé gère les incompatibilités SQLite/Hibernate :

| Type Hibernate | SQLite | Remarque |
|---|---|---|
| UUID | TEXT | SQLite n'a pas de type UUID natif |
| JSONB | TEXT | Utiliser du JSON sérialisé |
| ARRAY | TEXT | Utiliser une représentation sérialisée |
| INTERVAL | INTEGER | En secondes/millisecondes |
| BOOLEAN | INTEGER | 0 = false, 1 = true |
| NUMERIC(p,s) | NUMERIC | Support limité des décimales |
| TIMESTAMP | TEXT | Format ISO 8601 |

### Limitations Héritées de SQLite

| Feature | Support | Workaround |
|---|---|---|
| Foreign Keys | ✅ (avec PRAGMA) | Activé par défaut dans tests |
| Transactions | ✅ | Fonctionne normalement |
| Trigger | ✅ | SQL trigger standard |
| Index | ✅ | Index standard B-tree |
| Full-Text Search | ✅ | FTS5 module disponible |
| Window Functions | ❌ | Limité (éviter OVER PARTITION) |
| Common Table Expressions (CTE) | ✅ | WITH clause supportée |
| Recursive CTE | ✅ | Supporté |
| Distinct From | ❌ | Utiliser NOT EQUAL à la place |
| RETURNING Clause | ❌ | Absent (PostgreSQL 12+) |

## Checklist d'Utilisation

### Pour les Tests Simples (CRUD, JPA)
- ✅ Utiliser SQLite (`@DataJpaTest`, `@ActiveProfiles("test")`)
- ✅ Dépend que de `spring-boot-starter-test` + `sqlite-jdbc`
- ✅ Exécution: < 500ms

### Pour les Tests PostgreSQL-Dépendants
- ⚠️ Éviter les features PostgreSQL-spécifiques (UUID natif, JSONB, ARRAY, etc.)
- ⚠️ Ou: basculer sur Testcontainers + PostgreSQL

### Pour les Tests de Migration/Schéma
- ✅ Utiliser SQLite si aucune directive Flyway-spécifique
- ⚠️ Ou: utiliser Testcontainers si migrations Flyway PostgreSQL-dépendantes

### Dans le CI/CD
- ✅ Tests rapides (SQLite) : exécutés à chaque commit
- ⚠️ Tests critiques (Testcontainers) : exécutés nightly ou avant release

## Exemples de Patterns de Test

### Pattern 1: Repository Test (@DataJpaTest)

```java
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class TransactionRepositoryIntegrationTest {
    @Autowired
    private TransactionRepository repo;

    @Test
    void testCrudOperations() {
        // SQLite fournit une base vierge
        Transaction tx = repo.save(createTestTransaction());
        assertThat(repo.findById(tx.getId())).isPresent();
    }
}
```

### Pattern 2: Service Test (@SpringBootTest)

```java
@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceIntegrationTest {
    @Autowired
    private TransactionService service;
    @Autowired
    private TransactionRepository repo;

    @Test
    @Transactional
    void testBusinessLogic() {
        // SQLite en mémoire, context Spring chargé
        Transaction tx = service.createTransaction(...);
        assertThat(repo.findById(tx.getId())).isPresent();
    }
}
```

### Pattern 3: Test PostgreSQL-Critique (Testcontainers)

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainers")  // Voir config ci-dessous
class PostgresCriticalTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(...)
            .withDatabaseName("test_zaphira");

    @Test
    void testUUIDNativeStorage() {
        // PostgreSQL offre UUID natif
        // UUID uuid = UUID.randomUUID();
        // assertThat(repo.findByUUID(uuid)).isPresent();
    }
}
```

## Configuration pour Testcontainers (Optional)

Pour les tests critiques nécessitant PostgreSQL:

### Dépendance Maven

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
```

### Fichier: `application-testcontainers.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/test_zaphira
    username: test
    password: test
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
```

### Test Exemple

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainers")
class PostgresCriticalTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_zaphira")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void testPostgresUUID() {
        // Test avec UUID native PostgreSQL
    }
}
```

## Avantages et Inconvénients

### SQLite (Défaut)

**Avantages:**
- ⚡ Très rapide (in-memory, pas de réseau)
- 📦 Aucune dépendance externe (JDBC driver seulement)
- 🔄 Parfait pour tests CRUD/JPA basiques
- ♻️ Isolation automatique (@Transactional rollback)
- 🌍 Cross-platform (Windows, Linux, macOS)

**Inconvénients:**
- ❌ Pas UUID natif (TEXT mapping)
- ❌ Pas JSONB (JSON sérialisé seulement)
- ❌ Pas ARRAY type
- ❌ Pas window functions avancées
- ⚠️ Comportement légèrement différent (lock strategy, etc.)

### PostgreSQL + Testcontainers

**Avantages:**
- ✅ Fidélité 100% avec production
- ✅ UUID natif, JSONB, ARRAY, etc.
- ✅ Window functions, CTE, recursive queries
- ✅ Même comportement transactionnel qu'en prod
- ✅ Tests de migration réalistes

**Inconvénients:**
- 🐢 Plus lent (démarrage container, overhead réseau)
- 📦 Dépendances supplémentaires (testcontainers, Docker runtime)
- 🔧 Configuration plus complexe
- 💾 Plus gourmand en ressources CI/CD

## Recommandations par Cas d'Usage

| Cas d'Usage | DB Recommandée | Profil | Exemple |
|---|---|---|---|
| Tests CRUD basiques | SQLite | `test` | `TransactionRepositoryTest` |
| Tests de validation JPA | SQLite | `test` | `TransactionValidationTest` |
| Tests de filtre/pagination | SQLite | `test` | `TransactionFilterTest` |
| Tests UUID natif | PostgreSQL | `testcontainers` | `UUIDPersistenceTest` |
| Tests JSONB/ARRAY | PostgreSQL | `testcontainers` | `JsonbStorageTest` |
| Tests migration Flyway | PostgreSQL | `testcontainers` | `MigrationTest` |
| Tests e2e complets | PostgreSQL Stack | `docker-compose` | `E2ETest` |

## Pipeline CI/CD Recommandé

```yaml
# .github/workflows/test.yml (ou équivalent GitLab/Jenkins)
name: Tests
on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: mvn test -DProfile=test
        # Rapide: ~2-3 minutes
        # SQLite in-memory utilisé

  db-critical-tests:
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request' || github.ref == 'main'
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
      - uses: docker/setup-qemu-action@v2
      - uses: docker/setup-buildx-action@v2
      - run: mvn test -DProfile=testcontainers
        # Lent: ~10-15 minutes
        # PostgreSQL dans container utilisé

  e2e-tests:
    runs-on: ubuntu-latest
    if: github.ref == 'main'
    steps:
      - uses: actions/checkout@v3
      - uses: docker-compose-action@v2
      - run: ./scripts/run-e2e-tests.sh
        # Très lent: ~20-30 minutes
```

## Troubleshooting

### Problème: "org.sqlite.JDBC driver not found"
**Solution:** Vérifier que `sqlite-jdbc` est en scope `test` dans `pom.xml`

### Problème: "Cannot find dialect class"
**Solution:** Vérifier que `SQLiteDialectCustom` est dans le bon package et que `spring.jpa.database-platform` pointe le bon chemin

### Problème: Tests lents avec SQLite
**Solution:** Vérifier que:
- `spring.jpa.show_sql: false` (ne pas logger chaque SQL)
- `spring.jpa.properties.hibernate.jdbc.batch_size: 20` (batch inserts)
- Pas de `@Transactional` imbriquée

### Problème: "Unique constraint violation" avec in-memory
**Solution:** Vérifier que chaque test a un `@BeforeEach` qui crée des références uniques ou que `ddl-auto: create-drop` est activé

### Problème: PostgreSQL-only features manquent dans tests SQLite
**Solution:** 
1. Si critique: basculer sur Testcontainers
2. Si non-critique: implémenter workaround SQLite (ex: JSON au lieu de JSONB)
3. Documenter la limitation dans le code

## Ressources et Documentation

- [SQLite JDBC Driver](https://github.com/xerial/sqlite-jdbc)
- [Hibernate SQLite Dialect](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#database-dialect)
- [Testcontainers PostgreSQL](https://www.testcontainers.org/modules/databases/postgres/)
- [Spring Boot Test Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [SQLite Limits & Features](https://www.sqlite.org/features.html)

## Conclusion

L'intégration de **SQLite pour tests rapides** offre un excellent compromis entre vitesse et fiabilité. La stratégie multi-niveaux (SQLite → Testcontainers → Docker Compose) permet :

1. ⚡ **Feedback rapide** durant le développement (SQLite)
2. ✅ **Confiance** sur les features PostgreSQL-critiques (Testcontainers)
3. 🎯 **Validation e2e** complète (Docker Compose stack)

Cela réduit le temps de test global tout en maintenant une qualité élevée.
