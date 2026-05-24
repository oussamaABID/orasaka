/**
 * @file commands/graph.ts
 * @description Displays the Operation Graph with capability states.
 */

import { CliClient } from '../client';
import { requireAuth } from '../threads';
import type { OperationNode } from '../types';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

/**
 * Fetches and renders the full Operation Graph capability matrix.
 */
export async function handleGraph(): Promise<void> {
  const config = requireAuth();
  const client = new CliClient(`${GATEWAY_URL}/graphql`, config.token);

  try {
    const nodes = await client.getOperationGraph();

    console.log(`\n\x1b[1m\x1b[36m┌─────────────────────────────────────────────────────────────┐\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m│  🥷 ORASAKA OPERATION GRAPH                                 │\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m├─────────────────────────────────────────────────────────────┤\x1b[0m`);

    const visibleNodes = nodes.filter((n: OperationNode) => n.state.type !== 'INVISIBLE');

    if (visibleNodes.length === 0) {
      console.log(`\x1b[1m\x1b[36m│\x1b[0m  \x1b[90mNo active capabilities returned by the engine.\x1b[0m${' '.repeat(12)}\x1b[1m\x1b[36m│\x1b[0m`);
    } else {
      for (const node of visibleNodes) {
        const stateTag =
          node.state.type === 'ACTIVE'
            ? '\x1b[32m● ACTIVE \x1b[0m'
            : '\x1b[31m○ LOCKED \x1b[0m';

        const label = node.label.padEnd(30);
        console.log(`\x1b[1m\x1b[36m│\x1b[0m  ${stateTag} ${label}  \x1b[90m${node.id}\x1b[0m`);

        if (node.state.type === 'LOCKED' && node.state.reason) {
          console.log(`\x1b[1m\x1b[36m│\x1b[0m           \x1b[90mReason: ${node.state.reason}\x1b[0m`);
        }

        const exec = node.executionDetails;
        console.log(`\x1b[1m\x1b[36m│\x1b[0m           \x1b[90m${exec.httpMethod} ${exec.uriPath}\x1b[0m`);
      }
    }

    console.log(`\x1b[1m\x1b[36m├─────────────────────────────────────────────────────────────┤\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m│\x1b[0m  Total: ${visibleNodes.length} visible / ${nodes.length} total capabilities${' '.repeat(20)}\x1b[1m\x1b[36m│\x1b[0m`);
    console.log(`\x1b[1m\x1b[36m└─────────────────────────────────────────────────────────────┘\x1b[0m\n`);
  } catch (error: any) {
    console.error(`\x1b[31mFailed to load Operation Graph: ${error.message}\x1b[0m`);
  }
}
