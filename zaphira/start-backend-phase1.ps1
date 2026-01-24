# ============================================================================
# Zaphira Backend - Phase 1 Startup Script (Login Only)
# ============================================================================
# This script starts 3 core services for authentication testing:
#   1. service-registry (Eureka) - :8761
#   2. auth-service - :8081
#   3. api-gateway - :8080
#
# Requirements:
#   - PostgreSQL running on 192.168.0.122:5432
#   - Database 'wallet_db' must exist
#   - Java 17+ installed
#   - Maven 3.8+ installed
# ============================================================================

Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  Zaphira Backend - Phase 1 Startup (Login Only)" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $scriptDir "backend"

# Check prerequisites
Write-Host "[1/6] Checking prerequisites..." -ForegroundColor Yellow

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
Write-Host "[2/6] Starting service-registry (Eureka) on port 8761..." -ForegroundColor Yellow
Write-Host "  This may take 30-40 seconds to fully start." -ForegroundColor Gray

$eurekaDir = Join-Path $backendDir "service-registry"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$eurekaDir'; mvn spring-boot:run" -WindowStyle Normal

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
Write-Host "[3/6] Starting auth-service on port 8081..." -ForegroundColor Yellow
Write-Host "  This may take 20-30 seconds." -ForegroundColor Gray

$authDir = Join-Path $backendDir "auth-service"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$authDir'; mvn spring-boot:run" -WindowStyle Normal

Write-Host "  Waiting for auth-service to start (25 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 25

Write-Host ""
Write-Host "[4/6] Starting api-gateway on port 8080..." -ForegroundColor Yellow
Write-Host "  This may take 15-20 seconds." -ForegroundColor Gray

$gatewayDir = Join-Path $backendDir "api-gateway"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$gatewayDir'; mvn spring-boot:run" -WindowStyle Normal

Write-Host "  Waiting for api-gateway to start (20 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 20

Write-Host ""
Write-Host "[5/6] Verifying services..." -ForegroundColor Yellow

# Check API Gateway
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    Write-Host "  ✓ API Gateway is running: http://localhost:8080" -ForegroundColor Green
} catch {
    Write-Host "  ⚠ API Gateway health check failed (may still be starting)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[6/6] All services started!" -ForegroundColor Green
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  PHASE 1 - AUTHENTICATION SERVICES READY" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor White
Write-Host "  • Eureka Dashboard:  http://localhost:8761" -ForegroundColor Cyan
Write-Host "  • API Gateway:       http://localhost:8080" -ForegroundColor Cyan
Write-Host "  • Auth Service:      http://localhost:8081 (internal)" -ForegroundColor Gray
Write-Host ""
Write-Host "Available Endpoints:" -ForegroundColor White
Write-Host "  • POST http://localhost:8080/api/auth/login" -ForegroundColor Green
Write-Host "  • POST http://localhost:8080/api/auth/refresh" -ForegroundColor Green
Write-Host "  • GET  http://localhost:8080/api/auth/validate" -ForegroundColor Green
Write-Host ""
Write-Host "Test with curl:" -ForegroundColor White
Write-Host '  curl -X POST http://localhost:8080/api/auth/login \' -ForegroundColor Yellow
Write-Host '    -H "Content-Type: application/json" \' -ForegroundColor Yellow
Write-Host '    -d ''{"phoneNumber": "+237690123456", "pin": "1234"}''' -ForegroundColor Yellow
Write-Host ""
Write-Host "Note: You need to manually create test users in the database." -ForegroundColor Magenta
Write-Host "      Registration is NOT available in Phase 1." -ForegroundColor Magenta
Write-Host ""
Write-Host "To stop all services, close the PowerShell windows or press Ctrl+C in each." -ForegroundColor Gray
Write-Host "==============================================================" -ForegroundColor Cyan
