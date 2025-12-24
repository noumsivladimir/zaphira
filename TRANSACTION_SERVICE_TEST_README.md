# Guide de Test du Microservice Transaction

## Vue d'ensemble
Ce script PowerShell (`test_transaction_service.ps1`) permet de tester toutes les fonctionnalités du microservice Transaction qui fonctionne sur le port 8083.

## Prérequis
1. **Microservice Transaction en cours d'exécution** sur `http://localhost:8083`
2. **Base de données PostgreSQL** accessible avec des données de test
3. **Token JWT valide** pour l'authentification (remplacer `YOUR_JWT_TOKEN_HERE`)

## Structure des Tests

### 1. Transactions (TransactionController)
- ✅ **Création de transaction** (`POST /api/transactions`)
- ✅ **Liste des transactions** (`GET /api/transactions`)
- ✅ **Détails d'une transaction** (`GET /api/transactions/{id}`)
- ✅ **Historique d'une transaction** (`GET /api/transactions/{id}/history`)
- ✅ **Informations d'autorisation** (`GET /api/transactions/{id}/authorization`)
- ✅ **Autorisation de transaction** (`POST /api/transactions/{id}/authorize`)
- ✅ **Mise à jour du statut** (`PUT /api/transactions/{id}/status`)
- ✅ **Annulation de transaction** (`PUT /api/transactions/{id}/cancel`)

### 2. Transactions Programmées (ScheduledTransactionController)
- ✅ **Création** (`POST /api/transactions/scheduled`)
- ✅ **Liste** (`GET /api/transactions/scheduled`)
- ✅ **Détails** (`GET /api/transactions/scheduled/{id}`)
- ✅ **Annulation** (`DELETE /api/transactions/scheduled/{id}`)

### 3. Taux de Change (ExchangeRateController)
- ✅ **Consultation** (`GET /api/exchange-rates`)
- ✅ **Rafraîchissement** (`POST /api/exchange-rates/refresh`)

### 4. Rapports (ReportsController)
- ✅ **Rapport quotidien** (`GET /api/reports/daily/{date}`)
- ✅ **Liste des rapports** (`GET /api/reports/daily`)
- ✅ **Dernier rapport** (`GET /api/reports/daily/latest`)
- ✅ **Analytics utilisateur** (`GET /api/reports/user/{userId}`)
- ✅ **Historique utilisateur** (`GET /api/reports/user/{userId}/history`)

### 5. Litiges (DisputeController)
- ✅ **Création de litige** (`POST /api/disputes`)
- ✅ **Soumission de preuve** (`POST /api/disputes/{id}/evidence`)
- ✅ **Réponse au litige** (`POST /api/disputes/{id}/respond`)
- ✅ **Détails du litige** (`GET /api/disputes/{id}`)
- ✅ **Résolution du litige** (`PUT /api/disputes/{id}/resolve`)

## Utilisation

### 1. Préparation
```powershell
# Ouvrir PowerShell et naviguer vers le répertoire
cd "c:\Users\HP\Downloads\zaphira-24-12-2025\services-tree"

# Modifier le token JWT dans le script
# Remplacer YOUR_JWT_TOKEN_HERE par un token valide
```

### 2. Exécution
```powershell
# Exécuter le script complet
.\test_transaction_service.ps1

# Ou exécuter seulement une section spécifique
# (Modifier le script pour commenter/décommenter les sections)
```

### 3. Obtenir un Token JWT
Pour obtenir un token JWT valide, vous pouvez :
1. Utiliser l'endpoint de login du user-service
2. Utiliser un token de test fourni par l'équipe de développement
3. Générer un token via les outils d'authentification

## Données de Test Utilisées

### Wallets de Test
- `WALLET001` - Wallet expéditeur
- `WALLET002` - Wallet destinataire

### Utilisateurs de Test
- `user123` - Utilisateur standard
- `admin` - Administrateur
- `merchant456` - Commerçant

### Transactions de Test
- **Montant**: 100.00 XAF (transfert), 50.00 XAF (programmé)
- **Type**: TRANSFER
- **Canal**: MOBILE_APP, WEB_APP

## Gestion des Erreurs

Le script gère automatiquement les erreurs HTTP et affiche des messages informatifs :
- 🟢 **Succès**: Messages en vert
- 🟡 **Informations**: Messages en jaune
- 🔴 **Erreurs**: Messages en rouge avec détails

## Personnalisation

### Modifier les Données de Test
```powershell
# Dans le script, modifier ces variables selon vos besoins :
$transactionRequest = @{
    senderWalletNumber = "VOTRE_WALLET"
    receiverWalletNumber = "WALLET_DEST"
    amount = 500.00  # Modifier le montant
    currency = "EUR"  # Changer la devise
    # ... autres propriétés
} | ConvertTo-Json
```

### Tester des Endpoints Spécifiques
Commentez/décommentez les sections du script pour tester seulement certains endpoints :
```powershell
# Commentez cette ligne pour désactiver les tests de transactions
# Write-Host "=== 1. TESTS DES TRANSACTIONS ===" -ForegroundColor Green
```

## Résolution des Problèmes

### Erreur 401 Unauthorized
- Vérifiez que le token JWT est valide et non expiré
- Assurez-vous que le token a les bonnes permissions

### Erreur 404 Not Found
- Vérifiez que le microservice transaction est démarré
- Vérifiez l'URL de base (`http://localhost:8083`)

### Erreur 500 Internal Server Error
- Vérifiez les logs du microservice transaction
- Assurez-vous que la base de données est accessible
- Vérifiez la configuration des services dépendants (wallet-service, user-service)

### Erreur de Validation
- Vérifiez que les données de test correspondent aux contraintes de validation
- Assurez-vous que les wallets existent dans la base de données

## Logs et Monitoring

Le script affiche des informations détaillées sur :
- Le nombre de ressources récupérées
- Les IDs des ressources créées
- Les statuts des opérations
- Les erreurs rencontrées

## Extensions Possibles

### Tests de Performance
```powershell
# Ajouter des boucles pour tester la charge
for ($i = 1; $i -le 100; $i++) {
    # Code de test répété
}
```

### Tests d'Intégration
- Tester les interactions entre services
- Valider les workflows complets
- Tester les compensations en cas d'échec

### Tests de Sécurité
- Tester les contrôles d'accès
- Valider les autorisations
- Tester les limites de débit

---

**Note**: Ce script est conçu pour un environnement de développement/test. Ne l'utilisez pas en production sans adaptations appropriées.