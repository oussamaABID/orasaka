/**
 * @file playground.types.ts
 * @description Canonical type definitions for the Operation Graph domain.
 * Single source of truth — supersedes duplicate definitions from
 * `app/playground/types.ts` and `ContextPlusMenu.tsx`.
 */

/**
 * Describes the target execution URI resolved from the Operation Graph backend.
 */
export interface TargetExecutionUri {
  uriPath: string;
  httpMethod: string;
  payloadTemplate?: string;
}

/**
 * Represents the visibility and access state of an operation node.
 */
export interface NodeState {
  type: "ACTIVE" | "LOCKED" | "INVISIBLE";
  reason?: string;
  lockedAt?: string;
}

/**
 * A single capability node in the Orasaka Operation Graph.
 */
export interface OperationNode {
  id: string;
  label: string;
  icon: string;
  presentationContext: string;
  state: NodeState;
  executionDetails: TargetExecutionUri;
}
