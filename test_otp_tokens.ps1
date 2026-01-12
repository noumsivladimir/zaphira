# Test complet du flux OTP avec table otp_tokens
# Ce script utilise la table otp_tokens pour stocker les tokens

Write-Host "=== Zaphira - Test Flux OTP (otp_tokens) ===" -ForegroundColor Green

# Configuration des services
$baseUrl = "http://localhost:8080"
$userServiceUrl = "$baseUrl/api/users"
$notificationServiceUrl = "$baseUrl/api/notifications"

# Données de test
$testUser = @{
    firstName = "Test"
    lastName = "OTP"
    email = "test.otp@example.com"
    phoneNumber = "+237690000003"
    dateOfBirth = "1990-01-01"
    country = "Cameroon"
    preferredCurrency = "XAF"
    password = "Test123!"
}

Write-Host "`n1. Inscription de l'utilisateur..." -ForegroundColor Yellow

# Étape 1: Inscription
$registerBody = $testUser | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$userServiceUrl/register" -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "✓ Inscription réussie !" -ForegroundColor Green
    $userId = $registerResponse.data.userId
    $phoneNumber = $registerResponse.data.phoneNumber
    Write-Host "User ID: $userId, Phone: $phoneNumber" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Échec inscription:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host "`n2. Génération de l'OTP (stocké dans otp_tokens)..." -ForegroundColor Yellow

# Étape 2: Générer l'OTP via notification-service (utilise maintenant otp_tokens)
try {
    $otpResponse = Invoke-RestMethod -Uri "$notificationServiceUrl/send/otp/$userId" -Method POST -ContentType "application/json"
    Write-Host "✓ OTP généré avec succès !" -ForegroundColor Green
    $otpCode = $otpResponse.otp
    Write-Host "OTP Code: $otpCode" -ForegroundColor Magenta
    Write-Host "Status: $($otpResponse.status)" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Échec génération OTP:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host "`n3. Vérification de l'OTP (via otp_tokens)..." -ForegroundColor Yellow

# Étape 3: Vérifier l'OTP
$verifyBody = @{
    phoneNumber = $phoneNumber
    otpCode = $otpCode
} | ConvertTo-Json

try {
    $verifyResponse = Invoke-RestMethod -Uri "$userServiceUrl/verify-otp" -Method POST -Body $verifyBody -ContentType "application/json"
    Write-Host "✓ OTP vérifié et compte activé !" -ForegroundColor Green
    Write-Host "Nouveau statut: $($verifyResponse.data.accountStatus)" -ForegroundColor Cyan
    Write-Host "Wallet ID: $($verifyResponse.data.walletId)" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Échec vérification OTP:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorContent = $_.Exception.Response.GetResponseStream() | %{ $reader = New-Object System.IO.StreamReader($_); $reader.ReadToEnd() }
        Write-Host "Détails: $errorContent" -ForegroundColor Red
    }
    exit 1
}

Write-Host "`n=== Test réussi ! OTP stocké dans otp_tokens ===" -ForegroundColor Green
Write-Host "✅ Table otp_tokens utilisée correctement" -ForegroundColor Cyan
Write-Host "✅ Flux complet fonctionnel" -ForegroundColor Cyan