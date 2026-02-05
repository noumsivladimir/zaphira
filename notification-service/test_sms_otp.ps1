# Test de l'envoi d'OTP par SMS avec payload
# Utilisez ce script pour tester l'envoi d'OTP via le nouvel endpoint

$baseUrl = "http://localhost:8080/api/notifications"

Write-Host "=== Test d'envoi d'OTP par SMS avec payload ===" -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# Test 1: Envoi d'OTP par SMS avec payload
Write-Host "1. Test envoi d'OTP par SMS avec payload..." -ForegroundColor Green

# Données de test
$payload = @{
    phoneNumber = "+237656958696"
    otp = "123456"
    purpose = "REGISTRATION"
    expiresInMinutes = 5
} | ConvertTo-Json

Write-Host "Payload envoyé:" -ForegroundColor Gray
Write-Host $payload -ForegroundColor Gray
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/send/otp" -Method POST -Body $payload -ContentType "application/json"
    Write-Host "✓ OTP envoyé par SMS avec succès" -ForegroundColor Green
    Write-Host "Réponse: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Erreur lors de l'envoi de l'OTP: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        try {
            $errorResponse = $_.Exception.Response.GetResponseStream() | ConvertFrom-Json
            Write-Host "Détails de l'erreur: $($errorResponse | ConvertTo-Json)" -ForegroundColor Red
        } catch {
            Write-Host "Impossible de lire les détails de l'erreur" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "=== Test terminé ===" -ForegroundColor Cyan
Write-Host "Vérifiez votre téléphone pour le SMS OTP !" -ForegroundColor Yellow