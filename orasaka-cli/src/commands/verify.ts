/**
 * @file commands/verify.ts
 * @description Handles email verification token validation via the gateway REST API.
 */

import { promptInput } from './login';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

/**
 * Verifies a registration token against the gateway.
 *
 * @param args - CLI arguments: [token].
 */
export async function handleVerify(args: string[]): Promise<void> {
  let token = args[0];
  token = token || (await promptInput('\x1b[1mEnter Verification Token: \x1b[0m'));

  if (!token) {
    console.error('\x1b[31mError: Verification token is required.\x1b[0m');
    console.error('\x1b[90mUsage: orasaka verify <token>\x1b[0m');
    process.exit(1);
  }

  console.log(`\x1b[36mVerifying token...\x1b[0m`);

  try {
    const response = await fetch(`${GATEWAY_URL}/api/v1/auth/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
    });

    if (!response.ok) {
      const errBody = (await response.json().catch(() => ({}))) as any;
      throw new Error(errBody.error || `HTTP ${response.status}`);
    }

    console.log(`\x1b[32m✓ Email verified successfully!\x1b[0m`);
    console.log(`\x1b[33m  You can now login: orasaka login\x1b[0m`);
  } catch (error: any) {
    console.error(`\x1b[31mVerification Failed: ${error.message}\x1b[0m`);
    process.exit(1);
  }
}
