# Zaphira - Lots fonctionnels et workflows

Ce document décrit, par lot priorisé, les workflows principaux, les services impliqués, les endpoints/API attendus, les événements inter-services et les données clés. Il sert de guide d'intégration pour aligner l'implémentation avec la liste de fonctionnalités cible.

## Transaction Service

### Lot 1 – Coeur transactionnel indispensable
- Objectif: permettre créer, traiter, annuler, rembourser, inverser et consulter les transactions avec un cycle de vie complet.
- Services impliqués: transaction-service (core), wallet-service (validation solde/limites), notification-service (OTP/alerts), user-service (données utilisateur), auth-service (JWT), api-gateway.
- Endpoints cibles (transaction-service):
  - POST /api/transactions
  - GET /api/transactions/{reference}
  - GET /api/transactions/user/{userId}
  - GET /api/transactions/wallet/{walletNumber}
  - POST /api/transactions/{reference}/process
  - POST /api/transactions/{reference}/cancel
  - POST /api/transactions/{reference}/reverse
  - POST /api/transactions/{reference}/refund
  - POST /api/transactions/{reference}/retry
- Etat et journalisation:
  - Etats: INITIATED, PENDING, AUTHORIZED, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED, REFUNDED.
  - Historique: horodatage par état, acteur (userId/service), raison.
- Workflow principal (création → completion):
  1) API-Gateway reçoit la requête → route vers transaction-service.
  2) transaction-service valide input, appelle wallet-service pour validation (statut wallet, solde, limites de base).
  3) transaction-service crée la transaction (état INITIATED → PENDING/AUTHORIZED selon règles) et publie TransactionInitiatedEvent.
  4) Si autorisation requise (OTP/PIN), notification-service envoie OTP, transaction-service passe à PENDING.
  5) Autorisation reçue → état AUTHORIZED, lancement process → PROCESSING.
  6) Débit/crédit via wallet-service (bloquer puis appliquer). Si succès → COMPLETED, sinon FAILED et rollback blocage.
  7) notification-service envoie succès/échec; events: Completed/Failed.
- Workflows secondaires:
  - Cancel: PENDING/AUTHORIZED → CANCELLED (libération fonds si bloqués).
  - Reverse: COMPLETED → REVERSED (écriture inverse, adjust balances via wallet-service).
  - Refund: COMPLETED → REFUNDED (partiel/total, logique proche reverse).
  - Retry: FAILED → PROCESSING (avec compteur tentatives, backoff simple).

### Lot 2 – Paiements et transferts étendus
- Objectif: couvrir dépôts, retraits, paiements marchands, P2P/own-wallet, split/bulk, fees simples.
- Services impliqués: transaction-service, wallet-service, notification-service.
- Endpoints additionnels:
  - POST /api/transactions/deposit
  - POST /api/transactions/withdrawal
  - POST /api/transactions/merchant-payment
  - POST /api/transactions/transfer (P2P/own-wallet)
  - POST /api/transactions/bulk-transfer
- Workflow (ex: transfer):
  1) Validation wallets/solde/limites via wallet-service.
  2) Calcul frais (fixe + pourcentage) avant réservation.
  3) Blocage fonds côté wallet-service, création transaction PENDING/AUTHORIZED.
  4) Traitement → débit sender, crédit receiver, état COMPLETED, events transfer completed.
- Split/bulk: itère sur destinataires, gestion partielle/rollback si échec (all-or-nothing pour la version 1).

### Lot 3 – Scheduling & batch
- Endpoints: CRUD /api/transactions/scheduled, /api/transactions/batch.
- Workflow (scheduled):
  1) Création d’un job planifié stocké en DB (scheduled_transactions).
  2) Scheduler (Spring @Scheduled) déclenche à l’heure → crée transaction réelle Lot1 et suit le workflow standard.
  3) Cancel/Update modifie l’entrée planifiée; si annulé avant exécution, pas de transaction créée.
- Workflow (batch):
  1) Réception d’une liste d’ordres; validation globale (limites) puis par item.
  2) Exécution séquentielle ou en pool; si mode all-or-nothing, rollback blocages en cas d’échec.

### Lot 4 – Recherche & reporting léger
- Endpoints: /api/transactions/search (critères ref/status/date/amount/wallet/user), /api/transactions/summary (succès/échecs/volume).
- Workflow: requêtes paginées sur la table transactions + index; agrégations simples en SQL.

### Lot 5 – Disputes & refunds avancés
- Endpoints: /api/transactions/{ref}/dispute (POST/GET/resolve), /api/transactions/{ref}/refund (partiel), /api/transactions/{ref}/reverse.
- Workflow: crée une entrée dispute, change état en UNDER_REVIEW/ON_HOLD, décide résolution → refund/maintien.

### Lot 6 – FX & multi-devise
- Endpoints: déjà /api/exchange-rates; étendre pour appliquer conversion sur transaction (stockage devise source/target, taux appliqué, fees FX).
- Workflow: lookup FX, calcul montant cible + marge, persister taux utilisé, exécution balances dans devise cible.

## Wallet Service

### Lot 1 – Gestion wallet et solde de base
- Endpoints: POST /api/wallets, GET /api/wallets/{number}, GET /api/wallets/id/{id}, GET /api/wallets/{number}/summary, freeze/unfreeze/suspend/activate/close.
- Workflow création: API-Gateway → wallet-service → crée wallet, génère numéro, initialise limites et état ACTIVE, renvoie summary.
- Workflow statut: change state, journalise raison/acteur, empêche opérations selon état (ex: frozen bloque débits).

### Lot 2 – Transfert & validation
- Endpoints: POST /api/wallets/transfer (à ajouter), POST /api/wallets/validate-transaction (déjà), /has-balance, recalcul balance.
- Workflow transfert (côté wallet):
  1) Valider statut/solde/limites.
  2) Bloquer montant sur wallet source, préparer crédit cible.
  3) Appliquer débit/crédit atomique; en cas d’échec, rollback blocage.
- Validation transaction: utilisé par transaction-service avant création ou traitement.

### Lot 3 – Limites & contrôle
- Endpoints: PUT /api/wallets/{walletNumber}/limits, GET limites (à ajouter), has-balance.
- Workflow: mettre à jour limites (daily/monthly), stocker utilisation courante, vérifier avant opérations.

### Lot 4 – Sous-wallet & permissions
- Endpoints: POST /api/wallet/subWallet/create, POST /api/wallet/permission/id/{id}.
- Workflow: crée sous-wallet rattaché, initialise permissions par défaut.

### Lot 5 – Historique & statements léger
- Endpoints à ajouter: GET /api/wallets/{walletNumber}/transactions, /statement (export CSV/JSON simple).
- Workflow: lecture transactions par wallet depuis transaction-service ou vue matérialisée; export avec filtres date/type/status.

## User Service

### Lot 1 – Onboarding & vérification
- Endpoints: /api/users/register, /verify-email, /verify-otp, /send-otp/{userId}, /generate-email-otp/{userId}, /verify-email-link, /profile (GET/PUT), /{userIdOrWalletId}, /verification-status, security questions setup/status, PIN reset (PinResetController).
- Workflow inscription: register → envoie OTP/email (notification-service) → verify → activation compte → création wallet via wallet-service (si requis) → retour user info.
- PIN reset workflow: initiate (OTP) → verify OTP → verify security questions → reset PIN.

### Lot 2 – Lookup & notification info
- Endpoints: /api/users/{userId}/notification-info, /{userIdOrWalletId}.
- Workflow: fournir email/phone/status pour notifications ou autres services.

## Auth Service

### Lot 1 – Login JWT
- Endpoint: POST /api/auth/login (phone + PIN) → JWT.
- Workflow: authenticate user (user-service), générer JWT (auth-service), retourner user info minimale.
- Extension future: refresh/revoke tokens, audit log, MFA (placeholder).

## Notification Service

### Lot 1 – OTP & vérification
- Endpoints: /api/notifications/send/verification/{userId}, /resend/verification/{userId}, /send/otp/{userId}, /resend/otp/{userId}, /send/otp (payload), /verify/otp, /admin/token-stats.
- Workflow: génération code via verification-service, stockage token, envoi email/SMS (ou renvoi OTP JSON), vérification code, stats tokens.

### Lot 2 – Notifications transaction
- Endpoints: /send/transaction/{transactionId}.
- Workflow: récup transaction via transaction-service, récupérer emails via user-service, envoyer email expéditeur/destinataire; étendre pour statuts FAILED/CANCELLED/REVERSED/REFUNDED.

## Intégration et cohérence inter-services
- Routage: api-gateway expose les endpoints ci-dessus; eureka/registry pour découverte.
- Sécurité: auth-service fournit JWT; controllers protégés via filter/annotations (à aligner).
- Orchestration transaction ↔ wallet: toujours passer par wallet-service pour blocage/débit/crédit; transaction-service tient le registre métier et états.
- Notifications: transaction-service publie événements; notification-service écoute ou est appelé pour OTP/alerts.
- Données: scripts DB (database-migration) doivent aligner tables pour états, historiques, scheduled, disputes, fees, FX, etc.

## Prochaines étapes (implémentation incrémentale)
1) Activer Lot Transaction 1 + Wallet 1/2: endpoints commentés à décommenter/compléter, services/DTOs à finaliser, états et audit.
2) Ajouter endpoints manquants pour transfers, cancel/reverse/refund/retry, search minimal.
3) Connecter notification-service pour OTP et alerts de base.
4) Documenter dans ce dossier chaque lot une fois implémenté (checklist + endpoints actifs).

