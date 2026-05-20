# 🥷 ORASAKA: SYSTEM & AGENT STANDARDS

## 🏛️ 1. Core Architectural Directives

### A. Structural Agnosticism & Decoupling

* **The Stateless Core**: `orasaka-core` is a **standalone library** and must remain completely stateless regarding user storage or preferences.
* **No Starter Leaks**: `orasaka-core` must **NEVER** import `spring-boot-starter` or any framework auto-configuration dependencies. Only `spring-ai-core` 1.1.6 and direct provider implementations (Ollama, OpenAI) are permitted.
* **Structural Rebuttal**: Reject any code proposal that leaks `org.springframework.ai` types into the `orasaka-gateway`, `orasaka-identity`, or `orasaka-cli` modules.
* **Gateway Responsibility**: `orasaka-gateway` is strictly responsible for fetching user profiles from `orasaka-identity`, merging them into an immutable `OrasakaContext`, and injecting them into the Core Client on every single request.

### B. The Bridge Pattern 2.0

Direct exposure of external AI frameworks is strictly forbidden.

* **Encapsulation**: All `org.springframework.ai` types must be wrapped in Orasaka-native abstractions (`OrasakaChatRequest`, `OrasakaOptions`, etc.).
* **Facade Access**: External modules and clients must only interact with the core through the `OrasakaAiClient` facade.

---

## ⚡ 2. Technical & Coding Standards

### A. Java 21 Mandatory

All backend source code must utilize modern Java 21 features natively:

* **Records**: Mandatory for all configuration, data transfer objects (DTOs), context objects, and domain models.
* **Sealed Interfaces**: Mandatory for domain-driven hierarchies (e.g., RBAC, Provider types).
* **Pattern Matching**: Compulsory for switch expressions and instance checks across all orchestration logic.

### B. Concurrency & Streaming Architecture

* **Virtual Threads**: Every I/O or AI-intensive task MUST utilize `Executors.newVirtualThreadPerTaskExecutor()` to ensure high-concurrency scalability.
* **Async Orchestration**: All multi-threaded Gateway orchestration (resolving user preferences + executing AI token streaming) MUST run asynchronously using Virtual Threads.
* **Real-time Streaming**: Gateway stream endpoints must support GraphQL Subscriptions or HTTP SSE (Server-Sent Events) to seamlessly feed both `orasaka-ui` and the terminal `orasaka-cli`.

### C. Multi-Modal Contextual Profiles

* For non-conversational AI components (Text-To-Speech, Image Generation), request records must accept an `OrasakaContext` record carrying the `userId`, `conversationId`, and a defensively copied `Map<String, Object> preferences`.
* This context dynamically overrides global application configurations to enforce user-specific preferences (voice models, speed, stylistic aspect ratios) at the request level.

---

## 🛡️ 3. Guardian Protocol & Security Guardrails

### A. Data Leak Prevention (Strict Multi-Tenancy)

* You are the **Guardian of Orasaka**. Accuracy and isolation are non-negotiable.
* **CRITICAL**: Flag any generated data access object, repository mapping, or memory resolver that breaks isolation between different `userId` or `conversationId` boundaries. Multi-session and multi-user data MUST be strictly partitioned and isolated.
* Allow users to instantiate unlimited independent conversation threads. `orasaka-core` must resolve the specific `ChatMemory` state dynamically using a unique `conversationId` per thread.

### B. Dependency & Workspace Harmony

* Use the `orasaka-parent` BOM in the root `pom.xml` for all version consistency.
* **Spring AI Version**: Strictly locked and adhered to version **1.1.6**.

---

## 💻 4. Agent Execution Modes

* **Reasoning Chain**: All code generation must be preceded by a transparent technical reasoning chain explaining architectural choices.
* **Turbo Mode**: Verified terminal actions (compilation, folder scaffolding, tests) are unlocked and automated using the `// turbo` suffix.
* **Flash Optimization**: Every prompt generated for tools, models, or configurations must be concise, direct, and high-token-efficiency (Gemini 3 Flash optimized).

---

## 🎨 5. Frontend Architecture (orasaka-ui)

Laisse tomber l'Atomic Design théorique. Va au plus efficace et au plus scalable :

* **`components/ui/`**: Pour la plomberie visuelle brute (Boutons, Inputs, Cards).
* **`features/`**: Pour l'intelligence métier (Chat, Sessions, Rendu Dynamique).
* **`app/`**: Uniquement pour distribuer les routes et assembler les features.
* **`core/`**: Configuration globale et clients. **Règle absolue :** Toutes les requêtes asynchrones et la gestion d'état serveur doivent obligatoirement utiliser **TanStack Query** (React Query).

### Structure de référence (`src/`)

```
src/
├── app/                      # Next.js App Router (Pages, Routing, Layouts)
│   ├── chat/page.tsx
│   └── dashboard/page.tsx
├── components/               # Les Primitives (Agnostiques au métier)
│   └── ui/                   # Tes "Atomes" (Générés via Shadcn/UI)
│       ├── button.tsx
│       ├── card.tsx
│       └── dialog.tsx
├── features/                 # Le Cœur de l'application (Par Domaines)
│   ├── chat-session/         # Tout ce qui concerne le Chat & la Mémoire
│   │   ├── components/       # ChatWindow, MessageBubble, ThreadList
│   │   ├── hooks/            # useChatStream.ts
│   │   └── types/            # chat.types.ts
│   ├── dynamic-renderer/     # LA FEATURE CLÉ : Ton moteur de rendu d'UI à la volée
│   │   ├── components/       # RemoteUiResolver.tsx, ComponentMapper.tsx
│   │   └── utils/            # layoutEngine.ts
│   └── document-analyzer/    # Exemple d'une feature business future
└── core/                     # Configuration globale et clients
    ├── graphql/              # Client Apollo/Urql, requêtes & abonnements (SSE)
    └── context/              # Providers globaux (OrasakaContext, Theme)
```

---
*Orasaka: Precision in Implementation, Intelligence through Decoupling.*