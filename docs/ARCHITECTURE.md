# Orasaka Architecture Overview

This document provides the technical overview of the Orasaka monorepo architecture, describing its component modules, security boundaries, and runtime execution models.

---

## 🏛️ Component Hierarchy

Orasaka is structured as a decoupled monorepo, keeping business features, infrastructure layers, and AI engines completely isolated.

```mermaid
graph TD
    UI[orasaka-ui BFF/Next.js] <--> Gateway[orasaka-gateway orchestrator]
    CLI[orasaka-cli terminal tool] <--> Gateway
    
    Gateway --> CoreClient[OrasakaAiClient facade]
    Gateway --> Identity[orasaka-identity service]
    
    subgraph Core Engine Layer
        CoreClient --> Core[orasaka-core library]
        Core --> Tools[orasaka-tools configurations]
    end
    
    Identity --> Postgres[(PostgreSQL)]
    Tools --> Postgres
```

### Module Breakdown
- **[orasaka-core](./orasaka-core)**: The stateless, agnostic core library. It holds pure abstractions (RAG interfaces, Model Context Protocol declarations, engine interfaces) and strictly locks down Spring AI to version `1.1.6`. It is completely decoupled from Spring Boot auto-configuration.
- **[orasaka-tools](./orasaka-tools)**: The concrete tool execution and multi-tier cache module. Contains memory/persistent caffeine-to-postgres decorators and concrete MCP integrations.
- **[orasaka-identity](./orasaka-identity)**: Manages authentication credentials, user profiles, BCrypt hashing, and the data-driven Interception & Feedback engine.
- **[orasaka-gateway](./orasaka-gateway)**: The backend entry orchestrator. Handles secure stateless sessions, virtual threads executing parallel requests, and exposes GraphQL & Server-Sent Events stream interfaces.
- **[orasaka-ui](./orasaka-ui)**: Next.js 14 Web UI. Houses standard pages and features (e.g. Chat, Dynamic Remote UI Renderer), and acts as a BFF (Backend-For-Frontend) proxy layer.
- **[orasaka-cli](./orasaka-cli)**: Node-based terminal client allowing chat automation, profile queries, and streaming chat completions.

---

## 🌐 BFF (Backend-for-Frontend) Topology

To prevent security context leakage, browser-side CORS failures, and open-port exposures on the client, the UI follows a strict BFF topology pattern. 

**Key Rule**: The browser client NEVER connects directly to `orasaka-gateway` (port `8080`) or local AI execution environments (e.g., Ollama port `11434`). All asynchronous interactions must go through server-side API Routes in Next.js (`/api/graphql` or `/api/chat/stream/[conversationId]`).

### BFF Topology Flow

Below is the request flow from the browser to the backend database, mediated by Next.js API Routes.

> [!NOTE]
> Environment parameters (like `GATEWAY_URL`) are read exclusively on the Next.js server side. The client code is unaware of the actual backend network topology.

```mermaid
graph TD
    subgraph "Browser Client (Next.js frontend)"
        UI[orasaka-ui React components]
    end

    subgraph "BFF Proxy Layer (Next.js server-side)"
        ChatStream["/api/chat/stream/:conversationId"]
        GraphQL[/api/graphql/]
        NextAuth["/api/auth/*"]
    end

    subgraph "Backend (Port 8080 / Private network)"
        Gateway[orasaka-gateway orchestrator]
        Identity[orasaka-identity identity service]
        Postgres[(PostgreSQL database)]
    end

    %% Browser requests are sent only to the BFF API routes
    UI -- "GET (text/event-stream)" --> ChatStream
    UI -- "POST (graphql operation)" --> GraphQL
    UI -- "POST (login credentials)" --> NextAuth

    %% BFF API routes authenticate user session and proxy requests to backend
    ChatStream -- "GET with Bearer userId (Token injection)" --> Gateway
    GraphQL -- "POST with Bearer userId (Token injection)" --> Gateway
    NextAuth -- "POST /api/v1/auth/login (Credential delegation)" --> Gateway

    %% Backend fetches user profiles and saves conversations
    Gateway -- "Virtual Threads Auth / Profile fetch" --> Identity
    Identity -- "SQL persist/verify" --> Postgres

    %% Styling & annotations
    classDef client fill:#f9f,stroke:#333,stroke-width:2px;
    classDef bff fill:#bbf,stroke:#333,stroke-width:2px;
    classDef backend fill:#dfd,stroke:#333,stroke-width:2px;
    class UI client;
    class ChatStream,GraphQL,NextAuth bff;
    class Gateway,Identity,Postgres backend;
```

---

## 🧠 Cognitive Engine Execution Flow

The `OrasakaEngine` orchestrates the lifecycle of AI requests, RAG context enrichment, tool matching, and streaming token delivery. It executes on the Gateway tier via Virtual Threads.

### Engine Flow Diagram

Below is the internal flow of client calls through the engine abstractions:

```mermaid
graph TD
    User([Developer]) --> Client[OrasakaAiClient Facade]
    Client --> Engine[OrasakaEngine]
    
    subgraph Cognitive Layer
        Engine --> ToolRegistry[OrasakaToolRegistry]
        Engine --> KnowledgeService[OrasakaKnowledgeService]
        Engine --> McpService[OrasakaMcpService]
    end
    
    subgraph Spring AI Layer
        Engine --> ChatModel[Spring AI ChatModel]
        Engine --> ImageModel[Spring AI ImageModel]
        Engine --> EmbeddingModel[Spring AI EmbeddingModel]
        
        ChatModel --> Ollama[Ollama Provider]
        ChatModel --> OpenAI[OpenAI Provider]
    end
    
    subgraph Infrastructure
        KnowledgeService --> VectorStore[(VectorStore)]
        McpService --> McpServer[External MCP Servers]
        ToolRegistry --> JavaMethods[Local Java Methods]
    end
```

---

## 🔏 Generic Interception & Feedback Engine

The `orasaka-identity` module implements an "Intercept & Resume" Session Engine. Downstream business verticals can dynamically prompt users to perform feedback loops or complete surveys using abstract JSON configurations.

### Key Characteristics
1. **Zero-Polling Profile Injection**: Checked during initial Gateway token verification and cached in JWT payloads, preventing unnecessary runtime API queries.
2. **Generic Database Tracking**: Registered in `orasaka_user_interceptions` (mapping a `user_id` to an active `interception_type` and `schema_id`).
3. **Opt-in Passive Activation**: Controlled dynamically by feature flags inside backend configurations.
