# OTP Integration for Transaction Authorization

## Overview
This document describes the integration of OTP (One-Time Password) tokens for transaction authorization in the Zaphira transaction service.

## Database Schema

The OTP tokens are stored in the `otp_tokens` table with the following structure:

```sql
CREATE TABLE otp_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(6) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    attempts INT DEFAULT 0,
    used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    context VARCHAR(255)
);
```

### Fields Description

| Field | Type | Purpose |
|-------|------|---------|
| `id` | BIGINT | Unique identifier for the OTP record |
| `code` | VARCHAR(6) | The 6-digit OTP code |
| `phone_number` | VARCHAR(20) | User's phone number in E.164 format (e.g., +237681332456) |
| `purpose` | VARCHAR(50) | Purpose of OTP (e.g., TRANSACTION_AUTH, PIN_RESET) |
| `attempts` | INT | Number of failed verification attempts |
| `used` | BOOLEAN | Whether the OTP has been successfully verified |
| `expires_at` | TIMESTAMP | Expiration timestamp for the OTP |
| `verified_at` | TIMESTAMP | When the OTP was successfully verified |
| `context` | VARCHAR(255) | Optional context data (e.g., transaction ID) |

## Classes & Components

### 1. OtpToken (Entity)
- **Location**: `common-library/src/main/java/com/zaphira/common/model/entities/OtpToken.java`
- **Purpose**: JPA entity representing OTP tokens
- **Key Methods**:
  - `isValid()`: Checks if OTP is not expired and not used
  - `isMaxAttemptsExceeded(maxAttempts)`: Validates attempt count
  - `markAsVerified()`: Marks OTP as used with verification timestamp

### 2. OtpTokenRepository
- **Location**: `transaction-service/src/main/java/com/zaphira/transaction/repository/OtpTokenRepository.java`
- **Purpose**: JPA repository for OTP token operations
- **Key Methods**:
  - `findActiveOtpByPhoneAndPurpose()`: Get latest unused OTP for phone and purpose
  - `findByPhoneAndCode()`: Find OTP by phone and code
  - `findByPhoneCodeAndPurpose()`: Find OTP by phone, code, and purpose (most specific)
  - `findByIdAndPhone()`: Find OTP by ID and phone (for security validation)

### 3. OtpService
- **Location**: `transaction-service/src/main/java/com/zaphira/transaction/service/otp/OtpService.java`
- **Purpose**: Business logic for OTP management
- **Configuration**:
  - `transaction.otp.max-attempts`: Maximum allowed verification attempts (default: 3)
  - `transaction.otp.purpose`: Default OTP purpose (default: TRANSACTION_AUTH)

#### Key Methods

**verifyOtp(phoneNumber, code)**
- Verifies the OTP code for the given phone number
- Checks: expiry, usage, attempt count
- Throws `OtpVerificationException` on failure
- Marks OTP as verified and sets timestamp

**recordFailedAttempt(phoneNumber, code)**
- Increments attempt counter
- Marks OTP as used if max attempts exceeded
- Used when OTP verification fails

**getActiveOtp(phoneNumber) / getActiveOtp(phoneNumber, purpose)**
- Retrieves the most recent active OTP
- Returns Optional for null-safe handling

**isOtpValid(otp)**
- Validates if OTP is still active and usable

### 4. AuthorizationValidationRequest (DTO)
- **Location**: `transaction-service/src/main/java/com/zaphira/transaction/dto/AuthorizationValidationRequest.java`
- **Updated Fields**:
  - `method`: AuthorizationMethod (OTP, PIN, BIOMETRIC, etc.)
  - `code`: Authorization code/PIN
  - `authorizedBy`: User email or identifier
  - `phoneNumber`: User's phone number (NEW)
  - `otpCode`: OTP code from user (NEW)

### 5. OtpAuthorizationResponse (DTO)
- **Location**: `transaction-service/src/main/java/com/zaphira/transaction/dto/OtpAuthorizationResponse.java`
- **Purpose**: Response DTO for OTP authorization queries
- **Fields**:
  - `transactionId`: ID of the transaction
  - `otpRequired`: Whether OTP is required
  - `phoneNumber`: Masked phone number (security)
  - `otpExpiry`: When the OTP expires
  - `attemptsRemaining`: Remaining attempts for verification
  - `message`: Human-readable status message
  - `status`: OTP status (PENDING, VERIFIED)

### 6. OtpMapper
- **Location**: `transaction-service/src/main/java/com/zaphira/transaction/dto/mapper/OtpMapper.java`
- **Purpose**: Maps OTP entities to response DTOs
- **Key Methods**:
  - `toAuthorizationResponse()`: Convert OtpToken to OtpAuthorizationResponse
  - Masks phone numbers to show only last 4 digits for security

## Integration Points

### Transaction Service Integration

The `authorizeTransaction()` method now includes OTP verification:

```java
@Transactional
public Transaction authorizeTransaction(Long id, AuthorizationValidationRequest request) {
    Transaction tx = getTransaction(id);
    if (!Boolean.TRUE.equals(tx.getAuthorizationRequired())) {
        throw new IllegalStateException("Transaction does not require authorization");
    }
    complianceService.assertNotBlocked(tx);
    
    // ✅ NEW: Verify OTP if provided
    if (request.getPhoneNumber() != null && request.getOtpCode() != null) {
        otpService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
    }
    
    authorizationService.approveAuthorization(id, request.getMethod(), request.getCode(), request.getAuthorizedBy());
    changeStatus(tx, TransactionStatus.AUTHORIZED, request.getAuthorizedBy(), "Authorization approved");
    return processImmediateTransaction(tx, request.getAuthorizedBy());
}
```

## REST API Integration

### POST /api/transactions/{id}/authorize

**Request Body**:
```json
{
  "method": "OTP",
  "code": "123456",
  "authorizedBy": "user@example.com",
  "phoneNumber": "+237681332456",
  "otpCode": "802975"
}
```

**Success Response (200 OK)**:
```json
{
  "success": true,
  "message": "Transaction authorized successfully",
  "data": {
    "id": 18,
    "status": "COMPLETED",
    "amount": "5000.00",
    "currency": "XOF",
    "senderWalletNumber": "SENDER123",
    "receiverWalletNumber": "RECEIVER456"
  }
}
```

**OTP Verification Failure (400 Bad Request)**:
```json
{
  "success": false,
  "message": "Invalid OTP code",
  "error": "OtpVerificationException"
}
```

**Max Attempts Exceeded (400 Bad Request)**:
```json
{
  "success": false,
  "message": "Maximum OTP verification attempts exceeded",
  "error": "OtpVerificationException"
}
```

## Configuration

Add to `application.properties`:

```properties
# OTP Configuration for Transaction Authorization
transaction.otp.max-attempts=3
transaction.otp.purpose=TRANSACTION_AUTH
```

## Error Handling

The OTP service throws `OtpService.OtpVerificationException` for:
1. **Invalid Code**: OTP not found or code mismatch
2. **Expired OTP**: Current time is past expiration
3. **Already Used**: OTP marked as used
4. **Max Attempts Exceeded**: User exceeded maximum attempts

## Security Considerations

1. **Phone Number Masking**: Phone numbers are masked in responses (shows only last 4 digits)
2. **Attempt Limiting**: Maximum 3 attempts before OTP is locked
3. **Expiration**: OTPs expire after configured time window
4. **One-Time Use**: OTP is marked as used after successful verification
5. **Context Tracking**: Optional context field for transaction-OTP linkage

## Data Flow

```
User initiates transaction
    ↓
System generates OTP and sends to phone
    ↓
OTP stored in otp_tokens table
    ↓
User receives authorization request
    ↓
User submits: phoneNumber + otpCode
    ↓
verifyOtp() validates:
  - Code matches
  - Not expired
  - Not already used
  - Attempts < max
    ↓
OTP marked as verified (used = true, verified_at = now)
    ↓
Transaction proceeds to authorization
    ↓
Wallet transfer executed
    ↓
Transaction marked as COMPLETED
```

## Testing

### Test OTP Verification
```sql
-- Create test OTP
INSERT INTO otp_tokens (code, phone_number, purpose, attempts, used, expires_at)
VALUES ('802975', '+237681332456', 'TRANSACTION_AUTH', 0, false, NOW() + INTERVAL 5 MINUTE);

-- Verify OTP details
SELECT * FROM otp_tokens WHERE phone_number = '+237681332456' ORDER BY id DESC LIMIT 1;
```

### PowerShell Integration Test
```powershell
$otpCode = "802975"
$phoneNumber = "+237681332456"
$authCode = "123456"

$body = @{
    method = "OTP"
    code = $authCode
    authorizedBy = "user@example.com"
    phoneNumber = $phoneNumber
    otpCode = $otpCode
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8083/api/transactions/18/authorize" `
    -Method Post `
    -Headers @{
        "Authorization" = "Bearer <JWT_TOKEN>"
        "Content-Type" = "application/json"
    } `
    -Body $body

$response | ConvertTo-Json -Depth 10
```

## Future Enhancements

1. **OTP Generation Service**: Create automated OTP generation and SMS sending
2. **OTP Resend**: Allow users to request new OTP if first one expires
3. **OTP Audit Logging**: Track all OTP-related events for compliance
4. **Custom OTP Format**: Support numeric, alphanumeric, or custom patterns
5. **Multi-channel**: Support Email OTP, Push notifications, etc.
6. **Rate Limiting**: Limit OTP generation frequency per phone number
