# Glossary

> Quick reference for Orasaka ecosystem terminology, architectural patterns, and environment variables.

---

## Core Concepts

| Term                 | Definition                                                                                                                            | Module                |
| :------------------- | :------------------------------------------------------------------------------------------------------------------------------------ | :-------------------- |
| **CORE**             | Cognitive Orchestration & Retrieval Engine                                                                                            | `orasaka-core`        |
| **BFF**              | Backend-for-Frontend gateway layer                                                                                                    | `orasaka-gateway`     |
| **BFF Router Proxy** | Next.js API Routes (`/api/graphql`, `/api/chat/stream/[conversationId]`) that proxy browser requests, inject tokens, and resolve CORS | `orasaka-ui`          |
| **RBAC**             | Role-Based Access Control using Java 21 Sealed Interfaces                                                                             | `orasaka-identity`    |
| **PERSISTENCE**      | Decoupled Persistence & Infrastructure State Management                                                                               | `orasaka-persistence-app` |
| **PERSISTENCE-IDENTITY** | Decoupled Identity Persistence & DB Schema Contract                                                                              | `orasaka-persistence-identity` |

---

## Architecture Patterns

| Term                             | What it is                                                                                                         | Why it matters                                                                       |
| :------------------------------- | :----------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------- |
| **Bridge Pattern 2.0**           | Wraps all Spring AI framework models inside Orasaka-native abstractions                                            | Prevents vendor lock-in; external modules only see `AiClient`                        |
| **Ports & Adapters**             | Core defines interfaces (ports); tools provides implementations (adapters)                                         | Keeps `orasaka-core` 100% stateless and web-agnostic                                 |
| **Context-Matrix Pipeline**      | 4-stage ordered interceptor chain processing every request                                                         | Enables modular prompt enrichment without engine modification                        |
| **Passive Multi-Tier Caching**   | Configuration-driven cache wrapping (Caffeine → PostgreSQL)                                                        | Reduces LLM costs, cuts response latency, enables cross-node sharing                 |
| **Server-Driven UI (SDUI)**      | `OperationGraph` compiled by `GraphEngine` with polymorphic `NodeState`                                            | Backend controls exactly what the frontend renders                                   |
| **Resilience & Circuit Breaker** | Resilience4j circuit breakers wrapping remote model endpoints and brokers                                          | Detects failures, prevents system cascades, and returns graceful fallback behaviors  |
| **Mapper Isolation (ERR-107)**   | Package-private static `*Mapper` utility classes isolate entity construction boilerplate from services/controllers | Keeps business methods as pure orchestration; blocks setter-chain pollution          |
| **Immutable Time (ERR-108)**     | Functional, immutable time formatting and operations using `date-fns` and raw ISO-8601 UTC strings                 | Prevents side-effects of native JS `Date` mutations and ensures state predictability |

---

## Key Classes & Records

| Term                          | Class                                                         | Role                                                                                                                 |
| :---------------------------- | :------------------------------------------------------------ | :------------------------------------------------------------------------------------------------------------------- |
| **Engine**                    | `AbstractEngine`                                              | Bridges Spring AI models with high-level agentic logic                                                               |
| **Facade**                    | `AiClient`                                                    | Unified developer entry point for all AI interactions                                                                |
| **Context**                   | `com.orasaka.core.domain.model.Context`                       | Immutable request envelope carrying user preferences and security privileges                                         |
| **Authority**                 | `com.orasaka.core.domain.model.Authority`                     | Single security role name (e.g., `"ROLE_USER"`) used for thread-safe authority checks                                |
| **Role**                      | `Role` sealed interface                                       | Type-safe User, Admin, Guest hierarchy in `orasaka-identity`                                                         |
| **ToolRegistry**              | `ToolRegistry` interface                                      | Registry for mapping local Java methods as LLM-callable tools                                                        |
| **KnowledgeService**          | `KnowledgeService`                                            | RAG abstraction — orchestrates vector retrieval without direct storage binding                                       |
| **MCP**                       | Model Context Protocol                                        | Standard for integrating external context and tools                                                                  |
| **Chunker / Strategies**      | `Chunker`, `ChunkingStrategies`                               | Text-splitting algorithm registry (plain text, markdown, JSON array)                                                 |
| **Virtual Thread Executor**   | `newVirtualThreadPerTaskExecutor()` wrapped by `DelegatingSecurityContextExecutorService` | Runs heavy I/O and AI calls with near-zero memory footprint, propagating Spring Security context automatically to sub-tasks |
| **StringTemplate (.st)**      | Externalized template format                                  | Separates prompt instructions from Java source code                                                                  |
| **Media Pre-Processor Ports** | `AudioPreProcessor`, `ImagePreProcessor`, `VideoPreProcessor` | Port interfaces defining media ingestion contracts                                                                   |
| **ExtractedProfile**          | `ExtractedProfile` record                                     | Canonical bridge between external identity systems and internal user provisioning                                    |
| **JobPersistenceProvider**    | `JobPersistenceProvider`                                      | Public contract/implementation for job lifecycle state and persistence                                               |
| **RabbitMQConfig**            | `RabbitMQConfig`                                              | Configuration class declaring RabbitMQ queues, exchanges, and JSON converters                                        |
| **JobMessage**                | `JobMessage` record                                           | Enqueued payload containing async job data dispatched to workers                                                     |
| **SystemOverloadedException** | `SystemOverloadedException`                                   | Exception thrown when queue limits are reached or broker is down                                                     |
| **UserMapper**                | `UserMapper` (package-private)                                | Static mapper isolating `UserEntity` construction from `IdentityServiceImpl` and `IdentityReconciliationServiceImpl` |
| **VerificationTokenMapper**   | `VerificationTokenMapper` (package-private)                   | Static mapper isolating `VerificationTokenEntity` construction from registration flows                               |
| **UserNotFoundException**     | `UserNotFoundException` (unchecked)                           | Thrown by `getUser()` when user ID does not resolve (ERR-106 Null-Return Prohibition)                                |
| **BadCredentialsException**   | `BadCredentialsException` (unchecked)                         | Thrown by `authenticate()` on invalid email/password (ERR-106 Null-Return Prohibition)                               |
| **ConfigurationException**    | `ConfigurationException` (unchecked)                          | Thrown when required system configuration is missing or invalid at runtime                                            |
| **UserAlreadyExistsException**| `UserAlreadyExistsException` (unchecked)                      | Thrown by `register()` when email is already in use                                                                   |
| **InvalidRequestException**   | `InvalidRequestException` (unchecked)                         | Thrown by request DTO compact constructors on null/blank required fields (ERR-106)                                    |
| **SecurityGuardrailException**| `SecurityGuardrailException` (unchecked)                      | Thrown when a security constraint is violated during identity operations                                              |
| **PasswordRecoveryService**   | `PasswordRecoveryService` interface                           | Inbound port defining password reset lifecycle: `requestPasswordReset(email)` + `resetPassword(token, newPassword)` |
| **PasswordResetToken**        | `PasswordResetToken` record                                   | Domain model holding email, SHA-256 token hash, and expiration instant for password recovery                         |

---

## Infrastructure Terms

| Term                         | What it is                                                                                                          |
| :--------------------------- | :------------------------------------------------------------------------------------------------------------------ |
| **Ops Consolidation**        | Infrastructure directory (`/ops`) isolating Docker, Postgres schemas, scripts, and HTTP tests from application code |
| **Verification Token**       | SHA-256 hashed registration token in `orasaka_verification_tokens` for secure double opt-in                         |
| **Interception Registry**    | `orasaka_user_interceptions` table storing active session interruption events (onboarding, reviews)                 |
| **Video Engine**             | Local sovereign text-to-video engine powered by Stable Video Diffusion (SVD) XT via the standalone Python worker on port `8188`                                  |
| **Background RAG Ingestion** | `BackgroundScheduler` — async pipeline that indexes RAG sources into the vector database                            |
| **Password Reset Token**     | SHA-256 hashed recovery token in `orasaka_password_resets` with 15-minute expiry and single-use deletion policy     |

---

## Environment Variables

### Gateway & Server

| Variable                               | Default                 | Description                            |
| :------------------------------------- | :---------------------- | :------------------------------------- |
| `PORT`                                 | `8080`                  | Spring Boot Gateway listen port        |
| `ORASAKA_GATEWAY_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | CORS allowed origins (comma-separated) |

### Database

| Variable                          | Default                                       | Security  |
| :-------------------------------- | :-------------------------------------------- | :-------- |
| `SPRING_DATASOURCE_URL`           | `jdbc:postgresql://localhost:5432/orasaka_db` | Medium    |
| `SPRING_DATASOURCE_USERNAME`      | `orasaka_admin`                               | Medium    |
| `SPRING_DATASOURCE_PASSWORD`      | _(set in .env)_                               | 🔴 Secret |
| `SPRING_SQL_INIT_MODE`            | `never`                                       | Low       |
| `SPRING_GRAPHQL_GRAPHIQL_ENABLED` | `true`                                        | Low       |

### AI Models (Ollama)

| Variable                         | Default                   | Description                          |
| :------------------------------- | :------------------------ | :----------------------------------- |
| `ORASAKA_DEFAULT_PROVIDER`       | `ollama`                  | Global active AI provider            |
| `SPRING_AI_OLLAMA_BASE_URL`      | `http://localhost:11434`  | Spring AI auto-configured Ollama URL |
| `SPRING_AI_OLLAMA_CHAT_MODEL`    | `phi3:mini`               | Default chat model                   |
| `ORASAKA_OLLAMA_BASE_URL`        | `http://localhost:11434`  | Orasaka Ollama endpoint              |
| `ORASAKA_OLLAMA_MODEL`           | `llama3:8b`               | Running local model identifier       |
| `ORASAKA_OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text:latest` | Embedding model                      |
| `ORASAKA_OLLAMA_TEMPERATURE`     | `0.7`                     | Default inference temperature        |
| `OLLAMA_NUM_PARALLEL`            | `1`                       | Ollama parallel request count        |
| `OLLAMA_KEEP_ALIVE`             | `24h`                     | Ollama model keep-alive duration     |

### Upload & Storage

| Variable                          | Default                | Description                              |
| :-------------------------------- | :--------------------- | :--------------------------------------- |
| `ORASAKA_MEDIA_UPLOAD_DIR`        | `var/orasaka-uploads`  | Base directory for uploaded media files   |
| `ORASAKA_UPLOADS_HANDLER_PATH`    | `/uploads/**`          | Static resource handler URL path pattern |
| `ORASAKA_UPLOADS_CACHE_PERIOD`    | `0`                    | Upload file cache period (seconds)       |
| `ORASAKA_ASYNC_TIMEOUT_MS`        | `300000`               | Async request timeout in milliseconds    |

### Docker Container Aliases

| Variable              | Default            | Description                |
| :-------------------- | :----------------- | :------------------------- |
| `POSTGRES_CONTAINER`  | `orasaka-postgres` | PostgreSQL container name  |
| `REDIS_CONTAINER`     | `orasaka-redis`    | Redis container name       |
| `RABBITMQ_CONTAINER`  | `orasaka-rabbitmq` | RabbitMQ container name    |

### AI Models (OpenAI)

| Variable                     | Default                     | Security  |
| :--------------------------- | :-------------------------- | :-------- |
| `OPENAI_API_KEY`             | —                           | 🔴 Secret |
| `ORASAKA_OPENAI_BASE_URL`    | `https://api.openai.com/v1` | Low       |
| `ORASAKA_OPENAI_MODEL`       | `gpt-4o`                    | Low       |
| `ORASAKA_OPENAI_TEMPERATURE` | `0.7`                       | Low       |

### Pipeline Orchestration

| Variable                         | Default       | Description                        |
| :------------------------------- | :------------ | :--------------------------------- |
| `ORASAKA_ORCHESTRATION_ENABLED`  | `true`        | Context enrichment pipeline        |
| `ORASAKA_USER_CONTEXT_ENABLED`   | `true`        | User preferences injection         |
| `ORASAKA_SYSTEM_CONTEXT_ENABLED` | `true`        | System metrics injection           |
| `ORASAKA_REFINER_ENABLED`        | `false`       | Query refinement against history   |
| `ORASAKA_REFINER_PROVIDER`       | `openai`      | Refiner model provider             |
| `ORASAKA_REFINER_MODEL`          | `gpt-4-turbo` | Refiner model variant              |
| `ORASAKA_REFINER_TEMPERATURE`    | `0.2`         | Refiner temperature                |
| `ORASAKA_ROUTER_PROVIDER`        | `ollama`      | Router model provider              |
| `ORASAKA_ROUTER_MODEL`           | `llama3.1:8b` | Router intent model                |
| `ORASAKA_ROUTER_TEMPERATURE`     | `0.0`         | Router temperature (deterministic) |

### Identity & Security

| Variable                              | Default                  | Security |
| :------------------------------------ | :----------------------- | :------- |
| `ORASAKA_EMAIL_VERIFICATION_ENABLED`  | `true`                   | Low      |
| `ORASAKA_INTERCEPTIONS_ENABLED`       | `true`                   | Low      |
| `ORASAKA_RATE_LIMIT_ENABLED`          | `false`                  | Low      |
| `ORASAKA_REDIS_URL`                   | `redis://localhost:6379` | Medium   |
| `ORASAKA_RATE_LIMIT_DEFAULT_TIER`     | `free`                   | Low      |
| `ORASAKA_SECURITY_DEV_BYPASS_ENABLED` | `true`                   | Medium   |
| `ORASAKA_SECURITY_DEV_BYPASS_ID`      | `user-mock`              | Medium   |

### Media Generation

| Variable                       | Default                 | Description                   |
| :----------------------------- | :---------------------- | :---------------------------- |
| `VIDEO_WORKER_PORT`            | `8188`                  | SVD video worker local port   |
| `IMAGE_WORKER_PORT`            | `8085`                  | Local-AI image worker local port |
| `ORASAKA_VIDEO_WORKER_URL`     | `https://api.orasaka-gpu-cloud.internal` | SVD XT worker GPU service endpoint |
| `ORASAKA_VIDEO_MODEL`          | `svd`                   | Video model identifier        |
| `ORASAKA_LOCALAI_BASE_URL`     | `http://localhost:8085` | stable-diffusion.cpp endpoint |
| `ORASAKA_LOCALAI_API_KEY`      | `not-required`          | Not required for local mode   |
| `SPRING_AI_OLLAMA_IMAGE_MODEL` | `stable-diffusion-xe`   | Default image model           |

### Broker & Resilience

| Variable                          | Default         | Description                                                      |
| :-------------------------------- | :-------------- | :--------------------------------------------------------------- |
| `SPRING_RABBITMQ_HOST`            | `localhost`     | RabbitMQ broker address                                          |
| `SPRING_RABBITMQ_PORT`            | `5672`          | RabbitMQ broker port                                             |
| `SPRING_RABBITMQ_USERNAME`        | `guest`         | RabbitMQ broker username                                         |
| `SPRING_RABBITMQ_PASSWORD`        | `guest`         | RabbitMQ broker password                                         |
| `ORASAKA_BROKER_QUEUE_MAX_LENGTH` | `1000`          | Maximum capacity of the RabbitMQ jobs queue                      |
| `ORASAKA_BROKER_QUEUE_OVERFLOW`   | `rejectPublish` | Policy for handling queue saturation (rejects publish)           |
| `ORASAKA_CB_FAILURE_RATE`         | `50`            | Circuit breaker failure rate percentage threshold (Resilience4j) |
| `ORASAKA_CB_MIN_CALLS`            | `5`             | Minimum number of calls before evaluating failure rate           |
| `ORASAKA_CB_WAIT_TIME`            | `30s`           | Circuit Breaker open wait duration before checking again         |

### Frontend (Next.js)

| Variable                                    | Default                 | Security  |
| :------------------------------------------ | :---------------------- | :-------- |
| `NEXTAUTH_SECRET`                           | —                       | 🔴 Secret |
| `NEXTAUTH_URL`                              | `http://localhost:3000` | Low       |
| `GATEWAY_URL`                               | `http://localhost:8080` | Low       |
| `GITHUB_ID` / `GITHUB_SECRET`               | —                       | 🔴 Secret |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | —                       | 🔴 Secret |

### Logging

| Variable                 | Default | Description               |
| :----------------------- | :------ | :------------------------ |
| `LOGGING_LEVEL_ROOT`     | `INFO`  | Global root log level     |
| `LOGGING_LEVEL_ORASAKA`  | `DEBUG` | Orasaka package log level |
| `LOGGING_LEVEL_SECURITY` | `DEBUG` | Spring Security log level |

---

## UI Settings & Theming

UI theme accents are managed dynamically in the database under the `theme` category (table: `orasaka_models`), decoupling visual preferences from hardcoded client-side options.

### Available Themes

| Theme Key | Display Label | Default |
| :--- | :--- | :--- |
| `zinc` | Zinc Accent | ✅ |
| `rose` | Rose Accent | — |
| `emerald` | Emerald Accent | — |
| `amber` | Amber Accent | — |
| `indigo` | Indigo Accent | — |
| `violet` | Violet Accent | — |

### Settings Preferences Flow

1. User loads `/settings` → UI calls `GET /api/v1/models/catalog?category=theme`.
2. Dropdown populated from the database-driven theme list.
3. User saves → backend stores `themeAccent` key in user preferences.
4. Next.js client applies the accent via `useTenant()` context mapper → Tailwind dynamic color tokens.

---

## 📎 Related Documentation

| Document                                     | Description                                         |
| :------------------------------------------- | :-------------------------------------------------- |
| [Architecture Reference](ARCHITECTURE.md)    | System topology, module boundaries, execution flows |
| [API Reference](API_REFERENCE.md)            | Public types, facades, endpoints, data models       |
| [ADR Log](CONTEXT.md)                        | 29 Architectural Decision Records                   |
| [Business Guide](BUSINESS_IMPLEMENTATION.md) | Step-by-step feature implementation blueprint       |
