# ORASAKA SYSTEM RULE: NAMING CONVENTIONS & BOUNDARIES

## 🏗️ 1. Domain-Driven Java Architecture
* **Stereotypes**: 
  * Core client gateway must use `OrasakaAiClient`.
  * Core execution blueprints must extend `AbstractOrasakaEngine`.
  * Core configurations must map to `CoreProperties` bound strictly to the `orasaka.core.*` namespace.
* **Package Structure**: Must respect strict domain boundaries within `orasaka-core`:
  * `com.orasaka.core.client` -> Client interfaces and external API connectors.
  * `com.orasaka.core.engine` -> Core execution loops, memory resolution, and engine orchestrators.
  * `com.orasaka.core.config` -> Configuration binders and properties.
  * `com.orasaka.core.model` -> Request/Response models and options schemas.
  * `com.orasaka.core.exception` -> Framework custom exception hierarchy.
  * `com.orasaka.core.mcp` -> Model Context Protocol integration layers and orchestrators.
  * `com.orasaka.core.tool` -> Core tool registries and callbacks.
* **Stateless Core IoC Isolation**:
  * To decouple the core library from specific tool implementations (e.g., `orasaka-tools`), the core must use the abstract `SystemContextProvider` interface. Tool packages must register provider beans downstream without coupling the core runtime to tools.
### Backend Constraint Rules

* **Database Constraint**: Raw String SQL queries are strictly banned. Use typesafe Spring Data JPA Repositories or start-up validated `@Query` structures.
* **Persistence I/O Minimization Mandate**:
  * **Rule**: Transactional write/update methods (`save`, `register`, `provision`, `updatePreferences`) must NEVER invoke internal repository-backed read calls (e.g. `getUser()`) if the entity state is already present in the active thread memory. In mutation endpoints, the update method must return the fully updated domain representation instead of invoking a separate lookup.
  * **Remediation**: Force the extraction of static or private in-memory domain mappers to assemble the output data layer instantly, cutting database roundtrips by 50%.


## 🎨 2. Front-End Component Architecture (`orasaka-ui`)
* **Strict 250-Line Limit**: No React file (`.tsx`) under `orasaka-ui` may exceed 250 lines of code. Large files must be modularized and extracted into sub-components.
* **Inline Loops**: Any `.map()` loop rendering non-trivial inline JSX blocks must be extracted into its own sub-component.
* **State Logic**: All asynchronous server operations and data mutations must be driven exclusively via `@tanstack/react-query`.
