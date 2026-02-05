# Test complet de l'envoi d'OTP par SMS avec Twilio
# Ce script teste tout le processus OTP de bout en bout

Write-Host "=== TEST COMPLET OTP AVEC TWILIO ===" -ForegroundColor Cyan
Write-Host "Vérification de la configuration et test des SMS" -ForegroundColor Yellow
Write-Host ""

# Étape 1: Vérifier les variables d'environnement
Write-Host "1. Vérification des variables d'environnement Twilio..." -ForegroundColor Green
$twilioVars = @(
    @{Name="TWILIO_ACCOUNT_SID"; Value=$env:TWILIO_ACCOUNT_SID},
    @{Name="TWILIO_AUTH_TOKEN"; Value=$env:TWILIO_AUTH_TOKEN},
    @{Name="TWILIO_FROM_NUMBER"; Value=$env:TWILIO_FROM_NUMBER}
)

$allConfigured = $true
foreach ($var in $twilioVars) {
    if ([string]::IsNullOrEmpty($var.Value)) {
        Write-Host "❌ $($var.Name): MANQUANT" -ForegroundColor Red
        $allConfigured = $false
    } else {
        if ($var.Name -eq "TWILIO_AUTH_TOKEN") {
            Write-Host "✅ $($var.Name): $($var.Value.Substring(0,10))..." -ForegroundColor Green
        } else {
            Write-Host "✅ $($var.Name): $($var.Value)" -ForegroundColor Green
        }
    }
}

if (-not $allConfigured) {
    Write-Host ""
    Write-Host "❌ CONFIGURATION INCOMPLETE !" -ForegroundColor Red
    Write-Host "Exécutez d'abord le script configure_twilio.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "2. Vérification que le service notification fonctionne..." -ForegroundColor Green
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method GET
    Write-Host "✅ Service notification: UP" -ForegroundColor Green
} catch {
    Write-Host "❌ Service notification: DOWN ou non accessible" -ForegroundColor Red
    Write-Host "Démarrez le service avec: cd notification-service && mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "3. Test d'envoi SMS direct..." -ForegroundColor Green
$phoneNumber = Read-Host "Entrez votre numéro de téléphone (+237XXXXXXXXX)"
$testMessage = "Test Zaphira OTP - $(Get-Date -Format 'HH:mm:ss')"

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/test/send-sms?to=$phoneNumber&message=$testMessage" -Method POST
    Write-Host "✅ SMS de test envoyé !" -ForegroundColor Green
    Write-Host "Vérifiez votre téléphone pour: '$testMessage'" -ForegroundColor Yellow
} catch {
    Write-Host "❌ Erreur envoi SMS de test: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "4. Test complet OTP..." -ForegroundColor Green
$userId = Read-Host "Entrez l'ID utilisateur (ex: 218)"

try {
    # Envoi OTP
    Write-Host "Génération de l'OTP..." -ForegroundColor Yellow
    $otpResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/send/otp/$userId" -Method POST
    Write-Host "✅ OTP généré !" -ForegroundColor Green
    Write-Host "Réponse OTP: $($otpResponse | ConvertTo-Json)" -ForegroundColor Cyan

    # Extraction du code OTP depuis la réponse
    $otpCode = $otpResponse.otp
    Write-Host "Code OTP extrait: $otpCode" -ForegroundColor Green

    # Vérification OTP
    Write-Host "Vérification de l'OTP..." -ForegroundColor Yellow
    $verifyResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/verify/otp?userId=$userId&code=$otpCode" -Method POST
    Write-Host "✅ OTP vérifié avec succès !" -ForegroundColor Green
    Write-Host "Réponse: $($verifyResponse | ConvertTo-Json)" -ForegroundColor Gray

} catch {
    Write-Host "❌ Erreur lors du test OTP: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "Code OTP invalide ou expiré, ou compte pas en PENDING_VERIFICATION" -ForegroundColor Yellow
    } elseif ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "Utilisateur non trouvé ou numéro de téléphone manquant" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== TEST TERMINÉ ===" -ForegroundColor Cyan
Write-Host "L'OTP est maintenant généré et retourné directement dans la réponse JSON." -ForegroundColor Green
Write-Host "Plus besoin de Twilio pour les tests de développement." -ForegroundColor Green