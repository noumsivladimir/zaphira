# Script PowerShell pour créer un utilisateur via l'API user-service
# Utilise Invoke-RestMethod pour une requête HTTP POST

# Configuration de base
$BaseUrl = "http://localhost:8082"
$Endpoint = "/api/users/register"
$Url = "$BaseUrl$Endpoint"

# Headers
$Headers = @{
    "Content-Type" = "application/json"
    "Accept" = "application/json"
}

# Corps de la requête - UserRegistrationRequest
$Body = @{
    phoneNumber = "+237612345678"           # Format: +237 + 6 digits phone number
    pin = "123456"                          # 6 chiffres obligatoires
    email = "john.doe@example.com"          # Email valide
    firstName = "John"                      # Prénom obligatoire
    lastName = "Doe"                        # Nom de famille obligatoire
    dateOfBirth = "1990-05-15"             # Date de naissance au format yyyy-MM-dd
    country = "Cameroon"                    # Pays obligatoire
    city = "Douala"                         # Ville (optionnel)
    region = "LITTORAL"                     # Région du Cameroun (optionnel)
} | ConvertTo-Json

# Afficher les détails de la requête
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Création d'un nouvel utilisateur" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "URL: " -ForegroundColor Yellow -NoNewline
Write-Host $Url
Write-Host "Méthode: " -ForegroundColor Yellow -NoNewline
Write-Host "POST"
Write-Host "Headers: " -ForegroundColor Yellow
$Headers.GetEnumerator() | ForEach-Object { Write-Host "  $($_.Key): $($_.Value)" }
Write-Host ""
Write-Host "Corps de la requête:" -ForegroundColor Yellow
Write-Host $Body
Write-Host ""

# Effectuer la requête
try {
    Write-Host "Envoi de la requête..." -ForegroundColor Green
    
    $Response = Invoke-RestMethod `
        -Uri $Url `
        -Method POST `
        -Headers $Headers `
        -Body $Body `
        -TimeoutSec 30
    
    # Afficher la réponse
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✓ Réponse de succès (201 Created)" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host ($Response | ConvertTo-Json -Depth 10) -ForegroundColor Green
    
    # Extraire et afficher les informations importantes
    if ($Response.data) {
        Write-Host ""
        Write-Host "Informations de l'utilisateur créé:" -ForegroundColor Cyan
        Write-Host "  ID utilisateur: " -ForegroundColor Yellow -NoNewline
        Write-Host $Response.data.id
        Write-Host "  Numéro de portefeuille: " -ForegroundColor Yellow -NoNewline
        Write-Host $Response.data.walletNumber
        Write-Host "  Nom: " -ForegroundColor Yellow -NoNewline
        Write-Host "$($Response.data.firstName) $($Response.data.lastName)"
        Write-Host "  Email: " -ForegroundColor Yellow -NoNewline
        Write-Host $Response.data.email
        Write-Host "  Téléphone: " -ForegroundColor Yellow -NoNewline
        Write-Host $Response.data.phoneNumber
    }
    
}
catch {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "✗ Erreur lors de la requête" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    
    # Afficher les détails de l'erreur
    $ErrorResponse = $_.Exception.Response
    $StatusCode = [int]$ErrorResponse.StatusCode
    
    Write-Host "Code d'erreur: " -ForegroundColor Yellow -NoNewline
    Write-Host $StatusCode
    
    # Lire le contenu de l'erreur
    try {
        $Stream = $ErrorResponse.GetResponseStream()
        $Reader = New-Object System.IO.StreamReader($Stream)
        $ErrorBody = $Reader.ReadToEnd()
        
        Write-Host "Détails de l'erreur:" -ForegroundColor Yellow
        Write-Host ($ErrorBody | ConvertFrom-Json | ConvertTo-Json -Depth 10) -ForegroundColor Red
    }
    catch {
        Write-Host "Impossible de lire les détails de l'erreur" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Requête terminée" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
