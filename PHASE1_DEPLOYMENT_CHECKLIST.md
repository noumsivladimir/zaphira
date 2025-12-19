# ✅ PHASE 1 - DEPLOYMENT CHECKLIST
## Advanced Search & Filtering - Pre-Production Validation

**Phase:** 1 of 4  
**Component:** Transaction Service  
**Feature:** Advanced Search & Filtering  
**Status:** Ready for Deployment ✅

---

## 🔍 PRE-DEPLOYMENT VALIDATION

### Code Quality
- [ ] All files compile without errors
  ```bash
  mvn clean compile
  # Expected: BUILD SUCCESS
  ```
- [ ] No warnings in compilation
- [ ] All imports resolved
- [ ] Annotations properly applied
- [ ] Logging configured (@Slf4j)
- [ ] Code review passed
- [ ] Security review passed

### Database Migration
- [ ] Migration file V20251216__add_search_indexes.sql exists
- [ ] Migration file location: `src/main/resources/db/migration/`
- [ ] SQL syntax validated
- [ ] Index names don't conflict
- [ ] 12 indexes in migration file
- [ ] Test migration in DEV environment first

### Dependencies
- [ ] Spring Data JPA available
- [ ] Jackson dependencies available (for JSON export)
- [ ] Lombok available (@Slf4j, @Data, etc.)
- [ ] PostgreSQL JDBC driver available
- [ ] All Maven dependencies resolved

---

## 🧪 TESTING VALIDATION

### Unit Tests
- [ ] TransactionSearchServiceTest exists
- [ ] Test cases for all 10 methods
- [ ] All tests passing
  ```bash
  mvn test -Dtest=TransactionSearchServiceTest
  # Expected: All tests pass
  ```
- [ ] Code coverage > 80%

### Integration Tests
- [ ] TransactionControllerTest exists
- [ ] Tests for all 8 endpoints
- [ ] All tests passing
  ```bash
  mvn test -Dtest=TransactionControllerTest
  # Expected: All tests pass
  ```
- [ ] Integration with real DB tested

### API Tests
- [ ] All 8 endpoints responding (200 OK)
- [ ] Search endpoint returns paginated results
- [ ] Export endpoints return correct format
- [ ] Statistics endpoint returns JSON
- [ ] Error cases return proper HTTP status codes
- [ ] Authentication required (401 without token)

### Performance Tests
- [ ] Single search query < 50ms
- [ ] Complex query (all filters) < 200ms
- [ ] Export 10K rows CSV < 1 second
- [ ] Export 10K rows JSON < 1.5 seconds
- [ ] Pagination working smoothly
- [ ] No N+1 query issues

### Data Validation
- [ ] Test data inserted (50+ transactions)
- [ ] All transaction statuses represented
- [ ] Various amounts, currencies, dates
- [ ] Multiple senders/receivers
- [ ] Both scheduled and non-scheduled

---

## 🗄️ DATABASE VALIDATION

### Migration Execution
- [ ] Backup existing database first
  ```bash
  # In your staging/dev environment
  pg_dump wallet_db > backup_before_migration.sql
  ```
- [ ] Run migration in DEV
  ```bash
  mvn clean flyway:migrate -Dspring.profiles.active=dev
  ```
- [ ] Migration succeeds without errors
- [ ] Transaction_staging table not locked

### Index Verification
- [ ] 12 indexes successfully created
  ```sql
  SELECT indexname FROM pg_indexes 
  WHERE tablename = 'transactions'
  ORDER BY indexname;
  ```
  Expected output:
  ```
  idx_transactions_amount_created
  idx_transactions_created_at
  idx_transactions_currency
  idx_transactions_receiver_status
  idx_transactions_receiver_wallet
  idx_transactions_reference
  idx_transactions_route
  idx_transactions_scheduled
  idx_transactions_sender_status
  idx_transactions_sender_wallet
  idx_transactions_status_created
  idx_transactions_status_date_range
  idx_transactions_type_currency
  ```

- [ ] Index sizes reasonable (< 100MB for 1M records)
- [ ] Index usage verified with EXPLAIN ANALYZE
  ```sql
  EXPLAIN ANALYZE 
  SELECT * FROM transactions 
  WHERE sender_wallet_number = 'WAL001' 
  ORDER BY created_at DESC 
  LIMIT 20;
  ```
  Expected: Uses index (Index Scan, not Seq Scan)

- [ ] Duplicate indexes not created
- [ ] No index errors in PostgreSQL logs

### Data Integrity
- [ ] Transaction counts before/after match
  ```sql
  SELECT COUNT(*) FROM transactions;
  ```
- [ ] All data still accessible
- [ ] Foreign key relationships intact
- [ ] Constraints still enforced

---

## 🔒 SECURITY VALIDATION

### SQL Injection
- [ ] Test with special characters in search
  ```
  Reference: "; DROP TABLE transactions; --
  → Should return 0 results, not execute SQL
  ```
- [ ] Test with UNION attacks
  ```
  Reference: %' UNION SELECT * FROM users --
  → Should escape properly
  ```
- [ ] Test with numeric injection
  ```
  amountMin: "100; DELETE FROM transactions; --"
  → Should be rejected or escaped
  ```

### Authentication
- [ ] Endpoint requires token (401 without)
- [ ] Invalid token rejected
- [ ] Expired token rejected
- [ ] Wrong secret key rejected

### Input Validation
- [ ] Negative page size rejected
- [ ] Page size > 1000 capped to 1000
- [ ] Invalid date format rejected
- [ ] Invalid enum rejected
- [ ] Non-numeric amount rejected
- [ ] Empty parameters handled

### Data Exposure
- [ ] Sensitive fields not in response
- [ ] Passwords/secrets not logged
- [ ] Error messages don't leak information
- [ ] Pagination doesn't expose counts

---

## 🚀 DEPLOYMENT EXECUTION

### Pre-Deployment
- [ ] Announce maintenance window (if needed)
- [ ] Notify stakeholders
- [ ] Backup production database
- [ ] Have rollback plan ready

### Deployment (DEV → STAGING → PROD)

**Step 1: Build**
```bash
mvn clean package -DskipTests
# Expected: BUILD SUCCESS
# Expected: transaction-service-X.X.X.jar created
```
- [ ] Build succeeds
- [ ] JAR file created
- [ ] JAR size reasonable (< 100MB)

**Step 2: Migration (DEV)**
```bash
# Deploy to DEV first
java -jar target/transaction-service-X.X.X.jar --spring.profiles.active=dev
# App should start successfully
mvn flyway:migrate -Dspring.profiles.active=dev
# Migration should succeed
```
- [ ] App starts without errors
- [ ] Database migration succeeds
- [ ] Logs show successful initialization

**Step 3: Test (DEV)**
```bash
# Run basic API test
curl http://localhost:8080/api/transactions/search \
  -H "Authorization: Bearer DEV_TOKEN"
# Expected: 200 OK with results
```
- [ ] Can create transactions
- [ ] Can search transactions
- [ ] Can export transactions
- [ ] All endpoints responding
- [ ] Performance acceptable

**Step 4: Migration (STAGING)**
```bash
# Same as DEV
java -jar target/transaction-service-X.X.X.jar --spring.profiles.active=staging
mvn flyway:migrate -Dspring.profiles.active=staging
```
- [ ] Staging deployment succeeds
- [ ] All tests pass in staging
- [ ] Performance acceptable in staging

**Step 5: Load Test (STAGING)**
```bash
# Run concurrent requests
# Using Apache JMeter or similar
# Target: 100 concurrent requests
# Expected: All succeed, avg response < 200ms
```
- [ ] Handles 100+ concurrent requests
- [ ] No errors under load
- [ ] Response time acceptable
- [ ] Database handles load
- [ ] Memory usage reasonable

**Step 6: Migration (PRODUCTION)**
```bash
# Production deployment
java -jar target/transaction-service-X.X.X.jar --spring.profiles.active=prod
mvn flyway:migrate -Dspring.profiles.active=prod
```
- [ ] Production deployment succeeds
- [ ] Database migration succeeds
- [ ] App healthy and running
- [ ] No errors in logs

**Step 7: Smoke Test (PRODUCTION)**
```bash
# Test main endpoints
curl https://api.prod.example.com/api/transactions/search \
  -H "Authorization: Bearer PROD_TOKEN"
```
- [ ] All endpoints responding in production
- [ ] Search functionality working
- [ ] Export functionality working
- [ ] Statistics endpoint working
- [ ] Error handling correct

---

## 📊 POST-DEPLOYMENT MONITORING

### Immediate (First 1 hour)
- [ ] Monitor error rates (should be 0)
- [ ] Monitor response times (should be <200ms)
- [ ] Check logs for exceptions
- [ ] Verify database connections
- [ ] Check disk space usage
- [ ] Monitor CPU/Memory
- [ ] Test manual requests every 5 minutes

### Short-term (First 24 hours)
- [ ] Monitor daily transaction volume
- [ ] Check search query performance
- [ ] Verify index usage
- [ ] Monitor error rates
- [ ] Check for slow queries
- [ ] Verify backup completion
- [ ] Check disk usage growth

### Ongoing (Weekly)
- [ ] Review error logs
- [ ] Analyze performance metrics
- [ ] Check index fragmentation
  ```sql
  SELECT schemaname, tablename, indexname, 
         round(100.0*indisclustered/(indrelname), 2) AS clustering 
  FROM pg_stat_user_indexes 
  WHERE tablename = 'transactions';
  ```
- [ ] Review slow query logs
- [ ] Check autovacuum status

---

## 🔄 ROLLBACK PLAN

If issues occur:

### Immediate Rollback
```bash
# Stop current version
kill <pid>

# Restore from backup
psql wallet_db < backup_before_migration.sql

# Revert to previous JAR
java -jar transaction-service-OLD_VERSION.jar

# Start service
systemctl start transaction-service
```

### Rollback Validation
- [ ] Service started successfully
- [ ] Indexes rolled back
- [ ] Data restored to pre-deployment state
- [ ] All endpoints operational again
- [ ] No data loss

### Rollback Decision Criteria
Execute rollback if:
- [ ] Error rate > 1% for 10 minutes
- [ ] Response time > 1 second
- [ ] Database connection errors
- [ ] Out of memory errors
- [ ] Disk space issues
- [ ] Security vulnerability discovered

---

## 📋 CONFIGURATION VALIDATION

### application.properties
Verify settings for current environment:

**DEV/STAGING:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wallet_db
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
logging.level.root=DEBUG
```

**PRODUCTION:**
```properties
spring.datasource.url=jdbc:postgresql://db-prod:5432/wallet_db
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
logging.level.root=INFO
logging.level.com.zaphira=INFO
```

- [ ] Database URL correct for environment
- [ ] Credentials from environment variables (PROD)
- [ ] DDL mode set to "validate" (not "create" or "update")
- [ ] Flyway enabled
- [ ] Logging level appropriate
- [ ] Connection pool configured

---

## 📞 COMMUNICATION

### Before Deployment
- [ ] Notify stakeholders 24 hours in advance
- [ ] Notify support team
- [ ] Update status page (if applicable)
- [ ] Prepare rollback procedures

### During Deployment
- [ ] Post updates to team channel
- [ ] Provide ETA for completion
- [ ] Alert on any issues immediately

### After Deployment
- [ ] Post successful deployment notification
- [ ] Provide release notes
- [ ] Document any issues encountered
- [ ] Thank team members

---

## ✅ SIGN-OFF

**Deployed By:** ________________  
**Date:** ________________  
**Time:** ________________  
**Environment:** ☐ DEV ☐ STAGING ☐ PRODUCTION  

**Issues Encountered:** 
```
(List any issues and how they were resolved)
```

**Sign-off:** ☐ APPROVED ☐ ROLLBACK EXECUTED  

**Approval By:** ________________ (Tech Lead/PM)

---

## 📚 REFERENCES

- Implementation: [PHASE1_ADVANCED_SEARCH_IMPLEMENTATION.md](PHASE1_ADVANCED_SEARCH_IMPLEMENTATION.md)
- API Docs: [ADVANCED_SEARCH_API_DOCUMENTATION.md](ADVANCED_SEARCH_API_DOCUMENTATION.md)
- Testing: [PHASE1_TESTING_GUIDE.md](PHASE1_TESTING_GUIDE.md)
- Status: [PHASE1_STATUS_REPORT.md](PHASE1_STATUS_REPORT.md)

---

**Last Updated:** 16 December 2025  
**Version:** 1.0  
**Status:** Ready for Deployment ✅
