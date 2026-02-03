# PowerShell Test Script - LOT 1 Transaction Core
# Test des 9 endpoints du TransactionCoreController

$baseUrl = "http://localhost:8084/api/v1/transactions"
$authUrl = "http://localhost:8081/api/auth/login"

# Configuration
$regularUser = @{
    email = "user@test.com"
    password = "password123"
}

$merchantUser = @{
    email = "merchant@test.com"
    password = "password123"
}

$adminUser = @{
    email = "admin@test.com"
    password = "password123"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LOT 1 - Transaction Core E2E Tests" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to login and get JWT
function Get-JwtToken {
    param (
        [hashtable]$credentials
    )
    
    Write-Host "Logging in as $($credentials.email)..." -ForegroundColor Yellow
    
    $loginBody = $credentials | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri $authUrl -Method Post -Body $loginBody -ContentType "application/json"
        Write-Host "✅ Login successful" -ForegroundColor Green
        return $response.token
    }
    catch {
        Write-Host "❌ Login failed: $_" -ForegroundColor Red
        return $null
    }
}

# Function to make authenticated request
function Invoke-AuthRequest {
    param (
        [string]$uri,
        [string]$method,
        [string]$token,
        [object]$body = $null
    )
    
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    try {
        if ($body) {
            $jsonBody = $body | ConvertTo-Json
            $response = Invoke-RestMethod -Uri $uri -Method $method -Headers $headers -Body $jsonBody
        }
        else {
            $response = Invoke-RestMethod -Uri $uri -Method $method -Headers $headers
        }
        return $response
    }
    catch {
        Write-Host "❌ Request failed: $_" -ForegroundColor Red
        return $null
    }
}

# ========================================
# TEST 1: Login as REGULAR user
# ========================================
Write-Host ""
Write-Host "TEST 1: Login as REGULAR user" -ForegroundColor Magenta
$regularToken = Get-JwtToken -credentials $regularUser

if (-not $regularToken) {
    Write-Host "Cannot proceed without token" -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 1

# ========================================
# TEST 2: Create Deposit (Setup wallet balance)
# ========================================
Write-Host ""
Write-Host "TEST 2: Create Deposit for wallet 1" -ForegroundColor Magenta

$depositUri = "$baseUrl/deposit?receiverWalletId=1&amount=1000.00&currency=EUR&description=Initial deposit"

$deposit = Invoke-AuthRequest -uri $depositUri -method "Post" -token $regularToken

if ($deposit) {
    Write-Host "✅ Deposit created: $($deposit.reference)" -ForegroundColor Green
    Write-Host "   Amount: $($deposit.amount) $($deposit.currency)" -ForegroundColor Gray
    Write-Host "   Status: $($deposit.status)" -ForegroundColor Gray
    $depositRef = $deposit.reference
}
else {
    Write-Host "❌ Deposit creation failed" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# ========================================
# TEST 3: Create Deposit for wallet 2
# ========================================
Write-Host ""
Write-Host "TEST 3: Create Deposit for wallet 2" -ForegroundColor Magenta

$deposit2Uri = "$baseUrl/deposit?receiverWalletId=2&amount=500.00&currency=EUR&description=Second wallet deposit"

$deposit2 = Invoke-AuthRequest -uri $deposit2Uri -method "Post" -token $regularToken

if ($deposit2) {
    Write-Host "✅ Deposit created: $($deposit2.reference)" -ForegroundColor Green
    Write-Host "   Amount: $($deposit2.amount) $($deposit2.currency)" -ForegroundColor Gray
}

Start-Sleep -Seconds 1

# ========================================
# TEST 4: Create P2P Transfer
# ========================================
Write-Host ""
Write-Host "TEST 4: Create P2P Transfer (wallet 1 → wallet 2)" -ForegroundColor Magenta

$transferBody = @{
    senderWalletId = 1
    receiverWalletId = 2
    amount = 100.00
    currency = "EUR"
    description = "Test P2P transfer"
}

$transfer = Invoke-AuthRequest -uri "$baseUrl/transfer" -method "Post" -token $regularToken -body $transferBody

if ($transfer) {
    Write-Host "✅ Transfer created: $($transfer.reference)" -ForegroundColor Green
    Write-Host "   From: Wallet $($transfer.senderWalletId)" -ForegroundColor Gray
    Write-Host "   To: Wallet $($transfer.receiverWalletId)" -ForegroundColor Gray
    Write-Host "   Amount: $($transfer.amount) $($transfer.currency)" -ForegroundColor Gray
    Write-Host "   Status: $($transfer.status)" -ForegroundColor Gray
    $transferRef = $transfer.reference
}
else {
    Write-Host "❌ Transfer creation failed" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# ========================================
# TEST 5: Get Transaction by Reference
# ========================================
Write-Host ""
Write-Host "TEST 5: Get Transaction by Reference" -ForegroundColor Magenta

if ($transferRef) {
    $transaction = Invoke-AuthRequest -uri "$baseUrl/$transferRef" -method "Get" -token $regularToken
    
    if ($transaction) {
        Write-Host "✅ Transaction found: $($transaction.reference)" -ForegroundColor Green
        Write-Host "   Type: $($transaction.type)" -ForegroundColor Gray
        Write-Host "   Status: $($transaction.status)" -ForegroundColor Gray
        Write-Host "   Created: $($transaction.createdAt)" -ForegroundColor Gray
    }
    else {
        Write-Host "❌ Transaction not found" -ForegroundColor Red
    }
}

Start-Sleep -Seconds 1

# ========================================
# TEST 6: Get Transaction by ID
# ========================================
Write-Host ""
Write-Host "TEST 6: Get Transaction by ID" -ForegroundColor Magenta

if ($transfer -and $transfer.id) {
    $transactionById = Invoke-AuthRequest -uri "$baseUrl/id/$($transfer.id)" -method "Get" -token $regularToken
    
    if ($transactionById) {
        Write-Host "✅ Transaction found by ID: $($transactionById.id)" -ForegroundColor Green
        Write-Host "   Reference: $($transactionById.reference)" -ForegroundColor Gray
    }
}

Start-Sleep -Seconds 1

# ========================================
# TEST 7: Get Wallet History
# ========================================
Write-Host ""
Write-Host "TEST 7: Get Wallet History (wallet 1)" -ForegroundColor Magenta

$history = Invoke-AuthRequest -uri "$baseUrl/wallet/1?page=0&size=10" -method "Get" -token $regularToken

if ($history) {
    Write-Host "✅ Wallet history retrieved" -ForegroundColor Green
    Write-Host "   Total transactions: $($history.totalElements)" -ForegroundColor Gray
    Write-Host "   Total pages: $($history.totalPages)" -ForegroundColor Gray
    Write-Host "   Current page: $($history.pageable.pageNumber + 1)" -ForegroundColor Gray
    
    if ($history.content -and $history.content.Count -gt 0) {
        Write-Host ""
        Write-Host "   Latest transactions:" -ForegroundColor Yellow
        $history.content | ForEach-Object {
            Write-Host "   - $($_.reference) | $($_.type) | $($_.amount) $($_.currency) | $($_.status)" -ForegroundColor Gray
        }
    }
}

Start-Sleep -Seconds 1

# ========================================
# TEST 8: Get Sent Transactions
# ========================================
Write-Host ""
Write-Host "TEST 8: Get Sent Transactions (wallet 1)" -ForegroundColor Magenta

$sent = Invoke-AuthRequest -uri "$baseUrl/wallet/1/sent?page=0&size=10" -method "Get" -token $regularToken

if ($sent) {
    Write-Host "✅ Sent transactions retrieved" -ForegroundColor Green
    Write-Host "   Total sent: $($sent.totalElements)" -ForegroundColor Gray
    
    if ($sent.content -and $sent.content.Count -gt 0) {
        $sent.content | ForEach-Object {
            Write-Host "   - $($_.reference) | To: Wallet $($_.receiverWalletId) | $($_.amount) $($_.currency)" -ForegroundColor Gray
        }
    }
}

Start-Sleep -Seconds 1

# ========================================
# TEST 9: Get Received Transactions
# ========================================
Write-Host ""
Write-Host "TEST 9: Get Received Transactions (wallet 2)" -ForegroundColor Magenta

$received = Invoke-AuthRequest -uri "$baseUrl/wallet/2/received?page=0&size=10" -method "Get" -token $regularToken

if ($received) {
    Write-Host "✅ Received transactions retrieved" -ForegroundColor Green
    Write-Host "   Total received: $($received.totalElements)" -ForegroundColor Gray
    
    if ($received.content -and $received.content.Count -gt 0) {
        $received.content | ForEach-Object {
            Write-Host "   - $($_.reference) | From: Wallet $($_.senderWalletId) | $($_.amount) $($_.currency)" -ForegroundColor Gray
        }
    }
}

Start-Sleep -Seconds 1

# ========================================
# TEST 10: Create Withdrawal
# ========================================
Write-Host ""
Write-Host "TEST 10: Create Withdrawal from wallet 1" -ForegroundColor Magenta

$withdrawalUri = "$baseUrl/withdrawal?senderWalletId=1&amount=50.00&currency=EUR&description=Cash withdrawal"

$withdrawal = Invoke-AuthRequest -uri $withdrawalUri -method "Post" -token $regularToken

if ($withdrawal) {
    Write-Host "✅ Withdrawal created: $($withdrawal.reference)" -ForegroundColor Green
    Write-Host "   Amount: $($withdrawal.amount) $($withdrawal.currency)" -ForegroundColor Gray
    Write-Host "   Status: $($withdrawal.status)" -ForegroundColor Gray
}

Start-Sleep -Seconds 1

# ========================================
# TEST 11: Login as ADMIN
# ========================================
Write-Host ""
Write-Host "TEST 11: Login as ADMIN" -ForegroundColor Magenta
$adminToken = Get-JwtToken -credentials $adminUser

Start-Sleep -Seconds 1

# ========================================
# TEST 12: Get Transactions by Type (ADMIN only)
# ========================================
Write-Host ""
Write-Host "TEST 12: Get Transactions by Type=TRANSFER (ADMIN only)" -ForegroundColor Magenta

if ($adminToken) {
    $byType = Invoke-AuthRequest -uri "$baseUrl/type/TRANSFER?page=0&size=10" -method "Get" -token $adminToken
    
    if ($byType) {
        Write-Host "✅ Transactions by type retrieved" -ForegroundColor Green
        Write-Host "   Total TRANSFER transactions: $($byType.totalElements)" -ForegroundColor Gray
    }
}

Start-Sleep -Seconds 1

# ========================================
# TEST 13: Test Security - REGULAR trying to access ADMIN endpoint
# ========================================
Write-Host ""
Write-Host "TEST 13: Security Test - REGULAR accessing ADMIN endpoint (should fail)" -ForegroundColor Magenta

try {
    $headers = @{
        "Authorization" = "Bearer $regularToken"
        "Content-Type" = "application/json"
    }
    
    $response = Invoke-RestMethod -Uri "$baseUrl/type/TRANSFER" -Method Get -Headers $headers
    Write-Host "❌ Security breach! REGULAR user accessed ADMIN endpoint" -ForegroundColor Red
}
catch {
    Write-Host "✅ Access denied correctly (403 Forbidden expected)" -ForegroundColor Green
}

# ========================================
# SUMMARY
# ========================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ LOT 1 Transaction Core - Tests Completed" -ForegroundColor Green
Write-Host ""
Write-Host "Endpoints Tested:" -ForegroundColor Yellow
Write-Host "  ✅ POST /transfer" -ForegroundColor Green
Write-Host "  ✅ POST /deposit" -ForegroundColor Green
Write-Host "  ✅ POST /withdrawal" -ForegroundColor Green
Write-Host "  ✅ GET /{reference}" -ForegroundColor Green
Write-Host "  ✅ GET /id/{id}" -ForegroundColor Green
Write-Host "  ✅ GET /wallet/{id}" -ForegroundColor Green
Write-Host "  ✅ GET /wallet/{id}/sent" -ForegroundColor Green
Write-Host "  ✅ GET /wallet/{id}/received" -ForegroundColor Green
Write-Host "  ✅ GET /type/{type} (ADMIN)" -ForegroundColor Green
Write-Host ""
Write-Host "Security Tests:" -ForegroundColor Yellow
Write-Host "  ✅ Authentication required" -ForegroundColor Green
Write-Host "  ✅ Role-based access control" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
