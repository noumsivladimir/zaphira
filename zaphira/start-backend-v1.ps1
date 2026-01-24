# ==========================================
# ZAPHIRA PLATFORM - START ALL SERVICES
# ==========================================
# This script starts all backend microservices in the correct order
# Services: Eureka → Auth → User → Wallet → Notification → Gateway
# ==========================================

$BACKEND_DIR = "backend"
$SERVICES = @(
    @{Name="Service Registry (Eureka)"; Module="service-registry"; Port=8761; WaitTime=15},
    @{Name="Auth Service"; Module="auth-service"; Port=8081; WaitTime=10},
    @{Name="User Service"; Module="user-service"; Port=8082; WaitTime=10},
    @{Name="Wallet Service"; Module="wallet-service"; Port=8083; WaitTime=10},
    @{Name="Notification Service"; Module="notification-service"; Port=8089; WaitTime=8},
    @{Name="API Gateway"; Module="api-gateway"; Port=8080; WaitTime=10}
)

# ANSI Colors
$GREEN = "`e[32m"
$BLUE = "`e[36m"
$YELLOW = "`e[33m"
$RED = "`e[31m"
$NC = "`e[0m"

function Write-Status {
    param([string]$Message, [string]$Color = $NC)
    Write-Host "${Color}$Message${NC}"
}

function Test-Port {
    param([int]$Port)
    
    try {
        $connection = New-Object System.Net.Sockets.TcpClient("localhost", $Port)
        $connection.Close()
        return $true
    }
    catch {
        return $false
    }
}

function Wait-ForService {
    param(
        [string]$ServiceName,
        [int]$Port,
        [int]$MaxWaitSeconds
    )
    
    Write-Status "⏳ Waiting for $ServiceName to start (port $Port)..." $YELLOW
    
    $elapsed = 0
    while ($elapsed -lt $MaxWaitSeconds) {
        if (Test-Port -Port $Port) {
            Write-Status "✅ $ServiceName is UP!" $GREEN
            return $true
        }
        Start-Sleep -Seconds 1
        $elapsed++
        Write-Host "." -NoNewline
    }
    
    Write-Host ""
    Write-Status "⚠️  $ServiceName did not start within $MaxWaitSeconds seconds" $RED
    return $false
}

# ==========================================
# MAIN EXECUTION
# ==========================================

Write-Host ""
Write-Status "═══════════════════════════════════════════════════" $BLUE
Write-Status "    ZAPHIRA PLATFORM - BACKEND STARTUP" $BLUE
Write-Status "═══════════════════════════════════════════════════" $BLUE
Write-Host ""

# Check if Maven Wrapper exists
if (-not (Test-Path "$BACKEND_DIR\mvnw.cmd")) {
    Write-Status "❌ Maven wrapper not found in $BACKEND_DIR" $RED
    Write-Status "Please run this script from the project root directory" $RED
    exit 1
}

# Check Java installation
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Status "☕ Java: $javaVersion" $GREEN
}
catch {
    Write-Status "❌ Java not found. Please install Java 17+" $RED
    exit 1
}

Write-Host ""

# Start each service
foreach ($service in $SERVICES) {
    Write-Host ""
    Write-Status "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" $BLUE
    Write-Status "Starting: $($service.Name)" $BLUE
    Write-Status "Module: $($service.Module)" $BLUE
    Write-Status "Port: $($service.Port)" $BLUE
    Write-Status "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" $BLUE
    
    # Check if port is already in use
    if (Test-Port -Port $service.Port) {
        Write-Status "⚠️  Port $($service.Port) is already in use. Skipping $($service.Name)" $YELLOW
        continue
    }
    
    # Start service in new window
    $command = "cd $BACKEND_DIR; .\mvnw.cmd spring-boot:run -pl $($service.Module)"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $command
    
    # Wait for service to be ready
    $isReady = Wait-ForService -ServiceName $service.Name -Port $service.Port -MaxWaitSeconds $service.WaitTime
    
    if (-not $isReady) {
        Write-Status "⚠️  $($service.Name) may not have started correctly" $YELLOW
        $continue = Read-Host "Continue anyway? (y/n)"
        if ($continue -ne "y") {
            Write-Status "Startup cancelled by user" $RED
            exit 1
        }
    }
    
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host ""
Write-Status "═══════════════════════════════════════════════════" $GREEN
Write-Status "    ALL SERVICES STARTED!" $GREEN
Write-Status "═══════════════════════════════════════════════════" $GREEN
Write-Host ""

Write-Status "Service Status:" $BLUE
Write-Host ""

foreach ($service in $SERVICES) {
    $status = if (Test-Port -Port $service.Port) { "✅ RUNNING" } else { "❌ DOWN" }
    $color = if (Test-Port -Port $service.Port) { $GREEN } else { $RED }
    Write-Status "  $($service.Name) (Port $($service.Port)): $status" $color
}

Write-Host ""
Write-Status "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" $BLUE
Write-Status "Quick Links:" $BLUE
Write-Status "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" $BLUE
Write-Host "  Eureka Dashboard:   http://localhost:8761"
Write-Host "  API Gateway:        http://localhost:8080"
Write-Host "  Gateway Health:     http://localhost:8080/actuator/health"
Write-Host "  Gateway Routes:     http://localhost:8080/actuator/gateway/routes"
Write-Host ""
Write-Status "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" $BLUE
Write-Status "API v1 Endpoints:" $BLUE
Write-Status "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" $BLUE
Write-Host "  Auth:        POST http://localhost:8080/api/v1/auth/login"
Write-Host "  Register:    POST http://localhost:8080/api/v1/users/register"
Write-Host "  Wallets:     GET  http://localhost:8080/api/v1/wallets/user/{id}"
Write-Host "  Notifications: GET http://localhost:8080/api/v1/notifications"
Write-Host ""
Write-Status "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" $YELLOW
Write-Status "Next Steps:" $YELLOW
Write-Status "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" $YELLOW
Write-Host "  1. Run validation: .\test-gateway-v1-routes.ps1"
Write-Host "  2. View documentation: API_GATEWAY_V1_DOCUMENTATION.md"
Write-Host "  3. Start frontend: cd frontend\web-app && bun run dev"
Write-Host ""

Write-Status "Press any key to exit..." $BLUE
$null = $host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
