# ==========================================
# API GATEWAY v1 ROUTES VALIDATION SCRIPT
# ==========================================
# Purpose: Test all /api/v1/* routes through the API Gateway
# Gateway Port: 8080
# Services: auth-service, user-service, wallet-service, notification-service
# ==========================================

$GATEWAY_URL = "http://localhost:8080"
$EUREKA_URL = "http://localhost:8761"

# ANSI Color Codes
$GREEN = "`e[32m"
$RED = "`e[31m"
$YELLOW = "`e[33m"
$BLUE = "`e[36m"
$NC = "`e[0m" # No Color

function Write-Status {
    param(
        [string]$Message,
        [string]$Status
    )
    
    $color = switch ($Status) {
        "SUCCESS" { $GREEN }
        "FAILED" { $RED }
        "WARN" { $YELLOW }
        "INFO" { $BLUE }
        default { $NC }
    }
    
    Write-Host "${color}[$Status]${NC} $Message"
}

function Test-Endpoint {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [hashtable]$Body = $null,
        [hashtable]$Headers = @{},
        [string]$Description
    )
    
    $url = "$GATEWAY_URL$Path"
    Write-Host "`n----------------------------------------"
    Write-Host "Testing: $Description"
    Write-Host "Method: $Method"
    Write-Host "URL: $url"
    
    try {
        $params = @{
            Uri = $url
            Method = $Method
            Headers = $Headers
            TimeoutSec = 10
            ErrorAction = "Stop"
        }
        
        if ($Body) {
            $params.Body = ($Body | ConvertTo-Json -Depth 10)
            $params.Headers["Content-Type"] = "application/json"
        }
        
        $response = Invoke-WebRequest @params
        
        Write-Status "HTTP $($response.StatusCode)" "SUCCESS"
        Write-Host "Response Length: $($response.Content.Length) bytes"
        
        # Show response preview (first 200 chars)
        $preview = $response.Content.Substring(0, [Math]::Min(200, $response.Content.Length))
        Write-Host "Response Preview: $preview..."
        
        return $true
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode) {
            Write-Status "HTTP $statusCode - $($_.Exception.Message)" "FAILED"
        }
        else {
            Write-Status "Connection Failed - $($_.Exception.Message)" "FAILED"
        }
        return $false
    }
}

# ==========================================
# STEP 1: Check Gateway Health
# ==========================================
Write-Host "`n=========================================="
Write-Host "STEP 1: API GATEWAY HEALTH CHECK"
Write-Host "=========================================="

$gatewayHealth = Test-Endpoint -Path "/actuator/health" -Description "Gateway Health"
if (-not $gatewayHealth) {
    Write-Status "Gateway is not running! Start it first." "FAILED"
    Write-Host "`nRun: cd backend && mvnw spring-boot:run -pl api-gateway"
    exit 1
}

# ==========================================
# STEP 2: Check Eureka Service Registry
# ==========================================
Write-Host "`n=========================================="
Write-Host "STEP 2: EUREKA SERVICE REGISTRY"
Write-Host "=========================================="

try {
    $eureka = Invoke-RestMethod -Uri "$EUREKA_URL/eureka/apps" -Headers @{"Accept"="application/json"}
    $services = $eureka.applications.application.name
    
    Write-Status "Registered Services: $($services -join ', ')" "SUCCESS"
    
    $expectedServices = @("API-GATEWAY", "AUTH-SERVICE", "USER-SERVICE", "WALLET-SERVICE", "NOTIFICATION-SERVICE")
    foreach ($service in $expectedServices) {
        if ($services -contains $service) {
            Write-Status "$service is registered" "SUCCESS"
        }
        else {
            Write-Status "$service is NOT registered" "WARN"
        }
    }
}
catch {
    Write-Status "Cannot connect to Eureka at $EUREKA_URL" "WARN"
    Write-Host "Continuing with Gateway tests..."
}

# ==========================================
# STEP 3: Check Gateway Routes
# ==========================================
Write-Host "`n=========================================="
Write-Host "STEP 3: GATEWAY ROUTES CONFIGURATION"
Write-Host "=========================================="

try {
    $routes = Invoke-RestMethod -Uri "$GATEWAY_URL/actuator/gateway/routes"
    Write-Status "Found $($routes.Count) configured routes" "SUCCESS"
    
    foreach ($route in $routes) {
        Write-Host "`nRoute ID: $($route.route_id)"
        Write-Host "  URI: $($route.uri)"
        Write-Host "  Predicates: $($route.predicates -join ', ')"
        Write-Host "  Filters: $($route.filters -join ', ')"
    }
}
catch {
    Write-Status "Cannot retrieve routes from Gateway" "WARN"
}

# ==========================================
# STEP 4: Test API v1 Routes
# ==========================================
Write-Host "`n=========================================="
Write-Host "STEP 4: API v1 ROUTES VALIDATION"
Write-Host "=========================================="

$testResults = @{
    passed = 0
    failed = 0
    total = 0
}

# Test 1: Auth Service - Health
$testResults.total++
if (Test-Endpoint -Path "/api/v1/auth/actuator/health" -Description "Auth Service Health via Gateway") {
    $testResults.passed++
}
else {
    $testResults.failed++
}

# Test 2: User Service - Health
$testResults.total++
if (Test-Endpoint -Path "/api/v1/users/actuator/health" -Description "User Service Health via Gateway") {
    $testResults.passed++
}
else {
    $testResults.failed++
}

# Test 3: Wallet Service - Health
$testResults.total++
if (Test-Endpoint -Path "/api/v1/wallets/actuator/health" -Description "Wallet Service Health via Gateway") {
    $testResults.passed++
}
else {
    $testResults.failed++
}

# Test 4: Notification Service - Health
$testResults.total++
if (Test-Endpoint -Path "/api/v1/notifications/actuator/health" -Description "Notification Service Health via Gateway") {
    $testResults.passed++
}
else {
    $testResults.failed++
}

# Test 5: Security Questions
$testResults.total++
if (Test-Endpoint -Path "/api/v1/security-questions" -Description "Security Questions Endpoint") {
    $testResults.passed++
}
else {
    $testResults.failed++
}

# Test 6: Activity Logs
$testResults.total++
if (Test-Endpoint -Path "/api/v1/logs/actuator/health" -Description "Activity Logs Health via Gateway") {
    $testResults.passed++
}
else {
    $testResults.failed++
}

# ==========================================
# STEP 5: Test CORS Configuration
# ==========================================
Write-Host "`n=========================================="
Write-Host "STEP 5: CORS CONFIGURATION TEST"
Write-Host "=========================================="

$corsHeaders = @{
    "Origin" = "http://localhost:3000"
    "Access-Control-Request-Method" = "POST"
    "Access-Control-Request-Headers" = "Content-Type,Authorization"
}

try {
    $corsResponse = Invoke-WebRequest -Uri "$GATEWAY_URL/api/v1/auth/actuator/health" -Method OPTIONS -Headers $corsHeaders -ErrorAction Stop
    
    $corsHeadersReceived = $corsResponse.Headers["Access-Control-Allow-Origin"]
    if ($corsHeadersReceived) {
        Write-Status "CORS is properly configured" "SUCCESS"
        Write-Host "Access-Control-Allow-Origin: $corsHeadersReceived"
    }
    else {
        Write-Status "CORS headers not found in response" "WARN"
    }
}
catch {
    Write-Status "CORS preflight test failed" "WARN"
}

# ==========================================
# STEP 6: Test Circuit Breakers
# ==========================================
Write-Host "`n=========================================="
Write-Host "STEP 6: CIRCUIT BREAKER STATUS"
Write-Host "=========================================="

try {
    $circuitBreakers = Invoke-RestMethod -Uri "$GATEWAY_URL/actuator/circuitbreakers"
    Write-Status "Circuit Breakers are enabled" "SUCCESS"
    
    if ($circuitBreakers.circuitBreakers) {
        foreach ($cb in $circuitBreakers.circuitBreakers) {
            Write-Host "`nCircuit Breaker: $($cb.name)"
            Write-Host "  State: $($cb.state)"
        }
    }
}
catch {
    Write-Status "Circuit breaker metrics not available" "WARN"
}

# ==========================================
# FINAL REPORT
# ==========================================
Write-Host "`n=========================================="
Write-Host "TEST SUMMARY"
Write-Host "=========================================="

$successRate = if ($testResults.total -gt 0) { 
    [math]::Round(($testResults.passed / $testResults.total) * 100, 2) 
} else { 0 }

Write-Host "Total Tests: $($testResults.total)"
Write-Status "Passed: $($testResults.passed)" "SUCCESS"
Write-Status "Failed: $($testResults.failed)" "FAILED"
Write-Host "Success Rate: $successRate%"

if ($testResults.failed -eq 0) {
    Write-Status "`nAll tests passed! API Gateway v1 is properly configured." "SUCCESS"
    exit 0
}
else {
    Write-Status "`nSome tests failed. Check service availability and configuration." "WARN"
    exit 1
}
