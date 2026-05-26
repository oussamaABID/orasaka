/**
 * @file init.enhanced.command.ts
 * @description Enhanced workspace initialization with comprehensive checks
 * and recovery strategies.
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import * as crypto from "node:crypto";
import chalk from "chalk";
import {
  intro,
  outro,
  confirm,
  select,
  text,
  note,
  logSuccess,
  logWarning,
  logError,
  logStep,
  logInfo,
  createSpinner,
  handleCancel,
  isCancel,
} from "../ui/prompts";
import {
  resolveWorkspaceRoot,
  resolveEnvFile,
  resolveComposeFile,
  writeEnvFile,
  hasTool,
  ensureDir,
  parseEnvFile,
} from "../ui/platform";
import { runDiagnostics } from "../services/diagnostics";

function secureHex(bytes: number): string {
  return crypto.randomBytes(bytes).toString("hex");
}

function getWorkspaceDirs(root: string): string[] {
  return [
    path.join(root, "var", "orasaka-uploads"),
    path.join(root, "var", "temp"),
    path.join(root, "var", "logs"),
    path.join(root, "infra", "local-db"),
  ];
}

function getDefaultComposeYml(): string {
  return `version: '3.8'
services:
  postgres:
    image: ankane/pgvector:latest
    container_name: orasaka-postgres
    environment:
      POSTGRES_DB: \${POSTGRES_DB:-orasaka_db}
      POSTGRES_USER: \${SPRING_DATASOURCE_USERNAME:-postgres}
      POSTGRES_PASSWORD: \${SPRING_DATASOURCE_PASSWORD:?SPRING_DATASOURCE_PASSWORD must be set}
    command: postgres -c shared_buffers=256MB -c max_connections=100
    ports:
      - "5432:5432"
    volumes:
      - pgvector_data:/var/lib/postgresql/data
      - ./infra/local-db:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \${SPRING_DATASOURCE_USERNAME:-postgres}"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - orasaka-network

  redis:
    image: redis:7-alpine
    container_name: orasaka-redis
    command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - orasaka-network

  rabbitmq:
    image: rabbitmq:3.14-management-alpine
    container_name: orasaka-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: \${RABBITMQ_USER:-guest}
      RABBITMQ_DEFAULT_PASS: \${RABBITMQ_PASSWORD:-guest}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - orasaka-network

  mcp-debug-server:
    image: node:24-slim
    container_name: orasaka-mcp-debug
    working_dir: /app
    command: node -e "require('http').createServer((_, r) => r.end('OK')).listen(8080)"
    ports:
      - "8084:8080"
    networks:
      - orasaka-network

networks:
  orasaka-network:
    driver: bridge

volumes:
  pgvector_data:
  redis_data:
  rabbitmq_data:
`;
}

function getDefaultEnvVars(): Record<string, string> {
  const year = new Date().getFullYear();
  return {
    // Database
    POSTGRES_DB: "orasaka_db",
    SPRING_DATASOURCE_USERNAME: "postgres",
    SPRING_DATASOURCE_PASSWORD: secureHex(16),
    SPRING_DATASOURCE_URL: "jdbc:postgresql://localhost:5432/orasaka_db",

    // Redis
    SPRING_REDIS_HOST: "localhost",
    SPRING_REDIS_PORT: "6379",

    // RabbitMQ
    SPRING_RABBITMQ_HOST: "localhost",
    SPRING_RABBITMQ_PORT: "5672",
    RABBITMQ_USER: "guest",
    RABBITMQ_PASSWORD: "guest",

    // Ollama
    SPRING_AI_OLLAMA_BASE_URL: "http://localhost:11434",
    OLLAMA_MODEL: "phi3:mini",

    // JWT & Security
    JWT_SECRET: secureHex(32),
    JWT_EXPIRATION_MS: "86400000",

    // Application
    APP_NAME: "Orasaka",
    APP_VERSION: "1.0.0",
    BUILD_YEAR: String(year),
    NODE_ENV: "development",
  };
}

export const initEnhancedCommand = new Command("init")
  .description("Initialize workspace with comprehensive validation and recovery")
  .option("-f, --force", "Overwrite existing configuration")
  .option("-y, --yes", "Non-interactive mode with defaults")
  .option("--skip-validation", "Skip diagnostic checks")
  .action(async (options: { force?: boolean; yes?: boolean; skipValidation?: boolean }) => {
    await intro(chalk.cyan.bold("🥷 Orasaka Workspace Initialization"));

    const workspaceRoot = resolveWorkspaceRoot();

    // ─── Step 1: Validate workspace ─────────────────────────
    if (!options.skipValidation) {
      const spinner = await createSpinner();
      spinner.start("Validating workspace...");

      const report = await runDiagnostics();
      spinner.stop();

      if (report.criticalCount > 0) {
        await logError(`Critical issues detected. Run ${chalk.cyan("npx orasaka doctor --verbose")} to fix them.`);
        process.exit(1);
      }

      if (report.warningCount > 0) {
        await logWarning(`${report.warningCount} warning(s) detected.`);
        const proceed = await confirm({
          message: "Continue initialization?",
          initialValue: true,
        });
        if (!proceed) {
          await outro(chalk.yellow("Initialization cancelled."));
          return;
        }
      }
    }

    // ─── Step 2: Check for existing configuration ──────────
    const envFile = resolveEnvFile();
    const composeFile = resolveComposeFile();

    if (fs.existsSync(envFile) && !options.force && !options.yes) {
      await logWarning(".env already exists");
      const overwrite = await confirm({
        message: "Overwrite existing configuration?",
        initialValue: false,
      });
      if (!overwrite) {
        await outro(chalk.cyan("Using existing configuration."));
        return;
      }
    }

    // ─── Step 3: Configure environment ─────────────────────
    let envVars = getDefaultEnvVars();

    if (!options.yes) {
      const configMode = await select({
        message: "Configuration mode:",
        options: [
          { value: "quick", label: "Quick Setup", hint: "Use defaults (recommended)" },
          { value: "custom", label: "Custom Setup", hint: "Configure each variable" },
        ],
        initialValue: "quick",
      });
      handleCancel(configMode);

      if (configMode === "custom") {
        // Customize database
        const dbPass = await text({
          message: "PostgreSQL password:",
          defaultValue: envVars.SPRING_DATASOURCE_PASSWORD,
        });
        handleCancel(dbPass);
        envVars.SPRING_DATASOURCE_PASSWORD = dbPass as string;

        // Customize Ollama endpoint
        const ollamaUrl = await text({
          message: "Ollama base URL:",
          placeholder: "http://localhost:11434",
          defaultValue: envVars.SPRING_AI_OLLAMA_BASE_URL,
        });
        handleCancel(ollamaUrl);
        envVars.SPRING_AI_OLLAMA_BASE_URL = ollamaUrl as string;

        // Customize Ollama model
        const model = await text({
          message: "Ollama model name:",
          placeholder: "phi3:mini",
          defaultValue: envVars.OLLAMA_MODEL,
        });
        handleCancel(model);
        envVars.OLLAMA_MODEL = model as string;
      }
    }

    // ─── Step 4: Create directories ─────────────────────────
    const spinner = await createSpinner();
    spinner.start("Creating workspace directories...");

    const dirs = getWorkspaceDirs(workspaceRoot);
    for (const dir of dirs) {
      ensureDir(dir);
    }

    spinner.stop("Directories created.");

    // ─── Step 5: Write .env file ────────────────────────────
    spinner.start("Writing environment configuration...");
    writeEnvFile(envFile, envVars);
    spinner.stop(".env file written.");

    // ─── Step 6: Write docker-compose.yml ──────────────────
    spinner.start("Writing Docker Compose configuration...");

    if (!fs.existsSync(composeFile) || options.force) {
      const composeDir = path.dirname(composeFile);
      ensureDir(composeDir);
      fs.writeFileSync(composeFile, getDefaultComposeYml(), "utf-8");
      spinner.stop("docker-compose.yml written.");
    } else {
      spinner.stop("docker-compose.yml already exists (skipped).");
    }

    // ─── Step 7: Verification ──────────────────────────────
    const envCheck = fs.existsSync(envFile);
    const composeCheck = fs.existsSync(composeFile);

    if (envCheck && composeCheck) {
      await logSuccess("✨ Workspace initialized successfully!");
      await note(
        [
          `${chalk.yellow(".env")}                 ${chalk.cyan(path.relative(workspaceRoot, envFile))}`,
          `${chalk.yellow("docker-compose.yml")}   ${chalk.cyan(path.relative(workspaceRoot, composeFile))}`,
          `${chalk.yellow("Directories")}          Created successfully`,
        ].join("\n"),
        "Configuration Summary"
      );

      // Next steps
      await note(
        [
          `1. Review configuration: ${chalk.cyan("cat .env")}`,
          `2. Start services: ${chalk.cyan("npx orasaka start")}`,
          `3. Verify health: ${chalk.cyan("npx orasaka doctor")}`,
          `4. Open UI: ${chalk.cyan("http://localhost:3000")}`,
        ].join("\n"),
        "Next Steps"
      );

      await outro(chalk.cyan("Happy coding! 🚀"));
    } else {
      await logError("Failed to write configuration files.");
      process.exit(1);
    }
  });
