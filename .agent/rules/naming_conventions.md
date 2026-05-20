---
trigger: always_on
---

# Orasaka Naming Conventions

## Domain-Driven Naming

- **Core Client**: `OrasakaAiClient`
- **Engine**: `OrasakaEngine` (Abstract: `AbstractOrasakaEngine`)
- **Properties**: `CorsProperties`
- **Exceptions**: `OrasakaException`
- **Models**: Prefix with `Orasaka` (e.g., `OrasakaChatRequest`, `OrasakaChatResponse`)

## Package Structure

- `com.orasaka.core.client`: Facades and entry points.
- `com.orasaka.core.engine`: Core orchestration logic.
- `com.orasaka.core.config`: Configuration records.
- `com.orasaka.core.model`: Unified abstractions.
- `com.orasaka.core.exception`: Error hierarchy.
- `com.orasaka.core.tool`: Unified tooling and function calling.
- `com.orasaka.core.rag`: Knowledge and retrieval services.
- `com.orasaka.core.mcp`: Model Context Protocol integration.

## orasaka-ui Structure

Laisse tomber l'Atomic Design théorique. Va au plus efficace et au plus scalable :

- `components/ui/` : pour la plomberie visuelle brute (Boutons, Inputs, Cards).
- `features/` : pour l'intelligence métier (Chat, Sessions, Rendu Dynamique).
- `app/` : uniquement pour distribuer les routes et assembler les features.
- `core/` : configuration globale et clients (GraphQL, Context).

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
