# API Reference

> Complete specification of Orasaka's public interfaces, configuration properties, gateway endpoints, and data models.

---

## 🧠 Core Facade

### [AiClient](../orasaka-core/src/main/java/com/orasaka/core/client/AiClient.java)

The primary entry point for all AI capabilities. Every interaction with the Orasaka engine goes through this facade.

| Method                    | Returns              | Description                                  |
| :------------------------ | :------------------- | :------------------------------------------- |
| `chat(request)`           | `ChatResponse`       | Synchronous agentic chat with session memory |
| `stream(request)`         | `Flux<ChatResponse>` | Reactive token-by-token streaming            |
| `generateImage(request)`  | `ImageResponse`      | Text-to-image generation                     |
| `generateSpeech(request)` | `byte[]`             | Text-to-speech audio data                    |
| `getToolRegistry()`       | `ToolRegistry`       | Access the local tool management system      |
| `getKnowledgeService()`   | `KnowledgeService`   | Access RAG configuration and vector search   |

---

## ⚙️ Configuration

### [CoreProperties](../orasaka-core/src/main/java/com/orasaka/core/config/CoreProperties.java)

Type-safe configuration for `orasaka-core`. Prefix: `orasaka.core`

| Property          | Type      | Description                                   |
| :---------------- | :-------- | :-------------------------------------------- |
| `defaultProvider` | `String`  | Active AI provider (e.g., `ollama`, `openai`) |
| `overrides`       | `Map`     | Provider-specific option overrides            |
| `rag.enabled`     | `boolean` | Toggle RAG context enrichment                 |
| `rag.topK`        | `int`     | Number of vector search results to inject     |
| `mcp.servers`     | `List`    | External MCP server endpoints                 |

### [ToolsProperties](../orasaka-tools/src/main/java/com/orasaka/tools/config/ToolsProperties.java)

Type-safe configuration for `orasaka-tools`. Prefix: `orasaka.tools`

| Property                           | Type      | Description                                       |
| :--------------------------------- | :-------- | :------------------------------------------------ |
| `configs[toolId].cache.enabled`    | `boolean` | Enable multi-tier caching for this tool           |
| `configs[toolId].cache.ttlSeconds` | `int`     | Cache entry time-to-live                          |
| `configs[toolId].rag.enabled`      | `boolean` | Enable RAG ingestion for this tool                |
| `configs[toolId].rag.chunkerType`  | `enum`    | `PLAIN_TEXT` · `MARKDOWN_CHUNKERS` · `JSON_ARRAY` |
| `configs[toolId].rag.sourceTable`  | `String`  | Database table name for RAG sources               |

---

## 🏛️ Engine Abstractions

### [AbstractEngine](../orasaka-core/src/main/java/com/orasaka/core/execution/AbstractEngine.java)

Abstract bridge implementation (Bridge Pattern 2.0). Orchestrates RAG injection, tool attachment, and virtual thread execution through pure interfaces.

| Method            | Description                                                   |
| :---------------- | :------------------------------------------------------------ |
| `chat(request)`   | Synchronous context-augmented LLM prompt                      |
| `stream(request)` | Reactive token stream via `resolveChatModel().stream(prompt)` |

### [McpOrchestrator](../orasaka-core/src/main/java/com/orasaka/core/pipeline/interceptors/tool/McpOrchestrator.java) & [ToolRegistry](../orasaka-core/src/main/java/com/orasaka/core/pipeline/interceptors/tool/ToolRegistry.java)

Pure interfaces in `orasaka-core` that decouple tool and context resolution from engine orchestration. Concrete implementations live in `orasaka-tools`.

### [Chunker](../orasaka-core/src/main/java/com/orasaka/core/pipeline/chunking/Chunker.java) & [ChunkingStrategies](../orasaka-core/src/main/java/com/orasaka/core/pipeline/chunking/ChunkingStrategies.java)

Text-splitting abstractions for RAG vectorization:

| Strategy            | Behavior                                                       |
| :------------------ | :------------------------------------------------------------- |
| `PLAIN_TEXT`        | Splits by paragraph (double newlines)                          |
| `MARKDOWN_CHUNKERS` | Splits by markdown headers (`#`, `##`, etc.)                   |
| `JSON_ARRAY`        | Parses array elements into independent documents with metadata |

### [ContextInterceptor](../orasaka-core/src/main/java/com/orasaka/core/pipeline/interceptors/contract/ContextInterceptor.java)

Interface defining pre/post-processing hooks for LLM operations. Ordered implementations form the context-matrix pipeline:

| Order | Interceptor             | What it does                                                              |
| :---: | :---------------------- | :------------------------------------------------------------------------ |
|   1   | `UserContextResolver`   | Enriches context with user attributes, preferences, security clearances   |
|   2   | `SystemContextInjector` | Injects system signals, environment states, tool configurations           |
|   3   | `RefinerInterceptor`    | Refines inputs via `system-refinement.st` template + conversation history |
|   4   | `RouterInterceptor`     | Routes requests using `system-router.st` at `temperature: 0.0`            |

---

## 🛠️ Tools Implementation

### [DefaultMcpOrchestrator](../orasaka-tools/src/main/java/com/orasaka/tools/mcp/DefaultMcpOrchestrator.java)

Concrete `McpOrchestrator` implementation. Fetches context from external MCP servers via parallel virtual threads using Java 21 `HttpClient`.

### [DefaultToolRegistry](../orasaka-tools/src/main/java/com/orasaka/tools/functions/DefaultToolRegistry.java)

Concrete `ToolRegistry` implementation. Maps Java methods to LLM tools, applies caching decorators, and drives RAG ingestion.

| Method                                                 | Description                                                                       |
| :----------------------------------------------------- | :-------------------------------------------------------------------------------- |
| `registerTool(name, description, inputType, function)` | Registers a Java method as an LLM-callable tool                                   |
| `getRegisteredTools()`                                 | Returns active tools, auto-wrapped in `CachingToolCallback` if caching is enabled |
| `triggerIngestion()`                                   | Scans un-ingested database rows and maps them to vector stores                    |

### [ToolCacheService](../orasaka-tools/src/main/java/com/orasaka/tools/functions/ToolCacheService.java)

Multi-tier cache orchestrator for tool outputs:

| Tier                  | Backend                            | Details                                  |
| :-------------------- | :--------------------------------- | :--------------------------------------- |
| **Tier 1 (Memory)**   | Caffeine                           | Fast local cache, max 5000 entries       |
| **Tier 2 (Database)** | PostgreSQL (`orasaka_tools_cache`) | Persistent, cross-server synchronization |

### [CachingToolCallback](../orasaka-tools/src/main/java/com/orasaka/tools/functions/CachingToolCallback.java)

Decorator pattern for Spring AI `ToolCallback`. Intercepts tool invocations, returns cached results on hit, and stores new results on miss.

---

## 📹 Video Generation

### [VideoService](../orasaka-core/src/main/java/com/orasaka/core/domain/video/VideoService.java)

Executes text-to-video generation using the local Python SVD XT worker on port `8188`.

### [VideoRequest](../orasaka-core/src/main/java/com/orasaka/core/ingest/video/VideoRequest.java)

Immutable record for video generation requests:

| Field             | Type                  | Default | Validation                                |
| :---------------- | :-------------------- | :------ | :---------------------------------------- |
| `prompt`          | `String`              | —       | Non-null, non-blank (compact constructor) |
| `durationSeconds` | `Integer`             | `4`     | Null-safe default                         |
| `settings`        | `Map<String, Object>` | `{}`    | Defensive copy                            |
| `context`         | `Context`             | —       | Active request context                    |

### [VideoResponse](../orasaka-core/src/main/java/com/orasaka/core/ingest/video/VideoResponse.java)

| Field       | Type     | Default | Validation          |
| :---------- | :------- | :------ | :------------------ |
| `videoData` | `byte[]` | —       | Non-null, non-empty |
| `format`    | `String` | `"mp4"` | Blank-safe default  |

---

## 🛡️ Context & Security Models

### [Context](../orasaka-core/src/main/java/com/orasaka/core/context/Context.java)

Immutable request envelope carrying user preferences and security privileges:

| Field            | Type                  | Description                   |
| :--------------- | :-------------------- | :---------------------------- |
| `userId`         | `String`              | Authenticated user identifier |
| `conversationId` | `String`              | Active session thread ID      |
| `preferences`    | `Map<String, Object>` | Unmodifiable user overrides   |
| `authorities`    | `Set<Authority>`      | Unmodifiable security roles   |

**Method:** `hasAuthority(String)` — Thread-safe, case-insensitive authority lookup.

### [Authority](../orasaka-core/src/main/java/com/orasaka/core/context/Authority.java)

Immutable record representing a single security role (e.g., `"ROLE_USER"`). Non-null, non-blank name enforced by compact constructor.

### [SpeechRequest](../orasaka-core/src/main/java/com/orasaka/core/domain/model/speech/SpeechRequest.java)

| Field     | Type      | Description                           |
| :-------- | :-------- | :------------------------------------ |
| `text`    | `String`  | Text content to convert to speech     |
| `options` | `Object`  | Provider-specific TTS configuration   |
| `context` | `Context` | User preferences (voice model, speed) |

### [Role](../orasaka-identity/src/main/java/com/orasaka/identity/domain/Role.java)

Sealed interface RBAC hierarchy (Java 21):

| Record       | Authority | Access Level         |
| :----------- | :-------- | :------------------- |
| `Role.Admin` | `"ADMIN"` | Full access          |
| `Role.User`  | `"USER"`  | Standard execution   |
| `Role.Guest` | `"GUEST"` | Read-only, sandboxed |

> [!NOTE]
> Within `orasaka-core`, roles are represented as `Authority` records (plain `name` strings). The `Role` sealed interface lives in `orasaka-identity` exclusively.

### [IdentityService](../orasaka-identity/src/main/java/com/orasaka/identity/domain/ports/inbound/IdentityService.java)

| Method                                                   | Description                                                                    |
| :------------------------------------------------------- | :----------------------------------------------------------------------------- |
| `getUser(userId)`                                        | Resolves a `User` by UUID string. Throws `UserNotFoundException` if not found  |
| `authenticate(email, password)`                          | Verifies credentials. Throws `BadCredentialsException` on mismatch             |
| `register(username, email, password, language)`          | Self-service registration with optional verification tokens                    |
| `updatePreferences(userId, preferences)`                 | Merges preferences, returns updated `User` from in-memory mapping              |
| `verifyToken(token)`                                     | Validates email verification hashes                                            |
| `resolveInterception(userId, type, schemaId, responses)` | Merges responses into preferences, clears the interception                     |
| `getUserCredentials(userId)`                              | Returns list of `UserCredential` records for provider management               |

> [!NOTE]
> Per ERR-106 (Null-Return Prohibition), `getUser()` and `authenticate()` throw explicit unchecked exceptions instead of returning `null`. Procedural `if (result == null)` blocks in controllers are strictly forbidden.

### [PasswordRecoveryService](../orasaka-identity/src/main/java/com/orasaka/identity/domain/ports/inbound/PasswordRecoveryService.java)

Separated from `IdentityService` to respect Single Responsibility. Handles the full password recovery lifecycle with zero-enumeration protection.

| Method                              | Description                                                                                                         |
| :---------------------------------- | :------------------------------------------------------------------------------------------------------------------ |
| `requestPasswordReset(email)`       | Generates SHA-256 hashed token with 15-minute expiry. Always succeeds (zero-enumeration). Publishes `PasswordResetRequestedEvent`. |
| `resetPassword(token, newPassword)` | Validates token hash, updates BCrypt password, deletes token. Throws `InvalidRequestException` on failure.          |

> [!IMPORTANT]
> Tokens are single-use and auto-expire after 15 minutes. The `password_changed_at` column is updated atomically to enable downstream session invalidation.

---

## 🚀 Gateway API

All frontend clients communicate through the BFF layer. Direct access to `orasaka-core` or Ollama ports is prohibited.

### GraphQL — [AiController](../orasaka-gateway/src/main/java/com/orasaka/gateway/infrastructure/adapter/graphql/AiController.java)

**Schema:** [schema.graphqls](../orasaka-gateway/src/main/resources/graphql/schema.graphqls)

#### Queries

| Query                          | Returns          | Description                                 |
| :----------------------------- | :--------------- | :------------------------------------------ |
| `me`                           | `User`           | Current authenticated user profile          |
| `interceptionSchema(schemaId)` | `String`         | Dynamic JSON schema for onboarding/feedback |
| `operationGraph`               | `OperationGraph` | Server-driven UI capability graph           |

#### Mutations

| Mutation                                         | Returns          | Description                                          |
| :----------------------------------------------- | :--------------- | :--------------------------------------------------- |
| `chat(prompt, conversationId?)`                  | `ChatResponse`   | Single-turn agentic chat                             |
| `image(prompt)`                                  | `ChatResponse`   | Text-to-image via stable-diffusion.cpp (port `8085`) |
| `speech(prompt)`                                 | `ChatResponse`   | Text-to-speech via OpenAI TTS                        |
| `updatePreferences(preferences)`                 | `User`           | Merge and save user preference overrides             |
| `register(username, email, password, language?)` | `RegisterResult` | Self-service registration                            |
| `resolveInterception(type, schemaId, responses)` | `Boolean`        | Resolve active user interception                     |

#### Subscriptions

| Subscription                          | Returns        | Description                      |
| :------------------------------------ | :------------- | :------------------------------- |
| `chatStream(prompt, conversationId?)` | `ChatResponse` | Real-time word-by-word streaming |

### REST — [AuthController](../orasaka-gateway/src/main/java/com/orasaka/gateway/infrastructure/adapter/rest/AuthController.java), [ChatStreamController](../orasaka-gateway/src/main/java/com/orasaka/gateway/infrastructure/adapter/rest/ChatStreamController.java) & [JobController](../orasaka-gateway/src/main/java/com/orasaka/gateway/infrastructure/adapter/rest/JobController.java)

#### Authentication

| Endpoint                | Method | Payload                                         | Response                                 |
| :---------------------- | :----- | :---------------------------------------------- | :--------------------------------------- |
| `/api/v1/auth/login`    | `POST` | `{"email": "...", "password": "..."}`           | `{"token": "<uuid>", "username": "..."}` |
| `/api/v1/auth/oauth`    | `POST` | `{"email": "...", "username": "..."}`           | `{"token": "<uuid>", "username": "..."}` |
| `/api/v1/auth/register` | `POST` | `{"username", "email", "password", "language"}` | Registration result                      |
| `/api/v1/auth/verify`   | `POST` | `{"token": "<verification-token>"}`             | Activation result                        |
| `/api/v1/auth/forgot`   | `POST` | `{"email": "..."}`                              | `{"message": "If this account exists..."}`|
| `/api/v1/auth/reset`    | `POST` | `{"token": "...", "newPassword": "..."}`        | `{"message": "Password has been reset..."}`|

> [!IMPORTANT]
> **Zero-Enumeration Protection:** `/api/v1/auth/forgot` always returns `200 OK` with a generic message regardless of whether the email exists. `/api/v1/auth/reset` returns `400 Bad Request` with an `{"error": "..."}` payload on invalid/expired tokens. Both endpoints are public (`permitAll`) — no authentication required.

> [!NOTE]
> **Security annotations:** Both `/forgot` and `/reset` are whitelisted in `SecurityConfig.java` with `.permitAll()`. No `@PreAuthorize` — these are unauthenticated recovery flows.

#### Streaming, Video & Model Catalog

| Endpoint                               | Method | Content-Type        | Description              |
| :------------------------------------- | :----- | :------------------ | :----------------------- |
| `/api/v1/chat/stream/{conversationId}` | `GET`  | `text/event-stream` | SSE token streaming      |
| `/api/v1/ai/video`                     | `POST` | `application/json`  | Text-to-video generation |
| `/api/v1/models/catalog`               | `GET`  | `application/json`  | Get list of all dynamic active models |

#### Model Catalog Administration (Admin only)

| Endpoint                     | Method   | Payload                                      | Description |
| :--------------------------- | :------- | :------------------------------------------- | :---------- |
| `/api/v1/admin/models`       | `POST`   | `{"name": "...", "label": "...", ...}`       | Add new model to database catalog |
| `/api/v1/admin/models/{id}`  | `PUT`    | `{"name": "...", "label": "...", ...}`       | Update existing catalog model |
| `/api/v1/admin/models/{id}`  | `DELETE` | —                                            | Remove model from database catalog |

#### Model Context Protocol (MCP)

| Endpoint | Method | Content-Type | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/mcp/tools` | `GET` | `application/json` | List all registered MCP and local tools with their JSON schemas |
| `/api/v1/mcp/tools/{name}/execute` | `POST` | `application/json` | Execute a registered tool by its name with the arguments payload |
| `/api/v1/mcp/servers/user` | `GET` | `application/json` | List user-scoped MCP server registrations |
| `/api/v1/mcp/servers/user` | `POST` | `application/json` | Register a new user MCP server |
| `/api/v1/mcp/servers/user/{id}` | `DELETE` | — | Remove a user MCP server registration |
| `/api/v1/mcp/servers/platform` | `GET` | `application/json` | List platform-wide MCP servers (Admin only) |
| `/api/v1/mcp/servers/platform` | `POST` | `application/json` | Register a platform MCP server (Admin only) |
| `/api/v1/mcp/servers/platform/{id}` | `DELETE` | — | Remove a platform MCP server (Admin only) |

#### Chat Session Management

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/chats` | `GET` | List all chat sessions for the authenticated user |
| `/api/v1/chats` | `POST` | Create a new chat session |
| `/api/v1/chats/{sessionId}` | `PATCH` | Rename an existing chat session |
| `/api/v1/chats/{sessionId}` | `DELETE` | Delete a chat session |

#### User Credentials

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/user/credentials` | `GET` | List stored API credentials for the user |
| `/api/v1/user/credentials` | `POST` | Save or update a user API credential |
| `/api/v1/user/credentials/{providerName}` | `DELETE` | Remove a stored credential by provider |

#### Media Upload

| Endpoint | Method | Content-Type | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/media/upload` | `POST` | `multipart/form-data` | Upload a media file (image, audio, video) for processing |

#### Media Analysis

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/media/search-rag` | `GET` | Search the RAG vector index for matching document fragments |
| `/api/v1/media/analyze-image` | `POST` | Submit an image for multi-modal vision analysis |

#### Speech Generation

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/ai/speech` | `POST` | Text-to-speech synthesis via Piper TTS or OpenAI bridge |

#### Image Generation

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/ai/image` | `POST` | Text-to-image generation via stable-diffusion.cpp |

#### Operation Graph

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/operations/graph` | `GET` | Get the server-driven UI capability graph |

> [!IMPORTANT]
> This endpoint requires authentication (`ROLE_ADMIN` or `ROLE_USER`). It was previously public — hardened per security audit.

#### Bootstrap

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/bootstrap/features` | `GET` | Get initial feature flags for client bootstrap |

> [!IMPORTANT]
> This endpoint requires authentication (`ROLE_ADMIN` or `ROLE_USER`). It was previously public — hardened per security audit.

#### Admin Features (Admin only)

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/admin/features` | `GET` | List all feature flag configurations |
| `/api/v1/admin/features/{featureKey}` | `PUT` | Toggle a feature flag on/off |

#### Admin Jobs (Admin only)

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/admin/jobs/purge` | `POST` | Purge old completed/failed jobs |
| `/api/v1/admin/jobs/active-connections` | `GET` | List active SSE connections |

#### Asynchronous Jobs

| Endpoint              | Method | Payload / Query                           | Response                                   | Description                                                    |
| :-------------------- | :----- | :---------------------------------------- | :----------------------------------------- | :------------------------------------------------------------- |
| `/api/v1/jobs`        | `POST` | `{"featureKey": "...", "payload": {...}}` | `{"jobId": "<uuid>", "status": "PENDING"}` | Submit a job asynchronously (returns HTTP 202)                 |
| `/api/v1/jobs`        | `GET`  | `?page=0&size=10` (default)               | `Page<JobDto>`                             | Get a paginated list of jobs for authenticated user            |
| `/api/v1/jobs/{id}`   | `GET`  | —                                         | `JobDto`                                   | Fetch the single, atomic state of a specific job               |
| `/api/v1/jobs/stream` | `GET`  | —                                         | `text/event-stream`                        | Register a Server-Sent Events stream for real-time job updates |

**Video Request:**

```json
{
  "prompt": "A cinematic shot of cyberpunk streets, neon lighting, heavy rain, 4k",
  "image": "550e8400-e29b-41d4-a716-446655440002",
  "durationSeconds": 4,
  "settings": { "fps": 24, "width": 512, "height": 512 }
}
```

*Note: The `image` property is optional and specifies the asset ID of an uploaded image to animate (e.g., Stable Video Diffusion img2vid).*

**Video Response (RFC 2397 Data URL):**

```json
{
  "format": "mp4",
  "url": "data:video/mp4;base64,AAAAIGZ0eXBtcDQy..."
}
```

### BFF Extensions (Next.js Server-Side)

| Mutation             | Description                                        |
| :------------------- | :------------------------------------------------- |
| `VerifyEmail(token)` | Server-side M2M REST call to `/api/v1/auth/verify` |

---

## 🎬 Media Pre-Processor Contracts

Port/adapter contracts in `orasaka-core/ingest/` for preprocessing media before AI inference:

| Interface                                                                                               | Output Record           | Key Fields                                      |
| :------------------------------------------------------------------------------------------------------ | :---------------------- | :---------------------------------------------- |
| [ImagePreProcessor](../orasaka-core/src/main/java/com/orasaka/core/ingest/image/ImagePreProcessor.java) | `ProcessedImagePayload` | `base64Image`, `width`, `height` (non-negative) |
| [AudioPreProcessor](../orasaka-core/src/main/java/com/orasaka/core/ingest/audio/AudioPreProcessor.java) | `ProcessedAudioPayload` | `transcript` (defaults to empty string)         |
| [VideoPreProcessor](../orasaka-core/src/main/java/com/orasaka/core/ingest/video/VideoPreProcessor.java) | `ProcessedVideoPayload` | Extracted frames + audio data                   |

> [!NOTE]
> All pre-processor implementations must be **package-private** per ADR-019. Only the interfaces and payload records are public.

---

## 🖥️ CLI Client (v2.0.0)

### Commands

| Command                     | Description                     | Protocol                      |
| :-------------------------- | :------------------------------ | :---------------------------- |
| `login [email] [password]`  | Authenticate and cache JWT      | `POST /api/v1/auth/login`     |
| `register [user] [email]`   | Self-service registration       | `POST /api/v1/auth/register`  |
| `verify <token>`            | Email verification              | `POST /api/v1/auth/verify`    |
| `forgot [email]`            | Request password reset          | `POST /api/v1/auth/forgot`    |
| `reset [token] [password]`  | Reset password with token       | `POST /api/v1/auth/reset`     |
| `chat [prompt]`             | Interactive or single-shot chat | SSE streaming                 |
| `chat --gen-image <prompt>` | Generate image from text        | GraphQL `mutation { image }`  |
| `chat --speech <text>`      | Text-to-speech synthesis        | GraphQL `mutation { speech }` |
| `chat --image <filepath>`   | Vision analysis of local image  | Operation Graph               |
| `chat --audio <filepath>`   | Audio analysis of local file    | Operation Graph               |
| `chat --save <path>`        | Save generated media to file    | —                             |
| `video <prompt>`            | Text-to-video generation        | `POST /api/v1/ai/video`       |
| `profile`                   | Display user profile            | GraphQL `query { me }`        |
| `settings get`              | Show current preferences        | GraphQL `query { me }`        |
| `settings set <key> <val>`  | Update single preference        | GraphQL mutation              |
| `graph`                     | Display operation graph         | GraphQL query                 |

### Interactive Meta-Commands

| Command               | Description                             |
| :-------------------- | :-------------------------------------- |
| `/thread new`         | Create and switch to a new conversation |
| `/thread list`        | List all stored threads                 |
| `/thread switch <id>` | Switch active thread (prefix match)     |

### Local Persistence

| File                             | Content                                         |
| :------------------------------- | :---------------------------------------------- |
| `~/.orasaka-cli.json`            | JWT token, username, active thread, thread list |
| `~/.orasaka-threads/<uuid>.json` | Per-thread message history                      |

---

## ⚠️ Exceptions & Error Handling

### Gateway Exceptions

| Exception | HTTP | Trigger |
| :--- | :--- | :--- |
| [`SystemOverloadedException`](../orasaka-gateway/src/main/java/com/orasaka/gateway/exception/SystemOverloadedException.java) | `429` | Message broker queue overloaded or broker outage (Resilience4j fallback) |

### Identity Exceptions (ERR-106 Compliant)

All identity service methods throw explicit unchecked exceptions instead of returning `null` (per ERR-106 Null-Return Prohibition):

| Exception | Trigger |
| :--- | :--- |
| [`UserNotFoundException`](../orasaka-identity/src/main/java/com/orasaka/identity/infrastructure/support/UserNotFoundException.java) | `getUser(userId)` called with non-existent user ID |
| [`BadCredentialsException`](../orasaka-identity/src/main/java/com/orasaka/identity/infrastructure/support/BadCredentialsException.java) | `authenticate()` with invalid email or password |
| [`UserAlreadyExistsException`](../orasaka-identity/src/main/java/com/orasaka/identity/infrastructure/support/UserAlreadyExistsException.java) | `register()` with duplicate email |
| [`InvalidRequestException`](../orasaka-identity/src/main/java/com/orasaka/identity/infrastructure/support/InvalidRequestException.java) | Request DTO validation failure in compact constructor |
| [`ConfigurationException`](../orasaka-identity/src/main/java/com/orasaka/identity/infrastructure/support/ConfigurationException.java) | Missing or invalid system configuration at runtime |
| [`SecurityGuardrailException`](../orasaka-identity/src/main/java/com/orasaka/identity/infrastructure/support/SecurityGuardrailException.java) | Security constraint violation during identity operations |

---

## 📎 Related Documentation

| Document                                     | Description                                         |
| :------------------------------------------- | :-------------------------------------------------- |
| [Architecture Reference](ARCHITECTURE.md)    | System topology, module boundaries, execution flows |
| [Glossary](GLOSSARY.md)                      | Ecosystem terms, patterns, environment variables    |
| [ADR Log](CONTEXT.md)                        | 27 Architectural Decision Records                   |
| [Business Guide](BUSINESS_IMPLEMENTATION.md) | Step-by-step feature implementation blueprint       |
