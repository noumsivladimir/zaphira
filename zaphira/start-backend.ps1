# Zaphira Backend - Complete Integration Startup Script
# Starts all required services in the correct order

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Zaphira Backend - Complete Integration" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$BACKEND_DIR = "C:\Users\HP\Downloads\zaphira-fullstack-20-01-2026\zaphira-platform\backend"

# Check if backend directory exists
if (-not (Test-Path $BACKEND_DIR)) {
    Write-Host "ERROR: Backend directory not found at $BACKEND_DIR" -ForegroundColor Red
    exit 1
}

Write-Host "Backend directory: $BACKEND_DIR" -ForegroundColor Green
Write-Host ""

# Function to check if port is in use
function Test-Port {
    param($Port)
    $connection = Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
    return $connection
}

# Function to wait for service to be ready
function Wait-ForService {
    param(
        [string]$ServiceName,
        [int]$Port,
        [int]$MaxWaitSeconds = 60
    )
    
    Write-Host "Waiting for $ServiceName on port $Port..." -ForegroundColor Yellow
    $elapsed = 0
    $interval = 5
    
    while ($elapsed -lt $MaxWaitSeconds) {
        if (Test-Port -Port $Port) {
            Write-Host "✓ $ServiceName is ready!" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds $interval
        $elapsed += $interval
        Write-Host "  Still waiting... ($elapsed/$MaxWaitSeconds seconds)" -ForegroundColor Gray
    }
    
    Write-Host "✗ $ServiceName failed to start within $MaxWaitSeconds seconds" -ForegroundColor Red
    return $false
}

# ============================================
# PHASE 0: Infrastructure Check
# ============================================

Write-Host "PHASE 0: Infrastructure Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check PostgreSQL
Write-Host "Checking PostgreSQL (port 5432)..." -ForegroundColor Yellow
if (-not (Test-Port -Port 5432)) {
    Write-Host "✗ PostgreSQL is not running!" -ForegroundColor Red
    Write-Host "  Please start PostgreSQL before continuing." -ForegroundColor Yellow
    Write-Host "  Database required: wallet_db" -ForegroundColor Yellow
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        exit 1
    }
} else {
    Write-Host "✓ PostgreSQL is running" -ForegroundColor Green
}

# Check Kafka
Write-Host "Checking Kafka (port 9092)..." -ForegroundColor Yellow
if (-not (Test-Port -Port 9092)) {
    Write-Host "✗ Kafka is not running!" -ForegroundColor Red
    Write-Host "  wallet-service requires Kafka to start." -ForegroundColor Yellow
    Write-Host "  You can start Kafka with: docker run -d -p 9092:9092 apache/kafka:latest" -ForegroundColor Yellow
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        exit 1
    }
} else {
    Write-Host "✓ Kafka is running" -ForegroundColor Green
}

Write-Host ""

# ============================================
# PHASE 1: Build all services
# ============================================

Write-Host "PHASE 1: Building all services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Set-Location $BACKEND_DIR

Write-Host "Running: mvn clean install -DskipTests" -ForegroundColor Yellow
mvn clean install -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Build successful!" -ForegroundColor Green
Write-Host ""

# ============================================
# PHASE 2: Start services in order
# ============================================

Write-Host "PHASE 2: Starting services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Note: We're not using service-registry (Eureka) for this integration
# Services will communicate directly via configured URLs

# Service 1: wallet-service (port 8083)
Write-Host "[1/5] Starting wallet-service..." -ForegroundColor Cyan
$walletJob = Start-Job -ScriptBlock {
    Set-Location "C:\Users\HP\Downloads\zaphira-fullstack-20-01-2026\zaphira-platform\backend\wallet-service"
    mvn spring-boot:run 2>&1
}

if (-not (Wait-ForService -ServiceName "wallet-service" -Port 8083 -MaxWaitSeconds 90)) {
    Write-Host "Failed to start wallet-service. Check logs." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Service 2: user-service (port 8082)
Write-Host "[2/5] Starting user-service..." -ForegroundColor Cyan
$userJob = Start-Job -ScriptBlock {
    Set-Location "C:\Users\HP\Downloads\zaphira-fullstack-20-01-2026\zaphira-platform\backend\user-service"
    mvn spring-boot:run 2>&1
}

if (-not (Wait-ForService -ServiceName "user-service" -Port 8082 -MaxWaitSeconds 90)) {
    Write-Host "Failed to start user-service. Check logs." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Service 3: auth-service (port 8081)
Write-Host "[3/5] Starting auth-service..." -ForegroundColor Cyan
$authJob = Start-Job -ScriptBlock {
    Set-Location "C:\Users\HP\Downloads\zaphira-fullstack-20-01-2026\zaphira-platform\backend\auth-service"
    mvn spring-boot:run 2>&1
}

if (-not (Wait-ForService -ServiceName "auth-service" -Port 8081 -MaxWaitSeconds 90)) {
    Write-Host "Failed to start auth-service. Check logs." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Service 4: notification-service (port 8089) - Optional
Write-Host "[4/5] Starting notification-service (optional)..." -ForegroundColor Cyan
$notificationJob = Start-Job -ScriptBlock {
    Set-Location "C:\Users\HP\Downloads\zaphira-fullstack-20-01-2026\services-tree\notification-service"
    mvn spring-boot:run 2>&1
}

# Don't wait too long for notification service as it's optional
Start-Sleep -Seconds 30

Write-Host ""

# Service 5: api-gateway (port 8080) - MUST START LAST
Write-Host "[5/5] Starting api-gateway..." -ForegroundColor Cyan
$gatewayJob = Start-Job -ScriptBlock {
    Set-Location "C:\Users\HP\Downloads\zaphira-fullstack-20-01-2026\zaphira-platform\backend\api-gateway"
    mvn spring-boot:run 2>&1
}

if (-not (Wait-ForService -ServiceName "api-gateway" -Port 8080 -MaxWaitSeconds 60)) {
    Write-Host "Failed to start api-gateway. Check logs." -ForegroundColor Red
    exit 1
}

Write-Host ""

# ============================================
# PHASE 3: Summary
# ============================================

Write-Host "========================================" -ForegroundColor Green
Write-Host "✓ ALL SERVICES STARTED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Write-Host "Service Status:" -ForegroundColor Cyan
Write-Host "  • wallet-service      :8083  ✓" -ForegroundColor Green
Write-Host "  • user-service        :8082  ✓" -ForegroundColor Green
Write-Host "  • auth-service        :8081  ✓" -ForegroundColor Green
Write-Host "  • notification-srv    :8089  ?" -ForegroundColor Yellow
Write-Host "  • api-gateway         :8080  ✓" -ForegroundColor Green
Write-Host ""

Write-Host "Frontend Integration:" -ForegroundColor Cyan
Write-Host "  Base URL: http://localhost:8080" -ForegroundColor White
Write-Host "  Available endpoints:" -ForegroundColor White
Write-Host "    POST /api/auth/login" -ForegroundColor Gray
Write-Host "    POST /api/users/register" -ForegroundColor Gray
Write-Host "    POST /api/users/verify-email" -ForegroundColor Gray
Write-Host "    GET  /api/users/profile" -ForegroundColor Gray
Write-Host ""

Write-Host "To test the API:" -ForegroundColor Cyan
Write-Host '  curl http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"phoneNumber\":\"+237690123456\",\"pin\":\"1234\"}"' -ForegroundColor Gray
Write-Host ""

Write-Host "Job IDs (for stopping):" -ForegroundColor Cyan
Write-Host "  wallet-service: $($walletJob.Id)" -ForegroundColor Gray
Write-Host "  user-service: $($userJob.Id)" -ForegroundColor Gray
Write-Host "  auth-service: $($authJob.Id)" -ForegroundColor Gray
Write-Host "  notification-service: $($notificationJob.Id)" -ForegroundColor Gray
Write-Host "  api-gateway: $($gatewayJob.Id)" -ForegroundColor Gray
Write-Host ""

Write-Host "To stop all services, run:" -ForegroundColor Yellow
Write-Host "  Get-Job | Stop-Job; Get-Job | Remove-Job" -ForegroundColor Gray
Write-Host ""

Write-Host "Press Ctrl+C to stop monitoring (services will continue running)" -ForegroundColor Yellow
Write-Host "Press Enter to view live logs..." -ForegroundColor Yellow
Read-Host

# Monitor logs
Write-Host "Monitoring logs (Ctrl+C to exit)..." -ForegroundColor Cyan
Get-Job | Receive-Job -Wait
