/**
 * @file dev.command.ts
 * @description Parallel development orchestrator for the Orasaka full-stack.
 * Spawns Gateway backend, Web Client, Web Admin, and Mobile Expo servers
 * with color-coded prefixed output.
 */

import { Command } from "commander";
import { spawn, ChildProcess } from "node:child_process";
import * as path from "node:path";
import chalk from "chalk";
import { logStep, logWarning, logError } from "../ui/prompts";

/** Label configuration for each spawned process. */
interface ProcessConfig {
  label: string;
  color: (text: string) => string;
  command: string;
  args: string[];
  cwd: string;
  enabled: boolean;
}

/**
 * Resolves the monorepo root directory by walking up from the CLI source.
 * Expects: orasaka-apps/orasaka-ui/orasaka-cli/ → root is 4 levels up.
 */
function resolveMonorepoRoot(): string {
  return path.resolve(__dirname, "..", "..", "..", "..", "..");
}

/**
 * Spawns a child process with prefixed, color-coded stdout/stderr piping.
 */
function spawnWithPrefix(config: ProcessConfig): ChildProcess {
  const prefix = config.color(`[${config.label}]`);

  const child = spawn(config.command, config.args, {
    cwd: config.cwd,
    stdio: ["ignore", "pipe", "pipe"],
    shell: true,
    env: { ...process.env },
  });

  child.stdout?.on("data", (data: Buffer) => {
    const lines = data.toString().split("\n").filter(Boolean);
    for (const line of lines) {
      console.log(`${prefix} ${line}`);
    }
  });

  child.stderr?.on("data", (data: Buffer) => {
    const lines = data.toString().split("\n").filter(Boolean);
    for (const line of lines) {
      console.log(`${prefix} ${chalk.dim(line)}`);
    }
  });

  child.on("error", (err) => {
    logError(`${config.label} failed to start: ${err.message}`);
  });

  child.on("exit", (code) => {
    if (code !== null && code !== 0) {
      logWarning(`${config.label} exited with code ${code}`);
    }
  });

  return child;
}

export const devCommand = new Command("dev")
  .description("Launch the full Orasaka development stack in parallel")
  .option("--skip-core", "Skip Gateway backend (for IDE debugging)")
  .option("--skip-admin", "Skip Admin console")
  .option("--skip-mobile", "Skip Expo mobile server")
  .option("--skip-web", "Skip Web client")
  .option("--skip-krizaka", "Skip Krizaka corporate vitrine")
  .action((opts: {
    skipCore?: boolean;
    skipAdmin?: boolean;
    skipMobile?: boolean;
    skipWeb?: boolean;
    skipKrizaka?: boolean;
  }) => {
    const root = resolveMonorepoRoot();
    const uiRoot = path.join(root, "orasaka-apps", "orasaka-ui");

    logStep("Launching Orasaka development stack…");

    const processes: ProcessConfig[] = [
      {
        label: "CORE",
        color: chalk.green,
        command: "./mvnw",
        args: [
          "spring-boot:run",
          "-pl", "orasaka-apps/orasaka-gateway",
          "-Dspring-boot.run.profiles=e2e",
        ],
        cwd: root,
        enabled: !opts.skipCore,
      },
      {
        label: "WEB-CLIENT",
        color: chalk.cyan,
        command: "npm",
        args: ["run", "dev"],
        cwd: path.join(uiRoot, "orasaka-web-client"),
        enabled: !opts.skipWeb,
      },
      {
        label: "WEB-ADMIN",
        color: chalk.magenta,
        command: "npm",
        args: ["run", "dev"],
        cwd: path.join(uiRoot, "orasaka-web-admin"),
        enabled: !opts.skipAdmin,
      },
      {
        label: "MOBILE",
        color: chalk.yellow,
        command: "npx",
        args: ["expo", "start"],
        cwd: path.join(uiRoot, "orasaka-mobile-client"),
        enabled: !opts.skipMobile,
      },
      {
        label: "KRIZAKA",
        color: chalk.hex("#F59E0B"),
        command: "npm",
        args: ["run", "dev", "--", "-p", "3002"],
        cwd: path.join(uiRoot, "krizaka-com"),
        enabled: !opts.skipKrizaka,
      },
    ];

    const activeConfigs = processes.filter((p) => p.enabled);

    if (activeConfigs.length === 0) {
      logWarning("All processes skipped — nothing to launch.");
      return;
    }

    console.log("");
    console.log(chalk.cyan.bold("  ┌─ Orasaka Dev Stack ─────────────────────┐"));
    for (const cfg of activeConfigs) {
      console.log(
        `  ${chalk.cyan("│")}  ${cfg.color("●")} ${cfg.label.padEnd(14)} ${chalk.dim(cfg.cwd.replace(root, "."))}`,
      );
    }
    console.log(chalk.cyan.bold("  └─────────────────────────────────────────┘"));
    console.log("");

    const children: ChildProcess[] = [];

    for (const cfg of activeConfigs) {
      children.push(spawnWithPrefix(cfg));
    }

    // Graceful shutdown on SIGINT / SIGTERM
    const shutdown = () => {
      console.log(chalk.yellow("\n⏹  Shutting down all processes…"));
      for (const child of children) {
        child.kill("SIGTERM");
      }
      // Force kill after 5 seconds
      setTimeout(() => {
        for (const child of children) {
          if (!child.killed) {
            child.kill("SIGKILL");
          }
        }
        process.exit(0);
      }, 5000);
    };

    process.on("SIGINT", shutdown);
    process.on("SIGTERM", shutdown);
  });
