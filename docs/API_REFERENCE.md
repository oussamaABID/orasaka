# API Reference

> Complete specification of Orasaka's public interfaces, configuration properties, gateway endpoints, and data models.

---

## 🧠 Core Facade

### [AiClient](../orasaka-core/src/main/java/com/orasaka/core/client/AiClient.java)

The primary entry point for all AI capabilities. Every interaction with the Orasaka engine goes through this facade.

| Method | Returns | Description |
|:---|:---|:---|
| `chat(request)` | `InternalChatResponse` | Synchronous agentic chat with session memory |
| `stream(request)` | `Flux<InternalChatResponse>` | Reactive token-by-token streaming |
| `generateImage(request)` | `InternalImageResponse` | Text-to-image generation |
| `generateSpeech(request)` | `byte[]` | Text-to-speech audio data |
| `getToolRegistry()` | `ToolRegistry` | Access the local tool management system |
| `getKnowledgeService()` | `KnowledgeService` | Access RAG configuration and vector search |

---

## ⚙️ Configuration

### [CoreProperties](../orasaka-core/src/main/java/com/orasaka/core/engine/CoreProperties.java)

Type-safe configuration for `orasaka-core`. Prefix: `orasaka.core`

| Property | Type | Description |
|:---|:---|:---|
| `defaultProvider` | `String` | Active AI provider (e.g., `ollama`, `openai`) |
| `overrides` | `Map` | Provider-specific option overrides |
| `rag.enabled` | `boolean` | Toggle RAG context enrichment |
| `rag.topK` | `int` | Number of vector search results to inject |
| `mcp.servers` | `List` | External MCP server endpoints |

### [ToolsProperties](../orasaka-tools/src/main/java/com/orasaka/tools/config/ToolsProperties.java)

Type-safe configuration for `orasaka-tools`. Prefix: `orasaka.tools`

| Property | Type | Description |
|:---|:---|:---|
| `configs[toolId].cache.enabled` | `boolean` | Enable multi-tier caching for this tool |
| `configs[toolId].cache.ttlSeconds` | `int` | Cache entry time-to-live |
| `configs[toolId].rag.enabled` | `boolean` | Enable RAG ingestion for this tool |
| `configs[toolId].rag.chunkerType` | `enum` | `PLAIN_TEXT` · `MARKDOWN_CHUNKERS` · `JSON_ARRAY` |
| `configs[toolId].rag.sourceTable` | `String` | Database table name for RAG sources |

---

## 🏛️ Engine Abstractions

### [AbstractEngine](../orasaka-core/src/main/java/com/orasaka/core/engine/AbstractEngine.java)

Abstract bridge implementation (Bridge Pattern 2.0). Orchestrates RAG injection, tool attachment, and virtual thread execution through pure interfaces.

| Method | Description |
|:---|:---|
| `chat(request)` | Synchronous context-augmented LLM prompt |
| `stream(request)` | Reactive token stream via `resolveChatModel().stream(prompt)` |

### [McpOrchestrator](../orasaka-core/src/main/java/com/orasaka/core/pipeline/McpOrchestrator.java) & [ToolRegistry](../orasaka-core/src/main/java/com/orasaka/core/pipeline/ToolRegistry.java)

Pure interfaces in `orasaka-core` that decouple tool and context resolution from engine orchestration. Concrete implementations live in `orasaka-tools`.

### [Chunker](../orasaka-core/src/main/java/com/orasaka/core/pipeline/Chunker.java) & [ChunkingStrategies](../orasaka-core/src/main/java/com/orasaka/core/pipeline/ChunkingStrategies.java)

Text-splitting abstractions for RAG vectorization:

| Strategy | Behavior |
|:---|:---|
| `PLAIN_TEXT` | Splits by paragraph (double newlines) |
| `MARKDOWN_CHUNKERS` | Splits by markdown headers (`#`, `##`, etc.) |
| `JSON_ARRAY` | Parses array elements into independent documents with metadata |

### [ContextInterceptor](../orasaka-core/src/main/java/com/orasaka/core/pipeline/ContextInterceptor.java)

Interface defining pre/post-processing hooks for LLM operations. Ordered implementations form the context-matrix pipeline:

| Order | Interceptor | What it does |
|:---:|:---|:---|
| 1 | `UserContextResolver` | Enriches context with user attributes, preferences, security clearances |
| 2 | `SystemContextInjector` | Injects system signals, environment states, tool configurations |
| 3 | `RefinerInterceptor` | Refines inputs via `system-refinement.st` template + conversation history |
| 4 | `RouterInterceptor` | Routes requests using `system-router.st` at `temperature: 0.0` |

---

## 🛠️ Tools Implementation

### [DefaultMcpOrchestrator](../orasaka-tools/src/main/java/com/orasaka/tools/mcp/DefaultMcpOrchestrator.java)

Concrete `McpOrchestrator` implementation. Fetches context from external MCP servers via parallel virtual threads using Java 21 `HttpClient`.

### [DefaultToolRegistry](../orasaka-tools/src/main/java/com/orasaka/tools/functions/DefaultToolRegistry.java)

Concrete `ToolRegistry` implementation. Maps Java methods to LLM tools, applies caching decorators, and drives RAG ingestion.

| Method | Description |
|:---|:---|
| `registerTool(name, description, inputType, function)` | Registers a Java method as an LLM-callable tool |
| `getRegisteredTools()` | Returns active tools, auto-wrapped in `CachingToolCallback` if caching is enabled |
| `triggerIngestion()` | Scans un-ingested database rows and maps them to vector stores |

### [ToolCacheService](../orasaka-tools/src/main/java/com/orasaka/tools/functions/ToolCacheService.java)

Multi-tier cache orchestrator for tool outputs:

| Tier | Backend | Details |
|:---|:---|:---|
| **Tier 1 (Memory)** | Caffeine | Fast local cache, max 5000 entries |
| **Tier 2 (Database)** | PostgreSQL (`orasaka_tools_cache`) | Persistent, cross-server synchronization |

### [CachingToolCallback](../orasaka-tools/src/main/java/com/orasaka/tools/functions/CachingToolCallback.java)

Decorator pattern for Spring AI `ToolCallback`. Intercepts tool invocations, returns cached results on hit, and stores new results on miss.

---

## 📹 Video Generation

### [VideoService](../orasaka-core/src/main/java/com/orasaka/core/infrastructure/video/VideoService.java)

Executes text-to-video generation using the local `sd-server` runner on port `8086`.

### [VideoRequest](../orasaka-core/src/main/java/com/orasaka/core/ingest/video/VideoRequest.java)

Immutable record for video generation requests:

| Field | Type | Default | Validation |
|:---|:---|:---|:---|
| `prompt` | `String` | — | Non-null, non-blank (compact constructor) |
| `durationSeconds` | `Integer` | `4` | Null-safe default |
| `settings` | `Map<String, Object>` | `{}` | Defensive copy |
| `context` | `Context` | — | Active request context |

### [VideoResponse](../orasaka-core/src/main/java/com/orasaka/core/ingest/video/VideoResponse.java)

| Field | Type | Default | Validation |
|:---|:---|:---|:---|
| `videoData` | `byte[]` | — | Non-null, non-empty |
| `format` | `String` | `"mp4"` | Blank-safe default |

---

## 🛡️ Context & Security Models

### [Context](../orasaka-core/src/main/java/com/orasaka/core/support/Context.java)

Immutable request envelope carrying user preferences and security privileges:

| Field | Type | Description |
|:---|:---|:---|
| `userId` | `String` | Authenticated user identifier |
| `conversationId` | `String` | Active session thread ID |
| `preferences` | `Map<String, Object>` | Unmodifiable user overrides |
| `authorities` | `Set<Authority>` | Unmodifiable security roles |

**Method:** `hasAuthority(String)` — Thread-safe, case-insensitive authority lookup.

### [Authority](../orasaka-core/src/main/java/com/orasaka/core/support/Authority.java)

Immutable record representing a single security role (e.g., `"ROLE_USER"`). Non-null, non-blank name enforced by compact constructor.

### [SpeechRequest](../orasaka-core/src/main/java/com/orasaka/core/support/SpeechRequest.java)

| Field | Type | Description |
|:---|:---|:---|
| `text` | `String` | Text content to convert to speech |
| `options` | `Object` | Provider-specific TTS configuration |
| `context` | `Context` | User preferences (voice model, speed) |

### [Role](../orasaka-identity/src/main/java/com/orasaka/identity/domain/Role.java)

Sealed interface RBAC hierarchy (Java 21):

| Record | Authority | Access Level |
|:---|:---|:---|
| `Role.Admin` | `"ADMIN"` | Full access |
| `Role.User` | `"USER"` | Standard execution |
| `Role.Guest` | `"GUEST"` | Read-only, sandboxed |

> [!NOTE]
> Within `orasaka-core`, roles are represented as `Authority` records (plain `name` strings). The `Role` sealed interface lives in `orasaka-identity` exclusively.

### [IdentityService](../orasaka-identity/src/main/java/com/orasaka/identity/service/IdentityService.java)

| Method | Description |
|:---|:---|
| `getUser(userId)` | Resolves a `User` by UUID string, returns `null` if not found |
| `authenticate(email, password)` | Verifies credentials, returns user profile |
| `register(username, email, password, language)` | Self-service registration with optional verification tokens |
| `updatePreferences(userId, preferences)` | Merges preferences, returns updated `User` from in-memory mapping |
| `verifyToken(token)` | Validates email verification hashes |
| `resolveInterception(userId, type, schemaId, responses)` | Merges responses into preferences, clears the interception |

---

## 🚀 Gateway API

All frontend clients communicate through the BFF layer. Direct access to `orasaka-core` or Ollama ports is prohibited.

### GraphQL — [AiController](../orasaka-gateway/src/main/java/com/orasaka/gateway/endpoint/AiController.java)

**Schema:** [schema.graphqls](../orasaka-gateway/src/main/resources/graphql/schema.graphqls)

#### Queries

| Query | Returns | Description |
|:---|:---|:---|
| `me` | `User` | Current authenticated user profile |
| `interceptionSchema(schemaId)` | `String` | Dynamic JSON schema for onboarding/feedback |
| `operationGraph` | `OperationGraph` | Server-driven UI capability graph |

#### Mutations

| Mutation | Returns | Description |
|:---|:---|:---|
| `chat(prompt, conversationId?)` | `ChatResponse` | Single-turn agentic chat |
| `image(prompt)` | `ChatResponse` | Text-to-image via stable-diffusion.cpp (port `8085`) |
| `speech(prompt)` | `ChatResponse` | Text-to-speech via OpenAI TTS |
| `updatePreferences(preferences)` | `User` | Merge and save user preference overrides |
| `register(username, email, password, language?)` | `RegisterResult` | Self-service registration |
| `resolveInterception(type, schemaId, responses)` | `Boolean` | Resolve active user interception |

#### Subscriptions

| Subscription | Returns | Description |
|:---|:---|:---|
| `chatStream(prompt, conversationId?)` | `ChatResponse` | Real-time word-by-word streaming |

### REST — [AuthController](../orasaka-gateway/src/main/java/com/orasaka/gateway/endpoint/AuthController.java) & [ChatStreamController](../orasaka-gateway/src/main/java/com/orasaka/gateway/endpoint/ChatStreamController.java)

#### Authentication

| Endpoint | Method | Payload | Response |
|:---|:---|:---|:---|
| `/api/v1/auth/login` | `POST` | `{"email": "...", "password": "..."}` | `{"token": "<uuid>", "username": "..."}` |
| `/api/v1/auth/oauth` | `POST` | `{"email": "...", "username": "..."}` | `{"token": "<uuid>", "username": "..."}` |
| `/api/v1/auth/register` | `POST` | `{"username", "email", "password", "language"}` | Registration result |
| `/api/v1/auth/verify` | `POST` | `{"token": "<verification-token>"}` | Activation result |

#### Streaming & Video

| Endpoint | Method | Content-Type | Description |
|:---|:---|:---|:---|
| `/api/v1/chat/stream/{conversationId}` | `GET` | `text/event-stream` | SSE token streaming |
| `/api/v1/ai/video` | `POST` | `application/json` | Text-to-video generation |

**Video Request:**
```json
{
  "prompt": "A cinematic shot of cyberpunk streets, neon lighting, heavy rain, 4k",
  "durationSeconds": 4,
  "settings": { "fps": 24, "width": 512, "height": 512 }
}
```

**Video Response (RFC 2397 Data URL):**
```json
{
  "format": "mp4",
  "url": "data:video/mp4;base64,AAAAIGZ0eXBtcDQy..."
}
```

### BFF Extensions (Next.js Server-Side)

| Mutation | Description |
|:---|:---|
| `VerifyEmail(token)` | Server-side M2M REST call to `/api/v1/auth/verify` |

---

## 🎬 Media Pre-Processor Contracts

Port/adapter contracts in `orasaka-core/ingest/` for preprocessing media before AI inference:

| Interface | Output Record | Key Fields |
|:---|:---|:---|
| [ImagePreProcessor](../orasaka-core/src/main/java/com/orasaka/core/ingest/image/ImagePreProcessor.java) | `ProcessedImagePayload` | `base64Image`, `width`, `height` (non-negative) |
| [AudioPreProcessor](../orasaka-core/src/main/java/com/orasaka/core/ingest/audio/AudioPreProcessor.java) | `ProcessedAudioPayload` | `transcript` (defaults to empty string) |
| [VideoPreProcessor](../orasaka-core/src/main/java/com/orasaka/core/ingest/video/VideoPreProcessor.java) | `ProcessedVideoPayload` | Extracted frames + audio data |

> [!NOTE]
> All pre-processor implementations must be **package-private** per ADR-019. Only the interfaces and payload records are public.

---

## 🖥️ CLI Client (v2.0.0)

### Commands

| Command | Description | Protocol |
|:---|:---|:---|
| `login [email] [password]` | Authenticate and cache JWT | `POST /api/v1/auth/login` |
| `register [user] [email]` | Self-service registration | `POST /api/v1/auth/register` |
| `verify <token>` | Email verification | `POST /api/v1/auth/verify` |
| `chat [prompt]` | Interactive or single-shot chat | SSE streaming |
| `chat --gen-image <prompt>` | Generate image from text | GraphQL `mutation { image }` |
| `chat --speech <text>` | Text-to-speech synthesis | GraphQL `mutation { speech }` |
| `chat --image <filepath>` | Vision analysis of local image | Operation Graph |
| `chat --audio <filepath>` | Audio analysis of local file | Operation Graph |
| `chat --save <path>` | Save generated media to file | — |
| `video <prompt>` | Text-to-video generation | `POST /api/v1/ai/video` |
| `profile` | Display user profile | GraphQL `query { me }` |
| `settings get` | Show current preferences | GraphQL `query { me }` |
| `settings set <key> <val>` | Update single preference | GraphQL mutation |
| `graph` | Display operation graph | GraphQL query |

### Interactive Meta-Commands

| Command | Description |
|:---|:---|
| `/thread new` | Create and switch to a new conversation |
| `/thread list` | List all stored threads |
| `/thread switch <id>` | Switch active thread (prefix match) |

### Local Persistence

| File | Content |
|:---|:---|
| `~/.orasaka-cli.json` | JWT token, username, active thread, thread list |
| `~/.orasaka-threads/<uuid>.json` | Per-thread message history |

---

## 📖 Javadoc Generation

The root `pom.xml` configures `maven-javadoc-plugin` **3.6.3** in aggregate mode:

| Property | Value |
|:---|:---|
| Java source level | `21` (records, sealed types, virtual threads) |
| Doclint | `none` (prevents CI noise) |
| Output | `docs/apidocs/` |
| Lifecycle phase | `package` |

```bash
# Generate via lifecycle
mvn package -DskipTests

# Direct generation (faster)
mvn javadoc:aggregate
```

**Output:** [docs/apidocs/index.html](apidocs/index.html)

---

## 📎 Related Documentation

| Document | Description |
|:---|:---|
| [Architecture Reference](ARCHITECTURE.md) | System topology, module boundaries, execution flows |
| [Glossary](GLOSSARY.md) | Ecosystem terms, patterns, environment variables |
| [ADR Log](CONTEXT.md) | 24 Architectural Decision Records |
| [Business Guide](BUSINESS_IMPLEMENTATION.md) | Step-by-step feature implementation blueprint |
