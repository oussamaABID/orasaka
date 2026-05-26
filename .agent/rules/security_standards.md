# Rule: Zero-Trust Security Standards

## §1 Secrets & Cryptography
- Secrets: No hardcoding. Inject via properties/env. No `System.getenv()` in beans [ERR-113].
- URLs: No `localhost` or `127.0.0.1` in production code. No dummy/placeholder keys.
- Isolation: Hashing (`BCryptPasswordEncoder`) in `orasaka-identity` only. Banned: web/security starter dependencies in identity layer. Run hashing outside `@Transactional`.
- Scrubbing: Mask/sanitize logger outputs (no keys, tokens, prompts). Hide internal package paths in external stack traces.

## §2 BFF & Network
- Isolation: Browser client (`orasaka-ui`) cannot query gateway (8080) or Ollama (11434) directly. Routes must proxy via Next.js server.
- Auth Flow: NextAuth handles Google/GitHub OAuth2. BFF injects `Authorization: Bearer <userId>`. Cookie security: HTTP-only, Secure, SameSite.

## §3 Spring Security
- Engine: OAuth2 Resource Server. `BearerTokenAuthenticationFilter` extracts tokens from auth header or query. Custom `AuthenticationManager` introspects via `IdentityService`.
- Authorization: All API endpoints authenticated except `GET /health`, `POST /api/auth/login`, `POST /api/auth/register`. Admins require `ROLE_ADMIN`.
- CORS/CSRF: Bind CORS via `spring.graphql.cors` namespace. Custom CORS prohibited. Disable CSRF for stateless APIs.

## §4 Input & Injection
- Validation: DTOs must self-validate in compact constructors [ERR-106]. MIME and size checks (max 10MB) for base64/files.
- SQL Injection: Parameterized `@Query` or Spring Data only. Banned: raw SQL string concatenation.
- XSS/CSP: Sanitize inputs before storage. Explicit response headers. CSP configured at BFF.

## §5 Dependency Security
- Auditing: Scan dependencies (Snyk/OWASP). No wildcard version ranges or `SNAPSHOT` dependencies. Apply security patches within 72 hours. Pin versions in lockfiles.
