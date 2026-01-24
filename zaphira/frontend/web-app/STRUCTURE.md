# 📁 Structure du projet Zaphira Wallet Suite

## Organisation des pages

Le dossier `src/pages/` est maintenant organisé par services pour une meilleure maintenabilité :

### 🔐 **auth/** - Authentification
Pages liées à l'authentification et la sécurité :
- `Login.tsx` - Connexion utilisateur
- `Register.tsx` - Inscription nouvel utilisateur
- `ForgotPin.tsx` - Récupération du code PIN
- `OTPVerification.tsx` - Vérification OTP
- `SecurityQuestionsSetup.tsx` - Configuration des questions de sécurité

### 💳 **wallet/** - Gestion du portefeuille
Pages pour gérer le wallet et les transactions financières :
- `Transfer.tsx` - Transfert d'argent
- `Withdraw.tsx` - Retrait d'argent (mobile money, banque)
- `TopUp.tsx` - Recharge du wallet
- `ReceiveMoney.tsx` - Réception d'argent (QR Code)
- `WalletDetails.tsx` - Détails du wallet (soldes, statistiques)
- `WalletManagement.tsx` - Gestion générale du wallet
- `SubWalletsManagement.tsx` - Gestion des sous-portefeuilles

### 📊 **transactions/** - Historique et planification
Pages pour consulter et gérer les transactions :
- `TransactionList.tsx` - Liste des transactions (dernières 24h)
- `TransactionDetails.tsx` - Détails d'une transaction
- `ScheduledTransactions.tsx` - Liste des transactions planifiées
- `CreateScheduledTransaction.tsx` - Créer une transaction récurrente

### 👤 **profile/** - Profil utilisateur
Pages de gestion du profil et paramètres :
- `Profile.tsx` - Page profil principal
- `EditProfile.tsx` - Modification des informations
- `Settings.tsx` - Paramètres de l'application
- `ChangePin.tsx` - Changement du code PIN

### 🆘 **support/** - Support et notifications
Pages d'aide et communication :
- `Notifications.tsx` - Centre de notifications
- `HelpSupport.tsx` - FAQ et support client

### 🏠 **Racine (pages/)** - Pages principales
Pages qui ne font pas partie d'un service spécifique :
- `Index.tsx` - Page d'accueil/landing
- `Dashboard.tsx` - Tableau de bord principal
- `NotFound.tsx` - Page 404

## 🎯 Avantages de cette structure

1. **Organisation claire** - Chaque service a son propre dossier
2. **Maintenabilité** - Facile de trouver et modifier les pages
3. **Scalabilité** - Facile d'ajouter de nouvelles fonctionnalités
4. **Imports propres** - Structure logique des imports dans App.tsx

## 📦 Imports dans App.tsx

Les imports sont organisés par catégorie :

```typescript
// Pages principales
import Index from "./pages/Index";
import Dashboard from "./pages/Dashboard";

// Pages d'authentification
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";

// Pages wallet
import Transfer from "./pages/wallet/Transfer";
import Withdraw from "./pages/wallet/Withdraw";

// etc...
```

## 🚀 Prochaines étapes

Pour ajouter une nouvelle page :
1. Identifier le service approprié
2. Créer le fichier dans le dossier correspondant
3. Ajouter l'import dans `App.tsx`
4. Configurer la route si nécessaire
