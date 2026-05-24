/**
 * @file commands/register.ts
 * @description Handles self-service user registration via the gateway REST API.
 */

import { promptInput } from './login';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

/**
 * Executes the registration flow: creates a new user account.
 *
 * @param args - CLI arguments: [username, email, --language, langValue].
 */
export async function handleRegister(args: string[]): Promise<void> {
  let username = args[0];
  let email = args[1];
  let language: string | undefined;

  // Parse --language flag
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--language' && args[i + 1]) {
      language = args[i + 1];
    }
  }

  username = username || (await promptInput('\x1b[1mChoose Username: \x1b[0m'));
  email = email || (await promptInput('\x1b[1mEnter Email: \x1b[0m'));
  const password = await promptInput('\x1b[1mChoose Password: \x1b[0m', true);
  const confirmPassword = await promptInput('\x1b[1mConfirm Password: \x1b[0m', true);

  if (!username || !email || !password) {
    console.error('\x1b[31mError: Username, email, and password are required.\x1b[0m');
    process.exit(1);
  }

  if (password !== confirmPassword) {
    console.error('\x1b[31mError: Passwords do not match.\x1b[0m');
    process.exit(1);
  }

  console.log(`\x1b[36mRegistering user "${username}" (${email})...\x1b[0m`);

  try {
    const response = await fetch(`${GATEWAY_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password, language }),
    });

    if (!response.ok) {
      const errBody = (await response.json().catch(() => ({}))) as any;
      throw new Error(errBody.error || `HTTP ${response.status}`);
    }

    const data = (await response.json()) as any;
    if (data.error) {
      console.error(`\x1b[31mRegistration Failed: ${data.error}\x1b[0m`);
      process.exit(1);
    }

    console.log(`\x1b[32m✓ Registration successful for: ${data.user?.username || username}\x1b[0m`);
    console.log(`\x1b[33m  If email verification is enabled, check your inbox for a verification code.\x1b[0m`);
    console.log(`\x1b[33m  Then run: orasaka verify <token>\x1b[0m`);
  } catch (error: any) {
    console.error(`\x1b[31mRegistration Failed: ${error.message}\x1b[0m`);
    process.exit(1);
  }
}
