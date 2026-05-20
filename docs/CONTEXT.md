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
