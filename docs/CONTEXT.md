# Architecture Decision Records (ADR)

> Ledger of architectural decisions made in the Orasaka project.

---

## 1. Quick Reference Index

| ADR | Decision Summary | Status |
| :-: | :--- | :---: |
| **001** | Ollama as default provider | Approved |
| **002** | Core must be a standalone web-agnostic library | Approved |
| **003** | Client-side MCP integration via service bridge | Approved |
| **004** | Unified registry mapping Java methods to `FunctionCallback` | Approved |
| **005** | Native Ollama on macOS for GPU acceleration | Approved |
| **006** | Monorepo structure (`core`, `identity`, `gateway`, `tools`, etc.) | Approved |
| **007** | Move tool implementations to `orasaka-tools` | Approved |
| **008** | Enforce Next.js BFF proxy (browser never hits port 8080/11434) | Approved |
| **009** | Virtual threads + `SecurityContext` preservation | Approved |
| **010** | Multi-tier cache (Caffeine memory + Postgres DB) | Approved |
| **011** | Async RAG background indexer executing on virtual threads | Approved |
| **012** | Token-based double-opt-in account verification | Approved |
| **013** | In-memory cache for user interruptions state in JWT | Approved |
| **014** | Immutable rich DTO records validating in constructor | Approved |
| **015** | Open/Closed pipeline separating engine from interceptors | Approved |
| **016** | Workspace indexing via dedicated MCP database service | Approved |
| **017** | Anemic services delegation logic to domain records | Approved |
| **018** | Rigid record suffix naming rules | Approved |
| **019** | Strict package-private boundary controls | Approved |
| **020** | Code locality: related classes grouped in single files | Approved |
| **021** | stable-diffusion.cpp local image generator (port 8085) | Approved |
| **022** | Python SVD XT local video generator (port 8188) | Approved |
| **023** | Stateless OAuth2 token verifiers strategy pattern | Approved |
| **024** | Mapper isolation: mapping logic in static `*Mapper` files | Approved |
| **025** | Network verification runs outside `@Transactional` locks | Approved |
| **026** | User preferences managed in `orasaka-identity` boundary | Approved |
| **027** | Metadata-driven Feature registry bootstrap mapping | Approved |
| **028** | Unified `ProviderClassifier` helper for AI routes | Approved |
| **029** | Token-based password resets with zero-enumeration checks | Approved |
| **033** | Multi-tier testing matrix across modules | Approved |
| **034** | Testcontainers singleton reuse strategy | Approved |
| **038** | Cinematic design HUD first, input-blocking, delete CLI generate | Approved |
| **039** | `orasaka-business` persona library, transitive context session | Approved |
| **040** | Hermetic E2E testing profile and 4-tier validation matrix | Approved |

---

## 2. ADR Detailed Ledger

### ADR-001 through ADR-007 (Core Architecture)
- **ADR-001**: Ollama default provider for local, offline development.
- **ADR-002**: `orasaka-core` is web-agnostic and relies only on `spring-ai-core`.
- **ADR-003**: Dynamic client-side MCP bridge decoupling tools from engine.
- **ADR-004**: Mapping local Java methods to LLM tools via registry.
- **ADR-005**: Runs native Ollama to leverage macOS Metal GPU acceleration.
- **ADR-006**: Monorepo layout isolates layers (`core`, `identity`, `gateway`, `tools`).
- **ADR-007**: Concrete tools are encapsulated in `orasaka-tools`.

### ADR-008 through ADR-013 (Security & Performance)
- **ADR-008**: Browser requests must proxy through Next.js server-side API routes (BFF).
- **ADR-009**: Blocking tasks run on Virtual Threads; SecurityContext is manually propagated.
- **ADR-010**: Caffeine (L1) + PostgreSQL (L2) cache for tool callbacks.
- **ADR-011**: Daily background runner handles document ingestion asynchronously.
- **ADR-012**: Verification tokens stored securely as SHA-256 hashes.
- **ADR-013**: Gateway interruptions cached in JWT claims to bypass polling.

### ADR-014 through ADR-020 (Formatting & Standards)
- **ADR-014**: Defensive copy constraints validated in compact record constructors.
- **ADR-015**: Interceptors resolve RAG/MCP logic, keeping engine logic clean.
- **ADR-016**: Code-indexing delegates to externalized MCP service.
- **ADR-017**: Services are stateless; logic lives inside rich domain objects.
- **ADR-018**: Immutable models must be Java `record` types.
- **ADR-019**: Non-facade beans and configs are package-private.
- **ADR-020**: Tightly coupled records reside in the same file boundary.

### ADR-021 through ADR-027 (Media & Advanced Flows)
- **ADR-021**: stable-diffusion.cpp runs on port 8085, returns RFC 2397 base64 URLs.
- **ADR-022**: Standalone Python video worker runs SVD XT on port 8188.
- **ADR-023**: Reconciles OAuth2 credentials in `IdentityReconciliationService`.
- **ADR-024**: Mapping logic is isolated to static helper mappers (max 5 lines inline).
- **ADR-025**: API network requests are decoupled from `@Transactional` blocks.
- **ADR-026**: Preferences are isolated to the identity boundary.
- **ADR-027**: Gateway acts as bootstrapper, loading dynamic features lists from properties.

### ADR-028 through ADR-040 (Governance & Enterprise)
- **ADR-028**: Centralized provider checks in `ProviderClassifier` utility.
- **ADR-029**: Hashed single-use recovery tokens expire in 15 minutes.
- **ADR-033 / ADR-034**: Unified testing framework using single-instance Testcontainers.
- **ADR-038**: Cinematic dark-mode design system with strict input blocking during streaming. Banned CLI code generator `generate`.
- **ADR-039**: Prompts decoupled into `orasaka-business` markdown repository. Session ID mapped to context preferences.
- **ADR-040**: Hermetic E2E testing via `orasaka-apps/orasaka-end2end` module. 3-Tier test pyramid (httpyac API → CLI Vitest → Playwright Java UI). Configurable 4-Tier Validation Matrix: JSON Schema -> MCP Sandbox -> Multi-Agent Debate -> Test-Driven Response.
