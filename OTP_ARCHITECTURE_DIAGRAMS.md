# OTP Integration - Architecture & Class Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         REST Client                                 │
│                  (Mobile App / Web Client)                          │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │
                    POST /api/transactions/{id}/authorize
                    {phoneNumber, otpCode, ...}
                                   │
                                   ↓
┌──────────────────────────────────────────────────────────────────────┐
│                    TransactionController                            │
│                  @PostMapping("/{id}/authorize")                    │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │
                    AuthorizationValidationRequest
                                   │
                                   ↓
┌──────────────────────────────────────────────────────────────────────┐
│                    TransactionService                               │
│              authorizeTransaction(Long id, Request)                 │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ 1. Get transaction from DB                                     │ │
│  │ 2. Validate authorization required                             │ │
│  │ 3. Compliance check                                            │ │
│  │ 4. ✅ NEW: OTP Verification                                   │ │
│  │    ├─→ Call: otpService.verifyOtp(phone, code)              │ │
│  │    └─→ Throws OtpVerificationException on failure            │ │
│  │ 5. Approve authorization                                       │ │
│  │ 6. Change status to AUTHORIZED                                 │ │
│  │ 7. Process immediate transaction                               │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────┬────────────────────────────────┬─────────────────────┘
               │                                │
               ↓                                ↓
    ┌──────────────────────┐      ┌──────────────────────────────┐
    │  OtpService          │      │ TransactionAuthorizationService
    │ (NEW)                │      │ AuthorizationRequest → OTP
    │                      │      │ status management
    │ Key Methods:         │      │ approveAuthorization()
    │ - verifyOtp()        │      └──────────────────────────────┘
    │ - recordFailedAttempt│
    │ - getActiveOtp()     │
    │ - isOtpValid()       │
    │                      │
    └──────┬───────────────┘
           │
           ↓
    ┌──────────────────────────────┐
    │ OtpTokenRepository           │
    │ (NEW - JPA Repository)       │
    │                              │
    │ Methods:                     │
    │ - findByPhoneCodeAndPurpose()│
    │ - findByPhoneAndCode()       │
    │ - findActiveOtpByPhone()     │
    │ - findByIdAndPhone()         │
    └──────┬───────────────────────┘
           │
           ↓
    ┌──────────────────────────────┐
    │   PostgreSQL Database        │
    │  otp_tokens table            │
    │                              │
    │ Columns:                     │
    │ - id (PK)                    │
    │ - code                       │
    │ - phone_number               │
    │ - purpose                    │
    │ - attempts                   │
    │ - used                       │
    │ - expires_at                 │
    │ - verified_at                │
    │ - context                    │
    └──────────────────────────────┘
```

## Class Hierarchy Diagram

```
Common Library (Shared)
├── OtpToken (Entity)
│   ├── @Entity @Table("otp_tokens")
│   ├── Fields:
│   │   ├── id: Long @Id @GeneratedValue
│   │   ├── code: String
│   │   ├── phoneNumber: String
│   │   ├── purpose: String
│   │   ├── attempts: Integer
│   │   ├── used: Boolean
│   │   ├── expiresAt: LocalDateTime
│   │   ├── verifiedAt: LocalDateTime
│   │   └── context: String
│   └── Methods:
│       ├── isValid(): boolean
│       ├── isMaxAttemptsExceeded(int): boolean
│       └── markAsVerified(): void

Transaction Service
├── OtpTokenRepository (Interface extends JpaRepository)
│   ├── findActiveOtpByPhoneAndPurpose(phone, purpose): Optional<OtpToken>
│   ├── findByPhoneAndCode(phone, code): Optional<OtpToken>
│   ├── findByPhoneCodeAndPurpose(phone, code, purpose): Optional<OtpToken>
│   └── findByIdAndPhone(id, phone): Optional<OtpToken>
│
├── OtpService (@Service)
│   ├── Dependencies:
│   │   └── OtpTokenRepository
│   ├── Configuration:
│   │   ├── maxAttempts: int = 3
│   │   └── otpPurpose: String = "TRANSACTION_AUTH"
│   ├── Methods:
│   │   ├── verifyOtp(phone, code): OtpToken
│   │   ├── verifyOtp(phone, code, purpose): OtpToken
│   │   ├── recordFailedAttempt(phone, code): void
│   │   ├── getActiveOtp(phone): Optional<OtpToken>
│   │   ├── getActiveOtp(phone, purpose): Optional<OtpToken>
│   │   └── isOtpValid(otp): boolean
│   └── Exceptions:
│       └── OtpVerificationException extends RuntimeException
│
├── OtpMapper (@Component)
│   ├── toAuthorizationResponse(otp, txnId, maxAttempts): OtpAuthorizationResponse
│   └── maskPhoneNumber(phone): String
│
├── TransactionService (@Service) - UPDATED
│   ├── Dependencies:
│   │   ├── TransactionRepository
│   │   ├── OtpService (NEW)
│   │   └── ... (existing dependencies)
│   ├── Constructors:
│   │   ├── Primary (13 parameters including OtpService)
│   │   └── 3 backward-compatible overloads
│   └── Methods:
│       └── authorizeTransaction(Long id, AuthorizationValidationRequest)
│           ├── Get transaction
│           ├── Validate authorization required
│           ├── Compliance check
│           ├── ✅ Verify OTP (NEW)
│           ├── Approve authorization
│           ├── Change status
│           └── Process transaction
│
└── DTOs
    ├── AuthorizationValidationRequest (UPDATED)
    │   ├── method: AuthorizationMethod
    │   ├── code: String
    │   ├── authorizedBy: String
    │   ├── phoneNumber: String (NEW)
    │   └── otpCode: String (NEW)
    │
    └── OtpAuthorizationResponse (NEW)
        ├── transactionId: Long
        ├── otpRequired: Boolean
        ├── phoneNumber: String (masked)
        ├── otpExpiry: LocalDateTime
        ├── attemptsRemaining: Integer
        ├── message: String
        └── status: String
```

## Method Call Flow Diagram

```
┌─────────────────────────────────────────────────┐
│ POST /api/transactions/18/authorize             │
│ {                                               │
│   "phoneNumber": "+237681332456",              │
│   "otpCode": "802975"                          │
│ }                                               │
└────────────────┬────────────────────────────────┘
                 │
                 ↓
┌────────────────────────────────────────────────────────┐
│ TransactionService.authorizeTransaction()              │
│ ├─→ getTransaction(18)                                │
│ │   ↓                                                   │
│ │   └─→ TransactionRepository.findById(18)            │
│ │       ↓                                               │
│ │       └─→ SELECT FROM transaction WHERE id = 18     │
│ │                                                       │
│ ├─→ Validate authorizationRequired = true             │
│ │                                                       │
│ ├─→ complianceService.assertNotBlocked(tx)            │
│ │                                                       │
│ ├─→ ✅ IF phoneNumber && otpCode THEN:               │
│ │   │                                                   │
│ │   ↓                                                   │
│ │   OtpService.verifyOtp("+237681332456", "802975")   │
│ │   │                                                   │
│ │   ├─→ OtpTokenRepository.findByPhoneCodeAndPurpose()│
│ │   │   ├─→ SELECT FROM otp_tokens WHERE            │
│ │   │   │   phone_number = '+237681332456' AND       │
│ │   │   │   code = '802975' AND                      │
│ │   │   │   purpose = 'TRANSACTION_AUTH' AND         │
│ │   │   │   used = false                             │
│ │   │   │                                              │
│ │   │   └─→ If empty → THROW OtpVerificationException│
│ │   │                                                   │
│ │   ├─→ Validate OTP:                                │
│ │   │   ├─→ Check expires_at > now()                 │
│ │   │   │   └─→ If false → THROW "OTP has expired"  │
│ │   │   │                                              │
│ │   │   ├─→ Check used = false                       │
│ │   │   │   └─→ If true → THROW "Already used"      │
│ │   │   │                                              │
│ │   │   └─→ Check attempts < maxAttempts             │
│ │   │       └─→ If false → THROW "Max attempts exceeded"
│ │   │                                                   │
│ │   └─→ Mark OTP as verified:                        │
│ │       ├─→ otp.setUsed(true)                        │
│ │       ├─→ otp.setVerifiedAt(now())                 │
│ │       └─→ OtpTokenRepository.save(otp)             │
│ │           ↓                                          │
│ │           └─→ UPDATE otp_tokens SET                │
│ │               used = true,                          │
│ │               verified_at = now()                   │
│ │               WHERE id = 3                          │
│ │                                                       │
│ ├─→ authorizationService.approveAuthorization(...)    │
│ │   └─→ Mark authorization as approved               │
│ │                                                       │
│ ├─→ changeStatus(tx, AUTHORIZED, ...)                │
│ │   └─→ Update transaction.status = AUTHORIZED       │
│ │                                                       │
│ └─→ processImmediateTransaction(tx, actor)           │
│     └─→ Execute wallet transfer                       │
│         └─→ Update transaction.status = COMPLETED     │
│                                                        │
└────────────┬───────────────────────────────────────────┘
             │
             ↓
   ┌─────────────────────┐
   │ Return Transaction  │
   │ with COMPLETED      │
   │ status              │
   └─────────────────────┘
```

## Error Handling Flow

```
OTP Verification Attempt
        ↓
┌───────────────────────────────────────────────────┐
│ Input: phoneNumber, otpCode                       │
└────────────┬────────────────────────────────────────┘
             │
             ↓
┌───────────────────────────────────────────────────┐
│ OtpService.verifyOtp(phoneNumber, otpCode)        │
└────────┬─────────────────────────────────┬────────┘
         │                                 │
         ↓                                 │
    ┌─────────────────────┐                │
    │ Query Database      │                │
    │ findByPhoneCodeAnd- │                │
    │ Purpose()           │                │
    └──────┬──────────────┘                │
           │                               │
    ┌──────┴───────────┬─────────────────┐ │
    │                  │                 │ │
   NO                YES               YES │
    │                  │                 │ │
    ↓                  ↓                 ↓ │
 ┌─────────────┐   ┌──────────────┐  ┌───────────────┐
 │ Check if    │   │Check if found│  │ No further    │
 │found any    │   │ but already  │  │ validation    │
 │record       │   │ used         │  │ needed        │
 └──────┬──────┘   └──────┬───────┘  └───────┬───────┘
        │                 │                  │
        │                 ↓                  ↓
        │          ┌──────────────┐   ┌────────────────┐
        │          │ Set used=true│   │Continue to next│
        │          │ Mark as used │   │ validation     │
        │          └──────┬───────┘   └────────┬───────┘
        │                 │                    │
        ↓                 ↓                    ↓
  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
  │THROW         │  │THROW         │  │Check if expired  │
  │"Invalid OTP  │  │"OTP already  │  │expiresAt > now() │
  │code"         │  │been used"    │  └──────┬───────────┘
  └──────┬───────┘  └──────┬───────┘         │
         │                 │    ┌────────────┴──────┐
         │                 │    │                   │
         │                 │   YES                 NO
         │                 │    │                   │
         │                 │    ↓                   ↓
         │                 │  ┌────────────┐  ┌─────────────┐
         │                 │  │Continue to │  │THROW        │
         │                 │  │next check  │  │"OTP expired"│
         │                 │  └────────┬───┘  └─────┬───────┘
         │                 │           │             │
         │                 │           ↓             │
         │                 │  ┌──────────────────┐   │
         │                 │  │Check attempts    │   │
         │                 │  │attempts < max    │   │
         │                 │  └──────┬───────────┘   │
         │                 │         │               │
         │                 │  ┌──────┴────────┐      │
         │                 │  │               │      │
         │                 │ YES             NO     │
         │                 │  │               │      │
         │                 │  ↓               ↓      │
         │                 │┌────────┐  ┌──────────┐ │
         │                 ││Mark OTP│  │THROW     │ │
         │                 ││as used │  │"Max      │ │
         │                 ││and save│  │attempts" │ │
         │                 │└────┬───┘  └────┬─────┘ │
         │                 │     │            │       │
         │                 │     ↓            ↓       │
         │                 │  ┌────────────────────┐  │
         │                 │  │RETURN verified OTP │  │
         │                 │  │or throw exception  │  │
         │                 │  └─────────┬──────────┘  │
         │                 │            │             │
         └─────────────────┴────────────┼─────────────┘
                                        │
                    ┌───────────────────┴───────────┐
                    │                               │
                   YES                             NO
                    │                               │
                    ↓                               ↓
            ┌──────────────────┐        ┌───────────────────────┐
            │Continue with     │        │OtpVerificationException
            │authorization     │        │propagates to caller  │
            │process           │        │HTTP 400 Bad Request  │
            └──────┬───────────┘        └───────────┬───────────┘
                   │                                 │
                   ↓                                 ↓
            ┌──────────────────────────────┐  ┌──────────────────┐
            │TransactionService returns    │  │REST Error Response
            │completed Transaction         │  │with error message
            └──────────────────────────────┘  └──────────────────┘
```

## Database Schema Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    otp_tokens (New)                     │
├─────────────────────────────────────────────────────────┤
│ Primary Key:                                            │
│ ┌─ id: BIGINT (AUTO_INCREMENT)                         │
│                                                         │
│ Data Columns:                                           │
│ ├─ code: VARCHAR(6) NOT NULL                           │
│ │  └─ 6-digit OTP code sent to user                    │
│ │                                                       │
│ ├─ phone_number: VARCHAR(20) NOT NULL                  │
│ │  └─ User's phone in E.164 format (+237681332456)    │
│ │                                                       │
│ ├─ purpose: VARCHAR(50) NOT NULL                       │
│ │  └─ OTP purpose (TRANSACTION_AUTH, PIN_RESET, etc.) │
│ │                                                       │
│ ├─ attempts: INT NOT NULL DEFAULT 0                    │
│ │  └─ Count of failed verification attempts           │
│ │                                                       │
│ ├─ used: BOOLEAN NOT NULL DEFAULT FALSE                │
│ │  └─ Whether OTP has been successfully verified       │
│ │                                                       │
│ ├─ expires_at: TIMESTAMP NOT NULL                      │
│ │  └─ When OTP becomes invalid                         │
│ │                                                       │
│ ├─ verified_at: TIMESTAMP                              │
│ │  └─ When OTP was successfully verified (null = unused)
│ │                                                       │
│ └─ context: VARCHAR(255)                               │
│    └─ Optional context (transaction ID, user ID, etc.) │
│                                                         │
│ Indexes:                                                │
│ ├─ INDEX idx_otp_phone_purpose (phone_number, purpose)│
│ ├─ INDEX idx_otp_code_phone (code, phone_number)      │
│ └─ INDEX idx_otp_expires (expires_at)                 │
└─────────────────────────────────────────────────────────┘

Relationship with other tables:

┌──────────────────┐        ┌──────────────────────┐
│   transaction    │        │   otp_tokens (NEW)   │
├──────────────────┤        ├──────────────────────┤
│ id (PK)          │        │ id (PK)              │
│ reference        │        │ code                 │
│ status           │ <────→ │ phone_number         │
│ amount           │ (via   │ purpose              │
│ currency         │  context└ attempts            │
│ sender_wallet_.. │  field)  │ used                │
│ receiver_wallet..│        │ expires_at           │
│ authorization_.. │        │ verified_at          │
│                  │        │ context (txn_id ref) │
└──────────────────┘        └──────────────────────┘
```

## Component Interaction Matrix

```
                    OtpToken  Repo  Service  Controller  Trans.Svc
OtpToken            ---        R      R         -           -
OtpTokenRepository  W          ---    R         -           -
OtpService          W          R      ---       -           R
OtpMapper           R          -      -         R           -
AuthValidationReq   -          -      R         R           R
OtpAuthResponse     -          -      W         W           -
TransactionService  -          R      R         -           ---
TransactionCtrl     -          -      -         ---         R
WalletClient        -          -      -         -           R

Legend:
R = Reads from / Uses
W = Writes to / Creates
--- = Is itself
- = No direct interaction
```

## Data State Transitions

```
OTP State Machine:

        ┌─────────────────────────────────────┐
        │  NEW OTP Created                    │
        │  used = FALSE                       │
        │  attempts = 0                       │
        │  verified_at = NULL                 │
        └──────────┬──────────────────────────┘
                   │
        ┌──────────┴────────────┐
        │                       │
        ↓                       ↓
   ┌─────────┐            ┌──────────────────┐
   │Expired  │            │Active            │
   │expires_ │            │(waiting for user)│
   │at <now()│            └────────┬─────────┘
   └─────────┘                     │
        ↑          ┌───────────────┼───────────────┐
        │          │               │               │
        │          ↓               ↓               ↓
        │    ┌──────────┐  ┌──────────┐  ┌─────────────┐
        │    │Failed    │  │Failed    │  │Failed       │
        │    │Attempt 1 │  │Attempt 2 │  │Attempt 3    │
        │    │attempts=1│  │attempts=2│  │attempts=3   │
        │    └──────────┘  └──────────┘  └─────┬───────┘
        │         ↓             ↓               │
        │    ┌──────────────────────┐           │
        │    │Active (user retries) │           │
        │    └──────────────────────┘           │
        │                                       │
        │                                       ↓
        │                              ┌──────────────┐
        │                              │Locked        │
        │                              │used = TRUE   │
        │                              │attempts = 3  │
        │                              └──────┬───────┘
        │                                     │
        │                                     │ (user must request new OTP)
        │                                     │
        └─────────────────────────────────────┘
                    OR
                    ↓
           ┌────────────────────┐
           │Verified            │
           │used = TRUE         │
           │verified_at = <time>│
           │attempts = N        │
           └────────────────────┘
                    ↓
           ┌────────────────────┐
           │Transaction         │
           │Authorized &        │
           │Completed           │
           └────────────────────┘
```

---

This comprehensive architecture documentation provides developers with:
- System architecture overview
- Class hierarchy and relationships
- Method call sequences
- Error handling flows
- Database schema
- Component interactions
- Data state transitions

Use these diagrams as reference when implementing OTP functionality or debugging issues.
