# Guide de Démarrage Rapide - Backend Zaphira

**Version:** 1.0.0  
**Date:** 4 Février 2026  
**Status:** ✅ Production Ready (après tests)

---

## 🚀 Quick Start

### Prérequis
```bash
- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Docker (optionnel)
```

### Démarrage Rapide
```bash
# 1. Cloner le projet
cd zaphira/

# 2. Compiler
mvn clean install -DskipTests

# 3. Lancer les services (dans l'ordre)
# Terminal 1: Service Registry
cd service-registry && mvn spring-boot:run

# Terminal 2: Config Server
cd config-server && mvn spring-boot:run

# Terminal 3: API Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 4-8: Microservices
cd auth && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd wallet-service && mvn spring-boot:run
cd transaction-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

---

## 📋 Structure Standardisée

Tous les microservices suivent maintenant cette structure:

```
{service-name}/
├── src/
│   ├── main/
│   │   ├── java/com/zaphira/{service-name}/
│   │   │   ├── {ServiceName}Application.java
│   │   │   ├── client/          # Feign clients
│   │   │   ├── config/          # Configuration
│   │   │   ├── controller/      # REST endpoints
│   │   │   ├── dto/             # DTOs
│   │   │   ├── event/           # Kafka events
│   │   │   ├── exception/       # Exceptions
│   │   │   ├── mapper/          # Entity-DTO mappers
│   │   │   ├── model/           # JPA entities, enums
│   │   │   ├── repository/      # Spring Data repos
│   │   │   ├── security/        # Security config
│   │   │   ├── service/         # Business logic
│   │   │   └── util/            # Utilities
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-{profile}.yml
│   └── test/
│       └── java/com/zaphira/{service-name}/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🎯 Endpoints Principaux (LOT 1)

### Transaction Service - Port 8083
**Base URL:** `http://localhost:8083/api/v1/transactions`

#### Créer un Transfer
```http
POST /transfer
Content-Type: application/json
Authorization: Bearer {token}

{
  "senderWalletId": 1,
  "receiverWalletId": 2,
  "amount": 100.00,
  "currency": "EUR",
  "description": "Payment for services"
}
```

#### Créer un Deposit
```http
POST /deposit?receiverWalletId=1&amount=500&currency=EUR&description=Initial deposit
Authorization: Bearer {token}
```

#### Créer un Withdrawal
```http
POST /withdrawal?senderWalletId=1&amount=200&currency=EUR&description=ATM withdrawal
Authorization: Bearer {token}
```

#### Annuler une Transaction
```http
POST /{reference}/cancel
Authorization: Bearer {token}
```

#### ⭐ Inverser une Transaction (NOUVEAU)
```http
POST /{reference}/reverse?reason=Erreur de facturation
Authorization: Bearer {admin_token}
```

#### ⭐ Rembourser une Transaction (NOUVEAU)
```http
POST /{reference}/refund?reason=Produit défectueux
Authorization: Bearer {admin_or_merchant_token}
```

#### Réessayer une Transaction Échouée
```http
POST /{reference}/retry
Authorization: Bearer {token}
```

#### Obtenir une Transaction
```http
GET /{reference}
Authorization: Bearer {token}
```

#### Historique Wallet
```http
GET /wallet/{walletId}?page=0&size=20
Authorization: Bearer {token}
```

---

### Wallet Service - Port 8082
**Base URL:** `http://localhost:8082/api/wallets`

#### Créer un Wallet
```http
POST /
Content-Type: application/json
Authorization: Bearer {token}

{
  "userId": 1,
  "currency": "EUR"
}
```

#### Obtenir un Wallet
```http
GET /{walletNumber}
Authorization: Bearer {token}
```

#### Résumé Wallet
```http
GET /user/{userId}/summary
Authorization: Bearer {token}
```

#### Geler un Wallet (ADMIN)
```http
PUT /{walletNumber}/freeze
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "reason": "Suspicious activity",
  "frozenBy": "ADMIN_001"
}
```

#### Dégeler un Wallet (ADMIN)
```http
PUT /{walletNumber}/unfreeze?unfrozenBy=ADMIN_001&notes=Verification complete
Authorization: Bearer {admin_token}
```

---

### User Service - Port 8084
**Base URL:** `http://localhost:8084/api/users`

#### Inscription
```http
POST /register
Content-Type: application/json

{
  "email": "user@example.com",
  "phoneNumber": "+33612345678",
  "pin": "1234",
  "firstName": "Jean",
  "lastName": "Dupont",
  "dateOfBirth": "1990-01-01",
  "country": "France"
}
```

#### Vérification OTP
```http
POST /verify-otp
Content-Type: application/json

{
  "identifier": "+33612345678",
  "code": "123456"
}
```

#### Obtenir Profil
```http
GET /profile
Authorization: Bearer {token}
```

---

### Auth Service - Port 8081
**Base URL:** `http://localhost:8081/api/auth`

#### Login
```http
POST /login
Content-Type: application/json

{
  "phoneNumber": "+33612345678",
  "pin": "1234"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "user@example.com",
  "role": "REGULAR"
}
```

---

## 🔐 Authentification

### Obtenir un Token
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+33612345678",
    "pin": "1234"
  }'
```

### Utiliser le Token
```bash
curl -X GET http://localhost:8083/api/v1/transactions/{reference} \
  -H "Authorization: Bearer {token}"
```

---

## 🧪 Tests

### Compiler sans Tests
```bash
mvn clean install -DskipTests
```

### Lancer les Tests
```bash
# Tous les tests
mvn test

# Service spécifique
mvn test -pl transaction-service

# Test spécifique
mvn test -Dtest=TransactionServiceIntegrationTest
```

---

## 📊 Statuts des Transactions

| Statut | Description | Peut Transiter Vers |
|--------|-------------|---------------------|
| PENDING | En attente de traitement | PROCESSING, CANCELLED |
| PROCESSING | En cours de traitement | COMPLETED, FAILED |
| COMPLETED | Terminée avec succès | REVERSED, REFUNDED |
| FAILED | Échouée | PENDING (retry) |
| CANCELLED | Annulée par l'utilisateur | - |
| REVERSED | Inversée par ADMIN | - |
| REFUNDED | Remboursée | - |

---

## 🔄 Workflows

### Workflow Transfer
```
1. Client → POST /transfer
2. Service valide montant et wallets
3. Service bloque les fonds (sender)
4. Service débite sender
5. Service crédite receiver
6. Service complète transaction
7. Service publie événement
8. Notification envoyée
```

### Workflow Reverse
```
1. ADMIN → POST /{ref}/reverse
2. Service vérifie: status=COMPLETED
3. Service crée transaction inverse
4. Service exécute transfer inverse
5. Service marque originale: REVERSED
6. Notification envoyée
```

### Workflow Refund
```
1. ADMIN/Merchant → POST /{ref}/refund
2. Service vérifie: status=COMPLETED
3. Service crée transaction refund
4. Service exécute transfer refund
5. Service marque originale: REFUNDED
6. Notification envoyée
```

---

## 🐛 Debugging

### Logs
```bash
# Activer debug logging
# application.yml
logging:
  level:
    com.zaphira: DEBUG
```

### Ports par Défaut
| Service | Port |
|---------|------|
| Service Registry | 8761 |
| Config Server | 8888 |
| API Gateway | 8080 |
| Auth Service | 8081 |
| Wallet Service | 8082 |
| Transaction Service | 8083 |
| User Service | 8084 |
| Notification Service | 8085 |

---

## 🚨 Troubleshooting

### Service ne démarre pas
```bash
# Vérifier les ports
netstat -ano | findstr :8080

# Vérifier PostgreSQL
psql -h localhost -U postgres -d zaphira_db

# Vérifier logs
tail -f logs/application.log
```

### Erreurs de Compilation
```bash
# Nettoyer et recompiler
mvn clean install -U

# Supprimer .m2 cache
rm -rf ~/.m2/repository/com/zaphira
```

### Tests Échouent
```bash
# Désactiver JWT pour tests
# application-test.yml
app:
  security:
    jwt:
      enabled: false
```

---

## 📚 Ressources

### Documentation
- [STANDARD_MICROSERVICE_STRUCTURE.md](./STANDARD_MICROSERVICE_STRUCTURE.md) - Standards de structure
- [LOT1_IMPLEMENTATION_AUDIT.md](./LOT1_IMPLEMENTATION_AUDIT.md) - Audit implémentation
- [RESTRUCTURATION_COMPLETE.md](./RESTRUCTURATION_COMPLETE.md) - Rapport restructuration
- [lot-workflows.md](./docs/workflows/lot-workflows.md) - Spécifications LOT 1-6

### Endpoints Swagger
- Transaction Service: http://localhost:8083/swagger-ui.html
- Wallet Service: http://localhost:8082/swagger-ui.html
- User Service: http://localhost:8084/swagger-ui.html

---

## ✅ Checklist Mise en Production

### Avant le Déploiement
- [ ] Tous les tests passent
- [ ] Variables d'environnement configurées
- [ ] Base de données migrée
- [ ] Secrets/JWT configurés
- [ ] HTTPS activé
- [ ] Rate limiting configuré
- [ ] Monitoring activé
- [ ] Logs configurés
- [ ] Backups automatiques

### Post-Déploiement
- [ ] Health checks OK
- [ ] Endpoints testés
- [ ] Performance validée
- [ ] Sécurité auditée
- [ ] Documentation à jour

---

## 🎉 Félicitations!

Votre backend Zaphira est maintenant **structuré**, **complet** et **prêt**!

**Status:** 🟢 **READY FOR INTEGRATION TESTING**

Pour toute question: voir [RESTRUCTURATION_COMPLETE.md](./RESTRUCTURATION_COMPLETE.md)
