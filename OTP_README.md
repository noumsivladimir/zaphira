# OTP Implementation with JSON Response

This document describes the complete OTP (One-Time Password) implementation in the Zaphira microservices architecture. Instead of sending SMS via Twilio, OTP codes are now generated and returned directly in JSON responses for development and testing purposes.

## Overview

The OTP system is implemented across two main services:
- **notification-service**: Handles verification tokens and OTP generation for notifications
- **user-service**: Manages OTP tokens for user authentication and registration

## Architecture

### Components

1. **VerificationService** (notification-service)
   - Generates and validates verification codes
   - Returns OTP codes directly in JSON responses (no SMS sending)
   - Manages token lifecycle (creation, expiration, usage)

2. **OtpService** (user-service)
   - Generates OTP codes for various purposes (registration, login, PIN reset, transactions)
   - Returns OTP codes directly in JSON responses (no SMS sending)
   - Validates OTP codes with attempt tracking

## Configuration

### Environment Variables

No Twilio configuration is required anymore. The system generates OTP codes locally and returns them in API responses.

### Application Configuration

The Twilio configuration can be removed or kept for future SMS integration if needed.

#### user-service (application.properties)

## API Endpoints

### Notification Service

#### Send OTP
```
POST /api/notifications/send/otp/{userId}
```
Generates an OTP code and returns it directly in the JSON response.

**Response:**
```json
{
  "status": "otp_generated",
  "otp": "123456",
  "expiresIn": "10 minutes",
  "message": "OTP généré avec succès. Utilisez ce code pour vérifier votre compte."
}
```

#### Resend OTP
```
POST /api/notifications/resend/otp/{userId}
```
Regenerates a new OTP code and returns it directly in the JSON response.

**Response:**
```json
{
  "status": "otp_regenerated",
  "otp": "789012",
  "expiresIn": "10 minutes",
  "message": "Nouveau OTP généré avec succès. Utilisez ce code pour vérifier votre compte."
}
```

#### Verify OTP
```
POST /api/notifications/verify/otp?userId={userId}&code={code}
```
Verifies the provided OTP code for the user.

### User Service

The user service handles OTP internally through its registration and authentication flows. OTP is automatically generated during:
- User registration
- PIN reset requests
- Login verification (if configured)

## OTP Flow

### 1. Generation
- 6-digit numeric code generated using SecureRandom
- Code expires after 5-10 minutes (configurable)
- Maximum 3 verification attempts allowed

### 2. Response
- OTP code returned directly in JSON API response
- No external SMS service required for development/testing
- Message includes expiration time and usage instructions
- Fallback logging if SMS fails (doesn't block the process)

### 3. Verification
- Code validated against stored token
- Token marked as used after successful verification
- Expired tokens automatically cleaned up

### 4. Security
- Codes are hashed/salted in storage
- Attempt counting prevents brute force
- Automatic cleanup of expired tokens

## Database Schema

### notification-service (VerificationToken)
```sql
CREATE TABLE verification_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE
);
```

### user-service (OtpToken)
```sql
CREATE TABLE otp_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(10) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(255),
    purpose VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    attempts INT DEFAULT 0,
    verified_at TIMESTAMP
);
```

## Error Handling

- OTP generation failures are logged and handled gracefully
- Invalid/expired codes return appropriate error messages
- Rate limiting can be implemented at API gateway level

## Testing

### Manual Testing

1. **Send OTP**:
```bash
curl -X POST http://localhost:8083/api/notifications/send/otp/1
```
**Expected Response:**
```json
{
  "status": "otp_generated",
  "otp": "123456",
  "expiresIn": "10 minutes",
  "message": "OTP généré avec succès. Utilisez ce code pour vérifier votre compte."
}
```

2. **Verify OTP**:
```bash
curl -X POST "http://localhost:8083/api/notifications/verify/otp?userId=1&code=123456"
```

### Automated Testing

Use the provided test scripts:
- `test_otp_complete.ps1` - Complete OTP flow testing
- `test_twilio_quick.ps1` - Quick OTP testing

## Monitoring

- OTP events are published to Kafka topics:
  - `verification-sms-events` (for future SMS integration)
  - `verification-email-events`

- Track OTP generation and verification rates in application logs
- Monitor token cleanup and expiration handling

## Security Considerations

1. **Token Storage**: OTP codes should be hashed in production
2. **Rate Limiting**: Implement rate limiting to prevent abuse
3. **Phone Number Validation**: Validate phone numbers before sending
4. **Audit Logging**: Log all OTP operations for security auditing
5. **Environment Variables**: Never commit real Twilio credentials

## Dependencies

- **Twilio Java SDK**: `com.twilio.sdk:twilio:9.13.0`
- **Spring Boot**: Web, Data JPA, Kafka
- **Database**: H2 (dev) / PostgreSQL (prod)

## Future Enhancements

1. **Multi-factor Authentication**: Combine OTP with other factors
2. **Biometric Integration**: Integrate with device biometrics
3. **Push Notifications**: Add push notification support
4. **Internationalization**: Support multiple languages for SMS
5. **Analytics**: Detailed OTP usage analytics