# Configuration SMS pour Zaphira (Optionnel)

## 🚀 Configuration des SMS OTP avec Twilio (Optionnel pour le développement)

**Note**: Pour le développement et les tests, l'OTP est maintenant généré et retourné directement dans les réponses JSON. Twilio n'est plus requis pour les tests de base.

### Configuration de développement (Recommandé)

Pour le développement, aucune configuration Twilio n'est nécessaire. Les OTP sont générés localement et retournés dans les réponses API.

#### Test sans Twilio
```bash
# Générer un OTP
curl -X POST http://localhost:8083/api/notifications/send/otp/1

# Réponse attendue:
{
  "status": "otp_generated",
  "otp": "123456",
  "expiresIn": "10 minutes",
  "message": "OTP généré avec succès..."
}
```

### Configuration Twilio (Pour la production)

Si vous souhaitez activer l'envoi réel de SMS en production, suivez ces étapes:
Invoke-RestMethod -Uri "http://localhost:8083/api/notifications/test/send-sms?to=+237690000001&message=Test Zaphira" -Method POST
```

#### 4.2 Test complet OTP
```powershell
# Test complet de bout en bout
.\test_otp_complete.ps1
```

### 5. Dépannage

#### 5.1 SMS non reçus
- ✅ Vérifiez que les variables d'environnement sont définies
- ✅ Vérifiez le format du numéro: `+237690000001`
- ✅ Vérifiez que votre compte Twilio a des crédits
- ✅ Vérifiez que le numéro Twilio peut envoyer vers le Cameroun

#### 5.2 Erreurs courantes
- **401 Unauthorized**: Credentials Twilio incorrects
- **400 Bad Request**: Numéro de téléphone invalide
- **Insufficient Credits**: Plus de crédits Twilio

#### 5.3 Logs de débogage
Les logs détaillés sont maintenant activés. Vérifiez la console du service notification pour:
```
✅ SMS envoyé avec succès à +237690000001 - SID: SMxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 6. Endpoints disponibles

#### OTP par SMS
```powershell
# Envoi OTP (seulement si compte PENDING_VERIFICATION)
POST http://localhost:8083/api/notifications/send/otp/{userId}

# Vérification OTP
POST http://localhost:8083/api/notifications/verify/otp?userId={userId}&code={code}

# Renvoi OTP
POST http://localhost:8083/api/notifications/resend/otp/{userId}
```

#### Test direct (pour déboguer)
```powershell
# Test envoi SMS direct
POST http://localhost:8083/api/notifications/test/send-sms?to={phone}&message={message}
```

### 7. Format des numéros de téléphone

- ✅ Correct: `+237690000001`
- ❌ Incorrect: `690000001`, `00237690000001`, `237690000001`

### 8. Coûts Twilio

- **SMS sortant**: ~0.05€ par SMS
- **Test gratuit**: Crédits offerts à l'inscription
- **Pay-as-you-go**: Pas d'abonnement minimum

---

**🎯 Résumé**: Configurez vos credentials Twilio, exécutez `configure_twilio.ps1`, puis testez avec `test_otp_complete.ps1` !