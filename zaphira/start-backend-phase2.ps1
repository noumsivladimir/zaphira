# ============================================================================
# Zaphira Backend - Phase 2 Startup Script (Full Registration)
# ============================================================================
# This script starts 5 core services for full authentication + registration:
#   1. service-registry (Eureka) - :8761
#   2. wallet-service - :8083 (MUST start before user-service)
#   3. user-service - :8082
#   4. auth-service - :8081
#   5. api-gateway - :8080
#
# Requirements:
#   - PostgreSQL running on 192.168.0.122:5432
#   - Database 'wallet_db' must exist
#   - Java 17+ installed
#   - Maven 3.8+ installed
#   - (Optional) Kafka on 192.168.0.122:9092 for notifications
# ============================================================================

Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  Zaphira Backend - Phase 2 Startup (Full Registration)" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $scriptDir "backend"

# Check prerequisites
Write-Host "[1/9] Checking prerequisites..." -ForegroundColor Yellow

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "  ✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Java not found. Please install Java 17+." -ForegroundColor Red
    exit 1
}

# Check Maven
try {
    $mavenVersion = mvn -version | Select-String "Apache Maven" | Select-Object -First 1
    Write-Host "  ✓ Maven found: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Maven not found. Please install Maven 3.8+." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/9] Starting service-registry (Eureka) on port 8761..." -ForegroundColor Yellow
Write-Host "  This may take 30-40 seconds to fully start." -ForegroundColor Gray

$eurekaDir = Join-Path $backendDir "service-registry"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$eurekaDir'; Write-Host 'Starting Eureka...' -ForegroundColor Cyan; mvn spring-boot:run" -WindowStyle Normal

Write-Host "  Waiting for Eureka to start (30 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# Check if Eureka is running
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8761" -UseBasicParsing -TimeoutSec 5
    Write-Host "  ✓ Eureka started successfully!" -ForegroundColor Green
    Write-Host "    Dashboard: http://localhost:8761" -ForegroundColor Cyan
} catch {
    Write-Host "  ⚠ Warning: Could not verify Eureka startup. Continuing anyway..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[3/9] Starting wallet-service on port 8083..." -ForegroundColor Yellow
Write-Host "  ⚠ CRITICAL: This MUST start before user-service!" -ForegroundColor Magenta
Write-Host "  This may take 25-30 seconds." -ForegroundColor Gray

$walletDir = Join-Path $backendDir "wallet-service"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$walletDir'; Write-Host 'Starting Wallet Service...' -ForegroundColor Cyan; mvn spring-boot:run" -WindowStyle Normal

Write-Host "  Waiting for wallet-service to start (30 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

Write-Host "  ✓ Wallet service should be running now." -ForegroundColor Green

Write-Host ""
Write-Host "[4/9] Starting user-service on port 8082..." -ForegroundColor Yellow
Write-Host "  This service depends on wallet-service being UP." -ForegroundColor Gray
Write-Host "  This may take 25-30 seconds." -ForegroundColor Gray

$userDir = Join-Path $backendDir "user-service"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$userDir'; Write-Host 'Starting User Service...' -ForegroundColor Cyan; mvn spring-boot:run" -WindowStyle Normal

Write-Host "  Waiting for user-service to start (30 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

Write-Host "  ✓ User service should be running now." -ForegroundColor Green

Write-Host ""
Write-Host "[5/9] Starting auth-service on port 8081..." -ForegroundColor Yellow
Write-Host "  This may take 20-25 seconds." -ForegroundColor Gray

$authDir = Join-Path $backendDir "auth-service"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$authDir'; Write-Host 'Starting Auth Service...' -ForegroundColor Cyan; mvn spring-boot:run" -WindowStyle Normal

Write-Host "  Waiting for auth-service to start (25 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 25

Write-Host "  ✓ Auth service should be running now." -ForegroundColor Green

Write-Host ""
Write-Host "[6/9] Starting api-gateway on port 8080..." -ForegroundColor Yellow
Write-Host "  This should start LAST to ensure all routes are available." -ForegroundColor Gray
Write-Host "  This may take 15-20 seconds." -ForegroundColor Gray

$gatewayDir = Join-Path $backendDir "api-gateway"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$gatewayDir'; Write-Host 'Starting API Gateway...' -ForegroundColor Cyan; mvn spring-boot:run" -WindowStyle Normal

Write-Host "  Waiting for api-gateway to start (20 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 20

Write-Host ""
Write-Host "[7/9] Verifying services..." -ForegroundColor Yellow

# Check Eureka for registered services
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8761" -UseBasicParsing -TimeoutSec 5
    Write-Host "  ✓ Eureka is running: http://localhost:8761" -ForegroundColor Green
} catch {
    Write-Host "  ⚠ Eureka health check failed" -ForegroundColor Yellow
}

# Check API Gateway
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    Write-Host "  ✓ API Gateway is running: http://localhost:8080" -ForegroundColor Green
} catch {
    Write-Host "  ⚠ API Gateway health check failed (may still be starting)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[8/9] Waiting additional 10 seconds for service registration..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "[9/9] All services started!" -ForegroundColor Green
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  PHASE 2 - FULL REGISTRATION SERVICES READY" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor White
Write-Host "  • Eureka Dashboard:  http://localhost:8761" -ForegroundColor Cyan
Write-Host "  • API Gateway:       http://localhost:8080" -ForegroundColor Cyan
Write-Host "  • User Service:      http://localhost:8082 (internal)" -ForegroundColor Gray
Write-Host "  • Wallet Service:    http://localhost:8083 (internal)" -ForegroundColor Gray
Write-Host "  • Auth Service:      http://localhost:8081 (internal)" -ForegroundColor Gray
Write-Host ""
Write-Host "Available Endpoints:" -ForegroundColor White
Write-Host "  Authentication:" -ForegroundColor Yellow
Write-Host "    • POST http://localhost:8080/api/auth/login" -ForegroundColor Green
Write-Host "    • POST http://localhost:8080/api/auth/refresh" -ForegroundColor Green
Write-Host "    • GET  http://localhost:8080/api/auth/validate" -ForegroundColor Green
Write-Host ""
Write-Host "  User Management:" -ForegroundColor Yellow
Write-Host "    • POST http://localhost:8080/api/users/register" -ForegroundColor Green
Write-Host "    • POST http://localhost:8080/api/users/verify-email" -ForegroundColor Green
Write-Host "    • GET  http://localhost:8080/api/users/profile" -ForegroundColor Green
Write-Host "    • PUT  http://localhost:8080/api/users/profile" -ForegroundColor Green
Write-Host ""
Write-Host "Test Registration:" -ForegroundColor White
Write-Host '  curl -X POST http://localhost:8080/api/users/register \' -ForegroundColor Yellow
Write-Host '    -H "Content-Type: application/json" \' -ForegroundColor Yellow
Write-Host '    -d ''{"phoneNumber": "+237690123456", "email": "test@example.com", "pin": "1234", "firstName": "John", "lastName": "Doe"}''' -ForegroundColor Yellow
Write-Host ""
Write-Host "Note: Wallet creation happens SYNCHRONOUSLY via Feign during verification." -ForegroundColor Magenta
Write-Host "      Notifications (SMS/Email) are NOT enabled unless you start notification-service." -ForegroundColor Magenta
Write-Host ""
Write-Host "To stop all services, close the PowerShell windows or press Ctrl+C in each." -ForegroundColor Gray
Write-Host "==============================================================" -ForegroundColor Cyan
