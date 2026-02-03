# 📑 Database Migration - Index & Quick Reference

## 📂 Files in this Directory

| File | Type | Purpose | Execute Order |
|------|------|---------|---------------|
| `README.md` | Documentation | Complete migration guide | Read first |
| `00_database_architecture_analysis.md` | Documentation | Architecture analysis & decisions | Reference |
| `DATABASE_MAPPING_COMPLETE.md` | Documentation | Visual entity mapping | Reference |
| `execute-migrations.ps1` | Script | Automated execution script | Run after review |
| `01_create_databases.sql` | SQL Script | Creates databases & users | 1st |
| `02_user_service_schema.sql` | SQL Script | User Service tables | 2nd |
| `03_auth_service_schema.sql` | SQL Script | Auth Service tables | 3rd |
| `04_wallet_service_schema.sql` | SQL Script | Wallet Service tables | 4th |
| `05_transaction_service_schema.sql` | SQL Script | Transaction Service tables | 5th |
| `06_dispute_service_schema.sql` | SQL Script | Dispute Service tables | 6th |
| `07_reporting_service_schema.sql` | SQL Script | Reporting Service tables | 7th |
| `08_exchange_service_schema.sql` | SQL Script | Exchange Service tables | 8th |
| `09_notification_service_schema.sql` | SQL Script | Notification Service tables | 9th |

---

## 🚀 Quick Start

### Option 1: Automated Execution (Recommended)
```powershell
# Windows PowerShell
.\execute-migrations.ps1 -PostgresHost localhost -PostgresUser postgres

# With dry run (test without executing)
.\execute-migrations.ps1 -PostgresHost localhost -PostgresUser postgres -DryRun
```

### Option 2: Manual Execution
```bash
# Execute all scripts in order
psql -U postgres -f 01_create_databases.sql
psql -U postgres -f 02_user_service_schema.sql
psql -U postgres -f 03_auth_service_schema.sql
psql -U postgres -f 04_wallet_service_schema.sql
psql -U postgres -f 05_transaction_service_schema.sql
psql -U postgres -f 06_dispute_service_schema.sql
psql -U postgres -f 07_reporting_service_schema.sql
psql -U postgres -f 08_exchange_service_schema.sql
psql -U postgres -f 09_notification_service_schema.sql
```

---

## 📊 Database Overview

| Database | Service | Tables | Key Entities |
|----------|---------|--------|--------------|
| `zaphira_users_db` | User Service | 11 | users, kyc, user_sessions |
| `zaphira_auth_db` | Auth Service | 3 | tokens, refresh_token, activity_log |
| `zaphira_wallets_db` | Wallet Service | 5 | wallets, sub_wallets, permissions |
| `zaphira_transactions_db` | Transaction Service | 9 | transactions, settlements, refunds |
| `zaphira_disputes_db` | Dispute Service | 3 | disputes, evidence, timeline |
| `zaphira_reports_db` | Reporting Service | 5 | daily_reports, analytics |
| `zaphira_exchange_db` | Exchange Service | 4 | exchange_rates, conversions |
| `zaphira_notifications_db` | Notification Service | 9 | notifications, otp_codes, templates |

**Total: 8 Databases | 49 Tables**

---

## 🔑 Database Credentials

| Service | Username | Password | Database |
|---------|----------|----------|----------|
| User | `zaphira_user_service` | `UserService2026!Secure` | `zaphira_users_db` |
| Auth | `zaphira_auth_service` | `AuthService2026!Secure` | `zaphira_auth_db` |
| Wallet | `zaphira_wallet_service` | `WalletService2026!Secure` | `zaphira_wallets_db` |
| Transaction | `zaphira_transaction_service` | `TransactionService2026!Secure` | `zaphira_transactions_db` |
| Dispute | `zaphira_dispute_service` | `DisputeService2026!Secure` | `zaphira_disputes_db` |
| Reporting | `zaphira_report_service` | `ReportService2026!Secure` | `zaphira_reports_db` |
| Exchange | `zaphira_exchange_service` | `ExchangeService2026!Secure` | `zaphira_exchange_db` |
| Notification | `zaphira_notification_service` | `NotificationService2026!Secure` | `zaphira_notifications_db` |

⚠️ **Change these passwords in production!**

---

## 🔍 Quick Verification

```sql
-- Check all databases exist
\c postgres
SELECT datname FROM pg_database WHERE datname LIKE 'zaphira%';

-- Check all users exist
\du zaphira*

-- Verify table counts
\c zaphira_users_db
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: 11

\c zaphira_auth_db
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: 3

\c zaphira_wallets_db
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: 5

\c zaphira_transactions_db
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: 9

\c zaphira_disputes_db
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: 3

\c zaphira_reports_db
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: 5

\c zaphira_exchange_db
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: 4

\c zaphira_notifications_db
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: 9
```

---

## 📝 Configuration Update

After running migrations, update `application.yml` for each microservice:

```yaml
# Example for User Service
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/zaphira_users_db
    username: zaphira_user_service
    password: UserService2026!Secure
  jpa:
    hibernate:
      ddl-auto: validate  # ⚠️ IMPORTANT: Use 'validate' not 'update'
```

---

## 🔗 Related Documentation

- [README.md](README.md) - Complete migration guide
- [00_database_architecture_analysis.md](00_database_architecture_analysis.md) - Architecture decisions
- [DATABASE_MAPPING_COMPLETE.md](DATABASE_MAPPING_COMPLETE.md) - Entity relationship mapping

---

## 📞 Support & Troubleshooting

### Common Issues

**Issue**: Permission denied
```bash
# Solution: Run as postgres superuser
psql -U postgres -f 01_create_databases.sql
```

**Issue**: Database already exists
```bash
# Solution: Backup first, then script will DROP and recreate
pg_dump -U postgres zaphira_users_db > backup.sql
```

**Issue**: Connection refused
```bash
# Solution: Check PostgreSQL is running
sudo systemctl status postgresql  # Linux
# or check Services on Windows
```

---

## ✅ Post-Migration Checklist

- [ ] All 8 databases created
- [ ] All 8 users created with correct passwords
- [ ] All 49 tables created
- [ ] Indexes created (180+)
- [ ] Triggers created (20+)
- [ ] Functions created (25+)
- [ ] Views created (30+)
- [ ] Initial seed data inserted
- [ ] Spring Boot configs updated
- [ ] Hibernate ddl-auto set to 'validate'
- [ ] Application connection tests passed
- [ ] Backup schedule configured
- [ ] Monitoring configured

---

## 📈 Statistics

- **Total Execution Time**: ~2-5 minutes
- **Total SQL Statements**: 1,500+
- **Total Lines of SQL**: 5,000+
- **Databases**: 8
- **Tables**: 49
- **Indexes**: 180+
- **Foreign Keys**: 35
- **Check Constraints**: 60+
- **Triggers**: 20+
- **Functions**: 25+
- **Views**: 30+

---

## 🎯 Success Criteria

✓ All databases created  
✓ All users created with proper privileges  
✓ All tables created with correct schema  
✓ All indexes created  
✓ All constraints enforced  
✓ All triggers functioning  
✓ All views accessible  
✓ Initial data seeded  
✓ Application can connect  
✓ Queries execute successfully  

---

## 🔄 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-21 | Initial release - Complete database structure |

---

## 🏆 Features

✅ Complete schema for 8 microservices  
✅ Optimized indexes for performance  
✅ Data validation constraints  
✅ Automatic timestamp updates  
✅ Audit trail tables  
✅ Utility functions  
✅ Pre-computed views  
✅ Seed data  
✅ Comprehensive documentation  
✅ Automated execution script  

---

**Status**: Production Ready ✅  
**Last Updated**: 21 January 2026  
**Maintained by**: Zaphira Platform Team

---

For detailed information, see [README.md](README.md)
