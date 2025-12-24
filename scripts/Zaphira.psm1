# Fonction PowerShell réutilisable pour créer des utilisateurs
# À importer dans votre profil PowerShell ou session

function New-ZaphiraUser {
    <#
    .SYNOPSIS
        Crée un nouvel utilisateur via l'API Zaphira user-service

    .DESCRIPTION
        Envoie une requête POST à l'API pour créer un nouvel utilisateur avec les paramètres fournis.
        Affiche un résumé des données créées.

    .PARAMETER PhoneNumber
        Numéro de téléphone au format +237XXXXXXXX (obligatoire)

    .PARAMETER Pin
        Code PIN de 6 chiffres (obligatoire)

    .PARAMETER Email
        Adresse email valide (obligatoire)

    .PARAMETER FirstName
        Prénom de l'utilisateur (obligatoire)

    .PARAMETER LastName
        Nom de famille de l'utilisateur (obligatoire)

    .PARAMETER Country
        Pays de l'utilisateur (obligatoire)

    .PARAMETER DateOfBirth
        Date de naissance au format yyyy-MM-dd (optionnel, par défaut: 25 ans avant aujourd'hui)

    .PARAMETER City
        Ville de résidence (optionnel)

    .PARAMETER Region
        Région du Cameroun (optionnel)
        Valeurs possibles: LITTORAL, CENTRE, SOUTH_WEST, NORTH_WEST, NORTH, EAST, SOUTH, WEST, ADAMAOUA

    .PARAMETER BaseUrl
        URL de base du service user-service (par défaut: http://localhost:8082)

    .PARAMETER TimeoutSec
        Délai d'attente en secondes (par défaut: 30)

    .EXAMPLE
        New-ZaphiraUser -PhoneNumber "+237612345678" -Pin "123456" `
            -Email "john@example.com" -FirstName "John" -LastName "Doe" `
            -Country "Cameroon" -City "Douala" -Region "LITTORAL"

    .EXAMPLE
        # Créer plusieurs utilisateurs
        $Users = @(
            @{PhoneNumber="+237611111111"; Pin="111111"; Email="user1@test.com"; FirstName="Alice"; LastName="Johnson"; Country="Cameroon"},
            @{PhoneNumber="+237622222222"; Pin="222222"; Email="user2@test.com"; FirstName="Bob"; LastName="Smith"; Country="Cameroon"}
        )
        
        foreach ($User in $Users) {
            New-ZaphiraUser @User
        }

    .EXAMPLE
        # Avec gestion d'erreur personnalisée
        if (New-ZaphiraUser -PhoneNumber "+237612345678" -Pin "123456" `
            -Email "test@example.com" -FirstName "Test" -LastName "User" -Country "Cameroon") {
            Write-Host "Succès!" -ForegroundColor Green
        } else {
            Write-Host "Échec!" -ForegroundColor Red
        }

    .OUTPUTS
        PSCustomObject contenant les données de l'utilisateur créé

    .NOTES
        - Le PIN doit contenir exactement 6 chiffres
        - Le numéro de téléphone doit être au format +237XXXXXXXX
        - L'email doit être unique dans le système
        - Le numéro de téléphone doit être unique dans le système
    #>

    [CmdletBinding()]
    param(
        [Parameter(Mandatory=$true)]
        [string]$PhoneNumber,
        
        [Parameter(Mandatory=$true)]
        [ValidateLength(6,6)]
        [ValidatePattern('^\d{6}$')]
        [string]$Pin,
        
        [Parameter(Mandatory=$true)]
        [string]$Email,
        
        [Parameter(Mandatory=$true)]
        [string]$FirstName,
        
        [Parameter(Mandatory=$true)]
        [string]$LastName,
        
        [Parameter(Mandatory=$true)]
        [string]$Country,
        
        [Parameter(Mandatory=$false)]
        [datetime]$DateOfBirth = (Get-Date).AddYears(-25),
        
        [Parameter(Mandatory=$false)]
        [string]$City,
        
        [Parameter(Mandatory=$false)]
        [ValidateSet('LITTORAL', 'CENTRE', 'SOUTH_WEST', 'NORTH_WEST', 'NORTH', 'EAST', 'SOUTH', 'WEST', 'ADAMAOUA')]
        [string]$Region,
        
        [Parameter(Mandatory=$false)]
        [string]$BaseUrl = "http://localhost:8082",
        
        [Parameter(Mandatory=$false)]
        [int]$TimeoutSec = 30
    )

    $Endpoint = "/api/users/register"
    $Uri = "$BaseUrl$Endpoint"

    # Construire le corps de la requête
    $Body = @{
        phoneNumber = $PhoneNumber
        pin = $Pin
        email = $Email
        firstName = $FirstName
        lastName = $LastName
        dateOfBirth = $DateOfBirth.ToString("yyyy-MM-dd")
        country = $Country
    }

    # Ajouter les paramètres optionnels
    if ($City) { $Body.city = $City }
    if ($Region) { $Body.region = $Region }

    $JsonBody = $Body | ConvertTo-Json

    try {
        Write-Verbose "Envoi de la requête vers: $Uri"
        Write-Verbose "Corps: $JsonBody"
        
        $Response = Invoke-RestMethod -Uri $Uri `
            -Method POST `
            -Headers @{"Content-Type"="application/json"} `
            -Body $JsonBody `
            -TimeoutSec $TimeoutSec `
            -ErrorAction Stop

        Write-Verbose "Réponse reçue: $($Response | ConvertTo-Json)"
        
        # Afficher les informations
        Write-Host "✓ Utilisateur créé avec succès!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Détails:" -ForegroundColor Cyan
        Write-Host "  Nom complet: $($Response.data.firstName) $($Response.data.lastName)" -ForegroundColor White
        Write-Host "  Email: $($Response.data.email)" -ForegroundColor White
        Write-Host "  Téléphone: $($Response.data.phoneNumber)" -ForegroundColor White
        Write-Host "  ID: $($Response.data.id)" -ForegroundColor White
        Write-Host "  Portefeuille: $($Response.data.walletNumber)" -ForegroundColor White
        Write-Host "  Statut: $($Response.data.accountStatus)" -ForegroundColor White
        Write-Host ""
        
        return $Response.data
    }
    catch {
        Write-Host "✗ Erreur lors de la création d'utilisateur" -ForegroundColor Red
        Write-Host "URL: $Uri" -ForegroundColor Gray
        Write-Host "Erreur: $($_.Exception.Message)" -ForegroundColor Red
        
        # Essayer d'extraire plus de détails
        if ($_.Exception.Response) {
            try {
                $Stream = $_.Exception.Response.GetResponseStream()
                $Reader = New-Object System.IO.StreamReader($Stream)
                $ErrorBody = $Reader.ReadToEnd()
                $ErrorJson = $ErrorBody | ConvertFrom-Json
                
                if ($ErrorJson.errors) {
                    Write-Host ""
                    Write-Host "Erreurs de validation:" -ForegroundColor Yellow
                    foreach ($Error in $ErrorJson.errors) {
                        Write-Host "  - $($Error.field): $($Error.message)" -ForegroundColor Yellow
                    }
                }
            }
            catch {
                Write-Verbose "Impossible de parser la réponse d'erreur"
            }
        }
        
        return $null
    }
}

# Alias pour raccourcir
Set-Alias -Name nzu -Value New-ZaphiraUser

# Export des commandes
Export-ModuleMember -Function New-ZaphiraUser -Alias nzu

# Message d'aide
Write-Host "Fonction New-ZaphiraUser chargée!" -ForegroundColor Green
Write-Host "Utilisez: New-ZaphiraUser -PhoneNumber ... -Pin ... -Email ... -FirstName ... -LastName ... -Country ..." -ForegroundColor Cyan
Write-Host "Ou l'alias court: nzu -PhoneNumber ... -Pin ... -Email ... -FirstName ... -LastName ... -Country ..." -ForegroundColor Cyan
Write-Host ""
Write-Host "Obtenez de l'aide: Get-Help New-ZaphiraUser -Full" -ForegroundColor Cyan
