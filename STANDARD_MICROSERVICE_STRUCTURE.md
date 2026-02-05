# Standard Microservice Structure for Zaphira

## Package Naming Convention
All services must follow the pattern: `com.zaphira.{service-name}`

### Valid Service Names:
- `transaction` (not transaction-service)
- `wallet` (not wallet-service)
- `user` (not service_user or user-service)
- `auth` (not auth-service)
- `notification` (not notification-service)

## Standard Directory Structure

```
{service-name}/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── zaphira/
│   │   │           └── {service-name}/
│   │   │               ├── {ServiceName}Application.java
│   │   │               ├── client/              # Feign clients for other services
│   │   │               ├── config/              # Configuration classes
│   │   │               ├── controller/          # REST controllers
│   │   │               ├── dto/                 # Data Transfer Objects
│   │   │               │   ├── request/
│   │   │               │   └── response/
│   │   │               ├── event/               # Kafka events (if applicable)
│   │   │               ├── exception/           # Custom exceptions
│   │   │               ├── kafka/               # Kafka producers/consumers (if applicable)
│   │   │               ├── listener/            # Event listeners (if applicable)
│   │   │               ├── mapper/              # Entity ↔ DTO mappers
│   │   │               ├── model/               # JPA entities, enums
│   │   │               │   ├── entity/
│   │   │               │   └── enums/
│   │   │               ├── repository/          # Spring Data repositories
│   │   │               ├── security/            # Security config, filters
│   │   │               ├── service/             # Business logic services
│   │   │               │   ├── impl/
│   │   │               │   └── interfaces (or direct *.java)
│   │   │               └── util/                # Utility classes
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       └── application-prod.yml
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── zaphira/
│       │           └── {service-name}/
│       │               ├── integration/         # Integration tests
│       │               ├── unit/                # Unit tests
│       │               └── {ServiceName}ApplicationTests.java
│       └── resources/
│           └── application-test.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## Package Rules

### MANDATORY packages (all services):
- `controller/` - REST API endpoints
- `service/` - Business logic (singular, not services)
- `repository/` - Data access layer
- `model/` - Entities and enums (singular, not models)
- `dto/` - Request/response DTOs
- `exception/` - Custom exception classes
- `config/` - Configuration classes
- `security/` - Security configuration

### OPTIONAL packages (service-specific):
- `client/` - Only if service calls other microservices
- `event/` - Only if service publishes events
- `kafka/` - Only if service uses Kafka
- `listener/` - Only if service listens to events
- `mapper/` - Recommended for entity-DTO conversion
- `util/` - Common utilities (validators, helpers)

## Configuration Files

### application.yml (main)
```yaml
spring:
  application:
    name: {service-name}-service
  profiles:
    active: ${ACTIVE_PROFILE:dev}
server:
  port: ${PORT:808X}
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka}
```

### application-test.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
  kafka:
    enabled: false
app:
  security:
    jwt:
      enabled: false
```

## Current Status by Service

### ✅ Compliant Services:
- **transaction-service**: `com.zaphira.transaction` - CORRECT
- **auth**: `com.zaphira.auth` - CORRECT
- **notification-service**: `com.zaphira.notification` - CORRECT

### ❌ Non-Compliant Services:

#### wallet-service
- **Issue**: `models/` → should be `model/`
- **Action**: Rename package

#### user-service
- **Issue 1**: `com.zaphira.service_user` → should be `com.zaphira.user`
- **Issue 2**: `services/` → should be `service/`
- **Action**: Rename package and folder

## Migration Priority

1. **HIGH**: Fix user-service package naming (breaks convention)
2. **MEDIUM**: Rename wallet-service `models/` → `model/`
3. **MEDIUM**: Rename user-service `services/` → `service/`
4. **LOW**: Add missing `exception/` packages to auth and notification

## Verification Checklist

After reorganization, verify:
- [ ] All services use `com.zaphira.{service-name}` pattern
- [ ] All services use singular package names (`model`, `service`)
- [ ] All mandatory packages present
- [ ] Application class named `{ServiceName}Application.java`
- [ ] Configuration files follow naming convention
- [ ] Tests organized in `integration/` and `unit/` folders
- [ ] No duplicate or inconsistent package structures
