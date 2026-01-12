# 📚 Guide d'Implémentation : Architecture Microservices Zaphira

## 🎯 Vue d'Ensemble

Ce guide couvre les principales implémentations de l'architecture Zaphira :
- Processus d'inscription avec vérification OTP
- Création automatique de wallet
- Communication synchrone et asynchrone entre services

---

## 🔐 Processus d'Inscription avec Vérification OTP

### 🎯 Vue d'Ensemble

Le processus d'inscription suit un workflow en deux étapes :
1. **Inscription** : Création du compte utilisateur (statut `PENDING_VERIFICATION`)
2. **Vérification OTP** : Validation via code OTP et activation du compte

### 📊 Flux de l'Inscription

```
POST /api/users/register
  ↓
UserRegistrationService.registerRegularUser()
  ↓
User enregistré avec AccountStatus.PENDING_VERIFICATION
  ↓
Wallet NON créé (attente vérification)
  ↓
Réponse : "User registered successfully. Please request OTP verification to complete registration."
```

### 📱 Flux de Vérification OTP

```
POST /api/users/send-otp/{userId}
  ↓
NotificationServiceClient.sendOtp(userId)
  ↓
VerificationService.generateOtpForJsonResponse()
  ↓
OTP généré et stocké en base
  ↓
Réponse JSON avec l'OTP :
{
  "status": "otp_generated",
  "otp": "123456",
  "expiresIn": "10 minutes",
  "message": "OTP généré avec succès..."
}
```

### ✅ Flux d'Activation Finale

```
POST /api/users/verify-otp
Body: {"userId": 123, "otpCode": "123456"}
  ↓
UserRegistrationService.verifyOtpAndActivateAccount()
  ↓
NotificationServiceClient.verifyOtp() → Vérification via notification-service
  ↓
AccountStatus changé à ACTIVE
  ↓
Wallet créé automatiquement
  ↓
Email de bienvenue envoyé
  ↓
Réponse avec compte activé
```

### 🔧 Endpoints Implémentés

#### 1. Inscription Utilisateur
```http
POST /api/users/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+237612345678",
  "email": "john.doe@example.com",
  "pin": "1234",
  "dateOfBirth": "1990-01-01",
  "country": "Cameroon",
  "city": "Douala",
  "region": "Littoral",
  "neighborhood": "Bonapriso"
}
```

#### 2. Demande d'OTP
```http
POST /api/users/send-otp/{userId}
```

**Réponse :**
```json
{
  "status": "success",
  "data": {
    "status": "otp_generated",
    "otp": "123456",
    "expiresIn": "10 minutes",
    "message": "OTP généré avec succès..."
  }
}
```

#### 3. Vérification OTP
```http
POST /api/users/verify-otp
Content-Type: application/json

{
  "userId": 123,
  "otpCode": "123456"
}
```

### 📁 Fichiers Implémentés

1. **UserController** : `user-service/src/main/java/com/zaphira/service_user/controller/UserController.java`
   - `registerUser()` - Inscription
   - `sendOtpForVerification()` - Demande OTP
   - `verifyOtpAndActivate()` - Vérification et activation

2. **UserRegistrationServiceImpl** : `user-service/src/main/java/com/zaphira/service_user/services/UserRegistrationServiceImpl.java`
   - `registerRegularUser()` - Inscription sans OTP automatique
   - `sendOtpForUser()` - Appel notification-service
   - `verifyOtpForUser()` - Vérification via notification-service
   - `verifyOtpAndActivateAccount()` - Activation finale

3. **NotificationServiceClient** : `user-service/src/main/java/com/zaphira/service_user/client/NotificationServiceClient.java`
   - Client Feign pour communication avec notification-service

4. **VerifyOtpRequest** : `user-service/src/main/java/com/zaphira/service_user/dto/request/VerifyOtpRequest.java`
   - DTO pour la vérification OTP

### ✅ Avantages du Processus

- **Sécurité renforcée** : Vérification obligatoire avant activation
- **Expérience utilisateur** : OTP retourné directement (pas de SMS)
- **Architecture propre** : Séparation des responsabilités
- **Session persistante** : L'utilisateur peut prendre son temps pour entrer l'OTP
- **Timeout configurable** : 3 minutes pour entrer le code (côté frontend)

### 🧪 Test Automatique

Utilisez le script `test_otp_registration.ps1` pour tester le processus complet :

```powershell
.\test_otp_registration.ps1
```

---

## 💰 Création Automatique de Wallet

---

## 🔄 Option 1 : Communication Synchrone (FeignClient) - DÉJÀ IMPLÉMENTÉE

### ✅ Fichiers Existants

1. **FeignClient** : `auth/src/main/java/com/zaphira/auth/client/WalletServiceClient.java`
2. **Service** : `auth/src/main/java/com/zaphira/auth/service/UserService.java`
3. **Wallet Service** : `wallet-service/src/main/java/com/zaphira/wallet/service/WalletService.java`

### 📝 Comment ça fonctionne

```java
// Dans UserService.createNewUserWithWallet()
User savedUser = userRepository.save(newUser);

// Appel synchrone via FeignClient
WalletDTO wallet = walletServiceClient.createWallet(savedUser.getId(), "XOF");
```

### 🔧 Configuration

- ✅ FeignClient activé dans `AuthServiceApplication` avec `@EnableFeignClients`
- ✅ Eureka configuré pour la découverte de services
- ✅ Gestion d'erreur : try-catch avec log

### 📊 Flux

```
POST /api/auth/register
  ↓
UserService.registerUser()
  ↓
UserRepository.save() → User créé
  ↓
WalletServiceClient.createWallet() → Appel REST synchrone
  ↓
WalletService.createWallet() → Wallet créé
  ↓
Réponse avec User + Wallet créés
```

### ✅ Avantages
- Transaction atomique
- Réponse immédiate
- Gestion d'erreur simple

### ❌ Inconvénients
- Couplage fort
- Si wallet-service est down, création user échoue

---

## 📨 Option 2 : Communication Asynchrone (Kafka) - NOUVELLE IMPLÉMENTATION

### 📁 Fichiers Créés

1. **Publisher** : `auth/src/main/java/com/zaphira/auth/event/UserEventPublisher.java`
2. **Listener** : `wallet-service/src/main/java/com/zaphira/wallet/listener/UserEventListener.java`
3. **Kafka Config Auth** : `auth/src/main/java/com/zaphira/auth/config/KafkaConfig.java`
4. **Kafka Config Wallet** : `wallet-service/src/main/java/com/zaphira/wallet/config/KafkaConfig.java`
5. **Service Async** : `auth/src/main/java/com/zaphira/auth/service/UserServiceAsync.java`

### 📝 Comment ça fonctionne

```java
// Dans UserServiceAsync.createNewUserWithAsyncWallet()
User savedUser = userRepository.save(newUser);

// Publication d'événement vers Kafka
UserRegisteredEvent event = UserRegisteredEvent.builder()
    .userId(savedUser.getId())
    .email(savedUser.getEmail())
    .fullName(savedUser.getFullName())
    .phoneNumber(savedUser.getPhoneNumber())
    .build();

eventPublisher.publishUserRegistered(event);
```

### 🔧 Configuration

#### 1. Dépendances Maven

**auth/pom.xml** et **wallet-service/pom.xml** :
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### 2. Configuration Kafka

**auth/src/main/resources/application.properties** :
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

**wallet-service/src/main/resources/application.yml** :
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: wallet-service-group
      auto-offset-reset: earliest
```

#### 3. Démarrer Kafka

```bash
# Avec Docker
docker run -d -p 9092:9092 apache/kafka:latest

# Ou avec Docker Compose (ajouter dans docker-compose.yml)
kafka:
  image: apache/kafka:latest
  ports:
    - "9092:9092"
```

### 📊 Flux

```
POST /api/auth/register
  ↓
UserServiceAsync.registerUser()
  ↓
UserRepository.save() → User créé
  ↓
UserEventPublisher.publishUserRegistered() → Événement publié vers Kafka
  ↓
Réponse immédiate (user créé)
  ↓
[Kafka Queue]
  ↓
UserEventListener.handleUserRegistered() → Écoute l'événement
  ↓
WalletService.createWallet() → Wallet créé de manière asynchrone
```

### ✅ Avantages
- Découplage des services
- Résilience (queue Kafka)
- Scalabilité

### ❌ Inconvénients
- Pas de garantie immédiate
- Nécessite Kafka
- Gestion d'erreur plus complexe

---

## 🔀 Comment Basculer Entre les Deux Approches

### Utiliser l'Approche Synchrone (Par Défaut)

**auth/src/main/java/com/zaphira/auth/controller/AuthController.java** :
```java
@Autowired
private UserService userService; // Utilise FeignClient
```

### Utiliser l'Approche Asynchrone

**auth/src/main/java/com/zaphira/auth/controller/AuthController.java** :
```java
@Autowired
@Qualifier("userServiceAsync")
private UserServiceAsync userServiceAsync; // Utilise Kafka
```

Ou créer un profil Spring :

**auth/src/main/resources/application-async.properties** :
```properties
# Désactiver FeignClient
spring.cloud.openfeign.enabled=false
```

**auth/src/main/resources/application.properties** :
```properties
# Activer profil async
spring.profiles.active=async
```

---

## 📋 DTOs et Événements

### UserRegisteredEvent (déjà dans common-library)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private Long userId;
    private String email;
    private String fullName;
    private String phoneNumber;
}
```

### WalletDTO (déjà dans common-library)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private Long id;
    private String walletNumber;
    private BigDecimal balance;
    private String currency;
    private Boolean active;
    private Long userId;
}
```

---

## 🧪 Tests

### Test Synchrone

```java
@SpringBootTest
class UserServiceTest {
    @MockBean
    private WalletServiceClient walletClient;
    
    @Test
    void testRegisterUser_Synchronous() {
        // Test avec FeignClient mocké
    }
}
```

### Test Asynchrone

```java
@SpringBootTest
@EmbeddedKafka
class UserServiceAsyncTest {
    @Autowired
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    
    @Test
    void testRegisterUser_Async() {
        // Test avec Kafka embedded
    }
}
```

---

## 🚀 Démarrage

### 1. Démarrer les Services

```bash
# Terminal 1: Eureka
cd service-registry && mvn spring-boot:run

# Terminal 2: Auth Service
cd auth && mvn spring-boot:run

# Terminal 3: Wallet Service
cd wallet-service && mvn spring-boot:run

# Terminal 4: Kafka (si approche async)
docker run -d -p 9092:9092 apache/kafka:latest
```

### 2. Tester l'Endpoint

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "fullName": "John Doe",
    "password": "secret",
    "phoneNumber": "+237771234567"
  }'
```

### 3. Vérifier le Wallet

```bash
# Récupérer l'userId depuis la réponse
curl http://localhost:8082/api/wallets/user/{userId}
```

---

## 📊 Comparaison

| Critère | Synchrone (Feign) | Asynchrone (Kafka) |
|---------|-------------------|-------------------|
| **Latence** | Immédiate | Quelques ms |
| **Couplage** | Fort | Faible |
| **Résilience** | Faible | Forte |
| **Complexité** | Simple | Moyenne |
| **Garantie** | Transaction atomique | Eventual consistency |
| **Infrastructure** | Eureka | Eureka + Kafka |

---

## 🎯 Recommandation

- **Production avec haute disponibilité** : Utiliser Kafka (asynchrone)
- **Développement/Test** : Utiliser FeignClient (synchrone)
- **Hybride** : Implémenter les deux et basculer via configuration

---

## 📝 Notes Importantes

1. **Idempotence** : Le listener vérifie si le wallet existe déjà avant de créer
2. **Acknowledgment** : Le listener utilise l'acknowledgment manuel pour garantir la réception
3. **Retry** : En cas d'erreur, implémenter un mécanisme de retry ou dead letter queue
4. **Monitoring** : Surveiller les lag Kafka et les erreurs de consommation

---

## 🔧 Troubleshooting

### Problème : FeignClient ne trouve pas wallet-service

**Solution** : Vérifier que Eureka est démarré et que wallet-service est enregistré.

### Problème : Kafka ne reçoit pas les événements

**Solution** : 
1. Vérifier que Kafka est démarré : `docker ps | grep kafka`
2. Vérifier les logs : `docker logs <kafka-container>`
3. Vérifier la configuration bootstrap-servers

### Problème : Wallet non créé avec approche async

**Solution** :
1. Vérifier les logs du listener
2. Vérifier que le topic `user-registered` existe
3. Vérifier les offsets Kafka

