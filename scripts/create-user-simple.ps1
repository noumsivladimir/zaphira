# Script PowerShell simplifié - Version courte pour création d'utilisateur
# Copier-coller facile pour tester rapidement

# Configuration
$Url = "http://localhost:8082/api/users/register"

# Données utilisateur
$UserData = @{
    phoneNumber = "+237612345678"
    pin = "123456"
    email = "john.doe@example.com"
    firstName = "John"
    lastName = "Doe"
    dateOfBirth = "1990-05-15"
    country = "Cameroon"
    city = "Douala"
    region = "LITTORAL"
} | ConvertTo-Json

# Requête
$Response = Invoke-RestMethod -Uri $Url -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $UserData

# Afficher le résultat
$Response | ConvertTo-Json -Depth 10

# Accès aux données
Write-Host "Utilisateur créé: $($Response.data.firstName) $($Response.data.lastName)"
Write-Host "ID: $($Response.data.id)"
Write-Host "Portefeuille: $($Response.data.walletNumber)"
