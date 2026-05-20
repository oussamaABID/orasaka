# Orasaka Glossary

| Term | Definition | Role in Ecosystem |
| :--- | :--- | :--- |
| **CORE** | Cognitive Orchestration & Retrieval Engine (`orasaka-core`). | The AI Orchestration brain of the Orasaka ecosystem. |
| **BFF** | Backend-for-Frontend (`orasaka-gateway`). | The GraphQL gateway layer mediating between Core and UI. |
| **RBAC** | Role-Based Access Control (`orasaka-identity`). | Identity management using Java 21 Sealed Interfaces. |
| **Engine** | The `AbstractOrasakaEngine` and its implementations. | Bridges Spring AI models with high-level agentic logic. |
| **Facade** | The `OrasakaAiClient`. | Unified developer entry point for all AI interactions. |
| **Bridge** | Architectural pattern (Bridge Pattern 2.0). | Decouples Orasaka from external AI frameworks. |
| **ToolRegistry** | Registry for local Java methods. | Enables LLMs to invoke native Java logic as "tools". |
| **KnowledgeService** | RAG abstraction for Orasaka. | Orchestrates vector retrieval without direct storage binding. |
| **MCP** | Model Context Protocol. | Standard for integrating external context and tools. |
| **Tools Module** | The `orasaka-tools` module. | Holds concrete implementations of MCP orchestrators and tool registries. |
