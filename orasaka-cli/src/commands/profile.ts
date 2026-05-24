/**
 * @file commands/profile.ts
 * @description Displays the authenticated user's full profile via the `me` GraphQL query.
 */

import { CliClient } from '../client';
import { requireAuth } from '../threads';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

/**
 * Displays the user's profile card.
 */
export async function handleProfile(): Promise<void> {
  const config = requireAuth();
  const client = new CliClient(`${GATEWAY_URL}/graphql`, config.token);

  try {
    const user = await client.getMe();

    console.log(`\n\x1b[1m\x1b[36m┌─────────────────────────────────────────────┐\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m│  🥷 ORASAKA USER PROFILE                     │\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m├─────────────────────────────────────────────┤\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m│\x1b[0m  Username:    \x1b[1m${(user.username || '').padEnd(28)}\x1b[0m \x1b[1m\x1b[36m│\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m│\x1b[0m  Email:       ${(user.email || '').padEnd(28)} \x1b[1m\x1b[36m│\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m│\x1b[0m  ID:          \x1b[90m${(user.id || '').padEnd(28)}\x1b[0m \x1b[1m\x1b[36m│\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m│\x1b[0m  Authorities: \x1b[33m${(user.authorities?.join(', ') || 'None').padEnd(28)}\x1b[0m \x1b[1m\x1b[36m│\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m├─────────────────────────────────────────────┤\x1b[0m`);

    const prefs = user.preferences || {};
    const entries = Object.entries(prefs);
    if (entries.length > 0) {
      console.log(`\x1b[1m\x1b[36m│\x1b[0m  \x1b[1mPreferences:\x1b[0m${' '.repeat(31)}\x1b[1m\x1b[36m│\x1b[0m`);
      for (const [k, v] of entries) {
        const line = `  ${k}: ${v}`;
        console.log(`\x1b[1m\x1b[36m│\x1b[0m  \x1b[32m${line.padEnd(40)}\x1b[0m \x1b[1m\x1b[36m│\x1b[0m`);
      }
    } else {
      console.log(`\x1b[1m\x1b[36m│\x1b[0m  \x1b[90mNo preferences configured.${' '.repeat(16)}\x1b[0m \x1b[1m\x1b[36m│\x1b[0m`);
    }

    console.log(`\x1b[1m\x1b[36m└─────────────────────────────────────────────┘\x1b[0m\n`);
  } catch (error: any) {
    console.error(`\x1b[31mFailed to retrieve profile: ${error.message}\x1b[0m`);
  }
}
