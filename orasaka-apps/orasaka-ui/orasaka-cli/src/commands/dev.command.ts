/**
 * @file dev.command.ts
 * @description Parallel development orchestrator for the Orasaka full-stack.
 * Spawns Gateway backend, Web Client, Web Admin, and Mobile Expo servers
 * with color-coded prefixed output and premium DevX logging.
 */

import { Command } from "commander";
import { spawn, ChildProcess } from "node:child_process";
import * as path from "node:path";
import chalk from "chalk";
import { logWarning, logError } from "../ui/prompts";

/** Label configuration for each spawned process. */
interface ProcessConfig {
  label: string;
  color: (text: string) => string;
  command: string;
  args: string[];
  cwd: string;
  enabled: boolean;
  port?: number;
}

/** Lines matching these patterns are filtered from process output. */
const NOISE_PATTERNS: RegExp[] = [
  /^> [\w@./-]+ \w+$/,          // > package@1.0.0 script
  /^> .+$/,                      // > next dev --port 3000
  /^\s*$/,                       // blank lines
];

/**
 * Resolves the monorepo root directory by walking up from the CLI source.
 * Expects: orasaka-apps/orasaka-ui/orasaka-cli/ → root is 4 levels up.
 */
function resolveMonorepoRoot(): string {
  return path.resolve(__dirname, "..", "..", "..", "..", "..");
}

/**
 * Checks if a log line is noise that should be suppressed.
 */
function isNoiseLine(line: string): boolean {
  const stripped = line.replace(
    // eslint-disable-next-line no-control-regex
    /\u001b\[[0-9;]*m/g,
    "",
  ).trim();
  return NOISE_PATTERNS.some((p) => p.test(stripped));
}

/**
 * Spawns a child process with prefixed, color-coded stdout/stderr piping.
 * Uses shell:false to avoid DEP0190 deprecation warnings.
 */
function spawnWithPrefix(config: ProcessConfig): ChildProcess {
  const prefix = config.color(`  ${config.label.padEnd(12)}`);

  const child = spawn(config.command, config.args, {
    cwd: config.cwd,
    stdio: ["ignore", "pipe", "pipe"],
    env: { ...process.env, FORCE_COLOR: "1" },
  });

  child.stdout?.on("data", (data: Buffer) => {
    const lines = data.toString().split("\n").filter(Boolean);
    for (const line of lines) {
      if (!isNoiseLine(line)) {
        console.log(`${prefix} ${chalk.white("│")} ${line}`);
      }
    }
  });

  child.stderr?.on("data", (data: Buffer) => {
    const lines = data.toString().split("\n").filter(Boolean);
    for (const line of lines) {
      if (!isNoiseLine(line)) {
        console.log(`${prefix} ${chalk.white("│")} ${chalk.dim(line)}`);
      }
    }
  });

  child.on("error", (err) => {
    console.log(
      `${prefix} ${chalk.red("│")} ${chalk.red("✗")} Failed to start: ${chalk.dim(err.message)}`,
    );
  });

  child.on("exit", (code) => {
    if (code !== null && code !== 0) {
      console.log(
        `${prefix} ${chalk.red("│")} ${chalk.red("✗")} Exited with code ${code}`,
      );
    } else {
      console.log(
        `${prefix} ${chalk.dim("│")} ${chalk.dim("○ Stopped")}`,
      );
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
  .action((opts: {
    skipCore?: boolean;
    skipAdmin?: boolean;
    skipMobile?: boolean;
    skipWeb?: boolean;
  }) => {
    const root = resolveMonorepoRoot();
    const uiRoot = path.join(root, "orasaka-apps", "orasaka-ui");

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
        port: 8080,
      },
      {
        label: "WEB-CLIENT",
        color: chalk.cyan,
        command: "npm",
        args: ["run", "dev", "--", "--port", "3000"],
        cwd: path.join(uiRoot, "orasaka-web-client"),
        enabled: !opts.skipWeb,
        port: 3000,
      },
      {
        label: "WEB-ADMIN",
        color: chalk.magenta,
        command: "npm",
        args: ["run", "dev"],
        cwd: path.join(uiRoot, "orasaka-web-admin"),
        enabled: !opts.skipAdmin,
        port: 3001,
      },
      {
        label: "MOBILE",
        color: chalk.yellow,
        command: "npx",
        args: ["expo", "start"],
        cwd: path.join(uiRoot, "orasaka-mobile-client"),
        enabled: !opts.skipMobile,
        port: 8081,
      },
    ];

    const activeConfigs = processes.filter((p) => p.enabled);
    const skippedConfigs = processes.filter((p) => !p.enabled);

    if (activeConfigs.length === 0) {
      logWarning("All processes skipped — nothing to launch.");
      return;
    }

    // ─── Startup Banner ──────────────────────────────────────
    const BOX_W = 54; // inner width between │ chars

    const pad = (text: string, width: number): string =>
      text + " ".repeat(Math.max(0, width - text.length));

    console.log("");
    console.log(chalk.cyan.bold("  ┌" + "─".repeat(BOX_W) + "┐"));
    console.log(chalk.cyan.bold("  │") + chalk.white.bold(pad("  🥷  Orasaka Dev Stack", BOX_W)) + chalk.cyan.bold("│"));
    console.log(chalk.cyan.bold("  ├" + "─".repeat(BOX_W) + "┤"));

    for (const cfg of activeConfigs) {
      const row = `  ● ${cfg.label.padEnd(14)}:${cfg.port}  http://localhost:${cfg.port}`;
      console.log(
        chalk.cyan("  │") +
        `  ${cfg.color("●")} ${cfg.color(cfg.label.padEnd(14))}` +
        chalk.white(`:${cfg.port}`) +
        chalk.dim(`  http://localhost:${cfg.port}`) +
        " ".repeat(Math.max(0, BOX_W - row.length)) +
        chalk.cyan("│"),
      );
    }

    if (skippedConfigs.length > 0) {
      const dashContent = "  " + Array.from({ length: Math.floor((BOX_W - 4) / 2) }, () => "─ ").join("").trimEnd();
      console.log(chalk.cyan("  │") + chalk.dim(pad(dashContent, BOX_W)) + chalk.cyan("│"));
      for (const cfg of skippedConfigs) {
        const row = `  ○ ${cfg.label.padEnd(14)}skipped`;
        console.log(
          chalk.cyan("  │") +
          `  ${chalk.dim("○")} ${chalk.dim(cfg.label.padEnd(14))}` +
          chalk.dim("skipped") +
          " ".repeat(Math.max(0, BOX_W - row.length)) +
          chalk.cyan("│"),
        );
      }
    }

    console.log(chalk.cyan.bold("  └" + "─".repeat(BOX_W) + "┘"));
    console.log("");
    console.log(chalk.dim("  Press Ctrl+C to stop all processes"));
    console.log("");

    // ─── Spawn Processes ─────────────────────────────────────
    const children: ChildProcess[] = [];

    for (const cfg of activeConfigs) {
      children.push(spawnWithPrefix(cfg));
    }

    // ─── Graceful Shutdown (guard against double-fire) ───────
    let shuttingDown = false;

    const shutdown = () => {
      if (shuttingDown) return;
      shuttingDown = true;

      console.log("");
      console.log(chalk.yellow.bold("  ⏹  Shutting down…"));
      console.log("");

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
