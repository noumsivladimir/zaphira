# Script pour exécuter les tests d'intégration de tous les microservices
# Date: 3 Février 2026

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ZAPHIRA PLATFORM - TESTS D'INTEGRATION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Continue"
$services = @(
    @{Name="Transaction Service"; Path="transaction-service"},
    @{Name="Wallet Service"; Path="wallet-service"}
)

$totalTests = 0
$totalPassed = 0
$totalFailed = 0
$totalSkipped = 0

foreach ($service in $services) {
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    Write-Host "Testing: $($service.Name)" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    Write-Host ""
    
    $servicePath = Join-Path $PSScriptRoot $service.Path
    
    if (Test-Path $servicePath) {
        Push-Location $servicePath
        
        Write-Host "Nettoyage..." -ForegroundColor Gray
        mvn clean -q
        
        Write-Host "Compilation..." -ForegroundColor Gray
        $compileResult = mvn compile -q 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERREUR: Echec de compilation pour $($service.Name)" -ForegroundColor Red
            Write-Host $compileResult
            Pop-Location
            continue
        }
        
        Write-Host "Execution des tests..." -ForegroundColor Green
        $testOutput = mvn test -Dtest="*IntegrationTest" 2>&1 | Out-String
        
        # Parser les résultats
        if ($testOutput -match "Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)") {
            $run = [int]$matches[1]
            $failures = [int]$matches[2]
            $errors = [int]$matches[3]
            $skipped = [int]$matches[4]
            $passed = $run - $failures - $errors - $skipped
            
            $totalTests += $run
            $totalPassed += $passed
            $totalFailed += ($failures + $errors)
            $totalSkipped += $skipped
            
            Write-Host ""
            Write-Host "Résultats pour $($service.Name):" -ForegroundColor Cyan
            Write-Host "  Tests executés: $run" -ForegroundColor White
            Write-Host "  Réussis: $passed" -ForegroundColor Green
            Write-Host "  Echecs: $($failures + $errors)" -ForegroundColor Red
            Write-Host "  Ignorés: $skipped" -ForegroundColor Yellow
            Write-Host ""
            
            if (($failures + $errors) -gt 0) {
                Write-Host "ATTENTION: Certains tests ont échoué!" -ForegroundColor Red
                # Afficher les erreurs
                $testOutput -split "`n" | Where-Object { $_ -match "FAILED" -or $_ -match "ERROR" } | ForEach-Object {
                    Write-Host "  $_" -ForegroundColor Red
                }
            } else {
                Write-Host "SUCCES: Tous les tests sont passés!" -ForegroundColor Green
            }
        } else {
            Write-Host "Impossible de parser les résultats des tests" -ForegroundColor Yellow
            Write-Host $testOutput
        }
        
        Pop-Location
    } else {
        Write-Host "ERREUR: Service non trouvé: $servicePath" -ForegroundColor Red
    }
    
    Write-Host ""
}

# Résumé global
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUME GLOBAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total tests exécutés: $totalTests" -ForegroundColor White
Write-Host "Total réussis: $totalPassed" -ForegroundColor Green
Write-Host "Total échecs: $totalFailed" -ForegroundColor $(if ($totalFailed -gt 0) { "Red" } else { "Green" })
Write-Host "Total ignorés: $totalSkipped" -ForegroundColor Yellow
Write-Host ""

$successRate = if ($totalTests -gt 0) { [math]::Round(($totalPassed / $totalTests) * 100, 2) } else { 0 }
Write-Host "Taux de réussite: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 60) { "Yellow" } else { "Red" })

if ($totalFailed -eq 0 -and $totalTests -gt 0) {
    Write-Host ""
    Write-Host "✓ VALIDATION COMPLETE: Tous les tests sont passés!" -ForegroundColor Green
    Write-Host "Le core de l'API est fonctionnel." -ForegroundColor Green
} elseif ($totalFailed -gt 0) {
    Write-Host ""
    Write-Host "✗ ATTENTION: Certains tests ont échoué" -ForegroundColor Red
    Write-Host "Veuillez corriger les problèmes avant de continuer." -ForegroundColor Red
} else {
    Write-Host ""
    Write-Host "⚠ AVERTISSEMENT: Aucun test n'a été exécuté" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Pour voir les rapports détaillés:" -ForegroundColor Cyan
Write-Host "  Transaction Service: transaction-service/target/surefire-reports/" -ForegroundColor Gray
Write-Host "  Wallet Service: wallet-service/target/surefire-reports/" -ForegroundColor Gray
Write-Host ""

# Exit code
if ($totalFailed -gt 0) {
    exit 1
} else {
    exit 0
}
