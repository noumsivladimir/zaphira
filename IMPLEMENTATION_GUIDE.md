# 📚 Guide d'Implémentation : Création Automatique de Wallet

## 🎯 Vue d'Ensemble

Deux approches complètes sont disponibles pour créer automatiquement un wallet lorsqu'un utilisateur est enregistré.

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

