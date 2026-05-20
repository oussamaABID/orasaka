# Orasaka API Reference

## Core Facade

### [OrasakaAiClient](../orasaka-core/src/main/java/com/orasaka/core/client/OrasakaAiClient.java)

- **Role**: Primary entry point for developers.
- **Methods**:
  - `chat(OrasakaChatRequest)`: Executes agentic chat interactions.
  - `generateImage(OrasakaImageRequest)`: Triggers image generation flows.
  - `getToolRegistry()`: Access the local tool management system.
  - `getKnowledgeService()`: Access RAG configuration.

## Configuration

### [CoreProperties](../orasaka-core/src/main/java/com/orasaka/core/config/CoreProperties.java)

- **Role**: Type-safe configuration for the `orasaka-core` module.
- **Components**:
  - `defaultProvider`: Sets the global AI provider (Ollama, OpenAI, etc.).
  - `overrides`: Map of provider-specific settings.
  - `rag`: RAG enablement and top-K settings.
  - `mcp`: External MCP server endpoints.

## Engine Abstractions

### [AbstractOrasakaEngine](../orasaka-core/src/main/java/com/orasaka/core/engine/AbstractOrasakaEngine.java)

- **Role**: Abstract bridge implementation (Bridge Pattern 2.0).
- **Responsibility**: Orchestrates RAG context injection, Tool attachment, and Virtual Thread execution based purely on interfaces.

### [McpOrchestrator](../orasaka-core/src/main/java/com/orasaka/core/mcp/McpOrchestrator.java) & [OrasakaToolRegistry](../orasaka-core/src/main/java/com/orasaka/core/tool/OrasakaToolRegistry.java)

- **Role**: Pure interfaces defined in `orasaka-core` to decouple tool and context resolution logic from engine orchestration.

## Tools Implementation (`orasaka-tools`)

### [DefaultMcpOrchestrator](../orasaka-tools/src/main/java/com/orasaka/tools/mcp/DefaultMcpOrchestrator.java)

- **Role**: Concrete implementation of `McpOrchestrator`.
- **Responsibility**: Fetches contexts from external MCP servers via parallel Virtual Threads execution using Java 21 `HttpClient`.

### [DefaultOrasakaToolRegistry](../orasaka-tools/src/main/java/com/orasaka/tools/functions/DefaultOrasakaToolRegistry.java)

- **Role**: Concrete implementation of `OrasakaToolRegistry`.
- **Responsibility**: Maps native Java logic to `FunctionCallback` objects for the LLM.

## Gateway API (GraphQL)

### [ChatController](../orasaka-gateway/src/main/java/com/orasaka/gateway/controller/ChatController.java)

- **Role**: BFF entry point for UI clients.
- **GraphQL Schema**: [schema.graphqls](../orasaka-gateway/src/main/resources/graphql/schema.graphqls)
- **Mutations**:
  - `chat(prompt: String!)`: Returns a multi-modal AI response.
