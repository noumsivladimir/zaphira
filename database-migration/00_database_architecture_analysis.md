# 📊 ZAPHIRA Platform - Database Architecture Analysis & Mapping

**Date**: 21 Janvier 2026  
**Auteur**: AI Backend Architect  
**Version**: 1.0

---

## 🎯 Executive Summary

Ce document présente l'analyse complète du projet Spring Boot Zaphira et le mapping optimisé de la structure de base de données pour une architecture microservices.

### Architecture choisie: **Database per Microservice Pattern**

Chaque microservice possède sa propre base de données PostgreSQL, garantissant:
- ✅ **Isolation**: Pas de couplage au niveau base de données
- ✅ **Scalabilité**: Chaque DB peut être optimisée indépendamment
- ✅ **Résilience**: La panne d'une DB n'affecte pas les autres services
- ✅ **Évolutivité**: Changements de schéma sans impact sur les autres services

---

## 📦 Microservices Identifiés & Mapping

### 1️⃣ **USER-SERVICE** → Database: `zaphira_users_db`

**Responsabilité**: Gestion des utilisateurs, authentification, KYC, sessions

#### Entités Java détectées:
| Entité Java | Table SQL | Description |
|-------------|-----------|-------------|
| `User` (abstract) | `users` | Table parent (Single Table Inheritance) |
| `RegularUser` | `users` (discriminator='REGULAR') | Utilisateurs réguliers |
| `MerchantUser` | `users` (discriminator='MERCHANT') | Commerçants |
| `AdminUser` | `users` (discriminator='ADMIN') | Administrateurs |
| `KYC` | `kyc` | Vérification d'identité |
| `UserSession` | `user_sessions` | Sessions utilisateurs actives |
| `OtpCode` | `otp_codes` | Codes OTP temporaires |
| `OtpToken` | `otp_tokens` | Tokens OTP |
| `SecurityQuestion` | `security_question` | Questions de sécurité |
| `PredefinedSecurityQuestion` | `predefined_security_questions` | Questions prédéfinies |
| `UserSecurityAnswer` | `user_security_answers` | Réponses de sécurité |

#### Tables additionnelles (détectées dans le schéma existant):
- `user_analytics` - Analytics par utilisateur
- `activity_log` - Journal des activités (peut être dans auth-service)

**Relations clés**:
- `User` 1:1 `KYC`
- `User` 1:N `UserSession`
- `User` 1:N `UserSecurityAnswer`

---

### 2️⃣ **AUTH-SERVICE** → Database: `zaphira_auth_db`

**Responsabilité**: Authentification, tokens JWT, logs d'activité

#### Entités Java détectées:
| Entité Java | Table SQL | Description |
|-------------|-----------|-------------|
| `Token` | `tokens` | JWT tokens |
| `RefreshToken` | `refresh_token` | Refresh tokens |
| `ActivityLog` | `activity_log` | Journal des actions utilisateur |

**Relations clés**:
- `RefreshToken` N:1 `User` (via user_id)
- `Token` N:1 `User` (via user_id)
- `ActivityLog` N:1 `User` (via user_id)

⚠️ **Note**: Ce service référence `User` de common-library (pas de duplication)

---

### 3️⃣ **WALLET-SERVICE** → Database: `zaphira_wallets_db`

**Responsabilité**: Gestion des portefeuilles, sous-comptes, permissions

#### Entités Java détectées:
| Entité Java | Table SQL | Description |
|-------------|-----------|-------------|
| `Wallet` | `wallets` | Portefeuilles principaux |
| `SubWallet` | `sub_wallets` | Sous-comptes (épargne, business, etc.) |
| `WalletSubWallet` | `wallet_subwallet` | Table de liaison M:N |
| `WalletPermission` | `wallet_permissions` | Permissions par wallet |
| `WalletStatusHistory` | `wallet_status_history` | Historique des changements de statut |

**Relations clés**:
- `Wallet` 1:N `WalletPermission`
- `Wallet` 1:N `WalletStatusHistory`
- `Wallet` M:N `SubWallet` (via `wallet_subwallet`)

---

### 4️⃣ **TRANSACTION-SERVICE** → Database: `zaphira_transactions_db`

**Responsabilité**: Transactions, règlements, remboursements, autorisations

#### Tables détectées (pas encore d'entités @Entity):
| Table SQL | Description |
|-----------|-------------|
| `transactions` | Transactions principales |
| `transaction_states` | États des transactions |
| `transaction_status_history` | Historique des statuts |
| `transaction_settlements` | Règlements |
| `transaction_refunds` | Remboursements |
| `transaction_authorizations` | Autorisations 2FA |
| `transaction_audit_log` | Audit des transactions |
| `validation_requests` | Demandes de validation |
| `scheduled_transactions` | Transactions programmées |

**Relations clés**:
- `transactions` N:1 `wallets` (sender_wallet_id, receiver_wallet_id)
- `transactions` 1:N `transaction_status_history`
- `transactions` 1:1 `transaction_settlements`
- `transactions` 1:N `transaction_refunds`

---

### 5️⃣ **DISPUTE-SERVICE** → Database: `zaphira_disputes_db`

**Responsabilité**: Litiges, preuves, résolutions

#### Tables détectées:
| Table SQL | Description |
|-----------|-------------|
| `disputes` | Litiges |
| `dispute_evidence` | Preuves soumises |
| `dispute_timeline` | Chronologie des événements |

**Relations clés**:
- `disputes` N:1 `transactions` (transaction_id)
- `disputes` 1:N `dispute_evidence`
- `disputes` 1:N `dispute_timeline`

---

### 6️⃣ **REPORTING-SERVICE** → Database: `zaphira_reports_db`

**Responsabilité**: Rapports, analytics, métriques

#### Tables détectées:
| Table SQL | Description |
|-----------|-------------|
| `daily_reports` | Rapports quotidiens globaux |
| `merchant_analytics` | Analytics par merchant |
| `user_analytics` | Analytics par utilisateur |

**Relations clés**:
- `merchant_analytics` N:1 `users` (merchant_id)
- `user_analytics` N:1 `users` (user_id)

---

### 7️⃣ **EXCHANGE-SERVICE** → Database: `zaphira_exchange_db`

**Responsabilité**: Taux de change, conversions de devises

#### Tables détectées:
| Table SQL | Description |
|-----------|-------------|
| `exchange_rates` | Taux de change en temps réel |

---

### 8️⃣ **NOTIFICATION-SERVICE** → Database: `zaphira_notifications_db`

**Responsabilité**: Notifications, OTP, vérifications

#### Entités Java détectées:
| Entité Java | Table SQL | Description |
|-------------|-----------|-------------|
| `OtpCode` | `otp_codes` | Codes OTP (duplicate?) |
| `VerificationToken` | `verification_tokens` | Tokens de vérification email |

⚠️ **Note**: `otp_codes` existe aussi dans user-service → décider de la consolidation

---

## 🔗 Cross-Service Dependencies

### Dépendances identifiées:

```
USER-SERVICE (users)
    ↓
    ├─→ AUTH-SERVICE (tokens, activity_log)
    ├─→ WALLET-SERVICE (wallets via user_id)
    └─→ NOTIFICATION-SERVICE (verification_tokens)

WALLET-SERVICE (wallets)
    ↓
    └─→ TRANSACTION-SERVICE (transactions via wallet_id)

TRANSACTION-SERVICE (transactions)
    ↓
    ├─→ DISPUTE-SERVICE (disputes via transaction_id)
    ├─→ REPORTING-SERVICE (analytics)
    └─→ EXCHANGE-SERVICE (exchange_rates)
```

### ⚠️ Points d'attention:
1. **Foreign Keys Cross-Database**: Non supportées en microservices → utiliser des IDs logiques
2. **Cohérence des données**: Utiliser Saga Pattern ou Event Sourcing
3. **Transactions distribuées**: Implémenter 2PC ou eventual consistency

---

## 📊 Statistiques Globales

| Métrique | Valeur |
|----------|--------|
| **Nombre de microservices** | 8 |
| **Nombre de bases de données** | 8 |
| **Nombre total de tables** | 47 |
| **Entités @Entity détectées** | 22 |
| **Tables sans entité Java** | 25 |

---

## 🚀 Ordre de Création Recommandé

```sql
1. zaphira_users_db     -- Base principale (users)
2. zaphira_auth_db      -- Authentification (référence users)
3. zaphira_wallets_db   -- Wallets (référence users)
4. zaphira_transactions_db  -- Transactions (référence wallets)
5. zaphira_disputes_db  -- Litiges (référence transactions)
6. zaphira_reports_db   -- Analytics (référence users, transactions)
7. zaphira_exchange_db  -- Taux de change (indépendant)
8. zaphira_notifications_db -- Notifications (référence users)
```

---

## 🎨 Diagram ASCII - Architecture Globale

```
┌─────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY (Port 8080)                      │
│                     Load Balancer + Routing                         │
└────────────┬────────────────────────────────────────────────────────┘
             │
             ├───────────────────────────────────────────────┐
             │                                               │
┌────────────▼─────────┐                      ┌─────────────▼───────────┐
│   USER-SERVICE       │◄─────────────────────┤   AUTH-SERVICE          │
│   Port: 8081         │  validate user       │   Port: 8082            │
│                      │                      │                         │
│  DB: zaphira_users   │                      │  DB: zaphira_auth       │
│  ┌─────────────────┐ │                      │  ┌────────────────────┐ │
│  │ users           │ │                      │  │ tokens             │ │
│  │ kyc             │ │                      │  │ refresh_token      │ │
│  │ user_sessions   │ │                      │  │ activity_log       │ │
│  │ otp_codes       │ │                      │  └────────────────────┘ │
│  │ user_analytics  │ │                      │                         │
│  └─────────────────┘ │                      └─────────────────────────┘
└────────────┬─────────┘
             │ user_id
             │
┌────────────▼─────────┐
│  WALLET-SERVICE      │
│  Port: 8083          │
│                      │
│  DB: zaphira_wallets │
│  ┌─────────────────┐ │
│  │ wallets         │ │
│  │ sub_wallets     │ │
│  │ wallet_perms    │ │
│  └─────────────────┘ │
└────────────┬─────────┘
             │ wallet_id
             │
┌────────────▼────────────┐
│  TRANSACTION-SERVICE    │
│  Port: 8084             │
│                         │
│  DB: zaphira_trans...   │
│  ┌────────────────────┐ │
│  │ transactions       │ │
│  │ settlements        │ │
│  │ refunds            │ │
│  └────────────────────┘ │
└────────────┬────────────┘
             │ transaction_id
    ┌────────┴────────┐
    │                 │
┌───▼──────────┐  ┌──▼─────────────┐
│ DISPUTE-SVC  │  │ REPORTING-SVC  │
│ Port: 8085   │  │ Port: 8086     │
└──────────────┘  └────────────────┘

┌──────────────┐  ┌──────────────────┐
│ EXCHANGE-SVC │  │ NOTIFICATION-SVC │
│ Port: 8087   │  │ Port: 8088       │
└──────────────┘  └──────────────────┘
```

---

## 🔧 Next Steps

1. ✅ Exécuter `01_create_databases.sql` pour créer les bases et utilisateurs
2. ✅ Exécuter `02_user_service_schema.sql` pour USER-SERVICE
3. ✅ Exécuter `03_auth_service_schema.sql` pour AUTH-SERVICE
4. ✅ Exécuter `04_wallet_service_schema.sql` pour WALLET-SERVICE
5. ✅ Exécuter `05_transaction_service_schema.sql` pour TRANSACTION-SERVICE
6. ✅ Exécuter `06_dispute_service_schema.sql` pour DISPUTE-SERVICE
7. ✅ Exécuter `07_reporting_service_schema.sql` pour REPORTING-SERVICE
8. ✅ Exécuter `08_exchange_service_schema.sql` pour EXCHANGE-SERVICE
9. ✅ Exécuter `09_notification_service_schema.sql` pour NOTIFICATION-SERVICE

---

**Fin du document d'analyse** 🎉
