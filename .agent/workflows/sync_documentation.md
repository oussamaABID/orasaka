---
description: CONTINUOUS DOCUMENTATION SYNCHRONIZATION
---

# WORKFLOW: CONTINUOUS DOCUMENTATION SYNCHRONIZATION

## §1 — Trigger Conditions

Documentation sync is required when:
- Java interface signatures change (methods added, removed, or modified)
- Entity/record field layouts change
- Configuration keys are added or modified under `orasaka.core`, `orasaka.features`, or `orasaka.infrastructure.identity`
- Flyway migrations add or alter tables/columns
- Terraform modules, variables, or outputs change
- Environment variables are added to `.env` or `orasaka-ui/.env.local`
- API endpoints are added, removed, or security-hardened
- ADRs are created or updated in `docs/CONTEXT.md`

## §2 — Documentation Matrix

### Backend & Architecture
| File | Scope |
|:---|:---|
| `README.md` | Root overview, quickstart, project structure |
| `docs/ARCHITECTURE.md` | Module boundaries, package topology, mermaid diagrams |
| `docs/API_REFERENCE.md` | REST/GraphQL endpoints, security requirements, request/response schemas |
| `docs/ORASAKA_CORE.md` | Core engine pipeline, interceptors, configuration namespace |
| `docs/CONTEXT.md` | ADR log (ADR-001 through ADR-028+) |

### Domain & Operations
| File | Scope |
|:---|:---|
| `docs/AUTH.md` | Authentication flow (NextAuth → BFF → Spring Security) |
| `docs/ORASAKA101.md` | Developer onboarding, local setup, build commands |
| `docs/GLOSSARY.md` | Terminology, env var registry, naming constraints |
| `docs/DEPLOY.md` | Production deployment, Terraform modules, multi-cloud IaC |
| `docs/BUSINESS_IMPLEMENTATION.md` | Business enabler guide |

## §3 — Sync Rules

### API Reference Sync
- Every `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@MutationMapping`, `@QueryMapping` must have a matching entry in `API_REFERENCE.md`.
- Security annotations (`@PreAuthorize`, `hasAnyAuthority`) must be documented.
- Request/response record fields must match actual Java record definitions.

### Environment Variable Registry
- Every env var in `.env`, `orasaka-ui/.env.local`, or `docker-compose.yml` must be listed in `docs/GLOSSARY.md`.
- Each entry: key, purpose, default value, security classification (public/secret).

### Terraform / IaC Drift
- Changes to `ops/deploy/terraform/**` must trigger validation of `docs/DEPLOY.md`.
- Module inputs/outputs must match `variables.tf` and `outputs.tf`.
- Provider version constraints must be documented.

### ADR Sync
- New architectural decisions must be added to `docs/CONTEXT.md` with sequential numbering.
- AGENTS.md `§11` must reference the latest ADR range.

## §4 — Quality Gates

- Missing Javadoc on public interfaces/methods blocks compilation.
- Missing TSDoc on shared React components/hooks is a review violation.
- Documentation must use correct file links: `[ClassName](file:///path)` format.
- Mermaid diagrams must compile without syntax errors.