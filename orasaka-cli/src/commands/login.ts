/**
 * @file commands/login.ts
 * @description Handles user authentication via the gateway REST API.
 */

import * as readline from 'readline';
import * as crypto from 'crypto';
import { saveConfig } from '../threads';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

/**
 * Prompts the user for input, optionally masking the response.
 *
 * @param query - The prompt text.
 * @param secret - If true, masks the input with asterisks.
 * @returns The trimmed user input.
 */
export async function promptInput(query: string, secret = false): Promise<string> {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  });

  return new Promise((resolve) => {
    if (secret) {
      (rl as any)._writeToOutput = (stringToWrite: string) => {
        const isNewline = stringToWrite === '\r' || stringToWrite === '\n' || stringToWrite === '\r\n';
        process.stdout.write(isNewline ? stringToWrite : '*');
      };
    }
    rl.question(query, (answer: string) => {
      rl.close();
      resolve(answer.trim());
    });
  });
}

/**
 * Executes the login flow: authenticates against the gateway and caches the JWT token.
 *
 * @param args - CLI arguments: [email, password].
 */
export async function handleLogin(args: string[]): Promise<void> {
  const emailArg = args[0];
  const passwordArg = args[1];
  const email = emailArg || (await promptInput('\x1b[1mEnter Email: \x1b[0m'));
  const password = passwordArg || (await promptInput('\x1b[1mEnter Password: \x1b[0m', true));

  if (!email || !password) {
    console.error('\x1b[31mError: Email and password are required.\x1b[0m');
    process.exit(1);
  }

  console.log(`\x1b[36mAuthenticating user "${email}" against ${GATEWAY_URL}...\x1b[0m`);

  try {
    const response = await fetch(`${GATEWAY_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      const errBody = (await response.json().catch(() => ({}))) as any;
      throw new Error(errBody.error || `HTTP ${response.status}`);
    }

    const data = (await response.json()) as { token: string; username: string };
    const threadId = crypto.randomUUID();

    saveConfig({
      token: data.token,
      username: data.username,
      activeThreadId: threadId,
      threads: [{ conversationId: threadId, title: 'New Memory Block', updatedAt: Date.now() }],
    });

    console.log(`\x1b[32m✓ Login successful! Token cached for user: ${data.username}\x1b[0m`);
    console.log(`\x1b[90m  Active thread: ${threadId}\x1b[0m`);
  } catch (error: any) {
    console.error(`\x1b[31mAuthentication Failed: ${error.message}\x1b[0m`);
    process.exit(1);
  }
}
