# 🚀 API Gateway v1 - Quick Reference Card

## ⚡ Quick Start
```bash
# Start all services
.\start-backend-v1.ps1

# Test API Gateway
.\test-gateway-v1-routes.ps1
```

---

## 📡 Service Endpoints

### Base URL Pattern
```
http://localhost:8080/api/v1/{service}/{resource}
```

### Auth Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/login` | User login |
| `POST` | `/api/v1/auth/refresh` | Refresh JWT token |
| `POST` | `/api/v1/auth/logout` | User logout |
| `GET` | `/api/v1/auth/validate` | Validate token |

### User Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/users/register` | User registration |
| `GET` | `/api/v1/users/{id}` | Get user profile |
| `PUT` | `/api/v1/users/{id}` | Update profile |
| `DELETE` | `/api/v1/users/{id}` | Delete user |
| `POST` | `/api/v1/pin-reset/request` | Request PIN reset |
| `POST` | `/api/v1/pin-reset/verify` | Verify reset OTP |
| `GET` | `/api/v1/security-questions` | List questions |

### Wallet Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/wallets/user/{userId}` | Get user wallet |
| `GET` | `/api/v1/wallets/{number}` | Get wallet by number |
| `POST` | `/api/v1/wallets/transaction` | Process transaction |
| `GET` | `/api/v1/wallets/{number}/balance` | Get balance |
| `POST` | `/api/v1/wallet/permission/grant` | Grant permission |
| `POST` | `/api/v1/wallet/subWallet/create` | Create sub-wallet |

### Notification Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/notifications/send` | Send notification |
| `GET` | `/api/v1/notifications/{id}` | Get notification status |
| `GET` | `/api/v1/notifications/user/{userId}` | Get user notifications |

---

## 🔧 Port Reference

| Service | Port | Health Check | Dashboard |
|---------|------|--------------|-----------|
| **API Gateway** | 8080 | `/actuator/health` | - |
| **Eureka** | 8761 | `/actuator/health` | `http://localhost:8761` |
| **Auth** | 8081 | `/api/v1/auth/actuator/health` | - |
| **User** | 8082 | `/api/v1/users/actuator/health` | - |
| **Wallet** | 8083 | `/api/v1/wallets/actuator/health` | - |
| **Notification** | 8089 | `/api/v1/notifications/actuator/health` | - |

---

## 🧪 Testing Commands

### Health Checks
```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Check all registered services
curl http://localhost:8761/eureka/apps

# Check Gateway routes
curl http://localhost:8080/actuator/gateway/routes
```

### API Testing
```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"pass123"}'

# Register user
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePass123!"}'

# Get wallet
curl http://localhost:8080/api/v1/wallets/user/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### CORS Testing
```bash
curl -X OPTIONS http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -v
```

---

## 🎨 Frontend Integration

### Environment Variables
```env
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_API_VERSION=v1
```

### API Client Setup
```typescript
const API_BASE = 'http://localhost:8080/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
});

// Add JWT token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

---

## 🛡️ CORS Configuration

### Allowed Origins (Development)
- `http://localhost:3000` - React Dev Server
- `http://localhost:5173` - Vite Dev Server
- `http://localhost:4200` - Angular Dev Server

### Allowed Origins (Production)
- `https://zaphira.com`
- `https://app.zaphira.com`

### Allowed Methods
`GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`

### Exposed Headers
`Authorization`, `Content-Type`, `X-Total-Count`, `X-Page-Number`

---

## 🔄 Circuit Breakers

| Service | Timeout | Failure Threshold | Open Duration |
|---------|---------|-------------------|---------------|
| **Auth** | 5s | 50% | 10s |
| **User** | 8s | 50% | 15s |
| **Wallet** | 6s | 50% | 15s |

### Check Circuit Breaker Status
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

---

## 🐛 Troubleshooting

### Gateway returns 404
```bash
# Check if Gateway is registered
curl http://localhost:8761/eureka/apps

# Check routes configuration
curl http://localhost:8080/actuator/gateway/routes

# Verify target service is running
curl http://localhost:8081/api/auth/actuator/health
```

### CORS errors
```bash
# Verify CORS headers in response
curl -I http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:3000"

# Check for duplicate CORS config in microservices
# Remove CORS from services, keep only in Gateway
```

### Service Discovery issues
```bash
# Check Eureka dashboard
open http://localhost:8761

# Verify service registration
curl http://localhost:8761/eureka/apps/AUTH-SERVICE
```

### Path rewriting not working
```bash
# Enable debug logging
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY=DEBUG

# Test direct service call
curl http://localhost:8081/api/auth/actuator/health

# Test via Gateway
curl http://localhost:8080/api/v1/auth/actuator/health
```

---

## 📊 Monitoring

### Actuator Endpoints
```bash
# Health check
http://localhost:8080/actuator/health

# Metrics
http://localhost:8080/actuator/metrics

# Gateway routes
http://localhost:8080/actuator/gateway/routes

# Circuit breakers
http://localhost:8080/actuator/circuitbreakers

# Prometheus metrics
http://localhost:8080/actuator/prometheus
```

### Health Check Details
```bash
curl http://localhost:8080/actuator/health | jq

# Expected components:
# - discoveryComposite: Eureka registration
# - ping: Gateway liveness
# - circuitBreakers: Resilience status
```

---

## 🔑 Configuration Files

| Service | Config File |
|---------|-------------|
| Gateway | `backend/api-gateway/src/main/resources/application.yml` |
| Auth | `backend/auth-service/src/main/resources/application.properties` |
| User | `backend/user-service/src/main/resources/application.properties` |
| Wallet | `backend/wallet-service/src/main/resources/application.yml` |
| Notification | `backend/notification-service/src/main/resources/application.yml` |

---

## 📝 Common Request Examples

### Login Request
```json
POST /api/v1/auth/login
{
  "username": "user@example.com",
  "password": "SecurePass123!"
}
```

### Register Request
```json
POST /api/v1/users/register
{
  "email": "newuser@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890"
}
```

### Transaction Request
```json
POST /api/v1/wallets/transaction
{
  "sourceWalletNumber": "WAL123456",
  "targetWalletNumber": "WAL789012",
  "amount": 100.50,
  "currency": "USD",
  "description": "Payment for services"
}
```

### Send Notification
```json
POST /api/v1/notifications/send
{
  "userId": "123",
  "type": "EMAIL",
  "subject": "Welcome to Zaphira",
  "message": "Thank you for registering!"
}
```

---

## 🚀 Performance Tips

1. **Connection Pooling**: Gateway maintains connection pool to services
2. **Circuit Breakers**: Prevents cascading failures
3. **Service Discovery**: Load balancing via Eureka
4. **CORS Caching**: 1-hour preflight cache
5. **Request Timeout**: Configured per service (5-10s)

---

## 📚 Documentation

- **Full Guide**: [API_GATEWAY_V1_DOCUMENTATION.md](API_GATEWAY_V1_DOCUMENTATION.md)
- **Backend Analysis**: [BACKEND_ANALYSIS_AND_INTEGRATION_STRATEGY.md](BACKEND_ANALYSIS_AND_INTEGRATION_STRATEGY.md)
- **Integration Guide**: [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)

---

## 🆘 Quick Commands Cheat Sheet

```bash
# Start services
.\start-backend-v1.ps1

# Test Gateway
.\test-gateway-v1-routes.ps1

# Check Eureka
start http://localhost:8761

# Check Gateway health
curl http://localhost:8080/actuator/health

# List all routes
curl http://localhost:8080/actuator/gateway/routes | jq

# Refresh routes (after config change)
curl -X POST http://localhost:8080/actuator/gateway/refresh

# Stop all services (close PowerShell windows)
```

---

**Version**: 1.0.0  
**Last Updated**: January 2025  
**Support**: See [API_GATEWAY_V1_DOCUMENTATION.md](API_GATEWAY_V1_DOCUMENTATION.md)
