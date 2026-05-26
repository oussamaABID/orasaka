/**
 * @file sovereign-workflow-context.ts
 * @description Zod validation schemas mirroring the backend Java SovereignWorkflowContext record.
 * Shared between orasaka-web-client, orasaka-web-admin, and orasaka-mobile-client.
 */

import { z } from "zod";

/**
 * Zod schema for SovereignWorkflowContext — mirrors the Java record
 * at com.orasaka.business.domain.model.SovereignWorkflowContext.
 */
export const SovereignWorkflowContextSchema = z.object({
  contextId: z.string().min(1, "contextId must not be empty"),
  systemInstructions: z.string().min(1, "systemInstructions must not be empty"),
  userTier: z.string().default("DEFAULT"),
  forcedInterceptors: z.array(z.string()).default([]),
  skippedInterceptors: z.array(z.string()).default([]),
  metadata: z.record(z.string(), z.unknown()).default({}),
});

/** TypeScript type inferred from the Zod schema — always in sync with validation. */
export type SovereignWorkflowContext = z.infer<typeof SovereignWorkflowContextSchema>;

/**
 * Convenience factory matching the Java record's `minimal()` static method.
 */
export function minimalContext(
  contextId: string,
  systemInstructions: string,
): SovereignWorkflowContext {
  return SovereignWorkflowContextSchema.parse({
    contextId,
    systemInstructions,
    userTier: "DEFAULT",
    forcedInterceptors: [],
    skippedInterceptors: [],
    metadata: {},
  });
}
