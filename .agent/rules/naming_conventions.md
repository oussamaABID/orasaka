# ORASAKA RULE: NAMING CONVENTIONS & BOUNDARIES

## §1 — Java Package Structure

### orasaka-core
| Package | Purpose |
|:---|:---|
| `domain.ports.inbound` | Entry facades (`AiClient`) |
| `domain.ports.outbound` | Downstream interfaces (`ChatGeneratorClient`, `MemoryResolver`) |
| `domain.model` | Immutable records: requests, responses, metadata |
| `application.engine` | State-free execution blueprints |
| `application.pipeline` | Interceptor chains, context injectors, utilities |
| `application.processing` | Media ingestion, token estimation, preprocessing |
| `application.service` | Internal facade orchestrators |
| `infrastructure.config` | Properties, auto-configuration |
| `infrastructure.adapter.ai` | Concrete AI generator clients |
| `infrastructure.support` | Helpers, system utilities |

### orasaka-identity
| Package | Purpose |
|:---|:---|
| `domain` | User, Role, Credential models, provider verifiers |
| `application.service` | Auth logic, onboarding (package-private impl) |
| `infrastructure.config` | OAuth2 filters, context bindings |
| `infrastructure.persistence.entity` | JPA entities |
| `infrastructure.persistence.repository` | Spring Data repos |
| `infrastructure.persistence.converter` | Attribute converters |

### orasaka-gateway
| Package | Purpose |
|:---|:---|
| `domain.model` | Protocol-neutral DTOs, auth contracts |
| `application.service` | SSE dispatch, async runners, token managers |
| `infrastructure.config` | MVC, GraphQL, CORS, rate limiting |
| `infrastructure.adapter.rest` | REST/SSE controllers |
| `infrastructure.adapter.graphql` | GraphQL resolvers |
| `infrastructure.adapter.amqp` | RabbitMQ listeners |
| `infrastructure.support` | Security filters, token decoders |

### orasaka-tools
| Package | Purpose |
|:---|:---|
| `domain.ports` | Tool interfaces, registration abstractions |
| `application.service` | Tool execution managers, callback routers |
| `functions.[feature]` | Domain-specific tool callbacks |
| `infrastructure.config` | MCP endpoint properties |
| `infrastructure.persistence.*` | Entities, repos, converters |

### orasaka-persistence-*
| Package | Purpose |
|:---|:---|
| `domain` | Storage interface declarations |
| `application.service` | Transaction management, mapping utilities |
| `infrastructure.config` | Datasource, Redis, Flyway controls |
| `infrastructure.persistence.*` | JPA entities, CRUD repos, converters |

## §2 — Class Naming Rules

### Zero-Prefix [ERR-104]
- **Banned**: `OrasakaEngine`, `OrasakaSidebar`, `orasaka-cli-tool`
- **Correct**: `Engine`, `Sidebar`, `CliTool`
- Use package topology for ownership: `com.orasaka.core.application.engine.Engine`

### Suffixes
| Layer | Suffix Pattern |
|:---|:---|
| Services | `*Service` (interface), `*ServiceImpl` (package-private) |
| Interceptors | `*Interceptor` |
| Resolvers | `*Resolver` |
| Controllers | `*Controller` |
| Mappers | `*Mapper` (package-private `final class`, static methods) |
| Repositories | `*Repository` |
| Configuration | `*Config`, `*Properties` |
| Converters | `*Converter` |

## §3 — Frontend Naming (orasaka-ui)

- Components: `PascalCase` (e.g., `ChatWindow`, `Sidebar`)
- Hooks: `camelCase` with `use` prefix (e.g., `useStreamChat`)
- Constants files: `kebab-case.constants.ts`
- API services: `kebab-case.api.ts`
- Test files: `ComponentName.test.tsx`
- i18n keys: `dot.separated.camelCase` (e.g., `sidebar.chatSessions`)

## §4 — Python Naming (orasaka-video-worker)

- Functions/variables: `snake_case`
- Classes: `PascalCase`
- Constants: `UPPER_SNAKE_CASE`
- Modules: `snake_case.py`
- Type hints required on all public function signatures

## §5 — Terraform Naming

- Resources: `snake_case` (e.g., `aws_ecs_service.backend`)
- Variables: `snake_case` with descriptions and types
- Modules: `kebab-case` directories (e.g., `aws-compute-ecs/`)
- Outputs: descriptive `snake_case` (e.g., `vpc_id`, `ecs_cluster_arn`)

## §6 — PostgreSQL Naming

- Tables: `snake_case` plural (e.g., `chat_sessions`)
- Columns: `snake_case` (e.g., `created_at`)
- Indexes: `idx_{table}_{columns}` (e.g., `idx_chat_sessions_user_id`)
- Migration files: `V{N}__{description}.sql`
- Constraints: `uq_{table}_{columns}`, `fk_{table}_{ref_table}`

## §7 — Backend Constraints

- Raw SQL strings are banned. Use Spring Data JPA Repositories or `@Query`.
- `AttributeConverter` implementations must reside in `*.infrastructure.persistence.converter.*`.
- Database-query duplication banned — write methods return domain records.
- `@Transactional` write methods never invoke internal read calls for same entity.
- Hashing/crypto executes outside `@Transactional` blocks.
- Read-before-write existence checking is banned.
- N+1 queries banned — use `LEFT JOIN FETCH` or Entity Graphs.
- 250-line limit for React `.tsx` files.
- Inline `.map()` JSX loops must be extracted into named sub-components.
