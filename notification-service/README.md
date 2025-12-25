# Notification Service

Le service de notification de Zaphira gère l'envoi de notifications aux utilisateurs via différents canaux : Telegram, SMS et Email.

## Fonctionnalités

- **Notifications Telegram** : Envoi de messages formatés via l'API Bot Telegram
- **Notifications SMS** : Envoi de SMS (intégration à implémenter avec un fournisseur)
- **Notifications Email** : Envoi d'emails via SMTP
- **Écoute d'événements Kafka** : Réaction aux événements des autres microservices

## Événements écoutés

### User Service
- `user-registered` : Notification de bienvenue lors de la création d'un compte

### Wallet Service
- `wallet-created` : Notification de création de portefeuille
- `wallet-balance-updated` : Notification de mise à jour du solde

### Transaction Service
- `transaction-created` : Notification de transaction effectuée

## Configuration

### Variables d'environnement

```bash
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=votre_token_bot_telegram
TELEGRAM_DEFAULT_CHAT_ID=chat_id_par_defaut

# Email Configuration
MAIL_USERNAME=votre_email@gmail.com
MAIL_PASSWORD=votre_mot_de_passe_app

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=192.168.0.122:9092
```

### Création d'un Bot Telegram

1. Ouvrez Telegram et recherchez `@BotFather`
2. Envoyez `/newbot` et suivez les instructions
3. Copiez le token fourni
4. Définissez `TELEGRAM_BOT_TOKEN` avec ce token

### Configuration des utilisateurs

Pour recevoir des notifications Telegram, les utilisateurs doivent lier leur compte Telegram :

- Ajouter un champ `telegramChatId` dans la table `users`
- Créer un endpoint pour lier le chat Telegram
- Stocker le `chat_id` lors de l'interaction avec le bot

## Architecture

```
notification-service/
├── config/
│   ├── KafkaConfig.java          # Configuration Kafka
│   └── WebClientConfig.java      # Configuration HTTP
├── listener/
│   ├── UserEventListener.java    # Écoute user-registered
│   ├── WalletEventListener.java  # Écoute wallet events
│   └── TransactionEventListener.java # Écoute transaction events
└── service/
    ├── TelegramService.java      # Service Telegram
    ├── SmsService.java          # Service SMS (mock)
    ├── EmailService.java        # Service Email
    └── UserServiceClient.java   # Client pour user-service
```

## API Telegram

Le service utilise l'API Bot Telegram pour envoyer des messages :

- **Endpoint** : `https://api.telegram.org/bot{TOKEN}/sendMessage`
- **Format** : HTML avec emojis
- **Messages types** :
  - Bienvenue utilisateur
  - Création portefeuille
  - Mise à jour solde
  - Notifications transaction

## Développement

### Ajouter un nouveau type de notification

1. Créer un nouvel événement dans `common-library`
2. Ajouter un listener dans `notification-service`
3. Configurer Kafka dans `KafkaConfig.java`
4. Implémenter la logique d'envoi dans le service approprié

### Tester les notifications

```bash
# Démarrer le service
mvn spring-boot:run

# Variables d'environnement pour les tests
export TELEGRAM_BOT_TOKEN=your_test_token
export TELEGRAM_DEFAULT_CHAT_ID=your_test_chat_id
```

## Sécurité

- Le token Telegram doit être gardé secret
- Utiliser HTTPS pour les communications
- Valider les chat_id avant envoi
- Logs des envois sans données sensibles

## Monitoring

- Logs détaillés pour chaque envoi
- Métriques sur les taux de succès/échec
- Alertes sur les échecs répétés

## TODO

- [ ] Implémentation réelle du service SMS (Twilio/AWS SNS)
- [ ] Endpoint pour lier/délier Telegram
- [ ] Templates de messages configurables
- [ ] Retry mechanism pour les envois échoués
- [ ] Rate limiting pour éviter le spam
- [ ] Tests d'intégration complets