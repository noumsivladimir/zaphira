# OTP Transaction Authorization Request Guide

## Configuration Simplifiée

La validation de transaction a été simplifiée pour utiliser **3 paramètres obligatoires**:
- `method` - Méthode d'autorisation (OTP)
- `phoneNumber` - Numéro de téléphone pour vérification OTP
- `otpCode` - Code OTP à 6 chiffres

## Structure DTO Mise à Jour

### AuthorizationValidationRequest.java

```java
@Getter
@Setter
@Builder
public class AuthorizationValidationRequest {
    
    @NotNull(message = "Authorization method is required")
    @JsonProperty("method")
    private AuthorizationMethod method;  // OTP, BIOMETRIC, PASSWORD
    
    @NotBlank(message = "Phone number is required for authorization")
    @JsonProperty("phoneNumber")
    private String phoneNumber;          // Format: +33612345678 or 0612345678
    
    @NotBlank(message = "OTP code is required for transaction authorization")
    @JsonProperty("otpCode")
    private String otpCode;              // 6 digits
}
```

## Requête PowerShell Correcte

### 1. Avec JSON en variable

```powershell
$transactionId = "123"
$method = "OTP"
$phoneNumber = "+33612345678"
$otpCode = "654321"
$jwtToken = "YOUR_JWT_TOKEN_HERE"

$body = @{
    method = $method
    phoneNumber = $phoneNumber
    otpCode = $otpCode
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/transactions/$transactionId/authorize" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body `
    -Headers @{"Authorization" = "Bearer $jwtToken"}

$response | ConvertTo-Json -Depth 10
```

### 2. Requête inline simple

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/transactions/123/authorize" `
    -Method Post `
    -ContentType "application/json" `
    -Body (@{
        method = "OTP"
        phoneNumber = "+33612345678"
        otpCode = "654321"
    } | ConvertTo-Json) `
    -Headers @{"Authorization" = "Bearer YOUR_JWT_TOKEN"}
```

### 3. Avec gestion d'erreur complète

```powershell
try {
    $response = Invoke-RestMethod `
        -Uri "http://localhost:8080/api/transactions/123/authorize" `
        -Method Post `
        -ContentType "application/json" `
        -Body (@{
            method = "OTP"
            phoneNumber = "+33612345678"
            otpCode = "654321"
        } | ConvertTo-Json) `
        -Headers @{"Authorization" = "Bearer YOUR_JWT_TOKEN"}
    
    Write-Host "✅ Authorization successful"
    Write-Host "Transaction Status: $($response.data.status)"
    Write-Host "Authorized At: $($response.data.authorizedAt)"
}
catch {
    Write-Host "❌ Authorization failed"
    Write-Host "Error: $($_.Exception.Message)"
    
    # Try to extract error details from response
    try {
        $errorResponse = $_.Exception.Response.Content | ConvertFrom-Json
        Write-Host "Error Details: $($errorResponse.message)"
    }
    catch {
        Write-Host "Raw Response: $($_.Exception.Response)"
    }
}
```

## Flux de Validation OTP

1. **Vérification du code OTP**
   - Vérification de l'existence du code dans `otp_tokens`
   - Vérification que le code n'est pas expiré
   - Vérification que le code n'a pas déjà été utilisé
   - Vérification du nombre de tentatives (max 3)

2. **Enregistrement Audit**
   - Numéro de téléphone enregistré dans `transactions.phone_number_used_for_auth`
   - Méthode d'autorisation enregistrée dans `transactions.actual_authorization_method`
   - Timestamp d'autorisation automatique

3. **Changement de statut**
   - Statut change de `INITIATED` → `AUTHORIZED`
   - Exécution immédiate du transfert wallet

4. **Exécution de la transaction**
   - Appel au WalletClient pour exécuter le transfert
   - Enregistrement du résultat

## Gestion des Erreurs

### Erreur: OTP non trouvé
```
{
  "success": false,
  "message": "OTP code not found or has been used",
  "errorCode": "OTP_NOT_FOUND"
}
```

### Erreur: OTP expiré
```
{
  "success": false,
  "message": "OTP has expired",
  "errorCode": "OTP_EXPIRED"
}
```

### Erreur: Trop de tentatives
```
{
  "success": false,
  "message": "Maximum OTP verification attempts exceeded",
  "errorCode": "OTP_MAX_ATTEMPTS_EXCEEDED"
}
```

### Erreur: Transaction n'existe pas
```
{
  "success": false,
  "message": "Transaction not found",
  "errorCode": "TRANSACTION_NOT_FOUND"
}
```

### Erreur: Autorisation non requise
```
{
  "success": false,
  "message": "Transaction does not require authorization",
  "errorCode": "AUTHORIZATION_NOT_REQUIRED"
}
```

## Réponse Réussie

```json
{
  "success": true,
  "message": "Transaction authorized successfully",
  "data": {
    "id": 123,
    "reference": "TXN-2024-12-13-ABC123",
    "status": "AUTHORIZED",
    "authorizedAt": "2024-12-13T10:30:45Z",
    "phoneNumberUsedForAuth": "+33612345678",
    "actualAuthorizationMethod": "OTP",
    "amount": "1000.00",
    "currency": "XOF"
  }
}
```

## Schema Base de Données Mis à Jour

### Table: transactions (colonnes ajoutées)

```sql
-- Colonne pour tracker le numéro de téléphone utilisé pour l'OTP
ALTER TABLE transactions ADD COLUMN phone_number_used_for_auth VARCHAR(20);

-- Colonne pour tracker la méthode d'autorisation réelle utilisée
ALTER TABLE transactions ADD COLUMN actual_authorization_method VARCHAR(50);

-- Indexes pour performance
CREATE INDEX idx_transactions_phone_auth ON transactions(phone_number_used_for_auth);
CREATE INDEX idx_transactions_auth_method ON transactions(actual_authorization_method);
```

### Table: otp_tokens (existante)

```sql
CREATE TABLE otp_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(6) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    context TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_phone_code (phone_number, code),
    INDEX idx_phone_purpose (phone_number, purpose),
    INDEX idx_expires (expires_at)
);
```

## Étapes de Test

### 1. Créer une transaction
```powershell
# Obtenir un JWT token d'abord
$transactionBody = @{
    senderWalletNumber = "WALLET-001"
    receiverWalletNumber = "WALLET-002"
    amount = 1000.00
    currency = "XOF"
    type = "TRANSFER"
} | ConvertTo-Json

$tx = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/transactions" `
    -Method Post `
    -ContentType "application/json" `
    -Body $transactionBody `
    -Headers @{"Authorization" = "Bearer $jwtToken"}

$txId = $tx.data.id
Write-Host "Transaction créée avec ID: $txId"
```

### 2. Générer et envoyer un OTP
```powershell
# À effectuer via un endpoint OTP (GET /api/otp/generate ou similaire)
# Supposons que l'OTP reçu par SMS est: 654321
```

### 3. Autoriser la transaction avec OTP
```powershell
$authBody = @{
    method = "OTP"
    phoneNumber = "+33612345678"
    otpCode = "654321"
} | ConvertTo-Json

$authResponse = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/transactions/$txId/authorize" `
    -Method Post `
    -ContentType "application/json" `
    -Body $authBody `
    -Headers @{"Authorization" = "Bearer $jwtToken"}

Write-Host "Transaction authorized: $($authResponse.data.status)"
```

## Configuration Propriétés

```properties
# transaction.otp.max-attempts=3
# transaction.otp.purpose=TRANSACTION_AUTH
# transaction.otp.expiry-minutes=5
```

## Remarques Importantes

1. ✅ **Validation obligatoire**: `method`, `phoneNumber`, `otpCode`
2. ✅ **Pas de paramètres legacy**: `code`, `authorizedBy` ont été supprimés
3. ✅ **Audit trail**: Numéro téléphone et méthode enregistrés dans la transaction
4. ✅ **Sécurité**: Masquage du numéro dans les réponses API
5. ✅ **Compatibilité DB**: Migration de schéma nécessaire pour les 2 colonnes ajoutées

## Commandes Migration Database

```sql
-- PostgreSQL
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS phone_number_used_for_auth VARCHAR(20);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS actual_authorization_method VARCHAR(50);

-- MySQL
ALTER TABLE transactions 
ADD COLUMN phone_number_used_for_auth VARCHAR(20) AFTER authorized_at,
ADD COLUMN actual_authorization_method VARCHAR(50) AFTER phone_number_used_for_auth;

-- H2/Test
ALTER TABLE transactions ADD COLUMN phone_number_used_for_auth VARCHAR(20);
ALTER TABLE transactions ADD COLUMN actual_authorization_method VARCHAR(50);
```
