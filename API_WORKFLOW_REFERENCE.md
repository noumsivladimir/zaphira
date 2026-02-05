# 📚 ZAPHIRA API - Documentation Complète des Workflows

> **Version**: 2.0.0  
> **Date**: Février 2026  
> **Status**: Production Ready (100% implémenté)  
> **Total Endpoints**: 130+

---

## 📋 Table des Matières

1. [Vue d'ensemble Architecture](#1-vue-densemble-architecture)
2. [Bases de données PostgreSQL](#2-bases-de-données-postgresql)
3. [Événements Kafka](#3-événements-kafka)
4. [Auth Service (5 endpoints)](#4-auth-service)
5. [User Service (27 endpoints)](#5-user-service)
6. [Wallet Service (30 endpoints)](#6-wallet-service)
7. [Transaction Service (58 endpoints)](#7-transaction-service)
8. [Notification Service (10 endpoints)](#8-notification-service)
9. [Workflows Métier Complets](#9-workflows-métier-complets)
10. [Matrice des Tests](#10-matrice-des-tests)

---

## 1. Vue d'ensemble Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            API GATEWAY (Port 8080)                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│  AUTH SERVICE │           │ USER SERVICE  │           │WALLET SERVICE │
│   Port 8081   │           │   Port 8082   │           │   Port 8083   │
│   5 endpoints │           │  27 endpoints │           │  30 endpoints │
└───────────────┘           └───────────────┘           └───────────────┘
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│zaphira_auth_db│           │zaphira_users_db│          │zaphira_wallets│
└───────────────┘           └───────────────┘           └───────────────┘
        
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│  TRANSACTION  │           │ NOTIFICATION  │           │CONFIG SERVER  │
│    SERVICE    │           │   SERVICE     │           │   Port 8888   │
│   Port 8084   │           │   Port 8085   │           │               │
│  58 endpoints │           │  10 endpoints │           │               │
└───────────────┘           └───────────────┘           └───────────────┘
        │                             │                     
        ▼                             ▼                     
┌───────────────┐           ┌───────────────┐           
│zaphira_trans_ │           │zaphira_notif_ │           
│   actions_db  │           │   cations_db  │           
│zaphira_disputes│          └───────────────┘           
│zaphira_reports │                                      
└───────────────┘                                       

                    ┌─────────────────────────┐
                    │     APACHE KAFKA        │
                    │   Message Broker        │
                    │   14 Topics Actifs      │
                    └─────────────────────────┘
```

---

## 2. Bases de données PostgreSQL

### 2.1 Vue d'ensemble des 8 Bases de Données

| Base de données | Tables | Description |
|-----------------|--------|-------------|
| `zaphira_users_db` | 6 | Utilisateurs, KYC, sessions, questions sécurité |
| `zaphira_auth_db` | 3 | JWT tokens, refresh tokens, activity logs |
| `zaphira_wallets_db` | 5 | Portefeuilles, sous-portefeuilles, permissions |
| `zaphira_transactions_db` | 9 | Transactions, settlements, refunds, audit |
| `zaphira_disputes_db` | 4 | Litiges, preuves, timeline, résolutions |
| `zaphira_reports_db` | 4 | Rapports journaliers, analytics marchands/users |
| `zaphira_exchange_db` | 2 | Taux de change, historique FX |
| `zaphira_notifications_db` | 3 | Notifications, tokens vérification, OTP |

### 2.2 Détail des Tables par Service

#### 📦 zaphira_users_db (User Service)

| Table | Colonnes Clés | Description |
|-------|---------------|-------------|
| `users` | user_id, email, phone_number, first_name, last_name, user_type, role_type, account_status, wallet_number | Utilisateurs (STI pattern: CUSTOMER, MERCHANT, ADMIN) |
| `kyc` | id, user_id, kyc_status, document_type, document_number, verified_at | Documents KYC |
| `user_sessions` | session_id, user_id, token, refresh_token, is_active, expiry_time | Sessions actives |
| `security_questions` | id, question_text, is_active, display_order | Questions de sécurité prédéfinies |
| `user_security_answers` | id, user_id, question_id, answer_hash | Réponses aux questions sécurité |
| `otp_tokens` | id, user_id, code, purpose, expires_at, verified | Codes OTP de vérification |

**Enums:**
- `user_type_enum`: CUSTOMER, MERCHANT, ADMIN
- `account_status_enum`: PENDING_VERIFICATION, ACTIVE, SUSPENDED, DEACTIVATED
- `role_type_enum`: CUSTOMER, MERCHANT, MERCHANT_ADMIN, PLATFORM_ADMIN, SUPER_ADMIN
- `kyc_status_enum`: NOT_SUBMITTED, PENDING, UNDER_REVIEW, APPROVED, REJECTED, EXPIRED
- `otp_purpose_enum`: EMAIL_VERIFICATION, PHONE_VERIFICATION, PASSWORD_RESET, TWO_FACTOR

---

#### 🔐 zaphira_auth_db (Auth Service)

| Table | Colonnes Clés | Description |
|-------|---------------|-------------|
| `tokens` | id, user_id, token_hash, token_type, is_revoked, expires_at | JWT tokens stockés |
| `refresh_token` | id, token, user_id, revoked, expires_at | Refresh tokens pour renouvellement |
| `activity_log` | id, user_id, activity_type, ip_address, user_agent, timestamp | Journal d'activité utilisateur |

---

#### 💰 zaphira_wallets_db (Wallet Service)

| Table | Colonnes Clés | Description |
|-------|---------------|-------------|
| `wallets` | id, user_id, wallet_number, wallet_type, currency, available_balance, blocked_balance, status | Portefeuilles principaux |
| `sub_wallets` | id, name, wallet_type, currency, available_balance, blocked_balance | Sous-portefeuilles |
| `wallet_subwallet` | wallet_id, subwallet_id | Table de liaison M2M |
| `wallet_permissions` | id, wallet_id, permission_type, enabled, max_amount, daily_limit | Permissions granulaires |
| `wallet_status_history` | id, wallet_id, previous_status, new_status, reason, changed_at | Historique des changements |

**Enums:**
- `wallet_type_enum`: PERSONAL, BUSINESS, SAVINGS, ESCROW
- `wallet_status_enum`: ACTIVE, SUSPENDED, FROZEN, CLOSED
- `permission_type_enum`: SEND, RECEIVE, WITHDRAW, TRANSFER, VIEW_BALANCE, VIEW_HISTORY
- `currency_enum`: XAF, XOF, EUR, USD, GBP, NGN

---

#### 💳 zaphira_transactions_db (Transaction Service)

| Table | Colonnes Clés | Description |
|-------|---------------|-------------|
| `transactions` | id, reference, type, status, sender_wallet_number, receiver_wallet_number, amount, currency | Transactions principales |
| `transaction_status_history` | id, transaction_id, previous_status, new_status, changed_by | Historique des statuts |
| `transaction_states` | id, transaction_id, state, entered_at, exited_at, actor_user_id | Machine d'états |
| `transaction_authorizations` | id, transaction_id, method, status, approved_at, expires_at | Autorisations 2FA |
| `transaction_settlements` | id, transaction_id, status, original_amount, settled_amount, fx_rate | Règlements |
| `transaction_refunds` | id, original_transaction_id, refund_amount, refund_type, status | Remboursements |
| `transaction_audit_log` | id, transaction_id, action_type, actor_user_id, ip_address | Audit détaillé |
| `scheduled_transactions` | id, sender_wallet_number, receiver_wallet_number, scheduled_for, status | Transactions planifiées |
| `validation_requests` | id, transaction_id, correlation_id, status, expires_at | Workflows de validation |

**Enums:**
- `transaction_type_enum`: TRANSFER, PAYMENT, DEPOSIT, WITHDRAWAL, REFUND, REVERSAL
- `transaction_status_enum`: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED, REFUNDED
- `authorization_method_enum`: PIN, OTP, BIOMETRIC, TWO_FACTOR, NONE
- `settlement_status_enum`: PENDING, IN_PROGRESS, COMPLETED, FAILED, RETRYING

---

#### ⚖️ zaphira_disputes_db (Transaction Service - Disputes)

| Table | Colonnes Clés | Description |
|-------|---------------|-------------|
| `disputes` | id, transaction_id, initiated_by, category, status, claimed_amount, resolution_type | Litiges |
| `dispute_evidence` | id, dispute_id, evidence_type, file_url, submitted_by | Preuves soumises |
| `dispute_timeline` | id, dispute_id, event_type, old_status, new_status, actor | Timeline des événements |
| `dispute_resolutions` | id, dispute_id, resolution_type, resolution_amount, resolved_by | Résolutions finales |

**Enums:**
- `dispute_status_enum`: OPEN, PENDING, UNDER_REVIEW, ESCALATED, RESOLVED, CLOSED, WITHDRAWN, REJECTED
- `dispute_category_enum`: UNAUTHORIZED_TRANSACTION, SERVICE_NOT_RECEIVED, DUPLICATE_CHARGE, INCORRECT_AMOUNT, etc.
- `resolution_type_enum`: FULL_REFUND, PARTIAL_REFUND, NO_REFUND, MERCHANT_FAVOR, CUSTOMER_FAVOR

---

#### 📊 zaphira_reports_db (Transaction Service - Reporting)

| Table | Colonnes Clés | Description |
|-------|---------------|-------------|
| `daily_reports` | id, report_date, total_transactions, total_volume, total_fees, success_rate | Rapports journaliers plateforme |
| `merchant_analytics` | id, merchant_id, analytics_date, total_volume, dispute_rate, risk_score | Analytics marchands |
| `user_analytics` | id, user_id, analytics_date, transaction_count, transaction_volume | Analytics utilisateurs |
| `aggregate_summaries` | id, period_type, start_date, end_date, metrics_json | Résumés agrégés |

---

#### 💱 zaphira_exchange_db (Transaction Service - Exchange)

| Table | Colonnes Clés | Description |
|-------|---------------|-------------|
| `exchange_rates` | id, source_currency, target_currency, rate, effective_date | Taux de change actuels |
| `exchange_rate_history` | id, source_currency, target_currency, rate, recorded_at | Historique des taux |

---

#### 📧 zaphira_notifications_db (Notification Service)

| Table | Colonnes Clés | Description |
|-------|---------------|-------------|
| `notifications` | id, user_id, type, title, message, status, channel, sent_at | Notifications envoyées |
| `verification_tokens` | id, user_id, token, type, expires_at, verified_at | Tokens de vérification email/SMS |
| `otp_codes` | id, user_id, code, purpose, expires_at, verified | Codes OTP |

**Enums:**
- `notification_type_enum`: TRANSACTION, SECURITY, ACCOUNT, MARKETING, DISPUTE, SYSTEM
- `notification_status_enum`: PENDING, SENT, DELIVERED, READ, FAILED

---

## 3. Événements Kafka

### 3.1 Vue d'ensemble des Topics

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         KAFKA TOPICS (14 Topics)                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  USER EVENTS                    TRANSACTION EVENTS                          │
│  ├── user-registered            ├── transaction-created                     │
│  ├── user-created-topic         ├── transaction-completed                   │
│  └── email-send-events          ├── transaction-failed                      │
│                                 ├── transaction-cancelled                   │
│  WALLET EVENTS                  ├── transaction.refunded                    │
│  ├── wallet-created             ├── transaction.reversed                    │
│  ├── wallet-created-topic       └── transaction.validation.result           │
│  └── wallet-balance-updated                                                 │
│                                 DISPUTE EVENTS                              │
│  NOTIFICATION EVENTS            ├── dispute-created                         │
│  ├── verification-email-events  ├── dispute-resolved                        │
│  └── verification-sms-events    └── settlement-completed                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Détail des Événements par Service

#### 📤 Producteurs d'Événements

| Service | Topic | Event Class | Trigger | Données Clés |
|---------|-------|-------------|---------|--------------|
| **User Service** | `user-registered` | UserRegisteredEvent | Inscription réussie | userId, email, firstName, lastName, phoneNumber, roleType |
| **User Service** | `user-created-topic` | UserCreatedEvent | Création user | userId, correlationId |
| **User Service** | `email-send-events` | EmailEvent | Envoi email | email, subject, body, type |
| **Auth Service** | `user-registered` | UserRegisteredEvent | Activation compte | userId, email, firstName, lastName |
| **Wallet Service** | `wallet-created-topic` | WalletCreatedResponse | Création wallet | userId, walletNumber, correlationId |
| **Wallet Service** | `wallet-balance-updated` | WalletBalanceUpdatedEvent | Mise à jour solde | walletNumber, userId, newBalance |
| **Transaction Service** | `transaction-created` | TransactionCreatedEvent | Nouvelle transaction | transactionId, reference, amount, status |
| **Transaction Service** | `transaction-completed` | TransactionCompletedEvent | Transaction terminée | transactionId, reference, completedAt |
| **Transaction Service** | `transaction-failed` | TransactionEvent | Échec transaction | transactionId, failureReason |
| **Transaction Service** | `transaction-cancelled` | TransactionEvent | Annulation | transactionId, reference |
| **Transaction Service** | `transaction.refunded` | TransactionRefundedEvent | Remboursement | originalRef, refundRef, amount, reason |
| **Transaction Service** | `transaction.reversed` | TransactionReversedEvent | Inversion | originalRef, reversalRef, reason |
| **Transaction Service** | `dispute-created` | DisputeCreatedEvent | Création litige | disputeId, transactionId, category, claimedAmount |
| **Transaction Service** | `dispute-resolved` | DisputeResolvedEvent | Résolution litige | disputeId, resolutionType, resolvedAmount |
| **Transaction Service** | `settlement-completed` | SettlementCompletedEvent | Règlement terminé | settlementId, transactionId, settledAmount |
| **Notification Service** | `verification-email-events` | VerificationEmailEvent | Email vérifié | userId, email, code, isResend |
| **Notification Service** | `verification-sms-events` | VerificationSmsEvent | SMS vérifié | userId, phone, code, isResend |

#### 📥 Consommateurs d'Événements

| Service | Topic | Handler | Action | Tables Impactées |
|---------|-------|---------|--------|------------------|
| **User Service** | `wallet-created-topic` | WalletResponseListener | Mise à jour wallet_number user | users |
| **Notification Service** | `user-registered` | UserEventListener | Email de bienvenue | notifications |
| **Notification Service** | `wallet-created` | WalletEventListener | Email création wallet | notifications |
| **Notification Service** | `wallet-balance-updated` | WalletEventListener | Email mise à jour solde | notifications |
| **Notification Service** | `transaction-created` | TransactionEventListener | Email notification transaction | notifications |
| **Notification Service** | `transaction.refunded` | TransactionAdditionalEventsListener | Email remboursement | notifications |
| **Notification Service** | `transaction.reversed` | TransactionAdditionalEventsListener | Email annulation | notifications |
| **Notification Service** | `transaction.validation.result` | TransactionAdditionalEventsListener | Email résultat validation | notifications |
| **Notification Service** | `email-send-events` | EmailEventListener | Envoi email effectif | notifications |

---

## 4. Auth Service

### 4.1 Endpoints (5 total)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 1 | POST | `/api/auth/login` | Connexion utilisateur (phone + PIN) | ❌ | refresh_token | - |
| 2 | POST | `/api/auth/refresh` | Renouveler access token | ❌ | refresh_token | - |
| 3 | POST | `/api/auth/logout` | Déconnexion (révoque tokens) | ❌ | refresh_token | - |
| 4 | POST | `/api/auth/validate` | Valider token JWT | ✅ JWT | - | - |
| 5 | GET | `/api/logs/{userId}` | Historique activité utilisateur | ❌ | activity_log | - |

### 4.2 DTOs

```java
// Requêtes
LoginRequest { phoneNumber, pin }
RefreshTokenRequest { refreshToken }
LogoutRequest { accessToken }

// Réponses
AuthResponse { accessToken, refreshToken, user: UserResponse }
UserResponse { userId, firstName, lastName, roleType, walletId }
ValidateTokenResponse { valid, userId, email, roleType, expiresAt, message }
ActivityLog { id, userId, activityType, description, ipAddress, userAgent, timestamp }
```

### 4.3 Flux de Données

```
┌──────────────┐     ┌───────────────┐     ┌─────────────────────────┐
│ POST /login  │────▶│  Auth Service │────▶│ zaphira_auth_db         │
└──────────────┘     │               │     │ - tokens                │
                     │ Validation    │     │ - refresh_token         │
                     │ Génération JWT│     │ - activity_log          │
                     └───────────────┘     └─────────────────────────┘
                            │
                            ▼
                     ┌───────────────┐
                     │ AuthResponse  │
                     │ {token, user} │
                     └───────────────┘
```

---

## 5. User Service

### 5.1 Endpoints (27 total)

#### 5.1.1 UserController (21 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 1 | POST | `/api/users/register` | Inscription | ❌ | users, otp_tokens | `user-registered` ▶ |
| 2 | POST | `/api/users/register/email` | Inscription par email | ❌ | users, otp_tokens | `email-send-events` ▶ |
| 3 | POST | `/api/users/verify/email` | Vérifier email OTP | ❌ | users, otp_tokens | - |
| 4 | POST | `/api/users/verify/email/link` | Vérifier lien email | ❌ | users, verification_tokens | - |
| 5 | POST | `/api/users/resend-otp` | Renvoyer OTP | ❌ | otp_tokens | `email-send-events` ▶ |
| 6 | GET | `/api/users/me` | Profil utilisateur courant | ✅ JWT | users, kyc | - |
| 7 | PUT | `/api/users/me` | Modifier profil | ✅ JWT | users | - |
| 8 | GET | `/api/users/{userId}` | Détails utilisateur | ✅ ADMIN | users, kyc | - |
| 9 | PUT | `/api/users/{userId}` | Modifier utilisateur | ✅ ADMIN | users | - |
| 10 | DELETE | `/api/users/{userId}` | Supprimer utilisateur | ✅ ADMIN | users | - |
| 11 | GET | `/api/users` | Liste utilisateurs (paginated) | ✅ ADMIN | users | - |
| 12 | GET | `/api/users/search` | Recherche utilisateurs | ✅ ADMIN | users | - |
| 13 | POST | `/api/users/{userId}/suspend` | Suspendre compte | ✅ ADMIN | users | - |
| 14 | POST | `/api/users/{userId}/reactivate` | Réactiver compte | ✅ ADMIN | users | - |
| 15 | PUT | `/api/users/{userId}/status` | Changer statut | ✅ ADMIN | users | - |
| 16 | GET | `/api/users/{userId}/sessions` | Sessions actives | ✅ JWT | user_sessions | - |
| 17 | DELETE | `/api/users/{userId}/sessions` | Terminer toutes sessions | ✅ JWT | user_sessions | - |
| 18 | DELETE | `/api/users/{userId}/sessions/{sessionId}` | Terminer une session | ✅ JWT | user_sessions | - |
| 19 | POST | `/api/users/2fa/enable` | Activer 2FA | ✅ JWT | users, otp_tokens | - |
| 20 | POST | `/api/users/2fa/disable` | Désactiver 2FA | ✅ JWT | users | - |
| 21 | POST | `/api/users/2fa/verify` | Vérifier code 2FA | ✅ JWT | otp_tokens | - |

#### 5.1.2 UserLookupController (1 endpoint)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 22 | GET | `/api/users/lookup/{walletNumber}` | Trouver user par wallet | ✅ JWT | users | - |

#### 5.1.3 PinResetController (4 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 23 | POST | `/api/users/pin/reset/init` | Initier reset PIN | ✅ JWT | otp_tokens | `email-send-events` ▶ |
| 24 | POST | `/api/users/pin/reset/verify` | Vérifier OTP reset | ✅ JWT | otp_tokens | - |
| 25 | POST | `/api/users/pin/reset/complete` | Compléter reset PIN | ✅ JWT | users | - |
| 26 | POST | `/api/users/pin/change` | Changer PIN | ✅ JWT | users | - |

#### 5.1.4 SecurityQuestionController (3 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 27 | GET | `/api/users/security-questions` | Liste questions | ✅ JWT | security_questions | - |
| 28 | POST | `/api/users/security-answers` | Enregistrer réponses | ✅ JWT | user_security_answers | - |
| 29 | POST | `/api/users/security-verify` | Vérifier réponses | ✅ JWT | user_security_answers | - |

### 5.2 Workflow: Inscription Utilisateur

```
┌─────────────────────┐
│ POST /register/email│
└─────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                     USER SERVICE                             │
│  1. Valider données (email unique, password fort)           │
│  2. Créer utilisateur (status: PENDING_VERIFICATION)        │
│  3. Générer OTP 6 digits                                    │
│  4. Stocker dans otp_tokens (expires: 10 min)               │
│  5. Publier email-send-events                               │
└─────────────────────────────────────────────────────────────┘
          │                           │
          │                           ▼
          │                 ┌─────────────────────┐
          │                 │ email-send-events   │
          │                 │ (Kafka Topic)       │
          │                 └─────────────────────┘
          │                           │
          │                           ▼
          │                 ┌─────────────────────┐
          │                 │NOTIFICATION SERVICE │
          │                 │ Envoie email + OTP  │
          │                 └─────────────────────┘
          │
          ▼
┌─────────────────────┐
│ POST /verify/email  │  (avec code OTP)
└─────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                     USER SERVICE                             │
│  1. Valider OTP (code + expiration)                         │
│  2. Marquer email vérifié                                   │
│  3. Activer compte (status: ACTIVE)                         │
│  4. Publier user-registered                                 │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────┐     ┌─────────────────────┐
│   user-registered   │────▶│   WALLET SERVICE    │
│   (Kafka Topic)     │     │ Création auto wallet│
└─────────────────────┘     └─────────────────────┘
          │
          ▼
┌─────────────────────┐     ┌─────────────────────┐
│wallet-created-topic │────▶│    USER SERVICE     │
│   (Kafka Topic)     │     │ Mise à jour user    │
└─────────────────────┘     │ avec wallet_number  │
                            └─────────────────────┘
```

---

## 6. Wallet Service

### 6.1 Endpoints (30 total)

#### 6.1.1 WalletController (26 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 1 | POST | `/api/wallets` | Créer wallet | ✅ JWT | wallets, wallet_status_history | `wallet-created` ▶ |
| 2 | GET | `/api/wallets/me` | Mon wallet principal | ✅ JWT | wallets, sub_wallets | - |
| 3 | GET | `/api/wallets/{walletNumber}` | Détails wallet | ✅ JWT | wallets | - |
| 4 | GET | `/api/wallets/user/{userId}` | Wallets d'un user | ✅ JWT | wallets | - |
| 5 | PUT | `/api/wallets/{walletNumber}` | Modifier wallet | ✅ JWT | wallets | - |
| 6 | GET | `/api/wallets/{walletNumber}/balance` | Consulter solde | ✅ JWT | wallets | - |
| 7 | POST | `/api/wallets/{walletNumber}/credit` | Créditer wallet | ✅ JWT | wallets | `wallet-balance-updated` ▶ |
| 8 | POST | `/api/wallets/{walletNumber}/debit` | Débiter wallet | ✅ JWT | wallets | `wallet-balance-updated` ▶ |
| 9 | POST | `/api/wallets/{walletNumber}/block` | Bloquer montant | ✅ JWT | wallets | - |
| 10 | POST | `/api/wallets/{walletNumber}/unblock` | Débloquer montant | ✅ JWT | wallets | - |
| 11 | POST | `/api/wallets/{walletNumber}/suspend` | Suspendre wallet | ✅ ADMIN | wallets, wallet_status_history | - |
| 12 | POST | `/api/wallets/{walletNumber}/reactivate` | Réactiver wallet | ✅ ADMIN | wallets, wallet_status_history | - |
| 13 | POST | `/api/wallets/{walletNumber}/freeze` | Geler wallet | ✅ ADMIN | wallets, wallet_status_history | - |
| 14 | POST | `/api/wallets/{walletNumber}/unfreeze` | Dégeler wallet | ✅ ADMIN | wallets, wallet_status_history | - |
| 15 | POST | `/api/wallets/{walletNumber}/close` | Fermer wallet | ✅ ADMIN | wallets, wallet_status_history | - |
| 16 | GET | `/api/wallets/{walletNumber}/history` | Historique statuts | ✅ JWT | wallet_status_history | - |
| 17 | GET | `/api/wallets/{walletNumber}/transactions` | Historique transactions | ✅ JWT | - (via transaction-service) | - |
| 18 | GET | `/api/wallets` | Liste wallets (admin) | ✅ ADMIN | wallets | - |
| 19 | GET | `/api/wallets/search` | Recherche wallets | ✅ ADMIN | wallets | - |
| 20 | POST | `/api/wallets/transfer` | Transfert interne | ✅ JWT | wallets | `wallet-balance-updated` ▶ |
| 21 | GET | `/api/wallets/{walletNumber}/limits` | Consulter limites | ✅ JWT | wallet_permissions | - |
| 22 | PUT | `/api/wallets/{walletNumber}/limits` | Modifier limites | ✅ ADMIN | wallet_permissions | - |
| 23 | POST | `/api/wallets/validate` | Valider numéro wallet | ✅ JWT | wallets | - |
| 24 | GET | `/api/wallets/{walletNumber}/subwallets` | Liste sous-wallets | ✅ JWT | sub_wallets, wallet_subwallet | - |
| 25 | POST | `/api/wallets/{walletNumber}/subwallets` | Créer sous-wallet | ✅ JWT | sub_wallets, wallet_subwallet | - |
| 26 | DELETE | `/api/wallets/{walletNumber}/subwallets/{subwalletId}` | Supprimer sous-wallet | ✅ JWT | sub_wallets, wallet_subwallet | - |

#### 6.1.2 SubWalletController (4 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 27 | GET | `/api/subwallets/{id}` | Détails sous-wallet | ✅ JWT | sub_wallets | - |
| 28 | PUT | `/api/subwallets/{id}` | Modifier sous-wallet | ✅ JWT | sub_wallets | - |
| 29 | POST | `/api/subwallets/{id}/transfer` | Transfert vers wallet | ✅ JWT | sub_wallets, wallets | - |
| 30 | GET | `/api/subwallets/{id}/balance` | Solde sous-wallet | ✅ JWT | sub_wallets | - |

### 6.2 Workflow: Transfert entre Wallets

```
┌─────────────────────┐
│ POST /wallets/      │
│      transfer       │
└─────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                     WALLET SERVICE                           │
│  1. Valider source wallet (exists, active, owner)           │
│  2. Valider destination wallet (exists, active)             │
│  3. Vérifier solde disponible                               │
│  4. Vérifier limites journalières                           │
│  5. Débiter source                                          │
│  6. Créditer destination                                    │
│  7. Publier wallet-balance-updated (x2)                     │
└─────────────────────────────────────────────────────────────┘
          │
          ├─────────────────────────────────┐
          ▼                                 ▼
┌─────────────────────┐     ┌─────────────────────┐
│wallet-balance-updated│    │wallet-balance-updated│
│  (Source Wallet)    │     │  (Dest Wallet)      │
└─────────────────────┘     └─────────────────────┘
          │                           │
          └─────────────┬─────────────┘
                        ▼
              ┌─────────────────────┐
              │NOTIFICATION SERVICE │
              │ Emails aux 2 users  │
              └─────────────────────┘
```

---

## 7. Transaction Service

### 7.1 Endpoints (58 total)

#### 7.1.1 TransactionCoreController (21 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 1 | POST | `/api/transactions` | Créer transaction | ✅ JWT | transactions, transaction_status_history | `transaction-created` ▶ |
| 2 | GET | `/api/transactions/{reference}` | Détails transaction | ✅ JWT | transactions | - |
| 3 | GET | `/api/transactions/id/{id}` | Transaction par ID | ✅ JWT | transactions | - |
| 4 | GET | `/api/transactions` | Liste transactions | ✅ JWT | transactions | - |
| 5 | GET | `/api/transactions/wallet/{walletNumber}` | Transactions d'un wallet | ✅ JWT | transactions | - |
| 6 | GET | `/api/transactions/user/{userId}` | Transactions d'un user | ✅ JWT | transactions | - |
| 7 | POST | `/api/transactions/{reference}/cancel` | Annuler transaction | ✅ JWT | transactions | `transaction-cancelled` ▶ |
| 8 | POST | `/api/transactions/{reference}/reverse` | Inverser transaction | ✅ ADMIN | transactions, transaction_refunds | `transaction.reversed` ▶ |
| 9 | POST | `/api/transactions/{reference}/refund` | Rembourser transaction | ✅ ADMIN | transactions, transaction_refunds | `transaction.refunded` ▶ |
| 10 | GET | `/api/transactions/{reference}/status` | Statut transaction | ✅ JWT | transactions | - |
| 11 | GET | `/api/transactions/{reference}/history` | Historique statuts | ✅ JWT | transaction_status_history | - |
| 12 | POST | `/api/transactions/{reference}/authorize` | Autoriser transaction | ✅ JWT | transaction_authorizations | - |
| 13 | POST | `/api/transactions/{reference}/reject` | Rejeter transaction | ✅ JWT | transaction_authorizations | `transaction-failed` ▶ |
| 14 | GET | `/api/transactions/search` | Recherche avancée | ✅ JWT | transactions | - |
| 15 | GET | `/api/transactions/stats` | Statistiques | ✅ ADMIN | transactions | - |
| 16 | POST | `/api/transactions/batch` | Transactions en lot | ✅ MERCHANT | transactions | `transaction-created` ▶ (multiple) |
| 17 | GET | `/api/transactions/pending` | Transactions en attente | ✅ JWT | transactions | - |
| 18 | POST | `/api/transactions/{reference}/settle` | Règlement manuel | ✅ ADMIN | transaction_settlements | `settlement-completed` ▶ |
| 19 | GET | `/api/transactions/{reference}/settlement` | Détails règlement | ✅ JWT | transaction_settlements | - |
| 20 | GET | `/api/transactions/{reference}/audit` | Journal audit | ✅ ADMIN | transaction_audit_log | - |
| 21 | POST | `/api/transactions/validate` | Valider avant création | ✅ JWT | - | - |

#### 7.1.2 ScheduledTransactionController (7 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 22 | POST | `/api/scheduled-transactions` | Planifier transaction | ✅ JWT | scheduled_transactions | - |
| 23 | GET | `/api/scheduled-transactions` | Liste planifiées | ✅ JWT | scheduled_transactions | - |
| 24 | GET | `/api/scheduled-transactions/{id}` | Détails planifiée | ✅ JWT | scheduled_transactions | - |
| 25 | PUT | `/api/scheduled-transactions/{id}` | Modifier planifiée | ✅ JWT | scheduled_transactions | - |
| 26 | DELETE | `/api/scheduled-transactions/{id}` | Annuler planifiée | ✅ JWT | scheduled_transactions | - |
| 27 | POST | `/api/scheduled-transactions/{id}/execute` | Exécuter maintenant | ✅ JWT | scheduled_transactions, transactions | `transaction-created` ▶ |
| 28 | GET | `/api/scheduled-transactions/pending` | Planifiées en attente | ✅ JWT | scheduled_transactions | - |

#### 7.1.3 DisputeController (8 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 29 | POST | `/api/disputes` | Créer litige | ✅ JWT | disputes, dispute_timeline | `dispute-created` ▶ |
| 30 | GET | `/api/disputes/{reference}` | Détails litige | ✅ JWT | disputes, dispute_evidence | - |
| 31 | GET | `/api/disputes` | Liste litiges | ✅ JWT | disputes | - |
| 32 | GET | `/api/disputes/user/{userId}` | Litiges d'un user | ✅ JWT | disputes | - |
| 33 | GET | `/api/disputes/transaction/{transactionId}` | Litiges d'une transaction | ✅ JWT | disputes | - |
| 34 | POST | `/api/disputes/{reference}/evidence` | Ajouter preuve | ✅ JWT | dispute_evidence, dispute_timeline | - |
| 35 | POST | `/api/disputes/{reference}/resolve` | Résoudre litige | ✅ ADMIN | disputes, dispute_timeline | `dispute-resolved` ▶ |
| 36 | POST | `/api/disputes/{reference}/escalate` | Escalader litige | ✅ ADMIN | disputes, dispute_timeline | - |
| 37 | POST | `/api/disputes/{reference}/withdraw` | Retirer litige | ✅ JWT | disputes, dispute_timeline | - |
| 38 | GET | `/api/disputes/{reference}/timeline` | Timeline litige | ✅ JWT | dispute_timeline | - |

#### 7.1.4 MerchantReportController (6 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 39 | GET | `/api/reports/merchant/{merchantId}` | Rapport marchand | ✅ MERCHANT | merchant_analytics | - |
| 40 | GET | `/api/reports/merchant/{merchantId}/daily` | Rapport journalier | ✅ MERCHANT | merchant_analytics | - |
| 41 | GET | `/api/reports/merchant/{merchantId}/weekly` | Rapport hebdomadaire | ✅ MERCHANT | merchant_analytics | - |
| 42 | GET | `/api/reports/merchant/{merchantId}/monthly` | Rapport mensuel | ✅ MERCHANT | merchant_analytics | - |
| 43 | GET | `/api/reports/platform` | Rapport plateforme | ✅ ADMIN | daily_reports | - |
| 44 | POST | `/api/reports/generate` | Générer rapport | ✅ ADMIN | daily_reports, merchant_analytics | - |

#### 7.1.5 ExchangeRateController (5 endpoints)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 45 | GET | `/api/exchange-rates` | Taux actuels | ❌ | exchange_rates | - |
| 46 | GET | `/api/exchange-rates/{source}/{target}` | Taux spécifique | ❌ | exchange_rates | - |
| 47 | POST | `/api/exchange-rates` | Créer taux | ✅ ADMIN | exchange_rates | - |
| 48 | PUT | `/api/exchange-rates/{id}` | Modifier taux | ✅ ADMIN | exchange_rates | - |
| 49 | GET | `/api/exchange-rates/history` | Historique taux | ❌ | exchange_rate_history | - |

#### 7.1.6 Autres endpoints Transaction Service

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 50-58 | - | (endpoints additionnels) | Settlements, Refunds avancés | ✅ | Various | Various |

### 7.2 Workflow: Transaction Complète

```
┌─────────────────────┐
│POST /api/transactions│
└─────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                  TRANSACTION SERVICE                         │
│  1. Valider données (montant, wallets, type)                │
│  2. Vérifier autorisations (PIN/OTP si requis)              │
│  3. Créer transaction (status: PENDING)                      │
│  4. Insérer transaction_status_history                       │
│  5. Publier transaction-created                              │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────┐
│ transaction-created │
│   (Kafka Topic)     │
└─────────────────────┘
          │
          ├──────────────────┐
          ▼                  ▼
┌─────────────────┐  ┌─────────────────────┐
│NOTIFICATION SVC │  │   WALLET SERVICE    │
│Email notification│  │(si appel synchrone)│
└─────────────────┘  │ Débiter/Créditer    │
                     └─────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  TRANSACTION SERVICE                         │
│  6. Mettre à jour statut → COMPLETED                        │
│  7. Créer settlement si requis                              │
│  8. Publier transaction-completed                           │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌───────────────────────┐
│ transaction-completed │
│    (Kafka Topic)      │
└───────────────────────┘
```

---

## 8. Notification Service

### 8.1 Endpoints (10 total)

| # | Méthode | Endpoint | Description | Auth | Tables | Kafka |
|---|---------|----------|-------------|------|--------|-------|
| 1 | POST | `/api/notifications/otp/send` | Envoyer OTP | ✅ JWT | otp_codes | `verification-email-events` ▶ |
| 2 | POST | `/api/notifications/otp/verify` | Vérifier OTP | ✅ JWT | otp_codes | - |
| 3 | POST | `/api/notifications/email/send` | Envoyer email | ✅ SERVICE | notifications | - |
| 4 | POST | `/api/notifications/sms/send` | Envoyer SMS | ✅ SERVICE | notifications | - |
| 5 | GET | `/api/notifications/user/{userId}` | Notifications user | ✅ JWT | notifications | - |
| 6 | GET | `/api/notifications/{id}` | Détails notification | ✅ JWT | notifications | - |
| 7 | PUT | `/api/notifications/{id}/read` | Marquer comme lu | ✅ JWT | notifications | - |
| 8 | DELETE | `/api/notifications/{id}` | Supprimer notification | ✅ JWT | notifications | - |
| 9 | POST | `/api/notifications/transaction-status` | Notifier statut transaction | ✅ SERVICE | notifications | - |
| 10 | POST | `/api/notifications/dispute-update` | Notifier maj litige | ✅ SERVICE | notifications | - |

### 8.2 Flux Kafka Consommés

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       NOTIFICATION SERVICE - KAFKA LISTENERS                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐     ┌─────────────────────────────────────────────┐   │
│  │ user-registered │────▶│ UserEventListener                           │   │
│  └─────────────────┘     │ → Email de bienvenue                        │   │
│                          └─────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────┐     ┌─────────────────────────────────────────────┐   │
│  │ wallet-created  │────▶│ WalletEventListener                         │   │
│  └─────────────────┘     │ → Email + SMS création wallet               │   │
│                          └─────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────┐ ┌─────────────────────────────────────────────┐   │
│  │wallet-balance-updated│▶│ WalletEventListener                         │   │
│  └─────────────────────┘ │ → Email + SMS mise à jour solde             │   │
│                          └─────────────────────────────────────────────┘   │
│                                                                             │
│  ┌───────────────────┐   ┌─────────────────────────────────────────────┐   │
│  │transaction-created│──▶│ TransactionEventListener                    │   │
│  └───────────────────┘   │ → Email notification aux 2 parties          │   │
│                          └─────────────────────────────────────────────┘   │
│                                                                             │
│  ┌───────────────────┐   ┌─────────────────────────────────────────────┐   │
│  │transaction.refunded│─▶│ TransactionAdditionalEventsListener         │   │
│  └───────────────────┘   │ → Email notification remboursement          │   │
│                          └─────────────────────────────────────────────┘   │
│                                                                             │
│  ┌───────────────────┐   ┌─────────────────────────────────────────────┐   │
│  │transaction.reversed│─▶│ TransactionAdditionalEventsListener         │   │
│  └───────────────────┘   │ → Email notification annulation             │   │
│                          └─────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────┐ ┌─────────────────────────────────────────┐   │
│  │transaction.validation.  │▶│ TransactionAdditionalEventsListener     │   │
│  │         result          │ │ → Email résultat validation             │   │
│  └─────────────────────────┘ └─────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────┐ ┌─────────────────────────────────────────────┐   │
│  │ email-send-events  │─▶│ EmailEventListener                          │   │
│  └─────────────────────┘ │ → Envoi effectif via SMTP/Twilio            │   │
│                          └─────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Workflows Métier Complets

### 9.1 Workflow 1: Inscription → Vérification → Wallet

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    WORKFLOW: INSCRIPTION UTILISATEUR                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 1: Inscription                                                       │
│  ─────────────────────                                                      │
│  Client ──▶ POST /api/users/register/email                                  │
│          │  Body: { email, password, firstName, lastName, phoneNumber }     │
│          ▼                                                                  │
│  [USER SERVICE]                                                             │
│  • Valider unicité email                                                    │
│  • Hash password (BCrypt)                                                   │
│  • Créer user (status: PENDING_VERIFICATION)                               │
│  • Générer OTP 6 digits                                                     │
│  • Stocker OTP (expires: 10 min)                                           │
│  • ▶ Kafka: email-send-events                                               │
│                                                                             │
│  [NOTIFICATION SERVICE]                                                     │
│  • ◀ Kafka: email-send-events                                               │
│  • Envoyer email avec OTP                                                   │
│                                                                             │
│  Tables: users, otp_tokens, notifications                                   │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 2: Vérification Email                                                │
│  ────────────────────────────                                               │
│  Client ──▶ POST /api/users/verify/email                                    │
│          │  Body: { email, code: "123456" }                                 │
│          ▼                                                                  │
│  [USER SERVICE]                                                             │
│  • Valider OTP (code + non-expiré)                                         │
│  • Marquer email vérifié                                                    │
│  • Activer compte (status: ACTIVE)                                          │
│  • ▶ Kafka: user-registered                                                 │
│                                                                             │
│  Tables: users, otp_tokens                                                  │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 3: Création Wallet Automatique                                       │
│  ─────────────────────────────────────                                      │
│  [WALLET SERVICE] (déclenché par Kafka, optionnel si sync)                  │
│  • ◀ Kafka: user-registered                                                 │
│  • Créer wallet (type: PERSONAL, currency: XAF)                            │
│  • Générer wallet_number unique (8 chars)                                   │
│  • Créer permissions par défaut                                             │
│  • ▶ Kafka: wallet-created-topic                                            │
│                                                                             │
│  [USER SERVICE]                                                             │
│  • ◀ Kafka: wallet-created-topic                                            │
│  • Mettre à jour user.wallet_number                                         │
│                                                                             │
│  [NOTIFICATION SERVICE]                                                     │
│  • ◀ Kafka: user-registered                                                 │
│  • Envoyer email de bienvenue                                               │
│  • ◀ Kafka: wallet-created                                                  │
│  • Envoyer notification création wallet                                     │
│                                                                             │
│  Tables: wallets, wallet_permissions, users, notifications                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 9.2 Workflow 2: Transaction P2P

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    WORKFLOW: TRANSFERT P2P                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 1: Initiation Transaction                                            │
│  ───────────────────────────────                                            │
│  Client ──▶ POST /api/transactions                                          │
│          │  Body: {                                                         │
│          │    senderWalletNumber: "12345678",                               │
│          │    receiverWalletNumber: "87654321",                             │
│          │    amount: 50000,                                                │
│          │    currency: "XAF",                                              │
│          │    type: "TRANSFER",                                             │
│          │    description: "Paiement facture"                               │
│          │  }                                                               │
│          ▼                                                                  │
│  [TRANSACTION SERVICE]                                                      │
│  • Valider wallets (existent, actifs)                                       │
│  • Vérifier solde sender >= amount                                          │
│  • Vérifier limites journalières                                            │
│  • Créer transaction (status: PENDING)                                      │
│  • Générer reference unique                                                 │
│  • ▶ Kafka: transaction-created                                             │
│                                                                             │
│  Tables: transactions, transaction_status_history                           │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 2: Exécution Transaction                                             │
│  ──────────────────────────────                                             │
│  [WALLET SERVICE] (appelé par Transaction Service via HTTP ou Kafka)        │
│  • Bloquer montant sur sender                                               │
│  • Débiter sender (available_balance -= amount)                             │
│  • Créditer receiver (available_balance += amount)                          │
│  • ▶ Kafka: wallet-balance-updated (x2)                                     │
│                                                                             │
│  [TRANSACTION SERVICE]                                                      │
│  • Mettre à jour transaction (status: COMPLETED)                            │
│  • Insérer transaction_status_history                                       │
│  • Créer settlement record                                                  │
│  • ▶ Kafka: transaction-completed                                           │
│                                                                             │
│  Tables: wallets (x2), transactions, transaction_settlements                │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 3: Notifications                                                     │
│  ──────────────────────                                                     │
│  [NOTIFICATION SERVICE]                                                     │
│  • ◀ Kafka: transaction-created                                             │
│  • Email sender: "Votre transfert de 50,000 XAF est en cours"              │
│  • Email receiver: "Vous allez recevoir 50,000 XAF"                        │
│  • ◀ Kafka: wallet-balance-updated                                          │
│  • Email sender: "Solde mis à jour: -50,000 XAF"                           │
│  • Email receiver: "Solde mis à jour: +50,000 XAF"                         │
│                                                                             │
│  Tables: notifications                                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 9.3 Workflow 3: Litige et Résolution

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    WORKFLOW: GESTION DES LITIGES                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 1: Création du Litige                                                │
│  ────────────────────────────                                               │
│  Client ──▶ POST /api/disputes                                              │
│          │  Body: {                                                         │
│          │    transactionId: 12345,                                         │
│          │    category: "UNAUTHORIZED_TRANSACTION",                         │
│          │    reason: "Transaction non autorisée",                          │
│          │    claimedAmount: 50000                                          │
│          │  }                                                               │
│          ▼                                                                  │
│  [TRANSACTION SERVICE - DISPUTE]                                            │
│  • Valider transaction existe                                               │
│  • Vérifier user est partie prenante                                        │
│  • Créer dispute (status: OPEN)                                             │
│  • Générer reference unique                                                 │
│  • Insérer timeline event (CREATED)                                         │
│  • ▶ Kafka: dispute-created                                                 │
│                                                                             │
│  Tables: disputes, dispute_timeline                                         │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 2: Soumission Preuves                                                │
│  ────────────────────────────                                               │
│  Client ──▶ POST /api/disputes/{reference}/evidence                         │
│          │  Body: {                                                         │
│          │    evidenceType: "SCREENSHOT",                                   │
│          │    fileUrl: "https://...",                                       │
│          │    description: "Capture écran"                                  │
│          │  }                                                               │
│          ▼                                                                  │
│  [TRANSACTION SERVICE - DISPUTE]                                            │
│  • Valider dispute existe et status permet ajout                            │
│  • Créer dispute_evidence                                                   │
│  • Incrémenter evidence_submitted_count                                     │
│  • Insérer timeline event (EVIDENCE_ADDED)                                  │
│                                                                             │
│  Tables: disputes, dispute_evidence, dispute_timeline                       │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ÉTAPE 3: Résolution par Admin                                              │
│  ─────────────────────────────                                              │
│  Admin ──▶ POST /api/disputes/{reference}/resolve                           │
│         │  Body: {                                                          │
│         │    resolutionType: "FULL_REFUND",                                 │
│         │    resolutionAmount: 50000,                                       │
│         │    resolutionReason: "Transaction frauduleuse confirmée"          │
│         │  }                                                                │
│         ▼                                                                   │
│  [TRANSACTION SERVICE - DISPUTE RESOLUTION]                                 │
│  • Valider dispute status permet résolution                                 │
│  • Mettre à jour dispute (status: RESOLVED)                                 │
│  • Stocker resolution details                                               │
│  • Insérer timeline event (RESOLVED)                                        │
│  • Si refund: créer refund transaction                                      │
│  • ▶ Kafka: dispute-resolved                                                │
│                                                                             │
│  [WALLET SERVICE] (si remboursement)                                        │
│  • Créditer wallet client                                                   │
│  • Débiter wallet marchand                                                  │
│  • ▶ Kafka: wallet-balance-updated                                          │
│                                                                             │
│  [NOTIFICATION SERVICE]                                                     │
│  • ◀ Kafka: dispute-resolved                                                │
│  • Email aux parties: "Litige résolu - Remboursement accordé"              │
│                                                                             │
│  Tables: disputes, dispute_timeline, transactions, wallets, notifications   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Matrice des Tests

### 10.1 Tests Auth Service

| Test ID | Endpoint | Scénario | Données Test | Résultat Attendu | Tables |
|---------|----------|----------|--------------|------------------|--------|
| AUTH-001 | POST /login | Login valide | email: test@test.com, password: Test123! | 200 + JWT | tokens, activity_log |
| AUTH-002 | POST /login | Password incorrect | password: wrong | 401 Unauthorized | activity_log |
| AUTH-003 | POST /login | User inexistant | email: unknown@test.com | 401 Unauthorized | - |
| AUTH-004 | POST /register | Inscription valide | email unique, password fort | 201 Created | users |
| AUTH-005 | POST /refresh | Token valide | refreshToken valid | 200 + new JWT | tokens, refresh_token |
| AUTH-006 | POST /refresh | Token expiré | refreshToken expired | 401 Unauthorized | - |
| AUTH-007 | POST /logout | Déconnexion | JWT valide | 200 OK | tokens, refresh_token |
| AUTH-008 | GET /validate | Token valide | JWT valide | 200 + user info | - |
| AUTH-009 | GET /validate | Token expiré | JWT expiré | 401 Unauthorized | - |

### 10.2 Tests User Service

| Test ID | Endpoint | Scénario | Données Test | Résultat Attendu | Tables |
|---------|----------|----------|--------------|------------------|--------|
| USER-001 | POST /register/email | Inscription email | email unique | 201 + OTP envoyé | users, otp_tokens |
| USER-002 | POST /register/email | Email déjà utilisé | email existant | 409 Conflict | - |
| USER-003 | POST /verify/email | OTP correct | code valide | 200 + account active | users, otp_tokens |
| USER-004 | POST /verify/email | OTP expiré | code expiré | 400 Bad Request | - |
| USER-005 | GET /me | Profil courant | JWT valide | 200 + user data | users |
| USER-006 | PUT /me | Modifier profil | firstName: "New" | 200 + updated | users |
| USER-007 | GET /{userId}/sessions | Sessions actives | userId valide | 200 + sessions[] | user_sessions |
| USER-008 | POST /2fa/enable | Activer 2FA | JWT valide | 200 + QR code | users |
| USER-009 | POST /pin/reset/init | Reset PIN | JWT valide | 200 + OTP | otp_tokens |

### 10.3 Tests Wallet Service

| Test ID | Endpoint | Scénario | Données Test | Résultat Attendu | Tables |
|---------|----------|----------|--------------|------------------|--------|
| WAL-001 | POST /wallets | Créer wallet | userId valide | 201 + wallet | wallets |
| WAL-002 | GET /me | Mon wallet | JWT valide | 200 + wallet | wallets |
| WAL-003 | GET /{walletNumber}/balance | Consulter solde | wallet existant | 200 + balance | wallets |
| WAL-004 | POST /{walletNumber}/credit | Créditer | amount: 10000 | 200 + new balance | wallets |
| WAL-005 | POST /{walletNumber}/debit | Débiter | amount: 5000 | 200 + new balance | wallets |
| WAL-006 | POST /{walletNumber}/debit | Solde insuffisant | amount > balance | 400 Insufficient | - |
| WAL-007 | POST /transfer | Transfert valide | sender, receiver, amount | 200 + transfer | wallets (x2) |
| WAL-008 | POST /{walletNumber}/suspend | Suspendre | ADMIN role | 200 + suspended | wallets, wallet_status_history |
| WAL-009 | POST /{walletNumber}/subwallets | Créer sous-wallet | name, type | 201 + subwallet | sub_wallets |

### 10.4 Tests Transaction Service

| Test ID | Endpoint | Scénario | Données Test | Résultat Attendu | Tables |
|---------|----------|----------|--------------|------------------|--------|
| TXN-001 | POST /transactions | Créer transaction | wallets valides, amount | 201 + transaction | transactions |
| TXN-002 | GET /{reference} | Détails transaction | reference existante | 200 + transaction | transactions |
| TXN-003 | POST /{reference}/cancel | Annuler pending | status: PENDING | 200 + cancelled | transactions |
| TXN-004 | POST /{reference}/cancel | Annuler completed | status: COMPLETED | 400 Bad Request | - |
| TXN-005 | POST /{reference}/refund | Remboursement | ADMIN + completed | 201 + refund | transaction_refunds |
| TXN-006 | GET /{reference}/history | Historique statuts | reference valide | 200 + history[] | transaction_status_history |
| TXN-007 | POST /disputes | Créer litige | transactionId valide | 201 + dispute | disputes, dispute_timeline |
| TXN-008 | POST /disputes/{ref}/evidence | Ajouter preuve | dispute OPEN | 201 + evidence | dispute_evidence |
| TXN-009 | POST /disputes/{ref}/resolve | Résoudre | ADMIN role | 200 + resolved | disputes |
| TXN-010 | POST /scheduled-transactions | Planifier | date future | 201 + scheduled | scheduled_transactions |

### 10.5 Tests Notification Service

| Test ID | Endpoint | Scénario | Données Test | Résultat Attendu | Tables |
|---------|----------|----------|--------------|------------------|--------|
| NOT-001 | POST /otp/send | Envoyer OTP | userId, purpose | 200 + OTP | otp_codes |
| NOT-002 | POST /otp/verify | Vérifier OTP valide | code correct | 200 + verified | otp_codes |
| NOT-003 | POST /otp/verify | OTP expiré | code expiré | 400 Bad Request | - |
| NOT-004 | POST /email/send | Envoyer email | email, subject, body | 200 + sent | notifications |
| NOT-005 | GET /user/{userId} | Notifications user | userId valide | 200 + notifications[] | notifications |
| NOT-006 | PUT /{id}/read | Marquer lu | notification existante | 200 + read | notifications |

### 10.6 Tests Kafka Events

| Test ID | Topic | Événement | Producteur | Consommateur | Résultat Attendu |
|---------|-------|-----------|------------|--------------|------------------|
| KAF-001 | user-registered | UserRegisteredEvent | User Service | Notification, Wallet | Email bienvenue + Wallet créé |
| KAF-002 | wallet-created-topic | WalletCreatedResponse | Wallet Service | User Service | User.wallet_number mis à jour |
| KAF-003 | transaction-created | TransactionCreatedEvent | Transaction Service | Notification | Emails aux parties |
| KAF-004 | dispute-created | DisputeCreatedEvent | Transaction Service | Notification | Email notification litige |
| KAF-005 | dispute-resolved | DisputeResolvedEvent | Transaction Service | Notification | Email résolution |
| KAF-006 | wallet-balance-updated | WalletBalanceUpdatedEvent | Wallet Service | Notification | Email/SMS solde |

---

## 📝 Notes Finales

### Conventions API

- **Base URL**: `http://localhost:8080/api` (via API Gateway)
- **Authentication**: Bearer JWT token dans header `Authorization`
- **Content-Type**: `application/json`
- **Pagination**: `?page=0&size=20&sort=createdAt,desc`
- **Erreurs**: Format standard `{ error, message, timestamp, path }`

### Variables d'environnement

```bash
# Auth
JWT_SECRET=your-secret-key
JWT_EXPIRATION=3600000
REFRESH_EXPIRATION=604800000

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_GROUP_ID=zaphira-service-group

# PostgreSQL
DB_HOST=localhost
DB_PORT=5432

# Notification
TWILIO_SID=your-sid
TWILIO_TOKEN=your-token
SMTP_HOST=smtp.gmail.com
```

### Codes de Statut HTTP

| Code | Signification | Utilisation |
|------|---------------|-------------|
| 200 | OK | Succès lecture/modification |
| 201 | Created | Ressource créée |
| 204 | No Content | Suppression réussie |
| 400 | Bad Request | Données invalides |
| 401 | Unauthorized | Token manquant/invalide |
| 403 | Forbidden | Droits insuffisants |
| 404 | Not Found | Ressource inexistante |
| 409 | Conflict | Conflit (email déjà utilisé) |
| 422 | Unprocessable | Validation métier échouée |
| 500 | Server Error | Erreur interne |

---

> **Document généré automatiquement**  
> **Total**: 130+ endpoints, 8 bases de données, 14 topics Kafka  
> **Status**: 100% Production Ready
