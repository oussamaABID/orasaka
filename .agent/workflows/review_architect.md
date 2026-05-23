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

## 🚀 Workflow Pipeline Order
The final verification execution sequence must follow:
1. Generate / Refactor target files.
2. Run ecosystem formatters (`mvn spotless:apply` or `npm run format`).
3. Run static analysis quality gates (`mvn spotless:check` or `npm run lint`).
4. Execute project compiling (`mvn clean compile -pl orasaka-gateway -am`).