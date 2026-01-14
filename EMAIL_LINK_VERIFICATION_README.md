# Email Verification with Link - Testing Guide

## Overview
This guide explains how to test the enhanced email verification system with clickable verification links in the Zaphira microservices architecture.

## System Architecture
- **User Service**: Handles user registration and OTP generation
- **Notification Service**: Processes HTML email sending via Kafka events
- **Kafka**: Message broker for inter-service communication

## Enhanced Email Verification Flow
1. User registers with email and phone number
2. Account status set to `PENDING_VERIFICATION`
3. OTP code generated and sent via **HTML email with verification link**
4. User receives professional HTML email with clickable button
5. User clicks verification button → **automatic verification**
6. Account status changes to `ACTIVE`
7. Wallet is created automatically
8. Success page displayed

## Key Improvements
- ✅ **One-click verification** - No manual API calls needed
- ✅ **Professional HTML emails** with modern design
- ✅ **Automatic account activation** when link is clicked
- ✅ **Fallback verification code** included in email
- ✅ **Success/error pages** for better user experience

## Email Content
The verification email now includes:
- **Clickable verification button** that automatically verifies the email
- **Verification code** as backup for manual verification
- **Professional styling** with Zaphira branding
- **Expiration warning** (links expire with OTP)
- **Security information** and contact details

## API Endpoints

### Registration (unchanged)
```http
POST /api/users/register
```

### Manual Verification (still available)
```http
POST /api/users/verify-email
Content-Type: application/json

{
  "email": "user@example.com",
  "verificationCode": "123456"
}
```

### NEW: Link Verification
```http
GET /api/users/verify-email-link?email=user@example.com&code=123456
```
Returns HTML success page or error page.

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

### 2. Test Registration with Link
Use the PowerShell script:
```powershell
./test_email_link_verification.ps1
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

### 3. Check Email
The email sent will contain:
- A prominent green "Vérifier mon email" button
- The verification code as backup
- Professional styling and branding
- Expiration information

### 4. Click Verification Link
Clicking the verification button will:
- Automatically call the verification endpoint
- Activate the user account
- Create the wallet
- Display a success page with user information

### 5. Alternative: Manual Verification
If the link doesn't work, users can still verify manually:
```bash
curl -X POST http://localhost:8082/api/users/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "verificationCode": "CODE_FROM_EMAIL"
  }'
```

## Expected Results
- Registration response shows `accountStatus: "PENDING_VERIFICATION"`
- HTML email is sent with clickable verification button
- Clicking button shows success page with account details
- Account status changes to `ACTIVE`
- Wallet is created automatically

## Success Page
The success page displays:
- ✅ Success confirmation
- User account information
- Account status (ACTIVE)
- Link to login page
- Professional styling

## Error Handling
If verification fails, an error page shows:
- ❌ Error message
- Possible reasons (expired link, invalid code)
- Link back to registration
- Contact information

## Troubleshooting
- **No email received**: Check notification service logs for email sending errors
- **Link not working**: Verify URL encoding and parameters
- **Kafka connection issues**: Verify Kafka broker is running
- **Database errors**: Check PostgreSQL connection
- **Service startup failures**: Ensure all dependencies are compiled

## Key Components Enhanced
- ✅ `OtpServiceImpl.sendVerificationEmail()` - Now sends HTML emails with links
- ✅ `UserController.verifyEmailViaLink()` - NEW endpoint for link verification
- ✅ `EmailServiceImpl.sendHtmlEmail()` - Publishes HTML email events
- ✅ `EmailEventListener` - Processes HTML emails
- ✅ Professional email templates with responsive design
- ✅ Automatic verification flow with success/error pages

## Benefits
- **Better UX**: One-click verification instead of manual API calls
- **Professional appearance**: Branded HTML emails
- **Fallback options**: Manual verification still available
- **Security**: Links expire with OTP codes
- **Mobile-friendly**: Responsive email design