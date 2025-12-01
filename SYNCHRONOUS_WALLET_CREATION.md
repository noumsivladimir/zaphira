# 🔄 Implémentation Synchrone : Création Automatique de Wallet

## 📋 Vue d'Ensemble

Implémentation complète de la création automatique de wallet lors de l'inscription d'un utilisateur, utilisant **FeignClient** pour une communication REST synchrone entre `auth-service` et `wallet-service`.

---

## ✅ Fichiers Modifiés/Créés

### 1. **WalletServiceClient.java** (FeignClient)
### 2. **UserService.java** (Service avec création wallet)
### 3. **AuthController.java** (Controller retournant user + wallet)
### 4. **RegisterResponse.java** (DTO de réponse enrichi)

---

## 📝 Code Complet

### 1. WalletServiceClient.java

```java
package com.zaphira.auth.client;

import com.zaphira.common.dto.WalletDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * FeignClient pour communiquer avec wallet-service.
 * Utilisé pour créer et récupérer des wallets de manière synchrone.
 */
@FeignClient(name = "wallet-service")
public interface WalletServiceClient {
    
    /**
     * Crée un nouveau wallet pour un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @param currency La devise (par défaut XOF)
     * @return WalletDTO créé
     */
    @PostMapping("/api/wallets")
    WalletDTO createWallet(@RequestParam Long userId, 
                          @RequestParam(defaultValue = "XOF") String currency);
    
    /**
     * Récupère le wallet d'un utilisateur par son ID.
     * 
     * @param userId L'ID de l'utilisateur
     * @return WalletDTO
     */
    @GetMapping("/api/wallets/user/{userId}")
    WalletDTO getWalletByUserId(@PathVariable Long userId);
}
```

### 2. UserService.java

```java
package com.zaphira.auth.service;

import com.zaphira.auth.client.WalletServiceClient;
import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.model.Role;
import com.zaphira.auth.model.User;
import com.zaphira.common.dto.WalletDTO;
import com.zaphira.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletServiceClient walletServiceClient;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Enregistre un utilisateur et crée automatiquement un wallet.
     */
    public User registerUser(RegisterRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(existing -> {
                    if (request.getFullName() != null
                            && !request.getFullName().isBlank()
                            && !request.getFullName().equals(existing.getFullName())) {
                        existing.setFullName(request.getFullName());
                        userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createNewUserWithWallet(request));
    }

    /**
     * Enregistre un utilisateur et retourne le wallet créé.
     * Si l'utilisateur existe déjà, essaie de récupérer son wallet existant.
     * 
     * @param request Les informations d'inscription
     * @return WalletDTO créé ou existant, ou null si la création/récupération échoue
     */
    public WalletDTO registerUserAndCreateWallet(RegisterRequest request) {
        // Vérifier si l'utilisateur existe déjà
        var existingUser = userRepository.findByEmail(request.getEmail());
        
        if (existingUser.isPresent()) {
            // Utilisateur existe déjà, essayer de récupérer son wallet
            try {
                WalletDTO existingWallet = walletServiceClient.getWalletByUserId(existingUser.get().getId());
                log.info("✅ Existing wallet found for user {}: {}", 
                        existingUser.get().getId(), existingWallet.getWalletNumber());
                return existingWallet;
            } catch (Exception e) {
                log.warn("⚠️ Wallet not found for existing user {}, attempting to create", 
                        existingUser.get().getId());
                // Essayer de créer le wallet pour l'utilisateur existant
                return createWalletForUser(existingUser.get().getId());
            }
        }
        
        // Nouvel utilisateur, créer user puis wallet
        User newUser = registerUser(request);
        return createWalletForUser(newUser.getId());
    }

    /**
     * Authentifie un utilisateur.
     */
    public User authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(null);
    }

    /**
     * Récupère un compte via email.
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Méthode centralisée pour création user + wallet via wallet-service.
     * Crée l'utilisateur d'abord, puis le wallet.
     * En cas d'échec de création du wallet, l'utilisateur reste créé (pas de rollback automatique).
     */
    private User createNewUserWithWallet(RegisterRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Generate default phoneNumber if not provided
        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            phoneNumber = "+237" + String.valueOf(Math.abs(request.getEmail().hashCode()))
                    .substring(0, Math.min(9, String.valueOf(Math.abs(request.getEmail().hashCode())).length()));
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(hashedPassword)
                .phoneNumber(phoneNumber)
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("User created successfully: {}", savedUser.getId());

        // Create wallet via wallet-service (synchrone)
        createWalletForUser(savedUser.getId());

        return savedUser;
    }

    /**
     * Crée un wallet pour un utilisateur via wallet-service.
     * Gère les erreurs et log les problèmes.
     * 
     * @param userId L'ID de l'utilisateur
     * @return WalletDTO créé, ou null si la création échoue
     */
    private WalletDTO createWalletForUser(Long userId) {
        try {
            WalletDTO wallet = walletServiceClient.createWallet(userId, "XOF");
            log.info("✅ Wallet created successfully for user {}: {}", 
                    userId, wallet.getWalletNumber());
            return wallet;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("❌ Cannot connect to wallet-service for user {}. " +
                     "Wallet-service may be down.", userId);
            log.error("Error details: {}", e.getMessage());
            // L'utilisateur est créé mais le wallet ne l'est pas
            // En production, vous pourriez implémenter un mécanisme de retry
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ HTTP error while creating wallet for user {}: {}", 
                     userId, e.getStatusCode());
            log.error("Error details: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ Unexpected error while creating wallet for user {}: {}", 
                     userId, e.getMessage(), e);
            return null;
        }
    }
}
```

### 3. AuthController.java

```java
package com.zaphira.auth.controller;

import com.zaphira.auth.dto.LoginRequest;
import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.dto.RegisterResponse;
import com.zaphira.auth.dto.UserResponse;
import com.zaphira.auth.security.JwtUtil;
import com.zaphira.auth.service.RefreshTokenService;
import com.zaphira.auth.service.UserService;
import com.zaphira.common.dto.WalletDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/auth", ""})
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * Enregistre un nouvel utilisateur et crée automatiquement un wallet.
     * 
     * @param request Les informations d'inscription
     * @return RegisterResponse contenant les tokens, l'utilisateur et le wallet créé
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Créer l'utilisateur
            var user = userService.registerUser(request);
            
            // Générer les tokens
            var accessToken = jwtUtil.generateAccessToken(user.getEmail());
            var refreshToken = refreshTokenService.createToken(user.getId());

            // Créer le wallet de manière synchrone
            WalletDTO wallet = userService.registerUserAndCreateWallet(request);
            
            // Construire la réponse utilisateur
            var userResponse = new UserResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole().name(),
                    wallet != null ? wallet.getId() : null
            );

            // Construire la réponse complète avec wallet
            var registerResponse = RegisterResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .user(userResponse)
                    .wallet(wallet) // Wallet créé (peut être null si échec)
                    .build();

            // Si le wallet n'a pas pu être créé, retourner un warning mais l'utilisateur est créé
            if (wallet == null) {
                log.warn("⚠️ User {} created but wallet creation failed. " +
                        "Wallet can be created later.", user.getId());
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(registerResponse);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(registerResponse);
                    
        } catch (Exception e) {
            log.error("❌ Error during user registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during registration: " + e.getMessage());
        }
    }

    // ... autres méthodes (login, refresh, logout, me)
}
```

### 4. RegisterResponse.java

```java
package com.zaphira.auth.dto;

import com.zaphira.common.dto.WalletDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
    private WalletDTO wallet; // Wallet créé automatiquement
}
```

---

## 📊 Exemple de Requête/Réponse

### Requête POST `/api/auth/register`

**Headers** :
```
Content-Type: application/json
```

**Body (JSON)** :
```json
{
  "email": "user@example.com",
  "fullName": "John Doe",
  "password": "secret123",
  "phoneNumber": "+237771234567"
}
```

**Réponse 201 Created (Succès)** :
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "abc123def456...",
  "user": {
    "id": 1,
    "fullName": "John Doe",
    "email": "user@example.com",
    "role": "USER",
    "walletId": 1
  },
  "wallet": {
    "id": 1,
    "walletNumber": "12345678",
    "balance": 0.0,
    "currency": "XOF",
    "active": true,
    "userId": 1
  }
}
```

**Réponse 201 Created (Wallet non créé)** :
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "abc123def456...",
  "user": {
    "id": 1,
    "fullName": "John Doe",
    "email": "user@example.com",
    "role": "USER",
    "walletId": null
  },
  "wallet": null
}
```

---

## 🔧 Configuration

### application.properties (auth-service)

```properties
# Eureka Client
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# Feign Client Configuration
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=10000
```

### AuthServiceApplication.java

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients  // ✅ Important pour activer FeignClient
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

---

## ✅ Gestion d'Erreurs

### Scénarios Gérés

1. **Wallet-service indisponible** :
   - Log : `❌ Cannot connect to wallet-service`
   - Action : User créé, wallet = null dans la réponse
   - Code HTTP : 201 Created (avec wallet null)

2. **Erreur HTTP (4xx/5xx)** :
   - Log : `❌ HTTP error while creating wallet`
   - Action : User créé, wallet = null
   - Code HTTP : 201 Created (avec wallet null)

3. **Erreur inattendue** :
   - Log : `❌ Unexpected error while creating wallet`
   - Action : User créé, wallet = null
   - Code HTTP : 201 Created (avec wallet null)

### Logs Exemples

```
✅ User created successfully: 1
✅ Wallet created successfully for user 1: 12345678
```

ou

```
✅ User created successfully: 1
❌ Cannot connect to wallet-service for user 1. Wallet-service may be down.
⚠️ User 1 created but wallet creation failed. Wallet can be created later.
```

---

## 🚀 Test

### Commande cURL

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "fullName": "John Doe",
    "password": "secret123",
    "phoneNumber": "+237771234567"
  }'
```

### Commande PowerShell

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" `
  -Method Post `
  -Body '{"email":"user@example.com","fullName":"John Doe","password":"secret123","phoneNumber":"+237771234567"}' `
  -ContentType "application/json"
```

---

## 📋 Checklist

- [x] FeignClient créé et configuré
- [x] UserService crée le wallet après userRepository.save()
- [x] AuthController retourne user + wallet dans la réponse
- [x] RegisterResponse inclut le wallet
- [x] Gestion d'erreur complète avec logs
- [x] Pas de rollback automatique (user créé même si wallet échoue)
- [x] Configuration FeignClient dans application.properties
- [x] @EnableFeignClients dans AuthServiceApplication

---

## 🎯 Points Importants

1. **Communication Synchrone** : Le wallet est créé immédiatement après l'utilisateur
2. **Pas de Rollback** : Si le wallet échoue, l'utilisateur reste créé (évite la perte de données)
3. **Gestion d'Erreur** : Tous les cas d'erreur sont gérés avec des logs détaillés
4. **Réponse Enrichie** : La réponse inclut l'utilisateur ET le wallet créé
5. **Idempotence** : Si l'utilisateur existe déjà, on récupère son wallet existant

---

## 🔍 Vérification

Après l'inscription, vérifier le wallet créé :

```bash
GET http://localhost:8082/api/wallets/user/1
```

Ou via le walletNumber retourné dans la réponse.

