# ============================================================================
# ZAPHIRA Platform - Database Migration Execution Script
# ============================================================================
# Description: Automated execution of all database migration scripts
# Author: AI Backend Architect
# Date: 21 January 2026
# Usage: .\execute-migrations.ps1 -PostgresHost localhost -PostgresUser postgres
# ============================================================================

param(
    [string]$PostgresHost = "localhost",
    [int]$PostgresPort = 5432,
    [string]$PostgresUser = "postgres",
    [switch]$DryRun = $false,
    [switch]$SkipBackup = $false,
    [switch]$Force = $false
)

# Color output functions
function Write-Success { param($Message) Write-Host "✓ $Message" -ForegroundColor Green }
function Write-Error { param($Message) Write-Host "✗ $Message" -ForegroundColor Red }
function Write-Info { param($Message) Write-Host "ℹ $Message" -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host "⚠ $Message" -ForegroundColor Yellow }
function Write-Step { param($Message) Write-Host "`n━━━ $Message ━━━" -ForegroundColor Magenta }

# Script directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Migration files in order
$MigrationFiles = @(
    "01_create_databases.sql",
    "02_user_service_schema.sql",
    "03_auth_service_schema.sql",
    "04_wallet_service_schema.sql",
    "05_transaction_service_schema.sql",
    "06_dispute_service_schema.sql",
    "07_reporting_service_schema.sql",
    "08_exchange_service_schema.sql",
    "09_notification_service_schema.sql"
)

# Database names for verification
$Databases = @(
    "zaphira_users_db",
    "zaphira_auth_db",
    "zaphira_wallets_db",
    "zaphira_transactions_db",
    "zaphira_disputes_db",
    "zaphira_reports_db",
    "zaphira_exchange_db",
    "zaphira_notifications_db"
)

# ============================================================================
# Function: Test PostgreSQL Connection
# ============================================================================
function Test-PostgresConnection {
    Write-Step "Testing PostgreSQL Connection"
    
    try {
        $env:PGPASSWORD = Read-Host "Enter PostgreSQL password for user '$PostgresUser'" -AsSecureString
        $env:PGPASSWORD = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($env:PGPASSWORD)
        )
        
        $result = & psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d postgres -c "SELECT version();" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Successfully connected to PostgreSQL"
            Write-Info "PostgreSQL Version: $($result[2])"
            return $true
        } else {
            Write-Error "Failed to connect to PostgreSQL"
            Write-Error $result
            return $false
        }
    } catch {
        Write-Error "Connection test failed: $_"
        return $false
    }
}

# ============================================================================
# Function: Create Backup
# ============================================================================
function New-DatabaseBackup {
    if ($SkipBackup) {
        Write-Warning "Skipping backup as requested"
        return
    }
    
    Write-Step "Creating Database Backup"
    
    $BackupDir = Join-Path $ScriptDir "backups"
    if (-not (Test-Path $BackupDir)) {
        New-Item -ItemType Directory -Path $BackupDir | Out-Null
    }
    
    $Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $BackupFile = Join-Path $BackupDir "zaphira_backup_$Timestamp.sql"
    
    try {
        Write-Info "Creating full backup to: $BackupFile"
        
        # Check if any Zaphira databases exist
        $existingDbs = & psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d postgres -t -c "SELECT datname FROM pg_database WHERE datname LIKE 'zaphira%';" 2>&1
        
        if ($existingDbs -and $existingDbs.Count -gt 0) {
            & pg_dumpall -h $PostgresHost -p $PostgresPort -U $PostgresUser > $BackupFile 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                Write-Success "Backup created successfully"
                Write-Info "Backup location: $BackupFile"
            } else {
                Write-Warning "Backup failed, but continuing..."
            }
        } else {
            Write-Info "No existing Zaphira databases found - skipping backup"
        }
    } catch {
        Write-Warning "Backup failed: $_"
        Write-Warning "Continuing with migration..."
    }
}

# ============================================================================
# Function: Execute SQL File
# ============================================================================
function Invoke-SqlFile {
    param(
        [string]$FilePath,
        [string]$Description
    )
    
    Write-Info "Executing: $Description"
    Write-Info "File: $FilePath"
    
    if (-not (Test-Path $FilePath)) {
        Write-Error "File not found: $FilePath"
        return $false
    }
    
    if ($DryRun) {
        Write-Warning "[DRY RUN] Would execute: $FilePath"
        return $true
    }
    
    try {
        $result = & psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d postgres -f $FilePath 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Successfully executed: $Description"
            return $true
        } else {
            Write-Error "Failed to execute: $Description"
            Write-Error $result
            return $false
        }
    } catch {
        Write-Error "Execution failed: $_"
        return $false
    }
}

# ============================================================================
# Function: Verify Database Creation
# ============================================================================
function Test-DatabaseCreation {
    Write-Step "Verifying Database Creation"
    
    $allSuccess = $true
    
    foreach ($db in $Databases) {
        $result = & psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d postgres -t -c "SELECT 1 FROM pg_database WHERE datname = '$db';" 2>&1
        
        if ($result -and $result.Trim() -eq "1") {
            Write-Success "Database exists: $db"
        } else {
            Write-Error "Database missing: $db"
            $allSuccess = $false
        }
    }
    
    return $allSuccess
}

# ============================================================================
# Function: Verify Table Creation
# ============================================================================
function Test-TableCreation {
    Write-Step "Verifying Table Creation"
    
    $expectedTables = @{
        "zaphira_users_db" = 11
        "zaphira_auth_db" = 3
        "zaphira_wallets_db" = 5
        "zaphira_transactions_db" = 9
        "zaphira_disputes_db" = 3
        "zaphira_reports_db" = 5
        "zaphira_exchange_db" = 4
        "zaphira_notifications_db" = 9
    }
    
    $allSuccess = $true
    
    foreach ($db in $expectedTables.Keys) {
        $expected = $expectedTables[$db]
        
        $result = & psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d $db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>&1
        
        if ($result) {
            $actual = [int]$result.Trim()
            
            if ($actual -eq $expected) {
                Write-Success "$db : $actual tables (expected $expected)"
            } else {
                Write-Warning "$db : $actual tables (expected $expected)"
                $allSuccess = $false
            }
        } else {
            Write-Error "$db : Could not count tables"
            $allSuccess = $false
        }
    }
    
    return $allSuccess
}

# ============================================================================
# Function: Display Summary
# ============================================================================
function Show-Summary {
    Write-Step "Migration Summary"
    
    Write-Host "`nDatabases Created:" -ForegroundColor Cyan
    foreach ($db in $Databases) {
        Write-Host "  • $db" -ForegroundColor White
    }
    
    Write-Host "`nTotal Statistics:" -ForegroundColor Cyan
    Write-Host "  • Databases: 8" -ForegroundColor White
    Write-Host "  • Total Tables: 49" -ForegroundColor White
    Write-Host "  • Migration Scripts: 9" -ForegroundColor White
    
    Write-Host "`nNext Steps:" -ForegroundColor Cyan
    Write-Host "  1. Update Spring Boot application.yml files" -ForegroundColor Yellow
    Write-Host "  2. Configure connection strings for each microservice" -ForegroundColor Yellow
    Write-Host "  3. Set spring.jpa.hibernate.ddl-auto=validate" -ForegroundColor Yellow
    Write-Host "  4. Test application connectivity" -ForegroundColor Yellow
    Write-Host "  5. Deploy to development environment" -ForegroundColor Yellow
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

Write-Host @"
╔════════════════════════════════════════════════════════════════╗
║                                                                ║
║        ZAPHIRA Platform - Database Migration Script           ║
║                                                                ║
║        Version: 1.0                                            ║
║        Date: 21 January 2026                                   ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

Write-Host "`nConfiguration:" -ForegroundColor Cyan
Write-Host "  Host: $PostgresHost" -ForegroundColor White
Write-Host "  Port: $PostgresPort" -ForegroundColor White
Write-Host "  User: $PostgresUser" -ForegroundColor White
Write-Host "  Dry Run: $DryRun" -ForegroundColor White
Write-Host "  Skip Backup: $SkipBackup" -ForegroundColor White

# Confirm execution
if (-not $Force -and -not $DryRun) {
    Write-Host "`n" -NoNewline
    $confirmation = Read-Host "This will create/recreate 8 databases. Continue? (yes/no)"
    if ($confirmation -ne "yes") {
        Write-Warning "Migration cancelled by user"
        exit 1
    }
}

# Test connection
if (-not (Test-PostgresConnection)) {
    Write-Error "Cannot proceed without database connection"
    exit 1
}

# Create backup
New-DatabaseBackup

# Execute migration files
Write-Step "Executing Migration Scripts"

$success = $true
$executed = 0

foreach ($file in $MigrationFiles) {
    $filePath = Join-Path $ScriptDir $file
    $description = $file -replace '^\d+_', '' -replace '_', ' ' -replace '\.sql$', ''
    
    if (Invoke-SqlFile -FilePath $filePath -Description $description) {
        $executed++
    } else {
        $success = $false
        Write-Error "Migration failed at: $file"
        
        if (-not $Force) {
            Write-Error "Stopping migration due to error"
            break
        }
    }
    
    Start-Sleep -Milliseconds 500
}

# Verify results
if ($success -and -not $DryRun) {
    Write-Step "Verification"
    
    $dbVerified = Test-DatabaseCreation
    $tablesVerified = Test-TableCreation
    
    if ($dbVerified -and $tablesVerified) {
        Write-Step "✓ MIGRATION COMPLETED SUCCESSFULLY"
        Show-Summary
        exit 0
    } else {
        Write-Step "⚠ MIGRATION COMPLETED WITH WARNINGS"
        Show-Summary
        exit 2
    }
} elseif ($DryRun) {
    Write-Step "✓ DRY RUN COMPLETED"
    Write-Info "All $executed migration files would be executed"
    exit 0
} else {
    Write-Step "✗ MIGRATION FAILED"
    Write-Error "Migration was not completed successfully"
    exit 1
}

# ============================================================================
# END OF SCRIPT
# ============================================================================
