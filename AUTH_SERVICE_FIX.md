# 🔧 Correction du problème 403 Forbidden sur /api/auth/register

## Problème
L'endpoint `/api/auth/register` retournait une erreur 403 Forbidden lors de l'appel REST.

## Corrections apportées

### 1. SecurityConfig.java
- ✅ Ajout de `.cors(cors -> cors.disable())` pour désactiver CORS
- ✅ Ajout de routes supplémentaires dans `permitAll()` :
  - `/register`, `/login`, `/refresh`, `/logout`, `/me` (routes racine)
  - `/error` (pour les erreurs)

### 2. JwtFilter.java
- ✅ Amélioration de la méthode `shouldNotFilter()` pour ignorer correctement :
  - Toutes les routes commençant par `/api/auth`
  - Routes racine : `/register`, `/login`, `/refresh`, `/logout`, `/me`
  - Route `/error`
  - Route `/` (racine)

## Test de l'endpoint

### Commande PowerShell
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" -Method Post -Body '{"email":"user@example.com","fullName":"John Doe","password":"secret","phoneNumber":"+221771234567"}' -ContentType "application/json"
```

### Commande cURL (alternative)
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","fullName":"John Doe","password":"secret","phoneNumber":"+221771234567"}'
```

## Vérifications

1. ✅ Le service auth-service doit être démarré sur le port 8081
2. ✅ La base de données PostgreSQL doit être accessible
3. ✅ Le service wallet-service doit être accessible (pour créer le wallet)

## Si le problème persiste

1. Vérifier que le service est bien démarré :
   ```powershell
   Get-Process | Where-Object {$_.ProcessName -like "*java*"}
   ```

2. Vérifier les logs du service pour voir les erreurs :
   - Chercher les logs Spring Boot dans la console
   - Vérifier les logs de sécurité (TRACE level activé)

3. Tester avec un client HTTP comme Postman ou Insomnia

4. Vérifier que la base de données est accessible :
   - URL: `jdbc:postgresql://192.168.0.122:5432/wallet_db`
   - User: `postgres`
   - Password: `1234`

## Notes

- Le champ `phoneNumber` est maintenant requis dans `RegisterRequest`
- Le service crée automatiquement un wallet via `wallet-service` lors de l'enregistrement
- Si `wallet-service` n'est pas disponible, l'enregistrement échouera silencieusement (log d'erreur)

