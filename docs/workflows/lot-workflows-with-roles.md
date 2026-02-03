# Zaphira - Lots fonctionnels avec rôles et permissions

Ce document aligne chaque lot fonctionnel avec les rôles métiers (REGULAR, MERCHANT, ADMIN/OPS) et précise les permissions attendues par endpoint/processus. Il complète le document `lot-workflows.md` en ajoutant la dimension d’autorisation.

## Rôles de référence
- REGULAR : utilisateur final (payer, transférer, consulter ses données).
- MERCHANT : encaisse des paiements, peut rembourser ses ventes, accès à ses wallets marchands/rapports.
- ADMIN/OPS : opérations internes, support, forçage d’états, réglages limites, audit.

## Transaction Service

### Lot 1 – Cœur transactionnel indispensable
Fonctionnalités : create/process/cancel/retry/get/ref, cycle de vie complet (INITIATED…REFUNDED), audit états.

- REGULAR :
  - POST /api/transactions (create) — autorisé si owner de l’émetteur ou si auto-paiement.
  - POST /api/transactions/{ref}/process — autorisé si owner.
  - POST /api/transactions/{ref}/cancel — autorisé si owner et statut PENDING/PROCESSING.
  - GET /api/transactions/{ref}, GET /api/transactions/id/{id} — autorisé si owner (sender/receiver).
- MERCHANT :
  - Idem REGULAR sur ses transactions; préparé pour refund/reverse marchand (Lot 5) sur ses ventes.
- ADMIN/OPS :
  - Peut forcer cancel/retry, consulter toute transaction.

### Lot 2 – Paiements & transferts étendus
Fonctionnalités : deposit, withdrawal, merchant-payment, transfer P2P/own-wallet, bulk/split, fees simples.

- REGULAR :
  - P2P/own-wallet transfer, deposit, withdrawal si limites OK.
- MERCHANT :
  - merchant-payment (encaisse), bulk payouts vers sous-comptes/agents, refunds de ses ventes (Lot 5).
- ADMIN/OPS :
  - Peut déclencher/monitor bulk, ajuster limites.

### Lot 3 – Scheduling & batch
- REGULAR : scheduled/recurring perso, annulation/modif de ses jobs.
- MERCHANT : batch payouts/recurring liés à ses comptes marchands.
- ADMIN/OPS : gérer/annuler jobs pour support.

### Lot 4 – Recherche & reporting léger
- REGULAR : recherche paginée filtrée sur ses transactions seulement.
- MERCHANT : reports sur ses ventes, agrégats par statut/volume.
- ADMIN/OPS : vue globale.

### Lot 5 – Disputes & refunds avancés
- REGULAR : initier dispute sur ses transactions reçues/payées; voir statut.
- MERCHANT : traiter/refuser/accepter dispute sur ses ventes; initier refund partiel/total.
- ADMIN/OPS : arbitrage, forcer résolution, chargeback.

### Lot 6 – FX & multi-devise
- REGULAR/MERCHANT : conversion appliquée à leurs transactions; lecture des taux.
- ADMIN/OPS : gérer providers/refresh.

## Wallet Service

### Lot 1 – Gestion wallet & solde de base
- REGULAR : créer wallet perso, consulter solde/summary, freeze/unfreeze/close son wallet (si solde 0), credit/debit/block/unblock sur ses wallets via processus autorisés.
- MERCHANT : créer wallet marchand, solde/summary, opérations sur ses wallets marchands, limites plus élevées.
- ADMIN/OPS : ajustements exceptionnels, close forcé.

### Lot 2 – Transfert & validation
- REGULAR : transfert entre ses wallets / vers un autre wallet, validation solde/limites.
- MERCHANT : transferts entre wallets marchands/sous-comptes, payouts.
- ADMIN/OPS : override en support.

### Lot 3 – Limites & contrôle
- REGULAR : lire ses limites; demander mise à jour (si exposé), check balance/has-balance.
- MERCHANT : limites spécifiques marchands; bulk ops.
- ADMIN/OPS : set/update limites.

### Lot 4 – Sous-wallet & permissions
- MERCHANT : créer sous-wallets, initialiser permissions.
- REGULAR : (optionnel) sous-wallet perso si activé; sinon restreint.
- ADMIN/OPS : gérer hiérarchie/permissions.

### Lot 5 – Historique & statements
- REGULAR : historique/statement de ses wallets uniquement.
- MERCHANT : exports/rapports sur ses wallets marchands.
- ADMIN/OPS : vue globale.

## User Service

### Lot 1 – Onboarding & vérification
- Ouvert: register/verify email/OTP, PIN reset, security questions (self).
- Profile/PIN: uniquement propriétaire (REGULAR ou MERCHANT).
- ADMIN/OPS : consultation support (lecture), pas de modification sensible sans process dédié.

### Lot 2 – Lookup & notification info
- REGULAR/MERCHANT : accès à leurs infos; notification-info fourni pour leurs propres IDs.
- ADMIN/OPS : lookup support (lecture).

## Auth Service
- Login JWT pour tous; les rôles (REGULAR, MERCHANT, ADMIN) doivent être dans les claims.
- ADMIN/OPS : endpoints futurs refresh/revoke.

## Notification Service

### Lot 1 – OTP & vérification
- Ouvert pour l’utilisateur concerné (self) ; ADM/OPS peut déclencher renvoi OTP support.

### Lot 2 – Notifications transaction
- REGULAR/MERCHANT : notifications seulement si partie prenante (sender/receiver/merchant).
- ADMIN/OPS : pas de notification personnelle, mais peut déclencher/rejouer (support).

## Implémentation des permissions (cible)
- Spring Security + Method Security (`@PreAuthorize`).
- Claims JWT : `sub` (userId), `role` ∈ {REGULAR, MERCHANT, ADMIN}, éventuellement `merchantId`.
- Guards custom :
  - `@txSecurity.isOwner(ref)` ou `isParticipant(ref)` pour transactions.
  - `@txSecurity.isMerchantOwner(ref)` pour refunds/reverses marchands.
  - `@walletSecurity.isOwner(walletNumber|id)`.
- API Gateway : filtra ge global via JWT + propagation du rôle; services appliquent les contrôles fins.
