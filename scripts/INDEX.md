# 📦 Scripts de gestion des utilisateurs Zaphira

## 🎯 Vue d'ensemble rapide

Cet ensemble de scripts PowerShell permet de créer des utilisateurs via l'API Zaphira en utilisant `Invoke-RestMethod`.

## 📁 Fichiers disponibles

### 📖 Documentation
- **[README.md](README.md)** - Documentation complète avec exemples
- **[QUICK-REFERENCE.md](QUICK-REFERENCE.md)** - Aide-mémoire rapide à copier-coller

### 🛠️ Scripts PowerShell

#### 1. `create-user.ps1` - Script complet
**Utilité:** Script le plus complet avec gestion d'erreur avancée  
**Meilleur pour:** Production, debugging, logs détaillés

```powershell
.\create-user.ps1
```

**Caractéristiques:**
- ✓ Affichage détaillé de la requête
- ✓ Gestion complète des erreurs
- ✓ Extraction des informations importantes
- ✓ Couleurs pour meilleure lisibilité

---

#### 2. `create-user-simple.ps1` - Version minimale
**Utilité:** Version courte et directe  
**Meilleur pour:** Tests rapides, prototypage

```powershell
.\create-user-simple.ps1
```

**Caractéristiques:**
- ✓ Code simple et facile à comprendre
- ✓ Parfait pour copier-coller
- ✓ Moins de verbose

---

#### 3. `create-user-parametrized.ps1` - Version paramétrisée
**Utilité:** Flexible, permet les paramètres en ligne de commande  
**Meilleur pour:** Intégration dans d'autres scripts, automatisation

```powershell
# Usage simple
.\create-user-parametrized.ps1 `
    -PhoneNumber "+237612345678" `
    -Pin "123456" `
    -Email "test@example.com" `
    -FirstName "John" `
    -LastName "Doe" `
    -Country "Cameroon"

# Avec tous les paramètres
.\create-user-parametrized.ps1 `
    -PhoneNumber "+237612345678" `
    -Pin "123456" `
    -Email "test@example.com" `
    -FirstName "John" `
    -LastName "Doe" `
    -DateOfBirth "1990-05-15" `
    -Country "Cameroon" `
    -City "Douala" `
    -Region "LITTORAL" `
    -BaseUrl "http://localhost:8082" `
    -Verbose
```

**Caractéristiques:**
- ✓ Paramètres flexibles
- ✓ Validation de paramètres
- ✓ Valeurs par défaut
- ✓ Aide intégrée

---

#### 4. `create-multiple-users.ps1` - Batch de création
**Utilité:** Créer plusieurs utilisateurs d'un coup  
**Meilleur pour:** Tests, données de démonstration

```powershell
.\create-multiple-users.ps1
```

**Caractéristiques:**
- ✓ Crée 4 utilisateurs pré-définis
- ✓ Résumé des résultats
- ✓ Gestion d'erreur par utilisateur
- ✓ Délai entre les requêtes

---

#### 5. `Zaphira.psm1` - Module PowerShell
**Utilité:** Module réutilisable avec fonction New-ZaphiraUser  
**Meilleur pour:** Utilisation répétée dans votre session PowerShell

```powershell
# Charger le module
Import-Module .\Zaphira.psm1

# Utiliser la fonction
New-ZaphiraUser -PhoneNumber "+237612345678" -Pin "123456" `
    -Email "test@example.com" -FirstName "John" -LastName "Doe" -Country "Cameroon"

# Ou l'alias court
nzu -PhoneNumber "+237612345678" -Pin "123456" -Email "test@example.com" `
    -FirstName "John" -LastName "Doe" -Country "Cameroon"

# Obtenir l'aide complète
Get-Help New-ZaphiraUser -Full
```

**Caractéristiques:**
- ✓ Fonction réutilisable
- ✓ Validation complète des paramètres
- ✓ Gestion d'erreur avancée
- ✓ Alias court disponible

---

## 🚀 Démarrage rapide

### 1. Vérifier que le service est en cours d'exécution
```powershell
# Dans le dossier du projet
cd C:\Users\HP\Downloads\zaphira-15-12-2025
mvn spring-boot:run  # ou juste: mvn spring-boot:run -X
```

### 2. En PowerShell, naviguer vers le dossier scripts
```powershell
cd C:\Users\HP\Downloads\zaphira-15-12-2025\scripts
```

### 3. Exécuter un script
```powershell
# Le plus simple
.\create-user-simple.ps1

# Ou le plus complet
.\create-user.ps1

# Ou avec paramètres personnalisés
.\create-user-parametrized.ps1 -PhoneNumber "+237612345678" -Pin "123456" -Email "john@example.com" -FirstName "John" -LastName "Doe" -Country "Cameroon"
```

## 📊 Comparaison des scripts

| Feature | Simple | Complet | Paramétrisé | Multiple | Module |
|---------|--------|---------|-------------|----------|--------|
| Facile à utiliser | ✓✓✓ | ✓✓ | ✓✓ | ✓✓✓ | ✓✓ |
| Gestion d'erreur | ✓ | ✓✓✓ | ✓✓ | ✓✓ | ✓✓✓ |
| Paramètres flexibles | ✗ | ✗ | ✓✓✓ | ✗ | ✓✓✓ |
| Création multiple | ✗ | ✗ | Possible | ✓✓✓ | Possible |
| Réutilisable | Moyen | Moyen | Bon | Moyen | ✓✓✓ |
| Verbosité | Basse | Haute | Moyenne | Moyenne | Basse |

## 💡 Cas d'utilisation

### Je veux tester rapidement
→ Utilisez `create-user-simple.ps1`

### Je veux du debug détaillé
→ Utilisez `create-user.ps1`

### Je veux personnaliser les données
→ Utilisez `create-user-parametrized.ps1`

### Je veux créer 4 utilisateurs de test
→ Utilisez `create-multiple-users.ps1`

### Je veux une fonction réutilisable
→ Chargez `Zaphira.psm1` et utilisez `New-ZaphiraUser`

## 📝 Exemples rapides

### Créer un utilisateur
```powershell
.\create-user-parametrized.ps1 `
    -PhoneNumber "+237612345678" `
    -Pin "123456" `
    -Email "test@example.com" `
    -FirstName "Jean" `
    -LastName "Dupont" `
    -Country "Cameroon"
```

### Créer plusieurs utilisateurs en boucle
```powershell
$Numbers = @("11", "22", "33", "44", "55")
$Numbers | ForEach-Object {
    .\create-user-parametrized.ps1 `
        -PhoneNumber "+237612345$_" `
        -Pin "111111" `
        -Email "user$_@test.com" `
        -FirstName "User" `
        -LastName $_   `
        -Country "Cameroon"
    
    Start-Sleep -Seconds 1
}
```

### Stocker le résultat dans une variable
```powershell
$User = & .\create-user-parametrized.ps1 `
    -PhoneNumber "+237612345678" `
    -Pin "123456" `
    -Email "test@example.com" `
    -FirstName "John" `
    -LastName "Doe" `
    -Country "Cameroon"

Write-Host "ID créé: $($User.id)"
```

## 🔍 Dépannage

### Le script ne s'exécute pas
```powershell
# Vérifier la politique d'exécution
Get-ExecutionPolicy

# Autoriser l'exécution locale (si nécessaire)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### "Connection refused"
- Assurez-vous que le service user-service est démarré
- Vérifiez que le port 8082 est correct

### "Invalid phone number format"
- Utilisez le format: `+237XXXXXXXX` (Cameroun)
- Exemple: `+237612345678`

### "PIN must be exactly 6 digits"
- Le PIN doit contenir exactement 6 chiffres
- Exemple: `123456` ✓, `12345` ✗

### "Duplicate key value"
- Email ou téléphone existe déjà
- Utilisez des valeurs différentes

## 📚 Documentation détaillée

Pour plus d'informations détaillées, voir:
- [README.md](README.md) - Documentation complète
- [QUICK-REFERENCE.md](QUICK-REFERENCE.md) - Aide-mémoire

## 🎓 Apprentissage

### Points clés de Invoke-RestMethod
```powershell
Invoke-RestMethod `
    -Uri "http://..."              # URL de l'API
    -Method POST                    # Méthode HTTP
    -Headers @{"Content-Type"="..."} # Headers
    -Body $JsonData                 # Corps JSON
    -TimeoutSec 30                  # Délai
    -ErrorAction Stop               # Gestion d'erreur
```

### Conversion JSON
```powershell
# PowerShell vers JSON
$Object | ConvertTo-Json

# JSON vers PowerShell
$Json | ConvertFrom-Json
```

## 📞 Support

Pour l'aide sur un script spécifique:
```powershell
# Aide complète
Get-Help .\create-user-parametrized.ps1 -Full

# Syntaxe uniquement
Get-Help .\create-user-parametrized.ps1 -Syntax

# Exemples
Get-Help .\create-user-parametrized.ps1 -Examples
```

---

**Créé:** 20 décembre 2025  
**Version:** 1.0  
**Service cible:** Zaphira user-service (v1.0.0-SNAPSHOT)
