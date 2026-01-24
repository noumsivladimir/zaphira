# API Gateway v1 - Complete Configuration & Integration Guide

## Table of Contents
1. [Overview](#overview)
2. [Architecture Design](#architecture-design)
3. [API Versioning Strategy](#api-versioning-strategy)
4. [Route Configuration](#route-configuration)
5. [Security & CORS](#security--cors)
6. [Circuit Breakers & Resilience](#circuit-breakers--resilience)
7. [Testing & Validation](#testing--validation)
8. [Frontend Integration](#frontend-integration)
9. [Production Deployment](#production-deployment)
10. [Troubleshooting](#troubleshooting)

---

## Overview

The Zaphira Platform API Gateway provides a unified entry point for all microservices with:
- ✅ **API Versioning**: `/api/v1/*` pattern for future-proof evolution
- ✅ **Service Discovery**: Automatic routing via Eureka
- ✅ **CORS Configuration**: Pre-configured for React/Vite/Angular dev servers
- ✅ **Circuit Breakers**: Resilience4j integration for fault tolerance
- ✅ **Path Rewriting**: Transparent routing without controller changes
- ✅ **Observability**: Actuator endpoints for monitoring

### Technology Stack
- **Spring Boot**: 3.2.3
- **Spring Cloud Gateway**: 2023.0.0
- **Eureka Client**: Service Discovery
- **Resilience4j**: Circuit Breakers & Time Limiters
- **Actuator**: Health checks & metrics

---

## Architecture Design

### Request Flow
```
Frontend (localhost:3000)
    ↓
    → POST http://localhost:8080/api/v1/auth/login
    ↓
API Gateway (port 8080)
    ├─ CORS validation ✓
    ├─ Path rewrite: /api/v1/auth/login → /api/auth/login
    ├─ Service discovery: Eureka lookup for "auth-service"
    ├─ Circuit breaker: Check authServiceCircuitBreaker
    ↓
Auth Service (port 8081)
    ├─ Controller: @RequestMapping("/api/auth")
    ├─ Method: @PostMapping("/login")
    ├─ Processes: POST /api/auth/login
    ↓
Response (JWT Token) → Gateway → Frontend
```

### Service Topology
```
┌─────────────────────────────────────────────────────────┐
│              Eureka Service Registry (8761)             │
└─────────────────────────────────────────────────────────┘
                            ↑
                            │ Registration
                            │
┌─────────────────────────────────────────────────────────┐
│              API Gateway (8080)                         │
│  Routes:                                                │
│  - /api/v1/auth/**        → auth-service                │
│  - /api/v1/users/**       → user-service                │
│  - /api/v1/wallets/**     → wallet-service              │
│  - /api/v1/notifications/** → notification-service      │
└─────────────────────────────────────────────────────────┘
    ↓              ↓              ↓              ↓
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌──────────────┐
│  Auth   │  │  User   │  │ Wallet  │  │Notification  │
│ Service │  │ Service │  │ Service │  │   Service    │
│  8081   │  │  8082   │  │  8083   │  │    8089      │
└─────────┘  └─────────┘  └─────────┘  └──────────────┘
```

---

## API Versioning Strategy

### Why `/api/v1` Pattern?
1. **Future-Proof**: Easy to introduce v2, v3 without breaking existing clients
2. **Clear Separation**: Explicit version in URL path
3. **No Controller Changes**: RewritePath filter maintains backward compatibility

### Path Transformation
| Client Request          | Gateway Rewrite      | Controller Mapping     |
|-------------------------|----------------------|------------------------|
| `/api/v1/auth/login`    | `/api/auth/login`    | `@RequestMapping("/api/auth")` |
| `/api/v1/users/profile` | `/api/users/profile` | `@RequestMapping("/api/users")` |
| `/api/v1/wallets/123`   | `/api/wallets/123`   | `@RequestMapping("/api/wallets")` |

### RewritePath Filter Explained
```yaml
filters:
  - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
```
- **Regex Capture**: `(?<segment>.*)` captures everything after `/api/v1/`
- **Replacement**: `/api/${segment}` reconstructs path with `/api` prefix
- **Example**: `/api/v1/auth/login` → Captures `auth/login` → Replaces as `/api/auth/login`

---

## Route Configuration

### Complete Route Mapping

#### 1. Auth Service Routes
```yaml
# Authentication & JWT Management
- id: auth-service
  uri: lb://auth-service
  predicates:
    - Path=/api/v1/auth/**
  filters:
    - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
    - name: CircuitBreaker
      args:
        name: authServiceCircuitBreaker
        fallbackUri: forward:/fallback/auth
  metadata:
    response-timeout: 5000
    connect-timeout: 3000
```

**Endpoints**:
- `POST /api/v1/auth/login` - User authentication
- `POST /api/v1/auth/refresh` - JWT token refresh
- `POST /api/v1/auth/logout` - User logout
- `GET /api/v1/auth/validate` - Token validation

**Target Controller**: `@RequestMapping("/api/auth")`

---

#### 2. Activity Logs Route
```yaml
# Activity Logs (Auth Service)
- id: auth-service-logs
  uri: lb://auth-service
  predicates:
    - Path=/api/v1/logs/**
  filters:
    - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
```

**Endpoints**:
- `GET /api/v1/logs/activity` - Fetch user activity logs
- `POST /api/v1/logs/activity` - Create activity log

**Target Controller**: `@RequestMapping("/api/logs")`

---

#### 3. User Service Routes
```yaml
# User Management & Registration
- id: user-service
  uri: lb://user-service
  predicates:
    - Path=/api/v1/users/**
  filters:
    - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
    - name: CircuitBreaker
      args:
        name: userServiceCircuitBreaker
        fallbackUri: forward:/fallback/users
  metadata:
    response-timeout: 8000
    connect-timeout: 3000
```

**Endpoints**:
- `POST /api/v1/users/register` - User registration
- `GET /api/v1/users/{id}` - Get user profile
- `PUT /api/v1/users/{id}` - Update user profile
- `DELETE /api/v1/users/{id}` - Delete user

**Target Controller**: `@RequestMapping("/api/users")`

---

#### 4. PIN Reset Route
```yaml
# PIN Reset Management (User Service)
- id: user-service-pin-reset
  uri: lb://user-service
  predicates:
    - Path=/api/v1/pin-reset/**
  filters:
    - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
```

**Endpoints**:
- `POST /api/v1/pin-reset/request` - Request PIN reset
- `POST /api/v1/pin-reset/verify` - Verify reset OTP
- `POST /api/v1/pin-reset/confirm` - Confirm new PIN

**Target Controller**: `@RequestMapping("/api/pin-reset")`

---

#### 5. Security Questions Route
```yaml
# Security Questions (User Service)
- id: user-service-security-questions
  uri: lb://user-service
  predicates:
    - Path=/api/v1/security-questions/**
  filters:
    - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
```

**Endpoints**:
- `GET /api/v1/security-questions` - List available questions
- `POST /api/v1/security-questions/setup` - Setup user security questions
- `POST /api/v1/security-questions/verify` - Verify answers

**Target Controller**: `@RequestMapping("/api/security-questions")`

---

#### 6. Wallet Service Routes
```yaml
# Primary Wallet Management
- id: wallet-service
  uri: lb://wallet-service
  predicates:
    - Path=/api/v1/wallets/**
  filters:
    - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
    - name: CircuitBreaker
      args:
        name: walletServiceCircuitBreaker
        fallbackUri: forward:/fallback/wallets
  metadata:
    response-timeout: 6000
    connect-timeout: 3000
```

**Endpoints**:
- `GET /api/v1/wallets/user/{userId}` - Get user wallet
- `GET /api/v1/wallets/{walletNumber}` - Get wallet by number
- `POST /api/v1/wallets/transaction` - Process transaction
- `GET /api/v1/wallets/{number}/balance` - Get wallet balance

**Target Controller**: `@RequestMapping("/api/wallets")`

---

#### 7. Wallet Extended Routes
```yaml
# Permissions & SubWallets (Wallet Service)
- id: wallet-service-extended
  uri: lb://wallet-service
  predicates:
    - Path=/api/v1/wallet/**
  filters:
    - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
```

**Endpoints**:
- `POST /api/v1/wallet/permission/grant` - Grant wallet permission
- `POST /api/v1/wallet/permission/revoke` - Revoke permission
- `POST /api/v1/wallet/subWallet/create` - Create sub-wallet
- `GET /api/v1/wallet/subWallet/list` - List sub-wallets

**Target Controllers**: 
- `@RequestMapping("/api/wallet/permission")`
- `@RequestMapping("/api/wallet/subWallet")`

---

#### 8. Notification Service Route
```yaml
# SMS & Email Notifications
- id: notification-service
  uri: lb://notification-service
  predicates:
    - Path=/api/v1/notifications/**
  filters:
    - RewritePath=/api/v1/(?<segment>.*), /api/${segment}
  metadata:
    response-timeout: 10000
    connect-timeout: 3000
```

**Endpoints**:
- `POST /api/v1/notifications/send` - Send notification
- `GET /api/v1/notifications/{id}` - Get notification status
- `GET /api/v1/notifications/user/{userId}` - Get user notifications

**Target Controller**: `@RequestMapping("/api/notifications")`

---

## Security & CORS

### CORS Configuration
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "http://localhost:3000"        # React dev
              - "http://localhost:5173"        # Vite dev
              - "http://localhost:4200"        # Angular dev
              - "https://zaphira.com"          # Production
              - "https://app.zaphira.com"      # App subdomain
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
              - OPTIONS
            allowed-headers:
              - "*"
            exposed-headers:
              - Authorization
              - Content-Type
              - X-Total-Count
              - X-Page-Number
            allow-credentials: true
            max-age: 3600
```

### Key CORS Features
- **Preflight Requests**: Handles `OPTIONS` automatically
- **Credentials**: Supports cookies and JWT tokens
- **Custom Headers**: Exposes pagination headers
- **Cache**: 1-hour preflight cache for performance

### Adding JWT Validation at Gateway (Future Enhancement)
```yaml
# Recommended: Add JWT filter to all protected routes
filters:
  - name: JwtAuthenticationFilter
    args:
      jwtSecret: ${JWT_SECRET}
      excludePaths: /api/v1/auth/login,/api/v1/auth/register
```

---

## Circuit Breakers & Resilience

### Resilience4j Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      authServiceCircuitBreaker:
        register-health-indicator: true
        sliding-window-size: 10              # Track last 10 requests
        minimum-number-of-calls: 5           # Min calls before opening
        failure-rate-threshold: 50           # Open at 50% failure
        wait-duration-in-open-state: 10s     # Wait 10s before half-open
        slow-call-duration-threshold: 5s     # Consider >5s as slow
```

### Circuit Breaker States
1. **CLOSED**: Normal operation, requests flow through
2. **OPEN**: Service failing, requests blocked (fallback triggered)
3. **HALF-OPEN**: Testing recovery, limited requests allowed

### Service-Specific Timeouts
| Service      | Circuit Breaker Name          | Response Timeout | Connect Timeout |
|--------------|-------------------------------|------------------|-----------------|
| Auth Service | authServiceCircuitBreaker     | 5000ms           | 3000ms          |
| User Service | userServiceCircuitBreaker     | 8000ms           | 3000ms          |
| Wallet Service | walletServiceCircuitBreaker | 6000ms           | 3000ms          |

### Monitoring Circuit Breakers
```bash
# Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers

# Example response
{
  "circuitBreakers": [
    {
      "name": "authServiceCircuitBreaker",
      "state": "CLOSED",
      "metrics": {
        "failureRate": "0.0%",
        "slowCallRate": "0.0%"
      }
    }
  ]
}
```

---

## Testing & Validation

### Quick Start Testing

#### 1. Start Services
```powershell
# Terminal 1: Eureka
cd backend
mvnw spring-boot:run -pl service-registry

# Terminal 2: Auth Service
mvnw spring-boot:run -pl auth-service

# Terminal 3: User Service
mvnw spring-boot:run -pl user-service

# Terminal 4: Wallet Service
mvnw spring-boot:run -pl wallet-service

# Terminal 5: API Gateway
mvnw spring-boot:run -pl api-gateway
```

#### 2. Run Validation Script
```powershell
# Automated testing
.\test-gateway-v1-routes.ps1
```

### Manual Testing with cURL

#### Test Gateway Health
```bash
curl http://localhost:8080/actuator/health
```

#### Test Auth Service via Gateway
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "password123"
  }'
```

#### Test User Registration
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

#### Test Wallet Retrieval
```bash
curl http://localhost:8080/api/v1/wallets/user/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Test CORS Preflight
```bash
curl -X OPTIONS http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

### Gateway Introspection

#### List All Routes
```bash
curl http://localhost:8080/actuator/gateway/routes | jq
```

#### Refresh Routes (After Config Change)
```bash
curl -X POST http://localhost:8080/actuator/gateway/refresh
```

---

## Frontend Integration

### React/Next.js Example
```typescript
// api/client.ts
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const API_VERSION = '/api/v1';

export const apiClient = axios.create({
  baseURL: `${API_BASE_URL}${API_VERSION}`,
  timeout: 10000,
  withCredentials: true, // For cookies/sessions
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add JWT interceptor
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Example: Login
async function login(email: string, password: string) {
  const response = await apiClient.post('/auth/login', {
    username: email,
    password,
  });
  return response.data;
}

// Example: Get User Profile
async function getUserProfile(userId: string) {
  const response = await apiClient.get(`/users/${userId}`);
  return response.data;
}

// Example: Get Wallet Balance
async function getWalletBalance(walletNumber: string) {
  const response = await apiClient.get(`/wallets/${walletNumber}/balance`);
  return response.data;
}
```

### Environment Variables
```env
# .env.local (Development)
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_API_VERSION=v1

# .env.production
NEXT_PUBLIC_API_URL=https://api.zaphira.com
NEXT_PUBLIC_API_VERSION=v1
```

### Error Handling
```typescript
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Unauthorized - redirect to login
      window.location.href = '/login';
    } else if (error.response?.status === 503) {
      // Service unavailable - circuit breaker open
      console.error('Service temporarily unavailable');
    }
    return Promise.reject(error);
  }
);
```

---

## Production Deployment

### Environment Variables
```yaml
# application-prod.yml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "https://zaphira.com"
              - "https://app.zaphira.com"

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL}

logging:
  level:
    org.springframework.cloud.gateway: INFO
    com.zaphira: INFO
```

### Docker Deployment
```dockerfile
# Dockerfile (api-gateway)
FROM openjdk:17-slim
WORKDIR /app
COPY target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes Configuration
```yaml
# api-gateway-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: zaphira/api-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: EUREKA_SERVER_URL
          value: "http://eureka-service:8761/eureka/"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway-service
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: api-gateway
```

### Health Checks
- **Liveness Probe**: `/actuator/health/liveness`
- **Readiness Probe**: `/actuator/health/readiness`

---

## Troubleshooting

### Common Issues

#### 1. 404 Not Found on `/api/v1/*` Routes
**Symptoms**: Gateway returns 404 for all v1 routes

**Causes**:
- Gateway not registered with Eureka
- Incorrect service names in `uri: lb://service-name`
- Target services not running

**Solutions**:
```bash
# Check Eureka registry
curl http://localhost:8761/eureka/apps | jq

# Check Gateway routes
curl http://localhost:8080/actuator/gateway/routes

# Verify service names match Eureka registration
# In service application.yml:
spring:
  application:
    name: auth-service  # Must match lb://auth-service
```

---

#### 2. CORS Errors in Browser
**Symptoms**: `Access-Control-Allow-Origin` header missing

**Causes**:
- Origin not in `allowed-origins` list
- Preflight request failing
- Duplicate CORS headers (Gateway + Service)

**Solutions**:
```yaml
# Add origin to Gateway CORS config
allowed-origins:
  - "http://localhost:5173"  # Your frontend URL

# Remove CORS config from microservices
# CORS should ONLY be handled at Gateway level
```

---

#### 3. Circuit Breaker Opens Immediately
**Symptoms**: Requests fail with circuit breaker open message

**Causes**:
- Service response time exceeds timeout
- Service health check failing
- Too aggressive failure threshold

**Solutions**:
```yaml
# Increase timeout
metadata:
  response-timeout: 10000  # 10 seconds

# Relax circuit breaker
resilience4j:
  circuitbreaker:
    instances:
      myServiceCircuitBreaker:
        failure-rate-threshold: 70  # 70% failure before opening
        minimum-number-of-calls: 10  # More calls before decision
```

---

#### 4. Path Rewrite Not Working
**Symptoms**: Service receives `/api/v1/auth/login` instead of `/api/auth/login`

**Causes**:
- Incorrect RewritePath regex
- Filter order issue
- Multiple conflicting filters

**Solutions**:
```yaml
# Verify RewritePath syntax
filters:
  - RewritePath=/api/v1/(?<segment>.*), /api/${segment}

# Test with curl verbose
curl -v http://localhost:8080/api/v1/auth/login

# Check Gateway logs
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
```

---

#### 5. Service Discovery Not Working
**Symptoms**: Gateway cannot resolve `lb://service-name`

**Causes**:
- Eureka server not running
- Service not registered with Eureka
- Network issues between Gateway and Eureka

**Solutions**:
```bash
# Check Eureka is running
curl http://localhost:8761

# Check Gateway Eureka client
curl http://localhost:8080/actuator/health

# Expected response
{
  "status": "UP",
  "components": {
    "discoveryComposite": {
      "status": "UP"
    }
  }
}
```

---

### Debugging Commands

```bash
# Enable DEBUG logging
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY=DEBUG

# Watch Gateway logs
tail -f logs/api-gateway.log

# Test service directly (bypass Gateway)
curl http://localhost:8081/api/auth/actuator/health

# Test via Gateway
curl http://localhost:8080/api/v1/auth/actuator/health

# Compare responses
```

---

## Performance Optimization

### 1. Connection Pooling
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 100
          max-idle-time: 30s
```

### 2. Response Caching (Future)
```yaml
# Add Redis cache for GET requests
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10
      redis-rate-limiter.burstCapacity: 20
```

### 3. Request Rate Limiting
```yaml
# Prevent abuse
filters:
  - name: RequestRateLimiter
    args:
      key-resolver: "#{@userKeyResolver}"
      redis-rate-limiter.replenishRate: 100
      redis-rate-limiter.burstCapacity: 200
```

---

## Appendix: Complete Configuration

See [application.yml](backend/api-gateway/src/main/resources/application.yml) for the complete production-ready configuration.

### Quick Reference

| Component | Port | Health Check | Registry |
|-----------|------|--------------|----------|
| API Gateway | 8080 | `/actuator/health` | ✅ Eureka |
| Auth Service | 8081 | `/api/auth/actuator/health` | ✅ Eureka |
| User Service | 8082 | `/api/users/actuator/health` | ✅ Eureka |
| Wallet Service | 8083 | `/api/wallets/actuator/health` | ✅ Eureka |
| Notification Service | 8089 | `/api/notifications/actuator/health` | ✅ Eureka |

---

## Support & Contributing

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review Gateway logs: `tail -f logs/api-gateway.log`
3. Run validation script: `.\test-gateway-v1-routes.ps1`
4. Consult Spring Cloud Gateway docs: https://spring.io/projects/spring-cloud-gateway

---

**Last Updated**: January 2025  
**Version**: 1.0.0  
**Maintainer**: Zaphira Platform Team
