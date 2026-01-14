# Configuration Twilio pour les tests locaux
# Remplacez ces valeurs par vos vraies credentials Twilio

# IMPORTANT: Remplacez ces valeurs par vos vraies credentials Twilio
$env:TWILIO_ACCOUNT_SID = "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
$env:TWILIO_AUTH_TOKEN = "your_auth_token_here"
$env:TWILIO_FROM_NUMBER = "+1234567890"

Write-Host "=== CONFIGURATION TWILIO ===" -ForegroundColor Cyan
Write-Host "Variables d'environnement définies:" -ForegroundColor Yellow
Write-Host "TWILIO_ACCOUNT_SID: $env:TWILIO_ACCOUNT_SID" -ForegroundColor White
Write-Host "TWILIO_AUTH_TOKEN: $($env:TWILIO_AUTH_TOKEN.Substring(0,10))..." -ForegroundColor White
Write-Host "TWILIO_FROM_NUMBER: $env:TWILIO_FROM_NUMBER" -ForegroundColor White
Write-Host "" -ForegroundColor White

Write-Host "=== INSTRUCTIONS ===" -ForegroundColor Green
Write-Host "1. Obtenez vos credentials sur https://console.twilio.com/" -ForegroundColor White
Write-Host "2. Achetez un numéro de téléphone Twilio" -ForegroundColor White
Write-Host "3. Remplacez les valeurs ci-dessus par vos vraies credentials" -ForegroundColor White
Write-Host "4. Exécutez ce script avant de démarrer le service" -ForegroundColor White
Write-Host "" -ForegroundColor White

# Test de l'envoi d'un SMS
Write-Host "=== TEST D'ENVOI SMS ===" -ForegroundColor Green
$phoneNumber = Read-Host "Entrez votre numéro de téléphone pour le test (+237XXXXXXXXX)"
$testMessage = "Test Zaphira - Configuration Twilio OK"

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/notifications/test/send-sms?to=$phoneNumber&message=$testMessage" -Method POST
    Write-Host "✓ SMS de test envoyé avec succès !" -ForegroundColor Green
    Write-Host "Vérifiez votre téléphone pour le message: '$testMessage'" -ForegroundColor Yellow
} catch {
    Write-Host "✗ Erreur lors de l'envoi du SMS de test: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "Endpoint de test non trouvé. Le service notification n'est peut-être pas démarré." -ForegroundColor Yellow
    }
}