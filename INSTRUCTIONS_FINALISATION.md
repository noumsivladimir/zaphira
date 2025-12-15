# 🎯 Instructions de Finalisation - Architecture Microservices

## ✅ État Actuel

La structure de base de l'architecture microservices a été créée avec succès. Voici ce qui reste à finaliser.

## 🔧 Actions Immédiates Requises

### 1. Nettoyer Auth-Service

**Supprimer ces fichiers :**
```bash
auth/src/main/java/com/zaphira/auth/model/Wallet.java
auth/src/main/java/com/zaphira/auth/repository/WalletRepository.java
auth/src/main/java/com/zaphira/auth/service/WalletService.java
auth/src/main/java/com/zaphira/auth/controller/WalletController.java
auth/src/main/java/com/zaphira/auth/model/Transaction.java
auth/src/main/java/com/zaphira/auth/repository/TransactionRepository.java
auth/src/main/java/com/zaphira/auth/controller/TransactionController.java
```

**Vérifier :**
- ✅ `UserService` n'utilise plus `WalletRepository` (déjà fait)
- ✅ `User` n'a plus de relation JPA avec `Wallet` (déjà fait)
- ✅ `AuthController` n'utilise plus `user.getWallet()` (déjà fait)

### 2. Transaction-Service - Utiliser FeignClient

**Option A : Supprimer RestTemplate (Recommandé)**
```bash
# Supprimer
transaction-service/.../integration/wallet/RestTemplateWalletClient.java
transaction-service/.../config/RestTemplateConfig.java
transaction-service/.../config/WalletServiceProperties.java
```

**Option B : Désactiver RestTemplate**
- Annoter `RestTemplateWalletClient` avec `@Component` → `@Component("restTemplateWalletClient")`
- Annoter `FeignWalletClientAdapter` avec `@Primary` pour qu'il soit injecté par défaut

**Recommandation :** Option A (supprimer RestTemplate)

### 3. Wallet-Service - Compléter les Endpoints

**Ajouter dans `WalletController.java` :**
```java
@PostMapping("/transfer")
public ResponseEntity<Void> transfer(@RequestBody TransferRequest request) {
    walletService.debit(request.getSenderWalletNumber(), request.getAmount());
    walletService.credit(request.getReceiverWalletNumber(), request.getAmount());
    return ResponseEntity.ok().build();
}
```

**Créer `TransferRequest.java` :**
```java
package com.zaphira.wallet.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    private String senderWalletNumber;
    private String receiverWalletNumber;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private String description;
}
```

### 4. Notification-Service - Implémentation de Base

**Créer `EmailService.java` :**
```java
package com.zaphira.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
```

**Créer `TransactionEventListener.java` :**
```java
package com.zaphira.notification.listener;

import com.zaphira.common.event.TransactionCreatedEvent;
import com.zaphira.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {
    private final EmailService emailService;

    @KafkaListener(topics = "transaction-created", groupId = "notification-service")
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Received transaction event: {}", event.getReference());
        // emailService.sendEmail(...);
    }
}
```

**Ajouter dans `application.yml` :**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

### 5. Créer Dockerfiles

**Pour chaque service, créer `Dockerfile` :**
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Placer dans :**
- `auth-service/Dockerfile`
- `wallet-service/Dockerfile`
- `transaction-service/Dockerfile`
- `notification-service/Dockerfile`
- `api-gateway/Dockerfile`
- `service-registry/Dockerfile`
- `config-server/Dockerfile`

### 6. Vérifier la Compilation

```bash
# Depuis la racine zaphira-platform
mvn clean install -DskipTests

# Si erreurs, corriger :
# - Imports manquants
# - Dépendances manquantes dans POMs
# - Classes non trouvées
```

### 7. Tester le Démarrage

**Ordre de démarrage :**
1. PostgreSQL (si pas déjà démarré)
2. Eureka Server (port 8761)
3. Config Server (port 8888)
4. API Gateway (port 8080)
5. Auth Service (port 8081)
6. Wallet Service (port 8082)
7. Transaction Service (port 8083)
8. Notification Service (port 8084)

**Vérifier :**
- Eureka Dashboard : http://localhost:8761
- Tous les services apparaissent dans Eureka
- API Gateway route correctement

## 📋 Checklist de Finalisation

### Auth-Service
- [ ] Fichiers Wallet/Transaction supprimés
- [ ] Compilation sans erreur
- [ ] Register crée wallet via FeignClient
- [ ] Tests mis à jour

### Transaction-Service
- [ ] RestTemplate supprimé ou désactivé
- [ ] FeignWalletClientAdapter injecté
- [ ] Compilation sans erreur
- [ ] Tests mis à jour

### Wallet-Service
- [ ] Endpoint transfer créé
- [ ] TransferRequest DTO créé
- [ ] Compilation sans erreur
- [ ] Tests créés

### Notification-Service
- [ ] EmailService implémenté
- [ ] TransactionEventListener créé
- [ ] Configuration Kafka/Mail
- [ ] Compilation sans erreur

### Infrastructure
- [ ] Dockerfiles créés pour tous les services
- [ ] docker-compose.yml testé
- [ ] Eureka fonctionne
- [ ] Config Server fonctionne
- [ ] API Gateway route correctement

## 🚀 Commandes Utiles

```bash
# Build tous les modules
mvn clean install

# Build un module spécifique
cd auth-service && mvn clean install

# Démarrer avec Docker
cd docker && docker-compose up

# Vérifier les services dans Eureka
curl http://localhost:8761/eureka/apps

# Tester API Gateway
curl http://localhost:8080/api/auth/register
```

## 📝 Notes Finales

- **Base de données** : Tous les services partagent actuellement `wallet_db`. Pour production, séparer.
- **Ports** : Vérifier qu'aucun conflit de port
- **Logs** : Vérifier les logs de chaque service au démarrage
- **Sécurité** : JWT validation à ajouter au niveau Gateway

---

**Prochaine étape :** Exécuter les actions ci-dessus et tester la compilation complète.

