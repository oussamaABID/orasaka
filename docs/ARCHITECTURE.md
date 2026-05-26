# Architecture Reference

> Visual guide to Orasaka's system topology, module boundaries, and execution flows.

---

## 1. Overview & Module Topology

Orasaka follows a **Ports & Adapters (Hexagonal)** architecture, enforced by ArchUnit at compile time.

```mermaid
graph TD
    UI["orasaka-ui (Next.js BFF :3000)"] -->|BFF API Proxy| GW["orasaka-gateway (:8080)"]
    CLI["orasaka-cli"] -->|SSE Reverse Tunnel| GW

    GW --> Core["orasaka-core (Stateless AI Engine)"]
    GW --> ID["orasaka-identity (Security & RBAC)"]
    GW --> Tools["orasaka-tools (External APIs/MCP)"]

    Core --> PersistApp["orasaka-persistence/app"]
    ID --> PersistId["orasaka-persistence/identity"]
    Tools -->|Implements Ports| Core

    %% Interceptors AutoConfiguration
    Interceptors["orasaka-interceptors"] -->|AutoConfig SPI| Core
    subgraph InterceptorModules ["Interceptor Submodules"]
        direction LR
        Ctx["context"]
        Enr["enrichment"]
        Trans["translation"]
        Reform["reformulation"]
        Tooling["tooling"]
        Valid["validation"]
    end
    Interceptors --- InterceptorModules

    %% Business Templates
    Business["orasaka-business (Prompt Templates)"] -.->|ResourceLoader| Core

    %% Messaging / Workers
    GW -.->|AMQP exchange| RMQ["RabbitMQ (:5672)"]
    RMQ -.->|Async Jobs| WorkerJava["orasaka-workers/external-services (:8082)"]
    RMQ -.->|Video Tasks| WorkerPython["orasaka-workers/video (Python :8188)"]

    %% Databases
    DB[("PostgreSQL / Redis")]
    PersistApp --> DB
    PersistId --> DB
    WorkerJava --> DB
```

> [!IMPORTANT]
> **Outbound Port Event Patterns**: Domain events are published from `orasaka-identity` via pure Java interfaces (`UserEventPublisher` outbound ports). Concrete messaging adapters (`RabbitUserEventAdapter`) reside in `orasaka-persistence/app` and delegate them to RabbitMQ. This maintains core purity.

---

## 2. BFF (Backend-for-Frontend) Topology

The browser **never** connects directly to `orasaka-gateway` (port `8080`) or local AI engines. All traffic proxies through Next.js server-side API routes.

```mermaid
graph TD
    subgraph "Browser (Next.js client)"
        UI[orasaka-ui React UI]
    end

    subgraph "BFF Proxy (Next.js server)"
        ChatStream["/api/chat/stream/:conversationId"]
        GraphQL["/api/graphql"]
        NextAuth["/api/auth/*"]
    end

    subgraph "Backend (Private network)"
        Gateway[orasaka-gateway API root]
        Identity[orasaka-identity auth engine]
        IdentityPersistence[orasaka-persistence/identity schema]
        Postgres[(PostgreSQL)]
    end

    UI -- "text/event-stream" --> ChatStream
    UI -- "graphql post" --> GraphQL
    UI -- "credentials submit" --> NextAuth

    ChatStream -- "Bearer token inject" --> Gateway
    GraphQL -- "Bearer token inject" --> Gateway
    NextAuth -- "/api/v1/auth/login delegate" --> Gateway

    Gateway -- "Virtual Threads verify" --> Identity
    Identity --> IdentityPersistence
    IdentityPersistence --> Postgres
```

---

## 3. Cognitive Engine Flow

When `AiClient.chat()` is invoked, requests pass through the ordered context interceptor pipeline:

```mermaid
graph TD
    User([Developer / Client]) --> Client[AiClient Facade]
    Client --> Engine[AbstractEngine]

    subgraph Pipeline ["Cognitive Pipeline"]
        Engine --> Interceptors[ContextInterceptor Chain]
        Interceptors --> ToolInterceptor[ToolInterceptor]
        Interceptors --> RagInterceptor[RagInterceptor]
        Interceptors --> McpInterceptor[McpInterceptor]
        Interceptors --> MemoryInterceptor[MemoryInterceptor]
    end

    subgraph Services ["Services & Registry"]
        ToolInterceptor --> ToolRegistry[ToolRegistry]
        RagInterceptor --> KnowledgeService[KnowledgeService]
        McpInterceptor --> McpOrchestrator[McpOrchestrator]
    end

    subgraph SpringAI ["Spring AI Model Ports"]
        Engine --> ChatModel[Spring AI ChatModel]
        Engine --> ImageModel[Spring AI ImageModel]
    end

    subgraph Providers ["Local Providers"]
        ChatModel --> Ollama[Ollama Provider]
        KnowledgeService --> VectorStore[(VectorStore)]
        McpOrchestrator --> McpServer[External MCP Servers]
    end
```

### Context-Matrix Pipeline (Blueprints)
1. **UserContextResolver**: User profiles & RBAC roles.
2. **UserContextInterceptor**: Tenant-level context enrichment.
3. **SystemContextInjector**: Environment signals & active capabilities.
4. **LanguageAlignmentInterceptor**: English reasoning alignment.
5. **MemoryInterceptor**: FIFO conversation history prepend.
6. **DynamicMemoryCondenser**: Sliding window memory compaction.
7. **RagInterceptor**: Tenant-isolated vector store RAG context.
8. **HybridRagResolver**: Dense (PGVector) + Sparse (BM25) fused RAG queries.
9. **McpInterceptor**: Resolve external MCP server tools/data.
10. **RefinerInterceptor**: Resolves contextual refinement (AI-dependent).
11. **RouterInterceptor**: Routes to the optimal model based on intent (AI-dependent).
12. **SimDagRouterInterceptor**: DAG-based multi-step routing simulation.
13. **ToolInterceptor**: Dynamic tool registration & callbacks.
14. **CostShieldInterceptor**: Switches to cloud fallback if memory usage exceeds 85%.
15. **QuantumValidationAdvisor**: 4-tier closed-loop validation (A/B/C/D).

---

## 4. Video Generation Pipeline

Heavy rendering runs on an isolated video worker:

| Service | Port | Engine / Stack |
| :--- | :---: | :--- |
| Gateway | `8080` | Spring Boot GraphQL / REST |
| LocalAI (Speech & Audio) | `8085` | Piper TTS & Whisper (CPU) |
| Text-to-Image | `8086` | stable-diffusion.cpp (MPS) |
| Text-to-Video | `8188` | Stable Video Diffusion XT (Python) |

```mermaid
graph TD
    UI[React Video Canvas] -->|POST /api/v1/ai/video| GW[orasaka-gateway]
    GW -->|Inject context| SV[VideoService]
    SV -->|REST Post| LTR[Local Video Runner :8188]
    LTR -->|GPU execution| GPU[Metal / GPU]
```

---

## 5. Pipeline Orchestration Patterns

- **Declarative DB Config**: Declare chains in `pipeline_interceptor_config` (no rebuilds required).

---

## 6. Module Quick Reference

| Module | Location | Purpose |
| :--- | :--- | :--- |
| `orasaka-core` | `orasaka-framework/orasaka-core/` | Stateless AI orchestration engine. Web-agnostic. |
| `orasaka-interceptors/*` | `orasaka-framework/orasaka-interceptors/` | 6 submodules implementing pipeline filters (context, enrichment, translation, reformulation, tooling, validation). |
| `orasaka-gateway` | `orasaka-apps/orasaka-gateway/` | GraphQL, REST, SSE BFF controllers. Enforces security filters. |
| `orasaka-identity` | `orasaka-framework/orasaka-identity/` | Pure Java RBAC user controls, hashing, and OAuth2 reconciliation. |
| `orasaka-business` | `orasaka-framework/orasaka-business/` | Markdown persona prompt templates. |
| `orasaka-tools` | `orasaka-framework/orasaka-tools/` | Tool callbacks, MCP clients, Caffeine/Postgres caches. |
| `orasaka-persistence/app` | `orasaka-framework/orasaka-persistence/app/` | Chat session DB state and JPA repositories. |
| `orasaka-persistence/identity` | `orasaka-framework/orasaka-persistence/identity/` | PostgreSQL user authentication tables. |
| `orasaka-test-support` | `orasaka-framework/orasaka-test-support/` | Shared test infrastructure: `AbstractContainerIntegrationTest`, `GovernanceRules`. |
| `orasaka-workers/external-services` | `orasaka-apps/orasaka-workers/external-services/` | Async Java worker running Quartz jobs. |
| `orasaka-workers/video` | `orasaka-apps/orasaka-workers/video/` | Python Stable Video Diffusion worker node (GPU inference). |
| `orasaka-ui` | `orasaka-apps/orasaka-ui/` | Next.js 16 web client with input-blocking mechanisms. |
| `orasaka-cli` | `orasaka-apps/orasaka-cli/` | TypeScript developer CLI with offline SQLite logging. |
| `orasaka-end2end` | `orasaka-end2end/` | Hermetic E2E integration tests (ADR-040). |

---

## Related Documentation
- [Developer Onboarding Guide](101.md)
- [Core Pipeline Specifications](CORE.md)
- [Public API Details](API_REFERENCE.md)
- [E2E Testing Framework](END2END_TEST.md)
- [ADR Indexes](CONTEXT.md)
