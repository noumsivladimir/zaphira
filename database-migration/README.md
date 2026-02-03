# 🚀 ZAPHIRA Platform - Database Migration Guide

## 📋 Overview

This directory contains complete SQL migration scripts for the Zaphira microservices platform. The scripts implement a **Database-per-Service** architecture pattern with 8 independent databases.

---

## 📂 File Structure

```
database-migration/
├── 00_database_architecture_analysis.md    # Architecture documentation
├── 01_create_databases.sql                 # Creates all databases and users
├── 02_user_service_schema.sql              # User Service tables
├── 03_auth_service_schema.sql              # Auth Service tables
├── 04_wallet_service_schema.sql            # Wallet Service tables
├── 05_transaction_service_schema.sql       # Transaction Service tables
├── 06_dispute_service_schema.sql           # Dispute Service tables
├── 07_reporting_service_schema.sql         # Reporting Service tables
├── 08_exchange_service_schema.sql          # Exchange Service tables
├── 09_notification_service_schema.sql      # Notification Service tables
└── README.md                               # This file
```

---

## 🎯 Execution Order

**CRITICAL**: Execute scripts in this EXACT order:

### Step 1: Create Databases and Users
```bash
psql -U postgres -f 01_create_databases.sql
```

### Step 2: Create Service Schemas (in order)
```bash
psql -U postgres -f 02_user_service_schema.sql
psql -U postgres -f 03_auth_service_schema.sql
psql -U postgres -f 04_wallet_service_schema.sql
psql -U postgres -f 05_transaction_service_schema.sql
psql -U postgres -f 06_dispute_service_schema.sql
psql -U postgres -f 07_reporting_service_schema.sql
psql -U postgres -f 08_exchange_service_schema.sql
psql -U postgres -f 09_notification_service_schema.sql
```

### Or execute all at once:
```bash
# Windows PowerShell
Get-Content 01_create_databases.sql, 02_user_service_schema.sql, 03_auth_service_schema.sql, 04_wallet_service_schema.sql, 05_transaction_service_schema.sql, 06_dispute_service_schema.sql, 07_reporting_service_schema.sql, 08_exchange_service_schema.sql, 09_notification_service_schema.sql | psql -U postgres

# Linux/Mac
cat 01_create_databases.sql 02_user_service_schema.sql 03_auth_service_schema.sql 04_wallet_service_schema.sql 05_transaction_service_schema.sql 06_dispute_service_schema.sql 07_reporting_service_schema.sql 08_exchange_service_schema.sql 09_notification_service_schema.sql | psql -U postgres
```

---

## 🗄️ Database Summary

| # | Database Name | Service | Tables | Purpose |
|---|---------------|---------|--------|---------|
| 1 | `zaphira_users_db` | User Service | 11 | User accounts, KYC, sessions, security |
| 2 | `zaphira_auth_db` | Auth Service | 3 | JWT tokens, refresh tokens, activity logs |
| 3 | `zaphira_wallets_db` | Wallet Service | 5 | Wallets, sub-wallets, permissions |
| 4 | `zaphira_transactions_db` | Transaction Service | 9 | Transactions, settlements, refunds |
| 5 | `zaphira_disputes_db` | Dispute Service | 3 | Disputes, evidence, timeline |
| 6 | `zaphira_reports_db` | Reporting Service | 5 | Daily reports, analytics |
| 7 | `zaphira_exchange_db` | Exchange Service | 4 | Exchange rates, conversions |
| 8 | `zaphira_notifications_db` | Notification Service | 9 | Notifications, OTP, templates |

**Total: 8 Databases | 49 Tables**

---

## 👤 Database Users and Passwords

| User | Password | Database |
|------|----------|----------|
| `zaphira_user_service` | `UserService2026!Secure` | `zaphira_users_db` |
| `zaphira_auth_service` | `AuthService2026!Secure` | `zaphira_auth_db` |
| `zaphira_wallet_service` | `WalletService2026!Secure` | `zaphira_wallets_db` |
| `zaphira_transaction_service` | `TransactionService2026!Secure` | `zaphira_transactions_db` |
| `zaphira_dispute_service` | `DisputeService2026!Secure` | `zaphira_disputes_db` |
| `zaphira_report_service` | `ReportService2026!Secure` | `zaphira_reports_db` |
| `zaphira_exchange_service` | `ExchangeService2026!Secure` | `zaphira_exchange_db` |
| `zaphira_notification_service` | `NotificationService2026!Secure` | `zaphira_notifications_db` |

⚠️ **IMPORTANT**: Change these passwords in production!

---

## ⚙️ Spring Boot Configuration

Update your `application.yml` or `application.properties` for each microservice:

### Example: User Service

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/zaphira_users_db
    username: zaphira_user_service
    password: UserService2026!Secure
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # Important: use 'validate' not 'update' after migration
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

### Complete Configuration for All Services

```yaml
# User Service
spring.datasource.url=jdbc:postgresql://localhost:5432/zaphira_users_db
spring.datasource.username=zaphira_user_service
spring.datasource.password=UserService2026!Secure

# Auth Service
spring.datasource.url=jdbc:postgresql://localhost:5432/zaphira_auth_db
spring.datasource.username=zaphira_auth_service
spring.datasource.password=AuthService2026!Secure

# Wallet Service
spring.datasource.url=jdbc:postgresql://localhost:5432/zaphira_wallets_db
spring.datasource.username=zaphira_wallet_service
spring.datasource.password=WalletService2026!Secure

# Transaction Service
spring.datasource.url=jdbc:postgresql://localhost:5432/zaphira_transactions_db
spring.datasource.username=zaphira_transaction_service
spring.datasource.password=TransactionService2026!Secure

# Dispute Service
spring.datasource.url=jdbc:postgresql://localhost:5432/zaphira_disputes_db
spring.datasource.username=zaphira_dispute_service
spring.datasource.password=DisputeService2026!Secure

# Reporting Service
spring.datasource.url=jdbc:postgresql://localhost:5432/zaphira_reports_db
spring.datasource.username=zaphira_report_service
spring.datasource.password=ReportService2026!Secure

# Exchange Service
spring.datasource.url=jdbc:postgresql://localhost:5432/zaphira_exchange_db
spring.datasource.username=zaphira_exchange_service
spring.datasource.password=ExchangeService2026!Secure

# Notification Service
spring.datasource.url=jdbc:postgresql://localhost:5432/zaphira_notifications_db
spring.datasource.username=zaphira_notification_service
spring.datasource.password=NotificationService2026!Secure
```

---

## ✅ Verification Steps

### 1. Check Database Creation
```sql
-- Connect as postgres user
psql -U postgres

-- List all Zaphira databases
\l zaphira*

-- Expected output: 8 databases
```

### 2. Check Users
```sql
-- List all Zaphira users
\du zaphira*

-- Expected output: 8 users
```

### 3. Check Tables in Each Database
```sql
-- User Service
\c zaphira_users_db
\dt
-- Expected: 11 tables

-- Auth Service
\c zaphira_auth_db
\dt
-- Expected: 3 tables

-- Wallet Service
\c zaphira_wallets_db
\dt
-- Expected: 5 tables

-- Transaction Service
\c zaphira_transactions_db
\dt
-- Expected: 9 tables

-- Dispute Service
\c zaphira_disputes_db
\dt
-- Expected: 3 tables

-- Reporting Service
\c zaphira_reports_db
\dt
-- Expected: 5 tables

-- Exchange Service
\c zaphira_exchange_db
\dt
-- Expected: 4 tables

-- Notification Service
\c zaphira_notifications_db
\dt
-- Expected: 9 tables
```

### 4. Test User Permissions
```sql
-- Test connection as service user
psql -U zaphira_user_service -d zaphira_users_db -c "SELECT COUNT(*) FROM users;"
```

---

## 🔧 Common Issues and Solutions

### Issue 1: Permission Denied
```
ERROR: permission denied for database
```

**Solution**: Make sure you're running `01_create_databases.sql` as `postgres` superuser:
```bash
psql -U postgres -f 01_create_databases.sql
```

### Issue 2: Database Already Exists
```
ERROR: database "zaphira_users_db" already exists
```

**Solution**: The script includes `DROP DATABASE IF EXISTS`, but if you need to preserve data:
```sql
-- Backup first
pg_dump -U postgres zaphira_users_db > backup_users.sql

-- Then re-run script
```

### Issue 3: Foreign Key Violations
```
ERROR: violates foreign key constraint
```

**Solution**: This shouldn't happen if you follow the execution order. If it does, check that you haven't skipped any scripts.

### Issue 4: Hibernate DDL Auto Conflicts
```
ERROR: relation "users" already exists
```

**Solution**: Change `spring.jpa.hibernate.ddl-auto` to `validate` in application.yml:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # NOT 'create' or 'update'
```

---

## 📊 Database Statistics

After running all scripts, you can check statistics:

```sql
-- Total database sizes
SELECT 
    datname as database,
    pg_size_pretty(pg_database_size(datname)) as size
FROM pg_database
WHERE datname LIKE 'zaphira%'
ORDER BY pg_database_size(datname) DESC;

-- Table counts per database
SELECT 
    schemaname,
    COUNT(*) as table_count
FROM pg_tables
WHERE schemaname = 'public'
GROUP BY schemaname;
```

---

## 🚨 Security Recommendations

1. **Change Default Passwords**: Immediately change all default passwords in production
2. **Use Environment Variables**: Never hardcode passwords in application.yml
3. **Enable SSL**: Configure PostgreSQL to require SSL connections
4. **Restrict Network Access**: Use firewall rules to limit database access
5. **Enable Audit Logging**: Configure PostgreSQL logging for security audits
6. **Regular Backups**: Set up automated backup schedules
7. **Connection Pooling**: Configure HikariCP properly for each service

---

## 🔄 Rollback Procedure

If you need to rollback:

```sql
-- Backup all databases first
pg_dumpall -U postgres > full_backup.sql

-- Drop all databases
DROP DATABASE IF EXISTS zaphira_users_db;
DROP DATABASE IF EXISTS zaphira_auth_db;
DROP DATABASE IF EXISTS zaphira_wallets_db;
DROP DATABASE IF EXISTS zaphira_transactions_db;
DROP DATABASE IF EXISTS zaphira_disputes_db;
DROP DATABASE IF EXISTS zaphira_reports_db;
DROP DATABASE IF EXISTS zaphira_exchange_db;
DROP DATABASE IF EXISTS zaphira_notifications_db;

-- Drop all users
DROP USER IF EXISTS zaphira_user_service;
DROP USER IF EXISTS zaphira_auth_service;
DROP USER IF EXISTS zaphira_wallet_service;
DROP USER IF EXISTS zaphira_transaction_service;
DROP USER IF EXISTS zaphira_dispute_service;
DROP USER IF EXISTS zaphira_report_service;
DROP USER IF EXISTS zaphira_exchange_service;
DROP USER IF EXISTS zaphira_notification_service;
```

---

## 📞 Support

For issues or questions:
1. Review the architecture document: `00_database_architecture_analysis.md`
2. Check PostgreSQL logs: `tail -f /var/log/postgresql/postgresql-*.log`
3. Verify Spring Boot application logs

---

## ✨ Features Included

- ✅ Complete schema definitions for all 8 microservices
- ✅ Indexes for performance optimization
- ✅ Foreign key constraints (within same database only)
- ✅ Check constraints for data validation
- ✅ Triggers for automatic timestamp updates
- ✅ Utility functions for common operations
- ✅ Views for common queries
- ✅ Initial seed data where appropriate
- ✅ Comprehensive comments and documentation

---

**Version**: 1.0  
**Date**: 21 January 2026  
**PostgreSQL Version**: 14+  
**Status**: Ready for Production

---

## 🎉 Next Steps

1. Execute all SQL scripts in order
2. Update Spring Boot configuration files
3. Run application tests to verify connectivity
4. Deploy to development environment
5. Perform data migration if upgrading from existing system
6. Configure backup schedules
7. Set up monitoring and alerting

Good luck! 🚀
