# 🧪 Stratégie de Test Synchrone (SQLite + Feign)

## Objectif
Implémenter des tests d'intégration **sans dépendances externes** (PostgreSQL, Kafka) sur une seule machine.

## Architecture de Test

### 1. **Configuration SQLite (H2)**
- **Base de données** : SQLite en mémoire pour les tests
- **Localisation** : `application-test-sync.properties`
- **Avantage** : Démarrage rapide, isolation complète

### 2. **Communication Feign au lieu de Kafka**
```
┌─────────────────┐
│  User Service   │
│  (Test Profile) │
└────────┬────────┘
         │ Feign
         ↓
┌─────────────────┐
│ Wallet Service  │
│ (Test Profile)  │
└─────────────────┘
         │ Feign
         ↓
┌─────────────────┐
│ Transaction Svc │
│ (Test Profile)  │
└─────────────────┘
```

### 3. **Profils Spring**
- `test-sync` : Active SQLite + Feign messaging
- `local-test` : Mode test local (base de données en mémoire)

## Étapes d'Implémentation

### Phase 1 : Configuration de Base
- [ ] Créer `application-test-sync.properties` pour chaque service
- [ ] Configurer SQLite/H2 dans les tests
- [ ] Ajouter dépendances : h2, rest-assured

### Phase 2 : Contrôleurs de Test
- [ ] Créer `/test-sync/messages/{topic}` endpoints
- [ ] Implémenter routing vers handlers métier
- [ ] Ajouter fallback Feign

### Phase 3 : Tests d'Intégration
- [ ] Tests unitaires services
- [ ] Tests intégration BD + Feign
- [ ] Tests E2E complets

### Phase 4 : Automatisation
- [ ] Suite Maven pour tester tous les services
- [ ] CI/CD avec tests synchrones

## Services à Tester

| Service | Port | Tests |
|---------|------|-------|
| user-service | 8082 | ✓ Enregistrement, gestion utilisateurs |
| wallet-service | 8086 | ✓ Création portefeuille, transactions |
| transaction-service | 8083 | ✓ Transactions, règlements |
| notification-service | 8087 | ✓ Notifications |
| auth-service | 8089 | ✓ Authentication JWT |

## Exécution des Tests

```bash
# Tests avec profil test-sync
mvn test -Dspring.profiles.active=test-sync

# Tests isolés par service
mvn test -f user-service/pom.xml -Dspring.profiles.active=test-sync

# Tous les tests
mvn clean test -Dspring.profiles.active=test-sync
```

## Bénéfices
✅ Aucune infrastructure externe  
✅ Tests rapides et reproductibles  
✅ Facilement automatable  
✅ Fonctionne sur n'importe quelle machine  
✅ Intégration complète simulée via Feign
