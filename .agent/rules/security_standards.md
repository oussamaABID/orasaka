# ORASAKA RULE: ZERO-TRUST SECURITY STANDARDS

## §1 — Secrets & Cryptography

### Zero Hardcoded Secrets
- All keys, API credentials, and database secrets injected via `@ConfigurationProperties` or environment.
- No `System.getenv()` in functional beans [ERR-113].
- No `localhost:*` or `127.0.0.1` URLs in production source.
- No `dummy-key`, `test-secret`, or placeholder credentials in production.

### Cryptography Isolation
- Password hashing (`BCryptPasswordEncoder`) exclusively in `orasaka-identity` via `spring-security-crypto`.
- `orasaka-identity` must never depend on `spring-boot-starter-web` or `spring-boot-starter-security`.
- Hashing executes outside `@Transactional` blocks to minimize connection hold time.

### Log Scrubbing
- Loggers must sanitize output: no credentials, bearer tokens, API keys, or raw prompt metadata.
- Stack traces must not expose internal package paths to external clients.
- Sensitive fields in logs: mask with `***` or omit entirely.

## §2 — BFF Proxy & Network Isolation

### Client Restrictions
- Browser (`orasaka-ui`) is prohibited from calling backend port `8080` or Ollama port `11434`.
- All front-end requests proxy through Next.js API routes with session validation.
- BFF injects `Authorization: Bearer <userId>` header downstream.

### Token Flow
- NextAuth handles OAuth2 negotiation (Google, GitHub).
- Backend acts as identity verifier and reconciler — no stateful provider sessions.
- Session tokens stored in HTTP-only, Secure, SameSite cookies.

## §3 — Spring Security Standards

### Authentication
- Native Spring Security OAuth2 Resource Server.
- `BearerTokenAuthenticationFilter` extracts tokens from `Authorization` header or query params.
- Custom `AuthenticationManager` introspects tokens via `IdentityService`.
- Returns `UsernamePasswordAuthenticationToken` with proper `SimpleGrantedAuthority`.

### Authorization
- All API endpoints require authentication except:
  - `GET /health`
  - `POST /api/auth/login`
  - `POST /api/auth/register`
- Admin endpoints require `ROLE_ADMIN` authority.
- Feature endpoints require `ROLE_USER` or `ROLE_ADMIN`.

### CORS & CSRF
- CORS bound to `spring.graphql.cors` namespace via `GraphQlCorsProperties`.
- Custom CORS namespaces prohibited.
- CSRF disabled for stateless API operations.

## §4 — Input Validation & Injection Prevention

### Request Validation
- All request DTOs self-validate in compact constructors [ERR-106].
- Base64 media payloads limited to 10 MB decoded size.
- File uploads validated by:
  - MIME type whitelist (image/*, audio/*, video/*, application/pdf)
  - Maximum file size
  - Content-type header verification

### SQL Injection Prevention
- Raw SQL strings banned — use parameterized `@Query` or Spring Data methods.
- No string concatenation in query construction.
- Dynamic queries via `Specification<T>` or Criteria API only.

### XSS Prevention
- User input sanitized before storage.
- Response content-type headers explicitly set.
- CSP headers configured in BFF layer.

## §5 — Dependency Security

### Vulnerability Management
- Dependencies scanned via OWASP Dependency Check or Snyk.
- No `SNAPSHOT` dependencies in production builds.
- Security patches applied within 72 hours of CVE disclosure.

### Supply Chain
- Lock files (`package-lock.json`, `pom.xml` version pinning) committed to VCS.
- No wildcard version ranges in production dependencies.
