# 🏗️ Zaphira Platform - Complete Refactoring Architecture Plan v2.0

**Date**: 21 Janvier 2026  
**Auteur**: AI Backend Architect  
**Version**: 2.0 - Complete Optimization

---

## 📋 Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Analysis](#current-state-analysis)
3. [Refactoring Objectives](#refactoring-objectives)
4. [Architecture Decisions](#architecture-decisions)
5. [Implementation Plan](#implementation-plan)
6. [Microservices Refactoring Details](#microservices-refactoring-details)
7. [DTO & Mapping Strategy](#dto--mapping-strategy)
8. [API Gateway Integration](#api-gateway-integration)
9. [Best Practices & Standards](#best-practices--standards)
10. [Testing Strategy](#testing-strategy)

---

## 🎯 Executive Summary

### Objectives
Complete refactoring and optimization of the Zaphira Spring Boot microservices platform to:
- ✅ Align all entities with optimized database schemas
- ✅ Implement comprehensive DTO layer for all microservices
- ✅ Apply Spring Boot and microservices best practices
- ✅ Complete missing functionalities
- ✅ Ensure API Gateway proper integration
- ✅ Improve code quality and maintainability

### Scope
- **8 Microservices**: user-service, auth-service, wallet-service, transaction-service, notification-service, (+ dispute, exchange, reporting services to complete)
- **49 Database Tables**: Aligned with previously generated SQL schemas
- **100+ Endpoints**: RESTful APIs with proper validation
- **Common Library**: Shared DTOs, utilities, and domain objects

---

## 📊 Current State Analysis

### ✅ What's Working Well

1. **Basic Structure**
   - Maven multi-module setup properly configured
   - Spring Boot 3.2.3 with Java 17
   - Service Registry (Eureka) configured
   - API Gateway with Spring Cloud Gateway
   - Basic entity definitions exist

2. **Existing Components**
   - User, Wallet, Auth services have basic CRUD operations
   - Transaction service has core transaction processing
   - Notification service with Twilio SMS integration
   - Common library with shared User entities

3. **Technology Stack**
   - PostgreSQL databases
   - Kafka for async messaging
   - Lombok for boilerplate reduction
   - Spring Data JPA for persistence

### ⚠️ Issues Identified

1. **Entity-Schema Misalignment**
   - Transaction service missing many entities (settlements, refunds, disputes, etc.)
   - Reporting and analytics entities not implemented
   - Exchange rate service incomplete
   - Dispute service entities missing

2. **DTO Layer Incomplete**
   - Inconsistent DTO usage across services
   - Services exposing entities directly in some endpoints
   - No mapper framework (MapStruct) configured
   - Validation inconsistent

3. **Business Logic Issues**
   - Transaction validation incomplete
   - Wallet balance calculations need triggers/procedures
   - KYC workflow incomplete
   - Dispute resolution process not implemented

4. **API Gateway**
   - Routes configured but needs validation
   - Circuit breaker patterns partially implemented
   - CORS properly configured
   - Missing rate limiting and authentication filters

5. **Code Quality**
   - Duplicate code across services
   - Exception handling inconsistent
   - Missing transaction management annotations
   - Incomplete error responses

6. **Testing**
   - Limited unit tests
   - No integration tests
   - Missing test data builders

---

## 🎯 Refactoring Objectives

### 1. Entity Alignment
- ✅ Create all missing entities based on database schemas
- ✅ Implement proper JPA relationships
- ✅ Add auditing support (@CreatedDate, @LastModifiedDate)
- ✅ Implement optimistic locking (@Version)
- ✅ Add proper indexes and constraints annotations

### 2. DTO Implementation
- ✅ Create Request DTOs for all endpoints
- ✅ Create Response DTOs for all endpoints
- ✅ Implement MapStruct mappers
- ✅ Add Jakarta validation annotations
- ✅ Separate DTOs by concern (Create, Update, Response)

### 3. Service Layer Optimization
- ✅ Implement business logic properly
- ✅ Add transaction management
- ✅ Implement error handling
- ✅ Add logging and monitoring
- ✅ Implement caching where appropriate

### 4. Controller Layer
- ✅ Use only DTOs (never expose entities)
- ✅ Add proper validation
- ✅ Implement pagination for list endpoints
- ✅ Add OpenAPI documentation
- ✅ Standardize response format

### 5. Missing Functionalities
- ✅ Complete transaction workflows (authorization, settlement, refund)
- ✅ Implement dispute management system
- ✅ Add reporting and analytics
- ✅ Complete KYC verification workflow
- ✅ Implement exchange rate management

---

## 🏛️ Architecture Decisions

### 1. Layered Architecture Pattern

```
┌─────────────────────────────────────────┐
│          Controller Layer               │
│  - REST endpoints                       │
│  - Request/Response DTOs                │
│  - Validation                           │
│  - Exception handling                   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Service Layer                  │
│  - Business logic                       │
│  - Transaction management               │
│  - Event publishing                     │
│  - External service calls               │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Repository Layer               │
│  - JPA repositories                     │
│  - Custom queries                       │
│  - Entity management                    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Database Layer                 │
│  - PostgreSQL                           │
│  - Triggers & Functions                 │
│  - Constraints                          │
└─────────────────────────────────────────┘
```

### 2. DTO Mapping Strategy

**MapStruct Configuration**:
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

**Mapper Interface Pattern**:
```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    
    UserResponse toResponse(User user);
    
    List<UserResponse> toResponseList(List<User> users);
    
    User toEntity(UserCreateRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);
}
```

### 3. DTO Naming Conventions

- **Request DTOs**: `{Entity}{Action}Request`
  - Example: `UserRegistrationRequest`, `WalletCreateRequest`, `TransactionInitiateRequest`

- **Response DTOs**: `{Entity}Response` or `{Entity}{Detail}Response`
  - Example: `UserResponse`, `WalletDetailResponse`, `TransactionSummaryResponse`

- **Event DTOs**: `{Entity}{Event}Event`
  - Example: `UserRegisteredEvent`, `TransactionCompletedEvent`

### 4. Package Structure (Per Microservice)

```
com.zaphira.{service}/
├── controller/          # REST controllers
├── dto/
│   ├── request/        # Request DTOs
│   ├── response/       # Response DTOs
│   └── event/          # Event DTOs for Kafka
├── mapper/             # MapStruct mappers
├── service/            # Business logic services
│   ├── impl/          # Service implementations
│   └── validation/    # Custom validators
├── repository/         # JPA repositories
├── model/
│   ├── entity/        # JPA entities
│   └── enums/         # Enumerations
├── exception/          # Custom exceptions
├── config/            # Configuration classes
├── security/          # Security components
├── event/             # Event publishers/listeners
└── util/              # Utility classes
```

### 5. Common Library Organization

```
com.zaphira.common/
├── dto/               # Shared DTOs
│   ├── user/         # User-related DTOs
│   ├── wallet/       # Wallet-related DTOs
│   └── common/       # Common response wrappers
├── model/
│   ├── entities/     # Shared entities (User, KYC, etc.)
│   └── enums/        # Shared enums
├── exception/        # Common exceptions
├── util/             # Common utilities
├── validation/       # Custom validators
└── constants/        # Application constants
```

---

## 📋 Implementation Plan

### Phase 1: Foundation Setup (Priority: HIGH)

**Tasks**:
1. ✅ Add MapStruct dependencies to all microservices
2. ✅ Create common response wrappers (ApiResponse, PageResponse, ErrorResponse)
3. ✅ Implement global exception handler in each service
4. ✅ Add auditing configuration for all services
5. ✅ Configure logging properly (logback-spring.xml)

**Duration**: 1 day  
**Dependencies**: None

---

### Phase 2: Common Library Enhancement (Priority: HIGH)

**Tasks**:
1. ✅ Create common DTOs for shared entities (User, Wallet, KYC)
2. ✅ Implement common mappers
3. ✅ Add validation annotations
4. ✅ Create common exception classes
5. ✅ Add utility classes (date, string, encryption)

**Duration**: 1 day  
**Dependencies**: Phase 1

---

### Phase 3: User Service Refactoring (Priority: HIGH)

**Tasks**:
1. ✅ Review and update User entity to match schema
2. ✅ Create all Request/Response DTOs
3. ✅ Implement MapStruct mappers
4. ✅ Refactor UserService with business logic
5. ✅ Update UserController to use DTOs only
6. ✅ Complete KYC workflow implementation
7. ✅ Add validation and error handling
8. ✅ Implement user analytics tracking

**Duration**: 2 days  
**Dependencies**: Phase 2

**Files to Create/Update**:
- DTOs: 15 new DTOs
- Mappers: 3 MapStruct interfaces
- Services: 4 service classes
- Controllers: 2 controllers
- Repositories: 3 repositories

---

### Phase 4: Auth Service Refactoring (Priority: HIGH)

**Tasks**:
1. ✅ Review Token and RefreshToken entities
2. ✅ Create authentication DTOs
3. ✅ Implement JWT generation and validation
4. ✅ Add refresh token rotation
5. ✅ Implement activity logging
6. ✅ Add rate limiting for authentication endpoints
7. ✅ Implement password reset workflow

**Duration**: 1.5 days  
**Dependencies**: Phase 3

---

### Phase 5: Wallet Service Refactoring (Priority: HIGH)

**Tasks**:
1. ✅ Review Wallet entities (Wallet, SubWallet, WalletPermission, etc.)
2. ✅ Create Wallet DTOs (create, update, response, balance)
3. ✅ Implement wallet mappers
4. ✅ Refactor WalletService with balance management
5. ✅ Implement sub-wallet functionality
6. ✅ Add wallet permissions system
7. ✅ Implement wallet status history tracking
8. ✅ Add wallet analytics

**Duration**: 2 days  
**Dependencies**: Phase 3

---

### Phase 6: Transaction Service Refactoring (Priority: CRITICAL)

**Tasks**:
1. ✅ **Create Missing Entities**:
   - TransactionSettlement
   - TransactionRefund
   - TransactionAuthorization
   - ScheduledTransaction (exists but needs review)
   - TransactionStateHistory

2. ✅ **Create Complete DTO Layer**:
   - TransactionInitiateRequest
   - TransactionAuthorizationRequest
   - TransactionRefundRequest
   - TransactionSettlementRequest
   - TransactionResponse (detailed)
   - TransactionSummaryResponse

3. ✅ **Implement Services**:
   - TransactionService (core logic)
   - TransactionAuthorizationService
   - TransactionSettlementService
   - TransactionRefundService
   - TransactionValidationService
   - TransactionRoutingService

4. ✅ **Update Controllers**:
   - TransactionController (CRUD + initiate, authorize, settle)
   - RefundController
   - ScheduledTransactionController

5. ✅ **Implement Business Logic**:
   - Transaction state machine
   - Balance validation
   - Fee calculation
   - Currency conversion integration
   - Fraud detection hooks

**Duration**: 3 days  
**Dependencies**: Phases 3, 5

---

### Phase 7: Dispute Service Implementation (Priority: HIGH)

**Tasks**:
1. ✅ Create Dispute entities (Dispute, DisputeEvidence, DisputeTimeline)
2. ✅ Create Dispute DTOs
3. ✅ Implement DisputeService
4. ✅ Add dispute workflow (OPEN → INVESTIGATING → RESOLVED/REJECTED)
5. ✅ Implement evidence upload
6. ✅ Add timeline tracking
7. ✅ Integrate with transaction service

**Duration**: 2 days  
**Dependencies**: Phase 6

---

### Phase 8: Reporting Service Implementation (Priority: MEDIUM)

**Tasks**:
1. ✅ Create reporting entities (DailyReport, MerchantAnalytics, UserAnalytics)
2. ✅ Create reporting DTOs
3. ✅ Implement ReportingService with aggregation logic
4. ✅ Add scheduled jobs for report generation
5. ✅ Implement report export (CSV, PDF)
6. ✅ Create analytics dashboard endpoints

**Duration**: 2 days  
**Dependencies**: Phase 6

---

### Phase 9: Exchange Rate Service Implementation (Priority: MEDIUM)

**Tasks**:
1. ✅ Create ExchangeRate entity
2. ✅ Create exchange rate DTOs
3. ✅ Implement ExchangeRateService
4. ✅ Add currency pair management
5. ✅ Implement rate history archiving
6. ✅ Add conversion calculation methods
7. ✅ Integrate with external rate providers (optional)

**Duration**: 1.5 days  
**Dependencies**: Phase 6

---

### Phase 10: Notification Service Enhancement (Priority: MEDIUM)

**Tasks**:
1. ✅ Review notification entities
2. ✅ Create notification DTOs
3. ✅ Implement notification templates system
4. ✅ Add multi-channel support (Email, SMS, Push)
5. ✅ Implement notification preferences
6. ✅ Add notification history and tracking

**Duration**: 1.5 days  
**Dependencies**: Phase 2

---

### Phase 11: API Gateway Enhancement (Priority: HIGH)

**Tasks**:
1. ✅ Review all routes and update paths
2. ✅ Add authentication filters
3. ✅ Implement rate limiting
4. ✅ Add request/response logging
5. ✅ Implement circuit breaker for all services
6. ✅ Add API versioning support
7. ✅ Create fallback controllers

**Duration**: 1 day  
**Dependencies**: All service phases

---

### Phase 12: Testing & Documentation (Priority: HIGH)

**Tasks**:
1. ✅ Create unit tests for all services (target: 70% coverage)
2. ✅ Create integration tests for critical flows
3. ✅ Add test data builders
4. ✅ Update OpenAPI documentation
5. ✅ Create Postman collection
6. ✅ Write user documentation
7. ✅ Create deployment guide

**Duration**: 2 days  
**Dependencies**: All phases

---

## 🔧 Microservices Refactoring Details

### 1. User Service

**Current State**: ✅ Basic CRUD, registration with wallet creation, KYC entities

**Improvements Needed**:
- Complete KYC verification workflow (PENDING → SUBMITTED → VERIFIED/REJECTED)
- Add user analytics tracking
- Implement security questions properly
- Add profile photo upload
- Implement user suspension/reactivation

**New Endpoints to Add**:
```
POST   /api/users/{userId}/suspend          - Suspend user account
POST   /api/users/{userId}/reactivate       - Reactivate user account
GET    /api/users/{userId}/analytics        - Get user analytics
POST   /api/users/{userId}/kyc/documents    - Upload KYC documents
PUT    /api/users/{userId}/kyc/verify       - Admin: Verify KYC
PUT    /api/users/{userId}/kyc/reject       - Admin: Reject KYC
GET    /api/users/search                    - Search users (admin)
GET    /api/users/statistics                - Platform statistics (admin)
```

---

### 2. Auth Service

**Current State**: ✅ Login, JWT tokens, refresh tokens, activity log

**Improvements Needed**:
- Implement token rotation
- Add rate limiting for login attempts
- Implement password reset flow
- Add 2FA support preparation
- Enhance activity logging

**New Endpoints to Add**:
```
POST   /api/auth/forgot-password           - Initiate password reset
POST   /api/auth/reset-password            - Reset password with token
POST   /api/auth/change-password           - Change password (authenticated)
POST   /api/auth/verify-2fa                - Verify 2FA code
GET    /api/auth/sessions                  - Get active sessions
DELETE /api/auth/sessions/{sessionId}      - Revoke session
GET    /api/logs/activity                  - Get activity logs
```

---

### 3. Wallet Service

**Current State**: ✅ Basic wallet CRUD, sub-wallets, permissions

**Improvements Needed**:
- Implement balance calculation logic
- Add wallet freeze/unfreeze
- Implement spending limits
- Add wallet-to-wallet transfer
- Implement wallet analytics

**New Endpoints to Add**:
```
POST   /api/wallets/{walletId}/freeze       - Freeze wallet
POST   /api/wallets/{walletId}/unfreeze     - Unfreeze wallet
GET    /api/wallets/{walletId}/balance      - Get detailed balance
GET    /api/wallets/{walletId}/transactions - Get wallet transactions
POST   /api/wallets/{walletId}/limits       - Set spending limits
GET    /api/wallets/{walletId}/analytics    - Get wallet analytics
POST   /api/wallets/{walletId}/transfer     - Internal wallet transfer
```

---

### 4. Transaction Service

**Current State**: ⚠️ Basic transaction entity, partial implementation

**Critical Missing Components**:
1. **Entities**: Settlement, Refund entities incomplete
2. **Workflow**: State machine not implemented
3. **Validation**: Balance checks incomplete
4. **Settlement**: Batch settlement not implemented
5. **Refunds**: Partial/full refund logic missing

**Complete Endpoint List Needed**:
```
# Core Transactions
POST   /api/transactions/initiate           - Initiate transaction
POST   /api/transactions/{id}/authorize     - Authorize transaction
POST   /api/transactions/{id}/complete      - Complete transaction
POST   /api/transactions/{id}/cancel        - Cancel transaction
GET    /api/transactions/{id}               - Get transaction details
GET    /api/transactions                    - List transactions (paginated)
GET    /api/transactions/search             - Search transactions

# Refunds
POST   /api/transactions/{id}/refund        - Create refund
GET    /api/transactions/{id}/refunds       - List refunds for transaction

# Settlements
POST   /api/settlements/batch               - Create batch settlement
GET    /api/settlements/{id}                - Get settlement details
GET    /api/settlements                     - List settlements
PUT    /api/settlements/{id}/approve        - Approve settlement

# Scheduled Transactions
POST   /api/transactions/scheduled          - Create scheduled transaction
GET    /api/transactions/scheduled          - List scheduled transactions
PUT    /api/transactions/scheduled/{id}     - Update scheduled transaction
DELETE /api/transactions/scheduled/{id}     - Cancel scheduled transaction
```

---

### 5. Dispute Service (NEW)

**Current State**: ⚠️ Entities exist but no service implementation

**Implementation Needed**:
- Complete dispute workflow
- Evidence management
- Timeline tracking
- Integration with transactions

**Endpoints to Implement**:
```
POST   /api/disputes                        - Create dispute
GET    /api/disputes/{id}                   - Get dispute details
GET    /api/disputes                        - List disputes
PUT    /api/disputes/{id}/status            - Update dispute status
POST   /api/disputes/{id}/evidence          - Upload evidence
GET    /api/disputes/{id}/evidence          - List evidence
POST   /api/disputes/{id}/timeline          - Add timeline event
GET    /api/disputes/{id}/timeline          - Get timeline
PUT    /api/disputes/{id}/resolve           - Resolve dispute
```

---

### 6. Reporting Service (NEW)

**Current State**: ⚠️ Entities exist but service incomplete

**Implementation Needed**:
- Daily report generation
- Merchant analytics
- User analytics
- Export functionality

**Endpoints to Implement**:
```
GET    /api/reports/daily                   - Get daily reports
GET    /api/reports/daily/{date}            - Get specific date report
GET    /api/reports/merchants               - Merchant analytics
GET    /api/reports/merchants/{id}          - Specific merchant report
GET    /api/reports/users                   - User analytics
GET    /api/reports/export                  - Export report (CSV/PDF)
POST   /api/reports/generate                - Trigger report generation
```

---

### 7. Exchange Rate Service (NEW)

**Current State**: ⚠️ Entity exists, basic controller, incomplete

**Implementation Needed**:
- Currency pair management
- Rate history
- Conversion calculations
- External provider integration

**Endpoints to Implement**:
```
GET    /api/exchange-rates                  - Get current rates
GET    /api/exchange-rates/convert          - Convert currency
POST   /api/exchange-rates                  - Add rate (admin)
PUT    /api/exchange-rates/{id}             - Update rate (admin)
GET    /api/exchange-rates/history          - Rate history
GET    /api/exchange-rates/pairs            - Supported currency pairs
```

---

### 8. Notification Service

**Current State**: ✅ OTP, email, SMS basic functionality

**Improvements Needed**:
- Template management
- Notification preferences
- Multi-channel orchestration
- Notification history

**New Endpoints to Add**:
```
POST   /api/notifications/send              - Send notification
GET    /api/notifications/history           - Get notification history
POST   /api/notifications/templates         - Create template (admin)
GET    /api/notifications/templates         - List templates
PUT    /api/notifications/preferences       - Update preferences
GET    /api/notifications/preferences       - Get preferences
```

---

## 🎨 DTO & Mapping Strategy

### DTO Design Principles

1. **Separation of Concerns**
   - Request DTOs for input validation
   - Response DTOs for output formatting
   - Never expose entities directly

2. **Validation**
   - Use Jakarta validation annotations
   - Custom validators for complex rules
   - Fail-fast validation

3. **Immutability**
   - Use records for simple DTOs (Java 17+)
   - Final fields where possible
   - Builder pattern for complex DTOs

### MapStruct Configuration

**Add to all service POMs**:
```xml
<dependencies>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>1.5.5.Final</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Example DTO Structures

#### Request DTO
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phoneNumber;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String lastName;
    
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{4,6}$", message = "PIN must be 4-6 digits")
    private String pin;
    
    @NotNull(message = "User type is required")
    private UserType userType;
    
    private String referralCode;
}
```

#### Response DTO
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String userId;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String profilePhotoUrl;
    private UserType userType;
    private AccountStatus status;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean kycVerified;
    private Boolean twoFactorEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    // Nested objects
    private KYCStatusResponse kycStatus;
    private WalletSummaryResponse wallet;
}
```

#### Mapper Interface
```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    
    // Entity to Response
    @Mapping(source = "kyc.status", target = "kycStatus.status")
    @Mapping(source = "kyc.verifiedAt", target = "kycStatus.verifiedAt")
    UserResponse toResponse(User user);
    
    List<UserResponse> toResponseList(List<User> users);
    
    // Request to Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "pin", ignore = true) // Handle separately with encryption
    User toEntity(UserRegistrationRequest request);
    
    // Update Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);
}
```

---

## 🌐 API Gateway Integration

### Current Configuration Review

**Routes Pattern**: `/api/v1/{service}/{resource}`

### Authentication Filter Implementation

```java
@Component
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {
    
    private static final List<String> OPEN_ENDPOINTS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/forgot-password",
        "/api/v1/users/register"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }
        
        // Extract and validate JWT
        String token = extractToken(exchange.getRequest());
        if (token == null || !validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        // Add user context to headers
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header("X-User-Id", extractUserId(token))
            .header("X-User-Role", extractUserRole(token))
            .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
    
    @Override
    public int getOrder() {
        return -100; // High priority
    }
}
```

### Rate Limiting Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@userKeyResolver}"
```

---

## ✨ Best Practices & Standards

### 1. Controller Best Practices

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management endpoints")
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    @PostMapping
    @Operation(summary = "Register new user")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Registering new user: {}", request.getEmail());
        
        User user = userService.register(request);
        UserResponse response = userMapper.toResponse(user);
        
        return ApiResponse.success(response, "User registered successfully");
    }
    
    @GetMapping
    @Operation(summary = "List all users")
    public ApiResponse<Page<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userService.findAll(search, pageable);
        Page<UserResponse> response = users.map(userMapper::toResponse);
        
        return ApiResponse.success(response);
    }
}
```

### 2. Service Best Practices

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final WalletClient walletClient;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public User register(UserRegistrationRequest request) {
        log.debug("Starting user registration for email: {}", request.getEmail());
        
        // Validation
        validateUniqueEmail(request.getEmail());
        validateUniquePhone(request.getPhoneNumber());
        
        // Entity creation
        User user = User.builder()
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .pinHash(passwordEncoder.encode(request.getPin()))
            .userType(request.getUserType())
            .status(AccountStatus.PENDING_VERIFICATION)
            .build();
        
        user = userRepository.save(user);
        log.info("User created with ID: {}", user.getId());
        
        // Create wallet asynchronously
        createWalletAsync(user);
        
        // Publish event
        eventPublisher.publish(new UserRegisteredEvent(user.getId(), user.getEmail()));
        
        return user;
    }
    
    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already registered: " + email);
        }
    }
}
```

### 3. Exception Handling

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ApiResponse.error("Validation failed", errors);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ApiResponse.error("An unexpected error occurred");
    }
}
```

### 4. Response Wrapper

```java
@Data
@Builder
@AllArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static <T> ApiResponse<T> error(String message, Map<String, Object> metadata) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .metadata(metadata)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

---

## 🧪 Testing Strategy

### Unit Testing

**Example Service Test**:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private WalletClient walletClient;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    void register_WithValidRequest_ShouldCreateUser() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
            .email("test@example.com")
            .phoneNumber("+1234567890")
            .firstName("John")
            .lastName("Doe")
            .pin("1234")
            .userType(UserType.REGULAR)
            .build();
        
        User savedUser = User.builder()
            .id(1L)
            .email(request.getEmail())
            .build();
        
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // When
        User result = userService.register(request);
        
        // Then
        assertNotNull(result);
        assertEquals(request.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publish(any(UserRegisteredEvent.class));
    }
    
    @Test
    void register_WithDuplicateEmail_ShouldThrowException() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
            .email("existing@example.com")
            .build();
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        
        // When & Then
        assertThrows(DuplicateEmailException.class, () -> 
            userService.register(request)
        );
        verify(userRepository, never()).save(any());
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void register_WithValidData_ShouldReturn201() throws Exception {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
            .email("test@example.com")
            .phoneNumber("+1234567890")
            .firstName("John")
            .lastName("Doe")
            .pin("1234")
            .userType(UserType.REGULAR)
            .build();
        
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("test@example.com"));
        
        assertEquals(1, userRepository.count());
    }
}
```

---

## 📊 Success Metrics

### Code Quality Targets
- ✅ **Test Coverage**: > 70%
- ✅ **Code Duplication**: < 5%
- ✅ **Cyclomatic Complexity**: < 10 per method
- ✅ **SonarQube Quality Gate**: Pass

### Performance Targets
- ✅ **API Response Time**: < 200ms (p95)
- ✅ **Database Query Time**: < 50ms (p95)
- ✅ **Service Availability**: > 99.9%

### Documentation
- ✅ **OpenAPI Spec**: Complete for all endpoints
- ✅ **README**: Updated for each service
- ✅ **Architecture Docs**: Complete
- ✅ **Deployment Guide**: Available

---

## 🚀 Deployment Considerations

### Database Migrations
- Use Flyway or Liquibase for version control
- Test migrations in staging first
- Backup before production migration

### Service Deployment Order
1. Common Library
2. Service Registry
3. Config Server
4. User Service
5. Auth Service
6. Wallet Service
7. Transaction Service
8. Notification Service
9. API Gateway

### Monitoring & Logging
- ELK Stack for centralized logging
- Prometheus + Grafana for metrics
- Distributed tracing with Sleuth + Zipkin

---

## 📝 Summary

This comprehensive refactoring plan transforms the Zaphira platform into a production-ready microservices system with:

✅ **Complete DTO layer** for all services  
✅ **MapStruct integration** for efficient mapping  
✅ **All missing entities** implemented  
✅ **Business logic** properly implemented  
✅ **API Gateway** fully configured  
✅ **Best practices** applied throughout  
✅ **Testing strategy** in place  
✅ **Documentation** complete

**Total Estimated Duration**: 15-20 days  
**Priority Services**: User, Auth, Wallet, Transaction  
**Risk Level**: Medium (requires careful testing)

---

**Last Updated**: 21 Janvier 2026  
**Next Review**: After Phase 3 completion
