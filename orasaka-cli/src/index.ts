#!/usr/bin/env node
/**
 * @file index.ts
 * @module orasaka-cli
 * @description Orasaka CLI — Main entry point and command router.
 *
 * This module serves as the single entry point for the `orasaka` CLI binary.
 * It parses `process.argv`, identifies the requested command, and delegates
 * execution to the corresponding modular command handler under `./commands/`.
 *
 * The version string is dynamically read from the project's `package.json`
 * at startup, ensuring it always reflects the canonical published version
 * without requiring manual synchronization.
 *
 * @example
 * ```bash
 * # Single-shot text chat
 * orasaka chat "What is the weather?"
 *
 * # Interactive session
 * orasaka chat
 *
 * # Image generation
 * orasaka chat --gen-image "A cyberpunk cityscape" --save ./output.png
 *
 * # Show version
 * orasaka version
 * ```
 *
 * @see {@link handleChat} for the chat command implementation.
 * @see {@link handleLogin} for the authentication flow.
 */

import * as fs from 'fs';
import * as path from 'path';
import { handleLogin } from './commands/login';
import { handleRegister } from './commands/register';
import { handleVerify } from './commands/verify';
import { handleChat } from './commands/chat';
import { handleSettings } from './commands/settings';
import { handleProfile } from './commands/profile';
import { handleVideo } from './commands/video';
import { handleGraph } from './commands/graph';

/**
 * Reads the `version` field from the nearest `package.json` relative to this module.
 * Falls back to `'0.0.0'` if the file cannot be read or parsed.
 *
 * @returns The semantic version string from `package.json`.
 */
function getVersion(): string {
  try {
    const pkgPath = path.resolve(__dirname, '..', 'package.json');
    const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf-8'));
    return pkg.version || '0.0.0';
  } catch {
    return '0.0.0';
  }
}

/** The CLI version, dynamically sourced from `package.json`. */
const VERSION: string = getVersion();

/**
 * Prints the branded Orasaka CLI banner to stdout.
 * Includes the dynamically resolved version number.
 */
function printBanner(): void {
  console.log(`
\x1b[1m\x1b[36m   ╔═══════════════════════════════════════════╗
   ║                                           ║
   ║   🥷  O R A S A K A   C L I  v${VERSION}       ║
   ║       Orchestration Engine Terminal        ║
   ║                                           ║
   ╚═══════════════════════════════════════════╝\x1b[0m
`);
}

/**
 * Prints the full help text, including all available commands,
 * their flags, interactive meta-commands, and usage examples.
 * Calls {@link printBanner} first to display the branded header.
 */
function printHelp(): void {
  printBanner();
  console.log(`\x1b[1mUsage:\x1b[0m  orasaka <command> [options]

\x1b[1m\x1b[33mAuthentication:\x1b[0m
  login [email] [password]        Authenticate and cache JWT token
  register [user] [email]         Create a new account (interactive)
  verify <token>                  Verify email with registration token

\x1b[1m\x1b[33mChat & AI:\x1b[0m
  chat [prompt]                   Start interactive chat or single-shot query
    --gen-image <prompt>          Generate image via GraphQL mutation
    --speech <text>               Text-to-speech via GraphQL mutation
    --image <filepath>            Vision analysis (SDUI/REST)
    --audio <filepath>            Audio analysis (SDUI/REST)
    --save <filepath>             Save generated media to file

\x1b[1m\x1b[33mMedia:\x1b[0m
  video <prompt>                  Generate video from text prompt
    --duration <seconds>          Video duration (default: 4)
    --output <filepath>           Output file path

\x1b[1m\x1b[33mProfile & Settings:\x1b[0m
  profile                         View your user profile card
  settings get                    Display current preferences
  settings set <key> <value>      Update a preference atomically

\x1b[1m\x1b[33mEngine:\x1b[0m
  graph                           Display the Operation Graph capabilities

\x1b[1m\x1b[33mInteractive Chat Meta-Commands:\x1b[0m
  /thread new                     Create a new conversation thread
  /thread list                    List all conversation threads
  /thread switch <id>             Switch active thread

\x1b[1m\x1b[33mMisc:\x1b[0m
  help                            Show this help message
  version                         Show version info
`);
}

// ── Command Router ───────────────────────────────────────────────────────────

/**
 * Main entry point. Parses `process.argv` and routes to the
 * appropriate command handler based on the first positional argument.
 *
 * Supported commands:
 * - `login`    → {@link handleLogin}
 * - `register` → {@link handleRegister}
 * - `verify`   → {@link handleVerify}
 * - `chat`     → {@link handleChat}
 * - `settings` → {@link handleSettings}
 * - `profile`  → {@link handleProfile}
 * - `video`    → {@link handleVideo}
 * - `graph`    → {@link handleGraph}
 * - `version`  → Prints version string
 * - `help`     → Prints full usage text
 *
 * Unknown commands result in an error message and exit code `1`.
 *
 * @throws {Error} Propagated fatal errors are caught and printed to stderr.
 */
async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const command = args[0]?.toLowerCase();
  const commandArgs = args.slice(1);

  switch (command) {
    case 'login':
      await handleLogin(commandArgs);
      break;

    case 'register':
      await handleRegister(commandArgs);
      break;

    case 'verify':
      await handleVerify(commandArgs);
      break;

    case 'chat':
      await handleChat(commandArgs);
      break;

    case 'settings':
      await handleSettings(commandArgs);
      break;

    case 'profile':
      await handleProfile();
      break;

    case 'video':
      await handleVideo(commandArgs);
      break;

    case 'graph':
      await handleGraph();
      break;

    case 'version':
    case '--version':
    case '-v':
      console.log(`orasaka-cli v${VERSION}`);
      break;

    case 'help':
    case '--help':
    case '-h':
    case undefined:
      printHelp();
      break;

    default:
      console.error(`\x1b[31mUnknown command: "${command}"\x1b[0m`);
      console.error(`\x1b[90mRun "orasaka help" for available commands.\x1b[0m`);
      process.exit(1);
  }
}

main().catch((err) => {
  console.error(`\x1b[31mFatal Error: ${err.message}\x1b[0m`);
  process.exit(1);
});
