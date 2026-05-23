# ORASAKA SYSTEM RULE: ZERO-TRUST BFF SECURITY STANDARDS

## 🛡️ 1. Secrets, Cryptography & Isolation
* **Zero Hardcoded Secrets**: All keys, API credentials, and database secrets must be injected dynamically via standard Spring `@ConfigurationProperties(prefix = "orasaka.core")` or environment configurations.
* **Isolated Cryptography**: Password hashing logic using `BCryptPasswordEncoder` (or standard hashing components) belongs exclusively to the `orasaka-identity` package via `spring-security-crypto`. However, `orasaka-identity` must remain 100% purged of `spring-boot-starter-web` and `spring-boot-starter-security` to preserve library decoupling.
* **Log Scrubbing**: Loggers must filter and sanitize outputs to prevent trace leak of user credentials, bearer tokens, or AI prompt raw metadata (including API keys).

## 🔀 2. BFF Proxy Constraints & Token Flow
* **Network Isolation**: The client browser (`orasaka-ui`) is strictly prohibited from invoking the backend port `8080` or Ollama port `11434` directly.
* **Proxy Routing & M2M Token Injection**: All front-end interactions must pass through Next.js server-side API Routes (e.g., `/api/graphql`), which are responsible for validating the user session. The BFF proxy must securely inject the downstream identity header (`Authorization: Bearer <userId>`) to contextually assert tenant identity.
