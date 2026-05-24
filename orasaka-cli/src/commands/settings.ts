/**
 * @file commands/settings.ts
 * @description User preference management via GraphQL mutations.
 * Fixed: The `set` subcommand no longer performs a redundant GET before SET,
 * as the server-side `updatePreferences` mutation merges atomically (ERR-200 compliance).
 */

import { CliClient } from '../client';
import { requireAuth } from '../threads';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

/**
 * Handles the `settings` command with `get` and `set` subcommands.
 *
 * @param args - CLI arguments: [subcommand, key?, value?].
 */
export async function handleSettings(args: string[]): Promise<void> {
  const config = requireAuth();
  const client = new CliClient(`${GATEWAY_URL}/graphql`, config.token);
  const subCommand = args[0];

  if (subCommand === 'get') {
    try {
      const user = await client.getMe();
      console.log(`\n\x1b[1m\x1b[36m--- Orasaka Settings for User: ${user.username} ---\x1b[0m`);
      console.log(`\x1b[33mUser ID:\x1b[0m     ${user.id}`);
      console.log(`\x1b[33mEmail:\x1b[0m       ${user.email}`);
      console.log(`\x1b[33mAuthorities:\x1b[0m ${user.authorities?.join(', ') || 'None'}`);
      console.log(`\x1b[33mPreferences:\x1b[0m`);
      const prefs = user.preferences || {};
      for (const [k, v] of Object.entries(prefs)) {
        console.log(`  \x1b[32m${k}:\x1b[0m ${v}`);
      }
      console.log();
    } catch (error: any) {
      console.error(`\x1b[31mFailed to retrieve settings: ${error.message}\x1b[0m`);
    }
  } else if (subCommand === 'set') {
    const key = args[1];
    const rawValue = args[2];
    if (!key || rawValue === undefined) {
      console.error('\x1b[31mUsage: settings set <key> <value>\x1b[0m');
      process.exit(1);
    }

    // Coerce boolean and numeric strings to native types
    const typedValue =
      rawValue.toLowerCase() === 'true'
        ? true
        : rawValue.toLowerCase() === 'false'
          ? false
          : !isNaN(Number(rawValue))
            ? Number(rawValue)
            : rawValue;

    try {
      // Direct atomic merge — no redundant GET required (server merges server-side)
      await client.updatePreferences({ [key]: typedValue });
      console.log(`\x1b[32m✓ Preference "${key}" updated to: ${rawValue}\x1b[0m`);
    } catch (error: any) {
      console.error(`\x1b[31mFailed to update preference: ${error.message}\x1b[0m`);
    }
  } else {
    console.error('\x1b[31mInvalid settings command. Use "get" or "set".\x1b[0m');
    console.error('\x1b[90mUsage: orasaka settings get\x1b[0m');
    console.error('\x1b[90m       orasaka settings set <key> <value>\x1b[0m');
  }
}
