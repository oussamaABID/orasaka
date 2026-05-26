/**
 * @file recover.command.ts
 * @description Recovery and troubleshooting command with guided problem resolution.
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import { execSync } from "node:child_process";
import chalk from "chalk";
import {
  intro,
  outro,
  select,
  confirm,
  logSuccess,
  logWarning,
  logError,
  logStep,
  note,
  createSpinner,
} from "../ui/prompts";
import {
  resolveWorkspaceRoot,
  resolveEnvFile,
  resolveComposeFile,
  resolvePidFile,
} from "../ui/platform";

interface RecoveryOption {
  id: string;
  label: string;
  hint: string;
  dangerous?: boolean;
}

const recoveryOptions: readonly RecoveryOption[] = [
  {
    id: "full-reset",
    label: "Full System Reset",
    hint: "Wipe databases and restart from clean state",
    dangerous: true,
  },
  {
    id: "docker-clean",
    label: "Clean Docker Environment",
    hint: "Remove stopped containers and orphaned volumes",
  },
  {
    id: "env-reset",
    label: "Reset Environment Variables",
    hint: "Regenerate .env file with defaults",
  },
  {
    id: "pid-cleanup",
    label: "Cleanup Process IDs",
    hint: "Remove stale PID references and orphaned processes",
  },
  {
    id: "port-diagnosis",
    label: "Diagnose Port Conflicts",
    hint: "Find and kill processes holding critical ports",
  },
  {
    id: "log-cleanup",
    label: "Clear Logs",
    hint: "Wipe previous logs to reduce disk usage",
  },
  {
    id: "diagnose-all",
    label: "Full Diagnostic Report",
    hint: "Run comprehensive system analysis",
  },
];

export const recoverCommand = new Command("recover")
  .description("Diagnose and recover from common issues")
  .action(async () => {
    await intro(chalk.cyan.bold("🥷 Orasaka Recovery & Troubleshooting"));

    const issue = await select({
      message: "What would you like to do?",
      options: recoveryOptions.map((opt) => ({
        value: opt.id,
        label: opt.label,
        hint: opt.hint,
      })),
    });

    const workspaceRoot = resolveWorkspaceRoot();
    const spinner = await createSpinner();

    switch (issue) {
      case "full-reset": {
        await logWarning("This will delete all databases and application state.");
        const confirmed = await confirm({
          message: "Are you absolutely sure?",
          initialValue: false,
        });

        if (!confirmed) {
          await outro(chalk.yellow("Reset cancelled."));
          return;
        }

        spinner.start("Removing Docker volumes and databases...");
        try {
          execSync("docker volume prune -f", { stdio: "ignore" });
          execSync("docker system prune -f", { stdio: "ignore" });
          spinner.stop("Docker cleanup complete.");
          await logSuccess("✔ Full reset complete. Run npx orasaka start to rebuild.");
        } catch (err) {
          spinner.stop("Cleanup failed", 1);
          await logError("Some cleanup operations failed. Check logs for details.");
        }
        break;
      }

      case "docker-clean": {
        spinner.start("Cleaning Docker environment...");
        try {
          execSync("docker container prune -f", { stdio: "ignore" });
          execSync("docker volume prune -f", { stdio: "ignore" });
          spinner.stop("Docker environment cleaned.");
          await logSuccess("✔ Removed orphaned containers and volumes.");
        } catch (err) {
          spinner.stop("Cleanup failed", 1);
        }
        break;
      }

      case "env-reset": {
        const envFile = resolveEnvFile();
        const backup = `${envFile}.backup`;

        if (fs.existsSync(envFile)) {
          fs.copyFileSync(envFile, backup);
          await logSuccess(`✔ Backed up existing .env to ${path.basename(backup)}`);
        }

        // Re-run init with --force --yes
        spinner.start("Regenerating .env...");
        try {
          execSync("npx orasaka init --force --yes", { stdio: "ignore" });
          spinner.stop(".env regenerated.");
          await logSuccess("✔ Environment reset complete.");

          if (fs.existsSync(backup)) {
            await note(
              `Original .env backed up as: ${path.basename(backup)}\n` +
              `Restore with: ${chalk.gray("cp " + path.basename(backup) + " .env")}`,
              "Backup Info"
            );
          }
        } catch (err) {
          spinner.stop("Reset failed", 1);
          await logError("Could not regenerate .env file.");
        }
        break;
      }

      case "pid-cleanup": {
        const pidFile = resolvePidFile();
        spinner.start("Cleaning up process IDs...");

        if (fs.existsSync(pidFile)) {
          const pids = fs.readFileSync(pidFile, "utf-8").split("\n");
          let killed = 0;

          for (const pidEntry of pids) {
            if (!pidEntry.trim()) continue;
            const pidNum = pidEntry.split("=")[1];
            if (!pidNum) continue;

            try {
              process.kill(parseInt(pidNum), "SIGTERM");
              killed++;
            } catch {}
          }

          fs.unlinkSync(pidFile);
          spinner.stop(`Cleaned up ${killed} process(es).`);
          await logSuccess(`✔ Removed ${killed} stale process references.`);
        } else {
          spinner.stop("No PID file found.");
          await logWarning("No stale processes to clean.");
        }
        break;
      }

      case "port-diagnosis": {
        spinner.start("Scanning for port conflicts...");
        const criticalPorts = [
          { port: 5432, name: "PostgreSQL" },
          { port: 6379, name: "Redis" },
          { port: 5672, name: "RabbitMQ" },
          { port: 8080, name: "Gateway API" },
          { port: 3000, name: "Frontend UI" },
          { port: 11434, name: "Ollama" },
        ];

        const conflicts: typeof criticalPorts = [];

        for (const service of criticalPorts) {
          try {
            if (process.platform === "darwin") {
              execSync(`lsof -i :${service.port}`, { stdio: "ignore" });
              conflicts.push(service);
            } else if (process.platform === "linux") {
              execSync(`netstat -tlnp | grep :${service.port}`, { stdio: "ignore" });
              conflicts.push(service);
            }
          } catch {}
        }

        spinner.stop();

        if (conflicts.length === 0) {
          await logSuccess("✔ No port conflicts detected.");
        } else {
          const conflictList = conflicts
            .map(
              (s) =>
                `${chalk.red("✖")} Port ${s.port} (${s.name}): In use\n` +
                `  Kill with: ${chalk.gray(`lsof -ti :${s.port} | xargs kill -9`)}`
            )
            .join("\n\n");

          await note(conflictList, "Port Conflicts Found");
        }
        break;
      }

      case "log-cleanup": {
        const logDir = path.join(workspaceRoot, "var", "logs");
        spinner.start("Clearing logs...");

        if (fs.existsSync(logDir)) {
          const files = fs.readdirSync(logDir);
          let deleted = 0;

          for (const file of files) {
            fs.unlinkSync(path.join(logDir, file));
            deleted++;
          }

          spinner.stop(`Deleted ${deleted} log file(s).`);
          await logSuccess(`✔ Cleaned ${deleted} log file(s).`);
        } else {
          spinner.stop("Log directory not found.");
        }
        break;
      }

      case "diagnose-all": {
        spinner.start("Running full diagnostics...");
        try {
          execSync("npx orasaka doctor --verbose", { stdio: "inherit" });
          spinner.stop();
        } catch {
          spinner.stop("Diagnostics failed", 1);
        }
        break;
      }
    }

    await outro(chalk.cyan("Recovery operations complete."));
  });
