# Test Email Verification Registration
Write-Host "========================================" -ForegroundColor Green
Write-Host "Testing Email Verification Registration" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Test user registration
Write-Host "`nTesting user registration..." -ForegroundColor Yellow
$registrationResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" -Method POST -ContentType "application/json" -Body '{
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

Write-Host "Registration Response:" -ForegroundColor Cyan
$registrationResponse | ConvertTo-Json

# Extract user ID for verification
$userId = $registrationResponse.data.userId
Write-Host "`nUser registered with ID: $userId" -ForegroundColor Green
Write-Host "Status: $($registrationResponse.data.accountStatus)" -ForegroundColor Yellow

# Note: In a real test, you would need to check the email for the OTP code
# For this demo, we'll assume the OTP is logged or you can check the database
Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Next steps:" -ForegroundColor White
Write-Host "1. Check the email for the verification code" -ForegroundColor White
Write-Host "2. Use the code to verify the email:" -ForegroundColor White
Write-Host "   POST http://localhost:8082/api/users/verify-email" -ForegroundColor White
Write-Host "   Body: {`"email`": `"test@example.com`", `"verificationCode`": `"`<OTP_CODE>`"}" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Green