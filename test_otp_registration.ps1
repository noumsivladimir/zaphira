# Test du processus d'inscription avec vérification OTP en deux étapes
# Ce script démontre le nouveau workflow :
# 1. Inscription utilisateur (statut PENDING_VERIFICATION)
# 2. Demande d'OTP (généré par notification-service)
# 3. Vérification OTP (via notification-service)
# 4. Activation du compte

Write-Host "=== TEST PROCESSUS INSCRIPTION OTP ===" -ForegroundColor Cyan

# Configuration
$userServiceUrl = "http://localhost:8080"
$notificationServiceUrl = "http://localhost:8080"

# Étape 1: Inscription utilisateur
Write-Host ""
Write-Host "1. Inscription utilisateur..." -ForegroundColor Green

$userData = @{
    firstName = "Test"
    lastName = "User"
    phoneNumber = "+237" + (Get-Random -Minimum 600000000 -Maximum 699999999).ToString()
    email = "test$(Get-Random -Minimum 1000 -Maximum 9999)@example.com"
    pin = "1234"
    dateOfBirth = "1990-01-01"
    country = "Cameroon"
    city = "Douala"
    region = "Littoral"
    neighborhood = "Bonapriso"
    preferredLanguage = "fr"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$userServiceUrl/api/users/register" -Method POST -ContentType "application/json" -Body $userData
    Write-Host "✅ Inscription réussie !" -ForegroundColor Green
    Write-Host "Réponse: $($registerResponse | ConvertTo-Json)" -ForegroundColor Gray

    $userId = $registerResponse.data.userId
    Write-Host "User ID: $userId" -ForegroundColor Yellow

} catch {
    Write-Host "❌ Erreur lors de l'inscription: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Étape 2: Demande d'OTP
Write-Host ""
Write-Host "2. Demande d'OTP..." -ForegroundColor Green

try {
    $otpResponse = Invoke-RestMethod -Uri "$userServiceUrl/api/users/send-otp/$userId" -Method POST
    Write-Host "✅ OTP généré !" -ForegroundColor Green
    Write-Host "Réponse OTP: $($otpResponse | ConvertTo-Json)" -ForegroundColor Cyan

    $otpCode = $otpResponse.data.otp
    Write-Host "Code OTP extrait: $otpCode" -ForegroundColor Yellow

} catch {
    Write-Host "❌ Erreur lors de la demande d'OTP: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Étape 3: Vérification OTP et activation du compte
Write-Host ""
Write-Host "3. Vérification OTP et activation..." -ForegroundColor Green

$verifyData = @{
    userId = $userId
    otpCode = $otpCode
} | ConvertTo-Json

try {
    $verifyResponse = Invoke-RestMethod -Uri "$userServiceUrl/api/users/verify-otp" -Method POST -ContentType "application/json" -Body $verifyData
    Write-Host "✅ OTP vérifié et compte activé !" -ForegroundColor Green
    Write-Host "Réponse: $($verifyResponse | ConvertTo-Json)" -ForegroundColor Gray

    if ($verifyResponse.data.accountStatus -eq "ACTIVE") {
        Write-Host "✅ Compte activé avec succès !" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Statut du compte: $($verifyResponse.data.accountStatus)" -ForegroundColor Yellow
    }

} catch {
    Write-Host "❌ Erreur lors de la vérification OTP: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== PROCESSUS TERMINÉ AVEC SUCCÈS ===" -ForegroundColor Cyan
Write-Host "Le workflow d'inscription OTP fonctionne correctement !" -ForegroundColor Green