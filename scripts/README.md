# Guide d'utilisation - Création d'utilisateur via Invoke-RestMethod

## 📋 Vue d'ensemble

Ce guide explique comment créer des utilisateurs via l'API user-service en utilisant PowerShell et `Invoke-RestMethod`.

## 🚀 Prérequis

1. **PowerShell 5.0+** (ou PowerShell Core)
2. **Service user-service en cours d'exécution** sur `http://localhost:8082`
3. **Accès réseau** au service

## 📝 Structure de la requête

### Endpoint
```
POST http://localhost:8082/api/users/register
```

### Headers
```
Content-Type: application/json
Accept: application/json
```

### Corps (JSON)
```json
{
  "phoneNumber": "+237612345678",
  "pin": "123456",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "country": "Cameroon",
  "city": "Douala",
  "region": "LITTORAL"
}
```

## 📋 Champs obligatoires et optionnels

| Champ | Type | Requis | Format | Description |
|-------|------|--------|--------|-------------|
| phoneNumber | string | ✓ | +237XXXXXXXX | Numéro de téléphone au Cameroun |
| pin | string | ✓ | 6 chiffres | Code PIN de 6 chiffres |
| email | string | ✓ | email valide | Adresse email |
| firstName | string | ✓ | max 100 chars | Prénom |
| lastName | string | ✓ | max 100 chars | Nom de famille |
| dateOfBirth | string | ✓ | yyyy-MM-dd | Date de naissance (doit être dans le passé) |
| country | string | ✓ | max 100 chars | Pays |
| city | string | ✗ | max 100 chars | Ville |
| region | string | ✗ | enum | Région du Cameroun |

### Régions valides pour le Cameroun
- LITTORAL
- CENTRE
- SOUTH_WEST
- NORTH_WEST
- NORTH
- EAST
- SOUTH
- WEST
- ADAMAOUA

## 🎯 Utilisation des scripts

### 1. Script complet (create-user.ps1)
Le script le plus complet avec gestion d'erreurs avancée et affichage détaillé.

```powershell
.\create-user.ps1
```

**Caractéristiques:**
- ✓ Gestion complète des erreurs
- ✓ Affichage détaillé de la requête et réponse
- ✓ Extraction des données importantes
- ✓ Couleurs pour meilleure lisibilité

### 2. Script simplifié (create-user-simple.ps1)
Version courte et rapide pour tester rapidement.

```powershell
.\create-user-simple.ps1
```

**Caractéristiques:**
- ✓ Code court et facile à copier-coller
- ✓ Parfait pour les tests rapides
- ✓ Moins de verbose

### 3. Script parametrisé (create-user-parametrized.ps1)
Permet de passer les paramètres en ligne de commande.

```powershell
# Utilisation simple
.\create-user-parametrized.ps1 `
    -PhoneNumber "+237612345678" `
    -Pin "123456" `
    -Email "john@example.com" `
    -FirstName "John" `
    -LastName "Doe" `
    -Country "Cameroon"

# Avec tous les paramètres optionnels
.\create-user-parametrized.ps1 `
    -PhoneNumber "+237612345678" `
    -Pin "123456" `
    -Email "john@example.com" `
    -FirstName "John" `
    -LastName "Doe" `
    -DateOfBirth "1990-05-15" `
    -Country "Cameroon" `
    -City "Douala" `
    -Region "LITTORAL" `
    -Verbose

# Utiliser un serveur différent
.\create-user-parametrized.ps1 `
    -PhoneNumber "+237612345678" `
    -Pin "123456" `
    -Email "john@example.com" `
    -FirstName "John" `
    -LastName "Doe" `
    -Country "Cameroon" `
    -BaseUrl "http://api.example.com:8082"
```

**Caractéristiques:**
- ✓ Paramètres flexibles
- ✓ Validation de paramètres
- ✓ Valeurs par défaut
- ✓ Option Verbose pour debug

### 4. Script de test multiple (create-multiple-users.ps1)
Crée plusieurs utilisateurs d'un coup pour tester.

```powershell
.\create-multiple-users.ps1
```

**Caractéristiques:**
- ✓ Crée 4 utilisateurs différents
- ✓ Résumé des résultats
- ✓ Gestion des erreurs par utilisateur
- ✓ Délai entre les requêtes

## 💻 Exemples d'utilisation en ligne de commande

### Exemple 1: Requête directe minimale
```powershell
$Body = @{
    phoneNumber = "+237612345678"
    pin = "123456"
    email = "test@example.com"
    firstName = "Test"
    lastName = "User"
    dateOfBirth = "1990-01-01"
    country = "Cameroon"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $Body
```

### Exemple 2: Avec gestion d'erreur
```powershell
try {
    $Body = @{
        phoneNumber = "+237612345678"
        pin = "123456"
        email = "test@example.com"
        firstName = "Test"
        lastName = "User"
        dateOfBirth = "1990-01-01"
        country = "Cameroon"
    } | ConvertTo-Json

    $Response = Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $Body

    Write-Host "✓ Utilisateur créé: $($Response.data.firstName) $($Response.data.lastName)"
    Write-Host "ID: $($Response.data.id)"
    Write-Host "Portefeuille: $($Response.data.walletNumber)"
}
catch {
    Write-Host "✗ Erreur: $_" -ForegroundColor Red
}
```

### Exemple 3: Boucle de création d'utilisateurs
```powershell
$Users = @(
    @{ firstName="Alice"; lastName="Johnson"; phone="+237611111111"; email="alice@example.com"; pin="111111" },
    @{ firstName="Bob"; lastName="Smith"; phone="+237622222222"; email="bob@example.com"; pin="222222" },
    @{ firstName="Carol"; lastName="Williams"; phone="+237633333333"; email="carol@example.com"; pin="333333" }
)

foreach ($User in $Users) {
    $Body = @{
        phoneNumber = $User.phone
        pin = $User.pin
        email = $User.email
        firstName = $User.firstName
        lastName = $User.lastName
        dateOfBirth = "1990-01-01"
        country = "Cameroon"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" `
            -Method POST `
            -Headers @{"Content-Type"="application/json"} `
            -Body $Body
        
        Write-Host "✓ $($User.firstName) $($User.lastName)" -ForegroundColor Green
    }
    catch {
        Write-Host "✗ $($User.firstName) $($User.lastName): $_" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 300
}
```

## 📊 Réponse d'API

### Réponse de succès (201 Created)
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

### Réponse d'erreur (400/422)
```json
{
  "success": false,
  "errors": [
    {
      "field": "phoneNumber",
      "message": "Invalid phone number format"
    },
    {
      "field": "pin",
      "message": "PIN must be exactly 6 digits"
    }
  ],
  "message": "Validation failed",
  "timestamp": "2025-12-20T22:00:00Z"
}
```

## ⚠️ Erreurs courantes

| Erreur | Cause | Solution |
|--------|-------|----------|
| `Connection refused` | Service non démarré | Démarrer le service: `mvn spring-boot:run` |
| `Invalid phone number format` | Format numéro incorrect | Utiliser: `+237` + 6-8 chiffres |
| `PIN must be exactly 6 digits` | PIN n'a pas 6 chiffres | Utiliser exactement 6 chiffres |
| `Invalid email format` | Email invalide | Utiliser un email valide |
| `Duplicate key value` | Utilisateur existe déjà | Utiliser un phone/email différent |

## 🔐 Points importants

1. **PIN**: Toujours 6 chiffres exactement
2. **Téléphone**: Format +237XXXXXXXX (Cameroun)
3. **Email**: Doit être unique
4. **Téléphone**: Doit être unique
5. **Date de naissance**: Doit être dans le passé

## 🐛 Debug

Pour obtenir plus de détails sur les erreurs:

```powershell
# Activer le verbose mode
$VerbosePreference = "Continue"

$Response = Invoke-RestMethod -Uri "http://localhost:8082/api/users/register" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $Body `
    -Verbose

# Ou capturer l'exception complète
try {
    $Response = Invoke-RestMethod -Uri "..." -Method POST ...
}
catch {
    Write-Host "Exception: $($_.Exception)" -ForegroundColor Red
    Write-Host "Response: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
}
```

## 📚 Ressources supplémentaires

- Documentation Invoke-RestMethod: `Get-Help Invoke-RestMethod -Full`
- Documentation ConvertTo-Json: `Get-Help ConvertTo-Json -Full`
- API Swagger: `http://localhost:8082/swagger-ui.html`
