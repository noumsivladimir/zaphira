# Zaphira Platform - Backend

## Architecture Overview

This backend is built as a Spring Boot microservices architecture with a **single API Gateway** serving as the unique entry point for all frontend requests.

**✨ NEW:** Complete integration analysis available in [`BACKEND_ANALYSIS_AND_INTEGRATION_STRATEGY.md`](../BACKEND_ANALYSIS_AND_INTEGRATION_STRATEGY.md)

**🚀 Quick Start:** Use automated startup scripts:
- Phase 1 (Login Only): [`start-backend-phase1.ps1`](../start-backend-phase1.ps1)
- Phase 2 (Full Registration): [`start-backend-phase2.ps1`](../start-backend-phase2.ps1)

### Services

```
backend/
├── service-registry/       # Eureka service discovery :8761
├── api-gateway/            # Single entry point :8080 (ONLY exposed to frontend)
├── auth-service/           # Authentication, login, JWT management :8081
├── user-service/           # User registration, profile management :8082
├── wallet-service/         # Wallet creation, balance management :8083
├── notification-service/   # SMS/Email OTP via Twilio :8089
└── common-library/         # Shared DTOs, utilities, configurations
```

## 🎯 Quick Start

### Phase 1: Login Only (3 Services - 2 Minutes)

```powershell
.\start-backend-phase1.ps1
```

Services: Eureka + auth-service + api-gateway

**Test:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+237690123456", "pin": "1234"}'
```

### Phase 2: Full Registration (5 Services - 3 Minutes) ⭐ RECOMMENDED

```powershell
.\start-backend-phase2.ps1
```

Services: Eureka + wallet-service + user-service + auth-service + api-gateway

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

## API Gateway - Single Entry Point

The **API Gateway** is the **ONLY** service exposed to the frontend. It routes all requests to the appropriate backend services.

### Base URL
```
Development: http://localhost:8080
Production:  https://api.zaphira.cm
```

### Exposed Routes

#### Authentication Endpoints
```
POST   /api/auth/login              # User login
POST   /api/auth/refresh            # Refresh JWT token
GET    /api/auth/validate           # Validate JWT token
```

#### User Management Endpoints
```
POST   /api/users/register          # User registration
POST   /api/users/verify-email      # Email/OTP verification (creates wallet here)
GET    /api/users/profile           # Get user profile (requires JWT)
PUT    /api/users/profile           # Update user profile (requires JWT)
GET    /api/users/{id}              # Get user by ID (requires JWT)
```

## Service Responsibilities

### 🌐 api-gateway (:8080)
- **ONLY exposed port** to frontend
- Routes all requests to internal services
- CORS configuration
- Load balancing

### 🔍 service-registry (:8761)
- Eureka service discovery
- Service registration and health monitoring
- Must be started FIRST

### 🔐 auth-service (:8081)
- User authentication (login)
- JWT token issuance and validation
- Token refresh mechanism
- Security and authorization

**Port:** 8081 (internal, not exposed directly)

### 👤 user-service
- User registration
- User profile management
- User data CRUD operations
- User information retrieval

**Port:** 8082 (internal, not exposed directly)

### 🌐 api-gateway
- Routes frontend requests to backend services
- CORS configuration
- Request/response filtering
- Load balancing

**Port:** 8080 (ONLY exposed port to frontend)

### 📚 common-library
- Shared DTOs
- Common utilities
- Shared configurations
- Exception handling

## Frontend Integration Guide

### 1. Authentication Flow

```typescript
// Login
POST http://localhost:8080/api/v1/auth/login
Body: { "email": "user@example.com", "password": "password123" }
Response: { "token": "eyJhbGc...", "refreshToken": "..." }

// Use JWT in subsequent requests
Authorization: Bearer eyJhbGc...
```

### 2. User Registration Flow

```typescript
// Register new user
POST http://localhost:8080/api/v1/users/register
Body: {
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
Response: { "userId": "123", "message": "User registered successfully" }
```

### 3. Protected Endpoints

All requests to protected endpoints must include the JWT token:

```typescript
GET http://localhost:8080/api/v1/users/profile
Headers: {
  "Authorization": "Bearer eyJhbGc...",
  "Content-Type": "application/json"
}
```

## CORS Configuration

CORS is configured **ONLY at the API Gateway level** to allow frontend access:

- **Allowed Origins:** `http://localhost:3000`, `http://localhost:5173` (Vite)
- **Allowed Methods:** `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- **Allowed Headers:** `Authorization`, `Content-Type`
- **Exposed Headers:** `Authorization`

## Communication Architecture

```
┌─────────────┐
│  Frontend   │ (React/Vue/Angular)
│ TypeScript  │
└──────┬──────┘
       │ HTTP/REST
       │ Port 8080
       ▼
┌─────────────┐
│ API Gateway │ (Single Entry Point)
│  Port 8080  │
└──────┬──────┘
       │
       ├─────────────┐
       │             │
       ▼             ▼
┌──────────┐   ┌──────────┐
│   auth   │   │   user   │
│ service  │   │ service  │
│ :8081    │   │ :8082    │
└──────────┘   └──────────┘
```

**Key Points:**
- Frontend communicates **ONLY** with API Gateway (port 8080)
- Services communicate via HTTP through API Gateway
- No direct database sharing between services
- No internal service exposure to frontend

## Running the Backend

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL (or configured database)

### Start All Services

```bash
# From backend/ directory
mvn clean install
mvn spring-boot:run -pl api-gateway
mvn spring-boot:run -pl auth-service
mvn spring-boot:run -pl user-service
```

Or use individual service directories:

```bash
cd api-gateway && mvn spring-boot:run
cd auth-service && mvn spring-boot:run
cd user-service && mvn spring-boot:run
```

## Environment Configuration

Copy `.env.example` to `.env` and configure:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=zaphira

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# API Gateway
GATEWAY_PORT=8080
```

## Next Steps for Frontend Integration

1. **Generate TypeScript types** from OpenAPI/Swagger
2. **Configure Axios/Fetch** base URL to `http://localhost:8080`
3. **Implement JWT interceptor** for automatic token injection
4. **Handle token refresh** on 401 responses
5. **Use API Gateway routes** exclusively (never call services directly)

## Security Notes

- All passwords are hashed using BCrypt
- JWT tokens are stateless and validated on each request
- Refresh tokens should be stored securely (httpOnly cookies recommended)
- CORS is strictly configured at API Gateway level
- Services behind gateway are not directly accessible from frontend

## Support

For questions or issues, refer to the main project documentation or contact the development team.
