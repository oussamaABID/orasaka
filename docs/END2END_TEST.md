# 🧪 End-to-End (E2E) Testing Framework

Orasaka features a complete local bare-metal E2E testing harness using Python Playwright. This suite validates backend business logic, database transaction safety, REST/SSE APIs, GraphQL subscriptions, CLI commands, and visual UI theme accent applications under actual running servers with no mocks.

---

## 📁 Workspace Test Structure

All E2E validation scripts are co-located in the `scratch/` directory:

| Script Name | Target Module / System | What it Validates |
| :--- | :--- | :--- |
| `e2e_comprehensive.py` | Full Mesh | Comprehensive multi-phase flow: admin catalog CRUD, theme configuration, image/speech/video generation, vision analysis, audio transcription, RAG ingestion, MCP verification, CLI validation, and idempotent teardown. |
| `e2e_cli_test.py` | CLI Module | Validates Commander actions (`login`, `chat`, `--gen-image`, `--speech`, `settings set/get`, `profile`, `graph`) and M2M JWT generation. |
| `e2e_smoke_test.py` | Next.js UI | Basic user onboarding, login page, settings navigation, and chat canvas smoke validation. |
| `admin_crud_test.py` | Admin Console | Admin CRUD operations for managing the models catalog. |
| `ui_verification.py` | Next.js UI / Tenant | Multi-tenant dynamic branding, font sizes, and layout density visual controls. |

---

## 📸 Structured Screenshots & Visual Proofs

To prevent file clutter, all visual proofs captured during Playwright tests are saved under structured subdirectories:

*   **Primary storage**: `scratch/screenshots/` — flat + nested subdirectories per test phase.
*   **Auto-synced to docs**: Screenshots are automatically copied to `docs/assets/orasaka/` for documentation embedding.
*   **CLI outputs**: Saved under `scratch/cli/`.

---

## 🚀 Step-by-Step Test Execution

### 1. Pre-requisites & Local Servers

Before running the tests, ensure that:
1.  **Orasaka Gateway** is running on port `8080`.
2.  **Orasaka Next.js UI** is running on port `3000` (`npm run dev` in `orasaka-ui`).
3.  **Local AI Services** are active (Whisper/Piper on `8085`, SDXL on `8086`, SVD Python worker on `8188`).
4.  **Docker containers** are running (PostgreSQL, Redis, RabbitMQ).

### 2. Install Playwright Dependencies

Install the python automation dependencies locally:
```bash
pip install playwright Pillow
playwright install chromium
```

### 3. Run the Comprehensive E2E Script

> [!WARNING]
> **Default mode is destructive.** Without `--skip-bootstrap`, the script will kill all processes on ports 8080/3000, run `./stop` to tear down Docker, and restart everything from scratch. This can destabilize your machine.

**Recommended: Use `--skip-bootstrap` to test against already-running services:**
```bash
# Safe mode — uses existing servers (Gateway + UI must already be running)
python3 -u scratch/e2e_comprehensive.py --skip-bootstrap
```

**Full bootstrap mode (destructive — tears down and rebuilds everything):**
```bash
python3 -u scratch/e2e_comprehensive.py
```

### 4. Test Phases Executed

The comprehensive suite runs through the following phases in order:

| Phase | Name | Actions |
| :---: | :--- | :--- |
| 1 | **Admin Auth & Theme Config** | Admin credentials validation, theme catalog CRUD, accent selection, persistence |
| 2 | **Admin Model Catalog CRUD** | Dynamic model addition, label verification, screenshot capture |
| 3 | **Playground AI Pipelines** | Image (SDXL), TTS (Piper), Vision (Llama3.2), Audio Whisper, Video Analysis (keyframes), Video Generation (SVD XT) |
| 9 | **Timeout Circuit Breaker** | Hanging task timeout, EXECUTION_TIMEOUT_EXCEEDED verification |
| 7 | **Task History & Telemetry** | Job listing, GPU/inference metrics, speech duration assertions |
| 10 | **Chat Session Lifecycle** | Create → Rename → Persistence after reload → Delete → Empty placeholder |
| 11 | **Dynamic MCP Secure Routing** | Mock SSE server, UI registration, CLI `mcp list`, gateway log check |
| 8 | **Admin Catalog Cleanup** | Delete test model from catalog |
| 12 | **Feature-to-Code & CLI** | UI component request, CLI Next.js scaffold generation, disk assertions |
| 13 | **SIM-DAG Verification** | Intent split, backpressure mitigation, gateway log signatures |
| — | **Idempotent Teardown** | SSE close, Redis eviction, Active Sessions → 0 baseline |

### 5. Run CLI Integration Tests

Validates the native terminal commands and outputs results to `scratch/cli/`:
```bash
python3 scratch/e2e_cli_test.py
```

### 6. Verify Visual Application State Changes

Open the generated screenshots under `scratch/screenshots/` to confirm that:
- Theme modifications apply the correct Tailwind accent color class dynamically.
- The default model badges render correctly next to the active category defaults.
- Glassmorphism `glass-card` panels render with proper backdrop-filter blur.
- Generated AI assets (images, audio, video) are saved to `docs/assets/orasaka/`.

