# Rule: Naming Conventions & Boundaries

## §1 Java Package Map
- **orasaka-core**: `domain.ports.[inbound|outbound]`, `domain.model`, `application.[engine|pipeline|processing|service]`, `infrastructure.[config|adapter.ai|support]`
- **orasaka-identity**: `domain`, `application.service`, `infrastructure.[config|persistence.entity|persistence.repository|persistence.converter]`
- **orasaka-gateway**: `domain.model`, `application.service`, `infrastructure.[config|adapter.rest|adapter.graphql|adapter.amqp|support]`
- **orasaka-tools**: `domain.ports`, `application.service`, `functions.[feature]`, `infrastructure.[config|persistence.*]`
- **orasaka-persistence-***: `domain`, `application.service`, `infrastructure.[config|persistence.*]`

## §2 Class Naming
- **Zero-Prefix [ERR-104]**: No `Orasaka` prefix (e.g. use `Engine`, not `OrasakaEngine`).
- **Suffixes**: `*Service` (interface), `*ServiceImpl` (package-private), `*Interceptor`, `*Resolver`, `*Controller`, `*Mapper` (package-private final class, static methods only), `*Repository`, `*Config`, `*Properties`, `*Converter`.

## §3 Non-Java Standards
- **orasaka-ui (web-client/web-admin)**: Components: `PascalCase`. Hooks: `camelCase` (prefixed with `use`). Constants: `kebab-case.constants.ts`. APIs: `kebab-case.api.ts`. Tests: `*.test.tsx`. i18n keys: `dot.separated.camelCase`.
- **orasaka-apps/orasaka-workers/video (Python)**: `snake_case` (fn/var/module), `PascalCase` (classes), `UPPER_SNAKE_CASE` (constants). Type hints required on public APIs.
- **Terraform**: `snake_case` (resources, variables, outputs), `kebab-case` (module directories). Descriptions/types mandatory.
- **PostgreSQL**: Tables: `snake_case` plural (e.g., `chat_sessions`). Columns: `snake_case`. Indexes: `idx_{table}_{cols}`. Migrations: `V{N}__{desc}.sql`. Constraints: `uq_{table}_{cols}`, `fk_{table}_{ref}`.

## §4 Backend Invariants
- SQL: Banned raw strings. Use parameterized `@Query` or Spring Data.
- Converters: Place in `*.infrastructure.persistence.converter.*`.
- Queries: No duplication; write methods must return domain records.
- Transactions: Hashing/crypto must run outside `@Transactional`. Write methods must not call internal reads for same entity.
- Read-before-write: Banned. Catch `DataIntegrityViolationException` for unique constraints.
- N+1: Banned; use `LEFT JOIN FETCH` or `@EntityGraph`.
- UI Code Limits: Max 250 lines/file for `.tsx`. Inline `.map()` loops must be sub-components.
