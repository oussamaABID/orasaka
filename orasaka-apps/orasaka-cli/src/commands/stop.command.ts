/**
 * @file stop.command.ts
 * @description Teardown command with progressive status and cross-platform cleanup.
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import { execSync } from "node:child_process";
import chalk from "chalk";
import * as dotenv from "dotenv";
import {
  intro,
  outro,
  confirm,
  logSuccess,
  logWarning,
  createSpinner,
  note,
  handleCancel,
} from "../ui/prompts";
import {
  resolveWorkspaceRoot,
  resolveComposeFile,
  resolvePidFile,
  resolveEnvFile,
  resolveDockerComposeCmd,
} from "../ui/platform";

// Load environment variables
const envFilePath = resolveEnvFile();
if (fs.existsSync(envFilePath)) {
  dotenv.config({ path: envFilePath, quiet: true });
}

function terminateProcess(pid: number, _name: string): boolean {
  try {
    process.kill(pid, 0); // Check existence
    process.kill(pid, 15); // SIGTERM

    let alive = true;
    for (let i = 0; i < 5; i++) {
      try {
        process.kill(pid, 0);
        // Small delay without execSync("sleep")
        const start = Date.now();
        while (Date.now() - start < 1000) {
          // Busy wait for 1s — only used during teardown
        }
      } catch {
        alive = false;
        break;
      }
    }

    if (alive) {
      process.kill(pid, 9); // SIGKILL
    }
    return true;
  } catch {
    return false; // Already dead
  }
}

export const stopCommand = new Command("stop")
  .description("Teardown the Orasaka infrastructure — containers, workers, and state")
  .option("--purge", "Also purge database data and upload directories")
  .option("-y, --yes", "Skip confirmation prompts")
  .action(async (options: { purge?: boolean; yes?: boolean }) => {
    await intro(chalk.red.bold("🛑 Orasaka Infrastructure Teardown"));

    const workspaceRoot = resolveWorkspaceRoot();
    const pidFile = resolvePidFile();

    // Safety confirmation
    if (!options.yes) {
      const shouldContinue = await confirm({
        message: options.purge
          ? "This will stop ALL services AND delete database data. Continue?"
          : "This will stop all running Orasaka services. Continue?",
        initialValue: true,
        active: "Yes, stop everything",
        inactive: "Cancel",
      });
      handleCancel(shouldContinue);
      if (shouldContinue !== true) {
        await outro("Teardown cancelled.");
        return;
      }
    }

    const spinner = await createSpinner();
    let stoppedCount = 0;

    // ─── 1: PID-based teardown ──────────────────────────────
    if (fs.existsSync(pidFile)) {
      spinner.start("Terminating tracked processes...");
      const content = fs.readFileSync(pidFile, "utf-8");
      const lines = content.split("\n");

      for (const line of lines) {
        if (!line || line.startsWith("#")) continue;
        const parts = line.split("=");
        if (parts.length === 2) {
          const name = parts[0]!.trim();
          const pid = parseInt(parts[1]!.trim(), 10);
          if (!isNaN(pid)) {
            if (terminateProcess(pid, name)) {
              stoppedCount++;
            }
          }
        }
      }

      try { fs.unlinkSync(pidFile); } catch { /* Ignore */ }
      spinner.stop(`Terminated ${String(stoppedCount)} tracked process(es).`);
    }

    // ─── 2: Port sweep (Unix only) ─────────────────────────
    if (process.platform !== "win32") {
      spinner.start("Sweeping active ports...");
      const ports = [3000, 3001, 8080, 8082, 8085, 8086, 8188, 11434, 5432, 6379, 5672, 15672];

      for (const port of ports) {
        try {
          const pidsStr = execSync(`lsof -i :${String(port)} -t 2>/dev/null`).toString().trim();
          if (pidsStr) {
            for (const pidStr of pidsStr.split("\n")) {
              const pid = parseInt(pidStr.trim(), 10);
              if (isNaN(pid) || pid === process.pid) continue;

              let procName = "";
              try {
                procName = execSync(`ps -p ${String(pid)} -o comm= 2>/dev/null`).toString().trim();
              } catch { /* Ignore */ }

              // Skip Docker daemon
              if (procName.includes("Docker") || procName.includes("com.docker")) continue;

              try {
                process.kill(pid, 9);
                stoppedCount++;
              } catch { /* Ignore */ }
            }
          }
        } catch { /* No process on port */ }
      }
      spinner.stop("Port sweep complete.");
    }

    // ─── 2b: Database purge (optional) ──────────────────────
    if (options.purge) {
      try {
        const dockerPs = execSync("docker ps").toString();
        if (dockerPs.includes("orasaka-postgres")) {
          spinner.start("Purging database tables...");
          const sql = `
            TRUNCATE TABLE QRTZ_FIRED_TRIGGERS CASCADE;
            TRUNCATE TABLE QRTZ_SIMPLE_TRIGGERS CASCADE;
            TRUNCATE TABLE QRTZ_CRON_TRIGGERS CASCADE;
            TRUNCATE TABLE QRTZ_TRIGGERS CASCADE;
            TRUNCATE TABLE QRTZ_JOB_DETAILS CASCADE;
            TRUNCATE TABLE QRTZ_SCHEDULER_STATE CASCADE;
            TRUNCATE TABLE QRTZ_LOCKS CASCADE;
            TRUNCATE TABLE automation_job_execution_log CASCADE;
            TRUNCATE TABLE connector_credentials CASCADE;
            TRUNCATE TABLE orasaka_chat_sessions CASCADE;
            TRUNCATE TABLE orasaka_jobs CASCADE;
            TRUNCATE TABLE user_mcp_servers CASCADE;
            TRUNCATE TABLE platform_mcp_servers CASCADE;
            TRUNCATE TABLE platform_tool_configs CASCADE;
            TRUNCATE TABLE user_credentials CASCADE;
            TRUNCATE TABLE orasaka_users CASCADE;
            TRUNCATE TABLE orasaka_user_profiles CASCADE;
            TRUNCATE TABLE orasaka_verification_tokens CASCADE;
            TRUNCATE TABLE orasaka_user_interceptions CASCADE;
            TRUNCATE TABLE orasaka_tools_cache CASCADE;
            TRUNCATE TABLE orasaka_tools_rag_source CASCADE;
            TRUNCATE TABLE orasaka_password_resets CASCADE;
            TRUNCATE TABLE orasaka_ai_mcp_servers CASCADE;
            TRUNCATE TABLE orasaka_ai_rag_stores CASCADE;
          `.replace(/\s+/g, " ").trim();

          const dbUser = process.env.SPRING_DATASOURCE_USERNAME ?? "orasaka_admin";
          const dbName = process.env.POSTGRES_DB ?? "orasaka_db";
          execSync(`docker exec -i orasaka-postgres psql -U ${dbUser} -d ${dbName} -c "${sql}"`, { stdio: "ignore" });
          spinner.stop("Database tables purged.");
        }
      } catch {
        await logWarning("Database purge skipped (container may be stopped).");
      }
    }

    // ─── 3: Process pattern sweep (Unix only) ───────────────
    if (process.platform !== "win32") {
      const patterns = [
        "ollama", "orasaka-video-worker", "orasaka-automation-worker",
        "orasaka-worker-automation", "orasaka-workers/automation",
        "sd-server", "stable-diffusion", "next-dev", "next-server", "orasaka-gateway",
      ];

      for (const pattern of patterns) {
        try {
          const pidsStr = execSync(`pgrep -f "${pattern}" || true`).toString().trim();
          if (pidsStr) {
            for (const pidStr of pidsStr.split("\n")) {
              const pid = parseInt(pidStr.trim(), 10);
              if (!isNaN(pid) && pid !== process.pid) {
                try { process.kill(pid, 9); } catch { /* Ignore */ }
              }
            }
          }
        } catch { /* Ignore */ }
      }
    }

    // ─── 4: Docker Compose teardown ─────────────────────────
    const composeFile = resolveComposeFile();
    const dockerComposeCmd = resolveDockerComposeCmd();

    if (fs.existsSync(composeFile) && dockerComposeCmd) {
      spinner.start("Stopping Docker containers...");
      try {
        execSync(`${dockerComposeCmd} -p orasaka -f "${composeFile}" stop`, { stdio: "ignore" });
        if (options.purge) {
          execSync(`${dockerComposeCmd} -p orasaka -f "${composeFile}" down --timeout 5 -v`, { stdio: "ignore" });
          await logSuccess("Docker services and data volumes removed.");
        } else {
          execSync(`${dockerComposeCmd} -p orasaka -f "${composeFile}" down --timeout 5`, { stdio: "ignore" });
          await logSuccess("Docker services stopped (volumes preserved).");
        }
      } catch {
        await logWarning("Docker compose teardown had issues (containers may already be stopped).");
      }
      spinner.stop("Docker teardown complete.");
    }

    // ─── 5: Clean local directories ─────────────────────────
    if (options.purge) {
      spinner.start("Cleaning local directories...");
      const dirsToClean = [
        path.join(workspaceRoot, "var", "orasaka-uploads"),
        path.join(workspaceRoot, "var", "temp"),
      ];

      for (const dir of dirsToClean) {
        if (fs.existsSync(dir)) {
          for (const file of fs.readdirSync(dir)) {
            fs.rmSync(path.join(dir, file), { recursive: true, force: true });
          }
        }
      }

      // Clean PID files
      const varDir = path.join(workspaceRoot, "var");
      if (fs.existsSync(varDir)) {
        for (const file of fs.readdirSync(varDir)) {
          if (file.startsWith(".orasaka") && file.endsWith(".pid")) {
            fs.rmSync(path.join(varDir, file), { force: true });
          }
        }
      }

      spinner.stop("Local directories cleaned.");
    }

    // ─── 6: Memory flush (macOS) ────────────────────────────
    if (process.platform === "darwin") {
      try {
        execSync("purge", { stdio: "ignore" });
      } catch {
        try { execSync("sudo -n purge", { stdio: "ignore" }); } catch { /* Ignore */ }
      }
    }

    // ─── Summary ────────────────────────────────────────────
    console.log("");
    const summaryLines = [
      `${chalk.red("⏹")} Tracked processes terminated`,
      `${chalk.red("⏹")} Docker containers stopped`,
    ];
    if (options.purge) {
      summaryLines.push(`${chalk.red("⏹")} Database data purged`);
      summaryLines.push(`${chalk.red("⏹")} Upload and temp directories cleaned`);
    }
    summaryLines.push(
      "",
      `To restart: ${chalk.cyan("npx orasaka start")}`,
    );

    await note(summaryLines.join("\n"), "Teardown Complete");
    await outro(chalk.red("All services stopped."));
  });
