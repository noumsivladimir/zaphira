# Guide de Migration vers Architecture Microservices

## 📋 Structure Créée

```
zaphira-platform/
├── common-library/          ✅ Créé
├── service-registry/        ✅ Créé
├── config-server/           ✅ Créé
├── api-gateway/            ✅ Créé
├── auth-service/           ⚠️ À refactoriser
├── wallet-service/          ✅ Structure créée
├── transaction-service/     ⚠️ À mettre à jour
└── notification-service/    ✅ Structure créée
```

## 🔄 Actions Requises

### 1. Auth-Service
**À faire :**
- ✅ Garder : User, Role, Permission, AuthController, JwtFilter, JwtUtil
- ❌ Retirer : Wallet entity, WalletService, WalletController, Transaction entity
- ➕ Ajouter : FeignClient vers wallet-service pour créer wallet lors du register
- 📦 Mettre à jour : Imports vers common-library pour DTOs

**Fichiers à modifier :**
- `auth/src/main/java/com/zaphira/auth/controller/AuthController.java` - Ajouter appel FeignClient wallet-service
- `auth/src/main/java/com/zaphira/auth/service/UserService.java` - Retirer logique Wallet
- Supprimer : `auth/.../model/Wallet.java`, `auth/.../service/WalletService.java`, `auth/.../controller/WalletController.java`

### 2. Wallet-Service (NOUVEAU)
**À créer :**
- Copier `auth/.../model/Wallet.java` → `wallet-service/.../model/Wallet.java`
- Copier `auth/.../repository/WalletRepository.java` → `wallet-service/.../repository/WalletRepository.java`
- Créer `wallet-service/.../service/WalletService.java` (logique depuis auth)
- Créer `wallet-service/.../controller/WalletController.java`
- Créer `wallet-service/.../client/TransactionServiceClient.java` (FeignClient)
- Créer `wallet-service/.../WalletServiceApplication.java`

**Fichiers à créer :**
- `wallet-service/src/main/java/com/zaphira/wallet/WalletServiceApplication.java`
- `wallet-service/src/main/java/com/zaphira/wallet/model/Wallet.java`
- `wallet-service/src/main/java/com/zaphira/wallet/repository/WalletRepository.java`
- `wallet-service/src/main/java/com/zaphira/wallet/service/WalletService.java`
- `wallet-service/src/main/java/com/zaphira/wallet/controller/WalletController.java`
- `wallet-service/src/main/java/com/zaphira/wallet/client/TransactionServiceClient.java`

### 3. Transaction-Service
**À faire :**
- ✅ Garder : Toute la logique transaction existante
- ➕ Mettre à jour : Imports vers common-library
- ➕ Ajouter : FeignClient vers wallet-service (remplacer RestTemplateWalletClient)
- 📦 Utiliser : DTOs depuis common-library

**Fichiers à modifier :**
- `transaction-service/.../integration/wallet/RestTemplateWalletClient.java` → Remplacer par FeignClient
- Mettre à jour tous les imports de DTOs

### 4. Notification-Service (NOUVEAU)
**À créer :**
- `notification-service/.../NotificationServiceApplication.java`
- `notification-service/.../service/EmailService.java`
- `notification-service/.../service/SmsService.java`
- `notification-service/.../listener/TransactionEventListener.java` (Kafka/RabbitMQ)
- `notification-service/.../listener/UserEventListener.java`

### 5. API Gateway
**À créer :**
- `api-gateway/.../GatewayApplication.java`
- `api-gateway/src/main/resources/application.yml` avec routes

### 6. Service Registry
**À créer :**
- `service-registry/.../RegistryApplication.java` avec `@EnableEurekaServer`

### 7. Config Server
**À créer :**
- `config-server/.../ConfigServerApplication.java` avec `@EnableConfigServer`
- `config-server/src/main/resources/application.yml`

## 🔗 Communication Inter-Services

### Feign Clients à créer :

1. **Auth-Service → Wallet-Service**
   ```java
   @FeignClient(name = "wallet-service")
   public interface WalletServiceClient {
       @PostMapping("/api/wallets")
       WalletDTO createWallet(@RequestBody CreateWalletRequest request);
   }
   ```

2. **Wallet-Service → Transaction-Service**
   ```java
   @FeignClient(name = "transaction-service")
   public interface TransactionServiceClient {
       @PostMapping("/api/transactions")
       TransactionDTO createTransaction(@RequestBody TransactionRequest request);
   }
   ```

3. **Transaction-Service → Wallet-Service**
   ```java
   @FeignClient(name = "wallet-service")
   public interface WalletServiceClient {
       @GetMapping("/api/wallets/{walletNumber}")
       WalletDTO getWallet(@PathVariable String walletNumber);
       
       @PostMapping("/api/wallets/transfer")
       void executeTransfer(@RequestBody TransferRequest request);
   }
   ```

## 📝 Prochaines Étapes

1. ✅ Structure de base créée
2. ⏳ Déplacer Wallet depuis auth-service vers wallet-service
3. ⏳ Créer FeignClients
4. ⏳ Mettre à jour tous les imports
5. ⏳ Créer docker-compose.yml
6. ⏳ Tester compilation

## 🐳 Docker Compose

À créer dans `docker/docker-compose.yml` avec :
- PostgreSQL (shared ou par service)
- Eureka Server
- Config Server
- API Gateway
- Tous les microservices

