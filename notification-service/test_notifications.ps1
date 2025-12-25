# Test des Notifications Telegram
# Utilisez ce script pour tester facilement les notifications

$chatId = "7584204126"  # Votre chat ID
$baseUrl = "http://localhost:8084/api/notifications/test"

Write-Host "=== Test des Notifications Telegram ===" -ForegroundColor Cyan
Write-Host "Chat ID: $chatId" -ForegroundColor Yellow
Write-Host ""

# Test 1: Message simple
Write-Host "1. Test message simple..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/telegram?chatId=$chatId&message=Test%20de%20notification%20Telegram%20!%20%F0%9F%93%B1" -Method POST
    Write-Host "✓ Message envoyé avec succès" -ForegroundColor Green
} catch {
    Write-Host "✗ Erreur lors de l'envoi du message: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 2: Message de bienvenue
Write-Host "2. Test message de bienvenue..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/welcome?chatId=$chatId" -Method POST
    Write-Host "✓ Message de bienvenue envoyé" -ForegroundColor Green
} catch {
    Write-Host "✗ Erreur lors de l'envoi du message de bienvenue: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 3: Notification de transaction
Write-Host "3. Test notification de transaction..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/transaction?chatId=$chatId" -Method POST
    Write-Host "✓ Notification de transaction envoyée" -ForegroundColor Green
} catch {
    Write-Host "✗ Erreur lors de l'envoi de la notification de transaction: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 4: Notifications portefeuille
Write-Host "4. Test notifications portefeuille..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/wallet?chatId=$chatId" -Method POST
    Write-Host "✓ Notifications portefeuille envoyées" -ForegroundColor Green
} catch {
    Write-Host "✗ Erreur lors de l'envoi des notifications portefeuille: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Tests terminés ===" -ForegroundColor Cyan
Write-Host "Vérifiez votre Telegram pour voir les messages !" -ForegroundColor Yellow