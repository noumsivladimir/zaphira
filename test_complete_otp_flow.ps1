# Test complet du flux OTP - Inscription, Envoi et Vérification
# Ce script démontre le processus complet d'inscription avec OTP

Write-Host "=== Zaphira - Flux OTP Complet ===" -ForegroundColor Green

# Configuration des services
$baseUrl = "http://localhost:8080"
$userServiceUrl = "$baseUrl/api/users"
$notificationServiceUrl = "$baseUrl/api/notifications"

# Données de test
$testUser = @{
    firstName = "Jean"
    lastName = "Dupont"
    email = "jean.dupont@example.com"
    phoneNumber = "+237690000001"
    dateOfBirth = "1990-01-01"
    country = "Cameroon"
    preferredCurrency = "XAF"
    password = "SecurePass123!"
}

Write-Host "`n1. Inscription de l'utilisateur..." -ForegroundColor Yellow

# Étape 1: Inscription (crée l'utilisateur avec statut PENDING_VERIFICATION)
$registerBody = $testUser | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$userServiceUrl/register" -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "✓ Inscription réussie !" -ForegroundColor Green
    Write-Host "Utilisateur créé avec ID: $($registerResponse.data.userId)" -ForegroundColor Cyan
    Write-Host "Statut du compte: $($registerResponse.data.accountStatus)" -ForegroundColor Cyan

    $userId = $registerResponse.data.userId
    $phoneNumber = $registerResponse.data.phoneNumber

} catch {
    Write-Host "✗ Échec de l'inscription:" -ForegroundColor Red
    Write-Host "Erreur: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`n2. Envoi de l'OTP..." -ForegroundColor Yellow

# Étape 2: Envoi de l'OTP (notification-service retourne le code OTP)
try {
    $otpResponse = Invoke-RestMethod -Uri "$notificationServiceUrl/send/otp/$userId" -Method POST -ContentType "application/json"
    Write-Host "✓ OTP généré avec succès !" -ForegroundColor Green
    Write-Host "Statut: $($otpResponse.status)" -ForegroundColor Cyan
    Write-Host "Message: $($otpResponse.message)" -ForegroundColor Cyan

    $otpCode = $otpResponse.otp
    Write-Host "Code OTP reçu: $otpCode" -ForegroundColor Magenta

} catch {
    Write-Host "✗ Échec de l'envoi OTP:" -ForegroundColor Red
    Write-Host "Erreur: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`n3. Vérification de l'OTP..." -ForegroundColor Yellow

# Étape 3: Vérification de l'OTP (user-service active le compte)
$verifyOtpBody = @{
    phoneNumber = $phoneNumber
    otpCode = $otpCode
} | ConvertTo-Json

try {
    $verifyResponse = Invoke-RestMethod -Uri "$userServiceUrl/verify-otp" -Method POST -Body $verifyOtpBody -ContentType "application/json"
    Write-Host "✓ OTP vérifié et compte activé !" -ForegroundColor Green
    Write-Host "Message: $($verifyResponse.message)" -ForegroundColor Cyan
    Write-Host "Nouveau statut: $($verifyResponse.data.accountStatus)" -ForegroundColor Cyan
    Write-Host "Wallet ID: $($verifyResponse.data.walletId)" -ForegroundColor Cyan

} catch {
    Write-Host "✗ Échec de la vérification OTP:" -ForegroundColor Red
    Write-Host "Erreur: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorContent = $_.Exception.Response.GetResponseStream() | %{ $reader = New-Object System.IO.StreamReader($_); $reader.ReadToEnd() }
        Write-Host "Détails: $errorContent" -ForegroundColor Red
    }
    exit 1
}

Write-Host "`n=== Flux OTP terminé avec succès ! ===" -ForegroundColor Green
Write-Host "L'utilisateur peut maintenant se connecter avec son compte actif." -ForegroundColor Cyan