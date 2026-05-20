---
description: Scaffold Domain
---

# Workflow: Scaffold Domain

## Mission
Create a new business domain across the entire Orasaka monorepo following the 4-tier ecosystem architecture.

## Input
- **Domain Name**: e.g., `SaaS-Billing`
- **Modality**: e.g., `Text`, `Speech`, `Image`

## Execution Steps

### 1. Orasaka-Core (Library Layer)
- **Model Definition**: Create `com.orasaka.core.model.Orasaka[Domain]Request` as a Java 21 Record.
- **Context Injection**: Ensure the request record includes/accepts `OrasakaContext`.
- **Stateless Engine**: Implement logic in `orasaka-core` without using Spring Boot starters or persistence.
- **Bridge Pattern**: Wrap external AI models (Spring AI 1.1.6) in Orasaka abstractions.

### 2. Orasaka-Identity (Security Layer)
- **Security Contracts**: Define domain permissions using Sealed Interfaces.
- **Preference Mapping**: Add domain-specific user preferences to the `Identity` profile if needed.

### 3. Orasaka-Gateway (BFF Layer)
- **Contextual Resolution**: Implement a resolver that fetches `Identity` profiles and injects them into `OrasakaContext`.
- **GraphQL Schema**: Add types and subscriptions to `src/main/resources/graphql/schema.graphqls`.
- **Virtual Thread Execution**: Orchestrate the Core Client call within a Virtual Thread executor.
- **Streaming**: Ensure response mapping supports SSE or GraphQL Subscriptions.

### 4. Orasaka-UI (Frontend)
- **Typed Integration**: Generate types from the Gateway GraphQL schema.
- **Component Design**: Build premium Next.js components in `orasaka-ui/src/app/[domain]`.

### 5. Orasaka-CLI (Terminal Interface)
- **Blueprint Update**: Add CLI commands to the Node.js/TypeScript CLI to expose the new domain.

## Guardian Protocol Check
- [ ] No `spring-boot-starter` in `orasaka-core`?
- [ ] Java 21 Records used for all DTOs?
- [ ] `conversationId` used for session partitioning?
- [ ] No data leak between users/sessions?

## Success Criteria
- [ ] `mvn clean install` passes in root.
- [ ] GraphQL schema validates correctly.
- [ ] Integration test verifies context injection from Gateway to Core.
