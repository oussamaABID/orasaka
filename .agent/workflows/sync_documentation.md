---
description: CONTINUOUS DOCUMENTATION SYNCHRONIZATION
---

# Workflow: Documentation Sync

## §1 Trigger Conditions
Documentation updates are required on:
- Java interface or DTO/record modifications
- Config key changes (`orasaka.core`, `orasaka.features`, `orasaka.infrastructure.identity`)
- Flyway migration updates
- Terraform modules/variables/outputs changes
- Environment variables changes (`.env`, `orasaka-apps/orasaka-ui/orasaka-web-client/.env.local`)
- API endpoint routing or security changes
- ADR additions or updates in `docs/CONTEXT.md`
- Maven `pom.xml` changes (especially `frontend-maven-plugin` configuration)
- CI workflow changes (`.github/workflows/ci.yml`)
- Workspace structure changes (`orasaka-apps/orasaka-ui/package.json`)

## §2 Documentation Matrix
- **README.md**: Root overview, setup, module outline.
- **docs/ARCHITECTURE.md**: System topology, BFF schemas, execution flows.
- **docs/API_REFERENCE.md**: Endpoints, parameters, schemas, RBAC constraints.
- **docs/CORE.md**: Engine pipelines, configuration keys, interceptor blue-print.
- **docs/CONTEXT.md**: Architectural Decision Record (ADR) history.
- **docs/AUTH.md**: Authentication flow (NextAuth -> BFF -> Spring Security).
- **docs/101.md**: Developer onboarding & fast track.
- **docs/GLOSSARY.md**: Env vars, terms, naming rules.
- **docs/DEPLOY.md**: Deploy protocols, infrastructure IaC.
- **docs/BUSINESS_IMPLEMENTATION.md**: Business enabler patterns.
- **docs/AUTOMATION.md**: Workers, Quartz/Flyway, Local Agent Protocol.
- **docs/CLI.md**: Command-line reference guide.
- **docs/END2END_TEST.md**: E2E framework (scope: Java/Web API only).
- **docs/MODELS.md**: Model registry catalog details.
- **docs/UI_REFERENCE.md**: Frontend interfaces, workspace architecture.
- **docs/MASTER_FEATURES.md**: Feature delivery status matrix.

## §3 Sync Constraints
- **API Reference**: Every `@*Mapping` and `@PreAuthorize` constraint must match an entry in `API_REFERENCE.md`.
- **Environment**: All env variables in `.env` / `orasaka-apps/orasaka-ui/.env.local` must be documented in `docs/GLOSSARY.md` (key, purpose, default, classification).
- **IaC**: Changes to `infra/terraform/**` must update `docs/DEPLOY.md`.
- **ADR**: Log new decisions sequentially in `docs/CONTEXT.md` and reference in `AGENTS.md`.

## §4 Quality Gates
- Missing Javadoc or TSDoc is a build/review violation.
- File links must use `[Name](file:///path)` format.
- Mermaid diagrams must compile without errors.