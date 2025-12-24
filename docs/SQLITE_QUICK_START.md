# SQLite Test Integration - Quick Start Guide

## 🎯 Pour les Développeurs

### Utiliser SQLite dans vos tests (3 étapes)

#### 1. Ajouter l'annotation du profil
```java
@DataJpaTest
@ActiveProfiles("test")  // ← C'est tout ce qu'il faut!
class MyRepositoryTest {
    // ...
}
```

#### 2. Écrire votre test normalement
```java
@Test
void testSave() {
    MyEntity entity = repo.save(createEntity());
    assertThat(repo.findById(entity.getId())).isPresent();
}
```

#### 3. Lancer les tests
```bash
mvn clean test
# SQLite en-mémoire est utilisé automatiquement
```

---

## 📋 Checklist de Configuration

- [x] Dépendance `sqlite-jdbc` en scope `test` (pom.xml)
- [x] Fichier `application-test.yml` présent dans `src/test/resources`
- [x] Classe `SQLiteDialectCustom` implémentée
- [x] Exemples de tests fournis et fonctionnels

✅ **Tout est prêt à l'emploi!**

---

## 🔍 Modes de Fonctionnement

### Mode 1: In-Memory (Défaut, Rapide ⚡)
```bash
mvn clean test  # SQLite en mémoire
# Vitesse: ~2-3 minutes pour la suite complète
```

**Idéal pour:**
- Tests CRUD rapides
- Validation JPA/Hibernate
- Tests unitaires avec DB
- CI/CD rapide

### Mode 2: File-Based (Debug 🐛)
```bash
# Force un fichier SQLite pour inspection
export SQLITE_TEST_DB_PATH=/tmp/test_zaphira.db
mvn clean test

# Après les tests, inspecter:
sqlite3 /tmp/test_zaphira.db ".schema transactions"
sqlite3 /tmp/test_zaphira.db "SELECT COUNT(*) FROM transactions;"
```

**Idéal pour:**
- Déboguer les tests échoués
- Inspecter l'état de la BD
- Valider les migrations

---

## 🚀 Exemples Rapides

### ✅ Repository Test (Rapide)
```java
@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {
    @Autowired
    private TransactionRepository repo;

    @Test
    void testFind() {
        Transaction tx = createTestTx();
        repo.save(tx);
        
        assertThat(repo.findById(tx.getId())).isPresent();
    }
}
```
**Exécution:** < 100ms par test

### ✅ Service Test (Intégration complète)
```java
@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceTest {
    @Autowired
    private TransactionService service;

    @Test
    @Transactional
    void testCreateTransaction() {
        Transaction tx = service.create(...);
        assertThat(tx.getId()).isNotNull();
    }
}
```
**Exécution:** ~500ms par test

---

## ⚠️ Limitations SQLite et Solutions

### Limitation 1: UUID Natif ❌
**Problème:** SQLite n'a pas de type UUID natif
```java
@Column
private UUID id;  // ← Mapping to TEXT automatiquement
```
**Solution:** ✅ Hibernate gère la conversion (voir SQLiteDialectCustom)

### Limitation 2: JSONB ❌
**Problème:** SQLite utilise TEXT à la place de JSONB
```java
@Column(columnDefinition = "JSONB")
private String metadata;  // ← Stocké en TEXT (JSON sérialisé)
```
**Solution:** ✅ Fonctionne pour les cas simples. Pour les tests critiques de JSONB, utiliser Testcontainers + PostgreSQL.

### Limitation 3: ARRAY Type ❌
**Problème:** SQLite n'a pas de type ARRAY natif
```java
@Column
@ElementCollection
private List<String> tags;  // ← Nécessite une table de jointure
```
**Solution:** ✅ Utiliser `@ElementCollection` (JPA gère la table auxiliaire)

### Limitation 4: Window Functions Limitées ❌
```sql
-- ❌ NON supporté:
SELECT id, RANK() OVER (PARTITION BY status) FROM transactions;
```
**Solution:** ✅ Pour ces cas, utiliser Testcontainers + PostgreSQL

---

## 🔗 Switch vers Testcontainers + PostgreSQL

Si votre test a besoin de PostgreSQL natif (UUID, JSONB, ARRAY, window functions):

### Étape 1: Ajouter les dépendances
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

### Étape 2: Écrire le test
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainers")
class PostgresCriticalTest {
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_zaphira");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void testUUIDNative() {
        // PostgreSQL natif, UUID réel
    }
}
```

### Étape 3: Créer application-testcontainers.yml
```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
```

**Résultat:** Tests PostgreSQL-fidèles, à la demande

---

## 📊 Tableau Comparatif

| Feature | SQLite (défaut) | Testcontainers + PostgreSQL |
|---------|---|---|
| **Vitesse** | ⚡⚡⚡ Rapide (~100ms) | 🐢 Lent (~2s startup) |
| **UUID natif** | ❌ TEXT | ✅ UUID type |
| **JSONB** | ❌ TEXT | ✅ JSONB type |
| **ARRAY** | ❌ Via table | ✅ Array type |
| **Window Functions** | ❌ Limité | ✅ Complet |
| **Isolation** | ✅ Automatique | ✅ Automatique |
| **Dépendances** | 📦 Minimal | 📦📦 Docker requis |
| **Cas d'Usage** | 95% des tests | 5% tests critiques |

---

## 🐛 Troubleshooting

### Problème: "org.sqlite.JDBC not found"
```bash
# Solution: Vérifier pom.xml
grep -A5 "sqlite-jdbc" transaction-service/pom.xml
# Doit avoir: <scope>test</scope>
```

### Problème: "Cannot find dialect"
```bash
# Solution: Vérifier le chemin dans application-test.yml
# Doit être:
# spring.jpa.properties.hibernate.dialect: com.zaphira.transaction.config.SQLiteDialectCustom
```

### Problème: Tests lents
```bash
# Solution: Désactiver la log SQL
# Dans application-test.yml:
# spring.jpa.show_sql: false  # ← Doit être false!
```

### Problème: "Unique constraint violation"
```bash
# Solution: Chaque test doit avoir un @BeforeEach
# qui génère des données uniques (timestamps, UUID, etc.)
@BeforeEach
void setUp() {
    testTx.setReference("TXN-" + System.currentTimeMillis());
}
```

---

## 📚 Ressources

- **Architecture complète:** [docs/test-architecture-sqlite.md](./test-architecture-sqlite.md)
- **Commit/PR Template:** [docs/SQLITE_INTEGRATION_COMMIT_PR_TEMPLATE.md](./SQLITE_INTEGRATION_COMMIT_PR_TEMPLATE.md)
- **SQLite JDBC:** https://github.com/xerial/sqlite-jdbc
- **Testcontainers:** https://www.testcontainers.org/

---

## ✨ Tips & Tricks

### Tip 1: Batch Operations
```java
// Rapide avec SQLite:
List<Transaction> txs = List.of(tx1, tx2, tx3, ...);
repo.saveAll(txs);  // Hibernate batch inserts
```

### Tip 2: Déboguer avec Fichier
```bash
export SQLITE_TEST_DB_PATH=/tmp/debug.db
mvn test -Dtest=MyTest#testMethod

# Puis inspecter:
sqlite3 /tmp/debug.db
sqlite> SELECT * FROM transactions LIMIT 1;
```

### Tip 3: Test Isolation Automatique
```java
@SpringBootTest
@ActiveProfiles("test")
class MyTest {
    @Test
    @Transactional  // Rollback automatique
    void testOne() { /* ... */ }

    @Test
    @Transactional  // État vierge
    void testTwo() { /* ... */ }
    // testTwo voit une BD vierge même si testOne a sauvegardé des données
}
```

### Tip 4: Fixtures de Test
```java
// Créer une classe utilitaire:
class TransactionTestFixture {
    static Transaction createTransaction(String ref) {
        return Transaction.builder()
            .reference(ref)
            .senderWalletNumber("WALLET-001")
            .receiverWalletNumber("WALLET-002")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .status(TransactionStatus.PENDING)
            .type(TransactionType.TRANSFER)
            .channel(TransactionChannel.WEB)
            .createdAt(LocalDateTime.now())
            .build();
    }
}

// Utiliser dans les tests:
@Test
void testSave() {
    Transaction tx = createTransaction("TEST-" + System.currentTimeMillis());
    // ...
}
```

---

## 🎓 Bonnes Pratiques

1. **Utiliser SQLite par défaut** (95% des tests)
2. **Seulement basculer sur Testcontainers** si:
   - Besoin de UUID/JSONB/ARRAY natifs
   - Test de migration Flyway PostgreSQL-spécifique
   - Test de window functions avancées

3. **Toujours utiliser @Transactional** pour l'isolation
4. **Créer des références uniques** (timestamps, UUID random)
5. **Documenter les limitations** SQLite si présentes
6. **Déboguer avec fichier** (`SQLITE_TEST_DB_PATH`) si doute

---

## 🚀 Prochaines Étapes

1. ✅ Écrire des tests avec `@ActiveProfiles("test")`
2. ⚙️ (Optionnel) Ajouter Testcontainers pour tests critiques
3. 📊 Mesurer les gains de performance
4. 📝 Documenter les limitations SQLite dans les tests métier

**Bonne chance! 🎉**
