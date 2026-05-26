# ORASAKA — Agent Runtime Behavior & Development Rules

> **Scope**: This document governs the runtime behavior expectations for LLM developer sub-agents
> working within the Orasaka codebase. For the project-wide governance contract (module boundaries,
> ERR codes, frontend standards), see the root [`AGENTS.md`](../AGENTS.md).

---

## §1 Mandatory SovereignWorkflowContext Usage

All business feature implementations requiring AI orchestration **must** declare their
execution intent via a `SovereignWorkflowContext` record:

```java
SovereignWorkflowContext context = new SovereignWorkflowContext(
    "session-id",           // contextId
    "System instructions",  // systemInstructions
    "PRO",                  // userTier
    Set.of("Refiner"),      // forcedInterceptors
    Set.of(),               // skippedInterceptors
    Map.of("key", "value")  // metadata
);
orchestrator.executeSovereignPrompt(userPrompt, context);
```

**Banned**: Constructing `ChatRequest` or `Context` directly from business code. The
`SovereignWorkflowAdapter` in the gateway is the **sole** translation boundary.

---

## §2 Interceptor Bypass Prohibition

Agents generating code that interacts with the AI pipeline **must never**:

1. Circumvent the `DynamicPipelineExecutor` by calling `AiClient` directly from business logic.
2. Hardcode interceptor ordering — sequence is resolved dynamically from the `PipelineRegistry`.
3. Import Core infrastructure types (`Context`, `ChatRequest`, `PromptContext`) into `orasaka-business`.
4. Place concrete `PromptContextInterceptor` implementations inside `orasaka-core` (ERR-122).

---

## §3 Prompt Template Localization Rules

| Template Type | Location | Format |
|:---|:---|:---|
| **Structural engine templates** (routing, refinement, context envelope) | `orasaka-core/src/main/resources/prompts/` | `.st` (StringTemplate) |
| **Business persona prompts** (domain-specific, product verticals) | `orasaka-business/src/main/resources/prompts/business/` | `.md` (Markdown) |
| **Generic business prompts** (welcome agent, shared personas) | `orasaka-business/src/main/resources/prompts/` | `.md` (Markdown) |

**Banned**: Placing `.md` business prompts in `orasaka-core`. Placing `.st` engine templates
in `orasaka-business`.

---

## §4 ArchUnit Compliance Mandate

The `GovernanceTest` class in `orasaka-core` enforces compile-time architectural rules via
ArchUnit. These are **non-negotiable**:

| Rule | ERR Code | Enforcement |
|:---|:---|:---|
| No web starters in core | ERR-102 | `noClasses().dependOnClassesThat().resideInAPackage("org.springframework.web..")` |
| No AMQP in core | ERR-118 | `noClasses().dependOnClassesThat().resideInAPackage("org.springframework.amqp..")` |
| Only interface in interceptor package | ERR-122 | `classes().should().beInterfaces()` |
| No concrete interceptors in core | ERR-122+ | Zero implementations of `PromptContextInterceptor` |
| Core prompts allow-list | ERR-125 | Only `context-envelope.st`, `system-refinement.st`, `system-router.st` |
| Records are pure data carriers | ERR-135 | No loggers, I/O, Spring beans, or service facades |

Agents must run `./mvnw test -pl orasaka-framework/orasaka-core -Dtest=GovernanceTest` before
committing any changes to the core module.

---

## §5 Test Coverage Requirements

- **1:1 test file matching** is mandatory for all generated Java files (ERR-103).
- Test files must mirror the source package structure under `src/test/java/`.
- Self-validating records must have tests covering:
  - Compact constructor null-rejection
  - Defensive copying of mutable collections
  - Default value assignment
- Integration tests must extend `AbstractContainerIntegrationTest` (ADR-033/034).

---

## §6 Code Generation Checklist

Before an agent considers any code generation task complete, it must verify:

- [ ] All new Java files have a 1:1 test counterpart
- [ ] No new classes are prefixed with `Orasaka` (ERR-104)
- [ ] Records are stateless data carriers with no I/O or logging (ERR-135)
- [ ] Business features use `SovereignWorkflowContext` exclusively
- [ ] `./mvnw spotless:apply` passes without diff
- [ ] `./mvnw clean test-compile` produces zero errors
- [ ] `GovernanceTest` passes all ArchUnit rules
