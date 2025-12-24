# Test des Microservices - Plan d'action

## Prérequis
- ✅ Compilation réussie : `mvn clean compile -q`
- ✅ Tous les modules compilent correctement
- ✅ Configuration Docker Compose mise à jour avec Kafka

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                       │
└──────────────┬──────────────────────────────────────────────┘
               │
    ┌──────────┼──────────┬──────────┐
    │          │          │          │
    ▼          ▼          ▼          ▼
Auth-Service Wallet      Transaction Notification
(8081)       Service     Service     Service
             (8082)      (8083)      (8084)
    │          │          │          │
    └──────────┴──────────┴──────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    ▼          ▼          ▼
PostgreSQL  Kafka      Eureka
(5432)      (9092)     (8761)
```

## Services à Tester

### 1. Service Registry (Eureka) - Port 8761
```bash
mvn -pl service-registry spring-boot:run
```
- Healthcheck: http://localhost:8761

### 2. Config Server - Port 8888
```bash
mvn -pl config-server spring-boot:run
```

### 3. Auth Service - Port 8081
```bash
mvn -pl auth spring-boot:run
```

**Endpoints:**
- POST /api/auth/register - Créer utilisateur
- POST /api/auth/login - Login utilisateur
- POST /api/auth/refresh - Refresh token

### 4. Wallet Service - Port 8082
```bash
mvn -pl wallet-service spring-boot:run
```

**Endpoints:**
- POST /api/wallets - Créer wallet
- GET /api/wallets/{userId} - Récupérer wallet
- PATCH /api/wallets/{walletId}/balance - Mettre à jour solde

### 5. Transaction Service - Port 8083
```bash
mvn -pl transaction-service spring-boot:run
```

**Endpoints:**
- POST /api/transactions - Créer transaction
- GET /api/transactions/{userId} - Lister transactions
- GET /api/transactions/{transactionId} - Détail transaction

### 6. Notification Service - Port 8084
```bash
mvn -pl notification-service spring-boot:run
```

**Endpoints:**
- GET /api/notifications/user/{userId} - Récupérer notifications
- GET /api/notifications/user/{userId}/unread-count
- PUT /api/notifications/{id}/read - Marquer comme lu

## Commandes de Test

### Test 1: Inscription utilisateur
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### Test 2: Création de wallet
```bash
curl -X POST http://localhost:8080/api/wallets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "userId": "{USER_ID}",
    "currency": "USD",
    "initialBalance": 1000.00
  }'
```

### Test 3: Transaction
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "sourceWalletId": "{WALLET_ID}",
    "destinationUserId": "{RECIPIENT_ID}",
    "amount": 50.00,
    "description": "Test transfer"
  }'
```

### Test 4: Récupérer notifications
```bash
curl -X GET http://localhost:8080/api/notifications/user/{USER_ID} \
  -H "Authorization: Bearer {TOKEN}"
```

## Flux de Test Complet

1. **Démarrer l'infrastructure:**
   - Eureka Server (8761)
   - Config Server (8888)
   - PostgreSQL (5432)
   - Kafka (9092)

2. **Démarrer les services:**
   - Auth Service (8081)
   - Wallet Service (8082)
   - Transaction Service (8083)
   - Notification Service (8084)

3. **Exécuter le scénario complet:**
   - Créer utilisateur A
   - Créer utilisateur B
   - Créer wallet pour A (1000 USD)
   - Créer wallet pour B (500 USD)
   - Effectuer transaction A → B (100 USD)
   - Vérifier notifications (balance updated)
   - Marquer notifications comme lues

4. **Vérifications Kafka:**
   - Topics créés : notification.transaction.events, notification.wallet.events, notification.user.events
   - Events publiés correctement
   - Consumers lisent les messages

## Points de Contrôle

- [ ] Eureka enregistre tous les services
- [ ] Tokens JWT générés correctement
- [ ] Wallets créés avec la bonne structure BD
- [ ] Transactions validées et persistées
- [ ] Notifications créées après événements
- [ ] Événements Kafka publiés correctement
- [ ] API Gateway route correctement vers les services
- [ ] Gestion des erreurs fonctionnelle
