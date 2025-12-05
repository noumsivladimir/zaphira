# Transaction Service Integration Notes

This file documents integration points and required environment values for the `transaction-service`.

Required environment / configuration

- `app.jwt.secret` — HMAC secret used to validate JWTs issued by the auth-service. Must match auth-service `jwt.secret`.
  - Example (application.properties):
    `app.jwt.secret=YOUR_BASE64_OR_RAW_SECRET`

- `app.jwt.access-expiration` — access token lifetime in milliseconds (optional).
- `app.jwt.refresh-expiration` — refresh token lifetime in milliseconds (optional).

Feign / service discovery

- The transaction-service uses Feign to call the `wallet-service` endpoints (by service name `wallet-service`). Ensure the service registry (Eureka) or `spring.cloud.discovery` is configured to resolve service names.

Wallet verification

- Transactions require that the JWT contains a `userId` claim (numeric). The `JwtAuthenticationFilter` requires this claim and sets the authenticated principal accordingly.
- The service resolves the sender wallet via Feign client to `wallet-service` and verifies `wallet.userId == authenticated userId` before creating or processing a transaction.

Security

- The service enforces authentication for `/api/transactions/**` and uses a stateless JWT filter.
- Provide `app.jwt.secret` in your environment or config server. Without it JWT validation will fail.

Notes

- Cross-service DB-level foreign keys are not enforced. Wallet ownership is validated at the application level via the wallet-service.
- If you prefer better performance, include `userId` in JWT tokens issued by auth-service to avoid additional service lookups.
