# ✅ Plan de Test Microservices - PRÊT À DÉMARRER

## État du Projet

| Élément | Statut | Détails |
|---------|--------|---------|
| **Compilation** | ✅ SUCCESS | `mvn clean compile -q -DskipTests` en 47s |
| **Configurations** | ✅ UPDATED | localhost:5432 et localhost:9092 |
| **Docker Compose** | ✅ UPDATED | Kafka + Zookeeper ajoutés |
| **Scripts Démarrage** | ✅ CREATED | 6 fichiers .bat pour chaque service |
| **Documentation** | ✅ COMPLETE | Guide complet + Commandes rapides |

---

## 📍 Architecture

```
┌─────────────────────────────────────────────────────┐
│                 API Gateway (8080)                  │
└──────────────┬──────────────────────────────────────┘
               │
    ┌──────────┼──────────┬──────────┐
    │          │          │          │
    ▼          ▼          ▼          ▼
 Auth      Wallet     Transaction  Notification
(8081)     (8082)      (8083)       (8084)
    │          │          │          │
    └──────────┴──────────┴──────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    ▼          ▼          ▼
Eureka    PostgreSQL   Kafka
(8761)    (5432)       (9092)
(8888)
```

---

## 🚀 Démarrage Étape par Étape

### IMPORTANT: Prérequis
✅ **PostgreSQL** doit être en cours d'exécution sur localhost:5432
✅ **Kafka** doit être en cours d'exécution sur localhost:9092

---

### ÉTAPE 1: Eureka Server (8761)

```powershell
# Ouvrir PowerShell ou CMD dans: C:\Users\HP\Downloads\zaphira-15-12-2025
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl service-registry spring-boot:run
```

**Attendre:** "Tomcat started on port(s): 8761"

**Vérification:**
```powershell
# Dans une autre fenêtre PowerShell
Invoke-WebRequest http://localhost:8761
```

✅ Vous devriez voir la page HTML du dashboard Eureka

---

### ÉTAPE 2: Config Server (8888)

Ouvrir une **NOUVELLE fenêtre PowerShell**:

```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl config-server spring-boot:run
```

**Attendre:** "Tomcat started on port(s): 8888"

**Vérification:**
```powershell
Invoke-WebRequest http://localhost:8888/health
```

✅ Response: `{"status":"UP"}`

---

### ÉTAPE 3: Auth Service (8081)

Ouvrir une **NOUVELLE fenêtre PowerShell**:

```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl auth spring-boot:run
```

**Attendre:** "Started AuthApplication in ... seconds"

**Vérification:**
```powershell
Invoke-WebRequest http://localhost:8081/actuator/health
```

✅ Response: `{"status":"UP"}`

**Test 1: Créer un utilisateur**
```powershell
$body = @{
    email = "alice@example.com"
    password = "Alice123!"
    firstName = "Alice"
    lastName = "Smith"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $body

$response.Content | ConvertFrom-Json | Select-Object userId, email, firstName
```

**Expected Output:**
```
userId         : 550e8400-e29b-41d4-a716-446655440000
email          : alice@example.com
firstName      : Alice
```

✅ **Sauvegardez USER_ID_1 et USER_ID**

**Test 2: Login**
```powershell
$loginBody = @{
    email = "alice@example.com"
    password = "Alice123!"
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $loginBody

$loginResponse.Content | ConvertFrom-Json | Select-Object accessToken, expiresIn
```

✅ **Sauvegardez le TOKEN**

---

### ÉTAPE 4: Wallet Service (8082)

Ouvrir une **NOUVELLE fenêtre PowerShell**:

```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl wallet-service spring-boot:run
```

**Attendre:** "Started WalletApplication in ... seconds"

**Vérification:**
```powershell
Invoke-WebRequest http://localhost:8082/actuator/health
```

✅ Response: `{"status":"UP"}`

**Test: Créer un wallet**
```powershell
$TOKEN = "YOUR_ACCESS_TOKEN"
$USER_ID = "YOUR_USER_ID"

$walletBody = @{
    userId = $USER_ID
    currency = "USD"
    initialBalance = 1000.00
} | ConvertTo-Json

$walletResponse = Invoke-WebRequest -Uri "http://localhost:8082/api/wallets" `
  -Method POST `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $TOKEN"
  } `
  -Body $walletBody

$walletResponse.Content | ConvertFrom-Json | Select-Object walletId, currency, availableBalance
```

✅ **Sauvegardez WALLET_ID**

---

### ÉTAPE 5: Transaction Service (8083)

Ouvrir une **NOUVELLE fenêtre PowerShell**:

```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl transaction-service spring-boot:run
```

**Attendre:** "Started TransactionApplication in ... seconds"

**Vérification:**
```powershell
Invoke-WebRequest http://localhost:8083/actuator/health
```

✅ Response: `{"status":"UP"}`

---

### ÉTAPE 6: Notification Service (8084)

Ouvrir une **NOUVELLE fenêtre PowerShell**:

```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl notification-service spring-boot:run
```

**Attendre:** "Started NotificationApplication in ... seconds"

**Vérification:**
```powershell
Invoke-WebRequest http://localhost:8084/actuator/health
```

✅ Response: `{"status":"UP"}`

---

## 🧪 Tests de Flux Complet

Une fois tous les services démarrés:

### Test 1: Créer Utilisateur 2
```powershell
$body = @{
    email = "bob@example.com"
    password = "Bob123!"
    firstName = "Bob"
    lastName = "Johnson"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $body

$response.Content | ConvertFrom-Json | Select-Object userId, email
```

✅ **Sauvegardez USER_ID_2**

### Test 2: Login Utilisateur 2
```powershell
$loginBody = @{
    email = "bob@example.com"
    password = "Bob123!"
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $loginBody

$token2 = ($loginResponse.Content | ConvertFrom-Json).accessToken
Write-Host "Token 2: $token2"
```

✅ **Sauvegardez TOKEN_2 et WALLET_ID_2**

### Test 3: Créer Wallet pour Bob
```powershell
$TOKEN2 = "YOUR_TOKEN_2"
$USER_ID_2 = "YOUR_USER_ID_2"

$walletBody = @{
    userId = $USER_ID_2
    currency = "USD"
    initialBalance = 500.00
} | ConvertTo-Json

$walletResponse = Invoke-WebRequest -Uri "http://localhost:8082/api/wallets" `
  -Method POST `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $TOKEN2"
  } `
  -Body $walletBody

($walletResponse.Content | ConvertFrom-Json).walletId
```

### Test 4: Transaction Alice → Bob (100 USD)
```powershell
$TOKEN1 = "YOUR_TOKEN_1"
$WALLET_ID_1 = "YOUR_WALLET_ID_1"
$USER_ID_2 = "YOUR_USER_ID_2"

$txBody = @{
    sourceWalletId = $WALLET_ID_1
    destinationUserId = $USER_ID_2
    amount = 100.00
    description = "Payment for services"
} | ConvertTo-Json

$txResponse = Invoke-WebRequest -Uri "http://localhost:8083/api/transactions" `
  -Method POST `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $TOKEN1"
  } `
  -Body $txBody

$txResponse.Content | ConvertFrom-Json | Select-Object transactionId, status, amount
```

✅ **Status devrait être COMPLETED**

### Test 5: Vérifier Notifications Alice
```powershell
$TOKEN1 = "YOUR_TOKEN_1"
$USER_ID_1 = "YOUR_USER_ID_1"

$notif = Invoke-WebRequest -Uri "http://localhost:8084/api/notifications/user/$USER_ID_1" `
  -Headers @{"Authorization" = "Bearer $TOKEN1"}

$notif.Content | ConvertFrom-Json | Select-Object -ExpandProperty content
```

✅ **Vous devriez voir une notification WALLET_BALANCE_UPDATED**

---

## 📊 Checklist de Vérification

| Service | Port | Status | Check |
|---------|------|--------|-------|
| Eureka | 8761 | ✅ Running | Dashboard visible |
| Config | 8888 | ✅ Running | /health = UP |
| Auth | 8081 | ✅ Running | Login fonctionne |
| Wallet | 8082 | ✅ Running | Wallet créé |
| Transaction | 8083 | ✅ Running | Transaction complétée |
| Notification | 8084 | ✅ Running | Notifications créées |

---

## 🔧 Dépannage Rapide

### "Connection refused (5432)"
**Problème:** PostgreSQL n'est pas en cours d'exécution
**Solution:** Installez et démarrez PostgreSQL, créez la base `wallet_db`

### "Connection refused (9092)"
**Problème:** Kafka n'est pas en cours d'exécution
**Solution:** Démarrez Kafka + Zookeeper (Docker ou installation locale)

### "503 Service Unavailable"
**Problème:** Service n'est pas enregistré dans Eureka
**Solution:** Attendez 30 secondes supplémentaires après le démarrage

### "401 Unauthorized"
**Problème:** Token JWT invalide
**Solution:** Générez un nouveau token avec `/api/auth/login`

### Port déjà utilisé
```powershell
# Trouver quel processus utilise le port
Get-NetTCPConnection -LocalPort 8081 | Select-Object OwningProcess

# Tuer le processus
Stop-Process -Id <PID> -Force
```

---

## 📚 Fichiers de Référence

- [GUIDE_DEMARRAGE_TEST.md](./GUIDE_DEMARRAGE_TEST.md) - Guide complet avec tous les détails
- [COMMANDES_RAPIDES.md](./COMMANDES_RAPIDES.md) - Commandes PowerShell prêtes à copier-coller
- [docker/docker-compose.yml](./docker/docker-compose.yml) - Composition Docker mise à jour
- [TEST_MICROSERVICES.md](./TEST_MICROSERVICES.md) - Architecture et points de contrôle

---

## ⚡ Résumé

✅ **Projet 100% compilé et prêt au test**
✅ **Configurations corrigées pour localhost**
✅ **Docker Compose avec Kafka**
✅ **Documentation complète**
✅ **Scripts de démarrage créés**

👉 **Prochaine étape:** Démarrer PostgreSQL et Kafka, puis les services un par un
