---
description: CONTINUOUS DOCUMENTATION SYNCHRONIZATION
---

# ORASAKA WORKFLOW: CONTINUOUS DOCUMENTATION SYNCHRONIZATION

## 🔄 Synchronization Policy & Drift Enforcement
* **Trigger**: Any signature modification, entity layout shift, interface change, or configuration key addition under the `orasaka.core` namespace must immediately trigger a documentation compliance review.
* **Target Matrix Mapping**: Ensure absolute synchronization between code reality and the high-level documentation matrix. The target matrix consists of the following 6 files:
  1. `README.md` (Root level overview)
  2. `docs/ARCHITECTURE.md` (Core design decisions and package mappings)
  3. `docs/API_REFERENCE.md` (Public endpoint specifications)
  4. `docs/GLOSSARY.md` (Terminology, naming constraints, and concepts)
  5. `docs/CONTEXT.md` (Runtime environment variables and tool mappings)
  6. `docs/AUTH.md` (Authentification flow)
  7. `docs/BUSINESS_IMPLEMENTATION.md` (CinePulse AI business enabler guide)
* **Environment Variable Registry**: Any addition or modification of environment variables in `.env` or `orasaka-ui/.env.local` must immediately trigger an update of the "Environment Variables" section in `docs/GLOSSARY.md`. Each variable must be documented with its key, purpose, default value, and security classification.
* **Docstring Gate**: Missing Javadoc or TSDoc blocks on public components will automatically block compiling cycles.