---
description: Orasaka Documentation Synchronization Workflow
---

# Orasaka Documentation Synchronization Workflow

## Synchronization Policy
- **Trigger**: Any modification to Public Interfaces, Records (Properties), or Engine abstractions.
- **Action**: Update associated documentation artifacts to maintain "Single Source of Truth".

## Target Artifacts
1. **[README.md](file:///Users/oussamaabid/Documents/projects/orasaka/README.md)**: High-level overview and Quick Start. Always include the orasaka logo (file:///Users/oussamaabid/Documents/projects/orasaka/docs/assets/logo.svg) on the top of [README.md](file:///Users/oussamaabid/Documents/projects/orasaka/README.md).
2. **[API_REFERENCE.md](file:///Users/oussamaabid/Documents/projects/orasaka/docs/API_REFERENCE.md)**: Detailed technical specification of public types.
3. **[GLOSSARY.md](file:///Users/oussamaabid/Documents/projects/orasaka/docs/GLOSSARY.md)**: Definition of ecosystem terms.
4. **[CONTEXT.md](file:///Users/oussamaabid/Documents/projects/orasaka/docs/CONTEXT.md)**: Architectural Decision Records (ADR).

## Update Procedure
1. Scan changed files for documentation-impactful changes (signature changes, new configuration keys).
2. Propagate changes to `API_REFERENCE.md`.
3. If `orasaka-gateway` schemas change, update the "Gateway API" section in `API_REFERENCE.md`.
4. If new terminology is introduced, update `GLOSSARY.md`.
5. Reflect major architectural shifts in `CONTEXT.md`.
6. Ensure `![Orasaka Logo](docs/assets/logo.svg)` remains at the very top of `README.md`.