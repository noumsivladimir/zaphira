# Email Verification Testing Guide

## Overview
This guide explains how to test the email verification system for user registration in the Zaphira microservices architecture.

## System Architecture
- **User Service**: Handles user registration and OTP generation
- **Notification Service**: Processes email sending via Kafka events
- **Kafka**: Message broker for inter-service communication

## Email Verification Flow
1. User registers with email and phone number
2. Account status set to `PENDING_VERIFICATION`
3. OTP code generated and sent via email
4. User receives email with verification code
5. User verifies email with OTP code
6. Account status changes to `ACTIVE`
7. Wallet is created automatically

## Prerequisites
- All services compiled and ready
- PostgreSQL database running
- Kafka broker running on `192.168.0.122:9092`
- Email server configured (check notification service logs)

## Testing Steps

### 1. Start Services
Run the batch file to start all services:
```bash
./test_email_registration.bat
```

Or start services manually:
```bash
# Terminal 1: Config Server
cd config-server && mvn spring-boot:run

# Terminal 2: Eureka Server
cd service-registry && mvn spring-boot:run

# Terminal 3: Notification Service (with Kafka profile)
cd notification-service && mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=kafka

# Terminal 4: User Service
cd user-service && mvn spring-boot:run
```

### 2. Test Registration
Use the PowerShell script:
```powershell
./test_email_registration.ps1
```

Or use curl:
```bash
curl -X POST http://localhost:8082/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+237690000001",
    "email": "test@example.com",
    "pin": "1234",
    "firstName": "Test",
    "lastName": "User",
    "dateOfBirth": "1990-01-01",
    "country": "Cameroon",
    "neighborhood": "Test Neighborhood",
    "city": "Yaounde",
    "region": "Centre",
    "preferredLanguage": "fr"
  }'
```

### 3. Check Logs
Monitor the logs for:
- User registration with `PENDING_VERIFICATION` status
- OTP generation in user-service logs
- Kafka event publishing: `Publishing email event to topic: email-send-events`
- Email event consumption in notification-service logs
- Actual email sending confirmation

### 4. Verify Email
Check your email for the verification code, then verify:
```bash
curl -X POST http://localhost:8082/api/users/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "verificationCode": "<OTP_CODE_FROM_EMAIL>"
  }'
```

## Expected Results
- Registration response shows `accountStatus: "PENDING_VERIFICATION"`
- Email is sent with OTP code
- Verification activates account: `accountStatus: "ACTIVE"`
- Wallet is created automatically

## Troubleshooting
- **No email received**: Check notification service logs for email sending errors
- **Kafka connection issues**: Verify Kafka broker is running
- **Database errors**: Check PostgreSQL connection
- **Service startup failures**: Ensure all dependencies are compiled

## Key Components Verified
- ✅ `OtpServiceImpl.generateAndSendOtp()` - OTP generation and email sending
- ✅ `EmailServiceImpl.sendEmail()` - Kafka event publishing
- ✅ `EmailEventListener` - Email event consumption and sending
- ✅ `UserRegistrationServiceImpl.registerRegularUser()` - Registration flow
- ✅ `UserRegistrationServiceImpl.verifyEmailAndActivateAccount()` - Email verification
- ✅ Kafka topic configuration: `email-send-events`
- ✅ User status transitions: `PENDING_VERIFICATION` → `ACTIVE`