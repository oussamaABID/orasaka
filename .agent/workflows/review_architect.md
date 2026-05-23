---
description: ARCHITECTURAL STATIC GATE & REJECTION CRITERIA
---

# ORASAKA WORKFLOW: ARCHITECTURAL STATIC GATE & REJECTION CRITERIA

> ### 🔌 0. MCP ENGINE BOOTSTRAPPER & CONTEXT INITIALIZATION
> * **Mandatory Boot Sequence**: Before executing any generation cycle, code modification, or file review, you MUST invoke your filesystem MCP tools to read and index all configuration sheets inside the `.agent/rules/` workspace directory, specifically:
>   - `.agent/rules/naming_conventions.md`
>   - `.agent/rules/security_standards.md`
> * **Constraint**: Your internal generation tokens and constraints must remain 100% synchronized with these localized configuration states. Hallucinating or bypassing rule checking will trigger an immediate generation freeze.

## ⛔ Rejection Gates

Immediately halt execution and reject any generation cycle if the following patterns are detected:

1. **Inline Serialization Pollution**: Multi-line try-catch blocks handling Jackson mapping (`ObjectMapper`) inside standard service paths. *Remediation*: Extract into private mapping utilities or custom helper classes.
2. **Simulated Token Streaming**: Simulating streaming tokens via raw string splitting (`.split(" ")`) or arbitrary `Thread.sleep` calls. *Remediation*: Enforce native reactive non-blocking `Flux<OrasakaChatResponse>`.
3. **FQCN Inline Exposure**: Utilizing Fully Qualified Class Names inside method blocks (e.g., `java.util.Set<String>`). *Remediation*: Declare explicit imports at the top of the file.
4. **Mutation Query Duplication**: Resolving a database write or mutation followed by a separate query/read lookup of the same record within the controller/facade layer. *Remediation*: Ensure the write/mutation method returns the updated domain record directly.
5. **Procedural Invariant Checks & Anemic Records [ERR-042]**: Intercept any new or modified Application Service (`*Service.java`) performing defensive null-checks or assigning default domain values (e.g., `lang = language != null ? language : "en"`), or any anemic domain records missing validation or defensive copies. *Remediation*: Force the migration of validation and fallback logic directly into the Domain Record's compact constructor. The Service layer must remain a pure orchestration engine.
6. **Read-Before-Write Concurrency Vulnerability [ERR-043]**: Intercept any persistence or registration services performing a read check (e.g. `count*`, `exists*`, `find*`) solely to validate an upcoming write operation within the same thread context. *Remediation*: Offload unique constraints to database constraints handled via Spring's `DataIntegrityViolationException` catching. Hashing and heavy CPU tasks must be computed outside `@Transactional` contexts.
7. **Cognitive Core Infrastructure Coupling [ERR-100]**: Intercept any modifications or new declarations in the cognitive core engines (`AbstractOrasakaEngine.java` and `OrasakaEngine.java`) that introduce direct field bindings or constructor parameters referencing specialized infrastructure services (e.g., `OrasakaToolRegistry`, `McpOrchestrator`, `OrasakaKnowledgeService`, `OrasakaMemoryResolver`). *Remediation*: Force dynamic context enrichment through the generic interceptor pipeline (`List<OrasakaContextInterceptor>`).
8. **Enterprise Data Fetch & Persistence Purity [ERR-200]**: Intercept any business services or cognitive orchestration layers performing manual JSON parsing/serialization or unoptimized sequential repository lookups (causing N+1 queries). *Remediation*: Serialization must be offloaded to native JPA Attribute Converters, and multi-table reads combined via optimized JPQL fetch joins.
9. **Universal Record Invariant Validation Enforcement [ERR-103]**: Intercept any application services (`*Service.java`), clients (`*Client.java`), or engines (`*Engine.java`) performing manual, procedural validation logic or fallback defaults assignment (e.g. mapping null values, empty check guards, setting language codes, or using ternary expressions to initialize collections). All state constraints, collection safety copies, and field invariants MUST reside within the compact constructor of the corresponding Domain Record (e.g. `User.java`, `OrasakaChatRequest.java`). *Remediation*: Move validation logic and safety defaults to the compact constructor of the target Java Record, rejecting invalid state with `IllegalArgumentException` or `NullPointerException`.
10. **Eradication of Procedural Imperative Noise [ERR-104]**: Universal ban on local variable null mutations, intermediate procedural states, conditional ternary nesting for file type sniffing, and empty catch blocks. All boilerplate Javadoc, obvious `/** get */` method comments, or self-evident inline comments are forbidden and must be purged. Applies to both backend Java modules and the `orasaka-cli` TypeScript module.
11. **UI Logic Leaking & Operation Graph Bypass [ERR-106]**: The UI and CLI layers must remain anemic interpreters. Hardcoding feature toggles or mapping raw configuration blocks for element visibility is strictly banned. The "+" input menu tree and CLI options must map dynamically to the `TargetExecutionUri` contracts resolved from the `OrasakaOperationGraph` node states. Hardcoding CLI feature command switches locally is strictly prohibited.
12. **Pattern Matching For Sealed Graph States [ERR-107]**: Any internal controller, service, or mapper interrogating a node's capabilities must utilize Java 21 switch expressions with pattern matching over the NodeState sealed hierarchy. Legacy instanceof blocks or type-casting checks will REJECT THE BUILD.
13. **Pseudo-Record Accessor Check [ERR-109]**: Any standard `public class` data component mimicking record naming patterns without standard JavaBean prefixes (`get`, `is`, `has`) for its properties will automatically REJECT THE BUILD. Context maps and data transfer blocks must be true Java 21 `record` types.
14. **Architectural Encapsulation Leakage Protection [ERR-110]**: Component implementation sub-steps (Interceptors, Filters, internal Converters) must remain package-private. Only facades or unified orchestrators are allowed to be public.
15. **Strong Polymorphic Invariance (Back & Front) [ERR-111]**: Force the use of compile-time exhaustive pattern matching over sealed hierarchies in Java 21 and strict discriminated unions (`kind: 'text' | 'image' | 'audio'`) in TypeScript (e.g., inside the CLI console timeline render). Manual type-sniffing via regex or inline string testing in the UI is strictly banned. All boilerplate Javadoc, obvious `/** get */` method comments, or self-evident inline comments are forbidden and must be purged.
16. **High-Density Structural Locality & Unified Frontiers [ERR-112]**: Absolute ban on splitting API entry points into technical sub-packages by protocol (e.g., separating `rest` and `graphql` folders is FORBIDDEN). All entry points must live together in a single cohesive `.endpoint` perimeter. Universal ban on scattering interconnected data transfer structures (DTOs). All transport contracts for a specific feature domain (e.g., Login, Registration, OAuth) MUST be inline immutable Java 21 records grouped inside a single unified container file (e.g., `AuthContracts.java`). Failure to group transport layouts or technical protocols will **REJECT THE GENERATION**.
17. **Absolute Prohibition of Direct Environment Sniffing in Functional Beans [ERR-113]**: Injecting `org.springframework.core.env.Environment` into functional components, filters, services, interceptors, or controllers to call `.getProperty()` or `.acceptsProfiles()` inside code bodies is strictly FORBIDDEN. All functional components must remain completely oblivious to raw environment variables and Spring profile names. Configuration mapping must be managed exclusively at the configuration bootstrap layer using immutable, type-safe Record structures populated via Spring's programmatic Binder matrix. Components must receive clean, pre-resolved configuration properties through explicit constructor dependency injection.

## ✍️ Documentation Gate

* **Java 21 Javadoc Rule**: Missing Javadoc annotations on public interfaces, methods, or record structures will automatically fail compilation cycles.
* **Front-End TSDoc Rule**: All shared TypeScript components, hooks, and helpers under `orasaka-ui` must be fully documented using valid TSDoc annotations.

## 🧹 Code Quality & Formatting Gates

Enforce strict formatting using official ecosystem tools before task completion.

### 1. Java Backend Quality Gates (Spotless + Google Java Style)

* **Formatting Execution**: Prior to backend task completion, format the code within the specific modified module using Spotless:

  ```bash
  mvn spotless:apply -pl <module-name>
  ```

* **Validation Check**: Verify that no formatting rules are broken:

  ```bash
  mvn spotless:check -pl <module-name>
  ```

  If this check fails, formatting must be re-applied and re-verified.

### 2. Frontend Quality Gates (Prettier + ESLint)

* **Prettier Code Formatting**: format React, TypeScript, and JSON files inside `orasaka-ui` before committing:

  ```bash
  cd orasaka-ui && npm run format
  ```

* **ESLint Static Analysis**: Ensure ESLint checks pass cleanly:

  ```bash
  cd orasaka-ui && npm run lint
  ```

  All errors/warnings must be resolved until a clean exit status is achieved.

## 🔍 Workspace Discovery & Semantic Lookup Constraint

* **The Constraint**: Direct iterative file-looping or blind directory-walking across packages for multi-module reviews is now **STRICTLY BANNED**.
* **The Workflow**: For any broad architectural pass (such as enforcing `[ERR-100]` or `[ERR-200]`), you must first invoke the MCP tool `search_symbols` or `grep_pattern` from the workspace database (`orasaka-code-intel`). Extract only the semantic coordinates of matching files, and open exclusively those specific file buffers.

## 🚀 Workflow Pipeline Order

The final verification execution sequence must follow:

1. Generate / Refactor target files.
2. Run ecosystem formatters (`mvn spotless:apply` or `npm run format`).
3. Run static analysis quality gates (`mvn spotless:check` or `npm run lint`).
4. Execute project compiling (`mvn clean compile -pl orasaka-gateway -am`).