# 🗺️ ZAPHIRA Platform - Complete Database Mapping

## 📊 Visual Entity Relationship Mapping

---

## 🔵 1. USER-SERVICE → Database: `zaphira_users_db`

### Tables Hierarchy

```
users (MAIN TABLE - Single Table Inheritance)
│
├─► user_type discriminator:
│   ├─ 'REGULAR'  → RegularUser entity
│   ├─ 'MERCHANT' → MerchantUser entity
│   └─ 'ADMIN'    → AdminUser entity
│
├─[1:1]─► kyc
│        └─ Stores KYC verification documents
│
├─[1:N]─► user_sessions
│        └─ Active JWT sessions with device info
│
├─[1:N]─► user_security_answers
│        └─ Security questions for account recovery
│
└─[1:N]─► user_analytics
         └─ Daily user metrics and statistics

otp_codes (standalone)
├─ OTP verification codes
└─ References user_id

otp_tokens (standalone)
└─ Alternative OTP storage

predefined_security_questions (standalone)
└─ Predefined questions catalog

security_question (standalone)
└─ Custom user questions
```

### Key Fields in `users` Table
```
Common Fields (All User Types):
- user_id (PK)
- user_type (discriminator)
- email, phone_number
- pin, pin_changed_at
- first_name, last_name, date_of_birth
- country, region, city
- account_status, email_verified, phone_verified
- wallet_id (logical reference to wallet-service)

ADMIN-specific Fields:
- admin_level, employee_id, supervisor_id
- can_manage_users, can_manage_roles, can_managekyc
- can_approve_transactions, can_view_audit_logs

MERCHANT-specific Fields:
- business_name, business_registration_number
- is_verified_merchant, merchant_verified_at
- can_accept_payments, commission_rate
- total_transactions, total_transaction_volume
```

---

## 🟢 2. AUTH-SERVICE → Database: `zaphira_auth_db`

### Tables Structure

```
tokens
├─ JWT access tokens
├─ expired, revoked flags
└─ References user_id (from users table)

refresh_token
├─ Long-lived refresh tokens
├─ expiry_date, revoked flag
└─ References user_id

activity_log
├─ Complete audit trail
├─ action, timestamp, ip_address
└─ References user_id
```

### Cross-Service Reference
```
auth_db.tokens.user_id ───► users_db.users.user_id (logical reference)
auth_db.refresh_token.user_id ───► users_db.users.user_id (logical reference)
auth_db.activity_log.user_id ───► users_db.users.user_id (logical reference)
```

---

## 🟡 3. WALLET-SERVICE → Database: `zaphira_wallets_db`

### Tables Structure

```
wallets (MAIN)
│
├─[N:1]─► users (user_id - logical reference)
│
├─[1:N]─► wallet_permissions
│        └─ Granular permissions (SEND, RECEIVE, etc.)
│
├─[1:N]─► wallet_status_history
│        └─ Audit trail of status changes
│
└─[M:N]─► sub_wallets
         └─ via wallet_subwallet junction table

sub_wallets
├─ Independent sub-accounts
├─ Types: CHECKING, SAVINGS, BUSINESS, INVESTMENT, ESCROW
└─ Can be shared across multiple wallets

wallet_subwallet (junction)
└─ Links wallets to sub_wallets (M:N)
```

### Key Fields in `wallets` Table
```
Financial Fields:
- available_balance
- blocked_balance
- total_balance (calculated: available + blocked)
- daily_limit, monthly_limit
- daily_spent, monthly_spent

Status Fields:
- type: PERSONAL, BUSINESS, MERCHANT, SAVINGS, AGENT
- status: ACTIVE, INACTIVE, SUSPENDED, FROZEN, CLOSED
- frozen_reason, frozen_at, frozen_by

Merchant Fields:
- merchant_name, merchant_code

Optimistic Locking:
- version (for concurrency control)
```

### Cross-Service Reference
```
wallets_db.wallets.user_id ───► users_db.users.user_id (logical reference)
```

---

## 🔴 4. TRANSACTION-SERVICE → Database: `zaphira_transactions_db`

### Tables Structure

```
transactions (MAIN)
│
├─ References wallets (sender_wallet_id, receiver_wallet_id)
├─ References users (user_id)
│
├─[1:N]─► transaction_status_history
│        └─ Status change audit trail
│
├─[1:N]─► transaction_states
│        └─ Alternative status tracking
│
├─[1:1]─► transaction_authorizations
│        └─ 2FA/PIN authorization
│
├─[1:1]─► transaction_settlements
│        └─ Settlement processing
│
├─[1:N]─► transaction_refunds
│        └─ Refund records
│
├─[1:N]─► transaction_audit_log
│        └─ Detailed audit trail
│
└─[1:1]─► validation_requests
         └─ Validation workflows

scheduled_transactions (standalone)
└─ Scheduled/recurring transactions
```

### Transaction Lifecycle
```
PENDING → PROCESSING → COMPLETED
   ↓          ↓            ↓
FAILED   ON_HOLD    REVERSED/REFUNDED
   ↓    UNDER_REVIEW
CANCELLED  EXPIRED
```

### Key Fields in `transactions` Table
```
Transaction Details:
- sender_wallet_number, receiver_wallet_number
- amount, currency
- fee, service_fee, fx_fee
- total_amount, net_amount
- type: TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT, REFUND, REVERSAL

Status Tracking:
- status, created_at, initiated_at
- pending_at, processing_at, authorized_at
- completed_at, failed_at, cancelled_at

Authorization:
- authorization_method: PIN, OTP, BIOMETRIC, TWO_FACTOR
- authorization_required, authorization_level

Risk & Routing:
- risk_score, compliance_status
- channel: WEB, MOBILE, API, USSD
- route, device_info, ip_address
```

### Cross-Service References
```
transactions_db.transactions.sender_wallet_id ───► wallets_db.wallets.id (logical)
transactions_db.transactions.receiver_wallet_id ───► wallets_db.wallets.id (logical)
transactions_db.transactions.user_id ───► users_db.users.user_id (logical)
```

---

## 🟣 5. DISPUTE-SERVICE → Database: `zaphira_disputes_db`

### Tables Structure

```
disputes (MAIN)
│
├─ References transactions (transaction_id)
├─ References users (initiated_by)
│
├─[1:N]─► dispute_evidence
│        └─ Uploaded evidence files
│
└─[1:N]─► dispute_timeline
         └─ Complete event history
```

### Dispute Workflow
```
OPEN → PENDING → UNDER_REVIEW → ESCALATED
                      ↓              ↓
                  RESOLVED ──────► CLOSED
                      ↓
                  WITHDRAWN
                  REJECTED
```

### Key Fields in `disputes` Table
```
Dispute Details:
- transaction_id (reference to transaction)
- initiated_by (user_id who opened dispute)
- initiator_role: CUSTOMER, MERCHANT, ADMIN, SYSTEM
- category: UNAUTHORIZED_TRANSACTION, SERVICE_NOT_RECEIVED, etc.

Financial Details:
- claimed_amount, claimed_currency
- resolution_amount, resolution_type

Resolution:
- resolution_type: FULL_REFUND, PARTIAL_REFUND, NO_REFUND, etc.
- resolved_at, resolved_by

Evidence Tracking:
- evidence_submitted_count
- last_evidence_submitted_at
```

### Cross-Service References
```
disputes_db.disputes.transaction_id ───► transactions_db.transactions.id (logical)
disputes_db.disputes.initiated_by ───► users_db.users.user_id (logical)
```

---

## 🟠 6. REPORTING-SERVICE → Database: `zaphira_reports_db`

### Tables Structure

```
daily_reports (Platform-wide)
└─ Aggregated daily metrics for entire platform

merchant_analytics (Per Merchant)
├─ References merchant_id from users
└─ Daily merchant performance metrics

user_analytics (Per User)
├─ References user_id from users
└─ Daily user activity metrics

monthly_reports (Aggregated)
└─ Monthly summary of platform metrics

currency_daily_stats (Per Currency)
└─ Daily breakdown by currency
```

### Metrics Captured

#### Platform Metrics (daily_reports)
```
Transaction Metrics:
- total_transactions, successful_transactions
- failed_transactions, pending_transactions
- total_transaction_volume

Financial Metrics:
- total_fees_collected
- service_fees_collected, fx_fees_collected
- average_fee_percentage

Settlement Metrics:
- total_settlements, completed_settlements
- failed_settlements, total_settlement_volume

Dispute Metrics:
- total_disputes, new_disputes, resolved_disputes
- total_disputed_amount, dispute_rate

Rate Metrics:
- success_rate, settlement_success_rate
- refund_rate, reversal_rate
```

#### Merchant Metrics (merchant_analytics)
```
- total_transaction_count, total_volume
- transaction_success_rate
- gross_revenue, net_revenue
- settlement_success_rate
- dispute_rate, chargeback_rate
- risk_score, compliance_status
```

### Cross-Service References
```
reports_db.merchant_analytics.merchant_id ───► users_db.users.user_id (logical)
reports_db.user_analytics.user_id ───► users_db.users.user_id (logical)
```

---

## 🟤 7. EXCHANGE-SERVICE → Database: `zaphira_exchange_db`

### Tables Structure

```
exchange_rates (MAIN)
├─ Real-time exchange rates
├─ source_currency, target_currency
├─ rate, bid, ask
└─ expires_at (TTL)

exchange_rate_history
└─ Historical archive of rates

currency_pairs
├─ Supported currency pair configuration
├─ Trading limits, fees
└─ Update frequency

conversion_logs
├─ Audit log of all conversions
└─ References transaction_id, user_id
```

### Supported Currency Pairs (Default)
```
XAF ↔ USD
XAF ↔ EUR
USD ↔ EUR
GBP ↔ USD
GBP ↔ EUR
... (extendable)
```

### Key Fields in `exchange_rates` Table
```
Rate Information:
- source_currency, target_currency
- rate (mid-market rate)
- bid (buying rate)
- ask (selling rate)

Provider:
- provider: INTERNAL, CENTRAL_BANK, FOREX_API, etc.
- source (API endpoint)

Validity:
- created_at, updated_at
- expires_at (TTL for rate validity)
```

---

## 🔵 8. NOTIFICATION-SERVICE → Database: `zaphira_notifications_db`

### Tables Structure

```
notifications (MAIN)
├─ Notification queue and history
├─ type: EMAIL, SMS, PUSH, IN_APP, WEBHOOK
└─ status: PENDING, SENT, DELIVERED, FAILED

notification_templates
└─ Reusable message templates

otp_codes
├─ OTP codes with delivery tracking
└─ References notification_id

verification_tokens
└─ Email/phone verification tokens

email_logs
├─ Detailed email delivery logs
└─ References notification_id

sms_logs
├─ Detailed SMS delivery logs
└─ References notification_id

notification_preferences
├─ User-specific preferences
└─ References user_id
```

### Notification Flow
```
PENDING → SENT → DELIVERED → OPENED → CLICKED
   ↓        ↓
FAILED  BOUNCED
```

### Key Fields in `notifications` Table
```
Notification Details:
- user_id (recipient)
- recipient_email, recipient_phone
- type, status
- subject, body

Template:
- template_id, template_name
- template_variables (JSON)

Delivery Tracking:
- sent_at, delivered_at
- opened_at, clicked_at
- failed_at

Provider:
- provider (Twilio, SendGrid, etc.)
- provider_message_id
- provider_response

Priority:
- priority (1-10)
- scheduled_for
```

### Cross-Service References
```
notifications_db.notifications.user_id ───► users_db.users.user_id (logical)
notifications_db.notification_preferences.user_id ───► users_db.users.user_id (logical)
```

---

## 🌐 Complete Cross-Service Data Flow

```
┌─────────────┐
│   USERS     │ (Foundation)
│  user_id    │
└──────┬──────┘
       │
       ├──────────► AUTH (tokens, activity_log)
       │
       ├──────────► WALLETS (user_id → wallets)
       │                │
       │                └──────► TRANSACTIONS (wallet_id → transactions)
       │                              │
       │                              ├──────► DISPUTES (transaction_id)
       │                              │
       │                              └──────► REPORTING (analytics)
       │
       ├──────────► NOTIFICATIONS (user_id → notifications)
       │
       └──────────► REPORTING (user_analytics)

EXCHANGE (Independent)
└──────► Used by TRANSACTIONS for fx_rate
```

---

## 📈 Total Statistics

| Metric | Count |
|--------|-------|
| **Total Databases** | 8 |
| **Total Tables** | 49 |
| **Java @Entity Classes Found** | 22 |
| **Tables Without Entity (Future)** | 27 |
| **Total Indexes** | 180+ |
| **Total Foreign Keys** | 35 (within same DB only) |
| **Total Check Constraints** | 60+ |
| **Total Triggers** | 20+ |
| **Total Functions** | 25+ |
| **Total Views** | 30+ |

---

## 🎯 Key Design Principles Applied

1. **Database per Microservice**: Complete isolation
2. **No Cross-Database Foreign Keys**: Logical references only
3. **Single Table Inheritance**: For User entity hierarchy
4. **Optimistic Locking**: Version fields for concurrency
5. **Audit Trails**: Status history tables for important entities
6. **Soft Deletes**: Where appropriate (flags instead of DELETE)
7. **Timestamps**: created_at, updated_at on all main tables
8. **Indexes**: Strategic indexing for performance
9. **Constraints**: Data validation at DB level
10. **Views**: Pre-computed queries for common use cases

---

## 🔗 Logical vs Physical Relationships

### Physical Foreign Keys (Within Same Database)
```sql
-- EXAMPLE: User Service
kyc.user_id → users.user_id (FK constraint exists)
user_sessions.user_id → users.user_id (FK constraint exists)
user_security_answers.user_id → users.user_id (FK constraint exists)

-- EXAMPLE: Wallet Service
wallet_permissions.wallet_id → wallets.id (FK constraint exists)
wallet_status_history.wallet_id → wallets.id (FK constraint exists)
```

### Logical References (Cross-Database)
```sql
-- THESE DO NOT HAVE FK CONSTRAINTS - Maintained by application logic
wallets.user_id ──► users.user_id (no FK)
transactions.sender_wallet_id ──► wallets.id (no FK)
disputes.transaction_id ──► transactions.id (no FK)
```

---

## ✅ Implementation Checklist

- [x] Database creation scripts
- [x] Schema creation scripts  
- [x] Indexes for all foreign keys and query patterns
- [x] Check constraints for data validation
- [x] Triggers for automatic updates
- [x] Utility functions for common operations
- [x] Views for frequent queries
- [x] Initial seed data
- [x] Comments and documentation
- [x] Migration scripts
- [x] Rollback procedures

---

**Document Version**: 1.0  
**Last Updated**: 21 January 2026  
**Status**: Production Ready ✅

---

## 📝 Notes for Developers

1. **Entity Creation**: For tables without @Entity classes, create JPA entities in respective microservices
2. **Flyway/Liquibase**: Consider using migration tools for version control
3. **Testing**: Write integration tests to verify cross-service data integrity
4. **Monitoring**: Set up database monitoring and alerting
5. **Backups**: Implement automated backup strategy
6. **Documentation**: Keep this mapping updated as schema evolves

---

**End of Database Mapping Document** 🎉
