# ORASAKA SYSTEM RULE: NAMING CONVENTIONS & BOUNDARIES

## 🏗️ 1. Domain-Driven Java Architecture

* **Stereotypes**:
  * Core client gateway must use `AiClient`.
  * Core execution blueprints must extend `AbstractEngine`.
  * Core configurations must map to `CoreProperties` bound strictly to the `orasaka.core.*` namespace.
* **Package Structure**: Must respect strict domain boundaries within all modules:
  * **orasaka-core**:
    * `com.orasaka.core.config` -> Spring auto-configurations, Core and Features properties records.
    * `com.orasaka.core.domain.model` -> Pure, immutable operation graphs, nodes, execution states, and URIs.
    * `com.orasaka.core.execution` -> State-free execution engines, abstract bases, and factory builders.
    * `com.orasaka.core.pipeline` -> Interceptors, context matrix injectors, streams, and orchestrators.
    * `com.orasaka.core.ingest` -> Multi-modal payload records and ingest handlers.
    * `com.orasaka.core.support` -> Public facades, exceptions, options, and options mappers.
    * **pipeline subpackaging:** The root pipeline package must stay clean. Interceptors must be grouped inside `pipeline.interceptors.[feature]` based on their target scope (contract, rag, tool, prompt, memory). Text processing belongs to `pipeline.chunking`. Main pipeline execution hooks belong to `pipeline.execution`.
  * **orasaka-tools**:
    * `com.orasaka.tools.config` -> Tool context providers and typed properties configuration.
    * `com.orasaka.tools.mcp` -> MCP protocol services, executors, and tool registries.
    * `com.orasaka.tools.functions.[feature]` -> Exposed tool DTO definitions, request/response models, and callback implementations.
    * `com.orasaka.tools.service` -> Internal utility implementations and cache coordinators.
    * `com.orasaka.tools.infrastructure.persistence.entity` -> Cache entries and RAG data source database objects.
    * `com.orasaka.tools.infrastructure.persistence.repository` -> Spring Data repositories mapping to the database layer.
    * `com.orasaka.tools.infrastructure.persistence.converter` -> JPA AttributeConverters and database serialization helpers.
  * **orasaka-identity**:
    * `com.orasaka.identity.config` -> Federation security parameters and infrastructure properties.
    * `com.orasaka.identity.domain` -> Pure user domain records, role models, and profiles.
    * `com.orasaka.identity.infrastructure.persistence.entity` -> JPA database mapping entities.
    * `com.orasaka.identity.infrastructure.persistence.repository` -> Repository layers resolving verification tokens and user mappings.
    * `com.orasaka.identity.infrastructure.persistence.converter` -> JPA AttributeConverters and database serialization/token column helpers.
    * `com.orasaka.identity.service` -> Package-private security and user management orchestrations.
    * `com.orasaka.identity.exception` -> Domain exception topology for identity resolving failures.
  * **orasaka-gateway**:
    * `com.orasaka.gateway.config` -> Security filters, caching filters, and rate-limiting properties.
    * `com.orasaka.gateway.endpoint` -> GraphQL, REST, and SSE Controller edge routes.
    * `com.orasaka.gateway.dto` -> Web/inbound network DTO records.
    * `com.orasaka.gateway.support` -> Media contracts and helper structures.
* **Zero Infrastructure Coupling in Core Engine [ERR-100]**:
  * The core execution engine classes (`AbstractEngine` and `Engine`) must remain completely decoupled from specialized domain services, vector stores, registries, or orchestrators. They must never directly reference classes like `ToolRegistry`, `McpOrchestrator`, `KnowledgeService`, or `MemoryResolver` as fields or constructor arguments. Instead, all context-enrichment behaviors must be executed dynamically through the generic interceptor pipeline (`List<PromptInterceptor>`).
* **Stateless Core IoC Isolation**:
  * To decouple the core library from specific tool implementations (e.g., `orasaka-tools`), the core must use the abstract `SystemContextProvider` interface. Tool packages must register provider beans downstream without coupling the core runtime to tools.
 
### Backend Constraint Rules

* **Database Constraint**: Raw String SQL queries are strictly banned. Use typesafe Spring Data JPA Repositories or start-up validated `@Query` structures.
* **Persistence Plumbing & Converters Invariant**:
  * **Technical Converters:** Any class implementing `AttributeConverter` or framework database serialization helpers (e.g., JSON mapping, encrypted columns) belongs strictly to the database plumbing layer.
  * **Namespace Rule:** They must reside exclusively inside `com.orasaka.[module].infrastructure.persistence.converter..`.
  * **Layer Isolation:** Converters are prohibited from floating inside domain, root service, or entity packages. The `entity` subpackage must only hold structural DB schema mapping records or classes.
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
* **Rule**: Orasaka Ingress layers MUST strictly segregate protocol handlers into specialized, isolated sub-packages under the adapter layer: 'adapter.rest' for REST/SSE endpoints, 'adapter.graphql' for GraphQL schemas, and 'adapter.amqp' for asynchronous workers. Mixing different communication protocols in a flat package is now classified as a severe architectural violation.

## 🛡️ 4. Rich Domain Records & Defensive Architecture

* **Rule**: Procedural state validation inside Services is banned. Domain Records are the unique source of truth for their own validity.
* **Rule**: Collections inside Records must ALWAYS be defensively copied (`Map.copyOf`, `List.copyOf`, `Set.copyOf`) during construction to achieve compile-time and runtime immutability.
* **Rule**: Read-before-write existence checking (e.g. executing select queries prior to inserts) is strictly banned to prevent concurrency race conditions. Database-level UNIQUE constraints must handle key collision validation, caught via `DataIntegrityViolationException`.
* **Rule**: Hashing, cryptography, and heavy CPU-bound actions must execute outside transaction blocks (`@Transactional`) to minimize database connection holding times under Virtual Threads.

## 🎬 5. Multi-Modal Ingestion Boundaries [ERR-101]

* **The Ingestion Principle:** All raw binary or complex file formats (Video, Audio, Image, Documents) must be parsed, compressed, or transcribed *before* hitting the execution layer.
* **Package Distribution:**
  * `com.orasaka.core.ingest.*` → Ports, DTO templates, and lightweight processed records.
  * `com.orasaka.core.infrastructure.*` → Operational hardware adapters (e.g., FFmpeg pipelines, RestClient wrappers).
* **The Text Exception:** Raw textual sequences and native chat tokens bypass the ingest layer entirely, flowing directly into `pipeline` interceptors.
* **Inversion of Control:** The `engine` package must remain 100% blind to concrete infrastructure tools. It may only import structures from the `ingest` abstraction tree.
* **Credential Hygiene [ERR-102]:** Direct `System.getenv()` invocations and hardcoded credential fallbacks (`"dummy-key"`) are strictly banned in production source. All external values must flow through typed `CoreProperties` configuration records, with YAML `${ENV_VAR:default}` bindings.
