# Test des Notifications Email
# Utilisez ce script pour tester facilement les notifications par email

$baseUrl = "http://localhost:8080/api/notifications"

Write-Host "=== Test des Notifications Email ===" -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# Test 1: Envoi de code de vérification
Write-Host "1. Test envoi de code de vérification..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/send/verification/1" -Method POST
    Write-Host "✓ Code de vérification envoyé avec succès" -ForegroundColor Green
    Write-Host "Réponse: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Erreur lors de l'envoi du code de vérification: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 2: Simulation d'inscription utilisateur
Write-Host "2. Test simulation d'inscription utilisateur..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/test/simulate-user-registered?firstName=John&lastName=Doe&phoneNumber=237123456789&email=test@example.com" -Method POST
    Write-Host "✓ Simulation d'inscription effectuée" -ForegroundColor Green
    Write-Host "Réponse: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Erreur lors de la simulation d'inscription: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 3: Notification de transaction (nécessite une transaction existante)
Write-Host "3. Test notification de transaction..." -ForegroundColor Green
try {
    # Remplacer 1 par un ID de transaction réel
    $response = Invoke-RestMethod -Uri "$baseUrl/send/transaction/1" -Method POST
    Write-Host "✓ Notification de transaction envoyée" -ForegroundColor Green
    Write-Host "Réponse: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Erreur lors de l'envoi de la notification de transaction: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Note: Assurez-vous qu'une transaction avec l'ID 1 existe" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Tests terminés ===" -ForegroundColor Cyan
Write-Host "Vérifiez votre boîte email pour voir les notifications !" -ForegroundColor Yellow