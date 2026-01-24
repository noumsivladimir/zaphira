# 🗄️ MICROSERVICES DATABASE ARCHITECTURE

**Project**: Zaphira Platform - Database Decomposition  
**Date**: January 21, 2026  
**Approach**: Database-per-Service Pattern  
**Total Tables**: 42 tables across 8 microservices  

---

## 📊 ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────┐
│                     MICROSERVICES ECOSYSTEM                      │
└─────────────────────────────────────────────────────────────────┘
                                 │
                ┌────────────────┴────────────────┐
                │                                  │
        ┌───────▼────────┐              ┌─────────▼────────┐
        │  user-service  │              │  auth-service    │
        │  users_db      │              │  auth_db         │
        │  Port: 8082    │◄─────────────│  Port: 8081      │
        │  5 tables      │   validates  │  5 tables        │
        └───────┬────────┘              └──────────────────┘
                │ creates                         │
                │ wallet                          │ generates
                │                                 │ tokens
        ┌───────▼────────┐              ┌─────────▼────────┐
        │ wallet-service │              │  kyc-service     │
        │  wallets_db    │              │  kyc_db          │
        │  Port: 8083    │              │  Port: 8086      │
        │  5 tables      │              │  1 table         │
        └───────┬────────┘              └──────────────────┘
                │ sends to
                │ receiver
        ┌───────▼────────┐              ┌──────────────────┐
        │ transaction-   │              │ dispute-service  │
        │    service     │◄─────────────│  disputes_db     │
        │ transactions_db│   disputes   │  Port: 8087      │
        │  Port: 8084    │   on txn     │  3 tables        │
        │  9 tables      │              └──────────────────┘
        └───────┬────────┘
                │ reports
        ┌───────▼────────┐              ┌──────────────────┐
        │ reporting-     │              │ exchange-service │
        │    service     │              │  exchange_db     │
        │ reporting_db   │              │  Port: 8088      │
        │  Port: 8085    │              │  1 table         │
        │  2 tables      │              └──────────────────┘
        └────────────────┘
                │
        ┌───────▼────────┐
        │ activity-log-  │
        │    service     │
        │ activity_db    │
        │  Port: 8090    │
        │  1 table       │
        └────────────────┘
```

---

## 🎯 MICROSERVICE DATABASE MAPPING

### 1️⃣ **auth-service** → `auth_db`

**Purpose**: Authentication, authorization, token management, OTP verification  
**Database**: `auth_db`  
**User**: `auth_user`  
**Port**: 8081  

**Tables** (5):
```sql
✅ tokens              -- JWT access tokens
✅ refresh_token       -- JWT refresh tokens
✅ otp_codes          -- OTP codes for verification
✅ otp_tokens         -- Alternative OTP token storage
✅ verification_tokens -- Email verification tokens
```

**Key Columns**:
- `tokens`: id, token, user_id, expired, revoked
- `refresh_token`: id, token, user_id, expiry_date, revoked
- `otp_codes`: id, code, otp_hash, phone_number, email, purpose, expires_at, attempts, consumed
- `verification_tokens`: id, code, user_id, expires_at, used

**Dependencies**:
- ⬅️ **Reads** from `user-service`: Validates user credentials during login
- ➡️ **Called by**: All services for token validation

---

### 2️⃣ **user-service** → `users_db`

**Purpose**: User profile, security questions, sessions, user analytics  
**Database**: `users_db`  
**User**: `user_user`  
**Port**: 8082  

**Tables** (6):
```sql
✅ users                        -- Core user data
✅ user_sessions               -- Active user sessions
✅ user_security_answers       -- User security question answers
✅ user_analytics              -- User analytics data
✅ security_question           -- Security questions (deprecated)
✅ predefined_security_questions -- Predefined security questions
```

**Key Columns**:
- `users`: user_id (PK), email, phone_number, first_name, last_name, pin, wallet_id, role, kyc_verified_at
- `user_sessions`: session_id (PK), user_id (FK), token, refresh_token, ip_address, device_type
- `user_security_answers`: id (PK), user_id (FK), question_id (FK), answer_hash
- `user_analytics`: id (PK), user_id (FK), transaction_count, transaction_volume, risk_score

**Dependencies**:
- ➡️ **Creates**: `wallet-service` (wallet creation during user registration)
- ➡️ **Triggers**: `kyc-service` (KYC verification)
- ➡️ **Triggers**: `auth-service` (token generation)

---

### 3️⃣ **wallet-service** → `wallets_db`

**Purpose**: Wallet management, sub-wallets, permissions, balance tracking  
**Database**: `wallets_db`  
**User**: `wallet_user`  
**Port**: 8083  

**Tables** (5):
```sql
✅ wallets               -- Main wallet records
✅ sub_wallets          -- Sub-wallets for categorization
✅ wallet_permissions   -- Wallet operation permissions
✅ wallet_status_history -- Wallet status change audit
✅ wallet_subwallet     -- Wallet-to-subwallet relationship
```

**Key Columns**:
- `wallets`: id (PK), wallet_number (UNIQUE), user_id (FK), total_balance, available_balance, blocked_balance, status, currency
- `sub_wallets`: id (PK), sub_wallet_name, total_balance, available_balance, currency, is_default
- `wallet_permissions`: id (PK), wallet_id (FK), permission_type, max_amount, daily_limit, requires_approval
- `wallet_status_history`: id (PK), wallet_id (FK), previous_status, new_status, changed_by, reason

**Dependencies**:
- ⬅️ **Created by**: `user-service` (user_id must exist)
- ➡️ **Used by**: `transaction-service` (wallet balance operations)
- ⬅️ **Validated by**: `kyc-service` (KYC status check before transactions)

---

### 4️⃣ **transaction-service** → `transactions_db`

**Purpose**: Transaction processing, authorization, settlements, refunds  
**Database**: `transactions_db`  
**User**: `transaction_user`  
**Port**: 8084  

**Tables** (9):
```sql
✅ transactions                 -- Core transaction records
✅ transaction_authorizations   -- Transaction authorization records
✅ transaction_audit_log        -- Transaction audit trail
✅ transaction_refunds          -- Refund transactions
✅ transaction_settlements      -- Settlement processing
✅ transaction_states           -- Transaction state machine
✅ transaction_status_history   -- Transaction status changes
✅ scheduled_transactions       -- Scheduled/recurring transactions
✅ validation_requests          -- Transaction validation requests
```

**Key Columns**:
- `transactions`: id (PK), sender_wallet_id (FK), receiver_wallet_id (FK), user_id (FK), amount, currency, status, type, transaction_reference
- `transaction_authorizations`: id (PK), transaction_id (FK), method, challenge_code, status, approved_by
- `transaction_refunds`: id (PK), original_transaction_id (FK), refund_transaction_id (FK), refund_amount, reason
- `scheduled_transactions`: id (PK), sender_wallet_number, receiver_wallet_number, amount, scheduled_for, status

**Dependencies**:
- ⬅️ **Requires**: `wallet-service` (sender/receiver wallets must exist)
- ⬅️ **Requires**: `user-service` (user_id must exist)
- ➡️ **Triggers**: `dispute-service` (disputes on transactions)
- ➡️ **Reports to**: `reporting-service` (transaction metrics)
- ➡️ **Logs to**: `activity-log-service` (transaction activities)

---

### 5️⃣ **dispute-service** → `disputes_db`

**Purpose**: Transaction disputes, evidence management, dispute resolution  
**Database**: `disputes_db`  
**User**: `dispute_user`  
**Port**: 8087  

**Tables** (3):
```sql
✅ disputes           -- Dispute records
✅ dispute_evidence   -- Uploaded evidence files
✅ dispute_timeline   -- Dispute event timeline
```

**Key Columns**:
- `disputes`: id (PK), transaction_id (FK), initiated_by, initiator_role, category, claimed_amount, status, resolution_type
- `dispute_evidence`: id (PK), dispute_id (FK), evidence_type, file_url, submitted_by, verified, verified_by
- `dispute_timeline`: id (PK), dispute_id (FK), event_type, event_description, old_status, new_status, actor

**Dependencies**:
- ⬅️ **Requires**: `transaction-service` (transaction_id must exist)
- ⬅️ **Validates**: `user-service` (initiated_by user must exist)

---

### 6️⃣ **kyc-service** → `kyc_db`

**Purpose**: Know Your Customer verification, document management  
**Database**: `kyc_db`  
**User**: `kyc_user`  
**Port**: 8086  

**Tables** (1):
```sql
✅ kyc -- KYC verification records
```

**Key Columns**:
- `kyc`: kyc_id (PK), user_id (FK), document_type, document_number, document_front_image, document_back_image, selfie_image, kyc_status, verified_by, verified_at

**Dependencies**:
- ⬅️ **Linked to**: `user-service` (user_id must exist)
- ➡️ **Updates**: `user-service` (sets kyc_verified_at on users table)

---

### 7️⃣ **reporting-service** → `reporting_db`

**Purpose**: Daily reports, merchant analytics, business intelligence  
**Database**: `reporting_db`  
**User**: `reporting_user`  
**Port**: 8085  

**Tables** (2):
```sql
✅ daily_reports       -- Daily transaction/settlement reports
✅ merchant_analytics  -- Merchant performance analytics
```

**Key Columns**:
- `daily_reports`: id (PK), report_date, total_transactions, total_transaction_volume, successful_transactions, failed_transactions, success_rate
- `merchant_analytics`: id (PK), merchant_id, analytics_date, total_transaction_count, total_volume, transaction_success_rate, risk_score

**Dependencies**:
- ⬅️ **Aggregates from**: `transaction-service` (transaction data)
- ⬅️ **Aggregates from**: `dispute-service` (dispute data)
- ⬅️ **Aggregates from**: `wallet-service` (merchant wallet data)

---

### 8️⃣ **exchange-service** → `exchange_db`

**Purpose**: Currency exchange rates, FX operations  
**Database**: `exchange_db`  
**User**: `exchange_user`  
**Port**: 8088  

**Tables** (1):
```sql
✅ exchange_rates -- Real-time exchange rates
```

**Key Columns**:
- `exchange_rates`: id (PK), source_currency, target_currency, rate, bid, ask, provider, created_at, expires_at

**Dependencies**:
- ➡️ **Used by**: `transaction-service` (currency conversion in transactions)
- ➡️ **Used by**: `wallet-service` (multi-currency wallet operations)

---

### 9️⃣ **activity-log-service** → `activity_db`

**Purpose**: Cross-service activity logging, audit trail  
**Database**: `activity_db`  
**User**: `activity_user`  
**Port**: 8090  

**Tables** (1):
```sql
✅ activity_log -- User activity logs
```

**Key Columns**:
- `activity_log`: id (PK), user_id (FK), action, timestamp, created_at

**Dependencies**:
- ⬅️ **Receives from**: All services (activity events via Kafka/async messaging)

---

## 🔗 SERVICE DEPENDENCY MATRIX

| Service | Depends On | Depended By | Communication |
|---------|-----------|-------------|---------------|
| **user-service** | auth-service | wallet-service, kyc-service, transaction-service | Sync (Feign) |
| **auth-service** | user-service | All services (JWT validation) | Sync (Feign) |
| **wallet-service** | user-service, kyc-service | transaction-service | Sync (Feign) |
| **transaction-service** | wallet-service, user-service, exchange-service | dispute-service, reporting-service | Sync + Async (Kafka) |
| **dispute-service** | transaction-service, user-service | reporting-service | Sync (Feign) |
| **kyc-service** | user-service | wallet-service | Sync (Feign) |
| **reporting-service** | transaction-service, dispute-service, wallet-service | None (read-only) | Async (Kafka) |
| **exchange-service** | External APIs | transaction-service, wallet-service | Sync (Feign) |
| **activity-log-service** | All services | None (write-only) | Async (Kafka) |

---

## 📋 TABLE DISTRIBUTION SUMMARY

| Microservice | Database | User | Tables | Total Columns | Critical Tables |
|--------------|----------|------|--------|---------------|-----------------|
| **auth-service** | auth_db | auth_user | 5 | ~30 | tokens, refresh_token |
| **user-service** | users_db | user_user | 6 | ~80 | users, user_sessions |
| **wallet-service** | wallets_db | wallet_user | 5 | ~90 | wallets, sub_wallets |
| **transaction-service** | transactions_db | transaction_user | 9 | ~180 | transactions, transaction_settlements |
| **dispute-service** | disputes_db | dispute_user | 3 | ~50 | disputes, dispute_evidence |
| **kyc-service** | kyc_db | kyc_user | 1 | ~25 | kyc |
| **reporting-service** | reporting_db | reporting_user | 2 | ~60 | daily_reports, merchant_analytics |
| **exchange-service** | exchange_db | exchange_user | 1 | ~10 | exchange_rates |
| **activity-log-service** | activity_db | activity_user | 1 | ~6 | activity_log |
| **TOTAL** | **9 databases** | **9 users** | **42 tables** | **~531 columns** | **15 critical** |

---

## 🔐 SECURITY & ACCESS CONTROL

### Database User Privileges

Each microservice has:
- ✅ **Dedicated database user** with restricted access
- ✅ **Full CRUD permissions** on its own tables
- ✅ **Read-only access** to specific tables in other databases (via DB links or API)
- ✅ **No direct access** to other service databases (enforced via network policies)

### Cross-Service Communication

```
┌─────────────────────────────────────────────────────────┐
│              SYNCHRONOUS COMMUNICATION                   │
│  user-service ←─Feign─→ wallet-service                  │
│  wallet-service ←─Feign─→ transaction-service           │
│  transaction-service ←─Feign─→ exchange-service         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│             ASYNCHRONOUS COMMUNICATION                   │
│  transaction-service ──Kafka──→ reporting-service       │
│  All services ──Kafka──→ activity-log-service           │
└─────────────────────────────────────────────────────────┘
```

---

## ⚠️ CRITICAL DEPENDENCIES TO MANAGE

### 1. User → Wallet Creation
```
User Registration Flow:
1. POST /api/users/register (user-service)
2. Insert into users table
3. Feign call to wallet-service: POST /api/wallets
4. Insert into wallets table with user_id
5. Update users.wallet_id with created wallet_id
```

### 2. Wallet → Transaction Processing
```
Transaction Flow:
1. POST /api/transactions (transaction-service)
2. Validate sender wallet exists (Feign to wallet-service)
3. Validate receiver wallet exists (Feign to wallet-service)
4. Check sender balance (Feign to wallet-service)
5. Insert transaction record
6. Debit sender (Feign to wallet-service: POST /internal/debit)
7. Credit receiver (Feign to wallet-service: POST /internal/credit)
```

### 3. Transaction → Dispute Creation
```
Dispute Flow:
1. POST /api/disputes (dispute-service)
2. Validate transaction exists (Feign to transaction-service)
3. Validate user is transaction participant (Feign to transaction-service)
4. Insert dispute record
5. Update transaction status (Feign to transaction-service)
```

### 4. KYC → Wallet Limits
```
KYC Verification Flow:
1. POST /api/kyc/submit (kyc-service)
2. Admin approves KYC
3. Update kyc.kyc_status = 'VERIFIED'
4. Trigger event to user-service: Update users.kyc_verified_at
5. Trigger event to wallet-service: Upgrade wallet limits
```

---

## 🚀 DEPLOYMENT STRATEGY

### Phase 1: Database Creation (Week 1)
- Create 9 dedicated databases
- Create 9 database users with appropriate privileges
- Set up connection pooling configurations
- Configure database backups per service

### Phase 2: Schema Migration (Week 2)
- Export existing tables from monolithic `wallet_db`
- Import tables into respective microservice databases
- Validate data integrity and foreign key relationships
- Set up database replication for critical services

### Phase 3: Service Configuration (Week 3)
- Update Spring Boot `application.yml` with new database URLs
- Configure Feign clients for cross-service communication
- Implement database connection retry logic
- Set up health checks per database

### Phase 4: Testing & Validation (Week 4)
- Integration testing for cross-service transactions
- Load testing for database connection pools
- Failover testing for database replication
- Performance benchmarking

### Phase 5: Production Migration (Week 5)
- Blue-green deployment with database cutover
- Monitor database performance metrics
- Rollback plan with database restore points
- Post-migration validation

---

## 📊 PERFORMANCE CONSIDERATIONS

### Connection Pooling
```yaml
# Recommended HikariCP settings per service
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Database Sizing Estimates

| Database | Expected Growth | Initial Size | Backup Frequency |
|----------|----------------|--------------|------------------|
| transactions_db | HIGH (1M rows/month) | 50 GB | Every 6 hours |
| wallets_db | MEDIUM (100K rows/month) | 10 GB | Daily |
| users_db | MEDIUM (50K rows/month) | 5 GB | Daily |
| disputes_db | LOW (10K rows/month) | 2 GB | Daily |
| auth_db | MEDIUM (sessions grow rapidly) | 5 GB | Daily |
| kyc_db | LOW (documents are large) | 20 GB | Daily |
| reporting_db | MEDIUM (aggregated data) | 10 GB | Daily |
| exchange_db | LOW (rate updates) | 100 MB | Daily |
| activity_db | HIGH (all activities logged) | 30 GB | Daily |

---

## ✅ NEXT STEPS

1. **Review** this architecture with the team
2. **Execute** SQL scripts (see dedicated files)
3. **Migrate** tables from monolithic database
4. **Update** service configurations
5. **Test** cross-service communication
6. **Deploy** to production

**SQL Scripts Generated**:
- `01_create_databases.sql` - Database creation
- `02_create_users.sql` - User and privilege setup
- `03_auth_schema.sql` - Auth service schema
- `04_user_schema.sql` - User service schema
- `05_wallet_schema.sql` - Wallet service schema
- `06_transaction_schema.sql` - Transaction service schema
- `07_dispute_schema.sql` - Dispute service schema
- `08_kyc_schema.sql` - KYC service schema
- `09_reporting_schema.sql` - Reporting service schema
- `10_exchange_schema.sql` - Exchange service schema
- `11_activity_schema.sql` - Activity log service schema
- `12_migration_validation.sql` - Data validation queries

---

**Document Version**: 1.0  
**Last Updated**: January 21, 2026  
**Status**: Ready for Implementation  
**Approval Required**: DBA, Backend Team Lead, DevOps Lead
