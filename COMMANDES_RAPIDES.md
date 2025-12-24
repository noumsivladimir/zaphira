# 🎯 Commandes Rapides - Démarrage Microservices

## Terminal 1 - Eureka Server
```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl service-registry spring-boot:run
```
**Port:** 8761 | **URL:** http://localhost:8761

---

## Terminal 2 - Config Server
```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl config-server spring-boot:run
```
**Port:** 8888 | **URL:** http://localhost:8888/health

---

## Terminal 3 - Auth Service
```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl auth spring-boot:run
```
**Port:** 8081

### Test rapide:
```powershell
# Créer un utilisateur
$body = @{
    email = "test@example.com"
    password = "Password123!"
    firstName = "Test"
    lastName = "User"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8081/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $body

# Login
$loginBody = @{
    email = "test@example.com"
    password = "Password123!"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $loginBody
```

---

## Terminal 4 - Wallet Service
```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl wallet-service spring-boot:run
```
**Port:** 8082

### Test rapide:
```powershell
# Créer un wallet (besoin du TOKEN et USER_ID)
$body = @{
    userId = "{USER_ID}"
    currency = "USD"
    initialBalance = 1000.00
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8082/api/wallets" `
  -Method POST `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer {TOKEN}"
  } `
  -Body $body
```

---

## Terminal 5 - Transaction Service
```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl transaction-service spring-boot:run
```
**Port:** 8083

### Test rapide:
```powershell
# Créer une transaction
$body = @{
    sourceWalletId = "{WALLET_ID_1}"
    destinationUserId = "{USER_ID_2}"
    amount = 100.00
    description = "Test transfer"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8083/api/transactions" `
  -Method POST `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer {TOKEN}"
  } `
  -Body $body
```

---

## Terminal 6 - Notification Service
```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn -pl notification-service spring-boot:run
```
**Port:** 8084

### Test rapide:
```powershell
# Récupérer les notifications
Invoke-WebRequest -Uri "http://localhost:8084/api/notifications/user/{USER_ID}" `
  -Headers @{"Authorization" = "Bearer {TOKEN}"}

# Nombre de notifications non lues
Invoke-WebRequest -Uri "http://localhost:8084/api/notifications/user/{USER_ID}/unread-count" `
  -Headers @{"Authorization" = "Bearer {TOKEN}"}

# Marquer une notification comme lue
Invoke-WebRequest -Uri "http://localhost:8084/api/notifications/{NOTIFICATION_ID}/read" `
  -Method PUT `
  -Headers @{"Authorization" = "Bearer {TOKEN}"}
```

---

## 📌 Variables à Remplacer

| Variable | Description | Exemple |
|----------|-------------|---------|
| `{TOKEN}` | JWT Access Token | `eyJhbGc...` |
| `{USER_ID}` | ID Utilisateur | `550e8400-e29b-41d4-a716-446655440000` |
| `{WALLET_ID_1}` | ID Wallet 1 | `550e8400-e29b-41d4-a716-446655440001` |
| `{WALLET_ID_2}` | ID Wallet 2 | `550e8400-e29b-41d4-a716-446655440002` |
| `{USER_ID_2}` | ID Utilisateur 2 | `550e8400-e29b-41d4-a716-446655440003` |
| `{NOTIFICATION_ID}` | ID Notification | `550e8400-e29b-41d4-a716-446655440004` |

---

## ✅ Ordre Recommandé

1. **Eureka Server** (8761) - Attendez 5 secondes
2. **Config Server** (8888) - Attendez 5 secondes
3. **Auth Service** (8081) - Attendez 10 secondes
4. **Wallet Service** (8082) - Attendez 10 secondes
5. **Transaction Service** (8083) - Attendez 10 secondes
6. **Notification Service** (8084) - Attendez 10 secondes

---

## 🔍 Vérification Santé

```powershell
# Tous les services
@(8761, 8888, 8081, 8082, 8083, 8084) | ForEach-Object {
    Write-Host "Port: $_"
    try {
        Invoke-WebRequest -Uri "http://localhost:$_/actuator/health" -ErrorAction Stop
        Write-Host "✅ OK`n"
    } catch {
        Write-Host "❌ NOT RUNNING`n"
    }
}
```

---

## 📊 Dashboard

- **Eureka:** http://localhost:8761
- **Swagger (si configuré):** http://localhost:8081/swagger-ui.html
