# Orasaka CLI Reference

> Command-line interface (`npx orasaka`) for managing the Orasaka platform and local workers.

---

## 1. Setup & Quick Start

The CLI is included in the monorepo:
```bash
npx orasaka init      # Guided workspace setup
npx orasaka doctor    # Health diagnostic scan
npx orasaka start     # Spin up databases & local workers
npx orasaka stop      # Stop all services
npx orasaka chat      # Interactive local AI session
```

---

## 2. Command Reference

### Lifecycle Operations

#### `npx orasaka init`
Guided workspace configuration.
- Flags:
  - `-f, --force`: Overwrites existing `.env` files.
  - `-y, --yes`: Non-interactive mode, runs with default variables.

#### `npx orasaka doctor`
Performs environment checkups (CPU memory, toolchain version, port availability, model checks).
- Flags:
  - `-q, --quiet`: Print errors and warnings only.

#### `npx orasaka config`
Manages project environment values.
- Flags:
  - `-l, --list`: Print variables to console.
  - `-c, --category <name>`: Jumps directly to a configuration category.

#### `npx orasaka start`
Launches PostgreSQL (pgvector), Redis, RabbitMQ, Ollama, video workers, and Quartz integration service.
- Flags:
  - `--skip-health`: Bypasses service health checks.

#### `npx orasaka stop`
Gracefully stops services.
- Flags:
  - `--purge`: Deletes local databases, assets, uploads, and compose volumes.
  - `-y, --yes`: Skips confirmation prompts.

---

### User & AI Interaction Commands

- **Auth**:
  - `login [email] [password]`: Authenticates CLI and caches JWT locally (`~/.orasaka-cli.json`).
  - `register [user] [email]`: Creates local credentials profile.
  - `verify <token>` / `forgot [email]` / `reset [token] [password]`: Reset password workflows.
- **AI Commands**:
  - `chat [prompt]`: Interactive streaming chat.
  - `chat --gen-image <prompt>`: Single-turn text-to-image synthesis.
  - `chat --speech <text>`: Audio synthesis pipeline.
  - `chat --image <path>` / `chat --audio <path>`: Multi-modal file analysis.
  - `video <prompt>`: Async video generation task wrapper.
  - `graph`: Prints active SDUI capability grid.
  - `settings get` / `settings set <key> <val>`: Configure local preferences.

---

## 3. Platform Utilities (`src/ui/platform.ts`)

- `hasTool(name)`: Cross-platform command validation.
- `getSystemInfo()`: Core CPU/hardware diagnostics.
- `writeEnvFile(path, values)`: Saves key-value pairs back to `.env` file.

---

## Related Documentation
- [Developer Onboarding Guide](101.md)
- [Architecture Reference](ARCHITECTURE.md)
- [API Reference](API_REFERENCE.md)
- [Glossary](GLOSSARY.md)
