# Script PowerShell pour tester les fonctionnalités du microservice Transaction
# Port: 8083
# Base URL: http://localhost:8083

$baseUrl = "http://localhost:8083"
$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer YOUR_JWT_TOKEN_HERE"  # Remplacer par un token JWT valide
}

Write-Host "=== TESTS DU MICROSERVICE TRANSACTION ===" -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# =============================================================================
# 1. TESTS DES TRANSACTIONS (TransactionController)
# =============================================================================

Write-Host "=== 1. TESTS DES TRANSACTIONS ===" -ForegroundColor Green

# 1.1 Créer une transaction
Write-Host "1.1 Création d'une transaction..." -ForegroundColor Yellow
$transactionRequest = @{
    senderWalletNumber = "WALLET001"
    receiverWalletNumber = "WALLET002"
    amount = 100.00
    currency = "XAF"
    type = "TRANSFER"
    channel = "MOBILE_APP"
    description = "Test transaction"
    processInstantly = $true
    requestedBy = "user123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/transactions" -Method POST -Headers $headers -Body $transactionRequest
    Write-Host "Transaction créée avec succès. ID:" $response.id -ForegroundColor Green
    $transactionId = $response.id
} catch {
    Write-Host "Erreur lors de la création de la transaction:" $_.Exception.Message -ForegroundColor Red
}

# 1.2 Lister toutes les transactions
Write-Host "1.2 Récupération de toutes les transactions..." -ForegroundColor Yellow
try {
    $transactions = Invoke-RestMethod -Uri "$baseUrl/api/transactions" -Method GET -Headers $headers
    Write-Host "Nombre de transactions trouvées:" $transactions.Count -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de la récupération des transactions:" $_.Exception.Message -ForegroundColor Red
}

# 1.3 Obtenir une transaction spécifique
if ($transactionId) {
    Write-Host "1.3 Récupération de la transaction ID: $transactionId..." -ForegroundColor Yellow
    try {
        $transaction = Invoke-RestMethod -Uri "$baseUrl/api/transactions/$transactionId" -Method GET -Headers $headers
        Write-Host "Transaction trouvée - Statut:" $transaction.status -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la récupération de la transaction:" $_.Exception.Message -ForegroundColor Red
    }
}

# 1.4 Historique d'une transaction
if ($transactionId) {
    Write-Host "1.4 Historique de la transaction ID: $transactionId..." -ForegroundColor Yellow
    try {
        $history = Invoke-RestMethod -Uri "$baseUrl/api/transactions/$transactionId/history" -Method GET -Headers $headers
        Write-Host "Nombre d'événements dans l'historique:" $history.Count -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la récupération de l'historique:" $_.Exception.Message -ForegroundColor Red
    }
}

# 1.5 Informations d'autorisation
if ($transactionId) {
    Write-Host "1.5 Informations d'autorisation pour la transaction ID: $transactionId..." -ForegroundColor Yellow
    try {
        $authInfo = Invoke-RestMethod -Uri "$baseUrl/api/transactions/$transactionId/authorization" -Method GET -Headers $headers
        Write-Host "Informations d'autorisation récupérées" -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la récupération des informations d'autorisation:" $_.Exception.Message -ForegroundColor Red
    }
}

# 1.6 Autoriser une transaction
if ($transactionId) {
    Write-Host "1.6 Autorisation de la transaction ID: $transactionId..." -ForegroundColor Yellow
    $authRequest = @{
        pin = "123456"
        otpCode = "123456"
        authorizedBy = "admin"
    } | ConvertTo-Json

    try {
        $authorized = Invoke-RestMethod -Uri "$baseUrl/api/transactions/$transactionId/authorize" -Method POST -Headers $headers -Body $authRequest
        Write-Host "Transaction autorisée avec succès" -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de l'autorisation:" $_.Exception.Message -ForegroundColor Red
    }
}

# 1.7 Mettre à jour le statut d'une transaction
if ($transactionId) {
    Write-Host "1.7 Mise à jour du statut de la transaction ID: $transactionId..." -ForegroundColor Yellow
    $statusRequest = @{
        status = "COMPLETED"
        updatedBy = "system"
        reason = "Test completion"
    } | ConvertTo-Json

    try {
        $updated = Invoke-RestMethod -Uri "$baseUrl/api/transactions/$transactionId/status" -Method PUT -Headers $headers -Body $statusRequest
        Write-Host "Statut mis à jour avec succès" -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la mise à jour du statut:" $_.Exception.Message -ForegroundColor Red
    }
}

# 1.8 Annuler une transaction
Write-Host "1.8 Annulation d'une transaction..." -ForegroundColor Yellow
$cancelRequest = @{
    cancelledBy = "user123"
    reason = "Test cancellation"
} | ConvertTo-Json

try {
    $cancelled = Invoke-RestMethod -Uri "$baseUrl/api/transactions/123/cancel" -Method PUT -Headers $headers -Body $cancelRequest
    Write-Host "Transaction annulée avec succès" -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de l'annulation:" $_.Exception.Message -ForegroundColor Red
}

# =============================================================================
# 2. TESTS DES TRANSACTIONS PROGRAMMÉES (ScheduledTransactionController)
# =============================================================================

Write-Host "`n=== 2. TESTS DES TRANSACTIONS PROGRAMMÉES ===" -ForegroundColor Green

# 2.1 Créer une transaction programmée
Write-Host "2.1 Création d'une transaction programmée..." -ForegroundColor Yellow
$scheduledRequest = @{
    senderWalletNumber = "WALLET001"
    receiverWalletNumber = "WALLET002"
    amount = 50.00
    currency = "XAF"
    type = "TRANSFER"
    channel = "WEB_APP"
    description = "Scheduled payment"
    requestedBy = "user123"
    scheduledFor = "2025-12-25T10:00:00"
} | ConvertTo-Json

try {
    $scheduled = Invoke-RestMethod -Uri "$baseUrl/api/transactions/scheduled" -Method POST -Headers $headers -Body $scheduledRequest
    Write-Host "Transaction programmée créée avec succès. ID:" $scheduled.id -ForegroundColor Green
    $scheduledId = $scheduled.id
} catch {
    Write-Host "Erreur lors de la création de la transaction programmée:" $_.Exception.Message -ForegroundColor Red
}

# 2.2 Lister les transactions programmées
Write-Host "2.2 Récupération des transactions programmées..." -ForegroundColor Yellow
try {
    $scheduledTransactions = Invoke-RestMethod -Uri "$baseUrl/api/transactions/scheduled" -Method GET -Headers $headers
    Write-Host "Nombre de transactions programmées:" $scheduledTransactions.Count -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de la récupération des transactions programmées:" $_.Exception.Message -ForegroundColor Red
}

# 2.3 Obtenir une transaction programmée spécifique
if ($scheduledId) {
    Write-Host "2.3 Récupération de la transaction programmée ID: $scheduledId..." -ForegroundColor Yellow
    try {
        $scheduledTx = Invoke-RestMethod -Uri "$baseUrl/api/transactions/scheduled/$scheduledId" -Method GET -Headers $headers
        Write-Host "Transaction programmée trouvée - Programmée pour:" $scheduledTx.scheduledFor -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la récupération de la transaction programmée:" $_.Exception.Message -ForegroundColor Red
    }
}

# 2.4 Annuler une transaction programmée
if ($scheduledId) {
    Write-Host "2.4 Annulation de la transaction programmée ID: $scheduledId..." -ForegroundColor Yellow
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/transactions/scheduled/$scheduledId?cancelledBy=user123&reason=Test" -Method DELETE -Headers $headers
        Write-Host "Transaction programmée annulée avec succès" -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de l'annulation de la transaction programmée:" $_.Exception.Message -ForegroundColor Red
    }
}

# =============================================================================
# 3. TESTS DES TAUX DE CHANGE (ExchangeRateController)
# =============================================================================

Write-Host "`n=== 3. TESTS DES TAUX DE CHANGE ===" -ForegroundColor Green

# 3.1 Obtenir un taux de change
Write-Host "3.1 Récupération du taux de change USD vers EUR..." -ForegroundColor Yellow
try {
    $exchangeRate = Invoke-RestMethod -Uri "$baseUrl/api/exchange-rates?from=USD&to=EUR" -Method GET -Headers $headers
    Write-Host "Taux de change USD->EUR:" $exchangeRate.rate " (Fournisseur:" $exchangeRate.provider ")" -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de la récupération du taux de change:" $_.Exception.Message -ForegroundColor Red
}

# 3.2 Rafraîchir un taux de change
Write-Host "3.2 Rafraîchissement du taux de change XAF vers USD..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/exchange-rates/refresh?from=XAF&to=USD" -Method POST -Headers $headers
    Write-Host "Taux de change rafraîchi avec succès" -ForegroundColor Green
} catch {
    Write-Host "Erreur lors du rafraîchissement du taux de change:" $_.Exception.Message -ForegroundColor Red
}

# =============================================================================
# 4. TESTS DES RAPPORTS (ReportsController)
# =============================================================================

Write-Host "`n=== 4. TESTS DES RAPPORTS ===" -ForegroundColor Green

# 4.1 Rapport quotidien pour une date spécifique
Write-Host "4.1 Rapport quotidien pour aujourd'hui..." -ForegroundColor Yellow
$date = Get-Date -Format "yyyy-MM-dd"
try {
    $dailyReport = Invoke-RestMethod -Uri "$baseUrl/api/reports/daily/$date" -Method GET -Headers $headers
    Write-Host "Rapport quotidien récupéré - Total des transactions:" $dailyReport.totalTransactions -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de la récupération du rapport quotidien:" $_.Exception.Message -ForegroundColor Red
}

# 4.2 Liste des rapports quotidiens
Write-Host "4.2 Récupération de la liste des rapports quotidiens..." -ForegroundColor Yellow
try {
    $dailyReports = Invoke-RestMethod -Uri "$baseUrl/api/reports/daily" -Method GET -Headers $headers
    Write-Host "Nombre de rapports quotidiens:" $dailyReports.Count -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de la récupération des rapports quotidiens:" $_.Exception.Message -ForegroundColor Red
}

# 4.3 Dernier rapport quotidien
Write-Host "4.3 Récupération du dernier rapport quotidien..." -ForegroundColor Yellow
try {
    $latestReport = Invoke-RestMethod -Uri "$baseUrl/api/reports/daily/latest" -Method GET -Headers $headers
    Write-Host "Dernier rapport récupéré pour la date:" $latestReport.reportDate -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de la récupération du dernier rapport:" $_.Exception.Message -ForegroundColor Red
}

# 4.4 Analytics utilisateur
Write-Host "4.4 Analytics pour l'utilisateur 123..." -ForegroundColor Yellow
try {
    $userAnalytics = Invoke-RestMethod -Uri "$baseUrl/api/reports/user/123" -Method GET -Headers $headers
    Write-Host "Analytics utilisateur récupérés - Nombre de transactions:" $userAnalytics.totalTransactions -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de la récupération des analytics utilisateur:" $_.Exception.Message -ForegroundColor Red
}

# 4.5 Historique utilisateur
Write-Host "4.5 Historique des transactions pour l'utilisateur 123..." -ForegroundColor Yellow
try {
    $userHistory = Invoke-RestMethod -Uri "$baseUrl/api/reports/user/123/history" -Method GET -Headers $headers
    Write-Host "Historique utilisateur récupéré - Nombre d'événements:" $userHistory.Count -ForegroundColor Green
} catch {
    Write-Host "Erreur lors de la récupération de l'historique utilisateur:" $_.Exception.Message -ForegroundColor Red
}

# =============================================================================
# 5. TESTS DES LITIGES (DisputeController)
# =============================================================================

Write-Host "`n=== 5. TESTS DES LITIGES ===" -ForegroundColor Green

# 5.1 Créer un litige
Write-Host "5.1 Création d'un litige..." -ForegroundColor Yellow
$disputeRequest = @{
    transactionId = "123"
    reason = "UNAUTHORIZED_TRANSACTION"
    description = "Transaction non autorisée"
    evidence = @("receipt.pdf", "statement.pdf")
    contactEmail = "user@example.com"
    contactPhone = "+237612345678"
    disputedAmount = 100.00
    disputedBy = "user123"
} | ConvertTo-Json

try {
    $dispute = Invoke-RestMethod -Uri "$baseUrl/api/disputes" -Method POST -Headers $headers -Body $disputeRequest
    Write-Host "Litige créé avec succès. ID:" $dispute.id -ForegroundColor Green
    $disputeId = $dispute.id
} catch {
    Write-Host "Erreur lors de la création du litige:" $_.Exception.Message -ForegroundColor Red
}

# 5.2 Soumettre une preuve pour un litige
if ($disputeId) {
    Write-Host "5.2 Soumission d'une preuve pour le litige ID: $disputeId..." -ForegroundColor Yellow
    $evidenceRequest = @{
        evidenceType = "RECEIPT"
        description = "Preuve de paiement"
        submittedBy = "user123"
    } | ConvertTo-Json

    try {
        $evidence = Invoke-RestMethod -Uri "$baseUrl/api/disputes/$disputeId/evidence" -Method POST -Headers $headers -Body $evidenceRequest
        Write-Host "Preuve soumise avec succès" -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la soumission de la preuve:" $_.Exception.Message -ForegroundColor Red
    }
}

# 5.3 Répondre à un litige
if ($disputeId) {
    Write-Host "5.3 Réponse à un litige ID: $disputeId..." -ForegroundColor Yellow
    $responseRequest = @{
        response = "Transaction légitime"
        evidence = @("transaction_log.pdf")
        respondedBy = "merchant456"
    } | ConvertTo-Json

    try {
        $disputeResponse = Invoke-RestMethod -Uri "$baseUrl/api/disputes/$disputeId/respond" -Method POST -Headers $headers -Body $responseRequest
        Write-Host "Réponse au litige soumise avec succès" -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la réponse au litige:" $_.Exception.Message -ForegroundColor Red
    }
}

# 5.4 Obtenir les détails d'un litige
if ($disputeId) {
    Write-Host "5.4 Récupération des détails du litige ID: $disputeId..." -ForegroundColor Yellow
    try {
        $disputeDetails = Invoke-RestMethod -Uri "$baseUrl/api/disputes/$disputeId" -Method GET -Headers $headers
        Write-Host "Détails du litige récupérés - Statut:" $disputeDetails.status -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la récupération des détails du litige:" $_.Exception.Message -ForegroundColor Red
    }
}

# 5.5 Résoudre un litige
if ($disputeId) {
    Write-Host "5.5 Résolution du litige ID: $disputeId..." -ForegroundColor Yellow
    $resolveRequest = @{
        resolution = "REFUND"
        refundAmount = 100.00
        reason = "Litige résolu en faveur du client"
        resolvedBy = "admin"
    } | ConvertTo-Json

    try {
        $resolved = Invoke-RestMethod -Uri "$baseUrl/api/disputes/$disputeId/resolve" -Method PUT -Headers $headers -Body $resolveRequest
        Write-Host "Litige résolu avec succès" -ForegroundColor Green
    } catch {
        Write-Host "Erreur lors de la résolution du litige:" $_.Exception.Message -ForegroundColor Red
    }
}

# =============================================================================
# RÉSUMÉ DES TESTS
# =============================================================================

Write-Host "`n=== RÉSUMÉ DES TESTS ===" -ForegroundColor Cyan
Write-Host "Tests terminés pour le microservice Transaction (Port: 8083)" -ForegroundColor White
Write-Host ""
Write-Host "Endpoints testés:" -ForegroundColor Yellow
Write-Host "✓ Transactions: Création, lecture, mise à jour, annulation" -ForegroundColor Green
Write-Host "✓ Transactions programmées: CRUD complet" -ForegroundColor Green
Write-Host "✓ Taux de change: Consultation et rafraîchissement" -ForegroundColor Green
Write-Host "✓ Rapports: Quotidiens et analytics utilisateurs" -ForegroundColor Green
Write-Host "✓ Litiges: Création, gestion des preuves, résolution" -ForegroundColor Green
Write-Host ""
Write-Host "Note: Remplacez 'YOUR_JWT_TOKEN_HERE' par un token JWT valide pour les tests en conditions réelles." -ForegroundColor Magenta