# Orasaka CLI Reference

> **`npx orasaka`** — the command-line control plane for the Orasaka sovereign AI platform. Initialize, orchestrate, diagnose, and scaffold your entire multi-modal stack from a single terminal.

---

## The Developer Experience

One command launches your entire sovereign ecosystem:

```bash
npx orasaka dev
```

This spawns **four parallel processes** — a Java 21 Spring Boot Gateway, a Next.js 16 web client, a SecOps admin console, and an Expo mobile app — streaming color-coded output to your terminal with graceful `Ctrl+C` shutdown.

| Prefix | Service | Default Port | Tech |
|:-------|:--------|:------------:|:-----|
| `CORE` | Spring Boot Gateway (`-Dspring.profiles.active=e2e`) | 8080 | Java 21, Virtual Threads |
| `WEB-CLIENT` | Next.js 16 client application | 3000 | React 19, App Router |
| `WEB-ADMIN` | SecOps Administration Console | 3001 | Next.js 16, isolated |
| `MOBILE` | Expo SDK 53 development server | 8081 | React Native, 6-screen stack |

### Selective Launch

The `--skip-*` flags are engineering flexibility tools, not afterthoughts:

```bash
# Debug the Gateway in IntelliJ — let the CLI handle everything else
npx orasaka dev --skip-core

# Frontend-only iteration — skip the mobile server
npx orasaka dev --skip-mobile

# Backend + web only — no admin, no mobile
npx orasaka dev --skip-admin --skip-mobile
```

| Flag | Effect |
|:-----|:-------|
| `--skip-core` | Don't spawn the Java Gateway — debug it in your IDE instead |
| `--skip-web` | Don't spawn the Next.js web client |
| `--skip-admin` | Don't spawn the Admin Console |
| `--skip-mobile` | Don't spawn the Expo development server |

---

## Quick Start

```bash
# 1. Interactive setup — detects hardware, configures topology, generates .env
npx orasaka install

# 2. Launch infrastructure (Postgres, Redis, RabbitMQ, Ollama, LocalAI)
npx orasaka start

# 3. Launch the full development stack
npx orasaka dev

# 4. Monitor service health
npx orasaka status
```

### Recommended Development Flow

```bash
# First time setup (interactive wizard)
npx orasaka install               # Verify tools, configure topology, generate .env

# Daily development
npx orasaka start --mode dev      # Start middleware + AI engines
npx orasaka dev --skip-core --skip-mobile  # Launch web client + admin

# Or run services individually
# Gateway:    ./mvnw spring-boot:run -pl orasaka-apps/orasaka-gateway -Dspring-boot.run.profiles=e2e
# Web Client: cd orasaka-apps/orasaka-ui/orasaka-web-client && npm run dev
# Admin:      cd orasaka-apps/orasaka-ui/orasaka-web-admin && npm run dev
# Mobile:     cd orasaka-apps/orasaka-ui/orasaka-mobile-client && npx expo start

# Monitoring
npx orasaka status                # Service health dashboard (UP/DOWN per port)
npx orasaka logs --service gateway # Tail specific service logs
```

### npm Script Shortcuts

From `orasaka-apps/orasaka-ui/orasaka-cli/`:

| Script | Command | What it does |
|:-------|:--------|:-------------|
| `npm run check` | `install --check-only` | Verify tools are installed (read-only) |
| `npm run status` | `status` | Check which services are running (UP/DOWN) |
| `npm run setup` | `install` | Full wizard: tools + topology + .env generation |
| `npm run start:dev` | `dev --skip-core --skip-mobile` | Launch WEB-CLIENT + WEB-ADMIN |

---

## Command Reference

### Lifecycle

#### `npx orasaka init`

Smart workspace initialization with project detection.

- **Workspace Detection**: Scans for `pom.xml`, `orasaka-framework/`, `orasaka-apps/`, `AGENTS.md`.
- **Non-destructive .env merge**: Appends missing keys without overwriting existing values.

| Flag | Effect |
|:-----|:-------|
| `-f, --force` | Overwrite existing `.env` files |
| `-y, --yes` | Non-interactive mode with defaults |
| `--skip-validation` | Skip diagnostic checks |
| `--dir <path>` | Specify target directory |

#### `npx orasaka install`

Full setup wizard — verifies tools, installs dependencies, configures deployment topology.

| Phase | Action |
|:------|:-------|
| **1. Tool Verification** | Docker, Java 21, Node.js, Ollama, Python 3, FFmpeg, Git, LocalAI |
| **2. Guided Installation** | `brew install` for missing tools on macOS (user confirms) |
| **3. Video Worker Venv** | Python virtual environment in `orasaka-workers/video/.venv/` |
| **4. Topology Configuration** | Local Dev vs Production, Bundled vs External infrastructure |

| Flag | Effect |
|:-----|:-------|
| `--check-only` | Only check tool availability, skip configuration |
| `-y, --yes` | Non-interactive mode |

Generates: `.env` with context-aware endpoints + `docker-compose.override.yml` with topology overlay.

#### `npx orasaka start`

Launch infrastructure with dev/full mode support.

| Flag | Effect |
|:-----|:-------|
| `--mode <mode>` | `dev` (default) or `full` |
| `--only <service>` | Start specific service: `postgres`, `redis`, `rabbitmq`, `ollama`, `localai`, `video-worker` |
| `--skip-health` | Bypass service health verification |
| `--wait-timeout <ms>` | Startup timeout (default: 60000ms) |
| `--allow-partial` | Allow partial startup if some services fail |
| `--verbose` | Show detailed startup logs |
| `--logs` | Tail logs after startup |

**Dev Mode** (default): Starts middleware (Postgres, Redis, RabbitMQ), AI engines (Ollama, LocalAI), and workers. You manage Gateway and UI yourself or via `npx orasaka dev`.

**Full Mode** (`--mode full`): Starts everything including Gateway (from JAR) and the Next.js UI. Requires `mvn clean package -DskipTests` first.

#### `npx orasaka stop`

Graceful shutdown.

| Flag | Effect |
|:-----|:-------|
| `--purge` | Delete local databases, assets, uploads, and compose volumes |
| `-y, --yes` | Skip confirmation prompts |

#### `npx orasaka doctor`

Advanced system diagnostics with automatic recovery suggestions.

| Flag | Effect |
|:-----|:-------|
| `-q, --quiet` | Errors and warnings only |
| `--fix` | Auto-fix detected issues |
| `-v, --verbose` | Detailed diagnostics with solutions |

Checks: Node.js, Java 21+, Python 3.11+, Docker, Maven, Git, Apple Silicon detection, port availability, memory, workspace structure.

---

### Observability

#### `npx orasaka status`

Live dashboard — service health, ports, PIDs, grouped by category (Middleware, AI Engines, Workers, Applications).

| Flag | Effect |
|:-----|:-------|
| `--json` | Output as JSON for scripting |
| `--watch` | Auto-refresh every 5 seconds |

#### `npx orasaka logs`

Real-time log streaming with multiplexed color-coded output.

| Flag | Effect |
|:-----|:-------|
| `--service <name>` | Filter by: `gateway`, `ollama`, `video-worker`, `image-worker`, `ui` |
| `--list` | List all available log files with sizes and ages |
| `--since <duration>` | Show logs from last N hours (e.g., `1h`, `30m`, `2d`) |
| `--tail <lines>` | Lines from end (default: 50) |

---

### Code Generation

#### `npx orasaka generate` (aliases: `gen`, `scaffold`)

Smart code generator with workspace auto-detection and a full manifest of all files created.

| Template | Files Generated |
|:---------|:----------------|
| 📦 Business Feature | Service + ServiceImpl + Controller + React Hook + BFF Route |
| ⚙️ Technical Feature | Properties record + Configuration class + Tests |
| 🔗 Interceptor | Maven submodule + Interceptor + AutoConfig + SPI imports + Test |
| 🌐 API Connector | Client port + RestClient adapter + Properties + Test |
| 🔧 Configuration | `.env` updates + example env + Properties class |
| ☕ Java Service | Interface + Implementation |
| 🎯 REST Controller | Controller in `gateway/adapter/rest/` |
| ⚛️ React Hook | Hook with API integration |
| 🗃️ SQL Migration | Auto-numbered SQL in `infra/local-db/` |
| 🐳 Docker Service | Appended to `docker-compose.yml` |

**Example** — scaffolding a new interceptor:

```bash
$ npx orasaka generate
? What would you like to generate? › 🔗 Interceptor
? Enter feature/module name: sentiment-analysis
? Brief description: Analyzes user sentiment before routing

📋 File Manifest:
  ✚ [NEW] orasaka-interceptors/orasaka-interceptor-sentiment-analysis/pom.xml
  ✚ [NEW] .../SentimentAnalysisInterceptor.java
  ✚ [NEW] .../SentimentAnalysisAutoConfiguration.java
  ✚ [NEW] .../META-INF/spring/...AutoConfiguration.imports
  ✚ [NEW] .../SentimentAnalysisInterceptorTest.java

🚀 Next Steps:
  1. Add <module> to orasaka-interceptors/pom.xml
  2. Add dependency to orasaka-gateway/pom.xml
  3. Set correct order in getOrder() per context-matrix table
```

---

### Recovery & Troubleshooting

#### `npx orasaka recover`

Guided recovery with 11 options:

| Option | Description | Destructive? |
|:-------|:-----------|:---:|
| 🔄 Restart Service | Restart a specific service | No |
| 🔌 Fix Port Conflicts | Auto-detect and kill blocking processes | No |
| 📋 Validate .env | Check completeness, add missing keys | No |
| 🐍 Recreate Video Venv | Delete and recreate Python virtualenv | Yes |
| 🎨 Reinstall LocalAI | Reinstall via Homebrew (macOS) | Yes |
| 🐳 Clean Docker | Remove orphaned containers/volumes | Yes |
| 🔑 Reset Env Vars | Regenerate `.env` with secure defaults | Yes |
| 💀 Cleanup PIDs | Remove stale PID references | No |
| 🗑️ Clear Logs | Wipe previous logs | Yes |
| 💣 Full System Reset | Wipe databases and restart clean | **Destructive** |
| 🔍 Full Diagnostic | Run comprehensive system analysis | No |

---

### User & AI Interaction

| Command | Purpose |
|:--------|:--------|
| `login [email] [password]` | Authenticate and cache JWT locally |
| `register [user] [email]` | Create local credentials profile |
| `verify <token>` | Verify account token |
| `forgot [email]` | Request password reset |
| `reset [token] [password]` | Reset password |
| `chat [prompt]` | Interactive streaming chat via Ollama |
| `chat --image <path>` | Multi-modal image analysis |
| `chat --audio <path>` | Audio transcription and analysis |
| `video <prompt>` | Async video synthesis task |
| `graph` | Print active capability grid (SDUI) |
| `settings get / set <key> <val>` | Configure local preferences |

---

## Environment Variables

Key variables in `.env` (auto-generated by `npx orasaka init`):

```bash
# ─── Database (PostgreSQL + pgvector) ─────────────────────
POSTGRES_DB=orasaka_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<auto-generated-secure-key>
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orasaka_db

# ─── Cache (Redis) ─────────────────────────────────────────
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# ─── Queue (RabbitMQ) ──────────────────────────────────────
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# ─── LLM Inference (Ollama) ────────────────────────────────
SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=phi3:mini

# ─── Image Generation (LocalAI) ────────────────────────────
LOCALAI_PORT=8085
LOCALAI_MODELS_PATH=~/models/stable-diffusion

# ─── Video Synthesis ───────────────────────────────────────
VIDEO_WORKER_PORT=8188
VIDEO_WORKER_PYTHON_PATH=<auto-detected>

# ─── Security ──────────────────────────────────────────────
JWT_SECRET=<auto-generated-secure-key>
JWT_EXPIRATION_MS=86400000

# ─── Application ───────────────────────────────────────────
APP_NAME=Orasaka
APP_VERSION=1.0.0
NODE_ENV=development
```

---

## Port Map

| Port | Service | Purpose | Required |
|:-----|:--------|:--------|:--------:|
| 5432 | PostgreSQL | Database | ✓ |
| 6379 | Redis | Cache & Sessions | ✓ |
| 5672 | RabbitMQ | Message Queue | ✓ |
| 15672 | RabbitMQ Mgmt | Admin UI | Optional |
| 11434 | Ollama | LLM Inference | ✓ |
| 8085 | LocalAI | Image Generation | ✓ (Apple Silicon) |
| 8188 | Video Worker | Video Synthesis | ✓ (for video) |
| 8080 | Gateway API | Backend BFF | ✓ |
| 3000 | Web Client | Next.js UI | ✓ |
| 3001 | Web Admin | SecOps Console | Optional |
| 8081 | Mobile | Expo Dev Server | Optional |

---

## Hardware-Adaptive Configuration

The system automatically optimizes for your hardware:

| Platform | GPU Acceleration | Notes |
|:---------|:----------------|:------|
| **Apple Silicon (M1–M4)** | Metal MPS for Ollama, LocalAI, and video worker | Auto-configured by `install` and `init` |
| **Intel macOS** | CPU-only (warning displayed by `doctor`) | All features available, reduced performance |
| **Linux (x86_64)** | CUDA GPU if available, CPU fallback | Production-ready |

---

## Sovereign Compliance Verification

The backend `/api/v1/compliance/health` endpoint verifies that all AI execution stays **local**:

**Sovereign-Ready** if Ollama resolves to:
- `localhost`, `127.0.0.1`, `::1`
- Private subnets: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`
- Docker internal: `host.docker.internal`, container network aliases

**Non-Compliant** if any public IP address is detected — prevents unintended cloud offloading.

---

## Related Documentation

| Document | Purpose |
|:---------|:--------|
| [Developer Onboarding (101)](101.md) | Core concepts, architecture map, getting started |
| [Architecture Reference](ARCHITECTURE.md) | System topology, BFF schemas, execution flows |
| [API Reference](API_REFERENCE.md) | Endpoints, parameters, schemas, RBAC |
| [Auth & Security](AUTH.md) | Authentication flows |
| [Model Catalog](MODELS.md) | Tested models across all modalities |
| [Deployment Guide](DEPLOY.md) | Production deployment on AWS, RunPod, Modal |
| [Glossary](GLOSSARY.md) | Environment variables, terms, naming conventions |
| [Core Deep-Dive](CORE.md) | Engine pipeline and interceptor chain |
| [Automation & Workers](AUTOMATION.md) | Background workers, Quartz, Agent Protocol |