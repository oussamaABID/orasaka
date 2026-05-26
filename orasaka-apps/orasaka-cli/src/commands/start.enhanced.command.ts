/**
 * @file start.enhanced.command.ts
 * @description Enhanced service startup with recovery strategies and fallbacks.
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import * as http from "node:http";
import * as net from "node:net";
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
  select,
  confirm,
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
import { checkPortAvailable, checkHttpEndpoint } from "../services/diagnostics";

const envFilePath = resolveEnvFile();
if (fs.existsSync(envFilePath)) {
  dotenv.config({ path: envFilePath, quiet: true });
}

interface ServiceStatus {
  name: string;
  port: number;
  running: boolean;
  pid?: number;
}

async function checkServiceHealth(port: number, timeout: number = 2000): Promise<boolean> {
  const available = await checkPortAvailable(port);
  return !available; // If port NOT available, service is running
}

async function startWithRetry(
  command: string,
  options: { maxRetries?: number; delay?: number; timeout?: number } = {}
): Promise<boolean> {
  const { maxRetries = 3, delay = 2000, timeout = 30000 } = options;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      execSync(command, { timeout, stdio: "ignore" });
      return true;
    } catch {
      if (attempt < maxRetries) {
        await new Promise((r) => setTimeout(r, delay));
      }
    }
  }
  return false;
}

async function proposeRecovery(serviceName: string, port: number): Promise<string | null> {
  const inUse = !(await checkPortAvailable(port));

  if (inUse) {
    const response = await select({
      message: `Port ${port} is in use. Choose recovery strategy:`,
      options: [
        { value: "kill", label: "Terminate existing process", hint: "Kill PID using port" },
        { value: "skip", label: "Skip this service", hint: "Continue without this service" },
        { value: "different-port", label: "Use different port", hint: "Reconfigure the port" },
        { value: "abort", label: "Abort startup", hint: "Stop initialization" },
      ],
    });
    return response;
  }

  return "skip";
}

export const startEnhancedCommand = new Command("start")
  .description("Start Orasaka infrastructure with intelligent recovery strategies")
  .option("--skip-health", "Skip final health verification")
  .option("--wait-timeout <ms>", "Timeout for service startup (default: 60000ms)", "60000")
  .option("--allow-partial", "Allow partial startup (some services may fail)")
  .option("--verbose", "Show detailed startup logs")
  .action(async (options: {
    skipHealth?: boolean;
    waitTimeout?: string;
    allowPartial?: boolean;
    verbose?: boolean;
  }) => {
    await intro(chalk.cyan.bold("🥷 Orasaka Infrastructure Bootstrap v2"));

    const workspaceRoot = resolveWorkspaceRoot();
    const pidFile = resolvePidFile();
    const logDir = resolveLogDir();
    const timeout = parseInt(options.waitTimeout || "60000");
    ensureDir(logDir);

    // Verify environment
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
    const services: ServiceStatus[] = [];

    // ─── Phase 1: Docker Middleware ────────────────────────
    await logStep("Phase 1: Starting Docker services (Postgres, Redis, RabbitMQ)...");

    const spinner = await createSpinner();
    spinner.start("Launching Docker containers...");

    try {
      execSync(
        `${dockerComposeCmd} -p orasaka -f "${composeFile}" up -d postgres redis rabbitmq mcp-debug-server`,
        { stdio: options.verbose ? "inherit" : "ignore" }
      );
      spinner.stop("Docker containers launched.");
      services.push(
        { name: "PostgreSQL", port: 5432, running: true },
        { name: "Redis", port: 6379, running: true },
        { name: "RabbitMQ", port: 5672, running: true }
      );
    } catch (err) {
      spinner.stop("Failed to launch Docker containers", 1);

      if (options.allowPartial) {
        await logWarning("Continuing with partial startup...");
      } else {
        const msg = err instanceof Error ? err.message : "Unknown error";
        await logError(`Docker error: ${msg}`);
        process.exit(1);
      }
    }

    // Health check for Docker services
    spinner.start("Verifying Docker services...");
    let dockerHealthy = true;
    const dockeChecks = [
      { port: 5432, name: "PostgreSQL" },
      { port: 6379, name: "Redis" },
      { port: 5672, name: "RabbitMQ" },
    ];

    for (const check of dockeChecks) {
      let retries = 30;
      while (retries > 0 && !(await checkServiceHealth(check.port))) {
        await new Promise((r) => setTimeout(r, 1000));
        retries--;
      }

      if (retries > 0) {
        await logSuccess(`✔ ${check.name} healthy`);
      } else {
        await logWarning(`⚠ ${check.name} not responding`);
        dockerHealthy = false;
      }
    }

    spinner.stop();

    if (!dockerHealthy && !options.allowPartial) {
      await logError("Docker services failed to start.");
      process.exit(1);
    }

    // ─── Phase 2: Ollama ─────────────────────────────────────
    await logStep("Phase 2: Starting AI Engine (Ollama)...");

    const ollamaHealthy = await checkServiceHealth(11434);
    if (!ollamaHealthy) {
      await logWarning("Ollama not active on port 11434.");

      let started = false;

      // Try: macOS app
      if (process.platform === "darwin") {
        try {
          execSync("open -a Ollama", { stdio: "ignore" });
          await logStep("Launched Ollama.app on macOS...");
          started = true;
        } catch {}
      }

      // Try: CLI
      if (!started && hasTool("ollama")) {
        const ollamaLog = fs.createWriteStream(path.join(logDir, `${dateStamp}_ollama.log`), {
          flags: "a",
        });
        const ollamaProcess = spawn("ollama", ["serve"], {
          detached: true,
          stdio: ["ignore", ollamaLog, ollamaLog],
        });
        ollamaProcess.unref();
        fs.appendFileSync(pidFile, `ollama=${String(ollamaProcess.pid)}\n`, "utf-8");
        await logStep(`Started ollama CLI (PID ${ollamaProcess.pid})...`);
        started = true;
      }

      if (started) {
        // Wait for health
        let retries = 60;
        let healthy = false;
        while (retries > 0) {
          if (await checkServiceHealth(11434)) {
            healthy = true;
            break;
          }
          await new Promise((r) => setTimeout(r, 1000));
          retries--;
        }

        if (healthy) {
          await logSuccess("✔ Ollama online");
        } else {
          await logWarning("⚠ Ollama not responding (continuing without AI engine)");
          if (!options.allowPartial) {
            process.exit(1);
          }
        }
      } else {
        await logWarning(
          "⚠ Ollama not found.\n" +
          `  Install: ${chalk.gray("https://ollama.ai")} or ${chalk.gray("brew install ollama")}`
        );
      }
    } else {
      await logSuccess("✔ Ollama already active");
    }

    // Verify models
    if (hasTool("ollama")) {
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
          await logSuccess(`✔ Model ${model} ready`);
        }
      } catch {
        await logWarning("Could not verify Ollama models.");
      }
    }

    // ─── Phase 3: LocalAI Image Worker ────────────────────
    await logStep("Phase 3: Starting Image Generation Worker (LocalAI)...");

    let imageWorkerHealthy = false;
    const imageWorkerPort = parseInt(process.env.LOCALAI_PORT || "8085");
    const imageWorkerUrl = `http://localhost:${imageWorkerPort}`;
    const imageWorkerActive = await checkServiceHealth(imageWorkerPort);

    if (!imageWorkerActive) {
      if (process.platform === "darwin" && hasTool("local-ai")) {
        // macOS M1/M2/M3: Use local-ai CLI with Metal GPU support
        const modelsPath = resolveModelsDir();
        const localAiLog = fs.createWriteStream(path.join(logDir, `${dateStamp}_image-worker.log`), {
          flags: "a",
        });

        spinner.start("Launching LocalAI (Metal GPU acceleration on Apple Silicon)...");
        try {
          const localAiProcess = spawn("local-ai", [
            `--listen=127.0.0.1:${imageWorkerPort}`,
            `--models-path=${modelsPath}`,
          ], {
            detached: true,
            stdio: ["ignore", localAiLog, localAiLog],
            env: { ...process.env, FORCE_CPU: "false" }, // Allow GPU on M1/M2/M3
          });
          localAiProcess.unref();
          fs.appendFileSync(pidFile, `image-worker=${String(localAiProcess.pid)}\n`, "utf-8");
          spinner.stop(`LocalAI started (PID ${localAiProcess.pid})`);

          // Wait for LocalAI to be ready
          let retries = 45;
          while (retries > 0) {
            if (await checkServiceHealth(imageWorkerPort)) {
              imageWorkerHealthy = true;
              break;
            }
            await new Promise((r) => setTimeout(r, 1000));
            retries--;
          }

          if (imageWorkerHealthy) {
            await logSuccess(`✔ Image Worker ready at ${imageWorkerUrl}`);
          } else {
            await logWarning("⚠ Image Worker not responding (image generation may fail)");
          }
        } catch (err) {
          spinner.stop("Failed to start LocalAI", 1);
          await logWarning(
            `⚠ LocalAI startup failed.\n` +
            `  Install: ${chalk.gray("https://localai.io/docs/getting-started/")}`
          );
        }
      } else if (hasTool("docker")) {
        // Docker-based LocalAI fallback
        spinner.start("Pulling LocalAI Docker image...");
        try {
          execSync(
            `docker run -d --name orasaka-localai ` +
            `-p ${imageWorkerPort}:8080 ` +
            `-e MODELS_PATH=/models ` +
            `-v ${resolveModelsDir()}:/models ` +
            `localai:latest`,
            { stdio: options.verbose ? "inherit" : "ignore" }
          );
          spinner.stop("LocalAI container started.");
          imageWorkerHealthy = true;
        } catch {
          await logWarning("⚠ Could not start LocalAI container");
        }
      } else {
        await logWarning(
          `⚠ LocalAI not available (image generation disabled)\n` +
          `  On macOS M1: ${chalk.gray("brew install localai")}\n` +
          `  Or: ${chalk.gray("https://localai.io/docs/getting-started/")}`
        );
      }
    } else {
      await logSuccess(`✔ Image Worker already active on ${imageWorkerUrl}`);
      imageWorkerHealthy = true;
    }

    // ─── Phase 4: Video Worker ────────────────────────────
    await logStep("Phase 4: Starting Video Generation Worker...");

    let videoWorkerHealthy = false;
    const videoWorkerPort = parseInt(process.env.VIDEO_WORKER_PORT || "8188");
    const videoWorkerUrl = `http://localhost:${videoWorkerPort}`;
    const videoWorkerActive = await checkServiceHealth(videoWorkerPort);

    if (!videoWorkerActive) {
      const mainPy = path.join(workspaceRoot, "orasaka-workers", "video", "app", "main.py");

      if (fs.existsSync(mainPy) && hasTool("python3")) {
        const videoLog = fs.createWriteStream(path.join(logDir, `${dateStamp}_video-worker.log`), {
          flags: "a",
        });

        spinner.start("Starting Video Worker (Python)...");
        try {
          const videoProcess = spawn("python3", ["-u", mainPy], {
            detached: true,
            env: {
              ...process.env,
              PYTHONPATH: path.join(workspaceRoot, "orasaka-workers", "video"),
              VIDEO_WORKER_PORT: String(videoWorkerPort),
            },
            stdio: ["ignore", videoLog, videoLog],
          });
          videoProcess.unref();
          fs.appendFileSync(pidFile, `video-worker=${String(videoProcess.pid)}\n`, "utf-8");
          spinner.stop(`Video Worker started (PID ${videoProcess.pid})`);

          // Wait for video worker
          let retries = 30;
          while (retries > 0) {
            if (await checkServiceHealth(videoWorkerPort)) {
              videoWorkerHealthy = true;
              break;
            }
            await new Promise((r) => setTimeout(r, 1000));
            retries--;
          }

          if (videoWorkerHealthy) {
            await logSuccess(`✔ Video Worker ready at ${videoWorkerUrl}`);
          } else {
            await logWarning("⚠ Video Worker not responding (video generation may fail)");
            if (!options.allowPartial) {
              process.exit(1);
            }
          }
        } catch (err) {
          spinner.stop("Failed to start Video Worker", 1);
          await logWarning(`⚠ Video Worker not available`);
        }
      } else {
        await logWarning(
          `⚠ Video Worker not available\n` +
          `  Required: Python 3.11+ and orasaka-workers/video module\n` +
          `  Install: ${chalk.gray("python3 -m pip install -r requirements.txt")}`
        );
      }
    } else {
      await logSuccess(`✔ Video Worker already active on ${videoWorkerUrl}`);
      videoWorkerHealthy = true;
    }

    // ─── Phase 5: Summary ──────────────────────────────────
    await logStep("Phase 5: Startup Summary");

    const servicesUp = [
      "PostgreSQL",
      "Redis",
      "RabbitMQ",
      ollamaHealthy ? "Ollama (LLM)" : null,
      imageWorkerHealthy ? "LocalAI (Images)" : null,
      videoWorkerHealthy ? "Video Worker" : null,
    ].filter(Boolean);

    await note(
      [
        `${chalk.green("✔")} Services: ${servicesUp.join(", ")}`,
        `${chalk.yellow("📍")} Logs: ${chalk.cyan(path.relative(workspaceRoot, logDir))}`,
        `${chalk.yellow("🎨")} Image Worker: ${imageWorkerHealthy ? chalk.green("✔ Ready") : chalk.yellow("⚠ Not available")}`,
        `${chalk.yellow("🎬")} Video Worker: ${videoWorkerHealthy ? chalk.green("✔ Ready") : chalk.yellow("⚠ Not available")}`,
        `${chalk.yellow("⏱")}  Next: Launch backend & UI`,
      ].join("\n"),
      "Infrastructure Status"
    );

    await outro(chalk.cyan("🚀 Infrastructure ready! Open http://localhost:3000"));
  });
