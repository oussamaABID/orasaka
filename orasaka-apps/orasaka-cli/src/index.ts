#!/usr/bin/env node
/**
 * @file index.ts
 * @description Main entry point and bootstrap registry for the Orasaka CLI.
 * Registers all commands, displays the startup banner, and provides graceful error handling.
 */

import { Command } from "commander";
import * as fs from "node:fs";
import * as path from "node:path";
import chalk from "chalk";
import { VersionManager } from "./services/version-manager";

import { loginCommand } from "./commands/login.command";
import { registerCommand } from "./commands/register.command";
import { verifyCommand } from "./commands/verify.command";
import { forgotCommand } from "./commands/forgot.command";
import { resetCommand } from "./commands/reset.command";
import { chatCommand } from "./commands/chat.command";
import { settingsCommand } from "./commands/settings.command";
import { profileCommand } from "./commands/profile.command";
import { videoCommand } from "./commands/video.command";
import { graphCommand } from "./commands/graph.command";
import { mcpCommand } from "./commands/mcp.command";
import { initEnhancedCommand } from "./commands/init.enhanced.command";
import { doctorEnhancedCommand } from "./commands/doctor.enhanced.command";
import { startEnhancedCommand } from "./commands/start.enhanced.command";
import { recoverCommand } from "./commands/recover.command";
import { stopCommand } from "./commands/stop.command";
import { configCommand } from "./commands/config.command";
import { agentCommand } from "./commands/agent.command";
import { installCommand } from "./commands/install.command";
import { generateCommand } from "./commands/generate.command";

function getVersion(): string {
    const pkgPath = path.resolve(__dirname, "..", "package.json");
    const pkg = JSON.parse(fs.readFileSync(pkgPath, "utf-8")) as { version?: string };
    return pkg?.version ?? "UNKNOWN";
}

const VERSION = getVersion();

function getBanner(): string {
  const v = `v${VERSION}`;
  return `
${chalk.cyan("  ┌──────────────────────────────────────────────┐")}
${chalk.cyan("  │")}                                              ${chalk.cyan("│")}
${chalk.cyan("  │")}   ${chalk.white.bold("🥷  O R A S A K A")}   ${chalk.gray(v.padEnd(8))}              ${chalk.cyan("│")}
${chalk.cyan("  │")}   ${chalk.gray("Sovereign AI Orchestration Engine")}         ${chalk.cyan("│")}
${chalk.cyan("  │")}                                              ${chalk.cyan("│")}
${chalk.cyan("  └──────────────────────────────────────────────┘")}
`;
}

function getHelpFooter(): string {
  return `
${chalk.cyan.bold("Quick Start:")}
  ${chalk.white("$")} npx orasaka init       ${chalk.gray("Initialize workspace")}
  ${chalk.white("$")} npx orasaka doctor     ${chalk.gray("System diagnostics")}
  ${chalk.white("$")} npx orasaka config     ${chalk.gray("Configure environment")}
  ${chalk.white("$")} npx orasaka start      ${chalk.gray("Launch infrastructure")}

${chalk.cyan.bold("Documentation:")}
  ${chalk.gray("https://github.com/oussamaABID/orasaka/blob/main/docs/CLI.md")}
`;
}

const program = new Command("orasaka");

program
  .version(VERSION)
  .description("Orasaka AI CLI — Sovereign multi-modal AI orchestration from your terminal")
  .addHelpText("before", getBanner())
  .addHelpText("after", getHelpFooter())
  // ─── Lifecycle Commands ────────────────────────────────
  .addCommand(initEnhancedCommand)
  .addCommand(installCommand)
  .addCommand(doctorEnhancedCommand)
  .addCommand(configCommand)
  .addCommand(startEnhancedCommand)
  .addCommand(stopCommand)
  // ─── Recovery & Troubleshooting ─────────────────────────
  .addCommand(recoverCommand)
  // ─── Development & Code Generation ──────────────────────
  .addCommand(generateCommand)
  // ─── Auth Commands ─────────────────────────────────────
  .addCommand(loginCommand)
  .addCommand(registerCommand)
  .addCommand(verifyCommand)
  .addCommand(forgotCommand)
  .addCommand(resetCommand)
  // ─── Core Commands ─────────────────────────────────────
  .addCommand(chatCommand)
  .addCommand(settingsCommand)
  .addCommand(profileCommand)
  .addCommand(videoCommand)
  .addCommand(graphCommand)
  .addCommand(mcpCommand)
  .addCommand(agentCommand);

// Check for CLI updates (non-blocking, background check)
async function checkVersionAsync(): Promise<void> {
  try {
    // Skip if suppressed (CI/CD environments)
    if (process.env.ORASAKA_SKIP_VERSION_CHECK === "true") {
      return;
    }

    const versionManager = new VersionManager(VERSION);

    // Don't block startup - check in background with timeout
    const checkPromise = versionManager.checkForUpdates();
    const timeoutPromise = new Promise<void>((resolve) => {
      setTimeout(() => resolve(), 2000); // 2 second timeout
    });

    const result = await Promise.race([
      checkPromise.then((info) => ({ success: true as const, info })),
      timeoutPromise.then(() => ({ success: false as const, info: undefined })),
    ]);

    if (result.success && result.info) {
      versionManager.displayUpdateNotification(result.info);
    }
  } catch {
    // Silently fail - don't break CLI if version check fails
  }
}

// Graceful execution entry point
async function main(): Promise<void> {
  // Check for updates (non-blocking)
  checkVersionAsync().catch(() => {
    // Ignore errors from version check
  });

  await program.parseAsync(process.argv);
}

main().catch((err) => {
  console.error(chalk.red(`\n❌ Fatal Error: ${err instanceof Error ? err.message : String(err)}`));
  process.exit(1);
});
