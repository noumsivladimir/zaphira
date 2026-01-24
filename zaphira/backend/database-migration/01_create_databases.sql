-- ============================================================================
-- ZAPHIRA Platform - Database Creation Script
-- ============================================================================
-- Description: Creates all databases and users for Zaphira microservices
-- Author: AI Backend Architect
-- Date: 21 January 2026
-- Version: 1.0
-- PostgreSQL Version: 14+
-- ============================================================================

-- ============================================================================
-- SECTION 1: DATABASE CREATION
-- ============================================================================

-- 1. USER SERVICE DATABASE
DROP DATABASE IF EXISTS zaphira_users_db;
CREATE DATABASE zaphira_users_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

COMMENT ON DATABASE zaphira_users_db IS 'User management, authentication, KYC, and user sessions';

-- 2. AUTH SERVICE DATABASE
DROP DATABASE IF EXISTS zaphira_auth_db;
CREATE DATABASE zaphira_auth_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

COMMENT ON DATABASE zaphira_auth_db IS 'Authentication tokens, JWT, refresh tokens, activity logs';

-- 3. WALLET SERVICE DATABASE
DROP DATABASE IF EXISTS zaphira_wallets_db;
CREATE DATABASE zaphira_wallets_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

COMMENT ON DATABASE zaphira_wallets_db IS 'Wallet management, sub-wallets, permissions, wallet history';

-- 4. TRANSACTION SERVICE DATABASE
DROP DATABASE IF EXISTS zaphira_transactions_db;
CREATE DATABASE zaphira_transactions_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

COMMENT ON DATABASE zaphira_transactions_db IS 'Transactions, settlements, refunds, authorizations, audit';

-- 5. DISPUTE SERVICE DATABASE
DROP DATABASE IF EXISTS zaphira_disputes_db;
CREATE DATABASE zaphira_disputes_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

COMMENT ON DATABASE zaphira_disputes_db IS 'Disputes, evidence, dispute timeline, resolutions';

-- 6. REPORTING SERVICE DATABASE
DROP DATABASE IF EXISTS zaphira_reports_db;
CREATE DATABASE zaphira_reports_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

COMMENT ON DATABASE zaphira_reports_db IS 'Daily reports, analytics, merchant metrics, user metrics';

-- 7. EXCHANGE SERVICE DATABASE
DROP DATABASE IF EXISTS zaphira_exchange_db;
CREATE DATABASE zaphira_exchange_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

COMMENT ON DATABASE zaphira_exchange_db IS 'Exchange rates, currency conversions, forex data';

-- 8. NOTIFICATION SERVICE DATABASE
DROP DATABASE IF EXISTS zaphira_notifications_db;
CREATE DATABASE zaphira_notifications_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

COMMENT ON DATABASE zaphira_notifications_db IS 'Notifications, OTP codes, verification tokens, email/SMS logs';

-- ============================================================================
-- SECTION 2: USER CREATION WITH SECURE PASSWORDS
-- ============================================================================
-- NOTE: Change these passwords in production!
-- ============================================================================

-- 1. USER SERVICE USER
DROP USER IF EXISTS zaphira_user_service;
CREATE USER zaphira_user_service WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'UserService2026!Secure';

COMMENT ON ROLE zaphira_user_service IS 'Database user for User Service microservice';

-- 2. AUTH SERVICE USER
DROP USER IF EXISTS zaphira_auth_service;
CREATE USER zaphira_auth_service WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'AuthService2026!Secure';

COMMENT ON ROLE zaphira_auth_service IS 'Database user for Auth Service microservice';

-- 3. WALLET SERVICE USER
DROP USER IF EXISTS zaphira_wallet_service;
CREATE USER zaphira_wallet_service WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'WalletService2026!Secure';

COMMENT ON ROLE zaphira_wallet_service IS 'Database user for Wallet Service microservice';

-- 4. TRANSACTION SERVICE USER
DROP USER IF EXISTS zaphira_transaction_service;
CREATE USER zaphira_transaction_service WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'TransactionService2026!Secure';

COMMENT ON ROLE zaphira_transaction_service IS 'Database user for Transaction Service microservice';

-- 5. DISPUTE SERVICE USER
DROP USER IF EXISTS zaphira_dispute_service;
CREATE USER zaphira_dispute_service WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'DisputeService2026!Secure';

COMMENT ON ROLE zaphira_dispute_service IS 'Database user for Dispute Service microservice';

-- 6. REPORTING SERVICE USER
DROP USER IF EXISTS zaphira_report_service;
CREATE USER zaphira_report_service WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'ReportService2026!Secure';

COMMENT ON ROLE zaphira_report_service IS 'Database user for Reporting Service microservice';

-- 7. EXCHANGE SERVICE USER
DROP USER IF EXISTS zaphira_exchange_service;
CREATE USER zaphira_exchange_service WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'ExchangeService2026!Secure';

COMMENT ON ROLE zaphira_exchange_service IS 'Database user for Exchange Service microservice';

-- 8. NOTIFICATION SERVICE USER
DROP USER IF EXISTS zaphira_notification_service;
CREATE USER zaphira_notification_service WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'NotificationService2026!Secure';

COMMENT ON ROLE zaphira_notification_service IS 'Database user for Notification Service microservice';

-- ============================================================================
-- SECTION 3: GRANT PRIVILEGES
-- ============================================================================

-- 1. USER SERVICE PRIVILEGES
GRANT ALL PRIVILEGES ON DATABASE zaphira_users_db TO zaphira_user_service;
\c zaphira_users_db
GRANT ALL ON SCHEMA public TO zaphira_user_service;
GRANT ALL ON ALL TABLES IN SCHEMA public TO zaphira_user_service;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO zaphira_user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zaphira_user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zaphira_user_service;

-- 2. AUTH SERVICE PRIVILEGES
\c postgres
GRANT ALL PRIVILEGES ON DATABASE zaphira_auth_db TO zaphira_auth_service;
\c zaphira_auth_db
GRANT ALL ON SCHEMA public TO zaphira_auth_service;
GRANT ALL ON ALL TABLES IN SCHEMA public TO zaphira_auth_service;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO zaphira_auth_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zaphira_auth_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zaphira_auth_service;

-- 3. WALLET SERVICE PRIVILEGES
\c postgres
GRANT ALL PRIVILEGES ON DATABASE zaphira_wallets_db TO zaphira_wallet_service;
\c zaphira_wallets_db
GRANT ALL ON SCHEMA public TO zaphira_wallet_service;
GRANT ALL ON ALL TABLES IN SCHEMA public TO zaphira_wallet_service;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO zaphira_wallet_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zaphira_wallet_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zaphira_wallet_service;

-- 4. TRANSACTION SERVICE PRIVILEGES
\c postgres
GRANT ALL PRIVILEGES ON DATABASE zaphira_transactions_db TO zaphira_transaction_service;
\c zaphira_transactions_db
GRANT ALL ON SCHEMA public TO zaphira_transaction_service;
GRANT ALL ON ALL TABLES IN SCHEMA public TO zaphira_transaction_service;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO zaphira_transaction_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zaphira_transaction_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zaphira_transaction_service;

-- 5. DISPUTE SERVICE PRIVILEGES
\c postgres
GRANT ALL PRIVILEGES ON DATABASE zaphira_disputes_db TO zaphira_dispute_service;
\c zaphira_disputes_db
GRANT ALL ON SCHEMA public TO zaphira_dispute_service;
GRANT ALL ON ALL TABLES IN SCHEMA public TO zaphira_dispute_service;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO zaphira_dispute_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zaphira_dispute_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zaphira_dispute_service;

-- 6. REPORTING SERVICE PRIVILEGES
\c postgres
GRANT ALL PRIVILEGES ON DATABASE zaphira_reports_db TO zaphira_report_service;
\c zaphira_reports_db
GRANT ALL ON SCHEMA public TO zaphira_report_service;
GRANT ALL ON ALL TABLES IN SCHEMA public TO zaphira_report_service;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO zaphira_report_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zaphira_report_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zaphira_report_service;

-- 7. EXCHANGE SERVICE PRIVILEGES
\c postgres
GRANT ALL PRIVILEGES ON DATABASE zaphira_exchange_db TO zaphira_exchange_service;
\c zaphira_exchange_db
GRANT ALL ON SCHEMA public TO zaphira_exchange_service;
GRANT ALL ON ALL TABLES IN SCHEMA public TO zaphira_exchange_service;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO zaphira_exchange_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zaphira_exchange_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zaphira_exchange_service;

-- 8. NOTIFICATION SERVICE PRIVILEGES
\c postgres
GRANT ALL PRIVILEGES ON DATABASE zaphira_notifications_db TO zaphira_notification_service;
\c zaphira_notifications_db
GRANT ALL ON SCHEMA public TO zaphira_notification_service;
GRANT ALL ON ALL TABLES IN SCHEMA public TO zaphira_notification_service;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO zaphira_notification_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO zaphira_notification_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO zaphira_notification_service;

-- ============================================================================
-- SECTION 4: VERIFICATION
-- ============================================================================

\c postgres

-- List all databases
SELECT datname, pg_encoding_to_char(encoding), datcollate, datctype 
FROM pg_database 
WHERE datname LIKE 'zaphira_%'
ORDER BY datname;

-- List all service users
SELECT usename, usesuper, usecreatedb, usecreaterole 
FROM pg_user 
WHERE usename LIKE 'zaphira_%'
ORDER BY usename;

-- ============================================================================
-- END OF SCRIPT
-- ============================================================================
-- Next Steps:
-- 1. Execute this script as 'postgres' superuser
-- 2. Verify all databases and users are created
-- 3. Run individual schema creation scripts (02_*.sql to 09_*.sql)
-- 4. Update application.yml with new connection strings
-- ============================================================================
