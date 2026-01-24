# Zaphira Backend - Quick Start Guide

## 🎯 Executive Summary

**7 active microservices** in a Spring Boot ecosystem with:
- **Synchronous** communication via Feign HTTP clients
- **Asynchronous** messaging via Kafka (optional - notifications only)
- **Single entry point**: API Gateway (:8080)
- **Service discovery**: Eureka (:8761)

## 🚀 Quick Start Options

### Option 1: Login Only (3 Services) - 2 Minutes

**Use Case:** Test authentication, JWT validation

```powershell
# Run automated script
.\start-backend-phase1.ps1
```

**Services:** Eureka + auth-service + api-gateway

**Test:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+237690123456", "pin": "1234"}'
```

---

### Option 2: Full Registration (5 Services) - 3 Minutes ⭐ RECOMMENDED

**Use Case:** Complete user lifecycle (register → verify → login)

```powershell
# Run automated script
.\start-backend-phase2.ps1
```

**Services:** Eureka + wallet-service + user-service + auth-service + api-gateway

**Test:**
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+237690123456",
    "email": "test@example.com",
    "pin": "1234",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

---

## 📋 Prerequisites

- ✅ **Java 17+** (`java -version`)
- ✅ **Maven 3.8+** (`mvn -version`)
- ✅ **PostgreSQL** running on `192.168.0.122:5432`
- ✅ **Database** `wallet_db` must exist
- ⚠️ **Kafka** (optional - only for SMS/Email notifications)

---

## 🔑 Key Architectural Points

### 1. Wallet Creation is SYNCHRONOUS
- **NOT Kafka-based** (listener is commented out)
- user-service calls wallet-service via **Feign HTTP**
- This creates a **hard dependency**: user-service REQUIRES wallet-service at runtime

### 2. Service Startup Order Matters
```
1. Eureka (service-registry) - MUST start first
2. wallet-service - MUST start before user-service
3. user-service
4. auth-service
5. api-gateway - MUST start last
```

### 3. Database is Shared
- Multiple services use same `wallet_db` database
- Violates microservices data isolation principle
- Be aware of potential data consistency issues

### 4. Kafka is Optional
- **ONLY needed for notifications** (SMS/Email OTP)
- Core registration flow works **WITHOUT Kafka**
- Can skip notification-service for MVP

---

## 🌐 API Gateway Routes

**Base URL:** `http://localhost:8080`

### Authentication
```
POST /api/auth/login       → Login (returns JWT)
POST /api/auth/refresh     → Refresh JWT token
GET  /api/auth/validate    → Validate JWT token
```

### User Management
```
POST /api/users/register        → Register new user
POST /api/users/verify-email    → Verify OTP (creates wallet here)
GET  /api/users/profile         → Get user profile (JWT required)
PUT  /api/users/profile         → Update profile (JWT required)
GET  /api/users/{id}            → Get user by ID (JWT required)
```

### Wallet Operations
```
GET  /api/wallets/user/{userId}     → Get user wallets
GET  /api/wallets/{walletNumber}    → Get wallet details
```

---

## 🔧 Manual Startup (Without Scripts)

### Phase 1: Login Only

```powershell
# Terminal 1: Eureka
cd backend\service-registry
mvn spring-boot:run

# Wait 30 seconds, then Terminal 2: Auth
cd backend\auth-service
mvn spring-boot:run

# Wait 20 seconds, then Terminal 3: Gateway
cd backend\api-gateway
mvn spring-boot:run
```

### Phase 2: Full Registration

```powershell
# Terminal 1: Eureka
cd backend\service-registry
mvn spring-boot:run

# Wait 30 seconds, then Terminal 2: Wallet (MUST be UP before user-service)
cd backend\wallet-service
mvn spring-boot:run

# Wait 30 seconds, then Terminal 3: User
cd backend\user-service
mvn spring-boot:run

# Terminal 4: Auth
cd backend\auth-service
mvn spring-boot:run

# Terminal 5: Gateway (LAST)
cd backend\api-gateway
mvn spring-boot:run
```

---

## 🛠️ Troubleshooting

### Issue: User service won't start
**Cause:** wallet-service is not running or not registered with Eureka

**Solution:**
1. Check wallet-service terminal - should see "Started WalletServiceApplication"
2. Check Eureka dashboard: http://localhost:8761
3. Verify wallet-service is registered (green status)

### Issue: Registration hangs at verification
**Cause:** wallet-service is down during `verify-email` call

**Solution:**
1. Check wallet-service logs for errors
2. Verify wallet-service port 8083 is accessible
3. Check circuit breaker status: `http://localhost:8082/actuator/circuitbreakers`

### Issue: 401 Unauthorized on protected endpoints
**Cause:** JWT token invalid or expired

**Solution:**
1. Verify JWT in `Authorization: Bearer <token>` header
2. Check token expiration (15 minutes default)
3. Use refresh token to get new access token

### Issue: Eureka shows no registered instances
**Cause:** Services not registering with Eureka

**Solution:**
1. Verify Eureka is running: http://localhost:8761
2. Check each service's `application.properties`:
   ```properties
   eureka.client.register-with-eureka=true
   eureka.client.fetch-registry=true
   eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
   ```

---

## 📊 Service Ports Reference

| Service | Port | Type | Exposed to Frontend |
|---------|------|------|---------------------|
| api-gateway | 8080 | Infrastructure | ✅ YES (ONLY ONE) |
| service-registry | 8761 | Infrastructure | ❌ No (internal) |
| auth-service | 8081 | Business | ❌ No (via gateway) |
| user-service | 8082 | Business | ❌ No (via gateway) |
| wallet-service | 8083 | Business | ❌ No (via gateway) |
| notification-service | 8089 | Support | ❌ No (async only) |

---

## 🔐 Environment Variables

Create a `.env` file or set in system:

```bash
# Database
DB_HOST=192.168.0.122
DB_PORT=5432
DB_NAME=wallet_db
DB_USER=postgres
DB_PASSWORD=1234

# JWT
JWT_SECRET=7pBJFFNs9RzeTwTz/NHFY1e1QyFVnDoZbBTG8zsdGHeAsAzmqcxLneWASllVgTbAdgdrS+XhAR9nfg1hPglA3Q==
JWT_ACCESS_EXPIRATION=900000      # 15 minutes
JWT_REFRESH_EXPIRATION=2592000000 # 30 days

# Service Discovery
EUREKA_SERVER_URL=http://localhost:8761/eureka/

# Service URLs (Phase 2)
WALLET_SERVICE_URL=http://localhost:8083
USER_SERVICE_URL=http://localhost:8082

# Kafka (Optional - notifications only)
KAFKA_BOOTSTRAP_SERVERS=192.168.0.122:9092

# Twilio (Optional - notifications only)
TWILIO_ACCOUNT_SID=ACxxxx
TWILIO_AUTH_TOKEN=xxxx
TWILIO_FROM_NUMBER=+1234567890
```

---

## 📚 Additional Resources

- **Full Documentation:** `BACKEND_ANALYSIS_AND_INTEGRATION_STRATEGY.md`
- **Backend README:** `backend/README.md`
- **Integration Guide:** `INTEGRATION_GUIDE.md`
- **Eureka Dashboard:** http://localhost:8761
- **Swagger UI (if enabled):** http://localhost:808X/swagger-ui.html

---

## 🎯 Next Steps

1. ✅ Start with Phase 1 (Login Only) to test authentication
2. ✅ Create test users manually in database
3. ✅ Test login endpoint with frontend
4. ✅ Move to Phase 2 (Full Registration)
5. ✅ Test user registration flow
6. 🔄 Add notification-service for SMS/Email OTP (optional)
7. 🔄 Generate TypeScript types from OpenAPI schemas
8. 🔄 Deploy to staging environment

---

**Version:** 2.0  
**Last Updated:** January 21, 2026  
**Status:** ✅ Production Ready
