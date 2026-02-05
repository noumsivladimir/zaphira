# Test Email Verification with Link

Write-Host "========================================" -ForegroundColor Green
Write-Host "Testing Email Verification with Link" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Test user registration
Write-Host "`nTesting user registration..." -ForegroundColor Yellow
$registrationResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/users/register" -Method POST -ContentType "application/json" -Body '{
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

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Email sent with verification link!" -ForegroundColor White
Write-Host "Check your email for the HTML email with verification button" -ForegroundColor White
Write-Host "" -ForegroundColor White
Write-Host "The email contains:" -ForegroundColor White
Write-Host "- A clickable 'Verify Email' button" -ForegroundColor White
Write-Host "- The verification code as backup" -ForegroundColor White
Write-Host "- Professional HTML design" -ForegroundColor White
Write-Host "" -ForegroundColor White
Write-Host "Clicking the button will:" -ForegroundColor White
Write-Host "- Automatically verify the email" -ForegroundColor White
Write-Host "- Activate the account" -ForegroundColor White
Write-Host "- Create the wallet" -ForegroundColor White
Write-Host "- Show a success page" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Green

# Optional: Test the verification link directly (if you have the code)
# Uncomment and replace YOUR_CODE with the actual code from email
# Write-Host "`nTesting direct verification link..." -ForegroundColor Yellow
# $verifyResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/users/verify-email-link?email=test@example.com&code=YOUR_CODE"
# Write-Host "Verification Response:" -ForegroundColor Cyan
# $verifyResponse.Content