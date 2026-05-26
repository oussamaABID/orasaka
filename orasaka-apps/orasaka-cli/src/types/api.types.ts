/**
 * @file api.types.ts
 * @description GraphQL and REST API response types and contracts for Orasaka.
 */

// ── GraphQL Schema Types ─────────────────────────────────────────────────────

export interface ChatResponse {
  readonly content: string;
  readonly conversationId?: string;
  readonly metadata?: Record<string, unknown>;
}

export interface UserProfile {
  readonly id: string;
  readonly username: string;
  readonly email: string;
  readonly authorities: readonly string[];
  readonly preferences: Record<string, unknown> | null;
}

export interface RegisterResult {
  readonly user: UserProfile | null;
  readonly error: string | null;
}

// ── Operation Graph (SDUI) ───────────────────────────────────────────────────

export type NodeStateType = 'ACTIVE' | 'LOCKED' | 'INVISIBLE';

export interface ActiveNodeState {
  readonly type: 'ACTIVE';
}

export interface LockedNodeState {
  readonly type: 'LOCKED';
  readonly reason: string;
  readonly lockedAt?: string;
}

export interface InvisibleNodeState {
  readonly type: 'INVISIBLE';
}

export type NodeState = ActiveNodeState | LockedNodeState | InvisibleNodeState;

export interface TargetExecutionUri {
  readonly uriPath: string;
  readonly httpMethod: string;
  readonly payloadTemplate?: string;
}

export interface OperationNode {
  readonly id: string;
  readonly label: string;
  readonly icon: string;
  readonly presentationContext: string;
  readonly state: NodeState;
  readonly executionDetails: TargetExecutionUri;
}

// ── Timeline & Rendering ─────────────────────────────────────────────────────

export interface TextTimelineMessage {
  readonly kind: 'text';
  readonly content: string;
}

export interface ImageTimelineMessage {
  readonly kind: 'image';
  readonly content: string;
}

export interface AudioTimelineMessage {
  readonly kind: 'audio';
  readonly content: string;
}

export interface VideoTimelineMessage {
  readonly kind: 'video';
  readonly content: string;
}

export type TimelineMessage =
  | TextTimelineMessage
  | ImageTimelineMessage
  | AudioTimelineMessage
  | VideoTimelineMessage;

// ── Capability Descriptors ───────────────────────────────────────────────────

export interface CapabilityDescriptor {
  readonly id: string;
  readonly flag: string;
  readonly argName: string;
  readonly description: string;
  readonly renderKind: TimelineMessage['kind'];
  readonly responseField: 'content' | 'analysis';
  readonly processExtraParams?: (
    val: string,
    conversationId?: string,
  ) => Promise<Record<string, string>> | Record<string, string>;
}

export interface ChatInput {
  readonly flag?: string;
  readonly flagValue?: string;
  readonly prompt: string;
}
