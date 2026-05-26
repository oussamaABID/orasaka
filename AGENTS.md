# Orasaka — System and Agent Governance Contract

> This document is the **single source of truth** for architectural boundaries, coding standards, and LLM sub-agent compliance rules across the entire Orasaka monorepo. Every code generation, review, or refactoring action **must** enforce these constraints without exception.

---

## 1. Execution Contract & CLI

* **Rule Enforcement**: Load and enforce rules in `.agent/rules/*` and workflows in `.agent/workflows/*` before any code generation.
* **CLI Invariant (ERR-125)**: No shell scripts (`.sh` / `.bat`) for setup or operations. All automation **must** run via `npx orasaka` subcommands.
* **Development Stack**: `npx orasaka dev` is the canonical command for launching the full development environment. It parallel-spawns Gateway, Web Client, Admin Console, and Mobile App with graceful shutdown.

---

## 2. Module Architecture & Separation (ERR-102)

### Backend Modules

| Module | Boundary Rule | Banned Imports |
|:---|:---|:---|
| `orasaka-core` | Web-agnostic stateless engine. Bridge Spring AI via `AiClient` facade. | Web starters, Spring Security context, AMQP |
| `orasaka-identity` | Pure Java user domain — RBAC, BCrypt, OAuth2. | `spring-boot-starter-web`, any web dependency |
| `orasaka-business` | Business prompt templates and domain context (`SovereignWorkflowContext`). | Web dependencies, Spring Security |
| `orasaka-tools` | Tool interfaces, MCP wrappers, multi-tier cache. | `orasaka-identity` |
| `orasaka-gateway` | **Sole** BFF adapter referencing both `orasaka-identity` and `orasaka-core`. Passes `String userId` to core/tools. | — |
| `orasaka-interceptors` | Independent Maven submodules under `orasaka-framework/orasaka-interceptors/`. | — |

### Core Interceptor Purification (ERR-122)

`orasaka-framework/orasaka-core/application/interceptor/` must contain **only** the `PromptContextInterceptor` interface. All implementations live in `orasaka-framework/orasaka-interceptors/` and register via AutoConfiguration SPI.

### Client Tier Workspace Boundary

**All** front-end and client-side applications reside under `orasaka-apps/orasaka-ui/` as npm workspaces:

| Package | Role | Port |
|:---|:---|:---:|
| `orasaka-web-client` | Client-facing Next.js 16 App Router application | 3000 |
| `orasaka-web-admin` | Isolated SecOps Administration Console | 3001 |
| `orasaka-mobile-client` | Expo SDK 53 cross-platform mobile app (6-screen SaaS boilerplate) | 8081 |
| `orasaka-cli` | Developer automation CLI with offline SQLite job queue | — |
| `orasaka-shared` | Shared TypeScript types + Zod validation schemas | — |

**Banned**:
- Creating standalone JS/TS client applications outside `orasaka-apps/orasaka-ui/`.
- Duplicating TypeScript interfaces that exist in `orasaka-shared`.
- Importing types directly between client packages — always route through `orasaka-shared`.
- Using `workspace:*` protocol in `package.json` — use `"*"` for native npm workspace resolution.

**Mandatory**: All client packages must declare `"orasaka-shared": "*"` as a dependency and validate types against shared Zod schemas.

---

## 3. Backend Standards

### 3.1 Structuring & Isolation (ERR-103)

* One top-level class/interface/enum/record per `.java` file. 1:1 test file matching required.
* Class suffixes: `Service`, `ServiceImpl` (package-private), `Interceptor`, `Resolver`, `Controller`, `Mapper`, `Repository`, `Config`, `Properties`.
* Banned: Prefixing classes with `Orasaka` or `orasaka-` (ERR-104).

### 3.2 Domain Contracts & Validation (ERR-106)

* **Self-Validating Records**: Java records for DTOs and domain objects. Verify boundaries in compact constructors.
* **Owner Validates Field (ERR-116)**: The record/class owning a field validates it. Banned: inline `if (dto.field() != null)` guards in services.
* **Mapper Isolation (ERR-107)**: Mapping logic in package-private `final class *Mapper` using static methods. Banned: setter chains > 5 lines in controllers, services, or engines.

### 3.3 Persistence & DB Invariants (ERR-109)

* Banned: Raw SQL strings — use parameterized Repos or `@Query`.
* Banned: Read-before-write logic — catch `DataIntegrityViolationException` for unique constraints.
* Banned: DB query duplication — repo write methods must return domain records directly.
* Transactions: Run hash/crypto **outside** `@Transactional`. Write methods must not call internal reads.
* Segregated Ingress (ERR-112): GraphQL, REST, and AMQP handlers in separate sub-packages (`adapter.graphql/`, `adapter.rest/`, `adapter.amqp/`). Mixing in flat packages is banned.

### 3.4 Clients & Concurrency (ERR-120)

* HTTP Calls: Enforce Spring `RestClient` or `@HttpExchange` proxies. Banned: manual clients (e.g., `java.net.http.HttpClient`). Use `ByteArrayResource` in `MultiValueMap` for uploads.
* Concurrency: Run blocking I/O, remote calls, and DB access on Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`). Banned: `synchronized` over I/O. Use `ConcurrentHashMap` / `AtomicReference`.

---

## 4. System Layer Governance

### Business vs. Core Separation

| Layer | Responsibility | Artifacts | Location |
|:---|:---|:---|:---|
| **Business** | **"What"** — intentions, rules, policies, business prompts | `SovereignWorkflowContext`, `.md` persona templates, interceptor policy sets | `orasaka-framework/orasaka-business/` |
| **Core** | **"How"** — execution mechanics, caching, fallback topology | `DynamicPipelineExecutor`, `.st` engine templates, `PromptContext` state machine | `orasaka-framework/orasaka-core/` |

### SovereignWorkflowContext → Core Mapping

The Gateway's `SovereignWorkflowAdapter` translates business context into Core infrastructure types using these namespaces:

| Namespace | Purpose |
|:---|:---|
| `orasaka.pipeline.*` | Pipeline execution directives (contextId, forced/skipped interceptors) |
| `orasaka.user.*` | User-level attributes (tier, RBAC metadata) |
| `orasaka.user.meta.*` | Flattened business metadata entries |

**Absolute invariants**:
- Business code **never** imports `com.orasaka.core.*` types.
- Core code **never** contains business logic or domain rules.
- The Gateway is the **only** translation boundary between hexagons.
- Constructing `ChatRequest`, `Context`, or `PromptContext` directly from business code is **banned**.

### Prompt Template Segregation

| Type | Location | Governance |
|:---|:---|:---|
| Engine templates (`.st`) | `orasaka-core/src/main/resources/prompts/` | Allow-listed: `context-envelope.st`, `system-refinement.st`, `system-router.st` only (ERR-125) |
| Business templates (`.md`) | `orasaka-business/src/main/resources/prompts/` | Unrestricted business persona templates |

Adding any `.md` file to `orasaka-core/src/main/resources/prompts/` will fail the ArchUnit resource isolation test.

---

## 5. Context-Matrix Pipeline

Disable the full pipeline via `orasaka.core.orchestration.pipeline.enabled=false`.

| Order | Interceptor | Submodule | AI-Dep | Purpose |
|:---:|:---|:---|:---:|:---|
| 1 | `UserContextResolver` | `interceptor-context` | No | Load user profile, RBAC tier |
| 2 | `SystemContextInjector` | `interceptor-context` | No | Environment signals, active tools, system metadata |
| 3 | `LanguageAlignmentInterceptor` | `interceptor-translation` | No | Force LLM reasoning in English |
| 5 | `MemoryInterceptor` | `interceptor-enrichment` | No | FIFO conversation history prepend |
| — | `RagInterceptor` | `interceptor-enrichment` | No | Tenant-isolated vector store RAG context |
| — | `McpInterceptor` | `interceptor-enrichment` | No | Resolve external MCP server tools/data |
| 6 | `RefinerInterceptor` | `interceptor-reformulation` | Yes | Refine user query into precise instruction |
| 7 | `RouterInterceptor` | `interceptor-reformulation` | Yes | Classify intent and route to optimal model |
| — | `ToolInterceptor` | `interceptor-tooling` | No | Dynamic tool registration and callbacks |
| 9 | `CostShieldInterceptor` | `interceptor-validation` | No | Auto-shift to cloud if local memory > 85% |
| ∞ | `QuantumValidationAdvisor` | `interceptor-validation` | Yes | 4-tier closed-loop validation |

---

## 6. Local Agent Protocol & CLI Fault-Tolerance (ERR-130)

* **Transaction Store**: CLI persists jobs locally via SQLite (`~/.orasaka-tasks.db`), table `orasaka_local_jobs`.
* **Atomic Ingestion**: CLI writes payload as `PENDING` to SQLite before spawning subprocesses.
* **Heartbeat Sync**: CLI runs a 15-second heartbeat to sync jobs to Gateway `POST /api/v1/agent/report`.
* **SSE Tunnel**: Gateway dispatches tasks via SSE (`dispatchId`); CLI returns status via `POST /api/v1/agent/report`.

---

## 7. Next.js & Frontend Standards

* **Stack**: Next.js 16 (App Router) + React 19 + Tailwind CSS 4 + Lucide Icons.
* **Design System**: Cinematic dark-mode first. Frosted panels use `backdrop-filter: blur(16px)` with HSL variables. Banned: inline hex colors or hardcoded Tailwind color classes.
* **BFF Ingress**: Browser must **never** request Gateway (`:8080`) or Ollama (`:11434`) directly. All traffic proxied through Next.js server API routes.
* **Component Length**: Maximum 250 lines per file. Extract inline `.map()` loops to sub-components.
* **Date Formatting (ERR-108)**: Use `date-fns` exclusively. Banned: moment.js, dayjs, luxon, and native `.toLocaleTimeString()`, `.toISOString()`, `.getMonth()`, `.getDate()`.
* **Input Blocking (ERR-126)**: Text entry surfaces must lock when `isSending || isGenerating`. Disable textareas, menus, attachments, and submit buttons.
* **Form Event Typing**: Form `onSubmit` must use React 19+ native types (`React.SubmitEventHandler<HTMLFormElement>` or `React.SubmitEvent<HTMLFormElement>`). Banned: `React.FormEvent`.
* **Icon Registry**: Centralized Lucide icons from `Icon.tsx`. Banned: direct imports from external icon libraries.

---

## 8. Mobile Standards (Expo SDK 53)

* **Navigation**: React Navigation 7 with typed `RootStackParamList` — 6 screens: Login, Register, ForgotPassword, ResetPassword, ChatStream, Subscription.
* **Shared Types**: All TypeScript types imported from `orasaka-shared` — no local type duplication.
* **BFF Proxy**: Mobile client routes all API calls through the Next.js BFF proxy, never directly to Gateway.
* **Input Blocking**: Same `isSending || isGenerating` lock pattern as web client (ERR-126).

---

## 9. Testing & Infrastructure Invariants

* **Testcontainers (ADR-033/034)**: Integration tests must extend `AbstractContainerIntegrationTest` to bootstrap PostgreSQL, Redis, and RabbitMQ once per JVM process. Dynamic ports mapped via `@DynamicPropertySource`.
* **MLX Frame Clamping**: Apple Silicon MLX processing capped at 7–28 frames. Memory threshold breach shifts execution to cloud (CostShield).
* **ArchUnit Governance**: `GovernanceTest` (a.k.a. `GlobalArchitectureGuardrailIT`) validates all ring rules on every build. Pre-commit: `./mvnw test -pl orasaka-framework/orasaka-core -Dtest=GovernanceTest`.

---

## 10. MCP Context & Token Management

* At the start of every session, the agent must execute a full handshake with the active Filesystem/Directory MCP server (`orasaka-filesystem-agent`) to synchronize its state with the codebase architecture.
* Do not read individual files repeatedly if unchanged. Leverage structured semantic mapping and directory indexing via MCP tools to preserve raw context continuity, ensuring optimal utilization of LLM Prompt Caching.
* Abandon any segmented/chunked external vector DB logic (Pinecone RAG) to maximize Claude's native Prompt Caching efficiency (which requires absolute context continuity).
* The MCP server entrypoint is `.agent/mcp-server/index.js`, delegating to `@modelcontextprotocol/server-filesystem`.
* The single MCP client configuration lives in `.agent/config/mcp.json`. No other MCP directories (e.g., `.agent/mcp/`) are permitted. Discover local Antigravity MCP tools and use them when available.
