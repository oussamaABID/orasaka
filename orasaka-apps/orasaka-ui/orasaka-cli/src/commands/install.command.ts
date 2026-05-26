/**
 * @file install.command.ts
 * @description Setup wizard with tool verification, installation guidance, and
 * environment topology configuration. Verifies all required tools are available,
 * sets up Python virtual environments for workers, and configures deployment
 * topology (local dev vs production, bundled vs external).
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import * as crypto from "node:crypto";
import chalk from "chalk";
import {
  intro,
  outro,
  select,
  text,
  note,
  confirm,
  createSpinner,
  logSuccess,
  logWarning,
  logError,
  logStep,
  logInfo,
  handleCancel,
} from "../ui/prompts";
import {
  resolveWorkspaceRoot,
  resolveEnvFile,
  writeEnvFile,
  ensureDir,
  isAppleSilicon,
  hasTool,
} from "../ui/platform";
import {
  detectAllTools,
  formatToolResult,
  installViaBrew,
  setupVideoWorkerVenv,
  resolveVideoWorkerPython,
  type ToolCheckResult,
} from "../services/tool-installer";

function secureHex(bytes: number): string {
  return crypto.randomBytes(bytes).toString("hex");
}

export const installCommand = new Command("install")
  .description("Setup wizard — verify tools, install dependencies, and configure deployment topology")
  .option("--check-only", "Only check tool availability without configuring")
  .option("-y, --yes", "Non-interactive mode — skip install prompts")
  .action(async (options: { checkOnly?: boolean; yes?: boolean }) => {
    const workspaceRoot = resolveWorkspaceRoot();
    await intro(chalk.cyan.bold("🥷 Orasaka Setup Wizard"));

    // ─── Phase 1: Tool Verification ─────────────────────────────
    await logStep("Phase 1 — Tool Verification");

    const report = detectAllTools();
    const lines: string[] = [];

    // Group by category
    const categoryLabels: Record<string, string> = {
      infra: "🐳 Infrastructure",
      runtime: "⚙️  Runtime",
      ai: "🤖 AI Engines",
      media: "🎬 Media Processing",
      build: "🔧 Build Tools",
    };

    const grouped = new Map<string, ToolCheckResult[]>();
    for (const r of report.results) {
      const cat = r.tool.category;
      if (!grouped.has(cat)) grouped.set(cat, []);
      grouped.get(cat)!.push(r);
    }

    for (const [category, results] of grouped) {
      lines.push(`${chalk.bold(categoryLabels[category] ?? category)}`);
      for (const r of results) {
        lines.push(`  ${formatToolResult(r)}`);
      }
      lines.push("");
    }

    await note(lines.join("\n"), "Tool Status Report");

    // Report summary
    if (report.allCriticalMet) {
      await logSuccess("All critical tools are available ✔");
    } else {
      await logError(
        `${String(report.missingCritical.length)} critical tool(s) missing — these must be installed before running Orasaka.`
      );
    }

    if (report.missingOptional.length > 0) {
      await logWarning(
        `${String(report.missingOptional.length)} optional tool(s) missing — some features may be unavailable.`
      );
    }

    // ─── Check-only mode stops here ─────────────────────────────
    if (options.checkOnly) {
      await outro(chalk.cyan("Tool check complete."));
      return;
    }

    // ─── Phase 2: Guided Installation ───────────────────────────
    const allMissing = [...report.missingCritical, ...report.missingOptional];

    if (allMissing.length > 0 && process.platform === "darwin" && hasTool("brew")) {
      await logStep("Phase 2 — Install Missing Tools via Homebrew");

      for (const missing of allMissing) {
        if (!missing.tool.brewFormula) continue;

        let shouldInstall = options.yes;
        if (!shouldInstall) {
          const answer = await confirm({
            message: `Install ${chalk.cyan(missing.tool.name)} via ${chalk.gray(`brew install ${missing.tool.brewFormula}`)}?`,
            initialValue: true,
          });
          shouldInstall = answer === true;
        }

        if (shouldInstall) {
          const spinner = await createSpinner();
          spinner.start(`Installing ${missing.tool.name}...`);
          const success = installViaBrew(missing.tool.brewFormula);
          if (success) {
            spinner.stop(`${missing.tool.name} installed successfully.`);
            await logSuccess(`✔ ${missing.tool.name} installed`);
          } else {
            spinner.stop(`${missing.tool.name} installation failed.`, 1);
            await logError(`Failed to install ${missing.tool.name}. Install manually: ${chalk.cyan(missing.installHint)}`);
          }
        }
      }
    } else if (allMissing.length > 0) {
      await logInfo("Install missing tools manually:");
      for (const m of allMissing) {
        console.log(`  ${chalk.yellow("→")} ${m.tool.name}: ${chalk.cyan(m.installHint)}`);
      }
    }

    // ─── Phase 3: Video Worker Python Environment ───────────────
    const videoReqFile = path.join(workspaceRoot, "orasaka-apps", "orasaka-workers", "video", "requirements.txt");
    if (fs.existsSync(videoReqFile) && hasTool("python3")) {
      await logStep("Phase 3 — Video Worker Python Environment");

      const venvPath = path.join(workspaceRoot, "orasaka-apps", "orasaka-workers", "video", ".venv");
      const venvExists = fs.existsSync(venvPath);

      if (venvExists) {
        await logSuccess("Video worker venv already exists ✔");
      } else {
        let shouldSetup = options.yes;
        if (!shouldSetup) {
          const answer = await confirm({
            message: "Set up Python virtual environment for the video worker?",
            initialValue: true,
          });
          shouldSetup = answer === true;
        }

        if (shouldSetup) {
          const spinner = await createSpinner();
          spinner.start("Creating Python venv and installing dependencies...");
          const result = setupVideoWorkerVenv(workspaceRoot);
          if (result.success) {
            spinner.stop("Video worker environment ready.");
            await logSuccess("✔ Python venv created with dependencies");
          } else {
            spinner.stop("Venv setup failed.", 1);
            await logWarning(`Could not set up venv: ${result.error}`);
          }
        }
      }
    }

    // ─── Phase 4: Deployment Topology Configuration ─────────────
    await logStep("Phase 4 — Deployment Topology Configuration");

    const targetEnv = await select({
      message: "Select target environment profile:",
      options: [
        {
          value: "local_dev",
          label: "Local Dev (Apple Silicon ARM64)",
          hint: "Bypasses in-Docker Ollama to leverage macOS Metal acceleration",
        },
        {
          value: "production",
          label: "Production (Linux x86_64)",
          hint: "Hardened, containerized standard setup",
        },
      ],
      initialValue: "local_dev",
    });
    handleCancel(targetEnv);

    const infraMode = await select({
      message: "Select infrastructure topology:",
      options: [
        {
          value: "bundled",
          label: "Bundled (provisioned via Docker)",
          hint: "Postgres, Redis, and RabbitMQ spin up as local containers",
        },
        {
          value: "external",
          label: "External (connecting to enterprise clusters)",
          hint: "Bypasses local middleware container provisioning (BYO-Infra)",
        },
      ],
      initialValue: "bundled",
    });
    handleCancel(infraMode);

    // Collect external endpoints if needed
    const externalEndpoints: {
      dbHost?: string;
      redisHost?: string;
      rabbitmqHost?: string;
      ollamaHost?: string;
    } = {};

    if (infraMode === "external") {
      const dbHost = await text({
        message: "Enter external PostgreSQL hostname/IP:",
        placeholder: "e.g., pg-cluster.corp.internal",
        defaultValue: "external-db-host",
      });
      handleCancel(dbHost);
      externalEndpoints.dbHost = dbHost as string;

      const redisHost = await text({
        message: "Enter external Redis hostname/IP:",
        placeholder: "e.g., redis-cluster.corp.internal",
        defaultValue: "external-redis-host",
      });
      handleCancel(redisHost);
      externalEndpoints.redisHost = redisHost as string;

      const rabbitmqHost = await text({
        message: "Enter external RabbitMQ hostname/IP:",
        placeholder: "e.g., mq-cluster.corp.internal",
        defaultValue: "external-rabbitmq-host",
      });
      handleCancel(rabbitmqHost);
      externalEndpoints.rabbitmqHost = rabbitmqHost as string;

      if (targetEnv !== "local_dev") {
        const ollamaHost = await text({
          message: "Enter external Ollama hostname/IP:",
          placeholder: "e.g., ollama-server.corp.internal",
          defaultValue: "external-ollama-host",
        });
        handleCancel(ollamaHost);
        externalEndpoints.ollamaHost = ollamaHost as string;
      }
    }

    // ─── Generate artifacts ─────────────────────────────────────
    const spinner = await createSpinner();
    spinner.start("Generating deployment artifacts...");

    // Write/update .env
    const envPath = resolveEnvFile();
    if (!fs.existsSync(envPath)) {
      // Use template or generate defaults
      const templatePaths = [
        path.join(workspaceRoot, "exemple.env.txt"),
        path.join(workspaceRoot, ".env.example"),
      ];
      let templateContent = "";
      for (const tp of templatePaths) {
        if (fs.existsSync(tp)) {
          templateContent = fs.readFileSync(tp, "utf-8");
          break;
        }
      }
      if (!templateContent) {
        templateContent = [
          "PORT=8080",
          `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orasaka_db`,
          `SPRING_DATASOURCE_USERNAME=orasaka_admin`,
          `SPRING_DATASOURCE_PASSWORD=${secureHex(16)}`,
          `REDIS_URL=redis://localhost:6379`,
          `SPRING_RABBITMQ_HOST=localhost`,
          `SPRING_RABBITMQ_PORT=5672`,
          `DEFAULT_PROVIDER=ollama`,
          `SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434`,
          `OLLAMA_BASE_URL=http://localhost:11434`,
          `CRYPTO_KEY=${secureHex(16)}`,
          `CRYPTO_SALT=${secureHex(8)}`,
          `VIDEO_WORKER_PORT=8188`,
          `IMAGE_WORKER_PORT=8085`,
          `LOCALAI_PORT=8085`,
        ].join("\n");
      }
      fs.writeFileSync(envPath, templateContent, "utf-8");
    }

    // Apply topology-specific overrides
    const envUpdates: Record<string, string> = {
      DEFAULT_PROVIDER: "ollama",
    };

    if (targetEnv === "local_dev") {
      envUpdates["SPRING_AI_OLLAMA_BASE_URL"] = "http://host.docker.internal:11434";
      envUpdates["OLLAMA_BASE_URL"] = "http://host.docker.internal:11434";
    } else {
      if (infraMode === "external" && externalEndpoints.ollamaHost) {
        envUpdates["SPRING_AI_OLLAMA_BASE_URL"] = `http://${externalEndpoints.ollamaHost}:11434`;
        envUpdates["OLLAMA_BASE_URL"] = `http://${externalEndpoints.ollamaHost}:11434`;
      } else {
        envUpdates["SPRING_AI_OLLAMA_BASE_URL"] = "http://ollama:11434";
        envUpdates["OLLAMA_BASE_URL"] = "http://ollama:11434";
      }
    }

    if (infraMode === "external") {
      const dbH = externalEndpoints.dbHost || "external-db-host";
      const redisH = externalEndpoints.redisHost || "external-redis-host";
      const rabbitmqH = externalEndpoints.rabbitmqHost || "external-rabbitmq-host";
      envUpdates["SPRING_DATASOURCE_URL"] = `jdbc:postgresql://${dbH}:5432/orasaka_db`;
      envUpdates["REDIS_URL"] = `redis://${redisH}:6379`;
      envUpdates["SPRING_RABBITMQ_HOST"] = rabbitmqH;
    }

    // Detect video worker python
    const videoPython = resolveVideoWorkerPython(workspaceRoot);
    if (videoPython) {
      envUpdates["VIDEO_WORKER_PYTHON_PATH"] = videoPython;
    }

    writeEnvFile(envPath, envUpdates);

    // Generate docker-compose.override.yml
    const overrideLines: string[] = [
      "# ==============================================================================",
      "# ORASAKA — Docker Compose Override (Dynamic Configuration)",
      "# Generated by: npx orasaka install",
      `# Environment: ${targetEnv === "local_dev" ? "Local Dev (Apple Silicon)" : "Production (Linux)"}`,
      `# Infrastructure: ${infraMode === "bundled" ? "Bundled (Docker)" : "External Clusters"}`,
      "# ==============================================================================",
      "version: '3.8'",
      "services:",
    ];

    if (targetEnv === "local_dev") {
      overrideLines.push(
        "  ollama:",
        "    scale: 0",
        "",
        "  gateway:",
        "    environment:",
        "      - SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434",
        "    extra_hosts:",
        "      - \"host.docker.internal:host-gateway\"",
        ""
      );
    }

    if (infraMode === "external") {
      overrideLines.push(
        "  db-vector:",
        "    scale: 0",
        "",
        "  redis:",
        "    scale: 0",
        "",
        "  rabbitmq:",
        "    scale: 0",
        ""
      );

      if (targetEnv !== "local_dev") {
        overrideLines.push("  ollama:", "    scale: 0", "");
      }

      const dbH = externalEndpoints.dbHost || "external-db-host";
      const redisH = externalEndpoints.redisHost || "external-redis-host";
      const rabbitmqH = externalEndpoints.rabbitmqHost || "external-rabbitmq-host";

      overrideLines.push(
        "  gateway:",
        "    environment:",
        `      - SPRING_DATASOURCE_URL=jdbc:postgresql://${dbH}:5432/\${DB_NAME:-orasaka_db}`,
        `      - REDIS_URL=redis://${redisH}:6379`,
        `      - SPRING_RABBITMQ_HOST=${rabbitmqH}`
      );

      if (targetEnv !== "local_dev") {
        const ollamaH = externalEndpoints.ollamaHost || "external-ollama-host";
        overrideLines.push(`      - SPRING_AI_OLLAMA_BASE_URL=http://${ollamaH}:11434`);
      } else {
        overrideLines.push(
          "      - SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434",
          "    extra_hosts:",
          "      - \"host.docker.internal:host-gateway\""
        );
      }
      overrideLines.push("");
    }

    const infraOverridePath = path.join(workspaceRoot, "infra", "docker-compose.override.yml");
    ensureDir(path.dirname(infraOverridePath));
    fs.writeFileSync(infraOverridePath, overrideLines.join("\n"), "utf-8");

    spinner.stop("Deployment artifacts generated ✔");

    // ─── Summary ────────────────────────────────────────────────
    const summary = [
      `${chalk.green("✔")} Target: ${chalk.cyan(targetEnv === "local_dev" ? "Local Dev (Apple Silicon)" : "Production (Linux)")}`,
      `${chalk.green("✔")} Infra:  ${chalk.cyan(infraMode === "bundled" ? "Bundled (Docker)" : "External Clusters")}`,
      `${chalk.green("✔")} Generated: docker-compose.override.yml`,
      `${chalk.green("✔")} Configured: .env`,
    ];

    if (videoPython) {
      summary.push(`${chalk.green("✔")} Video Worker Python: ${chalk.gray(videoPython)}`);
    }

    if (isAppleSilicon()) {
      summary.push(`${chalk.green("✔")} Apple Silicon Metal MPS: enabled`);
    }

    summary.push(
      "",
      `${chalk.cyan("Next steps:")}`,
      `  ${chalk.white("1.")} ${chalk.cyan("npx orasaka start --mode dev")}  — Launch middleware + AI engines`,
      `  ${chalk.white("2.")} Start Gateway from ${chalk.cyan("IntelliJ")} (debug)`,
      `  ${chalk.white("3.")} Run ${chalk.cyan("npm run dev")} in orasaka-ui/`,
    );

    await note(summary.join("\n"), "✨ Setup Complete");
    await outro(chalk.green("Orasaka ready. Happy building! 🚀"));
  });
