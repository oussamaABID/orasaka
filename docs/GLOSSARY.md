# Glossary

> Quick reference for Orasaka ecosystem terminology, architectural patterns, and environment variables.

---

## Core Concepts

| Term | Definition | Module |
|:---|:---|:---|
| **CORE** | Cognitive Orchestration & Retrieval Engine | `orasaka-core` |
| **BFF** | Backend-for-Frontend gateway layer | `orasaka-gateway` |
| **BFF Router Proxy** | Next.js API Routes (`/api/graphql`, `/api/chat/stream/[conversationId]`) that proxy browser requests, inject tokens, and resolve CORS | `orasaka-ui` |
| **RBAC** | Role-Based Access Control using Java 21 Sealed Interfaces | `orasaka-identity` |

---

## Architecture Patterns

| Term | What it is | Why it matters |
|:---|:---|:---|
| **Bridge Pattern 2.0** | Wraps all Spring AI framework models inside Orasaka-native abstractions | Prevents vendor lock-in; external modules only see `AiClient` |
| **Ports & Adapters** | Core defines interfaces (ports); tools provides implementations (adapters) | Keeps `orasaka-core` 100% stateless and web-agnostic |
| **Context-Matrix Pipeline** | 4-stage ordered interceptor chain processing every request | Enables modular prompt enrichment without engine modification |
| **Passive Multi-Tier Caching** | Configuration-driven cache wrapping (Caffeine → PostgreSQL) | Reduces LLM costs, cuts response latency, enables cross-node sharing |
| **Server-Driven UI (SDUI)** | `OperationGraph` compiled by `GraphEngine` with polymorphic `NodeState` | Backend controls exactly what the frontend renders |

---

## Key Classes & Records

| Term | Class | Role |
|:---|:---|:---|
| **Engine** | `AbstractEngine` | Bridges Spring AI models with high-level agentic logic |
| **Facade** | `AiClient` | Unified developer entry point for all AI interactions |
| **Context** | `Context` record | Immutable request envelope carrying user preferences and security privileges |
| **Authority** | `Authority` record | Single security role name (e.g., `"ROLE_USER"`) used for thread-safe authority checks |
| **Role** | `Role` sealed interface | Type-safe User, Admin, Guest hierarchy in `orasaka-identity` |
| **ToolRegistry** | `ToolRegistry` interface | Registry for mapping local Java methods as LLM-callable tools |
| **KnowledgeService** | `KnowledgeService` | RAG abstraction — orchestrates vector retrieval without direct storage binding |
| **MCP** | Model Context Protocol | Standard for integrating external context and tools |
| **Chunker / Strategies** | `Chunker`, `ChunkingStrategies` | Text-splitting algorithm registry (plain text, markdown, JSON array) |
| **Virtual Thread Executor** | `newVirtualThreadPerTaskExecutor()` | Runs heavy I/O and AI calls with near-zero memory footprint |
| **StringTemplate (.st)** | Externalized template format | Separates prompt instructions from Java source code |
| **Media Pre-Processor Ports** | `AudioPreProcessor`, `ImagePreProcessor`, `VideoPreProcessor` | Port interfaces defining media ingestion contracts |

---

## Infrastructure Terms

| Term | What it is |
|:---|:---|
| **Ops Consolidation** | Infrastructure directory (`/ops`) isolating Docker, Postgres schemas, scripts, and HTTP tests from application code |
| **Verification Token** | SHA-256 hashed registration token in `orasaka_verification_tokens` for secure double opt-in |
| **Interception Registry** | `orasaka_user_interceptions` table storing active session interruption events (onboarding, reviews) |
| **Video Engine** | Local sovereign text-to-video engine powered by quantized LTX-Video on port `8086` |
| **Background RAG Ingestion** | `BackgroundScheduler` — async pipeline that indexes RAG sources into the vector database |

---

## Environment Variables

### Gateway & Server

| Variable | Default | Description |
|:---|:---|:---|
| `PORT` | `8080` | Spring Boot Gateway listen port |
| `ORASAKA_GATEWAY_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | CORS allowed origins (comma-separated) |

### Database

| Variable | Default | Security |
|:---|:---|:---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/orasaka_db` | Medium |
| `SPRING_DATASOURCE_USERNAME` | `orasaka_admin` | Medium |
| `SPRING_DATASOURCE_PASSWORD` | `orasaka_secure_pass` | 🔴 Secret |
| `SPRING_SQL_INIT_MODE` | `never` | Low |
| `SPRING_GRAPHQL_GRAPHIQL_ENABLED` | `true` | Low |

### AI Models (Ollama)

| Variable | Default | Description |
|:---|:---|:---|
| `ORASAKA_DEFAULT_PROVIDER` | `ollama` | Global active AI provider |
| `SPRING_AI_OLLAMA_BASE_URL` | `http://localhost:11434` | Spring AI auto-configured Ollama URL |
| `SPRING_AI_OLLAMA_CHAT_MODEL` | `phi3:mini` | Default chat model |
| `ORASAKA_OLLAMA_BASE_URL` | `http://localhost:11434` | Orasaka Ollama endpoint |
| `ORASAKA_OLLAMA_MODEL` | `llama3:8b` | Running local model identifier |
| `ORASAKA_OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text:latest` | Embedding model |
| `ORASAKA_OLLAMA_TEMPERATURE` | `0.7` | Default inference temperature |

### AI Models (OpenAI)

| Variable | Default | Security |
|:---|:---|:---|
| `OPENAI_API_KEY` | — | 🔴 Secret |
| `ORASAKA_OPENAI_BASE_URL` | `https://api.openai.com/v1` | Low |
| `ORASAKA_OPENAI_MODEL` | `gpt-4o` | Low |
| `ORASAKA_OPENAI_TEMPERATURE` | `0.7` | Low |

### Pipeline Orchestration

| Variable | Default | Description |
|:---|:---|:---|
| `ORASAKA_ORCHESTRATION_ENABLED` | `true` | Context enrichment pipeline |
| `ORASAKA_USER_CONTEXT_ENABLED` | `true` | User preferences injection |
| `ORASAKA_SYSTEM_CONTEXT_ENABLED` | `true` | System metrics injection |
| `ORASAKA_REFINER_ENABLED` | `false` | Query refinement against history |
| `ORASAKA_REFINER_PROVIDER` | `openai` | Refiner model provider |
| `ORASAKA_REFINER_MODEL` | `gpt-4-turbo` | Refiner model variant |
| `ORASAKA_REFINER_TEMPERATURE` | `0.2` | Refiner temperature |
| `ORASAKA_ROUTER_PROVIDER` | `ollama` | Router model provider |
| `ORASAKA_ROUTER_MODEL` | `llama3.1:8b` | Router intent model |
| `ORASAKA_ROUTER_TEMPERATURE` | `0.0` | Router temperature (deterministic) |

### Identity & Security

| Variable | Default | Security |
|:---|:---|:---|
| `ORASAKA_EMAIL_VERIFICATION_ENABLED` | `true` | Low |
| `ORASAKA_INTERCEPTIONS_ENABLED` | `true` | Low |
| `ORASAKA_RATE_LIMIT_ENABLED` | `false` | Low |
| `ORASAKA_REDIS_URL` | `redis://localhost:6379` | Medium |
| `ORASAKA_RATE_LIMIT_DEFAULT_TIER` | `free` | Low |
| `ORASAKA_SECURITY_DEV_BYPASS_ENABLED` | `true` | Medium |
| `ORASAKA_SECURITY_DEV_BYPASS_ID` | `user-mock` | Medium |

### Media Generation

| Variable | Default | Description |
|:---|:---|:---|
| `ORASAKA_VIDEO_BASE_URL` | `http://localhost:8086` | LTX-Video runner endpoint |
| `ORASAKA_VIDEO_MODEL` | `ltx-video` | Video model identifier |
| `ORASAKA_LOCALAI_BASE_URL` | `http://localhost:8085` | stable-diffusion.cpp endpoint |
| `ORASAKA_LOCALAI_API_KEY` | `not-required` | Not required for local mode |
| `SPRING_AI_OLLAMA_IMAGE_MODEL` | `stable-diffusion-xe` | Default image model |

### Frontend (Next.js)

| Variable | Default | Security |
|:---|:---|:---|
| `NEXTAUTH_SECRET` | — | 🔴 Secret |
| `NEXTAUTH_URL` | `http://localhost:3000` | Low |
| `GATEWAY_URL` | `http://localhost:8080` | Low |
| `GITHUB_ID` / `GITHUB_SECRET` | — | 🔴 Secret |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | — | 🔴 Secret |

### Logging

| Variable | Default | Description |
|:---|:---|:---|
| `LOGGING_LEVEL_ROOT` | `INFO` | Global root log level |
| `LOGGING_LEVEL_ORASAKA` | `DEBUG` | Orasaka package log level |
| `LOGGING_LEVEL_SECURITY` | `DEBUG` | Spring Security log level |

---

## 📎 Related Documentation

| Document | Description |
|:---|:---|
| [Architecture Reference](ARCHITECTURE.md) | System topology, module boundaries, execution flows |
| [API Reference](API_REFERENCE.md) | Public types, facades, endpoints, data models |
| [ADR Log](CONTEXT.md) | 22 Architectural Decision Records |
| [Business Guide](BUSINESS_IMPLEMENTATION.md) | Step-by-step feature implementation blueprint |
