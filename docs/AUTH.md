# Authentication Reference

> Complete guide to Orasaka's authentication architecture, covering local credentials, OAuth2 federation, password recovery, and provider extensibility.

---

## Overview

Orasaka supports three authentication strategies, governed by configuration flags:

| Strategy                  | Module             | Config Prefix                    | Description                                                       |
| :------------------------ | :----------------- | :------------------------------- | :---------------------------------------------------------------- |
| **Local Credentials**     | `orasaka-identity` | `orasaka.identity.auth.local`    | Email + password → BCrypt → JWT                                   |
| **OAuth2 Token-Exchange** | `orasaka-identity` | `orasaka.identity.auth.oauth2.*` | NextAuth → provider token → backend verification → reconciliation |
| **Password Recovery**     | `orasaka-identity` | —                                | Token-based reset with zero-enumeration and 15m expiry            |

Both login strategies produce the same output: a domain `User` record with authorities, preferences, and interception state.

> [!IMPORTANT]
> The backend **never** performs OAuth2 protocol negotiation (authorization code flows, redirects, etc.). That responsibility belongs entirely to the frontend layer (NextAuth). The backend acts purely as an **Identity Verifier & Reconciler** (ADR-023).

---

## 🔑 Local Credential Flow

```mermaid
sequenceDiagram
    participant Browser as NextAuth (Browser)
    participant BFF as Next.js BFF
    participant GW as orasaka-gateway
    participant IS as IdentityService
    participant IP as orasaka-persistence-identity
    participant DB as PostgreSQL

    Browser->>BFF: POST /api/auth/login {email, password}
    BFF->>GW: POST /api/v1/auth/login {email, password}
    GW->>IS: authenticate(email, password)
    IS->>IP: findByEmailAndEnabledTrueWithAssociations(email)
    IP->>DB: query user details
    DB-->>IP: raw entities
    IP-->>IS: UserEntity (with authorities, interceptions)
    IS->>IS: BCrypt.matches(password, passwordHash)
    IS-->>GW: User domain record
    GW-->>BFF: {token: userId, username, active_interceptions}
    BFF-->>Browser: Set session cookie
```

### Endpoint

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret"
}
```

### Response (200 OK)

```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "user@example.com",
  "authorities": ["ROLE_USER"],
  "active_interceptions": ["onboarding"]
}
```

---

## 🌐 OAuth2 Token-Exchange Flow

```mermaid
sequenceDiagram
    participant Browser as NextAuth (Browser)
    participant Provider as Google/GitHub
    participant BFF as Next.js BFF
    participant GW as orasaka-gateway
    participant RS as IdentityReconciliationService
    participant PV as OAuth2ProviderVerifier
    participant IP as orasaka-persistence-identity
    participant DB as PostgreSQL

    Browser->>Provider: OAuth2 Authorization Flow
    Provider-->>Browser: ID Token / Access Token
    Browser->>BFF: POST /api/auth/callback {provider, idToken}
    BFF->>GW: POST /api/v1/auth/oauth {provider, idToken}
    GW->>RS: reconcile(provider, idToken)
    RS->>PV: supports(provider) → true
    RS->>PV: verifyAndExtract(idToken)
    PV-->>RS: ExtractedProfile {email, providerId, name, avatarUrl}
    RS->>IP: findByProviderAndProviderId(provider, providerId)
    IP->>DB: query user identity
    alt User exists
        DB-->>IP: UserEntity
        IP-->>RS: UserEntity
    else New user
        RS->>IP: save(new UserEntity with null password)
        IP->>DB: INSERT UserEntity
        RS->>IP: save(AuthorityEntity ROLE_USER)
        IP->>DB: INSERT AuthorityEntity
    end
    RS-->>GW: User domain record
    GW-->>BFF: {token: userId, username, active_interceptions}
    BFF-->>Browser: Set session cookie
```

### Endpoint

```
POST /api/v1/auth/oauth
Content-Type: application/json

{
  "provider": "google",
  "idToken": "eyJhbGciOiJSUzI1NiIs...",
  "email": "user@gmail.com",
  "username": "John Doe"
}
```

| Field      | Required | Description                                                  |
| :--------- | :------: | :----------------------------------------------------------- |
| `provider` |    ✅    | Provider identifier: `"google"`, `"github"`, etc.            |
| `idToken`  |    ✅    | Raw identity/access token from the external provider         |
| `email`    |    ❌    | Optional email hint for logging                              |
| `username` |    ❌    | Optional username hint (falls back to provider profile name) |

### Response (200 OK)

```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "username": "John Doe",
  "email": "user@gmail.com",
  "authorities": ["ROLE_USER"],
  "active_interceptions": []
}
```

### Response (401 Unauthorized)

```json
{
  "error": "No active verifier found for provider: apple. Ensure the provider is enabled in configuration."
}
```

---

## 🔐 Password Recovery Flow

The password recovery subsystem implements a secure, asynchronous, token-based reset mechanism with zero-enumeration protection (§7.1).

```mermaid
sequenceDiagram
    participant Client as Browser / CLI
    participant BFF as Next.js BFF
    participant GW as orasaka-gateway
    participant PRS as PasswordRecoveryService
    participant IP as orasaka-persistence-identity
    participant DB as PostgreSQL

    Note over Client,DB: Step 1: Request Reset Token
    Client->>BFF: POST /api/auth/forgot {email}
    BFF->>GW: POST /api/v1/auth/forgot {email}
    GW->>PRS: requestPasswordReset(email)
    PRS->>IP: findByEmailAndEnabledTrue(email)
    IP->>DB: SELECT user
    alt User exists
        PRS->>PRS: Generate 48-byte SecureRandom token
        PRS->>PRS: SHA-256 hash the raw token
        PRS->>IP: save(PasswordResetTokenEntity)
        IP->>DB: INSERT token_hash, email, expires_at
        PRS->>PRS: Publish PasswordResetRequestedEvent
    end
    PRS-->>GW: void (always succeeds)
    GW-->>BFF: 200 {message: "If this account exists..."}
    BFF-->>Client: Generic success

    Note over Client,DB: Step 2: Reset Password
    Client->>BFF: POST /api/auth/reset {token, newPassword}
    BFF->>GW: POST /api/v1/auth/reset {token, newPassword}
    GW->>PRS: resetPassword(token, newPassword)
    PRS->>PRS: SHA-256 hash the raw token
    PRS->>IP: findByTokenHash(hashedToken)
    IP->>DB: SELECT token record
    alt Token valid and not expired
        PRS->>PRS: BCrypt.encode(newPassword) [outside @Transactional]
        PRS->>IP: updatePasswordHashByEmail(email, bcryptHash)
        IP->>DB: UPDATE password_hash, password_changed_at
        PRS->>IP: deleteByTokenHash(hashedToken)
        IP->>DB: DELETE token record
        PRS-->>GW: void (success)
        GW-->>BFF: 200 {message: "Password has been reset..."}
    else Token invalid or expired
        PRS-->>GW: throw InvalidRequestException
        GW-->>BFF: 400 {error: "Invalid or expired..."}
    end
```

### Forgot Password Endpoint

```
POST /api/v1/auth/forgot
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### Response (200 OK — Always)

```json
{
  "message": "If this account exists, a secure recovery email has been staged."
}
```

> [!IMPORTANT]
> **Zero-Enumeration Invariant:** This endpoint always returns `200 OK` with a generic message, regardless of whether the email exists in the database. This prevents attackers from enumerating valid accounts.

### Reset Password Endpoint

```
POST /api/v1/auth/reset
Content-Type: application/json

{
  "token": "a1b2c3d4e5f6...",
  "newPassword": "newSecurePassword123"
}
```

#### Response (200 OK)

```json
{
  "message": "Password has been reset successfully."
}
```

#### Response (400 Bad Request)

```json
{
  "error": "Invalid or expired reset token"
}
```

### Security Properties

| Property | Value |
|:---|:---|
| **Token generation** | 48-byte `SecureRandom` (384-bit entropy) |
| **Token storage** | SHA-256 hash (plaintext never persisted) |
| **Token expiration** | 15 minutes (`Instant.now().plusMinutes(15)`) |
| **Token usage** | Single-use — deleted immediately after successful reset |
| **Password hashing** | BCrypt via `PasswordEncoder` (outside `@Transactional`) |
| **Session invalidation** | `password_changed_at` column updated atomically |
| **Log scrubbing** | Only 8-character hash prefix logged (never full tokens) |
| **Authentication** | `permitAll()` — no session required |

### Database Schema

Flyway migrations `V22__create_password_resets_table.sql` and `V23__add_password_changed_at_to_users.sql`:

```sql
-- V22: Password reset tokens
CREATE TABLE orasaka_password_resets (
    id         VARCHAR(255) PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_password_resets_token_hash ON orasaka_password_resets(token_hash);
CREATE INDEX idx_password_resets_email ON orasaka_password_resets(email);

-- V23: Session invalidation support
ALTER TABLE orasaka_users
    ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE;
```

---

## ⚙️ Configuration Flags

All authentication strategies are governed by feature flags in `application.yml`:

```yaml
orasaka:
  identity:
    auth:
      local:
        enabled: true # Email/password login
      oauth2:
        google:
          enabled: ${ORASAKA_OAUTH2_GOOGLE_ENABLED:false}
          client-id: ${GOOGLE_CLIENT_ID:}
        github:
          enabled: ${ORASAKA_OAUTH2_GITHUB_ENABLED:false}
          client-id: ${GITHUB_CLIENT_ID:}
```

### Environment Variables

| Variable                        | Default   | Description                           |
| :------------------------------ | :-------- | :------------------------------------ |
| `ORASAKA_OAUTH2_GOOGLE_ENABLED` | `false`   | Enable/disable Google OAuth2 provider |
| `GOOGLE_CLIENT_ID`              | _(empty)_ | Google OAuth2 client ID               |
| `ORASAKA_OAUTH2_GITHUB_ENABLED` | `false`   | Enable/disable GitHub OAuth2 provider |
| `GITHUB_CLIENT_ID`              | _(empty)_ | GitHub OAuth2 client ID               |

> [!NOTE]
> When a provider is disabled (`enabled: false`), its `OAuth2ProviderVerifier` bean is **never instantiated**. This means zero startup overhead and zero memory allocation for unused providers.

---

## 🔌 Adding a New Provider

Adding a new OAuth2 provider follows the **Open-Closed Principle** — no existing code needs modification.

### Step 1: Create the Verifier

```java
package com.orasaka.identity.infrastructure.adapter.federation;

@Component
@ConditionalOnProperty(
    prefix = "orasaka.identity.auth.oauth2.apple",
    name = "enabled",
    havingValue = "true")
class AppleProviderVerifier implements OAuth2ProviderVerifier {

    @Override
    public boolean supports(String providerId) {
        return "apple".equalsIgnoreCase(providerId);
    }

    @Override
    public ExtractedProfile verifyAndExtract(String idToken) {
        // Implement Apple ID token verification
        // Return ExtractedProfile with email, providerId, name, avatarUrl
    }
}
```

### Step 2: Add Configuration

```yaml
orasaka:
  identity:
    auth:
      oauth2:
        apple:
          enabled: ${ORASAKA_OAUTH2_APPLE_ENABLED:false}
          client-id: ${APPLE_CLIENT_ID:}
```

### Step 3: Update `FederationProperties.java`

Add the new provider config record to `FederationProperties.OAuth2Auth`.

That's it. The `IdentityReconciliationService` automatically discovers the new verifier via Spring's `List<OAuth2ProviderVerifier>` injection.

---

## 🏛️ Architecture Invariants

| Rule                       | Enforcement                                                                 |
| :------------------------- | :-------------------------------------------------------------------------- |
| **Token-Exchange Only**    | Backend never performs OAuth2 authorization code flows (ERR-105)            |
| **Stateless Verification** | No provider sessions maintained in Spring Security filters                  |
| **Conditional Loading**    | `@ConditionalOnProperty` gates each provider bean                           |
| **Null Password Hash**     | Federated users have `password_hash = NULL` in the database                 |
| **Race Condition Safety**  | `unique_provider_user` constraint handles concurrent JIT provisioning       |
| **Web Agnosticism**        | `orasaka-identity` has zero web dependencies (no `spring-boot-starter-web`) |
| **Context Propagation**    | Global `SecurityContext` propagation across virtual threads is managed via `DelegatingSecurityContextExecutorService` wrapper bean. |
| **Non-blocking Transactions** | Network I/O and cryptographic verifications run outside db transactions to prevent connection pool starvation (ADR-025) |

---

## 📎 Related Documentation

| Document                                  | Description                                               |
| :---------------------------------------- | :-------------------------------------------------------- |
| [Architecture Reference](ARCHITECTURE.md) | System topology, module boundaries, execution flows       |
| [API Reference](API_REFERENCE.md)         | Public types, facades, endpoints, data models             |
| [ADR-023](CONTEXT.md)                     | Agnostic OAuth2 Token-Exchange Federation decision record |
| [AGENTS.md](../AGENTS.md)                 | ERR-105: Identity Federation Invariant                    |
