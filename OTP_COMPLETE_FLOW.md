# OTP Transaction Authorization - Complete Flow

## End-to-End Transaction Authorization with OTP

### Scenario
User wants to transfer 5000 XOF and needs OTP authorization.

## Step 1: User Initiates Transaction

### REST Request
```http
POST /api/transactions
Host: localhost:8083
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "senderWalletNumber": "SENDER123",
  "receiverWalletNumber": "RECEIVER456",
  "amount": 5000,
  "currency": "XOF",
  "description": "Payment for services"
}
```

### Database State After Step 1
```sql
-- Transaction created
INSERT INTO transaction (
  reference, sender_wallet_number, receiver_wallet_number, amount, 
  currency, status, authorization_required, requested_by, requested_at
) VALUES (
  'TXN20251213001', 'SENDER123', 'RECEIVER456', 5000, 'XOF',
  'PENDING', true, 'user@example.com', NOW()
);

-- Result: Transaction ID 18 created with status PENDING, authorizationRequired = true
```

### Response
```json
{
  "success": true,
  "message": "Transaction created successfully",
  "data": {
    "id": 18,
    "reference": "TXN20251213001",
    "status": "PENDING",
    "amount": "5000.00",
    "currency": "XOF",
    "authorizationRequired": true,
    "senderWalletNumber": "SENDER123",
    "receiverWalletNumber": "RECEIVER456"
  }
}
```

## Step 2: System Generates and Stores OTP

### Backend Process
```
1. Transaction requires authorization
2. System generates 6-digit OTP: 802975
3. System determines user's phone: +237681332456
4. OTP stored in database with expiration (5 minutes)
5. OTP sent to user's phone via SMS (external service)
```

### Database Insert
```sql
INSERT INTO otp_tokens (
  code, phone_number, purpose, attempts, used, expires_at, context
) VALUES (
  '802975', '+237681332456', 'TRANSACTION_AUTH', 0, false,
  NOW() + INTERVAL 5 MINUTE, 'txn_18'
);

-- Result: OTP Token ID 3 created
SELECT * FROM otp_tokens WHERE id = 3;
-- id | code  | phone_number    | purpose          | attempts | used  | expires_at           | verified_at | context
-- 3  | 802975| +237681332456   | TRANSACTION_AUTH | 0        | false | 2025-12-13 18:16:53 | NULL        | txn_18
```

## Step 3: User Receives OTP

User receives SMS on +237681332456:
```
Your Zaphira authorization code is: 802975
Valid for 5 minutes
```

## Step 4: User Submits Authorization Request with OTP

### REST Request
```http
POST /api/transactions/18/authorize
Host: localhost:8083
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "method": "OTP",
  "code": "123456",
  "authorizedBy": "user@example.com",
  "phoneNumber": "+237681332456",
  "otpCode": "802975"
}
```

### Authorization Service Processing

#### 4a. Verify OTP

```java
// TransactionService.authorizeTransaction()
if (request.getPhoneNumber() != null && request.getOtpCode() != null) {
    otpService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
    // Throws exception if invalid/expired/max attempts
}
```

#### 4b. Query OTP from Database

```sql
-- OtpTokenRepository.findByPhoneCodeAndPurpose()
SELECT * FROM otp_tokens 
WHERE phone_number = '+237681332456'
  AND code = '802975'
  AND purpose = 'TRANSACTION_AUTH'
  AND used = false
ORDER BY id DESC
LIMIT 1;

-- Result: Found OTP token with id=3
```

#### 4c. Validate OTP

Service checks:
```java
// 1. OTP exists - ✅ PASSED
// 2. Not expired - ✅ PASSED (expires_at = 18:16:53, now = 18:12:45)
// 3. Not used - ✅ PASSED (used = false)
// 4. Attempts < max - ✅ PASSED (attempts = 0, max = 3)
```

#### 4d. Mark OTP as Verified

```sql
UPDATE otp_tokens 
SET used = true, verified_at = NOW()
WHERE id = 3;

-- Result: OTP marked as used
SELECT * FROM otp_tokens WHERE id = 3;
-- id | code  | phone_number    | purpose          | attempts | used | verified_at          | context
-- 3  | 802975| +237681332456   | TRANSACTION_AUTH | 0        | true | 2025-12-13 18:12:47 | txn_18
```

## Step 5: Authorization Approved

### Database State After Approval
```sql
-- 1. Update transaction status to AUTHORIZED
UPDATE transaction 
SET status = 'AUTHORIZED', authorized_at = NOW(), authorization_method = 'OTP'
WHERE id = 18;

-- 2. Create authorization request record
INSERT INTO authorization_request (
  transaction_id, method, status, requested_at, requested_by
) VALUES (
  18, 'OTP', 'APPROVED', NOW(), 'user@example.com'
);

-- 3. Record state history
INSERT INTO transaction_state_history (
  transaction_id, status, changed_by, changed_at, reason
) VALUES (
  18, 'AUTHORIZED', 'user@example.com', NOW(), 'Authorization approved'
);
```

## Step 6: Transaction Processing Begins

### Database State After Processing
```sql
-- 1. Update transaction status to PROCESSING
UPDATE transaction 
SET status = 'PROCESSING', processing_started_at = NOW()
WHERE id = 18;

-- 2. Call wallet service to execute transfer
-- (External call to wallet-service)

-- 3. Upon success, update transaction status
UPDATE transaction 
SET status = 'COMPLETED', completed_at = NOW()
WHERE id = 18;

-- 4. Record completion in state history
INSERT INTO transaction_state_history (
  transaction_id, status, changed_by, changed_at, reason
) VALUES (
  18, 'COMPLETED', 'SYSTEM', NOW(), 'Transaction completed successfully'
);
```

### Wallet Service Call
```java
walletClient.executeTransfer(
    WalletTransferRequest.builder()
        .reference("TXN20251213001")
        .senderWalletNumber("SENDER123")
        .receiverWalletNumber("RECEIVER456")
        .amount(new BigDecimal("5000.00"))
        .currency("XOF")
        .description("Payment for services")
        .build()
);
```

## Step 7: Success Response

### REST Response
```json
{
  "success": true,
  "message": "Transaction authorized and completed successfully",
  "data": {
    "id": 18,
    "reference": "TXN20251213001",
    "status": "COMPLETED",
    "amount": "5000.00",
    "currency": "XOF",
    "senderWalletNumber": "SENDER123",
    "receiverWalletNumber": "RECEIVER456",
    "authorizedAt": "2025-12-13T18:12:47Z",
    "completedAt": "2025-12-13T18:12:50Z",
    "authorizationMethod": "OTP"
  }
}
```

## Error Scenarios

### Scenario 1: Invalid OTP Code

#### Request
```json
{
  "method": "OTP",
  "code": "123456",
  "authorizedBy": "user@example.com",
  "phoneNumber": "+237681332456",
  "otpCode": "999999"
}
```

#### Database Query
```sql
SELECT * FROM otp_tokens 
WHERE phone_number = '+237681332456'
  AND code = '999999'
  AND purpose = 'TRANSACTION_AUTH'
  AND used = false;

-- Result: No rows found - OTP code mismatch
```

#### Service Response
```java
// OtpService.verifyOtp() throws:
throw new OtpVerificationException("Invalid OTP code");
```

#### HTTP Response (400 Bad Request)
```json
{
  "success": false,
  "message": "Invalid OTP code",
  "error": "OtpVerificationException"
}
```

#### Database State
Transaction remains in PENDING status:
```sql
SELECT status FROM transaction WHERE id = 18;
-- status: PENDING (not changed)
```

### Scenario 2: OTP Expired

#### Database State Before Attempt
```sql
SELECT * FROM otp_tokens WHERE id = 3;
-- expires_at = 2025-12-13 18:11:53 (5 minutes ago)
-- Current time = 2025-12-13 18:16:53
```

#### Service Validation
```java
if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
    throw new OtpVerificationException("OTP has expired");
}
```

#### HTTP Response (400 Bad Request)
```json
{
  "success": false,
  "message": "OTP has expired",
  "error": "OtpVerificationException"
}
```

### Scenario 3: Maximum Attempts Exceeded

#### First Attempt - Wrong Code
```
User submits: 111111 (wrong)
Service increments: attempts = 1
User notified: 2 attempts remaining
```

#### Database After First Attempt
```sql
UPDATE otp_tokens SET attempts = 1 WHERE id = 3;

SELECT * FROM otp_tokens WHERE id = 3;
-- attempts: 1, used: false
```

#### Second Attempt - Wrong Code Again
```
User submits: 222222 (wrong)
Service increments: attempts = 2
User notified: 1 attempt remaining
```

#### Third Attempt - Wrong Code
```
User submits: 333333 (wrong)
Service increments: attempts = 3
Service checks: attempts (3) >= maxAttempts (3)
Service locks OTP: used = true
```

#### Database After Third Attempt
```sql
UPDATE otp_tokens SET attempts = 3, used = true WHERE id = 3;

SELECT * FROM otp_tokens WHERE id = 3;
-- attempts: 3, used: true (locked)
```

#### Service Response
```java
if (otp.getAttempts() >= maxAttempts) {
    log.warn("Max OTP attempts exceeded for phone: {}", phoneNumber);
    otp.setUsed(true);
    otpTokenRepository.save(otp);
    throw new OtpVerificationException("Maximum OTP verification attempts exceeded");
}
```

#### HTTP Response (400 Bad Request)
```json
{
  "success": false,
  "message": "Maximum OTP verification attempts exceeded",
  "error": "OtpVerificationException"
}
```

User must request a new OTP.

## Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: User Initiates Transaction                              │
│ POST /api/transactions                                          │
│ -> Transaction created with status=PENDING, authRequired=true   │
│ -> Transaction ID = 18                                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: System Generates OTP                                    │
│ - Generate code: 802975                                         │
│ - Phone: +237681332456                                          │
│ - Store in otp_tokens table                                     │
│ - Send SMS to user                                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 3-4: User Submits Authorization with OTP                   │
│ POST /api/transactions/18/authorize                             │
│ {phoneNumber: "+237681332456", otpCode: "802975"}              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                ┌─────────────────────────┐
                │ OTP Verification Logic  │
                └─────────────────────────┘
                     ↙          ↓          ↘
         ┌──────────┴───┐   ┌──────┐   ┌──────────────┐
         │ Query DB     │   │Validate│  │ Mark Used    │
         │ find OTP     │   │Checks  │  │ Set verified │
         └──────────────┘   └──────┘   └──────────────┘
                              ↓
                ┌─────────────────────────┐
                │ OTP Valid?              │
                └──────────┬──────────────┘
                    ┌──────┴──────┐
                    │             │
                   YES           NO
                    │             │
                    ↓             ↓
            ┌──────────────┐  ┌──────────────┐
            │ Approve Auth │  │ Return Error │
            │ Change Status│  │ Increment    │
            │ -> AUTH      │  │ Attempts     │
            └──────────────┘  └──────────────┘
                    │             │
                    ↓             ↓
            ┌──────────────┐  HTTP 400
            │Process Immedi│  {success:false}
            │ateTransaction│
            │ Call Wallet  │
            │ Service      │
            └──────────────┘
                    │
                    ↓
            ┌──────────────┐
            │Update Status │
            │-> COMPLETED  │
            └──────────────┘
                    │
                    ↓
            HTTP 200 OK with
            Transaction Data
```

## Monitoring & Analytics Queries

### OTP Success Rate
```sql
SELECT 
  DATE(verified_at) as verification_date,
  COUNT(CASE WHEN used = true THEN 1 END) as successful_verifications,
  COUNT(*) as total_otps_generated,
  ROUND(COUNT(CASE WHEN used = true THEN 1 END)::numeric / COUNT(*) * 100, 2) as success_rate
FROM otp_tokens
WHERE purpose = 'TRANSACTION_AUTH'
  AND verified_at IS NOT NULL
GROUP BY DATE(verified_at)
ORDER BY verification_date DESC;
```

### Failed Attempts Analysis
```sql
SELECT 
  phone_number,
  COUNT(*) as total_attempts,
  COUNT(CASE WHEN used = true AND attempts >= 3 THEN 1 END) as locked_otps,
  AVG(attempts) as avg_attempts_before_success
FROM otp_tokens
WHERE purpose = 'TRANSACTION_AUTH'
GROUP BY phone_number
HAVING COUNT(*) > 5
ORDER BY total_attempts DESC;
```

### OTP Validity Metrics
```sql
SELECT 
  'Expired' as category,
  COUNT(*) as count
FROM otp_tokens
WHERE expires_at < NOW() AND used = false
UNION ALL
SELECT 
  'Active',
  COUNT(*)
FROM otp_tokens
WHERE expires_at > NOW() AND used = false
UNION ALL
SELECT 
  'Verified',
  COUNT(*)
FROM otp_tokens
WHERE used = true;
```
