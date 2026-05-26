# API Reference

> Complete specification of Orasaka's public interfaces, properties, endpoints, and data models.

---

## 1. Core Facade & Properties

### Facade Port (`AiClient`)
Primary entry facade for all AI actions:
- `chat(request)` -> `ChatResponse`: Sync chat execution.
- `stream(request)` -> `Flux<ChatResponse>`: Streaming token responses.
- `image(request)` -> `ImageResponse`: Image synthesis.
- `audio(request)` -> `AudioResponse`: Piper / local TTS output.
- `video(request)` -> `VideoResponse`: Video generation.

### Properties Overrides
- **orasaka.core**: `defaultProvider` (active provider), `rag.enabled` (RAG toggle), `rag.topK` (RAG matches injected), `mcp.endpoints` (external servers).
- **orasaka.tools**: `configs[toolId].cache.enabled`, `configs[toolId].cache.ttlSeconds`, `configs[toolId].rag.enabled`, `configs[toolId].rag.chunkerType` (`PLAIN_TEXT`, `MARKDOWN_CHUNKERS`, `JSON_ARRAY`).

---

## 2. Inbound & Outbound Models

### Context DTO
Immutable session container:
- `userId` (String): ID.
- `conversationId` (String): Active session ID.
- `preferences` (Map): Session parameters.
- `authorities` (Set): Case-insensitive roles.

### RBAC Roles (orasaka-identity)
Java 21 sealed implementation:
- `Role.Admin` -> `"ADMIN"`
- `Role.User` -> `"USER"`
- `Role.Guest` -> `"GUEST"`

---

## 3. Gateway Ingress Endpoints

### GraphQL Ingress (AiController)
- **Queries**:
  - `me`: Authenticated profile.
  - `interceptionSchema(schemaId)`: User feedback JSON-Schema.
  - `operationGraph`: Server-driven UI capability graph (authenticated).
- **Mutations**:
  - `chat(prompt, conversationId?)`: Agent execution.
  - `image(prompt)`: stable-diffusion.cpp wrapper (port `8085`).
  - `speech(prompt)`: Piper TTS wrapper.
  - `updatePreferences(preferences)`: Merges user preferences.
  - `resolveInterception(type, schemaId, responses)`: Submits feedback.
- **Subscriptions**:
  - `chatStream(prompt, conversationId?)`: Word-by-word streaming.

### REST Ingress (AuthController, ChatStreamController, CodeStreamController, ImageController, SpeechController, VideoController, MediaAnalysisController, JobController, McpController, AdminFeatureController, AdminJobController, AdminModelController, AdminPipelineController, ValidationAdminController)
- **Authentication**:
  - `POST /api/v1/auth/login`: Custom credentials.
  - `POST /api/v1/auth/oauth`: OAuth verifier.
  - `POST /api/v1/auth/register`: Signup.
  - `POST /api/v1/auth/forgot` / `POST /api/v1/auth/reset`: Password recovery.
- **Streaming & Media**:
  - `GET /api/v1/chat/stream/{conversationId}`: Reactive token-stream.
  - `POST /api/v1/ai/video`: SVD XT video runner dispatch.
  - `GET /api/v1/models/catalog`: Model cache.
  - `POST /api/v1/media/upload`: Multipart file uploader.
- **Dynamic Catalog (Admin only)**:
  - `POST` / `PUT` / `DELETE` on `/api/v1/admin/models`
- **Asynchronous Jobs**:
  - `POST /api/v1/jobs`: Submits async tasks (returns `202 Accepted`).
  - `GET /api/v1/jobs?page=x&size=y`: User job list.
  - `GET /api/v1/jobs/{id}`: Inspect status.
  - `GET /api/v1/jobs/stream`: SSE job progress updates.

---

## 4. Exceptions & Errors

All identity and core methods return exceptions instead of `null` (`ERR-106`):
- `UserNotFoundException`: Requested user does not exist.
- `BadCredentialsException`: Invalid email/password.
- `UserAlreadyExistsException`: Email already registered.
- `InvalidRequestException`: Compact constructor validation failure.
- `SystemOverloadedException`: HTTP `429` (connection or broker overload).

---

## Related Documentation
- [Architecture Reference](ARCHITECTURE.md)
- [Developer Onboarding Guide](101.md)
- [ADR Indexes](CONTEXT.md)
