/**
 * Parsed arguments from CLI input for chat command execution.
 */
export interface ParsedArgs {
  readonly flag?: string;
  readonly flagValue?: string;
  readonly prompt: string;
  readonly savePath?: string;
  readonly model?: string;
  readonly voice?: string;
}

/**
 * Capability descriptor mapping CLI flags to Operation Graph nodes.
 */
export interface CapabilityDescriptor {
  readonly flag: string;
  readonly id: string;
  readonly renderKind: string;
  readonly responseField: string;
}

/**
 * Registry of all supported chat capabilities.
 */
export const CAPABILITIES: readonly CapabilityDescriptor[] = [
  { flag: "--text", id: "orasaka.core.chat.text", renderKind: "text", responseField: "content" },
  { flag: "--image", id: "orasaka.core.media.vision", renderKind: "image", responseField: "analysis" },
  { flag: "--audio", id: "orasaka.core.media.audio", renderKind: "audio", responseField: "analysis" },
  { flag: "--gen-image", id: "orasaka.core.chat.image", renderKind: "image", responseField: "content" },
  { flag: "--speech", id: "orasaka.core.chat.speech", renderKind: "audio", responseField: "content" },
] as const;

/**
 * Resolves the active flag from commander options.
 * Eliminates nested ternary chains (S3358).
 */
export function resolveActiveFlag(options: {
  readonly text?: boolean;
  readonly image?: string;
  readonly audio?: string;
  readonly genImage?: boolean;
  readonly speech?: boolean;
}): string | undefined {
  if (options.image) return "--image";
  if (options.audio) return "--audio";
  if (options.genImage) return "--gen-image";
  if (options.speech) return "--speech";
  if (options.text) return "--text";
  return undefined;
}

/**
 * Resolves the flag value for media flags (path-based).
 */
export function resolveFlagValue(options: {
  readonly image?: string;
  readonly audio?: string;
}): string | undefined {
  return options.image || options.audio;
}
