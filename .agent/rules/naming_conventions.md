# ORASAKA SYSTEM RULE: NAMING CONVENTIONS & BOUNDARIES

## 🏗️ 1. Domain-Driven Java Architecture

* **Stereotypes**:
  * Core client gateway must use `OrasakaAiClient`.
  * Core execution blueprints must extend `AbstractOrasakaEngine`.
  * Core configurations must map to `CoreProperties` bound strictly to the `orasaka.core.*` namespace.
* **Package Structure**: Must respect strict domain boundaries within `orasaka-core`:
  * `com.orasaka.core.client` -> Client interfaces and external API connectors.
  * `com.orasaka.core.engine` -> Core execution loops, engine, and option mappers.
  * `com.orasaka.core.config` -> Configuration binders and properties.
  * `com.orasaka.core.model` -> Request/Response models and options schemas.
  * `com.orasaka.core.exception` -> Framework custom exception hierarchy.
  * `com.orasaka.core.interceptors` -> Interceptor interfaces and specialized implementations (e.g. `mcp`, `tool`, `rag`, `memory`).
* **Zero Infrastructure Coupling in Core Engine [ERR-100]**:
  * The core execution engine classes (`AbstractOrasakaEngine` and `OrasakaEngine`) must remain completely decoupled from specialized domain services, vector stores, registries, or orchestrators. They must never directly reference classes like `OrasakaToolRegistry`, `McpOrchestrator`, `OrasakaKnowledgeService`, or `OrasakaMemoryResolver` as fields or constructor arguments. Instead, all context-enrichment behaviors must be executed dynamically through the generic interceptor pipeline (`List<OrasakaContextInterceptor>`).
* **Stateless Core IoC Isolation**:
  * To decouple the core library from specific tool implementations (e.g., `orasaka-tools`), the core must use the abstract `SystemContextProvider` interface. Tool packages must register provider beans downstream without coupling the core runtime to tools.

### Backend Constraint Rules

* **Database Constraint**: Raw String SQL queries are strictly banned. Use typesafe Spring Data JPA Repositories or start-up validated `@Query` structures.
* **Persistence I/O Minimization Mandate**:
  * **Rule**: Transactional write/update methods (`save`, `register`, `provision`, `updatePreferences`) must NEVER invoke internal repository-backed read calls (e.g. `getUser()`) if the entity state is already present in the active thread memory. In mutation endpoints, the update method must return the fully updated domain representation instead of invoking a separate lookup.
  * **Remediation**: Force the extraction of static or private in-memory domain mappers to assemble the output data layer instantly, cutting database roundtrips by 50%.
* **Universal Monorepo Data Fetch & Persistence Purity [ERR-200]**:
  * **Rule**: Service and orchestration layers must remain completely decoupled from manual JSON parsing/serialization (`ObjectMapper`) and unoptimized sequential queries (N+1 database reads).
  * **Remediation**: Manual JSON serialization is banned in service layers; concerns must be offloaded to native JPA Attribute Converters `@Convert`. Sequential queries for single-domain projections must be combined using JPQL `LEFT JOIN FETCH` or Entity Graphs to enforce exactly one single atomic database roundtrip.

## 🎨 2. Front-End Component Architecture (`orasaka-ui`)

* **Strict 250-Line Limit**: No React file (`.tsx`) under `orasaka-ui` may exceed 250 lines of code. Large files must be modularized and extracted into sub-components.
* **Inline Loops**: Any `.map()` loop rendering non-trivial inline JSX blocks must be extracted into its own sub-component.
* **State Logic**: All asynchronous server operations and data mutations must be driven exclusively via `@tanstack/react-query`.

## 🏛️ 3. Hexagonal Edge Packaging & Protocol Isolation

* **Rule**: `orasaka-identity` must NEVER declare dependencies on `spring-boot-starter-web` or `graphql`. It must remain a pure Java business core.
* **Rule**: All ingress networks must be strictly isolated inside `orasaka-gateway` and packaged into flat protocol folders (`rest/`, `graphql/`). Mixing REST components with GraphQL schemas inside a single generic controller package is strictly prohibited.

## 🛡️ 4. Rich Domain Records & Defensive Architecture

* **Rule**: Procedural state validation inside Services is banned. Domain Records are the unique source of truth for their own validity.
* **Rule**: Collections inside Records must ALWAYS be defensively copied (`Map.copyOf`, `List.copyOf`, `Set.copyOf`) during construction to achieve compile-time and runtime immutability.
* **Rule**: Read-before-write existence checking (e.g. executing select queries prior to inserts) is strictly banned to prevent concurrency race conditions. Database-level UNIQUE constraints must handle key collision validation, caught via `DataIntegrityViolationException`.
* **Rule**: Hashing, cryptography, and heavy CPU-bound actions must execute outside transaction blocks (`@Transactional`) to minimize database connection holding times under Virtual Threads.
