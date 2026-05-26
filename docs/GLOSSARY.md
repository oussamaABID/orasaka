# Glossary

> Quick reference for Orasaka ecosystem terms, patterns, themes, and environment variables.

---

## 1. Core Terms & Patterns

- **Bridge Pattern 2.0**: Core model wrappers ensuring Spring AI types do not leak outside `orasaka-core`.
- **Ports & Adapters**: Decouples business logic (`core` ports) from framework details (`gateway`, `tools` adapters).
- **Context-Matrix Pipeline**: 15-stage ordered filter chain enriching prompt context.
- **Multi-Tier Caching**: Caching tool outputs locally in Caffeine (L1) and PostgreSQL (L2).
- **Server-Driven UI (SDUI)**: Client layout capabilities dynamically driven by backend `OperationGraph` states.
- **Mapper Isolation (ERR-107)**: Package-private `*Mapper` files containing static construction mapping.
- **Immutable Time (ERR-108)**: Enforces `date-fns` and ISO-8601 strings, banning native Date mutations.
- **Input Blocking Invariant (ERR-126)**: Disables text fields, submit buttons, and menus during streaming.

---

## 2. Environment Variables

### Infrastructure & DB
- `PORT` (8080): Spring Boot Gateway listen port.
- `SPRING_DATASOURCE_URL` (`jdbc:postgresql://localhost:5432/orasaka_db`): Main PG connection.
- `SPRING_DATASOURCE_USERNAME` (`orasaka_admin`): DB user.
- `SPRING_DATASOURCE_PASSWORD`: DB password.
- `REDIS_URL` (`redis://localhost:6379`): Redis rate-limiting cache.

### Local AI Models (Ollama)
- `DEFAULT_PROVIDER` (`ollama`): Main active AI provider (bound to `orasaka.core.default-provider`).
- `SPRING_AI_OLLAMA_BASE_URL` (`http://localhost:11434`): Local Ollama service.
- `SPRING_AI_OLLAMA_CHAT_MODEL` (`llama3.2:3b`): Default chat model.
- `OLLAMA_EMBEDDING_MODEL` (`nomic-embed-text:latest`): Local embedding model.

### Cloud Fallback & Media Workers
- `LOCALAI_BASE_URL` (`http://localhost:8085`): LocalAI image/TTS service endpoint.
- `LOCALAI_API_KEY` (`not-required`): LocalAI API key (not needed for local usage).
- `LOCALAI_MODEL` (`tts-1`): Default LocalAI model.
- `VIDEO_WORKER_PORT` (`8188`): SVD video worker local port (probe-port, hardcoded in `InfrastructureProber`).
- `IMAGE_WORKER_PORT` (`8085`): stable-diffusion.cpp local port.

### Broker & Queue Configuration
- `SPRING_RABBITMQ_HOST` (`localhost`): RabbitMQ host address.
- `SPRING_RABBITMQ_PORT` (`5672`): RabbitMQ listen port.
- `BROKER_QUEUE_MAX_LENGTH` (`1000`): Max RabbitMQ queue capacity.
- `CB_FAILURE_RATE` (`50`): Circuit breaker failure threshold percentage.

### Frontend BFF (Next.js)
- `NEXTAUTH_SECRET`: BFF session encryption cookie key.
- `NEXTAUTH_URL` (`http://localhost:3000`): UI domain.
- `GATEWAY_URL` (`http://localhost:8080`): Internal Gateway API target.

---

## 3. UI Accents & Themes

UI theme accents are resolved from the database (`theme` category in `orasaka_models`), decoupling display styles from hardcoded configurations.
- **Accents**: `zinc` (default), `rose`, `emerald`, `amber`.
- **Flow**: User settings choice -> `themeAccent` saved in preferences -> Next.js client `useTenant()` context mapper resolves colors via `accentMap`.

---

## Related Documentation
- [Developer Onboarding Guide](101.md)
- [Architecture Reference](ARCHITECTURE.md)
- [API Reference](API_REFERENCE.md)
- [ADR Indexes](CONTEXT.md)
