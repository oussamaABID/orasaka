---
description: ARCHITECTURAL STATIC GATE & REJECTION CRITERIA
---

# ORASAKA WORKFLOW: ARCHITECTURAL STATIC GATE & REJECTION CRITERIA

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
10. **Eradication of Procedural Imperative Noise [ERR-104]**: Intercept any code files (services, filters, clients, or engines) utilizing local variable null mutations (e.g. initializing a variable to `null` to mutate it later inside a conditional block), empty catch blocks for business data flows, or imperative loop mutations. *Remediation*: Migrate procedural variables and loop structures into functional `Optional` mapping pipelines, stream-based mapping (`.stream()`), or immediate assignments.
11. **UI Capability Leaking & Manual Switch Bypass [ERR-106]**: The UI layer must remain a completely anemic interpreter. Hardcoding feature-specific conditional code blocks, toggles, or parsing raw flat configuration profiles to manage element visibility is explicitly banned. The UI must construct its input "+" interactive command hierarchy exclusively by traversing the server-provided OrasakaOperationGraph.
12. **Pattern Matching For Sealed Graph States [ERR-107]**: Any internal controller, service, or mapper interrogating a node's capabilities must utilize Java 21 switch expressions with pattern matching over the NodeState sealed hierarchy. Legacy instanceof blocks or type-casting checks will REJECT THE BUILD.

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