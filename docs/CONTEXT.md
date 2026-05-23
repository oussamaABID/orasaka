# Orasaka Architecture Decision Record (ADR)

## ADR 001: Ollama as Default Provider

- **Decision**: Ollama is the default provider for the `orasaka-core` module.
- **Rationale**: Enables local development and privacy-first AI interaction without requiring cloud credentials initially.
- **Status**: Approved.

## ADR 002: Standalone Library Constraint

- **Decision**: The `orasaka-core` module must remain a pure library, decoupled from Spring Boot.
- **Rationale**: Ensures portability across any Spring 6+ environment and prevents dependency bloat in consumer applications.
- **Status**: Approved.

## ADR 003: Client-Side MCP Orchestration

- **Decision**: MCP (Model Context Protocol) integration is handled via a dedicated service bridge.
- **Rationale**: Allows the engine to consume external tools and context dynamically while maintaining the core abstraction.
- **Status**: Approved.

## ADR 004: Unified Tool Registry

- **Decision**: Local Java methods are mapped to `FunctionCallback` via a unified registry.
- **Rationale**: Simplifies the developer experience by allowing native Java code to be used as LLM tools seamlessly.
- **Status**: Approved.

## ADR 005: Local Inference Optimization (Native Ollama)

- **Decision**: Use native Ollama on macOS instead of containerized Ollama.
- **Rationale**: Leverages macOS Metal GPU acceleration for significantly higher performance and reduced container overhead.
- **Status**: Approved.

## ADR 006: Domain-Driven Monorepo Refactor

- **Decision**: Transition from a single module (`cors`) to a domain-driven monorepo structure.
- **Rationale**: Enhances scalability, brand identity, and separation of concerns (Identity, Gateway, UI).
- **Status**: Approved (2026-05-15).

## ADR 007: Extraction of orasaka-tools

- **Decision**: Extract MCP clients and Function Tool implementations from `orasaka-core` into a new `orasaka-tools` module.
- **Rationale**: Ensures `orasaka-core` remains 100% agnostic by relying strictly on interfaces, allowing the `orasaka-tools` module to host concrete configuration and Virtual Thread executions.
- **Status**: Approved.

## ADR 008: Next.js BFF Router Proxying for CORS and Token Injection

- **Decision**: Browser clients must query backend APIs through Next.js server-side API Routes (`/api/graphql` and `/api/chat/stream/[conversationId]`) instead of hitting the gateway port (`8080`) or Ollama port (`11434`) directly.
- **Rationale**: Enforces security by injecting user context tokens on the server-side BFF route, prevents browser-side CORS verification failures, and decouples backend topologies from client-side bundle configurations.
- **Status**: Approved (2026-05-21).

## ADR 009: Virtual Thread Concurrency & Security Context Preservation

- **Decision**: Gateway controller operations (handling both GraphQL calls and Server-Sent Events chat streams) must run asynchronously via Java 21's `Executors.newVirtualThreadPerTaskExecutor()`.
- **Rationale**: Ensures high-concurrency scalability for I/O and model inference tasks under limited local server memory constraints (MacBook DevX), while preserving SecurityContext boundary propagation to secure user session isolation on concurrent virtual threads.
- **Status**: Approved (2026-05-21).

## ADR 010: Passive Multi-Tier Caching Decorator Pattern

- **Decision**: Implement a passive, configuration-driven caching decorator (`CachingToolCallback`) for Java-registered LLM tools using local memory (Caffeine) and database persistent (PostgreSQL) caching tiers.
- **Rationale**: Reduces LLM invocation costs, cuts down response latency by immediately fetching cached tool payloads, and ensures cross-node cache sharing and persistence via database synchronization. Enables opt-in activation at the tool-level via `application.yml`.
- **Status**: Approved (2026-05-22).

## ADR 011: Asynchronous Scheduled RAG Ingestion Pipeline

- **Decision**: Trigger RAG document ingestion asynchronously via a daily background component scheduler (`OrasakaBackgroundScheduler`) executing on lightweight virtual threads.
- **Rationale**: Isolates heavy document chunking and embedding operations from user request cycles, allows flexible customization of chunking strategies (`PLAIN_TEXT`, `MARKDOWN_CHUNKERS`, `JSON_ARRAY`), and enforces strict passivity by bypassing ingestion entirely if RAG properties are disabled.
- **Status**: Approved (2026-05-22).

## ADR 012: Passive Token-Based Account Verification

- **Decision**: Implement passive account double-opt-in verification using SHA-256 hashed verification tokens stored in `orasaka_verification_tokens`.
- **Rationale**: Secures user onboarding by verifying registration via a passive, optional mechanism controllable by configurations properties.
- **Status**: Approved (2026-05-22).

## ADR 013: Session-Injected Interception & Resume Engine

- **Decision**: Store active user interruptions (onboarding, reviews) in the database (`orasaka_user_interceptions`) and load them directly into JWT/User session contexts.
- **Rationale**: Eliminates polling overhead from browser-side apps by checking interception status on Gateway token queries, ensuring a high-performance session state resolution.
- **Status**: Approved (2026-05-22).

