# Notification Service

Le service de notification de Zaphira gère l'envoi de notifications aux utilisateurs via différents canaux : Email et SMS.

## Fonctionnalités

- **Notifications Email** : Envoi d'emails via SMTP pour les transactions, vérifications et notifications générales
- **Notifications SMS** : Envoi de SMS (intégration à implémenter avec un fournisseur)
- **Écoute d'événements Kafka** : Réaction aux événements des autres microservices

## Événements écoutés

### User Service
- `user-registered` : Notification de bienvenue et envoi du code de vérification par email lors de la création d'un compte

### Wallet Service
- `wallet-created` : Notification de création de portefeuille par email
- `wallet-balance-updated` : Notification de mise à jour du solde par email

### Transaction Service
- `transaction-created` : Notification de transaction effectuée par email
- `transaction.refunded` : Notification de remboursement par email
- `transaction.reversed` : Notification d'annulation de transaction par email
- `transaction.validation.result` : Notification de résultat de validation par email

## Configuration

### Variables d'environnement

```bash
# Email Configuration SMTP
MAIL_HOST=mail.saad-consulting.com
MAIL_PORT=587
MAIL_USERNAME=assistance@saad-consulting.com
MAIL_PASSWORD=gJ9$GvuMWg_hx*W

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```
- **Chat de test :** `7584204126`

Quand `chatId` de l'utilisateur est `null` ou non configuré, le système utilise automatiquement le chat de test.

### Création d'un Bot Telegram

1. Ouvrez Telegram et recherchez `@BotFather`
2. Envoyez `/newbot` et suivez les instructions
3. Copiez le token fourni
4. Définissez `TELEGRAM_BOT_TOKEN` avec ce token

### Configuration des utilisateurs

Les utilisateurs reçoivent automatiquement des notifications par email lors des événements importants. L'adresse email est récupérée depuis le service utilisateur.

## Architecture

```
notification-service/
├── config/
│   ├── KafkaConfig.java          # Configuration Kafka
│   └── WebClientConfig.java      # Configuration HTTP
├── controller/
│   └── NotificationController.java # Endpoints manuels pour tests
├── listener/
│   ├── UserEventListener.java    # Écoute user-registered
│   ├── WalletEventListener.java  # Écoute wallet events
│   ├── TransactionEventListener.java # Écoute transaction events
│   └── TransactionAdditionalEventsListener.java # Écoute événements supplémentaires
├── model/
│   └── VerificationToken.java    # Entité pour codes de vérification
├── repository/
│   └── VerificationTokenRepository.java # Repository JPA
└── service/
    ├── VerificationService.java  # Service de vérification
    ├── SmsService.java          # Service SMS (mock)
    ├── EmailService.java        # Service Email
    ├── UserServiceClient.java   # Client pour user-service
    └── TransactionServiceClient.java # Client pour transaction-service
```

## API Email

Le service utilise SMTP pour envoyer des emails :

- **Serveur SMTP** : mail.saad-consulting.com:587
- **Authentification** : assistance@saad-consulting.com
- **Types d'emails** :
  - Bienvenue utilisateur
  - Code de vérification (6 chiffres, valide 10 min)
  - Création portefeuille
  - Mise à jour solde
  - Notifications transaction
  - Remboursements et annulations

## Développement

### Ajouter un nouveau type de notification

1. Créer un nouvel événement dans `common-library`
2. Ajouter un listener dans `notification-service`
3. Configurer Kafka dans `KafkaConfig.java`
4. Implémenter la logique d'envoi dans le service approprié

### Tester les notifications

#### Démarrage des services

```bash
# 1. Démarrer Kafka et Zookeeper
docker-compose -f docker/docker-compose.yml up -d kafka zookeeper

# 2. Démarrer les services dans l'ordre
mvn spring-boot:run  # service-registry
mvn spring-boot:run  # config-server  
mvn spring-boot:run  # notification-service
mvn spring-boot:run  # auth-service
mvn spring-boot:run  # user-service
```

#### Test des emails

```bash
# Test direct d'envoi d'email
curl -X POST "http://localhost:8085/api/notifications/send/verification/1"

# Simulation d'inscription utilisateur
curl -X POST "http://localhost:8085/api/notifications/test/simulate-user-registered?firstName=John&lastName=Doe&phoneNumber=237123456789&email=test@example.com"
```

#### Test complet (inscription réelle)

```bash
# Inscription d'un utilisateur via user-service
curl -X POST http://localhost:8082/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "237123456789",
    "pin": "123456",
    "email": "test@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-01",
    "country": "Cameroon"
  }'
```

**Résultat attendu :** L'email est envoyé automatiquement :
1. Email de bienvenue
2. Code de vérification (6 chiffres)

à l'adresse email fournie.

#### Endpoints fonctionnels

- `POST /api/notifications/send/verification/{userId}` : Envoi manuel d'un code de vérification par email
- `POST /api/notifications/send/transaction/{transactionId}` : Envoi manuel d'une notification de transaction par email

#### Endpoints de test (développement)

- `POST /api/notifications/test/simulate-user-registered?firstName=X&lastName=Y&phoneNumber=Z&email=W` : Simulation d'événement d'inscription utilisateur

## Sécurité

- Les credentials SMTP doivent être gardés secrets
- Utiliser HTTPS pour les communications
- Valider les adresses email avant envoi
- Logs des envois sans données sensibles

## Monitoring

- Logs détaillés pour chaque envoi
- Métriques sur les taux de succès/échec
- Alertes sur les échecs répétés

## TODO

- [ ] Implémentation réelle du service SMS (Twilio/AWS SNS)
- [ ] Templates d'emails configurables
- [ ] Retry mechanism pour les envois échoués
- [ ] Rate limiting pour éviter le spam
- [ ] Tests d'intégration complets