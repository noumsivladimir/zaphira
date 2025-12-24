# 📦 Dépendances Requises pour Tests SQLite + Feign

Ajoutez ces dépendances à votre `pom.xml` pour chaque service nécessitant des tests.

## Pour User-Service et Wallet-Service

```xml
<!-- Test Dependencies -->

<!-- H2 Database (SQLite replacement for tests) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
    <version>2.2.224</version>
</dependency>

<!-- Rest Assured for API Testing -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito for Feign Client Mocking -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>

<!-- JUnit 5 (Already included in spring-boot-starter-test) -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ for Fluent Assertions -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Boot Test (Already included) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.junit.vintage</groupId>
            <artifactId>junit-vintage-engine</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Feign Client for Synchronous Testing -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Resilience4j Circuit Breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-core</artifactId>
</dependency>

<!-- TestContainers (Optional for advanced integration tests) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

## Vérification des Dépendances

```bash
# Vérifier que toutes les dépendances sont correctement téléchargées
mvn dependency:resolve

# Voir l'arborescence des dépendances
mvn dependency:tree

# Valider le pom.xml
mvn validate
```

## Configuration dans Parent POM

Si vous utilisez un parent POM, assurez-vous qu'il inclut les versions appropriées :

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
    <relativePath/>
</parent>

<properties>
    <java.version>17</java.version>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Versions Recommandées

| Dépendance | Version | Note |
|-----------|---------|------|
| Java | 17+ | Minimum requis |
| Spring Boot | 3.2.0+ | Latest LTS |
| Spring Cloud | 2023.0.0+ | Compatible avec 3.2.x |
| H2 Database | 2.2.224 | Latest stable |
| JUnit | 5.10+ | Inclus dans spring-boot-starter-test |
| AssertJ | 3.24+ | Latest |
| Mockito | 5.0+ | Latest |
| Rest Assured | 5.4+ | Latest |

## Vérification Post-Installation

Après avoir ajouté les dépendances, exécutez :

```bash
# Compilez pour vérifier que tout est correct
mvn clean compile

# Exécutez un test rapide
mvn test -Dspring.profiles.active=test-sync -Dtest=UserRegistrationE2ETest

# Si succès, vous verrez :
# ✅ Tests passed
```

## Fichiers de Configuration

Assurez-vous que les fichiers suivants existent :

```
user-service/src/test/resources/application-test-sync.properties
wallet-service/src/test/resources/application-test-sync.properties
transaction-service/src/test/resources/application-test-sync.properties
```

Si manquants, ils ont été créés avec le guide de configuration.

## Dépannage des Dépendances

### Erreur: "H2 class not found"
```bash
# Solution: Nettoyez et reconstruisez
mvn clean install
mvn test -Dspring.profiles.active=test-sync
```

### Erreur: "Feign client not available"
```bash
# Vérifiez que spring-cloud-starter-openfeign est présent
mvn dependency:tree | grep feign
```

### Erreur: "Test configuration not found"
```bash
# Vérifiez le chemin et le contenu du fichier
# application-test-sync.properties
cat user-service/src/test/resources/application-test-sync.properties
```

---

**Besoin de détails supplémentaires ?** Consultez les documentations officielles :
- Spring Boot: https://spring.io/projects/spring-boot
- Spring Cloud: https://spring.io/projects/spring-cloud
- H2 Database: https://www.h2database.com/
