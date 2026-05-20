# ORASAKA: Domain-Driven AI Monorepo

![Orasaka Logo](docs/assets/logo.svg)

> **Precision in Implementation. Intelligence through Decoupling.**

Orasaka is a professional-grade AI platform architected for **Multi-Session & Multi-Modal Context Memory**. It enforces strict domain isolation, stateless library design, and high-concurrency execution via Java 21 Virtual Threads.

---

## 📚 Documentation

| Document | Description |
| :--- | :--- |
| [API Reference](docs/API_REFERENCE.md) | Full specification of public types, facades, and engine abstractions |
| [Glossary](docs/GLOSSARY.md) | Definitions of all ecosystem terms and design patterns |
| [Architecture Decisions (ADR)](docs/CONTEXT.md) | Architectural Decision Records governing the platform |

---

## 🏛️ Multi-Module Blueprint

| Module | Role |
| :--- | :--- |
| **`orasaka-parent`** | Root BOM. Manages centralized dependency versions (Spring Boot 3.5, Spring AI 1.1.6, Java 21). |
| **`orasaka-core`** | Pure AI Orchestration Engine. Stateless library, Bridge Pattern 2.0. No Spring Boot starters allowed. |
| **`orasaka-identity`** | User management, RBAC, and cross-modality security contracts via Java 21 Sealed Interfaces. |
| **`orasaka-tools`** | Concrete implementations of MCP orchestrators and Function Tool registries. Depends on `orasaka-core`. |
| **`orasaka-gateway`** | GraphQL BFF (Spring Boot 3.5). Merges context from Identity and Core; streams results to UI and CLI. |
| **`orasaka-ui`** | Next.js frontend application. |
| **`orasaka-cli`** | Node.js/TypeScript CLI (future, executable via `npx`). |

> **Dependency Flow**: `orasaka-core` → `orasaka-identity`, `orasaka-tools` → `orasaka-gateway`. Strictly unidirectional. No circular dependencies.

---

## ⚙️ Architectural Mandates

### A. Chat Memory — Session-Based Elasticity

Users can instantiate unlimited independent conversation threads. `orasaka-core` resolves `ChatMemory` state dynamically per `conversationId`, ensuring full multi-tenant session isolation.

### B. Multi-Modal Context Profiles

For non-conversational AI (TTS, Image Generation), request records (e.g., `OrasakaSpeechRequest`) accept an immutable `OrasakaContext` profile carrying user-specific preferences: voice models, speed, and stylistic aspect ratios.

### C. The Decoupling Rule

`orasaka-core` is **stateless**. `orasaka-gateway` is responsible for fetching user profiles from `orasaka-identity`, merging them into an immutable `OrasakaContext`, and injecting that context into the Core Client on every single request.

---

## 🛡️ Technical Standards

| Standard | Enforcement |
| :--- | :--- |
| **Java 21** | Records, Sealed Interfaces, Pattern Matching, and Virtual Threads are mandatory. |
| **Spring AI 1.1.6** | Strictly locked. No version drift permitted via `orasaka-parent` BOM. |
| **No Starter Leaks** | `orasaka-core` must never import `spring-boot-starter` dependencies. |
| **Real-time Streaming** | Gateway stream endpoints use GraphQL Subscriptions or HTTP SSE. |
| **Virtual Threads** | All I/O-intensive and AI-inference tasks run on `Executors.newVirtualThreadPerTaskExecutor()`. |

---

## 🚀 Getting Started (DevX Experience)

### Prerequisites

- **Java 21+** (ensure `JAVA_HOME` points to JDK 21)
- **Maven 3.9+**
- **Node.js 20+** (required for `orasaka-ui` and `orasaka-cli`)
- **Docker Compose** (to spin up auxiliary services)

### Local Environment Setup

Run the bundled setup script which validates the JDK, checks for a native Ollama instance, pulls required models, and starts auxiliary containers:

```bash
./bin/setup.sh
```

> The script will also launch the `pgvector` database and the MCP debug server defined in [docker‑compose.yml](docker-compose.yml).

### Build & Run All Modules

```bash
mvn clean install
```

### Build a Single Module

For fast iteration on a specific module, e.g. the core library:

```bash
mvn clean install -pl orasaka-core
```

### Run the Gateway locally

```bash
mvn spring-boot:run -pl orasaka-gateway
```

The GraphQL Playground will be available at `http://localhost:8080/graphiql`.

For more details, see the [API Reference](docs/API_REFERENCE.md) and the [Architecture Decisions](docs/CONTEXT.md).

---

*Orasaka — Precision in Implementation, Intelligence through Decoupling.*
