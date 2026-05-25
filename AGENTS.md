# 🥷 ORASAKA: SYSTEM & AGENT GOVERNANCE

> [!IMPORTANT]
> **AGENT EXECUTION CONTRACT & CONTEXT LOADING ORDER**
>
> 1. **Pre-requisite**: You MUST dynamically load, parse, and strictly adhere to all active definitions found within `.agent/rules/*` and `.agent/workflows/*` prior to any file modification or code generation task.
> 2. **Rule Aggregation**: Treat the specialized local rules inside the `.agent/` directory as strict, non-negotiable extensions of this global master specification ledger.
> 3. **Validation Gate**: Every task, architectural review, code compilation, and documentation synchronization loop must simultaneously satisfy both the local manifests and this ledger.

---

## 🏛️ 1. Core Decoupling Boundaries & Architecture

### A. Structural Agnosticism

* **The Stateless Core**: `orasaka-core` is an isolated, standalone orchestration library. It must remain 100% agnostic of active user storage, HTTP protocols, or active sessions. It depends strictly on `spring-ai-core` 1.1.6. No web starters or web security dependencies are allowed inside its perimeter.
* **No Class Leakage**: Type signatures native to `org.springframework.ai` must remain encapsulated inside `orasaka-core`. They are strictly prohibited from leaking outward into the gateway controllers, CLI modules, or identity layers.
* **Gateway Context Mandate**: `orasaka-gateway` acts as the single source of orchestration context. It is strictly responsible for fetching data profiles from `orasaka-identity`, assembling them into an immutable `PromptContext` snapshot, and injecting that context into the Core Client on every request.

### 🏛️ Module Separation Invariant [ERR-102]

* **Isolation of Concerns:** The core processing workspace (`orasaka-core`) and utilities package (`orasaka-tools`) are completely stateless regarding user identity and authentication mechanisms.
* **The Gateway Rule:** Only `orasaka-gateway` possesses cross-cutting clearance to call identity models and feed the resulting identifiers as plain context to underlying engines.
* **The Adapter Pattern:** `orasaka-tools` may implement interfaces defined in `orasaka-core.pipeline.*` (Ports & Adapters per ADR-007), but must never import from `orasaka-identity`. The reverse direction (`core → tools`) is strictly prohibited.
* **Context-Passing Principle:** If `orasaka-core` or `orasaka-tools` require user references or session scopes, they must strictly consume them as primitive strings (`String userId`, `UUID sessionId`) inside their native request payloads. They must **NEVER** import security contexts, tokens, or domain objects belonging to `orasaka-identity`.

### 🪓 Source File Isolation Invariant [ERR-103]

* **The Single File Principle:** Every top-level Java class, interface, enum, or record—whether in production (`src/main`) or test suites (`src/test`)—must reside in its own dedicated `.java` source file matching its simple name.
* **Anti-God-File Mandate:** Multi-class bundling or hiding secondary components inside another class's file to "save space" or "go faster" is strictly prohibited. It destroys package scannability and violates the Single Responsibility Principle.
* **Anti-Bundling Rules:** Pooling tests for distinct production artifacts (e.g., `VideoRequest` and `VideoResponse`) under a single `@Nested` wrapper file is strictly prohibited. Each production class must have its own 1:1 dedicated test file container. `@Nested` may still be used *within* a single test class to organize behavioral groups.
* **Automation:** Programmatically guarded by `one_top_level_class_per_file` evaluating both main and test classpaths across all modules. Any violation will actively fail the CI gateway.

### 🪓 Global Nomenclature & Zero-Prefix Invariant [ERR-104]

* **Monorepo-Wide Policy:** Prepending the project name `Orasaka` or `orasaka-` to any internal architectural artifact is strictly forbidden across **ALL** layers (Backend Java modules, Frontend React/Next.js apps, and CLI binary tools).
* **Naming Standards:**
  1. **Backend:** Rely exclusively on package topology (`com.orasaka.core..`) for ownership. Use `RagInterceptor` (not `OrasakaRagInterceptor`). Classes must carry standard behavioral suffixes (`*Interceptor`, `*Resolver`, `*Injector`).
  2. **Frontend:** Core UI components must use clean domain tokens. Use `Sidebar` or `McpPanel`, never `OrasakaSidebar`.
* **Whitelist:** Only `OrasakaCoreConfiguration` (Spring Boot `@Configuration` class) retains the prefix.
* **Automation:** Enforced via ArchUnit `no_redundant_project_prefix` gateway on the backend and `no-restricted-syntax` ESLint rule on frontend/CLI spaces.

### 🪓 Identity Federation Invariant [ERR-105]

* **The Token-Exchange Rule:** The identity module handles external providers via stateless token validation strategies. Spring Security filters must not maintain stateful provider sessions. The frontend (NextAuth) performs OAuth2 protocol negotiation; the backend acts purely as an identity verifier and reconciler.
* **Open-Closed Extensibility:** Adding a new authentication provider (e.g., Apple, Facebook) requires creating a single isolated class implementing `OAuth2ProviderVerifier` guarded by its respective config property flag. No existing code modifications are required.
* **Provider Isolation:** Each `OAuth2ProviderVerifier` implementation is conditionally loaded via `@ConditionalOnProperty`. Disabled providers produce zero bean allocations and zero startup overhead.

### 🪓 Invariant Validation & Record Enforcement [ERR-106]

* **Self-Validating Records:** All request, response, and domain records must enforce validation invariants inside their compact constructors. Null or blank required fields must throw `InvalidRequestException` (for request DTOs) or `NullPointerException` via `Objects.requireNonNull` (for response/domain DTOs) at construction time, never at consumption time.
* **Null-Return Prohibition:** Service methods that resolve identity (e.g., `authenticate`) must throw explicit unchecked exceptions (e.g., `BadCredentialsException`) instead of returning `null`. Procedural `if (result == null)` blocks in controllers are strictly forbidden.
* **DTO Domain Blindness:** The `com.orasaka.gateway.dto` package must have zero compile-time dependencies on `com.orasaka.identity.domain`. Domain-to-DTO mapping must occur exclusively at the controller call site boundary, never inside the DTO record itself.
* **ArchUnit Omnipresence:** Every module (`core`, `identity`, `tools`, `gateway`) must contain at least one dedicated `*BoundaryTest.java` class enforcing its own local isolation invariants. Missing boundary tests constitute a CI-blocking violation.

### 🪓 Mapper Isolation Invariant [ERR-107]

* **Anti-Inline-Mapping Rule:** Repetitive, field-by-field entity-to-domain or domain-to-DTO mapping code (setter chains, manual constructor calls with 5+ arguments) is strictly forbidden inside controllers (`*Controller.java`), services (`*Service.java`, `*ServiceImpl.java`), and engines (`*Engine.java`). These layers must remain pure orchestration logic.
* **Dedicated Mapper Classes:** All entity/domain/DTO mapping boilerplate must be extracted into dedicated, package-private `final class *Mapper` utility classes with `static` methods and a private constructor. Mappers live in the same package as the service they support (e.g., `com.orasaka.identity.service.UserMapper`).
* **Single-Line Call Site:** The service or controller call site must reduce to a single, highly readable mapper invocation (e.g., `UserMapper.toEntity(profile, userId, provider, "free")`). Multi-line setter blocks at the call site are a CI-blocking violation.
* **Visibility Constraint:** Mapper classes must never be `public`. They are package-private implementation details, not part of the module's public API surface.

### B. The Bridge Pattern 2.0

Direct exposure of external AI frameworks to the rest of the application is strictly forbidden:

* **Encapsulation**: All third-party AI framework models, prompts, and options must be safely wrapped inside Orasaka-native abstractions (e.g., `ChatRequest`, `Options`).
* **Facade Access**: External modules and clients must interact with the core engine exclusively through the unified `AiClient` facade.

### C. Context-Matrix Orchestration Pipeline

When processing fuzzy queries, execution must flow sequentially through an isolated, ordered chain of independent `PromptInterceptor` beans to achieve user and system multi-tenant prompt enrichment. If disabled (`orasaka.core.orchestration.pipeline.enabled=false`), the engine switches to a zero-allocation bypass.

1. **UserContextResolver (Order 1)**: Dynamically extracts user profile attributes, RBAC security configurations, and rate-limiting tier states from session context tokens.
2. **SystemContextInjector (Order 2)**: Loops over active `SystemContextProvider` implementations to feed real-time environment signals, system variables, active tool arrays, and marketplace trends via Inversion of Control (IoC) without circular dependencies.
3. **RefinerInterceptor (Order 3)**: Resolves fuzzy user questions against active conversation history and compiled context matrices to reformulate them into clear, explicit instructions.
4. **RouterInterceptor (Order 4)**: Evaluates input intent at `temperature: 0.0` to dynamically route the refined request to the optimal model provider under the canonical `orasaka.core` configuration namespace.

---

## 🚀 2. Concurrency Rules & Multi-Module Rebuild Workflow

### A. Non-Blocking Concurrency & Persistence Patterns

* **Java 21 Virtual Threads**: Every blocking action, network invocation, remote model inference, or Vector database retrieval must execute inside un-pinned Virtual Threads utilizing `Executors.newVirtualThreadPerTaskExecutor()`. Never use heavy `synchronized` blocks over I/O loops.
* **Reactive Streams**: Continuous token streaming to clients must utilize native reactive non-blocking structures (`Flux<OrasakaChatResponse>`).
* **Persistence Pattern & I/O Isolation (Formal Mandate)**:
  * **Rule**: Database-query duplication is formally and strictly banned. Service methods must never query the database for records that have been freshly created, updated, or already loaded in memory within the current execution/transaction block.
  * **Model Separation**: Database entities (`*Entity`) must remain strictly isolated and decoupled from clean, immutable domain records (e.g., `User`).
  * **Remediation**: Force the implementation of static or private in-memory domain mappers to assemble the output data layer instantly, cutting database roundtrips by 50%.
* **Ports & Adapters Architecture & Packaging**:
  * **Rule**: Decouple domain logic from web/ingress infrastructure. The `orasaka-identity` and `orasaka-core` packages act as pure Java domain hexagons (Ports) and must remain completely web-agnostic.
  * **Rule**: `orasaka-gateway` acts as the poly-protocol infrastructure adapter layer of the Orasaka ecosystem. All ingress traffic formats (such as REST or GraphQL) must be isolated inside dedicated, flat protocol-driven packages (`rest/`, `graphql/`) within `orasaka-gateway` to prevent protocol bleed.

### B. Fast-Iteration Build Rebuild Workflow

When internal contracts, schema definitions, or module dependencies evolve, the compilation cascade must be strictly respected to prevent dependency staleness inside the target build execution context:

1. **Install and Update Contract Contexts**:
   Command: `mvn clean install -pl orasaka-identity`
   *Use case: Forces the synchronization of core security, cryptography, and data entity schemas into the local repository.*

2. **Recompile the Entire Integration Mesh (Automated Upstream Cascade)**:
   Command: `mvn clean compile -pl orasaka-gateway -am`
   *Use case: The `-am` (also-make) flag forces Maven to analyze and automatically rebuild all modified dependencies (including `orasaka-core` and `orasaka-tools`) that the gateway relies on, eradicating stale code anomalies in a single command.*

---

## 🏛️ 3. Architectural Decisions Log (ADR)

> [!IMPORTANT]
> **ADR Source of Truth**: All Architectural Decision Records are maintained in
> [`docs/CONTEXT.md`](docs/CONTEXT.md). Agents MUST dynamically load and
> enforce all active ADRs from that canonical ledger prior to any code
> generation or architectural review task. The following ADR identifiers are
> currently active and enforceable:
>
> ADR-001 through ADR-024 — see [`docs/CONTEXT.md`](docs/CONTEXT.md) for full
> context, rationale, and approval status.
