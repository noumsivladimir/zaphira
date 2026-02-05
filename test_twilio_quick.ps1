# Test rapide de Twilio - Configuration en clair dans application.yml

Write-Host "=== TEST RAPIDE TWILIO ===" -ForegroundColor Cyan
Write-Host "Configuration chargée depuis application.yml" -ForegroundColor Yellow
Write-Host ""

# Test 1: Vérifier que le service fonctionne
Write-Host "1. Vérification du service notification..." -ForegroundColor Green
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method GET
    Write-Host "✅ Service notification: UP" -ForegroundColor Green
} catch {
    Write-Host "❌ Service notification: DOWN" -ForegroundColor Red
    Write-Host "Démarrez le service: cd notification-service && mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}

# Test 2: Test d'envoi SMS direct
Write-Host ""
Write-Host "2. Test d'envoi SMS..." -ForegroundColor Green
$phoneNumber = Read-Host "Entrez votre numéro (+237XXXXXXXXX)"
$testMessage = "Test Zaphira - $(Get-Date -Format 'HH:mm:ss')"

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/test/send-sms?to=$phoneNumber&message=$testMessage" -Method POST
    Write-Host "✅ SMS envoyé avec succès !" -ForegroundColor Green
    Write-Host "Vérifiez votre téléphone pour: '$testMessage'" -ForegroundColor Yellow
} catch {
    Write-Host "❌ Erreur envoi SMS: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Test OTP complet
Write-Host ""
Write-Host "3. Test OTP complet..." -ForegroundColor Green
$userId = Read-Host "Entrez l'ID utilisateur (ex: 218)"

try {
    # Génération OTP
    Write-Host "Génération de l'OTP..." -ForegroundColor Yellow
    $otpResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/send/otp/$userId" -Method POST
    Write-Host "✅ OTP généré !" -ForegroundColor Green
    Write-Host "Réponse: $($otpResponse | ConvertTo-Json)" -ForegroundColor Cyan

    # Extraction du code OTP
    $otpCode = $otpResponse.otp
    Write-Host "Code OTP: $otpCode" -ForegroundColor Green

    # Vérification OTP
    $verifyResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/verify/otp?userId=$userId&code=$otpCode" -Method POST
    Write-Host "✅ OTP vérifié avec succès !" -ForegroundColor Green

} catch {
    Write-Host "❌ Erreur OTP: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "Code invalide ou compte pas en PENDING_VERIFICATION" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== TEST TERMINÉ ===" -ForegroundColor Cyan