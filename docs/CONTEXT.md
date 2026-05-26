# Architecture Decision Records (ADR)

> Canonical ledger of every major architectural decision made in the Orasaka project.
> Each record documents **what** was decided, **why**, and **when** — providing a transparent engineering history
> that enables contributors to understand the reasoning behind the system's design.
>
> [!IMPORTANT]
> This file is the single source of truth for all ADRs. It is dynamically loaded by
> [`AGENTS.md`](../AGENTS.md) as part of the Agent Execution Contract.

---

## Quick Reference

| ADR | Decision                                  | Category        |
| :-: | :---------------------------------------- | :-------------- |
| 001 | Ollama as default provider                | 🦙 AI Provider  |
| 002 | Core must be a standalone library         | 🏛️ Architecture |
| 003 | Client-side MCP orchestration             | 🔧 Tooling      |
| 004 | Unified tool registry                     | 🔧 Tooling      |
| 005 | Native Ollama (no Docker) on macOS        | ⚡ Performance  |
| 006 | Domain-driven monorepo                    | 🏛️ Architecture |
| 007 | Extraction of orasaka-tools               | 🏛️ Architecture |
| 008 | Next.js BFF router proxying               | 🔒 Security     |
| 009 | Virtual threads + security context        | ⚡ Performance  |
| 010 | Multi-tier caching decorator              | ⚡ Performance  |
| 011 | Async scheduled RAG ingestion             | 📚 RAG          |
| 012 | Token-based account verification          | 🔒 Security     |
| 013 | Session-injected interceptions            | 🔒 Security     |
| 014 | Self-validating domain records            | 🏛️ Architecture |
| 015 | Open/Closed interceptor pipeline          | 🏛️ Architecture |
| 016 | Protocol-driven workspace indexing        | 🔧 Tooling      |
| 017 | Anemic service orchestration              | 🏛️ Architecture |
| 018 | Record naming conventions                 | 📏 Standards    |
| 019 | Package-private encapsulation             | 📏 Standards    |
| 020 | Code locality & file density              | 📏 Standards    |
| 021 | Local sovereign image generation          | 🎨 Media        |
| 022 | Local sovereign video generation          | 🎬 Media        |
| 023 | Agnostic OAuth2 token-exchange federation | 🔒 Security     |
| 024 | Mapper isolation invariant (ERR-107)      | 🏠️ Architecture |
| 025 | Decoupled verification from db transactions | 🔒 Security     |
| 026 | User Preferences Domain Boundary          | 🔒 Security     |
| 027 | Metadata-Driven Feature Registry          | 🏛️ Architecture |
| 028 | ProviderClassifier centralization          | 🏛️ Architecture |
| 029 | Token-based password recovery             | 🔒 Security     |

---

## 🦙 AI Provider

### ADR-001: Ollama as Default Provider

|               |                                                                                                                                                                |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Ollama is the default provider for the `orasaka-core` module.                                                                                                  |
| **Rationale** | Enables local development and privacy-first AI interaction without requiring cloud credentials. Developers can start immediately with `ollama pull llama3:8b`. |
| **Status**    | ✅ Approved                                                                                                                                                    |

### ADR-005: Local Inference Optimization (Native Ollama)

|               |                                                                                                                                          |
| :------------ | :--------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Use native Ollama on macOS instead of containerized Ollama.                                                                              |
| **Rationale** | Leverages macOS Metal GPU acceleration for significantly higher performance. Containers add overhead and cannot access the GPU directly. |
| **Status**    | ✅ Approved                                                                                                                              |

---

## 🏛️ Architecture

### ADR-002: Standalone Library Constraint

|               |                                                                                                                                                           |
| :------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | The `orasaka-core` module must remain a pure library, decoupled from Spring Boot.                                                                         |
| **Rationale** | Ensures portability across any Spring 6+ environment and prevents dependency bloat in consumer applications. Core depends only on `spring-ai-core` 1.1.6. |
| **Status**    | ✅ Approved                                                                                                                                               |

### ADR-006: Domain-Driven Monorepo Refactor

|               |                                                                                                                              |
| :------------ | :--------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Transition from a single module to a domain-driven monorepo structure (`core`, `identity`, `gateway`, `tools`, `ui`, `cli`). |
| **Rationale** | Enhances scalability, brand identity, and separation of concerns. Each module has clear ownership boundaries.                |
| **Status**    | ✅ Approved (2026-05-15)                                                                                                     |

### ADR-007: Extraction of orasaka-tools

|               |                                                                                                                                                                                  |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Extract MCP clients and function tool implementations from `orasaka-core` into a new `orasaka-tools` module.                                                                     |
| **Rationale** | Ensures `orasaka-core` remains 100% agnostic by relying strictly on interfaces (Ports & Adapters). The tools module hosts concrete configurations and virtual thread executions. |
| **Status**    | ✅ Approved                                                                                                                                                                      |

### ADR-014: Self-Validating Rich Domain Records

|               |                                                                                                                                                                                             |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | All operational payloads and domain records must handle state constraints, collection defensive copying (`List.copyOf`, `Map.copyOf`), and fallback defaults in their compact constructors. |
| **Rationale** | Complete immunity against `NullPointerException` across the entire agent pipeline. Service retention windows are minimized, fully unlocking virtual thread performance.                     |
| **Status**    | ✅ Approved                                                                                                                                                                                 |

### ADR-015: Decoupled Cognitive Core & Open/Closed Pipeline

|               |                                                                                                                                                                                                              |
| :------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Full encapsulation of RAG, MCP, Memory, and Tool calling inside concrete `ContextInterceptor` implementations. The cognitive engine is purely generic, invoking a pipeline of interceptors during execution. |
| **Rationale** | Decouples the core engine from specific infrastructure or RAG/MCP logic. Satisfies the Open/Closed Principle and Ports & Adapters boundary.                                                                  |
| **Status**    | ✅ Approved                                                                                                                                                                                                  |

### ADR-017: Anemic Service Orchestration

|               |                                                                                                                                                            |
| :------------ | :--------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Services and engines are strictly anemic orchestration layers, relying on rich domain methods (like `request.compileMessages(...)`) for compiled payloads. |
| **Rationale** | Eradicates procedural state validation from services, guaranteeing immutability and thread-safety under heavy concurrent execution.                        |
| **Status**    | ✅ Approved                                                                                                                                                |

### ADR-024: Mapper Isolation Invariant (ERR-107)

|                 |                                                                                                                                                                                                                                                                                                                                                                                         |
| :-------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**    | All repetitive, field-by-field entity-to-domain or domain-to-DTO mapping code (setter chains, manual constructor calls with 5+ arguments) must be extracted into dedicated, package-private `final class *Mapper` utility classes with `static` methods and a private constructor. Controllers, services, and engines must reduce mapping call sites to single-line mapper invocations. |
| **Rationale**   | Inline setter blocks of 5+ lines pollute business orchestration methods, obscure the intended flow, and duplicate mapping logic across creation paths. Isolating construction boilerplate into dedicated mappers enforces Single Responsibility, improves readability, and enables centralized validation changes.                                                                      |
| **Enforcement** | `review_architect.md` Gate 18 (ERR-114) rejects any PR containing inline setter chains of 5+ lines in controllers or services. ArchUnit boundary tests enforce that mapper classes remain package-private.                                                                                                                                                                              |
| **Status**      | ✅ Approved (2026-05-25)                                                                                                                                                                                                                                                                                                                                                                |

### ADR-027: Metadata-Driven Feature Registry

|               |                                                                                                                                                                                                                                        |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Replace the binary feature flag toggles with a granular, metadata-driven Feature Registry configurable in `application.yml`, exposed via `GET /api/v1/bootstrap/features` and dynamically rendered in the frontend capabilities dropdown. |
| **Rationale** | Promotes extensibility, avoids hardcoding feature lists in the UI, and enables runtime parameters mapping and execution template resolving directly from system configuration.                                                        |
| **Status**    | ✅ Approved (2026-05-29)                                                                                                                                                                                                               |

---

## 🔒 Security

### ADR-008: Next.js BFF Router Proxying

|               |                                                                                                                                                                      |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Browser clients must query backend APIs through Next.js server-side API Routes instead of hitting `orasaka-gateway` (port `8080`) or Ollama (port `11434`) directly. |
| **Rationale** | Enforces security by injecting user context tokens server-side, prevents CORS failures, and decouples backend topologies from client-side configurations.            |
| **Status**    | ✅ Approved (2026-05-21)                                                                                                                                             |

### ADR-012: Passive Token-Based Account Verification

|               |                                                                                                                           |
| :------------ | :------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Implement passive account double-opt-in verification using SHA-256 hashed tokens stored in `orasaka_verification_tokens`. |
| **Rationale** | Secures user onboarding via a passive, optional mechanism controllable by configuration properties.                       |
| **Status**    | ✅ Approved (2026-05-22)                                                                                                  |

### ADR-013: Session-Injected Interception & Resume Engine

|               |                                                                                                                                                    |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Store active user interruptions (onboarding, reviews) in the database and load them directly into JWT/session contexts.                            |
| **Rationale** | Eliminates polling overhead by checking interception status during Gateway token verification, ensuring high-performance session state resolution. |
| **Status**    | ✅ Approved (2026-05-22)                                                                                                                           |

### ADR-023: Agnostic OAuth2 Token-Exchange Federation

|               |                                                                                                                                                                                                                                                                                                                                       |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Implement OAuth2 provider verification via a stateless Strategy Pattern within `orasaka-identity`. NextAuth handles protocol negotiation; the backend verifies tokens and reconciles identities via `IdentityReconciliationService`.                                                                                                  |
| **Rationale** | Avoids coupling to heavyweight Spring Security OAuth2 Client filters. Enables Open-Closed extensibility — new providers require only a single class implementing `OAuth2ProviderVerifier` and a config property flag. Each provider bean is conditionally loaded via `@ConditionalOnProperty`, producing zero overhead when disabled. |
| **Status**    | ✅ Approved (2026-05-24)                                                                                                                                                                                                                                                                                                              |

### ADR-025: Decoupled External Verification from Database Transactions

|               |                                                                                                                                                                                                                                                                                                                                       |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Decouple external network and cryptographic token verification from database transaction blocks. Perform identity checks and JIT user provisioning programmatically within an isolated `TransactionTemplate` rather than using method-level/class-level `@Transactional` scopes. |
| **Rationale** | Under Java 21 Virtual Threads, running long-duration remote HTTP requests or CPU-intensive token validation inside database transaction blocks holds connection pool threads active, causing rapid database connection starvation. Segregating network validation from database transactions keeps connection scopes minimal and high-performing. |
| **Status**    | ✅ Approved (2026-05-25) |

### ADR-026: User Preferences Domain Boundary

|               |                                                                                                                                                                                                                                                                          |
| :------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | User Preferences are strictly managed under the `orasaka-identity` domain boundary. The `orasaka-gateway` requests preference states directly from `orasaka-identity`, which resolves/queries them via its dedicated data contract `orasaka-persistence-identity`. |
| **Rationale** | Ensures strong encapsulation of user-scoped concerns. Prevents the gateway or core business logic from bypassing the identity domain boundary to fetch user-specific display, UI, or orchestration preferences.                                                          |
| **Status**    | ✅ Approved (2026-05-29)                                                                                                                                                    |

---

## ⚡ Performance

### ADR-009: Virtual Thread Concurrency & Security Context Preservation

|               |                                                                                                                                                                    |
| :------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Gateway controllers must run asynchronously using a custom `DelegatingSecurityContextExecutorService` bean that wraps Java 21's `Executors.newVirtualThreadPerTaskExecutor()`. |
| **Rationale** | Ensures high-concurrency scalability for I/O and model inference under limited local memory, while propagating `SecurityContext` propagation across virtual threads for session isolation. |
| **Status**    | ✅ Approved (2026-05-21)                                                                                                                                           |

### ADR-010: Passive Multi-Tier Caching Decorator

|               |                                                                                                                                    |
| :------------ | :--------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Implement a configuration-driven `CachingToolCallback` decorator using Caffeine (local) and PostgreSQL (persistent) caching tiers. |
| **Rationale** | Reduces LLM invocation costs, cuts response latency, and enables cross-node cache sharing. Opt-in per tool via `application.yml`.  |
| **Status**    | ✅ Approved (2026-05-22)                                                                                                           |

---

## 🔧 Tooling

### ADR-003: Client-Side MCP Orchestration

|               |                                                                                                                |
| :------------ | :------------------------------------------------------------------------------------------------------------- |
| **Decision**  | MCP (Model Context Protocol) integration is handled via a dedicated service bridge.                            |
| **Rationale** | Allows the engine to consume external tools and context dynamically while maintaining core abstraction purity. |
| **Status**    | ✅ Approved                                                                                                    |

### ADR-004: Unified Tool Registry

|               |                                                                                                      |
| :------------ | :--------------------------------------------------------------------------------------------------- |
| **Decision**  | Local Java methods are mapped to `FunctionCallback` via a unified registry.                          |
| **Rationale** | Simplifies the developer experience by allowing native Java code to be used as LLM tools seamlessly. |
| **Status**    | ✅ Approved                                                                                          |

### ADR-016: Protocol-Driven Workspace Indexing

|               |                                                                                                                                                          |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Transition to protocol-driven workspace indexing via an externalized MCP Workspace Database (`orasaka-code-intel`). Direct iterative scanning is banned. |
| **Rationale** | Directory-walking and file-looping across package boundaries are slow, redundant, and exhaust model context windows.                                     |
| **Status**    | ✅ Approved                                                                                                                                              |

---

## 📚 RAG

### ADR-011: Asynchronous Scheduled RAG Ingestion Pipeline

|               |                                                                                                                                                                                |
| :------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Trigger RAG document ingestion asynchronously via a daily `BackgroundScheduler` executing on virtual threads.                                                                  |
| **Rationale** | Isolates heavy chunking and embedding from user request cycles. Supports `PLAIN_TEXT`, `MARKDOWN_CHUNKERS`, and `JSON_ARRAY` strategies. Bypasses entirely if RAG is disabled. |
| **Status**    | ✅ Approved (2026-05-22)                                                                                                                                                       |

---

## 📏 Standards

### ADR-018: Strict Data Component Naming & Record Conventions

|               |                                                                                                                                                            |
| :------------ | :--------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | All pure data carriers must be modeled as immutable Java 21 `record` types. Standard `public class` components must adhere to JavaBean naming conventions. |
| **Rationale** | Prevents pseudo-record naming patterns on mutable standard Java classes.                                                                                   |
| **Status**    | ✅ Approved                                                                                                                                                |

### ADR-019: Package-Private Encapsulation Boundary

|               |                                                                                                                                                      |
| :------------ | :--------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Inner classes, orchestrator implementations, and pipeline utility beans must be marked package-private. Only interfaces and facades may be `public`. |
| **Rationale** | Prevents structural bleed and maintains high-cohesion API boundaries within monorepo modules.                                                        |
| **Status**    | ✅ Approved                                                                                                                                          |

### ADR-020: Code Locality & File Density

|               |                                                                                                                                                                  |
| :------------ | :--------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Group tightly coupled records, sealed hierarchies, and inner implementations inside the same file boundary. Split packages by transport protocol are prohibited. |
| **Rationale** | Maximizes Java 21 file density for shorter, cleaner structures, faster navigation, and immediate context visibility.                                             |
| **Status**    | ✅ Approved (2026-05-22)                                                                                                                                         |

---

## 🎨 Media

### ADR-021: Local Sovereign Image Generation

|               |                                                                                                                                 |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Integrate bare-metal `stable-diffusion.cpp` on port `8085` for image generation, returning RFC 2397 Data URLs with Base64 data. |
| **Rationale** | Removes cloud dependencies for image generation, ensures 100% data sovereignty, and formats output as browser-ready Data URLs.  |
| **Status**    | ✅ Approved (2026-05-23)                                                                                                        |

### ADR-022: Local Sovereign Video Generation

|               |                                                                                                                                                                             |
| :------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Decision**  | Integrate standalone Python video worker on port `8188` running Stable Video Diffusion (SVD) XT (with Apple Silicon Metal acceleration optimizations), exposing a dedicated `/api/v1/ai/video` REST endpoint returning RFC 2397 Data URLs. |
| **Rationale** | Provides high-performance local video rendering without cloud dependencies, isolates heavy video traffic on a separate port, and returns browser-parseable Data URL format. |
| **Status**    | ✅ Approved (2026-05-24)                                                                                                                                                    |

---

## 🏛️ Code Factorization

### ADR-028: ProviderClassifier Centralization

|               |                                                                                                                                 |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Centralize all provider classification logic (commercial vs. local, name resolution from ChatOptions) into a single `ProviderClassifier` utility class in `orasaka-core.application.pipeline`. |
| **Rationale** | Eliminates scattered `"openai".equalsIgnoreCase(provider)` checks across `AbstractEngine`, `EnginePipelineBridge`, and `EngineStreamBridge`. Provides a single source of truth for adding new providers (e.g., Mistral, Cohere) without modifying multiple files. |
| **Status**    | ✅ Approved (2026-05-30)                                                                                                        |

---

### ADR-029: Multi-Module Interceptor Extraction

|               |                                                                                                                                 |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Extract all interceptors from `orasaka-core.application.interceptor` into standalone Maven submodules under `orasaka-interceptors/`: `orasaka-interceptor-translation`, `orasaka-interceptor-validation`, `orasaka-interceptor-reformulation`. |
| **Rationale** | Core must remain a lightweight, stateless library containing only the `PromptContextInterceptor` port interface and the `DynamicPipelineOrchestrator`. Interceptor implementations are loaded dynamically via Spring Boot `AutoConfiguration.imports`, enabling plug-and-play composition without core recompilation. |
| **Status**    | ✅ Approved (2026-05-31)                                                                                                        |

---

### ADR-030: Air-Gapped Security Governance Kill-Switch

|               |                                                                                                                                 |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Add `orasaka.security.disable-ai=true` as a hard governance kill-switch. The `DynamicPipelineOrchestrator` throws a `SecurityException` if any interceptor returning `isAiDependent() == true` is invoked while the switch is active. |
| **Rationale** | Regulatory compliance and air-gapped deployment scenarios require the ability to completely disable all AI inference calls without code changes. The `isAiDependent()` flag on `PromptContextInterceptor` enables fine-grained control — non-AI interceptors (user context, memory, tools) continue operating normally. |
| **Status**    | ✅ Approved (2026-05-31)                                                                                                        |

---

### ADR-031: Hybrid Routing Engine (DETERMINISTIC + AGENTIC)

|               |                                                                                                                                 |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Support two interceptor routing modes: `DETERMINISTIC` (database-driven ordering via `PipelineConfigProvider`) and `AGENTIC` (LLM-driven runtime sequence generation). Configured via `orasaka.core.orchestration.routing.mode`. |
| **Rationale** | Deterministic mode gives administrators full control over interceptor execution order via the UI. Agentic mode enables the system to dynamically optimize the pipeline based on payload intent analysis. The `RoutingMode` enum and `DynamicPipelineOrchestrator` support both modes with a clean fallback strategy. |
| **Status**    | ✅ Approved (2026-05-31)                                                                                                        |

---

### ADR-029: Token-Based Password Recovery

|               |                                                                                                                                 |
| :------------ | :------------------------------------------------------------------------------------------------------------------------------ |
| **Decision**  | Implement an asynchronous, token-based password reset flow with SHA-256 hashing, 15-minute expiration, single-use enforcement, and zero-enumeration protection. Separated from `IdentityService` via a dedicated `PasswordRecoveryService` inbound port. |
| **Rationale** | Password recovery is a distinct lifecycle concern (SRP). Zero-enumeration prevents account harvesting (§7.1). BCrypt hashing runs outside `@Transactional` to avoid connection pool starvation. `password_changed_at` column enables downstream session invalidation. Token log scrubbing (8-char prefix only) prevents credential leaks in observability pipelines. |
| **Status**    | ✅ Approved (2026-05-31)                                                                                                        |

---

## 📎 Related Documentation

| Document                                     | Description                                         |
| :------------------------------------------- | :-------------------------------------------------- |
| [Architecture Reference](ARCHITECTURE.md)    | System topology, module boundaries, execution flows |
| [API Reference](API_REFERENCE.md)            | Public types, facades, endpoints, data models       |
| [Glossary](GLOSSARY.md)                      | Ecosystem terms, patterns, environment variables    |
| [Business Guide](BUSINESS_IMPLEMENTATION.md) | Step-by-step feature implementation blueprint       |

