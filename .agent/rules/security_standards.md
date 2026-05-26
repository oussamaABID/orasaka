# ORASAKA SYSTEM RULE: ZERO-TRUST BFF SECURITY STANDARDS

## 🛡️ 1. Secrets, Cryptography & Isolation
* **Zero Hardcoded Secrets**: All keys, API credentials, and database secrets must be injected dynamically via standard Spring `@ConfigurationProperties(prefix = "orasaka.core")` or environment configurations.
* **Isolated Cryptography**: Password hashing logic using `BCryptPasswordEncoder` (or standard hashing components) belongs exclusively to the `orasaka-identity` package via `spring-security-crypto`. However, `orasaka-identity` must remain 100% purged of `spring-boot-starter-web` and `spring-boot-starter-security` to preserve library decoupling.
* **Log Scrubbing**: Loggers must filter and sanitize outputs to prevent trace leak of user credentials, bearer tokens, or AI prompt raw metadata (including API keys).

## 🔀 2. BFF Proxy Constraints & Token Flow
* **Network Isolation**: The client browser (`orasaka-ui`) is strictly prohibited from invoking the backend port `8080` or Ollama port `11434` directly.
* **Proxy Routing & M2M Token Injection**: All front-end interactions must pass through Next.js server-side API Routes (e.g., `/api/graphql`), which are responsible for validating the user session. The BFF proxy must securely inject the downstream identity header (`Authorization: Bearer <userId>`) to contextually assert tenant identity.

## 🔑 3. Spring Security Native Authentication & Introspection
* **Native Resource Server**: Authentication must be handled natively by Spring Security's OAuth2 Resource Server. Session token extraction and decoding are delegated to the official BearerTokenAuthenticationFilter and DefaultBearerTokenResolver.
* **Introspecting Authentication Manager**: A custom AuthenticationManager must be registered to introspect bearer tokens via IdentityService and handle developer bypasses, returning standard UsernamePasswordAuthenticationToken instances with proper SimpleGrantedAuthority lists.
* **CORS & CSRF Management**: Standard MVC CORS mappings must be configured using GraphQlCorsProperties under the official Spring GraphQL namespace (`spring.graphql.cors`). Custom CORS configuration namespaces are strictly prohibited. CSRF is disabled for stateless API operations.
