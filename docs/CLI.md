# Orasaka CLI Reference (Enhanced v2)

> Command-line interface (`npx orasaka`) for managing the Orasaka platform, local workers, and adaptive on-premise topologies.

---

## 1. Setup & Quick Start

The CLI is included in the monorepo:
```bash
npx orasaka install   # Interactive installer wizard (hardware-adaptive topology)
npx orasaka init      # Guided workspace setup with validation
npx orasaka doctor    # Advanced health diagnostic scan with recovery suggestions
npx orasaka start     # Spin up databases, AI engines & workers
npx orasaka stop      # Stop all services gracefully
npx orasaka chat      # Interactive local AI session
npx orasaka generate  # Generate service templates & boilerplate
npx orasaka recover   # Diagnose and recover from common issues
```

---

## 2. Command Reference

### Lifecycle Operations

#### `npx orasaka install`
Interactive wizard to package and configure Orasaka's deployment topology.
- Prompts the operator:
  1. **Target Environment**:
     - `Local Dev (Apple Silicon ARM64)`: Configures GPU-accelerated Ollama (`http://host.docker.internal:11434`) with **Metal MPS** support for LocalAI image generation. Also manages LocalAI and video workers on ports 8085 & 8188.
     - `Production (Linux x86_64)`: Provisions standard containerized Ollama and middleware services.
  2. **Infrastructure Mode**:
     - `Bundled (provisioned via Docker)`: Standard local Docker containers for Postgres, Redis, RabbitMQ.
     - `External (connecting to enterprise clusters)`: Bypasses local middleware provisioning (BYO-Infra).
- Generates:
  - `.env` file containing context-aware endpoints, cryptographic keys, and worker ports.
  - `docker-compose.override.yml` containing the scaled-down service topology overlay.

#### `npx orasaka init`
Guided workspace configuration with comprehensive validation and recovery.
- Flags:
  - `-f, --force`: Overwrites existing `.env` files.
  - `-y, --yes`: Non-interactive mode, runs with default variables.
  - `--skip-validation`: Skip diagnostic checks before initialization.
- Validates:
  - Workspace structure integrity
  - Required tools availability (Node.js, Java, Python, Docker, Maven, Git)
  - Memory and storage requirements
  - Port availability for all services
  - LocalAI availability on macOS M1/M2/M3

#### `npx orasaka doctor`
Advanced system diagnostics with automatic recovery suggestions.
- Flags:
  - `-q, --quiet`: Print errors and warnings only.
  - `--fix`: Attempt to auto-fix detected issues.
  - `-v, --verbose`: Show detailed diagnostic output with solutions.
- Comprehensive Checks:
  - Node.js, Java 21+, Python 3.11+, Docker, Maven, Git versions
  - **Apple Silicon (M1/M2/M3)**: LocalAI availability for GPU-accelerated image generation (Metal MPS)
  - **Critical Ports**: PostgreSQL (5432), Redis (6379), RabbitMQ (5672), Ollama (11434), LocalAI (8085), Video Worker (8188)
  - System memory (8GB minimum recommended) and CPU specifications
  - Workspace structure validity
  - Existing configuration conflicts

#### `npx orasaka config`
Manages project environment values.
- Flags:
  - `-l, --list`: Print variables to console.
  - `-c, --category <name>`: Jumps directly to a configuration category.

#### `npx orasaka start`
Launches all infrastructure: databases, AI engines, image/video workers with intelligent recovery.
- Flags:
  - `--skip-health`: Bypasses service health verification.
  - `--wait-timeout <ms>`: Timeout for service startup (default: 60000ms).
  - `--allow-partial`: Allow partial startup if some services fail (non-critical workers only).
  - `--verbose`: Show detailed startup logs.
- **Five Phases**:
  1. **Docker Middleware** (Critical): PostgreSQL (pgvector), Redis, RabbitMQ with health checks
  2. **AI Engine** (Critical): Ollama LLM inference with model verification
  3. **Image Worker** (Critical for M1): LocalAI on port 8085 with Metal MPS GPU acceleration
  4. **Video Worker** (Critical for video generation): Python worker on port 8188
  5. **Health Summary**: Reports ready services and configuration status
- **Automatic Recovery**:
  - Detects port conflicts and proposes solutions
  - Retries failed service startups
  - Falls back to alternative launch methods (e.g., macOS app for Ollama)
  - Provides clear error messages and next steps

#### `npx orasaka stop`
Gracefully stops services.
- Flags:
  - `--purge`: Deletes local databases, assets, uploads, and compose volumes.
  - `-y, --yes`: Skips confirmation prompts.

---

### Development & Code Generation

#### `npx orasaka generate` (alias: `gen`, `scaffold`)
Generate production-ready service skeletons, endpoints, and boilerplate code.
- Interactive prompts for:
  1. **Template Type** (8 options):
     - `java-service`: Spring service with interface & package-private implementation
     - `java-controller`: REST @RestController with @RequestMapping
     - `typescript-hook`: React hook with API integration and error handling
     - `nextjs-action`: Server action for Next.js App Router with revalidation
     - `maven-pom`: Maven module structure with parent references
     - `docker-compose`: Containerized service definition with health checks
     - `sql-migration`: PostgreSQL migration with triggers and indexes
     - `graphql-resolver`: GraphQL resolver with NestJS decorators
  2. **Module Name**: Kebab-case identifier (validated, e.g., `user-auth`, `payment-processor`)
  3. **Description**: Brief module description for documentation

- **Output**:
  - Auto-formatted, production-ready templates
  - Complete JSDoc/Javadoc comments with author and dates
  - Package structure follows Orasaka conventions (ERR-102, ERR-103)
  - Next steps tailored to template type for proper integration

**Example**:
```bash
$ npx orasaka generate
? What would you like to generate? › Java Service & Implementation
? Enter module/feature name (kebab-case): user-auth
? Brief description: User authentication and session management

✨ Generated: src/commands/user-auth.service.ts
Next Steps:
  1. Place in: orasaka-framework/orasaka-core/src/main/java/.../service/
  2. Update pom.xml if adding dependencies
  3. Run tests: npm run test
```

---

### Recovery & Troubleshooting

#### `npx orasaka recover`
Guided recovery from common installation and startup issues.
- **Recovery Options**:
  1. **Full System Reset**: Wipe Docker volumes and restart clean (destructive)
  2. **Clean Docker Environment**: Remove orphaned containers and volumes
  3. **Reset Environment Variables**: Regenerate .env with secure defaults
  4. **Cleanup Process IDs**: Remove stale PID references and orphaned processes
  5. **Diagnose Port Conflicts**: Identify processes using critical ports (5432, 6379, 5672, 11434, 8085, 8188, 8080, 3000)
  6. **Clear Logs**: Wipe logs to free disk space
  7. **Full Diagnostic Report**: Run comprehensive system analysis with `doctor --verbose`

---

### User & AI Interaction Commands

- **Auth**:
  - `login [email] [password]`: Authenticate and cache JWT locally.
  - `register [user] [email]`: Create local credentials profile.
  - `verify <token>` / `forgot [email]` / `reset [token] [password]`: Password reset workflows.
- **AI Commands**:
  - `chat [prompt]`: Interactive streaming chat via Ollama.
  - `chat --image <path>`: Multi-modal image analysis.
  - `chat --audio <path>`: Audio transcription and analysis.
  - `video <prompt>`: Async video synthesis task (uses LocalAI models).
  - `graph`: Print active capability grid (SDUI).
  - `settings get` / `settings set <key> <val>`: Configure local preferences.

---

## 3. Environment Variables Reference

Key variables in `.env` (auto-generated by `init`):

```bash
# ─── Database (PostgreSQL with pgvector) ─────────────────
POSTGRES_DB=orasaka_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<auto-generated-secure-key>
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orasaka_db

# ─── Cache (Redis) ──────────────────────────────────────
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# ─── Queue (RabbitMQ) ──────────────────────────────────
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# ─── LLM Inference (Ollama) ────────────────────────────
SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=phi3:mini

# ─── Image Generation (LocalAI) — CRITICAL FOR M1 ──────
LOCALAI_PORT=8085
LOCALAI_MODELS_PATH=~/models/stable-diffusion

# ─── Video Synthesis ───────────────────────────────────
VIDEO_WORKER_PORT=8188
VIDEO_WORKER_PYTHON_PATH=/usr/local/bin/python3

# ─── Security ───────────────────────────────────────────
JWT_SECRET=<auto-generated-secure-key>
JWT_EXPIRATION_MS=86400000

# ─── Application ───────────────────────────────────────
APP_NAME=Orasaka
APP_VERSION=1.0.0
BUILD_YEAR=2026
NODE_ENV=development
```

---

## 4. Critical Dependencies for macOS M1/M2/M3

### LocalAI (Image Generation)

**Why it's critical**:
- Generates photorealistic images using Stable Diffusion 1.5
- Runs **locally** with **Metal MPS GPU acceleration** (much faster than CPU)
- Powers the `/api/v1/image/generate` endpoint
- Video generation depends on image models

**Installation**:
```bash
# Homebrew (recommended)
brew install localai

# Or download binary
https://github.com/go-skynet/LocalAI/releases

# Verify
local-ai --version
```

**Port Configuration**:
- Default: `8085` (configurable via `LOCALAI_PORT`)
- Must be available before `start` command completes
- Health check: `curl http://localhost:8085/v1/models`

**Without LocalAI**:
- `POST /api/v1/image/generate` → 503 Service Unavailable
- `POST /api/v1/video/generate` → 503 Service Unavailable
- LLM chat still works (via Ollama)

### Video Worker (Video Synthesis)

**Requirements**:
- Python 3.11+ with numpy, torch, PIL
- Depends on LocalAI for image models
- Uses FFmpeg for video encoding

**Port**: `8188` (configurable via `VIDEO_WORKER_PORT`)

---

## 5. Hardware-Adaptive Configuration

The system automatically optimizes for your hardware:

**Apple Silicon (M1/M2/M3)**:
- Ollama uses **Metal MPS** GPU acceleration
- LocalAI uses **Metal MPS** GPU acceleration for image generation
- Video worker leverages both for cinematic output
- Automatically configured by `install` and `init` commands

**Intel macOS**:
- CPU-based inference (slower, not GPU-optimized)
- Warning displayed by `doctor` command
- All features available but reduced performance

**Linux (x86_64)**:
- CUDA GPU support if available
- Standard CPU inference as fallback

---

## 6. Port Map & Services

| Port  | Service | Purpose | Required |
|-------|---------|---------|----------|
| 5432  | PostgreSQL | Database | ✓ Yes |
| 6379  | Redis | Cache & Sessions | ✓ Yes |
| 5672  | RabbitMQ | Message Queue | ✓ Yes |
| 11434 | Ollama | LLM Inference | ✓ Yes |
| 8085  | LocalAI | Image Generation | ✓ Yes (M1+) |
| 8188  | Video Worker | Video Synthesis | ✓ Yes (for video) |
| 8080  | Gateway API | Backend | ✓ Yes |
| 3000  | UI Frontend | Web Interface | ✓ Yes |

---

## 7. Sovereign Compliance Verification

The backend `/api/v1/compliance/health` endpoint verifies that all AI execution stays **local**:

**Sovereign-Ready** if:
- Ollama hostname resolves to: `localhost`, `127.0.0.1`, `::1`
- Or private subnet: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`
- Or Docker internal: `host.docker.internal`, container network aliases

**Non-Compliant** if:
- Any public IP address (prevents unintended cloud offloading)

---

## Related Documentation
- [Developer Onboarding Guide](101.md)
- [Architecture Reference](ARCHITECTURE.md)
- [API Reference](API_REFERENCE.md)
- [Glossary](GLOSSARY.md)
- [Models Guide](MODELS.md)
- [Deployment Guide](DEPLOY.md)
- [Authentication](AUTH.md)
- [Automation Tasks](AUTOMATION.md)
- [Core Integration](CORE.md)