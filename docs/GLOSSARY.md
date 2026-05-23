# Orasaka Glossary

| Term | Definition | Role in Ecosystem |
| :--- | :--- | :--- |
| **CORE** | Cognitive Orchestration & Retrieval Engine (`orasaka-core`). | The AI Orchestration brain of the Orasaka ecosystem. |
| **BFF** | Backend-for-Frontend (`orasaka-gateway`). | The GraphQL gateway layer mediating between Core and UI. |
| **BFF Router Proxy** | Next.js API Routes (`/api/graphql` and `/api/chat/stream/[conversationId]`). | Proxies browser requests, handles token injection, and resolves CORS on the server side. |
| **RBAC** | Role-Based Access Control (`orasaka-identity`). | Identity management using Java 21 Sealed Interfaces. |
| **Engine** | The `AbstractOrasakaEngine` and its implementations. | Bridges Spring AI models with high-level agentic logic. |
| **Facade** | The `OrasakaAiClient`. | Unified developer entry point for all AI interactions. |
| **Bridge** | Architectural pattern (Bridge Pattern 2.0). | Decouples Orasaka from external AI frameworks. |
| **ToolRegistry** | Registry for local Java methods. | Enables LLMs to invoke native Java logic as "tools". |
| **KnowledgeService** | RAG abstraction for Orasaka. | Orchestrates vector retrieval without direct storage binding. |
| **MCP** | Model Context Protocol. | Standard for integrating external context and tools. |
| **Tools Module** | The `orasaka-tools` module. | Holds concrete implementations of MCP orchestrators and tool registries. |
| **OrasakaContext** | Immutable request-level contextual envelope (`OrasakaContext`). | Transfers thread-safe user preferences and session authorities to the Core. |
| **Virtual Thread Executor** | dedicated virtual-thread executor (`newVirtualThreadPerTaskExecutor()`). | Runs heavy non-blocking I/O operations and AI calls with near-zero memory footprint. |
| **Role** | Sealed interface hierarchical roles. | Enforces type-safe User, Admin, and Guest roles with dynamic authority resolution. |
| **Passive Multi-Tier Caching** | Configuration-driven cache wrapping. | Intercepts tool execution to resolve cached results via fast local memory (Caffeine) or cross-node persistent storage (PostgreSQL). |
| **Background RAG Ingestion** | Asynchronous scheduler pipeline. | Automatically scans and indexes RAG source records into the vector database daily on a virtual-thread schedule. |
| **OrasakaChunker / Strategies** | Text-splitting algorithm registry. | Slices raw content strings into clean vector documents via plain text paragraph, markdown headers, or JSON array chunkers. |
| **Ops Consolidation** | Core infrastructure directory isolation (`/ops`). | Separates docker layouts, postgres schemas, scripts, and HTTP tests from application code boundaries. |
| **Verification Token** | Hash-based registration validation (`orasaka_verification_tokens`). | Enforces secure, passive double opt-in account enablement. |
| **Interception Registry** | Contextual interception mapping (`orasaka_user_interceptions`). | Stores active session interruption events (onboarding, reviews) resolved dynamically during login context queries. |

## Environment Variables

| Variable | Description | Default Value | Security Level |
| :--- | :--- | :--- | :--- |
| **PORT** | Port on which the Spring Boot Gateway server listens. | `8080` | Low |
| **ORASAKA_GATEWAY_CORS_ALLOWED_ORIGINS** | Comma-separated list of allowed origins for Gateway CORS configuration. | `http://localhost:3000` | Medium |
| **SPRING_DATASOURCE_URL** | JDBC database URL connecting Gateway to PostgreSQL database. | `jdbc:postgresql://localhost:5432/orasaka_db` | Medium |
| **SPRING_DATASOURCE_USERNAME** | Username for PostgreSQL database login credentials. | `orasaka_admin` | Medium |
| **SPRING_DATASOURCE_PASSWORD** | Password for PostgreSQL database login credentials. | `orasaka_secure_pass` | High (Secret) |
| **SPRING_SQL_INIT_MODE** | DB initialization mode control. | `never` | Low |
| **SPRING_GRAPHQL_GRAPHIQL_ENABLED** | Enables GraphiQL query console playground. | `true` | Low |
| **ORASAKA_DEFAULT_PROVIDER** | Default AI model provider (e.g. `ollama`, `openai`). | `ollama` | Low |
| **ORASAKA_OLLAMA_BASE_URL** | Target base endpoint for running local Ollama model instance. | `http://localhost:11434` | Low |
| **ORASAKA_OLLAMA_MODEL** | Running local Ollama model identifier. | `llama3:8b` | Low |
| **ORASAKA_OLLAMA_EMBEDDING_MODEL** | Embedding model identifier used inside local Ollama instance. | `all-minilm` | Low |
| **ORASAKA_OLLAMA_TEMPERATURE** | Default temperature for Ollama model inference. | `0.7` | Low |
| **OPENAI_API_KEY** | Credentials token for accessing upstream OpenAI models. | `your_openai_api_key_placeholder` | High (Secret) |
| **ORASAKA_OPENAI_BASE_URL** | Base URL for OpenAI API (or custom proxies). | `https://api.openai.com/v1` | Low |
| **ORASAKA_OPENAI_MODEL** | OpenAI model identifier used for core queries. | `gpt-4o` | Low |
| **ORASAKA_OPENAI_TEMPERATURE** | Temperature parameter for OpenAI model inference. | `0.7` | Low |
| **ORASAKA_ORCHESTRATION_ENABLED** | Enables Context Enrichment & Prompt Refinement pipeline. | `true` | Low |
| **ORASAKA_USER_CONTEXT_ENABLED** | Enriches prompts with user-tenant preferences/RBAC attributes. | `true` | Low |
| **ORASAKA_SYSTEM_CONTEXT_ENABLED** | Enriches prompts with active system metrics and environment parameters. | `true` | Low |
| **ORASAKA_REFINER_ENABLED** | Resolves fuzzy queries against conversation history. | `true` | Low |
| **ORASAKA_REFINER_PROVIDER** | Model provider targeting refiner execution (e.g., `openai`). | `openai` | Low |
| **ORASAKA_REFINER_MODEL** | Specific model variant executing prompt refiner. | `gpt-4-turbo` | Low |
| **ORASAKA_REFINER_TEMPERATURE** | Temperature parameter for Refiner model inference. | `0.2` | Low |
| **ORASAKA_ROUTER_PROVIDER** | Model provider evaluating user intents (e.g., `ollama`). | `ollama` | Low |
| **ORASAKA_ROUTER_MODEL** | Model executing query router intent analysis. | `llama3` | Low |
| **ORASAKA_ROUTER_TEMPERATURE** | Temperature parameter for Router model inference. | `0.0` | Low |
| **ORASAKA_EMAIL_VERIFICATION_ENABLED** | Requires email confirmation code validation before enabling user account logins. | `true` | Low |
| **ORASAKA_INTERCEPTIONS_ENABLED** | Triggers validation flow intercepts (onboarding, reviews) on session start. | `true` | Low |
| **ORASAKA_RATE_LIMIT_ENABLED** | Enforces rate limiting tier checks on active sessions. | `false` | Low |
| **ORASAKA_REDIS_URL** | Target connection URL for distributed rate limiting memory. | `redis://localhost:6379` | Medium |
| **ORASAKA_RATE_LIMIT_DEFAULT_TIER** | Default fallback tier assigned to newly registered users. | `free` | Low |
| **LOGGING_LEVEL_ROOT** | Global root logger stdout filter level. | `INFO` | Low |
| **LOGGING_LEVEL_ORASAKA** | Specific logging filter level targeting com.orasaka packages. | `DEBUG` | Low |
| **LOGGING_LEVEL_SECURITY** | Logging level for Spring Security packages. | `DEBUG` | Low |
| **NEXTAUTH_SECRET** | NextAuth signature key for securing server-side session JWTs. | `a_very_secure_secret_key_for_testing` | High (Secret) |
| **NEXTAUTH_URL** | Base callback canonical URL for the UI application server. | `http://localhost:3000` | Low |
| **GATEWAY_URL** | Target BFF address connecting Next.js API routes with spring-gateway. | `http://localhost:8080` | Low |
| **GITHUB_ID** / **GITHUB_SECRET** | Third-party GitHub OAuth credentials. | *(Empty)* | High (Secret) |
| **GOOGLE_CLIENT_ID** / **GOOGLE_CLIENT_SECRET** | Third-party Google OAuth credentials. | *(Empty)* | High (Secret) |
