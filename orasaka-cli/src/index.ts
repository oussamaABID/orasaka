#!/usr/bin/env node
/**
 * @file index.ts
 * @description Main entry point and bootstrap registry for the Orasaka CLI.
 */

import { Command } from "commander";
import * as fs from "fs";
import * as path from "path";
import chalk from "chalk";

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
import { generateCommand } from "./commands/generate.command";
import { mcpCommand } from "./commands/mcp.command";


function getVersion(): string {
  try {
    const pkgPath = path.resolve(__dirname, "..", "package.json");
    const pkg = JSON.parse(fs.readFileSync(pkgPath, "utf-8")) as { version?: string };
    return pkg.version || "1.0.0";
  } catch {
    return "1.0.0";
  }
}

const VERSION = getVersion();

function getBanner(): string {
  return chalk.cyan.bold(`
   ╔═══════════════════════════════════════════╗
   ║                                           ║
   ║   🥷  O R A S A K A   C L I  v${VERSION.padEnd(6)}       ║
   ║       Orchestration Engine Terminal        ║
   ║                                           ║
   ╚═══════════════════════════════════════════╝
  `);
}

const program = new Command("orasaka");

program
  .version(VERSION)
  .description("Orasaka AI CLI — Terminal client for the Orasaka Orchestration Engine")
  .addHelpText("before", getBanner())
  .addCommand(loginCommand)
  .addCommand(registerCommand)
  .addCommand(verifyCommand)
  .addCommand(forgotCommand)
  .addCommand(resetCommand)
  .addCommand(chatCommand)
  .addCommand(settingsCommand)
  .addCommand(profileCommand)
  .addCommand(videoCommand)
  .addCommand(graphCommand)
  .addCommand(generateCommand)
  .addCommand(mcpCommand);

// Graceful execution entry point
async function main(): Promise<void> {
  await program.parseAsync(process.argv);
}

main().catch((err) => {
  console.error(chalk.red(`Fatal Error: ${err instanceof Error ? err.message : String(err)}`));
  process.exit(1);
});
