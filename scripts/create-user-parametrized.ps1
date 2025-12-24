# Script PowerShell avec paramètres - Création flexible d'utilisateur
param(
    [Parameter(Mandatory=$true)]
    [string]$PhoneNumber,
    
    [Parameter(Mandatory=$true)]
    [ValidateLength(6,6)]
    [string]$Pin,
    
    [Parameter(Mandatory=$true)]
    [string]$Email,
    
    [Parameter(Mandatory=$true)]
    [string]$FirstName,
    
    [Parameter(Mandatory=$true)]
    [string]$LastName,
    
    [Parameter(Mandatory=$false)]
    [datetime]$DateOfBirth = (Get-Date).AddYears(-25),
    
    [Parameter(Mandatory=$true)]
    [string]$Country,
    
    [Parameter(Mandatory=$false)]
    [string]$City,
    
    [Parameter(Mandatory=$false)]
    [string]$Region,
    
    [Parameter(Mandatory=$false)]
    [string]$BaseUrl = "http://localhost:8082",
    
    [Parameter(Mandatory=$false)]
    [switch]$Verbose
)

# URL de l'API
$Endpoint = "/api/users/register"
$Url = "$BaseUrl$Endpoint"

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

# Ajouter les paramètres optionnels s'ils sont fournis
if ($City) { $Body.city = $City }
if ($Region) { $Body.region = $Region }

# Convertir en JSON
$JsonBody = $Body | ConvertTo-Json

if ($Verbose) {
    Write-Host "URL: $Url" -ForegroundColor Cyan
    Write-Host "Corps: $JsonBody" -ForegroundColor Cyan
}

# Envoyer la requête
try {
    $Response = Invoke-RestMethod -Uri $Url -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $JsonBody -ErrorAction Stop
    
    Write-Host "✓ Utilisateur créé avec succès!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Détails:" -ForegroundColor Cyan
    Write-Host "  Nom: $($Response.data.firstName) $($Response.data.lastName)" -ForegroundColor White
    Write-Host "  ID: $($Response.data.id)" -ForegroundColor White
    Write-Host "  Email: $($Response.data.email)" -ForegroundColor White
    Write-Host "  Téléphone: $($Response.data.phoneNumber)" -ForegroundColor White
    Write-Host "  Portefeuille: $($Response.data.walletNumber)" -ForegroundColor White
    Write-Host ""
    
    return $Response
}
catch {
    Write-Host "✗ Erreur: $_" -ForegroundColor Red
    exit 1
}

<#
.SYNOPSIS
    Crée un nouvel utilisateur via l'API user-service

.DESCRIPTION
    Ce script crée un nouvel utilisateur en envoyant une requête POST à l'API user-service.

.EXAMPLE
    .\create-user-parametrized.ps1 -PhoneNumber "+237612345678" -Pin "123456" `
        -Email "john@example.com" -FirstName "John" -LastName "Doe" `
        -Country "Cameroon" -City "Douala" -Region "LITTORAL"

.EXAMPLE
    .\create-user-parametrized.ps1 -PhoneNumber "+237612345678" -Pin "123456" `
        -Email "jane@example.com" -FirstName "Jane" -LastName "Smith" `
        -Country "Cameroon" -BaseUrl "http://api.example.com:8082" -Verbose

.NOTES
    - Le PIN doit contenir exactement 6 chiffres
    - Le numéro de téléphone doit être au format +237XXXXXXXX
    - L'email doit être au format valide
    - La date de naissance est optionnelle (par défaut: 25 ans ago)
#>
