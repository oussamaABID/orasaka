# 🥷 ORASAKA: SYSTEM & AGENT GOVERNANCE

> [!IMPORTANT]
> **AGENT EXECUTION CONTRACT & CONTEXT LOADING ORDER**
>
> 1. **Pre-requisite**: Dynamically load and enforce all definitions in `.agent/rules/*` and `.agent/workflows/*` prior to any code generation.
> 2. **Rule Aggregation**: Local rules in `.agent/` are strict, non-negotiable extensions of this ledger.
> 3. **Validation Gate**: Every task must satisfy both local manifests and this ledger simultaneously.

---

## §1 — Architecture Invariants

### 1.1 Hexagonal Module Topology

```

┌─────────────────────────────────────────────────────────────────┐
│ orasaka-gateway (Adapter Layer)                                 │
│  ├─ adapter.rest/   ← REST/SSE controllers                      │
│  ├─ adapter.graphql/ ← GraphQL resolvers                        │
│  └─ adapter.amqp/   ← Async message listeners                   │
├─────────────────────────────────────────────────────────────────┤
│ orasaka-core (Domain Hexagon — AI Orchestration)                │
│  ├─ domain.ports.inbound/  ← AiClient facade                    │
│  ├─ domain.ports.outbound/ ← Provider interfaces                │
│  ├─ domain.model/          ← Immutable records                  │
│  ├─ application.engine/    ← Abstract execution blueprints      │
│  ├─ application.pipeline/  ← Interceptor chain + utilities      │
│  └─ infrastructure.adapter.ai/ ← Concrete AI clients            │
├─────────────────────────────────────────────────────────────────┤
│ orasaka-identity (Domain Hexagon — User & Auth)                 │
│  ├─ domain/                ← User, Role, Credential models      │
│  ├─ application.service/   ← Auth logic (package-private impl)  │
│  └─ infrastructure.persistence/ ← JPA adapters                  │
├─────────────────────────────────────────────────────────────────┤
│ orasaka-tools (Adapter — Tool Registry & MCP)                   │
│ orasaka-persistence-* (Adapter — Data Access)                   │
│ orasaka-automation-worker (Quartz + AMQP Job Scheduler)         │
│ orasaka-ui (Next.js BFF + React SPA)                            │
│ orasaka-cli (TypeScript CLI client)                             │
│ orasaka-video-worker (Python — Stable Video Diffusion)          │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Module Separation Invariant [ERR-102]

| Rule | Description |
|:---|:---|
| **Stateless Core** | `orasaka-core` depends only on `spring-ai-core`. No web starters, no security, no sessions. |
| **Identity Isolation** | `orasaka-identity` is a pure Java domain hexagon. No `spring-boot-starter-web`. |
| **Gateway Rule** | Only `orasaka-gateway` may call identity services and inject context into core. |
| **Tools Adapter** | `orasaka-tools` may implement `orasaka-core` interfaces but must never import `orasaka-identity`. |
| **Context Passing** | Core/tools consume user refs as primitive strings (`String userId`), never security tokens or domain objects. |
| **No Reverse Flow** | `core → tools` is strictly prohibited. |

### 1.3 Class Leakage Prevention

- `org.springframework.ai` types must be encapsulated inside `orasaka-core`. They must never leak into gateway, CLI, or identity.
- All third-party AI models, prompts, and options are wrapped in Orasaka-native abstractions (**Bridge Pattern 2.0**).
- External modules interact with AI exclusively through the `AiClient` facade.

---

## §2 — Java/Spring Backend Standards

### 2.1 Source File Isolation [ERR-103]

- Every top-level class, interface, enum, or record resides in its own `.java` file matching its simple name.
- Multi-class bundling is prohibited. Each production class has its own 1:1 test file.
- Enforced by ArchUnit `one_top_level_class_per_file` in all modules.

### 2.2 Naming [ERR-104]

- **Zero-Prefix Rule**: `Orasaka`/`orasaka-` prefix on internal artifacts is forbidden across all layers.
- **Backend**: Use package topology for ownership. Use `RagInterceptor`, not `OrasakaRagInterceptor`.
- **Frontend**: Use `Sidebar`, not `OrasakaSidebar`.
- **Suffixes**: Classes must carry standard behavioral suffixes (`*Service`, `*Interceptor`, `*Resolver`, `*Controller`, `*Mapper`, `*Repository`).

### 2.3 Interface-Driven Boundaries [ERR-105]

- Cross-module interaction happens through interfaces only. `*ServiceImpl` classes are package-private.
- OAuth2 providers are conditionally loaded via `@ConditionalOnProperty` — disabled providers produce zero beans.

### 2.4 Self-Validating Records [ERR-106] & Owner-Validates-Field [ERR-116]

- DTOs and domain records validate in compact constructors. No procedural null-checking in services.
- **Owner-Validates-Field**: The record/class that owns a field is the sole authority for validating it. External callers must never `null`-check or validate fields that belong to another type — they trust the constructor invariant.
  - ✅ `Context` validates `userId` and `conversationId` in its own constructor.
  - ✅ `AiRequest.requireValid(prompt, context)` validates the shared interface contract.
  - ❌ `if (request.context() != null)` in a service — **banned**. Context is guaranteed non-null by construction.
- Favor Java `record` over `class` for immutable domain DTOs.
- Service identity methods (e.g., `authenticate`) throw explicit exceptions, never return `null`.
- Collections use defensive copies (`Map.copyOf`, `List.copyOf`).
- Use `Context.anonymous()` for test/internal pipeline scenarios — never pass `null`.

### 2.5 Mapper Isolation [ERR-107]

- Field-by-field mapping is forbidden in controllers, services, and engines.
- All mapping logic lives in package-private `final class *Mapper` with `static` methods.
- Call sites reduce to single-line invocations: `UserMapper.toEntity(profile, userId, provider, "free")`.

### 2.6 Persistence Layout [ERR-109]

- `AttributeConverter` implementations reside in `*.infrastructure.persistence.converter.*`.
- Entity sub-packages hold only structural DB schema mapping classes.
- Raw SQL strings are banned — use Spring Data JPA Repositories or `@Query`.
- Database-query duplication is banned. Write methods return domain records directly.
- Read-before-write patterns are prohibited. Use DB UNIQUE constraints + `DataIntegrityViolationException`.

### 2.7 Ingress Protocol Segregation [ERR-112]

- REST, GraphQL, and AMQP handlers must live in isolated sub-packages: `adapter.rest/`, `adapter.graphql/`, `adapter.amqp/`.
- No protocol mixing in flat packages.

### 2.8 Spring-First DI & Configuration

- `ApplicationContext` is the sole source of truth for AI routing and security state.
- Models resolved via constructor-injected Spring AI interfaces.
- No `@Autowired` field injection — constructor DI only.
- No `org.springframework.core.env.Environment` injection in functional beans [ERR-113].
- Configuration uses immutable `record` types via `@ConfigurationProperties`.

### 2.9 Concurrency (Java 21)

- Blocking actions execute on Virtual Threads via `Executors.newVirtualThreadPerTaskExecutor()`.
- No `synchronized` blocks over I/O. Use `ConcurrentHashMap`, `AtomicReference`.
- Reactive streaming uses `Flux<ChatResponse>` — no simulated streaming via `Thread.sleep`.
- Hashing/crypto executes outside `@Transactional` blocks.

### 2.10 Context-Matrix Orchestration Pipeline

Full 9-stage interceptor chain for prompt enrichment (disable via `orasaka.core.orchestration.pipeline.enabled=false`):

| Order | Interceptor | Responsibility |
|:---:|:---|:---|
| 1 | `UserContextResolver` | User profile, RBAC, rate-limiting tier |
| 2 | `SystemContextInjector` | Environment signals, tools, system vars |
| 3 | `RagInterceptor` | Vector store retrieval → context injection |
| 4 | `McpInterceptor` | External MCP knowledge resolution |
| 5 | `MemoryInterceptor` | Conversation history prepend (FIFO window) |
| 6 | `RefinerInterceptor` | Fuzzy query → precise instruction |
| 7 | `RouterInterceptor` | Intent → optimal provider (temp: 0.0) |
| 8 | `ToolInterceptor` | Tool callback attachment (demand-driven) |
| 9 | `MediaInterceptor` | Base64 media extraction & multimodal assembly |

### 2.11 Database Migrations

- Schema changes automated via versioned Flyway migrations.
- Manual `ALTER TABLE` on startup is forbidden.
- Migration files follow `V{N}__{description}.sql` naming.
- Every migration must be idempotent where possible (`CREATE INDEX IF NOT EXISTS`).

### 2.12 Modular Configuration Files

| File | Scope |
|:---|:---|
| `application.yml` | Datasource, Redis, Flyway, GraphQL, CORS, OAuth2 |
| `ai-models.yml` | Spring AI, Ollama, LocalAI, video endpoints |
| `identity-core.yml` | Validation, onboarding schemas |
| `playground-features.yml` | Feature flags, SDUI capability templates |

### 2.13 Automation Worker Isolation [ERR-117]

- `orasaka-automation-worker` is a standalone Spring Boot module on port `8082`.
- It depends on `orasaka-core` (domain model + outbound ports) and `orasaka-persistence-chat` (JPA repositories) — **never** on `orasaka-gateway`.
- The worker communicates with the gateway exclusively via AMQP (RabbitMQ exchanges), never via HTTP.
- Job definitions use Quartz `@DisallowConcurrentExecution` to prevent overlapping triggers.
- Schema migrations for `qrtz_*` tables are managed by Quartz's auto-DDL, not Flyway.
- Health check exposed at `/actuator/health` (port `8082`).

### 2.14 Reverse AMQP Tunneling Protocol [ERR-118]

- The automation worker publishes job results to RabbitMQ exchange `orasaka.jobs.results`.
- The gateway's `adapter.amqp/` listeners consume from `orasaka.jobs.results` queue.
- Messages use `application/json` content type with `correlationId` matching the originating job ID.
- Dead-letter routing: failed messages route to `orasaka.jobs.dlx` with a TTL of 3600s.
- No direct database writes from AMQP listeners — delegate to `*Service` layer.

### 2.15 Approval State Machine Invariants [ERR-119]

- Job state transitions follow a strict finite state machine: `PENDING → RUNNING → COMPLETED | FAILED`.
- Only the owning `*Service` may transition state — controllers and listeners must delegate.
- State transitions are atomic (single `UPDATE` with `WHERE status = ?` optimistic lock).
- Terminal states (`COMPLETED`, `FAILED`) are immutable — no re-transitions.
- All state changes emit a Spring `ApplicationEvent` for audit logging.

### 2.16 HTTP Client Standards [ERR-120]

- All HTTP calls in `orasaka-core` and `orasaka-tools` MUST use Spring `RestClient` or declarative `@HttpExchange` interface proxies.
- `java.net.http.HttpClient`, `java.net.HttpURLConnection`, and manual multipart boundary forging via `PrintWriter`/`ByteArrayOutputStream` are **permanently banned**.
- Multipart file uploads use Spring's `ByteArrayResource` within `MultiValueMap<String, Object>` payloads — zero manual boundary construction.
- Stream operations use `StreamUtils` (Spring) or `IOUtils` (Apache Commons IO). JSON queries use auto-configured Jackson `ObjectMapper`.
- Connection pooling: `RestClient` instances share a `ClientHttpRequestFactory` (e.g., Apache HttpClient 5) for socket reuse.
- **Exception**: MCP SDK transport adapters (`HttpClientSseClientTransport`) are framework-owned and explicitly excluded.

---

## §3 — Next.js/React Frontend Standards

### 3.1 Component Architecture

- **250-line limit**: No `.tsx` file may exceed 250 lines. Extract sub-components.
- **Inline loops**: Non-trivial `.map()` JSX blocks must be extracted into named components.
- **State**: Server operations use `@tanstack/react-query` exclusively.
- **Date operations**: Use `date-fns` (immutable) exclusively for all chronological computations [ERR-108].
  - ✅ `format(parseISO(timestamp), "HH:mm:ss")` — date-fns formatting.
  - ✅ `formatDistanceToNow(parseISO(timestamp))` — relative time.
  - ✅ `formatISO(subMinutes(new Date(), 5))` — ISO string creation.
  - ✅ `Date.now()` — epoch-ms for internal IDs and state (not display).
  - ❌ `new Date(x).toLocaleTimeString()` — **banned**. Native locale formatting.
  - ❌ `new Date().toISOString()` — **banned**. Use `formatISO()` from date-fns.
  - ❌ `new Date(x).getMonth()` / `.getFullYear()` — **banned** for rendering. Use date-fns `getMonth()` / `getYear()`.
  - **Exception**: `new Date().getFullYear()` for static copyright year is trivially acceptable.
- **Form Event Typing [ERR-121]**: All form `onSubmit` handlers must use React 19+ native event types exclusively.
  - ✅ `const handleSubmit: React.SubmitEventHandler<HTMLFormElement> = (event) => { ... }` — Pattern A (function declarations).
  - ✅ `(e: React.SubmitEvent<HTMLFormElement>) => { ... }` — Pattern B (inline / explicit event args).
  - ❌ `React.FormEvent<HTMLFormElement>` — **banned**. Deprecated legacy synthetic abstraction per `@types/react` 19+.
  - ❌ `React.SyntheticEvent<HTMLFormElement>` — **banned**. Overly generic fallback that defeats React 19's native event mapping precision.
  - ❌ Untyped or `any`-typed event handlers — **banned**.
  - Sonar rule `typescript:S1874` is suppressed project-wide — the deprecation is enforced at the governance level, not the linter level.

### 3.2 i18n Externalization [ERR-115]

- All user-facing strings must be registered in the typed `TranslationDictionary`.
- Hardcoded text in `.tsx` files (including catch blocks, validation, alerts) is forbidden.
- Both `en` and `fr` locales must have identical key structures.
- Enforced by automated test: `translations.test.ts`.

### 3.3 Design Token System

- Colors, spacing, typography, and animations use CSS custom properties.
- No inline hex colors or pixel values — reference design tokens only.
- Dark mode support via `prefers-color-scheme` and class toggling.

### 3.4 Accessibility (WCAG 2.1 AA)

- All interactive elements must have unique, descriptive `id` attributes.
- Form inputs must have associated `<label>` elements.
- Color contrast ratio ≥ 4.5:1 for text.
- Keyboard navigation support for all interactive flows.

### 3.5 BFF Security

- Browser never calls backend port `8080` or Ollama port `11434` directly.
- All requests proxy through Next.js API routes with session validation.
- BFF injects `Authorization: Bearer <userId>` downstream.

---

## §4 — Python Standards & Code Quality

### 4.1 Code Style & Linting

- **PEP 8 Compliance**: Strict alignment with PEP 8 style guide conventions.
- **Type Hints**: Strict PEP 484 type annotations for all function parameters and return types. No `Any` without explicit justification.
- **Naming**: `snake_case` for functions, methods, and variables; `PascalCase` for classes; `UPPER_CASE` for constants.
- **Package Management**: Virtual environments managed via `requirements.txt` or `pyproject.toml`.
- **Ruff & Black**: Code must be checked with `ruff check` and formatted using `black` or `ruff format`.
- **No Unused Code**: Dead code, unused imports, or unused variables must be purged.

### 4.2 Error Handling & Logging

- **No Bare Except**: Banned use of bare `except:` clauses. Always catch specific exceptions (e.g., `except ValueError:`, `except Exception:` with logging).
- **Production Logging**: Banned use of `print()` for operational logging. Standard Python `logging` module must be used.
- **Traceback Preservation**: When logging exceptions inside `except` blocks, use `logger.exception()` to preserve full stack trace context.

### 4.3 Design Patterns & Resource Management

- **Mutable Default Arguments**: Banned mutable defaults in function signatures (e.g., `def func(x=[])` or `def func(x={})`). Use `x: list | None = None` and initialize inside the function.
- **Dependency Isolation**: Workers and test scripts must never dynamically install packages at runtime; all dependencies must be declared in `requirements.txt` or `pyproject.toml`.
- **Path Management**: Use `pathlib.Path` instead of raw string concatenation or manual slicing for directory/file system resolution.
- **Resource Cleanups**: Always use context managers (e.g., `with` statements) for files, socket connections, and network resources to prevent file descriptor leaks.
- **Standard Exit Codes**: Scripts must return standard exit codes (0 for success, non-zero for failure) to ensure automation pipelines detect failure correctly.
- **Testing Standard**: Every worker endpoint or business logic unit must have corresponding unit tests (using `pytest` or `unittest`) aiming for a 1:1 file ratio where possible.
- **Stateless Worker Invariant**: `orasaka-video-worker` must remain stateless, run on port `8188` with Metal/CUDA acceleration, return RFC 2397 Data URLs, and provide a `/health` endpoint.

---

## §5 — Terraform/IaC Standards

### 5.1 Module Structure

```
ops/deploy/terraform/
├── main.tf          # Root module orchestration
├── variables.tf     # Input variables with descriptions + validation
├── outputs.tf       # Output values
├── providers.tf     # Provider version constraints
└── modules/
    ├── aws-vpc/     # Network isolation
    ├── aws-compute-ecs/  # Container orchestration
    ├── aws-brokers/      # RabbitMQ, Redis
    ├── compute-modal/    # GPU inference (Modal)
    └── compute-runpod/   # GPU inference (RunPod)
```

### 5.2 Rules

- All variables must have `description`, `type`, and `default` (where applicable).
- Sensitive variables must be marked `sensitive = true`.
- State stored remotely (S3 + DynamoDB locking).
- No hardcoded values — use `var.*` references.
- Module outputs must be documented.

---

## §6 — PostgreSQL Optimization

### 6.1 Indexing Strategy

- High-frequency query paths must have dedicated composite indexes.
- Indexes added via Flyway migrations with `CREATE INDEX CONCURRENTLY IF NOT EXISTS`.
- Index naming: `idx_{table}_{columns}`.

### 6.2 Query Patterns

- N+1 queries are banned. Use `LEFT JOIN FETCH` or Entity Graphs.
- Manual JSON serialization in services is banned — use JPA `@Convert`.
- Pagination via `Pageable` with sensible defaults (`page=0, size=20, max=100`).
- Read operations use `@Transactional(readOnly = true)` for connection pool optimization.

### 6.3 Connection Pool

- HikariCP with `maximum-pool-size` tuned per environment (dev: 5, prod: 20).
- `idle-timeout` and `max-lifetime` configured to prevent stale connections.

---

## §7 — Security Standards

### 7.1 Zero-Trust BFF

- All API endpoints behind authentication except health checks.
- Spring Security OAuth2 Resource Server for token validation.
- CSRF disabled for stateless API. CORS bound to Spring GraphQL namespace.

### 7.2 Secrets Management

- Zero hardcoded secrets. All via `@ConfigurationProperties` or environment injection.
- No `System.getenv()` in functional beans.
- No `localhost:*` URLs in production source.
- Log scrubbing: no credentials, tokens, or API keys in log output.

### 7.3 Input Validation

- Base64 media payloads limited to 10 MB decoded size.
- File uploads validated by MIME type and size.
- User input sanitized before database storage.
- SQL injection prevented by parameterized queries only.

### 7.4 Cryptography

- Password hashing via `BCryptPasswordEncoder` in `orasaka-identity` only.
- Hashing executes outside `@Transactional` blocks.
- `orasaka-identity` uses `spring-security-crypto` without `spring-boot-starter-security`.

---

## §8 — Performance Standards

### 8.1 Caching Strategy

| Cache | Backend | TTL | Max Entries |
|:---|:---|:---|:---|
| Model catalog | Caffeine | 5 min | 500 |
| Operation graph | In-memory (volatile) | 30 sec | 1 |
| Tool results | Caffeine + PostgreSQL | configurable | 5000 |

### 8.2 Virtual Thread Safety

- `@Transactional` boundaries kept minimal — no I/O inside transactions.
- `synchronized` blocks over I/O loops are banned.
- `ConcurrentHashMap.computeIfAbsent` for atomic lazy initialization.

### 8.3 Memory Management

- Conversation memory window: 50 messages max (FIFO eviction).
- Base64 payloads: 10 MB hard limit before allocation.
- Reactive streams for token streaming — no buffering full responses.

---

## §9 — UX/UI Design Standards

### 9.1 Design Principles

- **Premium aesthetics**: Glassmorphism, smooth gradients, micro-animations.
- **Typography**: Google Fonts (Inter, Outfit) — no browser defaults.
- **Color**: Curated HSL palettes — no generic hex colors.
- **Motion**: Framer Motion for page transitions, hover effects, loading states.
- **Responsive**: Mobile-first layouts with CSS Grid/Flexbox.

### 9.2 Component Standards

- Split layouts for auth pages (marketing left + form right).
- Consistent elevation system using CSS `box-shadow` tokens.
- Loading skeletons for all async data (no blank screens).
- Toast notifications for all user actions (success/error).

### 9.3 Interaction Patterns

- Keyboard shortcuts for power users.
- Drag-and-drop for file uploads.
- Real-time streaming with visual typing indicators.
- Progressive disclosure for complex forms.

---

## §10 — Build & Compilation Workflow

### Fast-Iteration Cascade

```bash
# 1. Install identity contracts
mvn clean install -pl orasaka-identity

# 2. Recompile full dependency mesh
mvn clean compile -pl orasaka-gateway -am

# 3. Format
mvn spotless:apply -pl <module>

# 4. Verify
mvn spotless:check -pl <module>
```

### Frontend

```bash
cd orasaka-ui && npm run format && npm run lint
```

---

## §11 — ADR Reference

> [!IMPORTANT]
> All Architectural Decision Records are maintained in [`docs/CONTEXT.md`](docs/CONTEXT.md).
> Agents must load and enforce all active ADRs (ADR-001 through ADR-028) prior to code generation.

---

## §12 — Video Generation & Infrastructure Compliance

### Video Generation & Infrastructure Compliance
- **Zero-Fallback Policy:** Generative workers (Video, Audio, Image) are strictly prohibited from implementing silent non-AI fallbacks (e.g., Pillow image manipulations, mock audio loops) to bypass infrastructure or tensor errors.
- **Hardware Acceleration:** All inference tasks must leverage native hardware acceleration layers (Apple Silicon MPS via float32 / CUDA via float16) unthrottled. If an inference pipeline fails, the agent must let the system fail hard with an explicit stack trace rather than masking the error.
- **MLX Native Engine:** Support for Apple Silicon Unified Memory architecture using the native MLX framework. MLX execution must run real spatio-temporal Conv2D and Linear operations on mx.array with forced evaluation, ensuring actual tensor compute without any mock animations or fallbacks.
- **Verification:** Agents must review logs via the resource_guard to ensure that physical GPU/CPU compute metrics reflect active Torch/Diffusers/MLX execution during validation phases.

---

## §13 — Semantic Routing & Two-Phase Pipeline Architecture

### 13.1 Two-Phase Execution Pattern

All incoming AI requests pass through a mandatory two-phase interceptor pipeline:

| Phase | Interceptors | Mutability | Governance |
|:---:|:---|:---|:---|
| **Phase 1 — Core Immutable** | `UserContextResolver` → `SystemContextInjector` → `RagInterceptor` | Non-bypassable, non-reorderable | Admin cannot disable via UI |
| **Phase 2 — Dynamic Custom** | Semantically-routed or deterministic DB-ordered | Configurable per-request | Admin controls via pipeline config |

- Phase 1 executes **sequentially** in fixed order for every request — identity/role verification, security guardrails, and base context/RAG slicing.
- Between Phase 1 and Phase 2, the `SemanticRoutingEngine` evaluates the prompt.
- Phase 2 interceptors are selected dynamically based on the classified intent, or fall back to the full deterministic chain.

### 13.2 SemanticRoutingEngine Contract

- **Endpoint**: `POST http://localhost:8085/v1/classify` (LocalAI port 8085).
- **Request**: `{ "input": "<prompt>" }`.
- **Response**: `{ "intents": [{ "label": "video_generation", "confidence": 0.92 }] }`.
- **Graceful Degradation**: If LocalAI is unreachable, the engine returns an empty classification — Phase 2 falls back to the deterministic chain. No hard failures.
- **HTTP Client**: Uses Spring `RestClient` per §2.16 [ERR-120]. No `java.net.http.HttpClient`.

### 13.3 ConditionalRoute Matching

- Routes are defined as `ConditionalRoute(intentLabel, requiredInterceptorKeys, confidenceThreshold)`.
- A route matches when the classified intent label equals `intentLabel` AND the confidence score ≥ `confidenceThreshold`.
- Multiple routes can match simultaneously — the union of required interceptor keys is activated.
- Default routes: `video_generation` → `[MediaInterceptor, ToolInterceptor]`, `translation_required` → `[RefinerInterceptor]`, `strict_json_format` → `[RefinerInterceptor, RouterInterceptor]`.

### 13.4 PipelineRegistry Hot-Reload

- In-memory cache: `AtomicReference<Map<String, PipelineConfig>>`.
- **Readers** (`getConfig`, `getActiveInterceptorIds`) perform a single `AtomicReference.get()` — lock-free, wait-free.
- **Writers** (`reload`) build a new immutable map from the database and swap it atomically.
- Active SSE streams referencing the old snapshot are unaffected — they hold immutable references.
- No `synchronized` blocks, no locks over I/O — fully Virtual Thread safe.

### 13.5 Early-Ack SSE Protocol

Before the LLM generates its first token, the SSE controller pushes a `pipeline-ack` event:

```json
{
  "pipelineId": "default",
  "coreInterceptorIds": ["UserContextResolver", "SystemContextInjector", "RagInterceptor"],
  "dynamicInterceptorIds": ["MediaInterceptor", "ToolInterceptor"],
  "estimatedLatencyMs": 25
}
```

- **Event name**: `pipeline-ack`
- **Serialization**: Jackson → `AdvancedPipelineSchema` record → `application/json`
- **Failure mode**: If schema building fails, the stream proceeds without the ack (non-fatal).

### 13.6 Micrometer Telemetry

| Meter Name | Type | Description |
|:---|:---|:---|
| `orasaka.pipeline.routing.latency` | Timer | Semantic classification round-trip latency (LocalAI call) |
| `orasaka.pipeline.active.interceptors.count` | Gauge | Live interceptor count per execution (Phase 1 + Phase 2) |

### 13.7 RoutingMode Enum

| Mode | Description |
|:---|:---|
| `DETERMINISTIC` | Database-driven ordering via `PipelineConfigProvider` |
| `AGENTIC` | LLM-driven runtime sequence generation (future) |
| `SEMANTIC` | Prompt-classified dynamic interceptor selection via `SemanticRoutingEngine` |

---

## §14 — Local Agent Protocol Examples

### 14.1 CLI Agent Registration (Persistence Layer)

This JSON block demonstrates how the `orasaka-persistence-app` layer expects a local CLI agent to register its tool definitions via the gateway AMQP tunnel:

```json
{
  "agentId": "cli-agent-macbook-pro",
  "status": "ONLINE",
  "capabilities": [
    {
      "toolName": "execute_bash_command",
      "description": "Executes a secure bash command in the user's local terminal.",
      "parameters": {
        "type": "object",
        "properties": {
          "command": {
            "type": "string",
            "description": "The exact shell command to run."
          }
        },
        "required": ["command"]
      }
    },
    {
      "toolName": "read_local_file",
      "description": "Reads a file from the host filesystem.",
      "parameters": {
        "type": "object",
        "properties": {
          "filePath": {
            "type": "string",
            "description": "Absolute path to the file."
          }
        },
        "required": ["filePath"]
      }
    }
  ],
  "telemetry": {
    "os": "macOS 26.5",
    "architecture": "arm64",
    "memory_capacity_mb": 65536
  }
}
```

### 14.2 Reverse Tunnel Task Dispatch

When the `orasaka-core` orchestrator decides to execute a bash command on the local user's machine, it dispatches this structured payload over SSE (Server-Sent Events) to the connected CLI agent:

```json
{
  "dispatchId": "dispatch-8a4b-4f9e-a0c3-11b2c3d4e5f6",
  "agentId": "cli-agent-macbook-pro",
  "toolName": "execute_bash_command",
  "arguments": {
    "command": "ls -la /var/logs/orasaka-video-worker"
  },
  "timeoutSeconds": 30
}
```

### 14.3 Agent Execution Report

After executing the action, the local agent POSTs the execution report back to the `orasaka-gateway` (`/api/v1/agent/report`):

```json
{
  "dispatchId": "dispatch-8a4b-4f9e-a0c3-11b2c3d4e5f6",
  "agentId": "cli-agent-macbook-pro",
  "status": "SUCCESS",
  "output": {
    "stdout": "total 4096\n-rw-r--r-- 1 admin staff 2048000 Jun 01 20:48 20260601_video-worker.log\n",
    "stderr": "",
    "exitCode": 0
  },
  "executionTimeMs": 45
}
```

> **Contract Rule**: All agent payloads use rigid JSON structures validated against domain schemas (`*Dto` classes). Failure to provide `dispatchId` or returning unescaped output will trigger a `400 Bad Request` drop at the gateway layer, preventing poisoned context injection in the LLM pipeline.


