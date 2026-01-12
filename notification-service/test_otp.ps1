# Test des fonctionnalités OTP avec Twilio
# Utilisez ce script pour tester facilement les fonctionnalités OTP

$baseUrl = "http://localhost:8089/api/notifications"

Write-Host "=== Test des fonctionnalités OTP avec Twilio ===" -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# Test 1: Envoi d'OTP par SMS
Write-Host "1. Test envoi d'OTP par SMS..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/send/otp/1" -Method POST
    Write-Host "✓ OTP envoyé par SMS avec succès" -ForegroundColor Green
    Write-Host "Réponse: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Erreur lors de l'envoi de l'OTP: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 3

# Test 2: Renvoi d'OTP par SMS
Write-Host "2. Test renvoi d'OTP par SMS..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/resend/otp/1" -Method POST
    Write-Host "✓ OTP renvoyé par SMS avec succès" -ForegroundColor Green
    Write-Host "Réponse: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Erreur lors du renvoi de l'OTP: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 3

# Test 3: Vérification d'OTP (remplacer 123456 par le code réel reçu)
Write-Host "3. Test vérification d'OTP..." -ForegroundColor Green
$otpCode = Read-Host "Entrez le code OTP reçu par SMS"
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/verify/otp?userId=1&code=$otpCode" -Method POST
    Write-Host "✓ OTP vérifié avec succès" -ForegroundColor Green
    Write-Host "Réponse: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Erreur lors de la vérification de l'OTP: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "Code OTP invalide ou expiré" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== Tests OTP terminés ===" -ForegroundColor Cyan
Write-Host "Vérifiez votre téléphone pour les SMS OTP !" -ForegroundColor Yellow
Write-Host ""
Write-Host "Note: Assurez-vous que:" -ForegroundColor Yellow
Write-Host "- Le service notification-service est démarré (port 8089)" -ForegroundColor Yellow
Write-Host "- Les variables d'environnement Twilio sont configurées" -ForegroundColor Yellow
Write-Host "- L'utilisateur avec ID 1 a un numéro de téléphone valide" -ForegroundColor Yellow