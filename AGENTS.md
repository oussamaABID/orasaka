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

### B. The Bridge Pattern 2.0

Direct exposure of external AI frameworks to the rest of the application is strictly forbidden:

* **Encapsulation**: All third-party AI framework models, prompts, and options must be safely wrapped inside Orasaka-native abstractions (e.g., `OrasakaChatRequest`, `OrasakaOptions`).
* **Facade Access**: External modules and clients must interact with the core engine exclusively through the unified `OrasakaAiClient` facade.

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

### ADR-004: Shift to Self-Validating Rich Domain Records via Java 21 Compact Constructors

* **Context**: Moving away from legacy procedural Java 8 coding patterns where services were littered with null-checks, leading to runtime risk and bloated service code.
* **Consequence**: Complete immunity against NullPointerException across the agent pipeline (orasaka-core, orasaka-gateway). Service retention windows are minimized, fully unlocking Virtual Threads performance.
* **Concurrency & Thread Safety**: Mandate short transaction retention windows under Virtual Threads by computing cryptographic operations outside `@Transactional` and letting PostgreSQL handle identity collisions atomically via unique constraints.

### ADR-005: Decoupled Cognitive Core & Open/Closed Interceptor Pipeline

* **Context**: The `orasaka-core` execution engines (`AbstractOrasakaEngine` and `OrasakaEngine`) were directly coupled with concrete components such as `OrasakaToolRegistry`, `McpOrchestrator`, `OrasakaKnowledgeService`, and `OrasakaMemoryResolver`. This violates the Open/Closed Principle and couples the core orchestration engine to specific infrastructure or RAG/MCP logic.
* **Consequence**: Full encapsulation of RAG, MCP, Memory, and Tool calling operations inside concrete `OrasakaContextInterceptor` implementations. The cognitive engine is now purely generic, invoking the pipeline of context interceptors (`List<OrasakaContextInterceptor>`) during the chat execution cycle. This ensures the core engine is decoupled, extensible, and adheres strictly to the Ports & Adapters boundary.
* **Cross-Module Compliance Boundary & Data Purity**:
  * To prevent N+1 queries and manual JSON serialization overhead inside business service layers (e.g. `orasaka-identity`), serialization logic must be offloaded to native JPA Attribute Converters (`@Convert`), and multi-table entity lookups combined using JPQL `LEFT JOIN FETCH` operations to enforce a single atomic database roundtrip.
  * This monorepo-wide data fetch and parsing optimization guarantees microsecond-range database connection windows, preventing connection pool exhaustion and ensuring structural stability and high throughput for the Java 21 Virtual Thread engine.

### ADR-006: Protocol-Driven Workspace Indexing via Externalized MCP Workspace Database

* **Context**: Iterative directory-walking and file-looping across package boundaries inside multi-module repositories are slow, redundant, and exhaust model context windows.
* **Consequence**: Full transition to protocol-driven workspace indexing via an externalized MCP Workspace Database (`orasaka-code-intel`). Direct iterative scanning is strictly banned. Broad architectural review sweeps are driven exclusively by targeting files using semantic coordinates resolved via workspace tools (`search_symbols`, `grep_pattern`).

### ADR-007: Enforcing Self-Validating Domain Records & Anemic Service Orchestration

* **Context**: Application services (`IdentityService`) and core cognitive engines (`AbstractOrasakaEngine`) were previously burdened with procedural data-assembly checks, null-checks, collection fallback mappings, and payload compilation loops. This leaked domain-integrity validation logic from the data layer into the orchestration/service layer, violating the separation of concerns.
* **Consequence**: Universal enforcement of the Self-Validating Domain Records pattern. All operational payloads and domain records (`User`, `OrasakaChatRequest`) must handle state constraints, collection defensive copying (`List.copyOf`, `Map.copyOf`), and fallback parameter defaults (e.g. language fallback) in their **Compact Constructors**. Services and engines are strictly anemic orchestration layers, relying on rich, self-contained domain methods (like `request.compileMessages(...)`) to retrieve compiled payloads ready for downstream consumption.
* **Data Purity**: Complete eradication of procedural conditionals inside `AbstractOrasakaEngine` and `IdentityService` when preparing inputs, guaranteeing immutability and thread-safety under heavy concurrent execution (Virtual Threads).

### ADR-008: Strict Data Component Naming & Record Conventions

* **Context**: Avoiding pseudo-record naming patterns on mutable standard Java classes.
* **Consequence**: All pure data carriers and context transfer contexts must be modeled as immutable Java 21 `record` types. If a component is a standard `public class`, it MUST adhere strictly to JavaBean naming conventions (e.g., using `get` prefixes for accessors). Mixing Record naming patterns (e.g., `userMetadata()`) inside standard mutable classes is strictly banned.

### ADR-009: Collapse Package Architecture & Encapsulation Boundary

* **Context**: Avoiding structural bleed, package-private leakage, and maintaining a high-cohesion API boundary within monorepo modules.
* **Consequence**: Inner classes, orchestrator implementations, and pipeline utility beans must be marked package-private. Only the main entry points (e.g. interfaces, factories, or facades) may be public. All internal mechanisms within a package must be hidden from external access.

### ADR-010: Code Locality, Fluid Cohesion & Unified Frontiers
* **Context**: Traditional Java codebases separate every single class, record, or enum into an independent file, cluttering the directory tree and breaking cognitive focus.
* **Consequence**: Maximize Java 21 file density. Group tightly coupled records, sealed hierarchies, and inner implementation steps inside the same file boundary using nested types or package-private inline structures. This leads to shorter, cleaner file structures, faster navigation, and immediate context visibility for the engineer.
* **Unified Frontiers Expanded Scope**: Application edge boundaries must present a unified facade. Protocols are just transport details; the domain component is the true owner. Split packages by technical transport protocols (e.g. `rest` and `graphql`) are strictly prohibited; entry points must reside in a unified `.endpoint` package boundary, and interconnected data transport objects (DTOs) must maintain ultimate density to prevent file-tree explosion by grouping them inline within singular container contract files (e.g., `AuthContracts.java`).

### ADR-011: Absolute Prohibition of Direct Environment Sniffing in Functional Beans [ERR-113]
* **Context**: Injecting `org.springframework.core.env.Environment` into functional components, filters, services, interceptors, or controllers creates fragile coupling to raw configuration state and Spring profile names.
* **Consequence**: Injecting `Environment` or calling `.getProperty()`/`.acceptsProfiles()` inside code bodies is strictly FORBIDDEN. All functional components must remain completely oblivious to raw environment variables and Spring profile names. Configuration mapping must be managed exclusively at the configuration bootstrap layer using immutable, type-safe Record structures populated via Spring's programmatic Binder matrix. Components must receive clean, pre-resolved configuration properties through explicit constructor dependency injection.


