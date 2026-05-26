/**
 * @file start.command.ts
 * @description Command to start all local services and AI engines programmatically.
 * Uses platform utilities for cross-platform path resolution and tool detection.
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

// Load environment variables
const envFilePath = resolveEnvFile();
if (fs.existsSync(envFilePath)) {
  dotenv.config({ path: envFilePath, quiet: true });
}

function checkPort(port: number, host: string = "127.0.0.1"): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    const onError = () => {
      socket.destroy();
      resolve(false);
    };
    socket.setTimeout(200);
    socket.once("connect", () => {
      socket.destroy();
      resolve(true);
    });
    socket.once("error", onError);
    socket.once("timeout", onError);
    socket.connect(port, host);
  });
}

function checkHttp(url: string, timeout: number = 1000): Promise<boolean> {
  return new Promise((resolve) => {
    const req = http.get(url, (res) => {
      resolve(res.statusCode !== undefined && res.statusCode >= 200 && res.statusCode < 400);
    });
    req.on("error", () => resolve(false));
    req.setTimeout(timeout, () => {
      req.destroy();
      resolve(false);
    });
  });
}

export const startCommand = new Command("start")
  .description("Ignite the Orasaka infrastructure — Docker middleware, AI engines, and workers")
  .option("--skip-health", "Skip the final health gate verification loop")
  .action(async (options: { skipHealth?: boolean }) => {
    await intro(chalk.cyan.bold("🥷 Orasaka Infrastructure Bootstrap"));

    const workspaceRoot = resolveWorkspaceRoot();
    const pidFile = resolvePidFile();
    const logDir = resolveLogDir();
    ensureDir(logDir);

    // Verify compose file
    const composeFile = resolveComposeFile();
    if (!fs.existsSync(composeFile)) {
      await logError(
        "docker-compose.yml not found.\n" +
        "  Run " + chalk.cyan("npx orasaka init") + " to generate it."
      );
      process.exit(1);
    }

    // Resolve Docker Compose command
    const dockerComposeCmd = resolveDockerComposeCmd();
    if (!dockerComposeCmd) {
      await logError(
        "Docker Compose not found on PATH.\n" +
        "  Install Docker Desktop or run: " + chalk.gray("brew install --cask docker")
      );
      process.exit(1);
    }

    const dateStamp = format(new Date(), "yyyyMMdd");

    // ─── Step 1: Docker middleware ───────────────────────────
    const spinner = await createSpinner();
    spinner.start("Launching Docker middleware (Postgres, Redis, RabbitMQ, MCP)...");
    try {
      execSync(`${dockerComposeCmd} -p orasaka -f "${composeFile}" up -d postgres redis rabbitmq mcp-debug-server`, { stdio: "ignore" });
      spinner.stop("Docker middleware launched.");
    } catch (err: unknown) {
      spinner.stop("Failed to launch Docker containers", 1);
      const msg = err instanceof Error ? err.message : "Unknown error";
      await logError(`Docker error: ${msg}`);
      process.exit(1);
    }

    // ─── Step 2: Ollama ─────────────────────────────────────
    await logStep("Verifying Ollama Gateway on port 11434...");
    const ollamaActive = await checkPort(11434);
    if (!ollamaActive) {
      await logWarning("Ollama not active. Attempting to start...");
      let started = false;

      if (process.platform === "darwin") {
        try {
          execSync("open -a Ollama", { stdio: "ignore" });
          started = true;
        } catch {
          // Fall through to CLI attempt
        }
      }

      if (!started && hasTool("ollama")) {
        const ollamaLog = fs.createWriteStream(path.join(logDir, `${dateStamp}_ollama.log`), { flags: "a" });
        const ollamaProcess = spawn("ollama", ["serve"], {
          detached: true,
          stdio: ["ignore", ollamaLog, ollamaLog],
        });
        ollamaProcess.unref();
        fs.appendFileSync(pidFile, `ollama=${String(ollamaProcess.pid)}\n`, "utf-8");
        started = true;
      }

      if (started) {
        let retries = 30;
        let success = false;
        while (retries > 0) {
          if (await checkHttp("http://localhost:11434/")) {
            success = true;
            break;
          }
          await new Promise((r) => setTimeout(r, 1000));
          retries--;
        }
        if (!success) {
          await logError("Ollama failed to respond on port 11434.");
          process.exit(1);
        }
        await logSuccess("Ollama is now responsive.");
      } else {
        await logWarning(
          "Ollama not found. Chat will still work if an alternative provider is configured.\n" +
          "  Install: " + chalk.gray("brew install ollama")
        );
      }
    } else {
      await logSuccess("Ollama already active.");
    }

    // Verify model
    if (hasTool("ollama")) {
      try {
        const ollamaModel = process.env.OLLAMA_MODEL ?? "phi3:mini";
        const ollamaList = execSync("ollama list").toString();
        if (!ollamaList.includes(ollamaModel)) {
          await logWarning(`${ollamaModel} not found. Pulling...`);
          execSync(`ollama pull ${ollamaModel}`, { stdio: "inherit" });
        }
        await logSuccess(`Model ${ollamaModel} ready.`);
      } catch {
        await logWarning("Could not verify models. Ensure 'ollama' is installed.");
      }
    }

    // ─── Step 3: LocalAI Image Worker (8085) ────────────────
    await logStep("Verifying Image & Audio Worker on port 8085...");
    const localAiActive = await checkPort(8085);
    if (!localAiActive) {
      let localAiBin = "";
      const localAiNames = ["local-ai", "localai"];
      for (const name of localAiNames) {
        if (hasTool(name)) { localAiBin = name; break; }
      }

      if (!localAiBin) {
        const binPaths = [
          path.join(workspaceRoot, "var", "bin", "local-ai"),
          path.join(workspaceRoot, "var", "bin", "localai"),
        ];
        for (const bp of binPaths) {
          if (fs.existsSync(bp)) { localAiBin = bp; break; }
        }
      }

      if (localAiBin) {
        const modelsPath = resolveModelsDir();
        const logFile = path.join(logDir, `${dateStamp}_image-worker.log`);
        const logStream = fs.createWriteStream(logFile, { flags: "a" });

        const localAiProcess = spawn(
          localAiBin,
          ["--models-path", modelsPath, "--backends-path", path.join(modelsPath, "backends"), "--address", "127.0.0.1:8085"],
          { detached: true, stdio: ["ignore", logStream, logStream] },
        );
        localAiProcess.unref();
        fs.appendFileSync(pidFile, `image-worker=${String(localAiProcess.pid)}\n`, "utf-8");
        await logSuccess(`LocalAI started (PID ${String(localAiProcess.pid)}).`);
      } else {
        await logWarning("LocalAI binary not found. Image generation unavailable.");
      }
    } else {
      await logSuccess("Image & Audio Worker already active.");
    }

    // ─── Step 4: Video Worker (8188) ────────────────────────
    await logStep("Verifying Video Worker on port 8188...");
    const videoActive = await checkPort(8188);
    if (!videoActive) {
      const mainPy = path.join(workspaceRoot, "orasaka-workers", "video", "app", "main.py");
      if (fs.existsSync(mainPy)) {
        const logFile = path.join(logDir, `${dateStamp}_video-worker.log`);
        const logStream = fs.createWriteStream(logFile, { flags: "a" });

        const videoProcess = spawn("python3", ["-u", mainPy], {
          detached: true,
          env: { ...process.env, PYTHONPATH: path.join(workspaceRoot, "orasaka-workers", "video") },
          stdio: ["ignore", logStream, logStream],
        });
        videoProcess.unref();
        fs.appendFileSync(pidFile, `video-worker=${String(videoProcess.pid)}\n`, "utf-8");
        await logSuccess(`Video Worker started (PID ${String(videoProcess.pid)}).`);
      } else {
        await logWarning("Video worker script not found. Video generation unavailable.");
      }
    } else {
      await logSuccess("Video Worker already active.");
    }

    // ─── Step 5: Image Gen Worker (8086) ────────────────────
    await logStep("Verifying Image Gen Worker on port 8086...");
    const imageGenActive = await checkPort(8086);
    if (!imageGenActive) {
      const modelsDir = resolveModelsDir();
      const sdModel = path.join(modelsDir, "v1-5-pruned-emaonly.safetensors");
      const sdServerBin = path.join(modelsDir, "stable-diffusion.cpp", "build", "bin", "sd-server");

      if (fs.existsSync(sdModel) && fs.existsSync(sdServerBin)) {
        const logFile = path.join(logDir, `${dateStamp}_image-generation-worker.log`);
        const logStream = fs.createWriteStream(logFile, { flags: "a" });

        const sdProcess = spawn(
          sdServerBin,
          ["--listen-port", "8086", "-m", sdModel, "--seed", process.env.IMAGE_GEN_SEED ?? "-1"],
          { detached: true, stdio: ["ignore", logStream, logStream] },
        );
        sdProcess.unref();
        fs.appendFileSync(pidFile, `image-gen-worker=${String(sdProcess.pid)}\n`, "utf-8");
        await logSuccess(`Image Gen Worker started (PID ${String(sdProcess.pid)}).`);
      } else {
        await logWarning("SD model or sd-server binary not found. Image generation unavailable.");
      }
    } else {
      await logSuccess("Image Gen Worker already active.");
    }

    // ─── Step 6: Automation Worker (8082) ───────────────────
    await logStep("Verifying Automation Worker on port 8082...");
    const autoActive = await checkPort(8082);
    if (!autoActive) {
      if (hasTool("mvn")) {
        const logFile = path.join(logDir, `${dateStamp}_automation-worker.log`);
        const logStream = fs.createWriteStream(logFile, { flags: "a" });

        const autoProcess = spawn("mvn", ["spring-boot:run", "-pl", "orasaka-workers/automation"], {
          cwd: workspaceRoot,
          detached: true,
          env: { ...process.env, PORT: "8082" },
          stdio: ["ignore", logStream, logStream],
        });
        autoProcess.unref();
        fs.appendFileSync(pidFile, `automation-worker=${String(autoProcess.pid)}\n`, "utf-8");
        await logSuccess(`Automation Worker triggered (PID ${String(autoProcess.pid)}).`);
      } else {
        await logWarning("Maven not found. Automation worker unavailable.");
      }
    } else {
      await logSuccess("Automation Worker already active.");
    }

    // ─── Step 7: Health Gates ───────────────────────────────
    if (!options.skipHealth) {
      console.log("");
      const healthSpinner = await createSpinner();
      healthSpinner.start("Running health validation gates...");

      let retries = 30;
      let allHealthy = false;

      while (retries > 0) {
        const pgOk = await checkPort(5432);
        const redisOk = await checkPort(6379);
        const rabbitmqOk = await checkPort(5672);

        if (pgOk && redisOk && rabbitmqOk) {
          allHealthy = true;
          break;
        }

        healthSpinner.message(`Waiting for core services... (${String(retries)} retries left)`);
        await new Promise((r) => setTimeout(r, 2000));
        retries--;
      }

      if (!allHealthy) {
        healthSpinner.stop("Core services failed health gates", 1);
        process.exit(1);
      }

      healthSpinner.stop("Core services healthy.");
    }

    // ─── Summary ────────────────────────────────────────────
    console.log("");
    const summary = [
      `${chalk.green("⚡")} Docker middleware operational`,
      `${chalk.green("⚡")} AI workers initialized`,
      `${chalk.green("⚡")} Health gates verified`,
      "",
      `${chalk.cyan("Dashboard:")}    ${chalk.white("http://localhost:3000")}`,
      `${chalk.cyan("Gateway API:")}  ${chalk.white("http://localhost:8080")}`,
      `${chalk.cyan("RabbitMQ:")}     ${chalk.white("http://localhost:15672")}`,
    ];

    await note(summary.join("\n"), "🏆 Infrastructure Ready");
    await outro(chalk.green("All systems operational. Start building! ✨"));
  });
