# Zaphira Backend - Deep Analysis & Production Integration Strategy

**Date:** January 21, 2026  
**Status:** ✅ Complete Analysis (Updated)  
**Architect:** Senior Backend Integration Specialist

---

## 📋 EXECUTIVE SUMMARY

This document provides a comprehensive analysis of the Zaphira Spring Boot microservices backend and defines a production-ready, incremental integration strategy optimized for frontend consumption via a single API Gateway.

**Key Findings:**
- **7 active microservices** (9 modules total, 2 inactive: config-server, transaction-service)
- **HYBRID architecture**: Synchronous (Feign) + Asynchronous (Kafka) communication
- **⚠️ CRITICAL FINDING:** Wallet creation is **SYNCHRONOUS via Feign**, NOT async via Kafka
- **Critical dependency chain**: user-service → wallet-service (MANDATORY at runtime)
- **Kafka is OPTIONAL**: Only required for SMS/Email notifications
- **Recommended first phase**: auth-service + api-gateway + eureka (3 services for login only)
- **Recommended MVP phase**: 5 services (add user-service + wallet-service for registration)

---

## 1. DEEP BACKEND ANALYSIS

### 1.1 Microservices Inventory & Responsibilities

| Service | Port | Responsibility | Database | Type |
|---------|------|----------------|----------|------|
| **api-gateway** | 8080 | Single entry point, routing, CORS | None | Infrastructure |
| **service-registry** (Eureka) | 8761 | Service discovery | None | Infrastructure |
| **config-server** | 8888 | Centralized configuration | None | Infrastructure |
| **auth-service** | 8081 | Login, JWT tokens, OTP verification | wallet_db (shared) | Core |
| **user-service** | 8082 | User registration, profiles, email verification | wallet_db (shared) | Core |
| **wallet-service** | 8083 | Wallet creation, balance management | wallet_db (shared) | Business |
| **transaction-service** | 8085 | P2P transfers, transaction history | transaction_db | Business |
| **notification-service** | 8089 | Email, SMS, OTP via Twilio | wallet_db (shared) | Support |

**⚠️ Architecture Note:** Multiple services share the same database (`wallet_db`), which creates tight coupling at the data layer despite microservices separation.

### 1.2 Inter-Service Communication Patterns

#### A. **Synchronous (Feign Clients)**

```
user-service → wallet-service (WalletServiceClient)
  └─ POST /api/wallets/auto-create/{userId}
  └─ GET /api/wallets/user/{userId}/primary

wallet-service → transaction-service (TransactionServiceClient)
  └─ POST /api/transactions

wallet-service → user-service (UserServiceClient)
  └─ GET /api/users/{id}

transaction-service → wallet-service (FeignWalletClient)
  └─ POST /api/wallets/transfer
  └─ GET /api/wallets/{walletNumber}

notification-service → user-service (UserServiceClient)
  └─ GET /api/users/{userId}/notification-info
```

#### B. **Asynchronous (Kafka Events)**

**⚠️ IMPORTANT:** Kafka is currently used **ONLY for notifications**, NOT for wallet creation.

```
Kafka Broker: 192.168.0.122:9092

user-service → Kafka:
  ✉ user-registered (topic)
    └─ UserRegisteredEvent (for notifications)

notification-service ← Kafka:
  ✉ user-registered → Welcome SMS/Email
  ✉ OTP events → OTP SMS delivery

wallet-service ← Kafka:
  ✉ [COMMENTED OUT] UserEventListener is disabled
  └─ Kafka listener NOT active - wallet creation via Feign instead

transaction-service → Kafka:
  ✉ transaction-created, transaction-completed

notification-service ← Kafka:
  ✉ transaction-created → Transaction notifications
  ✉ transaction-completed → Confirmation emails
```

**Key Implications:**
- Kafka can be disabled for core user registration flow
- Kafka is REQUIRED only if SMS/Email notifications are needed
- Wallet creation is **synchronous and blocking** via Feign HTTP calls

---

## 2. CRITICAL DEPENDENCY MAP

### 2.0 Visual Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND                                │
│                     (TypeScript/React)                          │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP/REST
                            ▼
                    ┌───────────────┐
                    │  API Gateway  │ :8080 ◄─── ONLY EXPOSED PORT
                    │   (Spring     │
                    │    Cloud      │
                    │   Gateway)    │
                    └───────┬───────┘
                            │
                    ┌───────▼────────┐
                    │    EUREKA      │ :8761
                    │  (Service      │
                    │  Discovery)    │
                    └────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ auth-service │    │ user-service │    │ wallet-srv   │
│    :8081     │    │    :8082     │───▶│    :8083     │
│              │    │              │    │              │
│ ✅ LOGIN     │    │ ✅ REGISTER  │    │ ✅ WALLETS   │
│ ✅ JWT       │    │ ✅ PROFILE   │    │              │
└──────────────┘    └───────┬──────┘    └──────────────┘
                            │ Feign (SYNC)
                            │ createWallet()
                            │
                    ┌───────▼──────────┐
                    │ Kafka Broker     │ :9092
                    │ (OPTIONAL)       │
                    └───────┬──────────┘
                            │
                    ┌───────▼──────────┐
                    │ notification-srv │ :8089
                    │ (SMS/Email OTP)  │
                    │ (OPTIONAL)       │
                    └──────────────────┘

                    ┌──────────────────┐
                    │  PostgreSQL      │ :5432
                    │  wallet_db       │
                    │  (SHARED DB)     │
                    └──────────────────┘
```

**Legend:**
- `───▶` : Synchronous HTTP call (Feign)
- `│` : Service registration with Eureka
- `✅` : Core functionality

### 2.1 Service Dependency Graph

```
                    ┌───────────────┐
                    │  Frontend     │
                    │  (TypeScript) │
                    └───────┬───────┘
                            │ HTTP
                            ▼
                    ┌───────────────┐
                    │ API Gateway   │ :8080
                    │ (ONLY exposed)│
                    └───────┬───────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ auth-service │    │ user-service │    │ wallet-srv   │
│    :8081     │    │    :8082     │    │    :8083     │
└──────────────┘    └───────┬──────┘    └──────────────┘
                            │
                    ┌───────▼──────────┐
                    │ wallet-service   │ (Feign sync)
                    │ MANDATORY        │
                    └──────────────────┘
                            │
                    ┌───────▼──────────┐
                    │ Kafka Broker     │ (async)
                    │ :9092            │
                    └───────┬──────────┘
                            │
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
    ┌──────────┐   ┌──────────────┐  ┌──────────────┐
    │ wallet   │   │ notification │  │ transaction  │
    │ listener │   │   service    │  │   service    │
    └──────────┘   └──────────────┘  └──────────────┘
```

### 2.2 Dependency Classification

#### **REQUIRED Services (Tier 1 - Must Run)**
1. **api-gateway** - Single entry point for frontend
2. **service-registry** (Eureka) - Service discovery (Feign depends on it)
3. **user-service** - Core user registration & authentication
4. **auth-service** - JWT token issuance & validation

#### **CONDITIONALLY REQUIRED (Tier 2 - Depends on Use Case)**
5. **wallet-service** - **REQUIRED** for user registration flow
   - User registration → creates wallet automatically via Kafka
   - UserRegistrationServiceImpl publishes `UserRegisteredEvent`
   - wallet-service consumes event and creates default wallet

6. **notification-service** - **OPTIONAL** for core flows
   - Sends welcome SMS/email
   - OTP generation for verification
   - Can be mocked or disabled for early integration

#### **OPTIONAL Services (Tier 3 - Can Be Deferred)**
7. **transaction-service** - P2P transfers (not needed for login/registration)
8. **config-server** - Centralized config (can use local application.properties)

---

## 3. FUNCTIONAL DEPENDENCY ANALYSIS

### 3.1 User Registration Flow (CRITICAL PATH)

**⚠️ CRITICAL FINDING: Wallet Creation is SYNCHRONOUS (Not Kafka-based)**

The wallet-service Kafka listener (`UserEventListener.java`) is **COMMENTED OUT**, meaning wallet creation happens via **direct Feign client calls**, not asynchronous messaging.

```
1. Frontend → API Gateway → user-service
   POST /api/users/register
   {
     "phoneNumber": "+237690123456",
     "email": "user@example.com",
     "pin": "1234",
     "firstName": "John",
     "lastName": "Doe"
   }

2. user-service:
   ✅ Validate user doesn't exist
   ✅ Hash PIN
   ✅ Save user (status: PENDING_VERIFICATION)
   ✅ Generate OTP
   ✅ Return response (walletId: null at this stage)

3. notification-service (Kafka listener):
   ✅ Send OTP via SMS (Twilio)

4. Frontend → user-service
   POST /api/users/verify-email
   {
     "userId": 123,
     "verificationCode": "123456"
   }

5. user-service (after OTP verification):
   ✅ Verify OTP
   ✅ Update status → ACTIVE
   ✅ **SYNCHRONOUS CALL:** walletServiceClient.createWallet(userId)
      → Feign HTTP call to wallet-service
      → Creates default wallet (XOF, 0 balance)
      → Returns wallet number immediately
   ✅ Link wallet to user
   ✅ Return complete user + wallet

6. wallet-service:
   ✅ Receives POST /api/wallets?userId={userId}
   ✅ Validates user doesn't already have wallet
   ✅ Creates wallet (balance: 0, currency: XOF, type: PRIMARY)
   ✅ Saves to database
   ✅ Returns WalletResponse (walletNumber, userId, balance, currency)
```

**⚠️ CRITICAL DEPENDENCIES:**  
- **user-service REQUIRES wallet-service to be RUNNING** during email verification flow
- **SYNCHRONOUS blocking call** - if wallet-service is DOWN → email verification returns 503
- Circuit breaker is configured with **Resilience4j** but provides **degraded fallback** (mock wallet response)
- **NO Kafka required** for core registration flow (Kafka only used for notifications)

### 3.2 Login Flow (MINIMAL PATH)

```
1. Frontend → API Gateway → auth-service
   POST /api/auth/login
   {
     "phoneNumber": "+237690123456",
     "pin": "1234"
   }

2. auth-service:
   ✅ Verify credentials (database lookup)
   ✅ Generate JWT access + refresh tokens
   ✅ Return tokens

3. Frontend stores JWT
4. Subsequent requests include:
   Authorization: Bearer <JWT_TOKEN>
```

**✅ MINIMAL DEPENDENCY:**  
- auth-service ONLY needs database access
- NO dependency on wallet-service for login
- NO Kafka dependency

### 3.3 Startup Dependencies

#### **Must Start BEFORE Others:**
1. **service-registry** (Eureka) - Port 8761
   - ALL services register with Eureka
   - Feign clients use Eureka for service discovery

2. **Kafka Broker** - 192.168.0.122:9092
   - wallet-service will FAIL to start if Kafka is unavailable
   - notification-service will NOT receive events

#### **Can Start in Any Order (After Eureka):**
- api-gateway
- auth-service
- user-service
- wallet-service
- notification-service

---

## 4. INCREMENTAL INTEGRATION STRATEGY

### 4.1 Phase 0: Infrastructure Setup ⚙️

**Objective:** Prepare runtime environment

**Required Actions:**
```bash
# 1. Start PostgreSQL
psql -U postgres
CREATE DATABASE wallet_db;
CREATE DATABASE transaction_db;

# 2. Start Kafka (if using wallet-service)
docker run -d --name kafka \
  -p 9092:9092 \
  apache/kafka:latest

# 3. Start Eureka (service-registry)
cd services-tree/service-registry
mvn spring-boot:run
```

**Validation:**
- Eureka Dashboard: http://localhost:8761
- PostgreSQL accessible on port 5432

---

### 4.2 Phase 1: MINIMAL VIABLE INTEGRATION 🚀

**Goal:** Enable login ONLY (no registration yet)

#### **Services to Start (3 core services):**
1. ✅ **service-registry** (Eureka) - :8761
2. ✅ **auth-service** - :8081
3. ✅ **api-gateway** - :8080

**Database Required:** PostgreSQL (wallet_db) - auth-service stores users

#### **Frontend Endpoints Available:**
```
POST /api/auth/login         → Login (JWT tokens)
POST /api/auth/refresh       → Refresh JWT
GET  /api/auth/validate      → Validate JWT
```

#### **Startup Commands:**
```bash
# Terminal 1: Start Eureka
cd backend/service-registry
mvn spring-boot:run

# Wait 30 seconds for Eureka to fully start

# Terminal 2: Start Auth Service
cd backend/auth-service
mvn spring-boot:run

# Terminal 3: Start API Gateway
cd backend/api-gateway
mvn spring-boot:run
```

#### **Configuration:**
```yaml
# api-gateway/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
```

**⚠️ Limitations:**
- ❌ No user registration (requires user-service + wallet-service)
- ❌ No wallet operations
- ❌ Must pre-create test users in database manually

**✅ Advantages:**
- Fastest to deploy (3 services only)
- Zero functional dependencies (no Kafka needed)
- Perfect for authentication testing
- Can test JWT issuance and validation immediately

**Manual Test User Creation:**
```sql
-- Connect to PostgreSQL
psql -U postgres -d wallet_db

-- Insert test user (password: "1234")
INSERT INTO users (user_type, phone_number, pin, first_name, last_name, 
                   account_status, role_type, registration_date)
VALUES ('REGULAR', '+237690123456', 
        '$2a$10$hashedPinHere', 'John', 'Doe', 
        'ACTIVE', 'USER', NOW());
```

---

### 4.3 Phase 2: REGISTRATION INTEGRATION 📝

**Goal:** Enable full user registration + email/SMS verification

#### **Additional Services Required:**
4. ✅ **user-service** - :8082
5. ✅ **wallet-service** - :8083 (MANDATORY - called synchronously via Feign)
6. ⚠️ **notification-service** - :8089 (OPTIONAL - for OTP SMS/Email)

**Infrastructure Required:**
- PostgreSQL (wallet_db) - Already running from Phase 1
- ⚠️ Kafka (192.168.0.122:9092) - ONLY if using notification-service

#### **Frontend Endpoints Available:**
```
POST /api/users/register           → User registration
POST /api/users/verify-email       → Email/OTP verification (creates wallet here)
GET  /api/users/profile            → Get user profile
PUT  /api/users/profile            → Update profile
GET  /api/users/{id}               → Get user by ID
```

#### **Startup Order (CRITICAL):**
```bash
# 1. Start Eureka (if not already running)
cd backend/service-registry
mvn spring-boot:run
# Wait 30 seconds

# 2. Start Wallet Service (MUST be UP before user-service)
cd backend/wallet-service
mvn spring-boot:run
# Wait 20 seconds until you see "Started WalletServiceApplication"

# 3. Start User Service (depends on wallet-service)
cd backend/user-service
mvn spring-boot:run

# 4. (Optional) Start Notification Service for OTP
cd backend/notification-service
mvn spring-boot:run

# 5. Start Auth Service (if not already running)
cd backend/auth-service
mvn spring-boot:run

# 6. Start API Gateway (LAST)
cd backend/api-gateway
mvn spring-boot:run
```

**Why wallet-service MUST start before user-service:**
- user-service has a `WalletServiceClient` Feign client
- During email verification, user-service calls `walletServiceClient.createWallet(userId)`
- If wallet-service is DOWN → Circuit breaker activates → Fallback returns mock wallet
- Application will start but wallet creation will fail

**Environment Variables Required:**
```properties
# user-service/application.properties
wallet.service.url=http://localhost:8083
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

# wallet-service/application.yml
user.service.url=http://localhost:8082
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

# notification-service/application.yml (ONLY if using notifications)
spring.kafka.bootstrap-servers=192.168.0.122:9092
twilio.account-sid=ACxxxx
twilio.auth-token=xxxx
twilio.from-number=+1234567890
```

#### **Testing Without Notifications:**
You can skip notification-service if you don't need SMS/Email OTP. In this case:

1. User registration succeeds but NO OTP is sent
2. Use a mock/bypass verification endpoint (if available)
3. OR manually verify users in database:

```sql
UPDATE users 
SET account_status = 'ACTIVE' 
WHERE phone_number = '+237690123456';
```

#### **Key Architectural Note:**
- Wallet creation is **SYNCHRONOUS via Feign**, not asynchronous via Kafka
- This means user-service **blocks** waiting for wallet-service response
- Typical response time: 200-500ms
- If wallet-service is slow/down: 5-10 second timeout → Circuit breaker activates

---

### 4.4 Phase 3: FULL PLATFORM 💼

**Goal:** Enable all business features

#### **Additional Services:**
7. ✅ **transaction-service** - :8085 (P2P transfers)

#### **Frontend Endpoints Available:**
```
POST /api/transactions/transfer   → P2P transfers
GET  /api/transactions/history    → Transaction history
GET  /api/wallets/{number}        → Wallet details
```

---

## 5. API GATEWAY & FRONTEND CONSUMPTION

### 5.1 Base API URL

```
Production:  https://api.zaphira.cm
Development: http://localhost:8080
```

### 5.2 Gateway Routes Configuration

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "http://localhost:3000"
              - "http://localhost:5173"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowed-headers:
              - Authorization
              - Content-Type
            exposed-headers:
              - Authorization
            allow-credentials: true
            max-age: 3600

      routes:
        # Authentication
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1

        # User Management
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1

        # Wallet Operations
        - id: wallet-service
          uri: lb://wallet-service
          predicates:
            - Path=/api/wallets/**
          filters:
            - StripPrefix=1

        # Transactions
        - id: transaction-service
          uri: lb://transaction-service
          predicates:
            - Path=/api/transactions/**
          filters:
            - StripPrefix=1
```

### 5.3 Frontend Routing Table

| Frontend Route | Backend Service | Method | Authentication |
|----------------|----------------|--------|----------------|
| `/api/auth/login` | auth-service | POST | ❌ Public |
| `/api/auth/refresh` | auth-service | POST | ❌ Public |
| `/api/users/register` | user-service | POST | ❌ Public |
| `/api/users/verify-email` | user-service | POST | ❌ Public |
| `/api/users/profile` | user-service | GET | ✅ JWT Required |
| `/api/users/profile` | user-service | PUT | ✅ JWT Required |
| `/api/wallets/{id}` | wallet-service | GET | ✅ JWT Required |
| `/api/transactions/transfer` | transaction-service | POST | ✅ JWT Required |

### 5.4 TypeScript API Client Example

```typescript
// api/client.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Add JWT to all requests
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: Handle token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const { data } = await axios.post(
            'http://localhost:8080/api/auth/refresh',
            { refreshToken }
          );
          localStorage.setItem('accessToken', data.accessToken);
          error.config.headers.Authorization = `Bearer ${data.accessToken}`;
          return apiClient.request(error.config);
        } catch {
          // Redirect to login
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 6. CLEAN INTEGRATION RULES

### 6.1 Service Startup Order (MANDATORY)

**Phase 1 (Login Only):**
```
1. service-registry (Eureka)     → Wait 30s for full startup
2. auth-service                  → Wait 20s
3. api-gateway                   → Start LAST (routes to services)
```

**Phase 2 (Full Registration):**
```
1. service-registry (Eureka)     → Wait 30s for full startup
2. wallet-service                → MUST start BEFORE user-service (Feign dependency)
                                   Wait 20s until "Started WalletServiceApplication"
3. user-service                  → Depends on wallet-service being UP
4. notification-service          → Optional (for SMS/Email OTP)
5. auth-service                  → Independent
6. api-gateway                   → Start LAST (routes to services)
```

**Critical Notes:**
- Eureka MUST be running before any other service
- wallet-service MUST be fully started before user-service attempts to register with Eureka
- Feign clients will fail-fast if target service is not registered in Eureka
- API Gateway should start last to ensure all routes are available

### 6.2 Environment Variables (MANDATORY)

```bash
# ===================================
# DATABASE (All Services)
# ===================================
DB_HOST=192.168.0.122
DB_PORT=5432
DB_NAME=wallet_db
DB_USER=postgres
DB_PASSWORD=1234

# ===================================
# JWT (auth-service)
# ===================================
JWT_SECRET=7pBJFFNs9RzeTwTz/NHFY1e1QyFVnDoZbBTG8zsdGHeAsAzmqcxLneWASllVgTbAdgdrS+XhAR9nfg1hPglA3Q==
JWT_ACCESS_EXPIRATION=900000      # 15 minutes
JWT_REFRESH_EXPIRATION=2592000000 # 30 days

# ===================================
# EUREKA (All Services)
# ===================================
EUREKA_SERVER_URL=http://localhost:8761/eureka/

# ===================================
# FEIGN CLIENT URLS (Critical for Phase 2)
# ===================================
# user-service → wallet-service
WALLET_SERVICE_URL=http://localhost:8083

# wallet-service → user-service
USER_SERVICE_URL=http://localhost:8082

# ===================================
# KAFKA (Optional - Only if using notifications)
# ===================================
KAFKA_BOOTSTRAP_SERVERS=192.168.0.122:9092

# ===================================
# TWILIO (Optional - Only if using notification-service)
# ===================================
TWILIO_ACCOUNT_SID=ACxxxx
TWILIO_AUTH_TOKEN=xxxx
TWILIO_FROM_NUMBER=+1234567890
```

**Critical Notes:**
- `WALLET_SERVICE_URL` in user-service: MUST match actual wallet-service port
- `USER_SERVICE_URL` in wallet-service: Used for validation (less critical)
- Kafka variables ONLY needed if running notification-service
- All services share same `wallet_db` database (tight coupling at data layer)

### 6.3 Fail-Fast Behavior

**Recommended Configuration:**

```properties
# ===================================
# EUREKA CLIENT (All Services)
# ===================================
eureka.client.fail-fast=true
eureka.client.initial-instance-info-replication-interval-seconds=5
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# ===================================
# FEIGN CLIENT CONFIGURATION (user-service)
# ===================================
feign.circuitbreaker.enabled=true
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=10000

# ===================================
# RESILIENCE4J CIRCUIT BREAKER (user-service)
# ===================================
resilience4j.circuitbreaker.instances.walletService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.walletService.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.walletService.sliding-window-size=10
resilience4j.circuitbreaker.instances.walletService.minimum-number-of-calls=5

# ===================================
# RETRY CONFIGURATION (user-service)
# ===================================
resilience4j.retry.instances.walletService.max-attempts=3
resilience4j.retry.instances.walletService.wait-duration=2s
```

**Behavior:**
- **If wallet-service is DOWN during email verification:**
  1. user-service attempts Feign call to `POST /api/wallets?userId={id}`
  2. Feign client times out after 10 seconds (readTimeout)
  3. Retry mechanism attempts 3 times with 2-second delays
  4. After 3 failures, circuit breaker OPENS
  5. Fallback factory returns mock `WalletResponse` with error flag
  6. User verification succeeds but wallet creation is deferred
  7. User's `walletId` field remains `null` in database

- **If Eureka is DOWN:**
  - ALL services fail to start (except Eureka itself)
  - Services log: `Connection refused: connect to http://localhost:8761/eureka`
  - Services exit with `DiscoveryClient registration failed`

- **If Kafka is DOWN:**
  - notification-service fails to start
  - user-service and wallet-service start normally (Kafka NOT required)
  - NO notifications are sent (OTP, welcome emails)

**Circuit Breaker States:**
- **CLOSED:** Normal operation, all calls go through
- **OPEN:** Fast-fail, all calls return fallback immediately (no network call)
- **HALF_OPEN:** Test if service recovered, allow limited calls

### 6.4 Mock Strategy for Non-Essential Services

#### **Option A: Disable Kafka Listeners**

```properties
# notification-service
spring.kafka.enabled=false
```

#### **Option B: Mock Feign Clients**

```java
@Component
public class WalletServiceClientFallback implements WalletServiceClient {
    @Override
    public WalletResponse createWallet(Long userId) {
        return WalletResponse.builder()
            .walletNumber("MOCK-WALLET")
            .userId(userId)
            .balance(BigDecimal.ZERO)
            .currency("XOF")
            .build();
    }
}
```

---

## 7. RECOMMENDED FIRST-PHASE SETUP

### **OPTION A: Minimal (Login Only) - 3 Services**

```
✅ service-registry  :8761
✅ auth-service      :8081
✅ api-gateway       :8080
```

**Use Case:** Authentication testing, JWT validation, rapid prototyping  
**Limitations:** 
- No registration (manual user creation required)
- No wallet operations
- Good for frontend login UI development

**Startup Time:** ~60 seconds  
**Infrastructure:** PostgreSQL only

---

### **OPTION B: Complete Auth + Registration - 5 Services** ⭐ **RECOMMENDED**

```
✅ service-registry      :8761  (Eureka - service discovery)
✅ wallet-service        :8083  (MANDATORY - called by user-service via Feign)
✅ user-service          :8082  (Core registration logic)
✅ auth-service          :8081  (Login, JWT tokens)
✅ api-gateway           :8080  (Single entry point)
```

**Use Case:** Production-ready authentication + registration  
**Advantages:**
- Complete user lifecycle (register → verify → login)
- Real wallet creation (synchronous Feign call)
- Can operate WITHOUT Kafka (notifications disabled)
- Suitable for MVP deployment

**Startup Time:** ~90 seconds  
**Infrastructure:** PostgreSQL only (Kafka optional)

---

### **OPTION C: Full Production - 6 Services**

```
✅ service-registry      :8761
✅ wallet-service        :8083
✅ user-service          :8082
✅ auth-service          :8081
✅ notification-service  :8089  (SMS/Email OTP)
✅ api-gateway           :8080
```

**Use Case:** Full production with SMS/Email OTP verification  
**Advantages:**
- SMS OTP via Twilio
- Email verification
- Welcome messages

**Startup Time:** ~120 seconds  
**Infrastructure:** PostgreSQL + Kafka + Twilio account

**Additional Requirements:**
- Kafka broker running on 192.168.0.122:9092
- Twilio credentials configured
- SMTP server (for emails)

---

## 8. QUICK START GUIDE

### Phase 1: Login Only (3 Services)

```bash
# Terminal 1: Eureka
cd backend/service-registry
mvn clean spring-boot:run

# Wait 30 seconds, verify Eureka dashboard: http://localhost:8761

# Terminal 2: Auth Service
cd backend/auth-service
mvn spring-boot:run

# Terminal 3: API Gateway
cd backend/api-gateway
mvn spring-boot:run

# Test login endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+237690123456", "pin": "1234"}'
```

### Phase 2: Full Registration (5 Services)

```bash
# Terminal 1: Eureka (if not already running)
cd backend/service-registry
mvn clean spring-boot:run

# Wait 30 seconds

# Terminal 2: Wallet Service (MUST start before user-service)
cd backend/wallet-service
mvn spring-boot:run

# Wait 20 seconds until you see "Started WalletServiceApplication"

# Terminal 3: User Service
cd backend/user-service
mvn spring-boot:run

# Terminal 4: Auth Service
cd backend/auth-service
mvn spring-boot:run

# Terminal 5: API Gateway (start LAST)
cd backend/api-gateway
mvn spring-boot:run

# Test registration endpoint
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

## 8. FRONTEND INTEGRATION CHECKLIST

### ✅ Prerequisites
- [ ] API Gateway accessible at http://localhost:8080
- [ ] CORS configured for frontend origin (localhost:3000 / 5173)
- [ ] JWT secret shared between auth-service and frontend

### ✅ Implementation
- [ ] Axios/Fetch configured with base URL: `http://localhost:8080`
- [ ] Request interceptor adds `Authorization: Bearer <token>`
- [ ] Response interceptor handles 401 → refresh token flow
- [ ] Tokens stored securely (httpOnly cookies or localStorage)

### ✅ Endpoints Tested
- [ ] POST `/api/auth/login` → Returns JWT
- [ ] POST `/api/users/register` → Creates user
- [ ] POST `/api/users/verify-email` → Activates account
- [ ] GET `/api/users/profile` → Returns user data (JWT required)

---

## 9. PRODUCTION DEPLOYMENT GUIDELINES

### 9.1 Containerization Strategy

```dockerfile
# Dockerfile (per service)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 9.2 Docker Compose (Development)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: wallet_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: 1234
    ports:
      - "5432:5432"

  kafka:
    image: apache/kafka:latest
    ports:
      - "9092:9092"

  eureka:
    build: ./service-registry
    ports:
      - "8761:8761"

  wallet-service:
    build: ./wallet-service
    depends_on:
      - postgres
      - kafka
      - eureka
    environment:
      EUREKA_SERVER: http://eureka:8761/eureka
      DB_HOST: postgres

  user-service:
    build: ./user-service
    depends_on:
      - wallet-service
      - eureka
    environment:
      WALLET_SERVICE_URL: http://wallet-service:8083

  auth-service:
    build: ./auth
    depends_on:
      - postgres
      - eureka

  api-gateway:
    build: ./api-gateway
    depends_on:
      - eureka
    ports:
      - "8080:8080"
```

### 9.3 Health Checks

```yaml
# Add to each service application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  health:
    circuitbreakers:
      enabled: true
```

---

## 10. TROUBLESHOOTING GUIDE

### Issue: User registration hangs

**Cause:** wallet-service is DOWN or unreachable

**Solution:**
```bash
# Check wallet-service logs
tail -f wallet-service/logs/application.log

# Verify Kafka connection
kafkacat -b 192.168.0.122:9092 -L

# Check circuit breaker status
curl http://localhost:8082/actuator/circuitbreakers
```

### Issue: 401 Unauthorized on protected endpoints

**Cause:** JWT validation failing

**Solution:**
```bash
# Verify JWT secret matches across services
grep JWT_SECRET auth-service/.env
grep JWT_SECRET user-service/.env

# Test JWT manually
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/users/profile
```

### Issue: Eureka shows no registered instances

**Cause:** Services not registering with Eureka

**Solution:**
```properties
# Verify in each service application.properties
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

---

## 11. FINAL RECOMMENDATIONS

### For Initial Frontend Integration:
✅ **Start with Phase 1 (Minimal):**
- api-gateway + auth-service + service-registry
- Manually create test users in database
- Focus on authentication flow first

### For Complete Integration:
✅ **Move to Phase 2:**
- Add user-service + wallet-service + notification-service
- Enable full registration flow
- Configure Kafka + Twilio

### Architecture Improvements (Future):
1. **Database Separation:** Move services to independent databases
2. **API Versioning:** Add `/v1/` prefix to all routes
3. **GraphQL Gateway:** Consider GraphQL for flexible frontend queries
4. **Async Compensation:** Add saga pattern for wallet creation failures

---

## 12. CONCLUSION

**Summary:**
- Backend has **7 active microservices** (9 modules in pom.xml, 2 inactive: config-server, transaction-service)
- **Architecture:** Hybrid synchronous (Feign) + asynchronous (Kafka for notifications only)
- **Minimal integration** requires only **3 services** (gateway, auth, registry) for login
- **Production-ready registration** requires **5 services** including wallet-service
- **Kafka is OPTIONAL** - only needed for SMS/Email notifications
- Frontend should **ONLY call API Gateway** (port 8080)
- JWT Bearer tokens for authentication
- CORS configured at gateway level

**Critical Architectural Findings:**

1. **Wallet Creation is SYNCHRONOUS:**
   - Kafka listener `UserEventListener.java` is **commented out** in wallet-service
   - user-service calls wallet-service via **Feign HTTP client** (blocking call)
   - This creates a hard runtime dependency: user-service REQUIRES wallet-service

2. **Database Coupling:**
   - Multiple services share same database (`wallet_db`)
   - This violates microservices data isolation principle
   - Potential for data consistency issues

3. **Service Discovery via Eureka:**
   - ALL services require Eureka to be running
   - Feign clients use Eureka for service resolution (`lb://service-name`)
   - No direct URL fallback in production mode

4. **Circuit Breaker Protection:**
   - Resilience4j configured on critical Feign clients
   - Fallback factories provide degraded responses
   - Circuit breaker opens after 50% failure rate

**Recommended Integration Path:**

**Week 1: Authentication Only**
- Deploy: Eureka + auth-service + api-gateway (3 services)
- Test: Login, JWT issuance, token refresh
- Frontend: Implement authentication UI
- Manual user creation in database

**Week 2: Full Registration**
- Add: wallet-service + user-service (5 services total)
- Test: User registration, wallet creation, profile management
- Frontend: Implement registration flow
- Skip notification-service initially

**Week 3: Notifications**
- Add: notification-service + Kafka broker
- Test: SMS OTP, email verification, welcome messages
- Configure: Twilio credentials

**Week 4: Transactions**
- Add: transaction-service (if available)
- Test: P2P transfers, transaction history
- Frontend: Implement transaction UI

**Next Steps:**
1. ✅ Deploy Phase 1 (minimal) for authentication testing
2. ✅ Validate JWT flow with frontend TypeScript client
3. ✅ Deploy Phase 2 for full registration
4. 🔄 Generate OpenAPI/Swagger documentation for each service
5. 🔄 Generate TypeScript types from OpenAPI schemas
6. 🔄 Consider migrating to async wallet creation (uncomment Kafka listener)
7. 🔄 Separate databases per service (long-term architecture improvement)

**Performance Expectations:**
- Login: 100-200ms
- Registration: 300-500ms (includes synchronous wallet creation)
- Wallet creation: 150-300ms (Feign HTTP call)
- With circuit breaker open: <50ms (fallback response)

---

**Document Version:** 2.0  
**Last Updated:** January 21, 2026  
**Architect:** Senior Backend Integration Specialist  
**Contact:** Backend Integration Team
