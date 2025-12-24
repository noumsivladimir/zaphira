# 🚀 Guide de Démarrage et Test des Microservices

## 📋 Prérequis
- ✅ Maven 3.8+
- ✅ Java 17+
- ✅ PostgreSQL 15+ (localhost:5432)
- ✅ Kafka 3.0+ (localhost:9092)

## 🔧 Ordre de Démarrage

### Étape 1: Service Registry (Eureka) - Port 8761
```bash
# Windows
start-eureka.bat

# Linux/Mac
mvn -pl service-registry spring-boot:run
```

**Test:**
```bash
curl http://localhost:8761
```

Vous devriez voir le dashboard Eureka.

---

### Étape 2: Config Server - Port 8888
```bash
# Windows
start-config-server.bat

# Linux/Mac
mvn -pl config-server spring-boot:run
```

**Test:**
```bash
curl http://localhost:8888/health
```

---

### Étape 3: Auth Service - Port 8081
```bash
# Windows
start-auth-service.bat

# Linux/Mac
mvn -pl auth spring-boot:run
```

**Attendre:** "Started AuthApplication in ... seconds"

**Test 1: Health Check**
```bash
curl http://localhost:8081/actuator/health
```

Response:
```json
{"status":"UP"}
```

**Test 2: Créer un utilisateur**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

Response:
```json
{
  "userId": "uuid",
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "createdAt": "2025-12-19T18:00:00Z"
}
```

**Test 3: Login**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 900
}
```

---

### Étape 4: Wallet Service - Port 8082
```bash
# Windows
start-wallet-service.bat

# Linux/Mac
mvn -pl wallet-service spring-boot:run
```

**Attendre:** "Started WalletApplication in ... seconds"

**Test 1: Health Check**
```bash
curl http://localhost:8082/actuator/health
```

**Test 2: Créer un wallet**
```bash
curl -X POST http://localhost:8082/api/wallets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {ACCESS_TOKEN}" \
  -d '{
    "userId": "{USER_ID}",
    "currency": "USD",
    "initialBalance": 1000.00
  }'
```

Response:
```json
{
  "walletId": "uuid",
  "userId": "uuid",
  "currency": "USD",
  "availableBalance": 1000.00,
  "status": "ACTIVE",
  "createdAt": "2025-12-19T18:00:00Z"
}
```

**Test 3: Récupérer wallet**
```bash
curl http://localhost:8082/api/wallets/{USER_ID} \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

---

### Étape 5: Transaction Service - Port 8083
```bash
# Windows
start-transaction-service.bat

# Linux/Mac
mvn -pl transaction-service spring-boot:run
```

**Attendre:** "Started TransactionApplication in ... seconds"

**Test 1: Health Check**
```bash
curl http://localhost:8083/actuator/health
```

**Test 2: Créer une transaction**
```bash
curl -X POST http://localhost:8083/api/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {ACCESS_TOKEN}" \
  -d '{
    "sourceWalletId": "{WALLET_ID_1}",
    "destinationUserId": "{USER_ID_2}",
    "amount": 100.00,
    "description": "Test transfer"
  }'
```

Response:
```json
{
  "transactionId": "uuid",
  "status": "COMPLETED",
  "sourceWalletId": "uuid",
  "destinationWalletId": "uuid",
  "amount": 100.00,
  "description": "Test transfer",
  "createdAt": "2025-12-19T18:00:00Z"
}
```

**Test 3: Lister les transactions**
```bash
curl http://localhost:8083/api/transactions/{USER_ID} \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

---

### Étape 6: Notification Service - Port 8084
```bash
# Windows
start-notification-service.bat

# Linux/Mac
mvn -pl notification-service spring-boot:run
```

**Attendre:** "Started NotificationApplication in ... seconds"

**Test 1: Health Check**
```bash
curl http://localhost:8084/actuator/health
```

**Test 2: Récupérer les notifications (après transaction)**
```bash
curl http://localhost:8084/api/notifications/user/{USER_ID} \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

Response:
```json
{
  "content": [
    {
      "id": "uuid",
      "userId": "uuid",
      "eventType": "WALLET_BALANCE_UPDATED",
      "title": "Wallet Balance Updated",
      "message": "Your wallet balance has been updated: $900.00",
      "status": "PENDING",
      "priority": "NORMAL",
      "channel": "IN_APP",
      "createdAt": "2025-12-19T18:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0
}
```

**Test 3: Nombre de notifications non lues**
```bash
curl http://localhost:8084/api/notifications/user/{USER_ID}/unread-count \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

Response:
```json
{"unreadCount": 1}
```

**Test 4: Marquer une notification comme lue**
```bash
curl -X PUT http://localhost:8084/api/notifications/{NOTIFICATION_ID}/read \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

---

## 🧪 Scénario de Test Complet (E2E)

### Préparation
Notez les valeurs suivantes au fur et à mesure:
- `ACCESS_TOKEN_USER1` - Token après login utilisateur 1
- `USER_ID_1` - ID de l'utilisateur 1
- `WALLET_ID_1` - ID du wallet de l'utilisateur 1
- `USER_ID_2` - ID de l'utilisateur 2
- `WALLET_ID_2` - ID du wallet de l'utilisateur 2

### Exécution

**1. Créer Utilisateur 1**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "Alice123!",
    "firstName": "Alice",
    "lastName": "Smith"
  }'
```

Sauvegardez `USER_ID_1`

**2. Créer Utilisateur 2**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "bob@example.com",
    "password": "Bob123!",
    "firstName": "Bob",
    "lastName": "Johnson"
  }'
```

Sauvegardez `USER_ID_2`

**3. Login Utilisateur 1**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "Alice123!"
  }'
```

Sauvegardez `ACCESS_TOKEN_USER1`

**4. Créer Wallet pour Alice (1000 USD)**
```bash
curl -X POST http://localhost:8082/api/wallets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {ACCESS_TOKEN_USER1}" \
  -d '{
    "userId": "{USER_ID_1}",
    "currency": "USD",
    "initialBalance": 1000.00
  }'
```

Sauvegardez `WALLET_ID_1`

**5. Créer Wallet pour Bob (500 USD)**

D'abord, login avec Bob:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "bob@example.com",
    "password": "Bob123!"
  }'
```

Sauvegardez `ACCESS_TOKEN_USER2`

```bash
curl -X POST http://localhost:8082/api/wallets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {ACCESS_TOKEN_USER2}" \
  -d '{
    "userId": "{USER_ID_2}",
    "currency": "USD",
    "initialBalance": 500.00
  }'
```

Sauvegardez `WALLET_ID_2`

**6. Transaction: Alice → Bob (100 USD)**
```bash
curl -X POST http://localhost:8083/api/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {ACCESS_TOKEN_USER1}" \
  -d '{
    "sourceWalletId": "{WALLET_ID_1}",
    "destinationUserId": "{USER_ID_2}",
    "amount": 100.00,
    "description": "Payment for services"
  }'
```

**7. Vérifier Solde Alice**
```bash
curl http://localhost:8082/api/wallets/{USER_ID_1} \
  -H "Authorization: Bearer {ACCESS_TOKEN_USER1}"
```

Attendu: `availableBalance: 900.00`

**8. Vérifier Solde Bob**
```bash
curl http://localhost:8082/api/wallets/{USER_ID_2} \
  -H "Authorization: Bearer {ACCESS_TOKEN_USER2}"
```

Attendu: `availableBalance: 600.00`

**9. Vérifier Notifications Alice**
```bash
curl http://localhost:8084/api/notifications/user/{USER_ID_1} \
  -H "Authorization: Bearer {ACCESS_TOKEN_USER1}"
```

Attendu: Au moins 1 notification avec `eventType: WALLET_BALANCE_UPDATED`

**10. Vérifier Notifications Bob**
```bash
curl http://localhost:8084/api/notifications/user/{USER_ID_2} \
  -H "Authorization: Bearer {ACCESS_TOKEN_USER2}"
```

Attendu: Notification de réception de fonds

---

## 📊 Points de Contrôle

| Point | Attendu | Statut |
|-------|---------|--------|
| Eureka enregistre les services | 2 services actifs (Wallet, Transaction, Notification) | ✅ |
| Auth génère JWT valides | Token avec email et userId | ✅ |
| Wallet créé avec solde correct | Balance = 1000 USD | ✅ |
| Transaction déduit du portefeur source | Alice: 1000 → 900 | ✅ |
| Transaction ajoute au portefeur destination | Bob: 500 → 600 | ✅ |
| Notifications créées après transaction | 2+ notifications par utilisateur | ✅ |
| Kafka publie événements | Topics reçoivent les messages | ✅ |
| Notifications marquées comme lues | PUT /read fonctionne | ✅ |

---

## 🔍 Dépannage

### Erreur: Connection refused (5432)
```
PostgreSQL n'est pas démarré
Solution: Installez PostgreSQL et créez la base wallet_db
```

### Erreur: Connection refused (9092)
```
Kafka n'est pas démarré
Solution: Démarrez Kafka (via Docker ou installation locale)
```

### Erreur: 503 Service Unavailable
```
Service n'est pas enregistré dans Eureka
Solution: Attendez 30 secondes après démarrage du service
```

### Erreur: 401 Unauthorized
```
Token JWT invalide ou expiré
Solution: Générez un nouveau token avec /api/auth/login
```

---

## 📝 Notes

- Les transactions sont asynchrones (Kafka) → les notifications arrivent dans les 1-2 secondes
- Les tokens JWT expirent après 15 minutes
- Tous les soldes sont en `BigDecimal` (2 décimales)
- Les notifications sont stockées en BD PostgreSQL
