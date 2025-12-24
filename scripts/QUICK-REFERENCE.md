# ⚡ AIDE-MÉMOIRE - Création d'utilisateur avec Invoke-RestMethod

## 🔥 Une ligne pour copier-coller

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phoneNumber="+237612345678";pin="123456";email="test@example.com";firstName="John";lastName="Doe";dateOfBirth="1990-01-01";country="Cameroon"} | ConvertTo-Json)
```

## 📋 Formule de base (copier-coller)

```powershell
# 1. Définir les données
$User = @{
    phoneNumber = "+237612345678"
    pin = "123456"
    email = "test@example.com"
    firstName = "John"
    lastName = "Doe"
    dateOfBirth = "1990-01-01"
    country = "Cameroon"
}

# 2. Envoyer la requête
$Response = Invoke-RestMethod `
    -Uri "http://localhost:8082/api/users/register" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body ($User | ConvertTo-Json)

# 3. Afficher le résultat
$Response.data
```

## 📚 Scripts disponibles

| Script | Utilité | Commande |
|--------|---------|----------|
| `create-user.ps1` | Complet avec gestion d'erreur | `.\create-user.ps1` |
| `create-user-simple.ps1` | Version minimaliste | `.\create-user-simple.ps1` |
| `create-user-parametrized.ps1` | Avec paramètres | `.\create-user-parametrized.ps1 -PhoneNumber ... -Pin ... -Email ... -FirstName ... -LastName ... -Country ...` |
| `create-multiple-users.ps1` | Créer 4 utilisateurs | `.\create-multiple-users.ps1` |
| `Zaphira.psm1` | Module PowerShell réutilisable | `Import-Module .\Zaphira.psm1; New-ZaphiraUser ...` |

## 🎯 Champs obligatoires

```
phoneNumber: +237XXXXXXXX    (Format Cameroun)
pin: XXXXXX                  (6 chiffres)
email: user@domain.com       (Email valide, unique)
firstName: John              (Texte)
lastName: Doe                (Texte)
dateOfBirth: YYYY-MM-DD     (Avant aujourd'hui)
country: Cameroon            (Texte)
```

## 🛠️ Exemples rapides

### Créer un utilisateur simple
```powershell
$Body = @{
    phoneNumber = "+237611111111"
    pin = "111111"
    email = "alice@test.com"
    firstName = "Alice"
    lastName = "Johnson"
    dateOfBirth = "1995-05-10"
    country = "Cameroon"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" -Method POST `
    -Headers @{"Content-Type"="application/json"} -Body $Body
```

### Avec gestion d'erreur basique
```powershell
try {
    $Response = Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body (@{ phoneNumber="+237612345678"; pin="123456"; email="test@test.com"; firstName="Test"; lastName="User"; dateOfBirth="1990-01-01"; country="Cameroon" } | ConvertTo-Json)
    
    Write-Host "✓ Créé: $($Response.data.firstName) (ID: $($Response.data.id))" -ForegroundColor Green
}
catch {
    Write-Host "✗ Erreur: $_" -ForegroundColor Red
}
```

### Créer plusieurs utilisateurs en boucle
```powershell
@(
    @{phone="+237611111111"; pin="111111"; email="u1@test.com"; first="Alice"; last="Johnson"},
    @{phone="+237622222222"; pin="222222"; email="u2@test.com"; first="Bob"; last="Smith"},
    @{phone="+237633333333"; pin="333333"; email="u3@test.com"; first="Carol"; last="Williams"}
) | ForEach-Object {
    $Body = @{
        phoneNumber = $_.phone
        pin = $_.pin
        email = $_.email
        firstName = $_.first
        lastName = $_.last
        dateOfBirth = "1990-01-01"
        country = "Cameroon"
    } | ConvertTo-Json
    
    Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" -Method POST `
        -Headers @{"Content-Type"="application/json"} -Body $Body | ForEach-Object {
        Write-Host "✓ $($_.data.firstName)" -ForegroundColor Green
    }
    
    Start-Sleep -Milliseconds 300
}
```

## 🔍 Vérifier la réponse

```powershell
# Stocker la réponse
$Response = Invoke-RestMethod ...

# Accéder aux données
$Response.data.id                  # ID utilisateur
$Response.data.firstName           # Prénom
$Response.data.lastName            # Nom
$Response.data.email               # Email
$Response.data.phoneNumber         # Téléphone
$Response.data.walletNumber        # Numéro portefeuille
$Response.data.accountStatus       # Statut du compte
$Response.success                  # Succès (true/false)
$Response.message                  # Message
```

## ⚠️ Erreurs courantes

```powershell
# "Connection refused" → Démarrer le service
mvn spring-boot:run

# "Invalid phone number format" → Utiliser +237XXXXXXXX
phoneNumber = "+237612345678"  # ✓ Bon
phoneNumber = "0612345678"     # ✗ Mauvais

# "PIN must be exactly 6 digits" → 6 chiffres exactement
pin = "123456"  # ✓ Bon (6)
pin = "12345"   # ✗ Mauvais (5)
pin = "1234567" # ✗ Mauvais (7)

# "Duplicate key value" → Email ou téléphone existe déjà
# Solution: Utiliser un email/téléphone différent

# "Invalid email format" → Email invalide
email = "test@example.com"  # ✓ Bon
email = "test@"             # ✗ Mauvais
email = "test"              # ✗ Mauvais
```

## 📊 Structure de la réponse réussie

```json
{
  "success": true,
  "data": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+237612345678",
    "walletNumber": "WAL-1234567890",
    "accountStatus": "ACTIVE",
    "createdAt": "2025-12-20T22:00:00Z"
  },
  "message": "User registered successfully",
  "timestamp": "2025-12-20T22:00:00Z"
}
```

## 🚀 Module PowerShell (Zaphira.psm1)

Une fois chargé, utiliser simplement:

```powershell
# Charger le module
Import-Module .\Zaphira.psm1

# Créer un utilisateur
New-ZaphiraUser -PhoneNumber "+237612345678" -Pin "123456" `
    -Email "john@example.com" -FirstName "John" -LastName "Doe" -Country "Cameroon"

# Ou avec l'alias court
nzu -PhoneNumber "+237612345678" -Pin "123456" -Email "john@example.com" `
    -FirstName "John" -LastName "Doe" -Country "Cameroon"

# Avec aide
Get-Help New-ZaphiraUser -Full
```

## 💡 Tips utiles

```powershell
# Récupérer juste l'ID créé
$ID = (Invoke-RestMethod ...).data.id

# Vérifier si succès
if ((Invoke-RestMethod ...).success) { ... }

# Utiliser une variable pour l'URL
$Url = "http://localhost:8082/api/users/register"
Invoke-RestMethod -Uri $Url -Method POST ...

# Mesurer le temps
Measure-Command { Invoke-RestMethod ... }

# Ajouter un délai entre les requêtes
Start-Sleep -Seconds 1

# Exporter en CSV
Invoke-RestMethod ... | Select-Object @{n='ID';e={$_.data.id}}, @{n='Nom';e={"$($_.data.firstName) $($_.data.lastName)"}} | Export-Csv users.csv
```
