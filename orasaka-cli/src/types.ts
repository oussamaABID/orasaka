/**
 * @file types.ts
 * @module orasaka-cli/types
 * @description Centralized TypeScript type definitions for the Orasaka CLI.
 *
 * This module acts as the **single source of truth** for all data structures
 * shared across the CLI codebase. Types are organized into the following
 * logical domains:
 *
 * - **GraphQL Schema Types** — Mirror the gateway's GraphQL schema (`schema.graphqls`).
 * - **Operation Graph (SDUI)** — Server-Driven UI capability nodes with polymorphic states.
 * - **Timeline & Rendering** — Discriminated union messages for the multi-modal output pipeline.
 * - **Capability Descriptors** — Metadata binding CLI flags to Operation Graph capabilities.
 * - **Chat Input** — Parsed command-line input for chat execution.
 * - **Local Persistence** — Disk-persisted thread and configuration structures.
 *
 * All interfaces use `readonly` fields to enforce immutability at the type level,
 * following the self-validating record pattern mandated by ADR-007.
 */

// ── GraphQL Schema Types ─────────────────────────────────────────────────────

/**
 * Response returned by the gateway's GraphQL `chat`, `image`, and `speech` mutations,
 * as well as the `chatStream` subscription.
 *
 * @property content - The primary payload: text content, RFC 2397 Data URL for images,
 *   or `data:audio/mp3;base64,...` for speech output.
 * @property conversationId - The server-assigned conversation thread identifier.
 * @property metadata - Optional key-value metadata attached by the engine (e.g., model, tokens used).
 */
export interface ChatResponse {
  /** The primary response payload (text, Data URL, or base64 audio). */
  readonly content: string;
  /** Server-assigned conversation thread identifier. */
  readonly conversationId?: string;
  /** Optional engine metadata (e.g., model name, token count). */
  readonly metadata?: Record<string, unknown>;
}

/**
 * User profile returned by the `me` GraphQL query.
 * Maps directly to the `orasaka-identity` `User` domain record.
 *
 * @property id - The UUID primary key from the identity database.
 * @property username - The user's chosen display name.
 * @property email - The user's email address (unique constraint).
 * @property authorities - RBAC authority strings (e.g., `ROLE_USER`, `ROLE_ADMIN`).
 * @property preferences - Key-value user preferences map, or null if none are set.
 */
export interface UserProfile {
  /** UUID primary key from the identity database. */
  readonly id: string;
  /** User's chosen display name. */
  readonly username: string;
  /** User's email address (unique constraint). */
  readonly email: string;
  /** RBAC authority strings (e.g., `ROLE_USER`, `ROLE_ADMIN`). */
  readonly authorities: readonly string[];
  /** Key-value user preferences map, or `null` if none are configured. */
  readonly preferences: Record<string, unknown> | null;
}

/**
 * Result of the `register` GraphQL mutation.
 * Contains either a successfully created user or an error message — never both.
 *
 * @property user - The newly created user profile, or `null` on failure.
 * @property error - A human-readable error message, or `null` on success.
 */
export interface RegisterResult {
  /** The newly created user profile, or `null` on failure. */
  readonly user: UserProfile | null;
  /** Human-readable error message, or `null` on success. */
  readonly error: string | null;
}

// ── Operation Graph (SDUI) ───────────────────────────────────────────────────

/**
 * String literal union for the three polymorphic states an Operation Graph
 * node can occupy at any point in time.
 *
 * - `'ACTIVE'` — The capability is available for execution.
 * - `'LOCKED'` — The capability is temporarily disabled (with a reason string).
 * - `'INVISIBLE'` — The capability is hidden from the user entirely.
 */
export type NodeStateType = 'ACTIVE' | 'LOCKED' | 'INVISIBLE';

/**
 * State variant indicating the capability is available for execution.
 * No additional metadata is required.
 */
export interface ActiveNodeState {
  /** Discriminator tag. Always `'ACTIVE'`. */
  readonly type: 'ACTIVE';
}

/**
 * State variant indicating the capability is temporarily locked.
 * Includes a human-readable reason and an optional ISO-8601 timestamp.
 *
 * @property reason - Why the capability is locked (e.g., "Rate limit exceeded").
 * @property lockedAt - Optional ISO-8601 timestamp of when the lock was applied.
 */
export interface LockedNodeState {
  /** Discriminator tag. Always `'LOCKED'`. */
  readonly type: 'LOCKED';
  /** Human-readable explanation for the lock. */
  readonly reason: string;
  /** Optional ISO-8601 timestamp of when the lock was applied. */
  readonly lockedAt?: string;
}

/**
 * State variant indicating the capability is hidden from the user.
 * Invisible nodes are filtered out of CLI rendering entirely.
 */
export interface InvisibleNodeState {
  /** Discriminator tag. Always `'INVISIBLE'`. */
  readonly type: 'INVISIBLE';
}

/**
 * Discriminated union of all possible Operation Graph node states.
 * Use `node.state.type` as the discriminator in switch/if blocks.
 */
export type NodeState = ActiveNodeState | LockedNodeState | InvisibleNodeState;

/**
 * Execution metadata for an Operation Graph node, defining how the CLI
 * should invoke the corresponding gateway endpoint.
 *
 * @property uriPath - The relative URI path on the gateway (e.g., `/api/v1/chat/stream`).
 * @property httpMethod - The HTTP method to use (e.g., `GET`, `POST`).
 * @property payloadTemplate - Optional JSON template with `${param}` placeholders
 *   that the CLI substitutes before sending.
 */
export interface TargetExecutionUri {
  /** Relative URI path on the gateway (e.g., `/api/v1/chat/stream`). */
  readonly uriPath: string;
  /** HTTP method to use (e.g., `GET`, `POST`). */
  readonly httpMethod: string;
  /** Optional JSON payload template with `${param}` placeholders. */
  readonly payloadTemplate?: string;
}

/**
 * A single node in the Operation Graph, representing one AI capability
 * exposed by the Orasaka engine. Each node carries its current state,
 * presentation metadata, and execution details.
 *
 * @property id - Canonical capability identifier (e.g., `orasaka.core.chat.text`).
 * @property label - Human-readable display label (e.g., "Text Chat").
 * @property icon - Icon identifier for UI rendering (unused in CLI, reserved for SDUI parity).
 * @property presentationContext - UI layout hint (e.g., `'primary'`, `'secondary'`).
 * @property state - The current polymorphic state of this capability.
 * @property executionDetails - How to invoke this capability on the gateway.
 */
export interface OperationNode {
  /** Canonical capability identifier (e.g., `orasaka.core.chat.text`). */
  readonly id: string;
  /** Human-readable display label. */
  readonly label: string;
  /** Icon identifier (reserved for SDUI parity). */
  readonly icon: string;
  /** UI layout hint (e.g., `'primary'`, `'secondary'`). */
  readonly presentationContext: string;
  /** Current polymorphic state of this capability. */
  readonly state: NodeState;
  /** Execution metadata for invoking this capability on the gateway. */
  readonly executionDetails: TargetExecutionUri;
}

// ── Timeline & Rendering ─────────────────────────────────────────────────────

/**
 * A timeline message containing plain text content.
 * Rendered directly to stdout via `console.log`.
 */
export interface TextTimelineMessage {
  /** Discriminator tag. Always `'text'`. */
  readonly kind: 'text';
  /** The plain text content. */
  readonly content: string;
}

/**
 * A timeline message containing image content.
 * Content is typically an RFC 2397 Data URL that gets decoded and saved to disk.
 */
export interface ImageTimelineMessage {
  /** Discriminator tag. Always `'image'`. */
  readonly kind: 'image';
  /** Image content — RFC 2397 Data URL or a plain text description. */
  readonly content: string;
}

/**
 * A timeline message containing audio content.
 * Content is typically a `data:audio/mp3;base64,...` Data URL.
 */
export interface AudioTimelineMessage {
  /** Discriminator tag. Always `'audio'`. */
  readonly kind: 'audio';
  /** Audio content — RFC 2397 Data URL or a plain text link. */
  readonly content: string;
}

/**
 * A timeline message containing video content.
 * Content is typically a `data:video/mp4;base64,...` Data URL.
 */
export interface VideoTimelineMessage {
  /** Discriminator tag. Always `'video'`. */
  readonly kind: 'video';
  /** Video content — RFC 2397 Data URL or a plain text link. */
  readonly content: string;
}

/**
 * Discriminated union of all supported timeline message types.
 * The `kind` field acts as the discriminator for exhaustive switch matching
 * in the {@link renderTimeline} function.
 */
export type TimelineMessage =
  | TextTimelineMessage
  | ImageTimelineMessage
  | AudioTimelineMessage
  | VideoTimelineMessage;

// ── Capability Descriptors ───────────────────────────────────────────────────

/**
 * Metadata binding a CLI command flag to an Operation Graph capability.
 * Used by the chat command to resolve user flags (e.g., `--gen-image`)
 * to the correct execution path (GraphQL or SDUI REST).
 *
 * @property id - The canonical Operation Graph node ID (e.g., `orasaka.core.chat.image`).
 * @property flag - The CLI flag string (e.g., `--gen-image`, `--speech`).
 * @property argName - The display name for the flag's argument in help text.
 * @property description - A brief description of the capability.
 * @property renderKind - Which {@link TimelineMessage} kind to use for rendering the response.
 * @property responseField - Which field of the API response JSON contains the payload.
 * @property processExtraParams - Optional async function to preprocess flag values
 *   (e.g., reading a file from disk and encoding it as base64).
 */
export interface CapabilityDescriptor {
  /** Canonical Operation Graph node ID. */
  readonly id: string;
  /** CLI flag string (e.g., `--gen-image`). */
  readonly flag: string;
  /** Display name for the flag's argument in help text. */
  readonly argName: string;
  /** Brief description of the capability. */
  readonly description: string;
  /** Timeline message kind used for rendering the response. */
  readonly renderKind: TimelineMessage['kind'];
  /** API response JSON field containing the payload. */
  readonly responseField: 'content' | 'analysis';
  /**
   * Optional async preprocessor that transforms the raw flag value
   * into extra parameters for the API request.
   *
   * @param val - The raw flag value (e.g., a file path).
   * @param conversationId - The active conversation thread ID.
   * @returns A map of extra parameters to merge into the request payload.
   */
  readonly processExtraParams?: (
    val: string,
    conversationId?: string,
  ) => Promise<Record<string, string>> | Record<string, string>;
}

// ── Chat Input ───────────────────────────────────────────────────────────────

/**
 * Parsed representation of a chat command invocation.
 * Produced by the `parseChatInput` function in the chat command module.
 *
 * @property flag - The matched capability flag (e.g., `--gen-image`), or undefined for text chat.
 * @property flagValue - The value following the flag (e.g., a file path or prompt), or undefined.
 * @property prompt - The remaining positional arguments joined as a single prompt string.
 */
export interface ChatInput {
  /** The matched capability flag, or `undefined` for default text chat. */
  readonly flag?: string;
  /** The value following the flag, or `undefined`. */
  readonly flagValue?: string;
  /** Remaining positional arguments joined as a single prompt string. */
  readonly prompt: string;
}

// ── Local Persistence ────────────────────────────────────────────────────────

/**
 * A single conversation thread entry stored in the local CLI configuration.
 * Analogous to the `orasaka-ui` `useThreadManagement` hook's thread model.
 *
 * @property conversationId - UUID identifying this thread (matches the gateway's conversation ID).
 * @property title - Display title, auto-set from the first user message.
 * @property updatedAt - Unix timestamp (ms) of the last activity in this thread.
 */
export interface ChatThread {
  /** UUID identifying this thread. */
  readonly conversationId: string;
  /** Display title, auto-set from the first user message. */
  readonly title: string;
  /** Unix timestamp (ms) of the last activity in this thread. */
  readonly updatedAt: number;
}

/**
 * A single message entry within a thread's persistent history file.
 * Written to `~/.orasaka-threads/<conversationId>.json`.
 *
 * @property role - The message author: `'user'` or `'assistant'`.
 * @property content - The message content (text, Data URL, or base64 payload).
 * @property kind - The media type of the content, matching {@link TimelineMessage} kinds.
 * @property timestamp - Unix timestamp (ms) when the message was created.
 */
export interface StoredMessage {
  /** The message author: `'user'` or `'assistant'`. */
  readonly role: 'user' | 'assistant';
  /** The message content (text, Data URL, or base64 payload). */
  readonly content: string;
  /** Media type of the content. */
  readonly kind: TimelineMessage['kind'];
  /** Unix timestamp (ms) when the message was created. */
  readonly timestamp: number;
}

/**
 * CLI configuration persisted to `~/.orasaka-cli.json`.
 * Contains the authenticated session token, user identity,
 * and the local thread management state.
 *
 * @property token - The JWT Bearer token obtained via the `login` command.
 * @property username - The authenticated user's display name.
 * @property activeThreadId - UUID of the currently active conversation thread.
 * @property threads - Array of all known conversation threads, most recent first.
 */
export interface CliConfig {
  /** JWT Bearer token obtained via the `login` command. */
  readonly token: string;
  /** Authenticated user's display name. */
  readonly username: string;
  /** UUID of the currently active conversation thread. */
  readonly activeThreadId: string;
  /** All known conversation threads, most recent first. */
  readonly threads: ChatThread[];
}
