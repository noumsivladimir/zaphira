# Script de test - Crée plusieurs utilisateurs pour des scénarios différents

$BaseUrl = "http://localhost:8082"
$Endpoint = "/api/users/register"
$Url = "$BaseUrl$Endpoint"

# Liste d'utilisateurs à créer
$Users = @(
    @{
        phoneNumber = "+237611111111"
        pin = "111111"
        email = "user1@example.com"
        firstName = "Alice"
        lastName = "Johnson"
        dateOfBirth = "1995-03-10"
        country = "Cameroon"
        city = "Yaoundé"
        region = "CENTRE"
    },
    @{
        phoneNumber = "+237622222222"
        pin = "222222"
        email = "user2@example.com"
        firstName = "Bob"
        lastName = "Smith"
        dateOfBirth = "1988-07-20"
        country = "Cameroon"
        city = "Douala"
        region = "LITTORAL"
    },
    @{
        phoneNumber = "+237633333333"
        pin = "333333"
        email = "user3@example.com"
        firstName = "Carol"
        lastName = "Williams"
        dateOfBirth = "1992-12-15"
        country = "Cameroon"
        city = "Buea"
        region = "SOUTH_WEST"
    },
    @{
        phoneNumber = "+237644444444"
        pin = "444444"
        email = "user4@example.com"
        firstName = "David"
        lastName = "Brown"
        dateOfBirth = "1990-06-25"
        country = "Cameroon"
        city = "Bamenda"
        region = "NORTH_WEST"
    }
)

# Tableau pour stocker les résultats
$Results = @()

Write-Host "================================" -ForegroundColor Cyan
Write-Host "Création de 4 utilisateurs test" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$Count = 1

# Créer chaque utilisateur
foreach ($User in $Users) {
    Write-Host "[$Count/$($Users.Count)] Création de $($User.firstName) $($User.lastName)..." -ForegroundColor Yellow
    
    try {
        $JsonBody = $User | ConvertTo-Json
        
        $Response = Invoke-RestMethod -Uri $Url -Method POST `
            -Headers @{"Content-Type"="application/json"} `
            -Body $JsonBody
        
        Write-Host "  ✓ Succès!" -ForegroundColor Green
        Write-Host "    - ID: $($Response.data.id)" -ForegroundColor Gray
        Write-Host "    - Portefeuille: $($Response.data.walletNumber)" -ForegroundColor Gray
        
        $Results += @{
            Success = $true
            User = $User.firstName
            ID = $Response.data.id
            Wallet = $Response.data.walletNumber
            Email = $Response.data.email
        }
    }
    catch {
        Write-Host "  ✗ Erreur: $_" -ForegroundColor Red
        
        $Results += @{
            Success = $false
            User = $User.firstName
            Error = $_.Exception.Message
        }
    }
    
    Write-Host ""
    $Count++
    
    # Petit délai entre les requêtes
    Start-Sleep -Milliseconds 500
}

# Afficher le résumé
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Résumé des opérations" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$SuccessCount = ($Results | Where-Object { $_.Success }).Count
$FailureCount = ($Results | Where-Object { -not $_.Success }).Count

Write-Host "Total créé: " -ForegroundColor Yellow -NoNewline
Write-Host $SuccessCount -ForegroundColor Green
Write-Host "Total échoué: " -ForegroundColor Yellow -NoNewline
Write-Host $FailureCount -ForegroundColor Red
Write-Host ""

if ($SuccessCount -gt 0) {
    Write-Host "Utilisateurs créés avec succès:" -ForegroundColor Green
    $Results | Where-Object { $_.Success } | ForEach-Object {
        Write-Host "  - $($_.User) (ID: $($_.ID), Wallet: $($_.Wallet))" -ForegroundColor Green
    }
}

if ($FailureCount -gt 0) {
    Write-Host ""
    Write-Host "Utilisateurs échoués:" -ForegroundColor Red
    $Results | Where-Object { -not $_.Success } | ForEach-Object {
        Write-Host "  - $($_.User): $($_.Error)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
