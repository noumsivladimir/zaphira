# Test OTP Verification Endpoint
# This script demonstrates the complete OTP registration flow

Write-Host "=== Zaphira OTP Registration Test ===" -ForegroundColor Green

# Configuration
$baseUrl = "http://localhost:8080"
$userServiceUrl = "$baseUrl/api/users"

# Test data
$phoneNumber = "+237690000001"
$otpCode = "123456"  # This would be the actual OTP received

Write-Host "Testing OTP verification endpoint..." -ForegroundColor Yellow

# Test the verify-otp endpoint
$verifyOtpBody = @{
    phoneNumber = $phoneNumber
    otpCode = $otpCode
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$userServiceUrl/verify-otp" -Method POST -Body $verifyOtpBody -ContentType "application/json"
    Write-Host "OTP verification successful!" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 3
} catch {
    Write-Host "OTP verification failed:" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorContent = $_.Exception.Response.GetResponseStream() | %{ $reader = New-Object System.IO.StreamReader($_); $reader.ReadToEnd() }
        Write-Host "Response: $errorContent" -ForegroundColor Red
    }
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Green