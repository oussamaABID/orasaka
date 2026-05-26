# Orasaka: System and Agent Governance

## 1. Execution Contract & CLI
* **Rule Enforcement**: Load/enforce rules in `.agent/rules/*` & workflows in `.agent/workflows/*` before gen.
* **CLI Invariant (ERR-125)**: No shell scripts (.sh/.bat) for setup/ops. Must run via `npx orasaka` subcommands.

## 2. Module Architecture & Separation (ERR-102)
* **orasaka-core**: Web-agnostic. No web starters, spring security context, or AMQP. Bridge Spring AI via facades.
* **orasaka-interceptors**: Grouped as independent Maven submodules under `orasaka-framework/orasaka-interceptors/`.
* **orasaka-identity**: Pure Java domain. Banned: web dependencies (e.g. `spring-boot-starter-web`).
* **orasaka-gateway**: Entry BFF adapter. Sole module referencing both `orasaka-identity` and `orasaka-core`. Passes `String userId` to core/tools.
* **orasaka-tools**: Tool interfaces & MCP wrappers. Banned: importing `orasaka-identity`.
* **orasaka-business**: Prompt templates library. Banned: web dependencies & spring security.
* **Core Interceptor Purification (ERR-122)**: `orasaka-framework/orasaka-core/application/interceptor/` must only contain the `PromptContextInterceptor` interface. Implementations live in `orasaka-framework/orasaka-interceptors/` and register via auto-config SPI.

## 3. Backend Standards
### 3.1 Structuring & Isolation (ERR-103)
* One top-level class/interface/enum/record per `.java` file. 1:1 test file matching required.
* Class suffixes: `Service`, `ServiceImpl` (package-private), `Interceptor`, `Resolver`, `Controller`, `Mapper`, `Repository`, `Config`, `Properties`.
* Banned: Prefixing classes with `Orasaka` or `orasaka-` (ERR-104).

### 3.2 Domain Contracts & Validation (ERR-106)
* **Self-Validating Records**: Java records for DTOs/domain. Verify boundaries in compact constructors.
* **Owner Validates Field (ERR-116)**: Record/class owning field validates it. Banned: inline `if (dto.field() != null)` in services.
* **Mapper Isolation (ERR-107)**: Mapping logic in package-private `final class *Mapper` using static methods. Banned: setter chains > 5 lines in controllers/services/engines.

### 3.3 Persistence & DB Invariants (ERR-109)
* Banned: Raw SQL strings. Use parameterized Repos or `@Query`.
* Banned: Read-before-write logic. Catch `DataIntegrityViolationException` for unique constraints.
* Banned: DB query duplication. Repo write methods must return domain records directly.
* Transactions: Run hash/crypto outside `@Transactional`. Write methods must not call internal reads.
* Segregated Ingress (ERR-112): GraphQL, REST, AMQP handlers in separate sub-packages (`adapter.graphql/`, `adapter.rest/`, `adapter.amqp/`). Mixing in flat packages is banned.

### 3.4 Clients & Concurrency (ERR-120)
* HTTP Calls: Enforce Spring `RestClient` or `@HttpExchange` proxies. Banned: manual HTTP clients (e.g. `java.net.http.HttpClient`). Use `ByteArrayResource` in `MultiValueMap` for uploads.
* Concurrency: Run blocking I/O, remote calls, and DB on Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`). Banned: `synchronized` over I/O. Use `ConcurrentHashMap`/`AtomicReference`.

## 4. Context-Matrix Pipeline
Disable via `orasaka.core.orchestration.pipeline.enabled=false`. Imports order:

| Order | Interceptor | Submodule | AI-Dep | Purpose |
|:---:|:---|:---|:---:|:---|
| 1 | `UserContextResolver` | `interceptor-context` | No | Load user profile, RBAC tier |
| 2 | `SystemContextInjector` | `interceptor-context` | No | Env signals, active tools, system metadata |
| 3 | `LanguageAlignmentInterceptor` | `interceptor-translation` | No | Force LLM reasoning in English |
| 5 | `MemoryInterceptor` | `interceptor-enrichment` | No | FIFO conversation history prepend |
| - | `RagInterceptor` | `interceptor-enrichment` | No | Tenant-isolated vector store RAG context |
| - | `McpInterceptor` | `interceptor-enrichment` | No | Resolve external MCP server tools/data |
| 6 | `RefinerInterceptor` | `interceptor-reformulation` | Yes | Refine query into precise instruction |
| 7 | `RouterInterceptor` | `interceptor-reformulation` | Yes | Classify intent and route model category |
| - | `ToolInterceptor` | `interceptor-tooling` | No | Dynamic tool registration & callbacks |
| 9 | `CostShieldInterceptor` | `interceptor-validation` | No | Shift to cloud if local memory > 85% |
| Inf | `QuantumValidationAdvisor` | `interceptor-validation` | Yes | 4-tier closed-loop validation |

## 5. Local Agent Protocol & CLI Fault-Tolerance (ERR-130)
* **Transaction Store**: CLI persists jobs locally via SQLite (`~/.orasaka-tasks.db`), table `orasaka_local_jobs`.
* **Atomic Ingestion**: CLI writes payload as `PENDING` to SQLite before spawning subprocesses.
* **Sync**: CLI runs 15-second heartbeat to sync jobs to Gateway `/api/v1/agent/report`.
* **SSE Tunnel**: Gateway dispatches tasks via SSE (`dispatchId`); CLI returns status via `POST /api/v1/agent/report`.

## 6. Next.js & Frontend Standards
* **Stack**: Next.js 16 (App Router) + React 19 + Tailwind CSS 4 + Lucide Icons.
* **Design System**: Cinematic dark-mode first. Frosted panels use `backdrop-filter: blur(16px)` & HSL variables. Banned: inline hex/ Tailwind colors.
* **BFF Ingress**: Browser must never request Gateway (8080) or Ollama (11434) directly. Proxy all through Next.js server API routes.
* **Component Length**: Max file length 250 lines. Extract inline `.map()` loops to sub-components.
* **Date Formatting (ERR-108)**: Use `date-fns` exclusively. Banned: moment.js, dayjs, luxon, and native `.toLocaleTimeString()`, `.toISOString()`, `.getMonth()`, `.getDate()` on Dates.
* **Input Blocking (ERR-126)**: Text entry surface must lock when `isSending || isGenerating`. Disable textareas, menus, attachments, submit buttons.
* **Form Event Typing**: Form `onSubmit` must use React 19+ native types (`React.SubmitEventHandler<HTMLFormElement>` or `React.SubmitEvent<HTMLFormElement>`). Banned: `React.FormEvent`.

## 7. Testing & Infrastructure Invariants
* **Testcontainers (ADR-033/034)**: Integration tests must extend `AbstractContainerIntegrationTest` to bootstrap PostgreSQL, Redis, and RabbitMQ once per JVM process. Dynamic ports mapped via `@DynamicPropertySource`.
* **MLX Frame Clamping**: Apple Silicon MLX processing capped at 7-28 frames. Memory threshold breach shifts execution to cloud (CostShield).
* **Registry Icons**: Centralized Lucide icons from `Icon.tsx`. Banned: direct imports of external icon libraries.

## 8. MCP Context & Token Management
* At the start of every session, the agent must execute a full handshake with the active Filesystem/Directory MCP server (`orasaka-filesystem-agent`) to synchronize its state with the codebase architecture.
* Do not read individual files repeatedly if unchanged. Leverage structured semantic mapping and directory indexing via MCP tools to preserve raw context continuity, ensuring optimal utilization of LLM Prompt Caching.
* Abandon any segmented/chunked external vector DB logic (Pinecone RAG) to maximize Claude's native Prompt Caching efficiency (which requires absolute context continuity).
* The MCP server entrypoint is `.agent/mcp-server/index.js`, delegating to `@modelcontextprotocol/server-filesystem`.
* The single MCP client configuration lives in `.agent/config/mcp.json`. No other MCP directories (e.g. `.agent/mcp/`) are permitted. discover local Antigravity MCP tools and use them when available.
