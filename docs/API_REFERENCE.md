# Orasaka API Reference

This document specifies the public API interfaces, configuration properties, gateway endpoints, and core data models of the Orasaka platform.

---

## 🧠 Core Facade

### [OrasakaAiClient](../orasaka-core/src/main/java/com/orasaka/core/client/OrasakaAiClient.java)

- **Role**: Primary entry point for developers consuming Orasaka AI capabilities.
- **Methods**:
  - `chat(OrasakaChatRequest)`: Executes agentic chat interactions under isolated session memory.
  - `stream(OrasakaChatRequest)`: Streams reactive tokens as a `Flux<OrasakaChatResponse>`.
  - `generateImage(OrasakaImageRequest)`: Triggers image generation flows.
  - `getToolRegistry()`: Access the local tool management system.
  - `getKnowledgeService()`: Access RAG configuration.

---

## ⚙️ Configuration Properties

### [CoreProperties](../orasaka-core/src/main/java/com/orasaka/core/config/CoreProperties.java)

- **Role**: Type-safe configuration for the `orasaka-core` module.
- **Components**:
  - `defaultProvider`: Sets the global active AI provider (e.g., Ollama, OpenAI).
  - `overrides`: Map of provider-specific options.
  - `rag`: RAG enablement status and search parameter overrides.
  - `mcp`: Configured external Model Context Protocol (MCP) server endpoints.

### [OrasakaToolsProperties](../orasaka-tools/src/main/java/com/orasaka/tools/config/OrasakaToolsProperties.java)

- **Role**: Type-safe configuration properties mapping tool IDs to cache and RAG policies.
- **Prefix**: `orasaka.tools`
- **Components**:
  - `configs`: Map of tool ID to `ToolConfig` blocks:
    - `cache`: `CacheConfig` specifying whether caching is active (`enabled`) and the entry Time-To-Live (`ttlSeconds`).
    - `rag`: `RagConfig` specifying RAG database status (`enabled`), `chunkerType` (e.g. `PLAIN_TEXT`, `MARKDOWN_CHUNKERS`, `JSON_ARRAY`), and the `sourceTable` name.

---

## 🏛️ Engine Abstractions

### [AbstractOrasakaEngine](../orasaka-core/src/main/java/com/orasaka/core/engine/AbstractOrasakaEngine.java)

- **Role**: Abstract bridge implementation (Bridge Pattern 2.0).
- **Responsibility**: Orchestrates RAG context injection, Tool attachment, and Virtual Thread execution based purely on interfaces.
- **Methods**:
  - `chat(OrasakaChatRequest)`: Executes a synchronous context-augmented LLM prompt.
  - `stream(OrasakaChatRequest)`: Executes a reactive token-augmented stream using `resolveChatModel().stream(prompt)`.

### [McpOrchestrator](../orasaka-core/src/main/java/com/orasaka/core/mcp/McpOrchestrator.java) & [OrasakaToolRegistry](../orasaka-core/src/main/java/com/orasaka/core/tool/OrasakaToolRegistry.java)

- **Role**: Pure interfaces defined in `orasaka-core` to decouple tool and context resolution logic from engine orchestration.

### [OrasakaChunker](../orasaka-core/src/main/java/com/orasaka/core/rag/OrasakaChunker.java) & [OrasakaChunkingStrategies](../orasaka-core/src/main/java/com/orasaka/core/rag/OrasakaChunkingStrategies.java)

- **Role**: Core text-splitting abstractions for RAG vectorization.
- **Strategies**:
  - `PLAIN_TEXT`: Splits raw content strings into paragraphs (separated by double newlines).
  - `MARKDOWN_CHUNKERS`: Identifies and splits sections using Markdown headers (`#`, `##`, etc.).
  - `JSON_ARRAY`: Parses array elements into independent documents, appending structured fields to metadata mappings.

### [OrasakaBackgroundScheduler](../orasaka-core/src/main/java/com/orasaka/core/rag/OrasakaBackgroundScheduler.java)

- **Role**: Daily automated execution scheduler running on a lightweight virtual thread background pool.
- **Cron Trigger**: Daily at 3:00 AM (configured via `${orasaka.tools.rag.cron:0 0 3 * * ?}`).
- **Behavior**: Inspects registry configurations, triggers asynchronous RAG vector store generation if ingestion is active, and safely skips execution via a passive bypass if disabled.

---

## 🛠️ Tools Implementation (`orasaka-tools`)

### [DefaultMcpOrchestrator](../orasaka-tools/src/main/java/com/orasaka/tools/mcp/DefaultMcpOrchestrator.java)

- **Role**: Concrete implementation of `McpOrchestrator`.
- **Responsibility**: Fetches contexts from external MCP servers via parallel Virtual Threads execution using Java 21 `HttpClient`.

### [DefaultOrasakaToolRegistry](../orasaka-tools/src/main/java/com/orasaka/tools/functions/DefaultOrasakaToolRegistry.java)

- **Role**: Concrete implementation of `OrasakaToolRegistry`.
- **Responsibility**: Maps native Java methods to LLM tools, decorates cached tools on retrieval, and drives database RAG source scanning (`orasaka_tools_rag_source` table) and chunking.
- **Methods**:
  - `registerTool(name, description, inputType, function)`: Adds a local Java method to the tools cache.
  - `getRegisteredTools()`: Resolves active tools, automatically wrapping them inside `CachingToolCallback` if a caching policy is enabled.
  - `triggerIngestion()`: Scans non-ingested source database rows and maps them to vector stores via chunker engines.

### [ToolCacheService](../orasaka-tools/src/main/java/com/orasaka/tools/functions/ToolCacheService.java)

- **Role**: Multi-tier cache orchestrator managing tool outputs.
- **Tiers**:
  - *Tier 1 (Memory)*: Fast local Caffeine cache instance limited to 5000 entries.
  - *Tier 2 (Database)*: Persistent PostgreSQL storage table (`orasaka_tools_cache`) ensuring cross-server synchronization and persistence.
- **Methods**:
  - `get(toolId, key)`: Resolves active cache values, returning null if expired or missing.
  - `put(toolId, key, value, ttlSeconds)`: Writes entries concurrently to Caffeine and PostgreSQL tiers.

### [CachingToolCallback](../orasaka-tools/src/main/java/com/orasaka/tools/functions/CachingToolCallback.java)

- **Role**: Caching decorator pattern implementation for Spring AI `ToolCallback`.
- **Behavior**: Intercepts LLM tool invocations, returns hit targets from `ToolCacheService` immediately to save execution overhead, and caches execution results upon cache misses.

---

## 🛡️ Context & Security Models

### [OrasakaContext](../orasaka-core/src/main/java/com/orasaka/core/context/OrasakaContext.java)

- **Role**: Immutable request envelope carrying user preferences and security privileges.
- **Fields**:
  - `userId`: Unique identifier of the authenticated user.
  - `conversationId`: Active session thread identifier.
  - `preferences`: Map of user overrides (e.g., specific voices, model names, aspects).
  - `authorities`: Set of granted `OrasakaAuthority` records.

### [Role](../orasaka-identity/src/main/java/com/orasaka/identity/domain/Role.java)

- **Role**: Domain-driven Role-Based Access Control hierarchy implemented using Java 21 **Sealed Interfaces**.
- **Permitted Records**:
  - `Role.Admin`: Full access rights (resolves to `"ADMIN"`).
  - `Role.User`: Standard execution privileges (resolves to `"USER"`).
  - `Role.Guest`: Read-only, sandboxed access (resolves to `"GUEST"`).

### [IdentityService](../orasaka-identity/src/main/java/com/orasaka/identity/service/IdentityService.java)

- **Role**: Backend Service managing user authentication, profile resolution, passive registration verification, and user interceptions.
- **Methods**:
  - `authenticate(email, password)`: Parameterized verification of active user credentials.
  - `register(username, email, password, language)`: Self-service registration. Optionally emits verification tokens and/or triggers dynamic onboarding blocks depending on properties configurations.
  - `verifyToken(token)`: Parameterized lookup and verification of active user registration hashes in the `orasaka_verification_tokens` table.
  - `triggerInterception(userId, interceptionType, schemaId)`: Registers active interceptions (e.g. feedback, onboarding blocks) in `orasaka_user_interceptions`.
  - `resolveInterception(userId, interceptionType, schemaId, responses)`: Merges interception response payloads into user preferences and clears the interception block.

---

## 🚀 Gateway API (BFF Layer)

All frontend clients communicate solely through the BFF layer. Direct communication with the backend core engine or Ollama ports is prohibited.

### 1. GraphQL Gateway: [AiController](../orasaka-gateway/src/main/java/com/orasaka/gateway/graphql/AiController.java)

- **GraphQL Schema**: [schema.graphqls](../orasaka-gateway/src/main/resources/graphql/schema.graphqls)
- **Queries**:
  - `me: User`: Resolves the profile and preferences of the currently authenticated user.
  - `interceptionSchema(schemaId: String!): String`: Fetches the dynamic JSON schema string for onboarding or feedback blocks.
- **Mutations**:
  - `chat(prompt: String!, conversationId: String): ChatResponse`: Executes a single-turn agentic chat.
  - `updatePreferences(preferences: Map!): User`: Merges and saves user preference overrides.
  - `register(username: String!, email: String!, password: String!, language: String): RegisterResult!`: Triggers registration flow.
  - `resolveInterception(interceptionType: String!, schemaId: String!, responses: Map!): Boolean!`: Validates and resolves active user interception preferences.
- **Subscriptions**:
  - `chatStream(prompt: String!, conversationId: String): ChatResponse`: Reactive subscription streaming word-by-word responses.

### 2. REST API Gateway: [ChatStreamController](../orasaka-gateway/src/main/java/com/orasaka/gateway/controller/ChatStreamController.java) & [VerificationController](../orasaka-gateway/src/main/java/com/orasaka/gateway/controller/VerificationController.java)

- **Authentication & Verification Endpoints**:
  - `POST /api/v1/auth/login`: Authenticates the user and returns their UUID session token.
    - *Payload*: `{"email": "<email>", "password": "<pass>"}`
    - *Response*: `{"token": "<user-uuid>", "username": "<name>"}`
  - `POST /api/v1/auth/verify`: Private machine-to-machine validation endpoint to activate user accounts using email tokens.
    - *Payload*: `{"token": "<verification-token>"}`
    - *Response*: `{"username": "<name>", "email": "<email>", "enabled": true}`
- **Server-Sent Events (SSE) Streaming**:
  - `GET /api/v1/chat/stream/{conversationId}?prompt=...`
    - *Headers*: `Authorization: Bearer <userId>`
    - *Response Type*: `text/event-stream`
    - *Payload*: Event chunks of serialized JSON `OrasakaChatResponse` records.

### 3. BFF GraphQL Server-Side Extensions (Next.js BFF)

To maintain security boundaries, public validation endpoints are proxied as BFF mutations:
- **Mutations**:
  - `VerifyEmail(token: String!)`: Executed on the Next.js server, making an internal private M2M REST call to `orasaka-gateway` at `/api/v1/auth/verify`.

---

## 🔧 Build & DevOps

### Javadoc Aggregation

The root [pom.xml](../pom.xml) configures `maven-javadoc-plugin` **3.6.3** in aggregate mode so that a single lifecycle command generates a unified API reference spanning all sub-modules (`orasaka-core`, `orasaka-identity`, `orasaka-gateway`, `orasaka-tools`).

| Property | Value | Notes |
|---|---|---|
| `source` | `21` | Records, sealed types, virtual threads |
| `doclint` | `none` | Prevents formatting noise from breaking CI |
| `reportOutputDirectory` | `${project.basedir}/docs` | Plugin appends `/apidocs` → final path is `docs/apidocs/` |
| Lifecycle phase | `package` | Docs stay in sync with every packaged build |

**Generate / refresh the unified API site:**

```bash
# Via lifecycle (runs compile + test + package + javadoc aggregate)
mvn package -DskipTests

# Via direct plugin goal (faster, no compile/test cycle)
mvn javadoc:aggregate
```

**Output location:** `docs/apidocs/index.html` — the entry point for the full cross-module Javadoc site.
