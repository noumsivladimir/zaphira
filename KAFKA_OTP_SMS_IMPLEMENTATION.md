# Kafka OTP SMS Implementation

## Vue d'ensemble

Cette implémentation ajoute un flux asynchrone pour l'envoi des SMS OTP via Kafka entre le **User Service** (producteur) et le **Notification Service** (consommateur/expéditeur Twilio).

## Architecture

```
┌─────────────────┐      Kafka Topic        ┌──────────────────────┐
│   User Service  │  ─────────────────────► │ Notification Service │
│                 │   otp-sms-request       │                      │
│  OtpServiceImpl │                         │ OtpSmsRequestListener│
│        │        │                         │         │            │
│        ▼        │                         │         ▼            │
│ OtpSmsPublisher │                         │    TwilioSmsService  │
│        │        │                         │         │            │
│        ▼        │                         │         ▼            │
│OtpSmsEventProd. │   otp-sms-result        │   Twilio API         │
│                 │  ◄─────────────────────  │                      │
└─────────────────┘                         └──────────────────────┘
```

## Fichiers Créés

### Common Library
- `common-library/src/main/java/com/zaphira/common/event/OtpSmsRequestEvent.java`
  - Événement Kafka pour les requêtes d'envoi OTP SMS
  - Champs: userId, phoneNumber, otpCode, purpose, expiresInMinutes, correlationId

- `common-library/src/main/java/com/zaphira/common/event/OtpSmsResultEvent.java`
  - Événement Kafka pour les résultats d'envoi
  - Champs: success, twilioMessageSid, errorMessage, sentAt

### User Service
- `user-service/src/main/java/com/zaphira/user/kafka/OtpSmsPublisher.java`
  - Interface pour publier les requêtes OTP SMS

- `user-service/src/main/java/com/zaphira/user/kafka/OtpSmsEventProducer.java`
  - Implémentation Kafka du publisher
  - Active quand `spring.kafka.enabled=true`

- `user-service/src/main/java/com/zaphira/user/kafka/NoOpOtpSmsEventProducer.java`
  - Implémentation fallback (no-op) quand Kafka est désactivé

### Notification Service
- `notification-service/src/main/java/com/zaphira/notification/listener/OtpSmsRequestListener.java`
  - Consumer Kafka qui écoute le topic `otp-sms-request`
  - Envoie les SMS via Twilio
  - Publie le résultat sur `otp-sms-result`

## Configuration

### User Service (`application.properties`)
```properties
# Kafka Configuration
spring.kafka.enabled=true
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP:192.168.100.4:9092}

# Kafka Topics
kafka.topics.otp-sms-request=otp-sms-request
kafka.topics.otp-sms-result=otp-sms-result
```

### Notification Service (`application.yml`)
```yaml
spring:
  kafka:
    enabled: true
    bootstrap-servers: ${infra.kafka.bootstrap}
    consumer:
      group-id: notification-otp-sms-group
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

kafka:
  topics:
    otp-sms-request: otp-sms-request
    otp-sms-result: otp-sms-result
```

### Twilio Configuration
```yaml
twilio:
  account-sid: "AC4ee2203ca9ee74fe8df3010675a03cd2"
  auth-token: "c94f6cac374ebe85351766659a3ced4b"
  from-number: "+17177897813"
```

## Flux de Données

### 1. Génération et Envoi OTP
```
User Request → OtpServiceImpl.generateAndSendOtp()
    │
    ├── [Si Kafka activé]
    │   └── OtpSmsPublisher.publishOtpSmsRequest()
    │       └── Publie OtpSmsRequestEvent sur 'otp-sms-request'
    │
    └── [Sinon / Fallback]
        └── NotificationServiceClient.sendOtpPayload() (Feign)
            └── Fallback: smsService.sendSms() (direct)
```

### 2. Réception et Envoi SMS
```
Kafka Topic 'otp-sms-request'
    │
    ▼
OtpSmsRequestListener.handleOtpSmsRequest()
    │
    ├── Construit le message SMS
    ├── smsService.sendSms() → TwilioSmsService
    │
    └── Publie OtpSmsResultEvent sur 'otp-sms-result'
```

## Topics Kafka

| Topic | Producteur | Consommateur | Description |
|-------|------------|--------------|-------------|
| `otp-sms-request` | User Service | Notification Service | Requêtes d'envoi OTP SMS |
| `otp-sms-result` | Notification Service | User Service (optionnel) | Résultats d'envoi pour tracking |

## Fallback Strategy

L'implémentation inclut une stratégie de fallback à 3 niveaux:

1. **Kafka** (priorité haute) - Asynchrone, scalable
2. **Feign HTTP** (fallback) - Synchrone via notification-service
3. **SMS Direct** (dernier recours) - Appel Twilio direct depuis User Service

```java
if (kafkaEnabled) {
    try {
        otpSmsPublisher.publishOtpSmsRequest(...);
    } catch (Exception ex) {
        sendOtpViaFeignOrDirectSms(phoneNumber, code, purpose);
    }
} else {
    sendOtpViaFeignOrDirectSms(phoneNumber, code, purpose);
}
```

## Messages SMS

Les messages sont personnalisés selon le type d'OTP:

| Purpose | Message |
|---------|---------|
| REGISTRATION | "Votre code d'inscription est: XXXXXX" |
| PIN_RESET | "Votre code de réinitialisation de PIN est: XXXXXX" |
| LOGIN | "Votre code de connexion est: XXXXXX" |
| TRANSACTION | "Votre code de transaction est: XXXXXX" |

## Compilation et Déploiement

### 1. Compiler common-library
```bash
cd common-library
mvn clean install
```

### 2. Compiler user-service
```bash
cd user-service
mvn clean package -DskipTests
```

### 3. Compiler notification-service
```bash
cd notification-service
mvn clean package -DskipTests
```

### 4. Vérifier Kafka
Assurez-vous que Kafka est disponible sur `192.168.100.4:9092`

### 5. Lancer les services
```bash
# User Service
java -jar user-service/target/user-service-*.jar

# Notification Service
java -jar notification-service/target/notification-service-*.jar
```

## Test

### Test d'inscription avec OTP
```powershell
$body = @{
    phoneNumber = "+33612345678"
    email = "test@example.com"
    firstName = "Test"
    lastName = "User"
    pin = "1234"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" -Method POST -Body $body -ContentType "application/json"
```

### Vérifier les logs Kafka
Dans **User Service**:
```
[OTP_SMS_PRODUCER] ✅ OTP SMS request published - correlationId: xxx
```

Dans **Notification Service**:
```
[OTP_SMS_LISTENER] 📩 Received OTP SMS request
[OTP_SMS_LISTENER] ✅ OTP SMS sent successfully
```

## Monitoring

### Métriques à surveiller
- Nombre de messages publiés sur `otp-sms-request`
- Taux de succès d'envoi SMS
- Latence Kafka → Twilio
- Erreurs Twilio

### Logs clés
```
[OTP_SMS_PRODUCER] - Production de messages Kafka
[OTP_SMS_LISTENER] - Consommation et envoi via Twilio
```

## Troubleshooting

### Le SMS n'est pas envoyé
1. Vérifier que `spring.kafka.enabled=true`
2. Vérifier la connectivité Kafka
3. Vérifier les credentials Twilio
4. Consulter les logs notification-service

### Kafka non disponible
Le système bascule automatiquement sur Feign HTTP puis SMS direct.

### Erreur Twilio
Vérifier les logs pour le message d'erreur et le code Twilio.
