/**
 * @file status.command.ts
 * @description Live dashboard showing all Orasaka service statuses,
 * ports, PIDs, and health indicators.
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import chalk from "chalk";
import {
  intro,
  outro,
  note,
} from "../ui/prompts";
import {
  resolveWorkspaceRoot,
  resolvePidFile,
  resolveLogDir,
  isAppleSilicon,
} from "../ui/platform";
import { checkPortAvailable } from "../services/diagnostics";

interface ServiceEntry {
  readonly name: string;
  readonly port: number;
  readonly category: "middleware" | "ai" | "worker" | "app";
  readonly critical: boolean;
}

const SERVICES: ServiceEntry[] = [
  { name: "PostgreSQL", port: 5432, category: "middleware", critical: true },
  { name: "Redis", port: 6379, category: "middleware", critical: true },
  { name: "RabbitMQ", port: 5672, category: "middleware", critical: true },
  { name: "RabbitMQ Mgmt", port: 15672, category: "middleware", critical: false },
  { name: "Ollama", port: 11434, category: "ai", critical: true },
  { name: "LocalAI", port: 8085, category: "worker", critical: false },
  { name: "Video Worker", port: 8188, category: "worker", critical: false },
  { name: "Gateway API", port: 8080, category: "app", critical: true },
  { name: "Frontend UI", port: 3000, category: "app", critical: true },
];

function readPids(pidFile: string): Map<string, number> {
  const pids = new Map<string, number>();
  if (!fs.existsSync(pidFile)) return pids;

  const content = fs.readFileSync(pidFile, "utf-8");
  for (const line of content.split("\n")) {
    if (!line || line.startsWith("#")) continue;
    const parts = line.split("=");
    if (parts.length === 2) {
      const name = parts[0]!.trim();
      const pid = parseInt(parts[1]!.trim(), 10);
      if (!isNaN(pid)) pids.set(name, pid);
    }
  }
  return pids;
}

function isPidAlive(pid: number): boolean {
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

export const statusCommand = new Command("status")
  .description("Show live status of all Orasaka services")
  .option("--json", "Output status as JSON")
  .option("--watch", "Refresh every 5 seconds")
  .action(async (options: { json?: boolean; watch?: boolean }) => {
    const workspaceRoot = resolveWorkspaceRoot();

    const renderStatus = async (): Promise<void> => {
      const pidFile = resolvePidFile();
      const pids = readPids(pidFile);

      const results: Array<{
        name: string;
        port: number;
        status: "running" | "stopped" | "unknown";
        pid: number | null;
        category: string;
      }> = [];

      for (const svc of SERVICES) {
        const portInUse = !(await checkPortAvailable(svc.port));
        let pid: number | null = null;

        // Try to find PID from pid file
        const pidKeys = [svc.name.toLowerCase().replace(/\s+/g, "-"), svc.name.toLowerCase()];
        for (const key of pidKeys) {
          if (pids.has(key)) {
            const p = pids.get(key)!;
            if (isPidAlive(p)) pid = p;
            break;
          }
        }

        results.push({
          name: svc.name,
          port: svc.port,
          status: portInUse ? "running" : "stopped",
          pid,
          category: svc.category,
        });
      }

      if (options.json) {
        console.log(JSON.stringify(results, null, 2));
        return;
      }

      // Format table output
      const categoryLabels: Record<string, string> = {
        middleware: "🐳 Docker Middleware",
        ai: "🤖 AI Engines",
        worker: "🎨 Workers",
        app: "📱 Applications",
      };

      const lines: string[] = [];
      let currentCategory = "";

      for (const r of results) {
        if (r.category !== currentCategory) {
          if (currentCategory) lines.push("");
          lines.push(chalk.bold(categoryLabels[r.category] ?? r.category));
          currentCategory = r.category;
        }

        const statusIcon = r.status === "running"
          ? chalk.green("● UP  ")
          : chalk.red("○ DOWN");

        const portStr = chalk.gray(`:${String(r.port)}`);
        const pidStr = r.pid ? chalk.gray(`PID ${String(r.pid)}`) : "";

        lines.push(
          `  ${statusIcon} ${r.name.padEnd(18)} ${portStr.padEnd(16)} ${pidStr}`
        );
      }

      // Summary counts
      const running = results.filter((r) => r.status === "running").length;
      const total = results.length;
      lines.push("");
      lines.push(
        `${chalk.gray("Summary:")} ${chalk.green(String(running))}/${String(total)} services running` +
        (isAppleSilicon() ? ` ${chalk.gray("| Apple Silicon ✔")}` : "")
      );

      await note(lines.join("\n"), "🥷 Orasaka Service Status");
    };

    if (options.watch) {
      console.log(chalk.gray("Refreshing every 5s — Ctrl+C to stop\n"));
      const refresh = async (): Promise<void> => {
        console.clear();
        await renderStatus();
      };

      await refresh();
      const interval = setInterval(() => {
        refresh().catch(() => {});
      }, 5000);

      await new Promise<void>((resolve) => {
        process.on("SIGINT", () => {
          clearInterval(interval);
          resolve();
        });
      });
    } else {
      await intro(chalk.cyan.bold("🥷 Orasaka Status"));
      await renderStatus();

      const logDir = resolveLogDir();
      if (fs.existsSync(logDir)) {
        const logFiles = fs.readdirSync(logDir).filter((f) => f.endsWith(".log"));
        if (logFiles.length > 0) {
          console.log(
            `\n  ${chalk.gray("Logs:")} ${chalk.cyan(`npx orasaka logs`)} (${String(logFiles.length)} file(s) in ${path.relative(workspaceRoot, logDir)})`
          );
        }
      }

      await outro(chalk.gray("Use --watch for live updates"));
    }
  });
