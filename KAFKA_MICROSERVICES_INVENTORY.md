# 📊 Inventaire Complet des Microservices - Kafka & BDD

## 📋 Tableau Récapitulatif

| **Microservice** | **Producteurs Kafka** | **Consommateurs Kafka** | **Topics** | **Base de Données** |
|---|---|---|---|---|
| **auth-service** | `KafkaConfig.java` - KafkaTemplate<String, UserRegisteredEvent> | ❌ Aucun | `user-registered` (publié par auth) | PostgreSQL<br/>- auth_db<br/>- Port: 5432<br/>- Hibernate: create-drop |
| **user-service** | `UserEventProducer.java` - KafkaTemplate<String, UserCreatedEvent> | `WalletResponseListener.java` (@KafkaListener sur wallet-created-topic) | **Publie:**<br/>- user-created-topic<br/><br/>**Consomme:**<br/>- wallet-created-topic | PostgreSQL<br/>- wallet_db<br/>- Port: 5432<br/>- Hibernate: update |
| **wallet-service** | `UserEventConsumer.java` - KafkaTemplate<String, WalletCreatedEvent> | `UserEventConsumer.java` (@KafkaListener sur user-created)<br/>`UserEventListener.java` (@KafkaListener sur user-registered) | **Publie:**<br/>- wallet-created-topic<br/><br/>**Consomme:**<br/>- user-created-topic<br/>- user-registered | PostgreSQL<br/>- wallet_db<br/>- Port: 5432<br/>- Hibernate: create-drop |
| **transaction-service** | `ValidationRequestProducer.java` - KafkaTemplate<String, TransactionValidationRequest> | ❌ Consommateurs potentiels non trouvés | **Publie:**<br/>- transaction.validation.request | PostgreSQL<br/>- wallet_db<br/>- Port: 5432<br/>- Hibernate: update |
| **notification-service** | ❌ Aucun producteur | `UserEventListener.java` (@KafkaListener sur user-registered)<br/>`TransactionEventListener.java` (@KafkaListener sur transaction-created)<br/>`UserEventConsumer.java` (@KafkaListener sur notification.user.events)<br/>`TransactionEventConsumer.java` (@KafkaListener sur notification.transaction.events)<br/>`WalletEventConsumer.java` (@KafkaListener sur notification.wallet.events) | **Consomme:**<br/>- user-registered<br/>- transaction-created<br/>- notification.user.events<br/>- notification.transaction.events<br/>- notification.wallet.events | PostgreSQL<br/>- notification_db<br/>- Port: 5432<br/>- Hibernate: validate |
| **common-library** | ❌ Pas de producteur (Librairie partagée) | ❌ Pas de consommateur | Contient les définitions d'événements (UserRegisteredEvent, WalletCreatedEvent, TransactionCreatedEvent, etc.) | ❌ N/A (Librairie) |

---

## 🔄 Flux d'Événements Kafka

### 1️⃣ Flux d'Enregistrement Utilisateur
```
auth-service
    ↓ (publie UserRegisteredEvent)
    ↓ topic: user-registered
    ├→ wallet-service (UserEventListener)
    └→ notification-service (UserEventListener)
```

### 2️⃣ Flux de Création de Compte Utilisateur
```
user-service (UserEventProducer)
    ↓ (publie UserCreatedEvent)
    ↓ topic: user-created-topic
    ├→ wallet-service (UserEventConsumer)
    └→ [WalletResponseListener attend réponse]
```

### 3️⃣ Flux de Création de Portefeuille
```
wallet-service (UserEventConsumer)
    ↓ (crée wallet et publie WalletCreatedEvent)
    ↓ topic: wallet-created-topic
    └→ user-service (WalletResponseListener)
```

### 4️⃣ Flux de Validation de Transaction
```
transaction-service (ValidationRequestProducer)
    ↓ (publie TransactionValidationRequest)
    ↓ topic: transaction.validation.request
    └→ [Consommateur externe non trouvé]
```

### 5️⃣ Flux de Notifications
```
transaction-service / user-service / wallet-service
    ↓ (publient événements de domaine)
    ↓ topics variés (transaction-created, etc.)
    └→ notification-service (multiples listeners)
```

---

## 📦 Dépendances Kafka par Microservice

| Microservice | Dépendances |
|---|---|
| auth-service | ✅ spring-kafka<br/>✅ PostgreSQL 42.7.3 |
| user-service | ✅ spring-kafka<br/>✅ PostgreSQL |
| wallet-service | ✅ spring-kafka<br/>✅ PostgreSQL |
| transaction-service | ✅ spring-kafka<br/>✅ PostgreSQL 42.7.3 |
| notification-service | ✅ spring-kafka<br/>✅ spring-kafka-test<br/>✅ PostgreSQL<br/>✅ Flyway (migration DB) |
| common-library | ✅ spring-kafka (optional)<br/>✅ spring-data-jpa |

---

## 🗄️ Configuration des Bases de Données

### Auth Service
- **URL**: `jdbc:postgresql://192.168.0.122:5432/auth_db`
- **Utilisateur**: postgres
- **Password**: 1234
- **DDL Strategy**: `create-drop` (réinitialise à chaque démarrage)

### User Service
- **URL**: `jdbc:postgresql://192.168.0.122:5432/wallet_db`
- **Utilisateur**: postgres
- **Password**: 1234
- **DDL Strategy**: `update` (évolution progressive du schéma)

### Wallet Service
- **URL**: `jdbc:postgresql://localhost:5432/wallet_db`
- **Utilisateur**: postgres
- **Password**: 1234
- **DDL Strategy**: `create-drop` (réinitialise à chaque démarrage)

### Transaction Service
- **URL**: `jdbc:postgresql://192.168.0.122:5432/wallet_db`
- **Utilisateur**: postgres
- **Password**: 1234
- **DDL Strategy**: `update` (évolution progressive du schéma)

### Notification Service
- **URL**: `jdbc:postgresql://localhost:5432/notification_db`
- **Utilisateur**: postgres
- **Password**: postgres
- **DDL Strategy**: `validate` (refuse de démarrer si schéma ne correspond pas)
- **Migrations**: Flyway (fichiers de migration en DB)

---

## 🔌 Configuration Kafka

### Bootstrap Servers
- **Locale**: `localhost:9092`
- **Réseau**: `192.168.0.122:9092`

### Sérialisation
- **Producteurs**: `StringSerializer` (clés) + `JsonSerializer` (valeurs)
- **Consommateurs**: `StringDeserializer` (clés) + `JsonDeserializer` (valeurs)

### Consumer Groups
| Microservice | Consumer Group |
|---|---|
| user-service | `user-service-group` |
| wallet-service | `wallet-service-group` |
| transaction-service | `transaction-service-validation-result` |
| notification-service | `notification-service` / `notification-service-*-group` |
| auth-service | `auth-service-group` |

### Propriétés Communes
- `auto-offset-reset`: `earliest`
- `enable-auto-commit`: `false` (accréditation manuelle où applicable)
- `spring.json.trusted.packages`: `*`

---

## 📝 Topics Utilisés (Complète List)

| Topic | Producteur | Consommateurs | Type |
|---|---|---|---|
| **user-registered** | auth-service | wallet-service, notification-service | Événement utilisateur |
| **user-created-topic** | user-service | wallet-service | Événement utilisateur |
| **wallet-created-topic** | wallet-service | user-service | Événement portefeuille |
| **transaction.validation.request** | transaction-service | ❌ [Non trouvé] | Requête validation |
| **notification.user.events** | ❌ [Non trouvé] | notification-service | Événement notification |
| **notification.transaction.events** | ❌ [Non trouvé] | notification-service | Événement notification |
| **notification.wallet.events** | ❌ [Non trouvé] | notification-service | Événement notification |
| **transaction-created** | ❌ [Non trouvé] | notification-service | Événement transaction |
| **wallet.updated** | ❌ [Non trouvé] | notification-service | Événement portefeuille |

---

## ⚠️ Observations & Lacunes

### 🔴 Problèmes Identifiés

1. **Transaction Service**
   - Publie `TransactionValidationRequest` sur `transaction.validation.request`
   - ❌ **Aucun consommateur trouvé** pour cette requête
   - Risque: Messages accumulés dans Kafka sans traitement

2. **Notification Service**
   - Écoute 5 topics différents mais les producteurs sont absents ou non trouvés
   - Topics `notification.*.events` ne semblent pas être publiés

3. **Incohérence de Bases de Données**
   - wallet-service utilise `localhost:5432`
   - Autres services utilisent `192.168.0.122:5432`
   - **Risque**: Deux instances PostgreSQL différentes potentielles

4. **Stratégies DDL Incohérentes**
   - `create-drop`: auth-service, wallet-service (données perdues à chaque redémarrage)
   - `update`: user-service, transaction-service (évolution progressive)
   - `validate`: notification-service (bloquant si schéma absent)

### 🟡 Avertissements

1. **Mode de Développement Détecté**
   - JWT secrets hardcodés
   - Connexions localhost/réseau mixtes
   - Passwords simples (1234, postgres)

2. **Manque de Configuration Centralisée**
   - Topics définis dans `@Value` et fichiers de config dispersés
   - Pas de registre centralisé des topics

3. **Authentification Manquante**
   - Aucun mécanisme de sécurité Kafka (SSL/SASL)
   - Suitable only for development/testing

---

## 📊 Statistiques

| Métrique | Valeur |
|---|---|
| **Microservices totaux** | 6 (+ 3 services d'infrastructure) |
| **Producteurs Kafka** | 3 microservices |
| **Consommateurs Kafka** | 4 microservices |
| **Topics actifs** | 9 (dont 3 orphelins) |
| **Bases de données** | 5 instances PostgreSQL |
| **Versions Spring Boot** | 3.3.4 (common-library) |
| **Versions PostgreSQL** | 42.7.3 (multiple) |

---

## 🚀 Recommandations

### Immédiat
- [ ] Identifier le consommateur manquant pour `transaction.validation.request`
- [ ] Aligner les URLs PostgreSQL (localhost vs 192.168.0.122)
- [ ] Centraliser la configuration des topics Kafka

### Court Terme
- [ ] Implémenter les producteurs manquants pour notification-service
- [ ] Aligner les stratégies DDL (utiliser `update` partout en production)
- [ ] Ajouter retry et Dead Letter Queue (DLQ) policies

### Moyen Terme
- [ ] Implémenter la sécurité Kafka (SSL/SASL)
- [ ] Ajouter le monitoring Kafka (Prometheus/Grafana)
- [ ] Documenter le schéma des événements

---

**Généré le:** 21 Décembre 2025  
**Couverture:** Tous les microservices principaux  
**État:** ✅ Analyse complète du code source
