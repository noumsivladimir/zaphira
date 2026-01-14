# Implémentation Twilio SMS pour Zaphira

## Vue d'ensemble

Le service notification de Zaphira utilise Twilio pour l'envoi d'OTP par SMS. Cette implémentation permet d'envoyer des codes de vérification aux utilisateurs lors de l'inscription et d'autres opérations sensibles.

## Configuration

### Variables d'environnement

Assurez-vous que les variables suivantes sont configurées dans `application.yml` :

```yaml
twilio:
  account-sid: "AC4ee2203ca9ee74fe8df3010675a03cd2"
  auth-token: "votre_auth_token_twilio"
  from-number: "+17177897813"
```

### Obtention des credentials Twilio

1. Créez un compte sur [Twilio Console](https://console.twilio.com/)
2. Obtenez votre `Account SID` et `Auth Token` depuis le dashboard
3. Achetez ou utilisez un numéro de téléphone Twilio pour l'envoi de SMS

## API Endpoints

### Envoi d'OTP par SMS

**Endpoint:** `POST /api/notifications/send/otp`

**Payload:**
```json
{
  "phoneNumber": "+237656958696",
  "otp": "123456",
  "purpose": "REGISTRATION",
  "expiresInMinutes": 5
}
```

**Réponse de succès:**
```json
{
  "status": "otp_sent",
  "message": "OTP envoyé par SMS avec succès"
}
```

**Validation:**
- `phoneNumber` doit être au format international (commencer par +)
- `otp` est obligatoire
- `purpose` et `expiresInMinutes` sont optionnels

## Format des messages

Les messages SMS suivent ce format :
```
Zaphira - Votre code de vérification est: 123456. Ce code expire dans 5 minutes.
```

## Gestion des erreurs

### Erreurs de configuration
Si les credentials Twilio ne sont pas configurés, le service loguera :
```
❌ CONFIGURATION TWILIO MANQUANTE:
  - AccountSid: MANQUANT
  - AuthToken: MANQUANT
  - FromNumber: MANQUANT
```

### Erreurs d'API Twilio
Les erreurs de l'API Twilio sont loguées avec le code d'erreur et le message.

## Test

Utilisez le script `test_sms_otp.ps1` pour tester l'envoi d'OTP :

```powershell
.\test_sms_otp.ps1
```

## Sécurité

- Les numéros de téléphone sont masqués dans les logs
- Les credentials Twilio sont stockés de manière sécurisée
- Validation stricte du format des numéros de téléphone

## Dépendances

- `twilio-java-sdk` pour l'intégration Twilio
- Spring Boot pour la configuration et l'injection de dépendances