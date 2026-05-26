/**
 * @file start.enhanced.command.ts
 * @description Enhanced service startup with dev/full mode, selective service
 * launching, and intelligent recovery strategies.
 *
 * Dev mode:  Starts only Docker middleware + AI engines (user runs Gateway from IDE)
 * Full mode: Starts everything including Gateway and Next.js
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import { execSync, spawn } from "node:child_process";
import chalk from "chalk";
import { format } from "date-fns";
import * as dotenv from "dotenv";
import {
  intro,
  outro,
  logSuccess,
  logWarning,
  logError,
  logStep,
  logInfo,
  createSpinner,
  note,
} from "../ui/prompts";
import {
  resolveWorkspaceRoot,
  resolveComposeFile,
  resolveLogDir,
  resolvePidFile,
  resolveEnvFile,
  resolveModelsDir,
  resolveDockerComposeCmd,
  hasTool,
  ensureDir,
} from "../ui/platform";
import { checkPortAvailable } from "../services/diagnostics";
import { resolveVideoWorkerPython } from "../services/tool-installer";

const envFilePath = resolveEnvFile();
if (fs.existsSync(envFilePath)) {
  dotenv.config({ path: envFilePath, quiet: true });
}

async function checkServiceHealth(port: number): Promise<boolean> {
  const available = await checkPortAvailable(port);
  return !available; // If port NOT available, service is running
}

async function waitForService(port: number, maxRetries: number = 30): Promise<boolean> {
  for (let i = 0; i < maxRetries; i++) {
    if (await checkServiceHealth(port)) return true;
    await new Promise((r) => setTimeout(r, 1000));
  }
  return false;
}

/** Services that can be started individually. */
type ServiceName = "postgres" | "redis" | "rabbitmq" | "ollama" | "localai" | "video-worker" | "gateway" | "ui";

export const startEnhancedCommand = new Command("start")
  .description("Start Orasaka infrastructure — supports dev mode (middleware only) and full mode")
  .option("--mode <mode>", "Startup mode: dev (default) or full", "dev")
  .option("--only <service>", "Start only a specific service (postgres, redis, rabbitmq, ollama, localai, video-worker)")
  .option("--skip-health", "Skip final health verification")
  .option("--wait-timeout <ms>", "Timeout for service startup (default: 60000ms)", "60000")
  .option("--allow-partial", "Allow partial startup (some services may fail)")
  .option("--verbose", "Show detailed startup logs")
  .option("--logs", "Tail log files after startup")
  .action(async (options: {
    mode?: string;
    only?: string;
    skipHealth?: boolean;
    waitTimeout?: string;
    allowPartial?: boolean;
    verbose?: boolean;
    logs?: boolean;
  }) => {
    const mode = (options.mode ?? "dev") as "dev" | "full";
    const onlyService = options.only as ServiceName | undefined;

    await intro(chalk.cyan.bold(
      mode === "dev"
        ? "🥷 Orasaka Dev Infrastructure"
        : "🥷 Orasaka Full Infrastructure Bootstrap"
    ));

    const workspaceRoot = resolveWorkspaceRoot();
    const pidFile = resolvePidFile();
    const logDir = resolveLogDir();
    ensureDir(logDir);
    ensureDir(path.dirname(pidFile));

    // Verify compose file
    const composeFile = resolveComposeFile();
    if (!fs.existsSync(composeFile)) {
      await logError(
        "docker-compose.yml not found.\n" +
        `  Run ${chalk.cyan("npx orasaka init")} to generate it.`
      );
      process.exit(1);
    }

    const dockerComposeCmd = resolveDockerComposeCmd();
    if (!dockerComposeCmd) {
      await logError(
        "Docker Compose not found.\n" +
        `  Install Docker Desktop: ${chalk.cyan("https://www.docker.com/products/docker-desktop")}`
      );
      process.exit(1);
    }

    const dateStamp = format(new Date(), "yyyyMMdd_HHmmss");
    const startedServices: string[] = [];
    const spinner = await createSpinner();

    // Helper: should we start this service?
    const shouldStart = (service: ServiceName): boolean => {
      if (onlyService) return onlyService === service;
      return true;
    };

    // ═══════════════════════════════════════════════════════════
    // PHASE 1: Docker Middleware (Postgres, Redis, RabbitMQ)
    // ═══════════════════════════════════════════════════════════
    if (shouldStart("postgres") || shouldStart("redis") || shouldStart("rabbitmq")) {
      await logStep("Phase 1: Docker Services (Postgres, Redis, RabbitMQ)");

      const servicesToStart: string[] = [];
      if (shouldStart("postgres")) servicesToStart.push("postgres");
      if (shouldStart("redis")) servicesToStart.push("redis");
      if (shouldStart("rabbitmq")) servicesToStart.push("rabbitmq");

      // Check if any compose service names differ (handle both naming conventions)
      const composeContent = fs.readFileSync(composeFile, "utf-8");
      const actualServices: string[] = [];
      for (const svc of servicesToStart) {
        // Check for alternative names in compose file
        if (svc === "postgres") {
          if (composeContent.includes("db-vector:")) actualServices.push("db-vector");
          else if (composeContent.includes("postgres:")) actualServices.push("postgres");
          else actualServices.push(svc);
        } else {
          actualServices.push(svc);
        }
      }

      spinner.start("Launching Docker containers...");
      try {
        execSync(
          `${dockerComposeCmd} -p orasaka -f "${composeFile}" up -d ${actualServices.join(" ")}`,
          { stdio: options.verbose ? "inherit" : "ignore" }
        );
        spinner.stop("Docker containers launched.");
      } catch (err) {
        spinner.stop("Failed to launch Docker containers", 1);
        if (!options.allowPartial) {
          const msg = err instanceof Error ? err.message : "Unknown error";
          await logError(`Docker error: ${msg}`);
          process.exit(1);
        }
        await logWarning("Continuing with partial startup...");
      }

      // Health checks
      spinner.start("Verifying Docker services...");
      const dockerChecks = [
        { port: 5432, name: "PostgreSQL", service: "postgres" as ServiceName },
        { port: 6379, name: "Redis", service: "redis" as ServiceName },
        { port: 5672, name: "RabbitMQ", service: "rabbitmq" as ServiceName },
      ];

      for (const check of dockerChecks) {
        if (!shouldStart(check.service)) continue;
        const healthy = await waitForService(check.port, 30);
        if (healthy) {
          await logSuccess(`✔ ${check.name} healthy (port ${String(check.port)})`);
          startedServices.push(check.name);
        } else {
          await logWarning(`⚠ ${check.name} not responding`);
          if (!options.allowPartial) process.exit(1);
        }
      }
      spinner.stop();
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 2: Ollama LLM Engine
    // ═══════════════════════════════════════════════════════════
    if (shouldStart("ollama")) {
      await logStep("Phase 2: AI Engine (Ollama)");

      const ollamaActive = await checkServiceHealth(11434);
      if (ollamaActive) {
        await logSuccess("✔ Ollama already active");
        startedServices.push("Ollama");
      } else {
        await logWarning("Ollama not active on port 11434.");
        let started = false;

        // Try macOS app first
        if (process.platform === "darwin") {
          try {
            execSync("open -a Ollama", { stdio: "ignore" });
            await logStep("Launched Ollama.app...");
            started = true;
          } catch { /* app not installed */ }
        }

        // Try CLI
        if (!started && hasTool("ollama")) {
          const ollamaLog = fs.createWriteStream(
            path.join(logDir, `${dateStamp}_ollama.log`),
            { flags: "a" },
          );
          const ollamaProcess = spawn("ollama", ["serve"], {
            detached: true,
            stdio: ["ignore", ollamaLog, ollamaLog],
          });
          ollamaProcess.unref();
          ensureDir(path.dirname(pidFile));
          fs.appendFileSync(pidFile, `ollama=${String(ollamaProcess.pid)}\n`, "utf-8");
          await logStep(`Started ollama serve (PID ${String(ollamaProcess.pid)})`);
          started = true;
        }

        if (started) {
          spinner.start("Waiting for Ollama...");
          const healthy = await waitForService(11434, 60);
          spinner.stop();
          if (healthy) {
            await logSuccess("✔ Ollama online");
            startedServices.push("Ollama");
          } else {
            await logWarning("⚠ Ollama not responding");
            if (!options.allowPartial) process.exit(1);
          }
        } else {
          await logWarning(
            "⚠ Ollama not found.\n" +
            `  Install: ${chalk.gray("https://ollama.ai")} or ${chalk.gray("brew install ollama")}`
          );
        }
      }

      // Pull model if needed
      if (hasTool("ollama") && (await checkServiceHealth(11434))) {
        try {
          const model = process.env.OLLAMA_MODEL || "phi3:mini";
          const modelList = execSync("ollama list").toString();
          if (!modelList.includes(model)) {
            spinner.start(`Pulling model ${model}...`);
            execSync(`ollama pull ${model}`, {
              stdio: options.verbose ? "inherit" : "ignore",
            });
            spinner.stop(`Model ${model} ready.`);
          } else {
            await logSuccess(`✔ Model ${model} available`);
          }
        } catch {
          await logWarning("Could not verify Ollama models.");
        }
      }
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 3: LocalAI Image Worker (macOS M1/M2/M3)
    // ═══════════════════════════════════════════════════════════
    if (shouldStart("localai")) {
      await logStep("Phase 3: Image Generation (LocalAI)");

      const imageWorkerPort = parseInt(process.env.LOCALAI_PORT || "8085");
      const imageWorkerActive = await checkServiceHealth(imageWorkerPort);

      if (imageWorkerActive) {
        await logSuccess(`✔ Image Worker already active on port ${String(imageWorkerPort)}`);
        startedServices.push("LocalAI");
      } else if (process.platform === "darwin" && hasTool("local-ai")) {
        const modelsPath = resolveModelsDir();
        const localAiLog = fs.createWriteStream(
          path.join(logDir, `${dateStamp}_image-worker.log`),
          { flags: "a" },
        );

        spinner.start("Launching LocalAI (Metal GPU on Apple Silicon)...");
        try {
          const localAiProcess = spawn("local-ai", [
            `--listen=127.0.0.1:${String(imageWorkerPort)}`,
            `--models-path=${modelsPath}`,
          ], {
            detached: true,
            stdio: ["ignore", localAiLog, localAiLog],
            env: { ...process.env, FORCE_CPU: "false" },
          });
          localAiProcess.unref();
          fs.appendFileSync(pidFile, `image-worker=${String(localAiProcess.pid)}\n`, "utf-8");
          spinner.stop(`LocalAI started (PID ${String(localAiProcess.pid)})`);

          const healthy = await waitForService(imageWorkerPort, 45);
          if (healthy) {
            await logSuccess(`✔ Image Worker ready on port ${String(imageWorkerPort)}`);
            startedServices.push("LocalAI");
          } else {
            await logWarning("⚠ Image Worker not responding (image generation may fail)");
          }
        } catch {
          spinner.stop("Failed to start LocalAI", 1);
          await logWarning("⚠ LocalAI startup failed");
        }
      } else {
        await logWarning(
          "⚠ LocalAI not available (image generation disabled)\n" +
          `  On macOS: ${chalk.gray("brew install localai")}\n` +
          `  Or: ${chalk.gray("https://localai.io/docs/getting-started/")}`
        );
      }
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 4: Video Worker (Python)
    // ═══════════════════════════════════════════════════════════
    if (shouldStart("video-worker")) {
      await logStep("Phase 4: Video Generation Worker");

      const videoWorkerPort = parseInt(process.env.VIDEO_WORKER_PORT || "8188");
      const videoWorkerActive = await checkServiceHealth(videoWorkerPort);

      if (videoWorkerActive) {
        await logSuccess(`✔ Video Worker already active on port ${String(videoWorkerPort)}`);
        startedServices.push("Video Worker");
      } else {
        // Use correct monorepo path: orasaka-apps/orasaka-workers/video/app/main.py
        const mainPy = path.join(workspaceRoot, "orasaka-apps", "orasaka-workers", "video", "app", "main.py");
        const pythonCmd = resolveVideoWorkerPython(workspaceRoot);

        if (fs.existsSync(mainPy) && pythonCmd) {
          const videoLog = fs.createWriteStream(
            path.join(logDir, `${dateStamp}_video-worker.log`),
            { flags: "a" },
          );

          spinner.start("Starting Video Worker (Python)...");
          try {
            const videoProcess = spawn(pythonCmd, ["-u", mainPy], {
              detached: true,
              env: {
                ...process.env,
                PYTHONPATH: path.join(workspaceRoot, "orasaka-apps", "orasaka-workers", "video"),
                VIDEO_WORKER_PORT: String(videoWorkerPort),
              },
              stdio: ["ignore", videoLog, videoLog],
            });
            videoProcess.unref();
            fs.appendFileSync(pidFile, `video-worker=${String(videoProcess.pid)}\n`, "utf-8");
            spinner.stop(`Video Worker started (PID ${String(videoProcess.pid)})`);

            const healthy = await waitForService(videoWorkerPort, 30);
            if (healthy) {
              await logSuccess(`✔ Video Worker ready on port ${String(videoWorkerPort)}`);
              startedServices.push("Video Worker");
            } else {
              await logWarning("⚠ Video Worker not responding");
            }
          } catch {
            spinner.stop("Failed to start Video Worker", 1);
            await logWarning("⚠ Video Worker startup failed");
          }
        } else {
          const issues: string[] = [];
          if (!fs.existsSync(mainPy)) issues.push("main.py not found at expected path");
          if (!pythonCmd) issues.push("Python 3 not found");
          await logWarning(
            `⚠ Video Worker not available (${issues.join(", ")})\n` +
            `  Setup: ${chalk.gray("npx orasaka install")} to configure video worker environment`
          );
        }
      }
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 5: Gateway + UI (full mode only)
    // ═══════════════════════════════════════════════════════════
    if (mode === "full" && !onlyService) {
      // Gateway
      if (shouldStart("gateway")) {
        await logStep("Phase 5a: Starting Gateway (Spring Boot)...");
        const gatewayJar = path.join(workspaceRoot, "orasaka-apps", "orasaka-gateway", "target",
          fs.readdirSync(path.join(workspaceRoot, "orasaka-apps", "orasaka-gateway", "target") || ".")
            .find((f: string) => f.endsWith(".jar") && !f.endsWith("-sources.jar")) || "orasaka-gateway.jar"
        );

        if (fs.existsSync(gatewayJar)) {
          const gatewayLog = fs.createWriteStream(
            path.join(logDir, `${dateStamp}_gateway.log`),
            { flags: "a" },
          );
          const gatewayProcess = spawn("java", ["-jar", gatewayJar], {
            detached: true,
            env: { ...process.env },
            stdio: ["ignore", gatewayLog, gatewayLog],
          });
          gatewayProcess.unref();
          fs.appendFileSync(pidFile, `gateway=${String(gatewayProcess.pid)}\n`, "utf-8");

          const healthy = await waitForService(8080, 60);
          if (healthy) {
            await logSuccess("✔ Gateway ready on port 8080");
            startedServices.push("Gateway");
          } else {
            await logWarning("⚠ Gateway did not start in time");
          }
        } else {
          await logWarning(
            "⚠ Gateway JAR not found. Build first:\n" +
            `  ${chalk.gray("cd orasaka-apps/orasaka-gateway && mvn clean package -DskipTests")}`
          );
        }
      }

      // UI
      if (shouldStart("ui")) {
        await logStep("Phase 5b: Starting Next.js UI...");
        const uiDir = path.join(workspaceRoot, "orasaka-apps", "orasaka-ui");
        if (fs.existsSync(uiDir)) {
          const uiLog = fs.createWriteStream(
            path.join(logDir, `${dateStamp}_ui.log`),
            { flags: "a" },
          );
          const uiProcess = spawn("npm", ["run", "dev"], {
            detached: true,
            cwd: uiDir,
            stdio: ["ignore", uiLog, uiLog],
          });
          uiProcess.unref();
          fs.appendFileSync(pidFile, `ui=${String(uiProcess.pid)}\n`, "utf-8");
          await logSuccess(`✔ UI started on port 3000 (PID ${String(uiProcess.pid)})`);
          startedServices.push("Next.js UI");
        } else {
          await logWarning("⚠ UI directory not found");
        }
      }
    }

    // ═══════════════════════════════════════════════════════════
    // SUMMARY
    // ═══════════════════════════════════════════════════════════
    const summaryLines = [
      `${chalk.green("✔")} Services: ${startedServices.length > 0 ? startedServices.join(", ") : chalk.yellow("none")}`,
      `${chalk.yellow("📍")} Logs: ${chalk.cyan(path.relative(workspaceRoot, logDir))}`,
      `${chalk.yellow("📋")} PIDs: ${chalk.cyan(path.relative(workspaceRoot, pidFile))}`,
    ];

    if (mode === "dev") {
      summaryLines.push(
        "",
        `${chalk.cyan.bold("Dev mode — You manage these yourself:")}`,
        `  ${chalk.white("→")} ${chalk.cyan("Gateway")}: Start from ${chalk.yellow("IntelliJ")} (Run/Debug GatewayApplication)`,
        `  ${chalk.white("→")} ${chalk.cyan("Frontend")}: ${chalk.gray(`cd orasaka-apps/orasaka-ui && npm run dev`)}`,
        "",
        `${chalk.gray("Tip:")} Use ${chalk.cyan("npx orasaka logs")} to tail all service logs`,
        `${chalk.gray("Tip:")} Use ${chalk.cyan("npx orasaka status")} to check service health`,
      );
    }

    await note(summaryLines.join("\n"), mode === "dev" ? "🛠  Dev Infrastructure Ready" : "🚀 Full Infrastructure Ready");

    // Tail logs if requested
    if (options.logs) {
      await logInfo("Tailing logs (Ctrl+C to stop)...\n");
      try {
        const logFiles = fs.readdirSync(logDir)
          .filter((f: string) => f.startsWith(dateStamp))
          .map((f: string) => path.join(logDir, f));

        if (logFiles.length > 0) {
          const tail = spawn("tail", ["-f", ...logFiles], {
            stdio: "inherit",
          });
          await new Promise<void>((resolve) => {
            tail.on("exit", () => resolve());
          });
        }
      } catch { /* ignore */ }
    }

    await outro(chalk.cyan(mode === "dev"
      ? "Dev infrastructure ready! Start Gateway from IntelliJ 🚀"
      : "Full infrastructure ready! Open http://localhost:3000 🚀"
    ));
  });
