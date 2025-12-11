**Liste des endpoints par microservice**

Ce document liste les routes exposées par les microservices du projet et les paramètres nécessaires pour les appeler (path, query, body, headers obligatoires).

**Remarque générale**: les endpoints protégés exigent le header `Authorization: Bearer <accessToken>` émis par le service `auth`.

**AUTH**
- **POST** `/api/auth/register`
  - Body JSON requis: `email`, `fullName`, `password`, `phoneNumber`
  - Headers: aucun

- **POST** `/api/auth/login`
  - Body JSON requis: `email`, `password`
  - Headers: aucun

- **POST** `/api/auth/refresh`
  - Paramètre requis: `refreshToken` (query ou form)
  - Headers: aucun

- **POST** `/api/auth/logout`
  - Headers requis: `Authorization: Bearer <accessToken>`

- **GET** `/api/auth/me`
  - Headers requis: `Authorization: Bearer <accessToken>`

- **GET** `/api/logs/{userId}`
  - Path param requis: `userId` (Long)
  - Headers: selon sécurité (optionnellement `Authorization`)

**SERVICE-USER**
- **GET** `/api/users/email/{email}`
  - Path param requis: `email` (String, URL-encoded si nécessaire)
  - Usage: lookup cross-service — retourne `UserDTO`

- **GET** `/api/users/profile`
  - Headers requis: `Authorization: Bearer <accessToken>` (middleware positionne `userId`)

- **PUT** `/api/users/profile`
  - Headers requis: `Authorization: Bearer <accessToken>`
  - Body JSON: champs optionnels possibles: `firstName`, `lastName`, `phoneNumber`, `neighborhood`, `city`, `region`, `preferredLanguage`, `notificationsEnabled`, `emailNotificationsEnabled`, `smsNotificationsEnabled`

- **POST** `/api/users/profile/picture`
  - Headers requis: `Authorization: Bearer <accessToken>`
  - Body multipart: `file` (image)

- **DELETE** `/api/users/profile`
  - Headers requis: `Authorization: Bearer <accessToken>`

- **GET** `/api/users/{userId}`
  - Path param requis: `userId` (Long)
  - Headers: typiquement `Authorization`

**WALLET-SERVICE**
- **POST** `/api/wallets`
  - Query params requis: `userId` (Long)
  - Query params optionnels: `currency` (String, défaut `XOF`)
  - Body: none
  - Response: `WalletDTO`

- **GET** `/api/wallets/user/{userId}`
  - Path param requis: `userId` (Long)
  - Response: `WalletDTO`

- **GET** `/api/wallets/{walletNumber}`
  - Path param requis: `walletNumber` (String)
  - Response: `WalletDTO`

- **POST** `/api/wallets/transfer`
  - Body JSON requis: `senderWalletNumber`, `receiverWalletNumber`, `amount`
  - Body optionnel: `currency`, `reference`, `description`

**TRANSACTION-SERVICE**
- **POST** `/api/transactions`
  - Headers requis: `Authorization: Bearer <accessToken>`
  - Body JSON requis: `senderWalletNumber`, `receiverWalletNumber`, `amount`, `currency`, `type` (enum), `channel` (enum), `requestedBy`
  - Body optionnel: `description`, `processInstantly` (boolean)

- **GET** `/api/transactions`
  - Headers: `Authorization: Bearer <accessToken>` (probable)

- **GET** `/api/transactions/{id}`
  - Path param requis: `id` (Long)
  - Headers requis: `Authorization`

- **GET** `/api/transactions/{id}/history`
  - Path param requis: `id` (Long)
  - Headers requis: `Authorization`

- **GET** `/api/transactions/{id}/authorization`
  - Path param requis: `id` (Long)
  - Headers requis: `Authorization`

- **POST** `/api/transactions/{id}/authorize`
  - Path param requis: `id` (Long)
  - Headers requis: `Authorization`
  - Body JSON requis: `method`, `code`, `authorizedBy`

- **PUT** `/api/transactions/{id}/status`
  - Path param requis: `id` (Long)
  - Headers requis: `Authorization`
  - Body JSON requis: `status`, `changedBy` (optionnel `reason`)

- **PUT** `/api/transactions/{id}/cancel`
  - Path param requis: `id` (Long)
  - Headers requis: `Authorization`
  - Body JSON requis: `status` (Cancelled), `changedBy`

- **POST** `/api/transactions/scheduled`
  - Headers requis: `Authorization`
  - Body JSON requis: `senderWalletNumber`, `receiverWalletNumber`, `amount`, `currency`, `type`, `channel`, `requestedBy`, `scheduledFor` (future datetime)
  - Body optionnel: `description`

- **GET** `/api/transactions/scheduled`
  - Headers requis: `Authorization`

- **GET** `/api/transactions/scheduled/{id}`
  - Path param requis: `id` (Long)
  - Headers requis: `Authorization`

- **DELETE** `/api/transactions/scheduled/{id}`
  - Path param requis: `id` (Long)
  - Query param requis: `cancelledBy` (String)
  - Query param optionnel: `reason` (String)
  - Headers requis: `Authorization`

**INTER-SERVICE (Feign) — endpoints utilisés**
- `GET /api/wallets/{walletNumber}` — path `walletNumber` (String)
- `GET /api/wallets/user/{userId}` — path `userId` (Long)
- `POST /api/wallets/transfer` — body `TransferRequest` { `senderWalletNumber`, `receiverWalletNumber`, `amount`, ... }
- `GET /api/users/email/{email}` — path `email` (String)
- `POST /api/transactions` — body `TransactionRequest` (utilisé par wallet-service)

---

Si vous voulez, je peux aussi créer `test-endpoints.ps1` avec des commandes `Invoke-RestMethod` exemples (flow: register → login → create wallet → create transaction). Dites-moi si je l'ajoute.
