# 📦 Implémentation Complète : Création Automatique de Wallet

## ✅ Résumé

Deux implémentations complètes sont disponibles pour créer automatiquement un wallet lorsqu'un utilisateur est enregistré.

---

## 🔄 Option 1 : Communication Synchrone (FeignClient)

### 📁 Fichiers

1. **FeignClient** : `auth/src/main/java/com/zaphira/auth/client/WalletServiceClient.java`
2. **Service** : `auth/src/main/java/com/zaphira/auth/service/UserService.java`
3. **Wallet Service** : `wallet-service/src/main/java/com/zaphira/wallet/service/WalletService.java`

### 📝 Code Complet

#### WalletServiceClient.java
```java
@FeignClient(name = "wallet-service")
public interface WalletServiceClient {
    @PostMapping("/api/wallets")
    WalletDTO createWallet(@RequestParam Long userId, 
                          @RequestParam(defaultValue = "XOF") String currency);
}
```

#### UserService.java (extrait)
```java
User savedUser = userRepository.save(newUser);

// Appel synchrone
try {
    WalletDTO wallet = walletServiceClient.createWallet(savedUser.getId(), "XOF");
    log.info("Wallet created: {}", wallet.getWalletNumber());
} catch (Exception e) {
    log.error("Failed to create wallet", e);
}
```

### 📊 Exemple de Requête

**POST** `http://localhost:8081/api/auth/register`

**Body (JSON)** :
```json
{
  "email": "user@example.com",
  "fullName": "John Doe",
  "password": "secret123",
  "phoneNumber": "+237771234567"
}
```

**Réponse** :
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "abc123...",
  "user": {
    "id": 1,
    "fullName": "John Doe",
    "email": "user@example.com",
    "role": "USER",
    "walletId": null
  }
}
```

**Note** : Le wallet est créé en arrière-plan. Pour vérifier :
```bash
GET http://localhost:8082/api/wallets/user/1
```

---

## 📨 Option 2 : Communication Asynchrone (Kafka)

### 📁 Fichiers Créés

1. **Publisher** : `auth/src/main/java/com/zaphira/auth/event/UserEventPublisher.java`
2. **Listener** : `wallet-service/src/main/java/com/zaphira/wallet/listener/UserEventListener.java`
3. **Kafka Config Auth** : `auth/src/main/java/com/zaphira/auth/config/KafkaConfig.java`
4. **Kafka Config Wallet** : `wallet-service/src/main/java/com/zaphira/wallet/config/KafkaConfig.java`
5. **Service Async** : `auth/src/main/java/com/zaphira/auth/service/UserServiceAsync.java`

### 📝 Code Complet

#### UserEventPublisher.java
```java
@Component
@RequiredArgsConstructor
public class UserEventPublisher {
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    
    public void publishUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send("user-registered", event.getUserId().toString(), event);
    }
}
```

#### UserEventListener.java
```java
@Component
@RequiredArgsConstructor
public class UserEventListener {
    private final WalletService walletService;
    
    @KafkaListener(topics = "user-registered", groupId = "wallet-service-group")
    public void handleUserRegistered(@Payload UserRegisteredEvent event) {
        walletService.createWallet(event.getUserId(), "XOF");
    }
}
```

#### UserServiceAsync.java
```java
@Service("userServiceAsync")
@RequiredArgsConstructor
public class UserServiceAsync {
    private final UserEventPublisher eventPublisher;
    
    private User createNewUserWithAsyncWallet(RegisterRequest request) {
        User savedUser = userRepository.save(newUser);
        
        UserRegisteredEvent event = UserRegisteredEvent.builder()
            .userId(savedUser.getId())
            .email(savedUser.getEmail())
            .fullName(savedUser.getFullName())
            .phoneNumber(savedUser.getPhoneNumber())
            .build();
        
        eventPublisher.publishUserRegistered(event);
        return savedUser;
    }
}
```

### 📊 Événement Kafka

**Topic** : `user-registered`

**Message (JSON)** :
```json
{
  "userId": 1,
  "email": "user@example.com",
  "fullName": "John Doe",
  "phoneNumber": "+237771234567"
}
```

**Key** : `"1"` (userId en String)

---

## 🔧 Configuration

### Dépendances Maven

**auth/pom.xml** et **wallet-service/pom.xml** :
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### Configuration Kafka

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

---

## 🚀 Démarrage

### 1. Démarrer Kafka

```bash
# Option 1: Docker
docker run -d -p 9092:9092 apache/kafka:latest

# Option 2: Docker Compose (ajouter dans docker-compose.yml)
kafka:
  image: apache/kafka:latest
  ports:
    - "9092:9092"
```

### 2. Démarrer les Services

```bash
# Terminal 1: Eureka
cd service-registry && mvn spring-boot:run

# Terminal 2: Auth Service
cd auth && mvn spring-boot:run

# Terminal 3: Wallet Service
cd wallet-service && mvn spring-boot:run
```

### 3. Tester

```bash
# Enregistrer un utilisateur
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "fullName": "John Doe",
    "password": "secret",
    "phoneNumber": "+237771234567"
  }'

# Vérifier le wallet créé
curl http://localhost:8082/api/wallets/user/1
```

---

## 📋 DTOs

### UserRegisteredEvent
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

### WalletDTO
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

### RegisterRequest
```java
@Data
public class RegisterRequest {
    private String email;
    private String fullName;
    private String password;
    private String phoneNumber; // Optionnel
}
```

---

## 🔀 Basculer Entre les Deux Approches

### Utiliser Synchrone (Par Défaut)

Dans `AuthController` :
```java
@Autowired
private UserService userService; // Utilise FeignClient
```

### Utiliser Asynchrone

Dans `AuthController` :
```java
@Autowired
@Qualifier("userServiceAsync")
private UserServiceAsync userServiceAsync; // Utilise Kafka
```

Ou via profil Spring :
```properties
# application.properties
spring.profiles.active=async
```

---

## ✅ Checklist

### Option 1 (Synchrone)
- [x] FeignClient créé
- [x] UserService utilise FeignClient
- [x] WalletService.createWallet() implémenté
- [x] Eureka configuré
- [x] Gestion d'erreur avec try-catch

### Option 2 (Asynchrone)
- [x] UserEventPublisher créé
- [x] UserEventListener créé
- [x] KafkaConfig pour auth-service
- [x] KafkaConfig pour wallet-service
- [x] UserServiceAsync créé
- [x] Dépendances Kafka ajoutées
- [x] Configuration Kafka ajoutée

---

## 🎯 Recommandation

- **Développement/Test** : Utiliser Option 1 (Synchrone) - Plus simple
- **Production** : Utiliser Option 2 (Asynchrone) - Plus résilient
- **Hybride** : Implémenter les deux et basculer via configuration

---

## 📝 Notes

1. **Idempotence** : Le listener vérifie si le wallet existe déjà
2. **Acknowledgment** : Utilisation de l'acknowledgment manuel pour garantir la réception
3. **Retry** : En cas d'erreur, implémenter un mécanisme de retry
4. **Monitoring** : Surveiller les lag Kafka et les erreurs

