# OTP Integration - Quick Reference Guide

## For Developers

### How to Use OTP Service in Your Code

#### 1. Inject OtpService
```java
@Service
public class MyService {
    private final OtpService otpService;
    
    public MyService(OtpService otpService) {
        this.otpService = otpService;
    }
}
```

#### 2. Verify OTP
```java
// Verify with default purpose (TRANSACTION_AUTH)
try {
    OtpToken verifiedOtp = otpService.verifyOtp("+237681332456", "802975");
    // OTP verified successfully
} catch (OtpService.OtpVerificationException e) {
    // Handle error: Invalid code, expired, max attempts, etc.
    log.error("OTP verification failed: {}", e.getMessage());
}
```

#### 3. Verify OTP with Custom Purpose
```java
try {
    OtpToken otp = otpService.verifyOtp("+237681332456", "123456", "PIN_RESET");
} catch (OtpService.OtpVerificationException e) {
    log.error("PIN reset OTP failed: {}", e.getMessage());
}
```

#### 4. Record Failed Attempt
```java
// When OTP verification fails, record the attempt
otpService.recordFailedAttempt("+237681332456", "123456");
```

#### 5. Get Active OTP
```java
// Get most recent active OTP for a phone
Optional<OtpToken> activeOtp = otpService.getActiveOtp("+237681332456");
if (activeOtp.isPresent()) {
    OtpToken otp = activeOtp.get();
    // Do something with the OTP
}
```

#### 6. Check OTP Validity
```java
OtpToken otp = // ... get from somewhere
if (otpService.isOtpValid(otp)) {
    // OTP is valid and can be used
}
```

## Common Patterns

### Pattern 1: Authorization with OTP
```java
@PostMapping("/{id}/authorize")
public ResponseEntity<?> authorize(
        @PathVariable Long id,
        @RequestBody AuthorizationValidationRequest request) {
    
    // Verify OTP first
    if (request.getPhoneNumber() != null && request.getOtpCode() != null) {
        try {
            otpService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
        } catch (OtpService.OtpVerificationException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    // Continue with authorization
    Transaction tx = transactionService.authorizeTransaction(id, request);
    return ResponseEntity.ok(tx);
}
```

### Pattern 2: OTP Verification with Fallback
```java
public boolean verifyWithRetry(String phone, String code, int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        try {
            otpService.verifyOtp(phone, code);
            return true;
        } catch (OtpService.OtpVerificationException e) {
            log.warn("Attempt {} failed: {}", i + 1, e.getMessage());
            if (i == maxRetries - 1) {
                return false;
            }
            // Wait before retry
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
    return false;
}
```

### Pattern 3: OTP Response DTO
```java
@GetMapping("/{id}/otp-status")
public ResponseEntity<OtpAuthorizationResponse> getOtpStatus(@PathVariable Long id) {
    Transaction tx = transactionService.getTransaction(id);
    Optional<OtpToken> otp = otpService.getActiveOtp(tx.getPhoneNumber());
    
    if (otp.isPresent()) {
        OtpAuthorizationResponse response = otpMapper.toAuthorizationResponse(
            otp.get(), id, maxAttempts
        );
        return ResponseEntity.ok(response);
    }
    
    return ResponseEntity.notFound().build();
}
```

## Error Handling

### Exception Types
```java
try {
    otpService.verifyOtp(phone, code);
} catch (OtpService.OtpVerificationException e) {
    String message = e.getMessage();
    
    // Check the specific error
    if (message.contains("Invalid OTP code")) {
        // Wrong code
    } else if (message.contains("expired")) {
        // OTP expired
    } else if (message.contains("already been used")) {
        // Reused OTP
    } else if (message.contains("Maximum OTP verification attempts")) {
        // Too many attempts
    }
}
```

### Logging Best Practices
```java
try {
    otpService.verifyOtp("+237681332456", "802975");
    log.info("OTP verification succeeded for phone: {}", maskPhone("+237681332456"));
} catch (OtpService.OtpVerificationException e) {
    log.warn("OTP verification failed: {} for phone: {}", 
        e.getMessage(), maskPhone("+237681332456"));
}

private String maskPhone(String phone) {
    if (phone.length() < 4) return phone;
    return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
}
```

## Testing

### Unit Test Template
```java
@SpringBootTest
class OtpServiceTest {
    
    @Autowired
    private OtpService otpService;
    
    @Autowired
    private OtpTokenRepository otpTokenRepository;
    
    @Test
    void testVerifyValidOtp() {
        // Given
        String phone = "+237681332456";
        String code = "802975";
        OtpToken otp = OtpToken.builder()
            .code(code)
            .phoneNumber(phone)
            .purpose("TRANSACTION_AUTH")
            .attempts(0)
            .used(false)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
        otpTokenRepository.save(otp);
        
        // When
        OtpToken verified = otpService.verifyOtp(phone, code);
        
        // Then
        assertTrue(verified.getUsed());
        assertNotNull(verified.getVerifiedAt());
    }
    
    @Test
    void testVerifyExpiredOtp() {
        // Given
        String phone = "+237681332456";
        String code = "123456";
        OtpToken otp = OtpToken.builder()
            .code(code)
            .phoneNumber(phone)
            .purpose("TRANSACTION_AUTH")
            .attempts(0)
            .used(false)
            .expiresAt(LocalDateTime.now().minusMinutes(1)) // Expired
            .build();
        otpTokenRepository.save(otp);
        
        // When & Then
        assertThrows(OtpService.OtpVerificationException.class,
            () -> otpService.verifyOtp(phone, code));
    }
}
```

### Integration Test with REST
```bash
# Get OTP status
curl -X GET http://localhost:8083/api/transactions/18/otp-status \
  -H "Authorization: Bearer <JWT>"

# Authorize with OTP
curl -X POST http://localhost:8083/api/transactions/18/authorize \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{
    "method": "OTP",
    "code": "123456",
    "authorizedBy": "user@example.com",
    "phoneNumber": "+237681332456",
    "otpCode": "802975"
  }'
```

## Configuration

### Default Values
```properties
transaction.otp.max-attempts=3
transaction.otp.purpose=TRANSACTION_AUTH
```

### Customization Examples
```properties
# Increase attempts for testing
transaction.otp.max-attempts=5

# Use different purpose
transaction.otp.purpose=PIN_VERIFICATION

# Or override in code
@Value("${transaction.otp.max-attempts:3}")
private int maxAttempts;
```

## Database Queries

### View All OTPs for a Phone
```sql
SELECT * FROM otp_tokens 
WHERE phone_number = '+237681332456'
ORDER BY id DESC;
```

### View Active OTPs
```sql
SELECT * FROM otp_tokens 
WHERE used = false 
  AND expires_at > NOW()
ORDER BY id DESC;
```

### View Failed OTPs
```sql
SELECT * FROM otp_tokens 
WHERE attempts >= 3
ORDER BY id DESC
LIMIT 10;
```

### Check OTP Status
```sql
SELECT 
  id,
  code,
  phone_number,
  attempts,
  used,
  expires_at > NOW() as is_valid,
  EXTRACT(EPOCH FROM (expires_at - NOW())) as seconds_until_expiry
FROM otp_tokens 
WHERE phone_number = '+237681332456'
ORDER BY id DESC
LIMIT 1;
```

## Troubleshooting

### Issue: OTP not found
- Check if OTP actually exists in database
- Verify phone number format matches (+237681332456)
- Check purpose matches configured value

### Issue: OTP expired
- Check expires_at timestamp in database
- Ensure server time is synchronized
- Consider extending OTP validity in config

### Issue: Max attempts exceeded
- OTP becomes locked after 3 failed attempts
- User needs new OTP to be generated
- Implement resend OTP endpoint

### Issue: OTP marked as used too quickly
- Verify verifiedAt timestamp
- Check if transaction is being replayed
- Add idempotency checks if needed

## Performance Tips

1. **Index your queries**:
```sql
CREATE INDEX idx_otp_phone_purpose ON otp_tokens(phone_number, purpose);
CREATE INDEX idx_otp_code_phone ON otp_tokens(code, phone_number);
```

2. **Cleanup expired OTPs** periodically:
```sql
DELETE FROM otp_tokens WHERE expires_at < NOW() - INTERVAL 30 DAY;
```

3. **Cache active OTP status**:
```java
@Cacheable(value = "activeOtps", key = "#phoneNumber")
public Optional<OtpToken> getActiveOtp(String phoneNumber) {
    return otpTokenRepository.findActiveOtpByPhoneAndPurpose(phoneNumber, otpPurpose);
}
```

## Security Reminders

✅ Always mask phone numbers in logs
✅ Never log OTP codes
✅ Use HTTPS for all OTP endpoints
✅ Implement rate limiting on OTP verification
✅ Set appropriate expiration times
✅ Limit attempts to prevent brute force
✅ Validate phone number format
✅ Consider CAPTCHA for repeated failures
