/**
 * User representation inside the chat session context.
 */
export interface User {
  id: string;
  username: string;
  preferences: Record<string, unknown>;
}

/**
 * Chat response wrapper returned by backend streaming or REST endpoints.
 */
export interface ChatResponse {
  content: string;
  conversationId: string;
  metadata?: Record<string, unknown>;
}

export interface BaseChatMessage {
  id: string;
  role: "user" | "assistant" | "system";
  timestamp: number;
}

export interface TextChatMessage extends BaseChatMessage {
  kind: "text";
  content: string;
}

export interface ImageChatMessage extends BaseChatMessage {
  kind: "image";
  content: string;
}

export interface AudioChatMessage extends BaseChatMessage {
  kind: "audio";
  content: string;
}

export type ChatMessage = TextChatMessage | ImageChatMessage | AudioChatMessage;

/**
 * Thread item metadata representing a single conversation history session.
 */
export interface ChatThread {
  conversationId: string;
  title: string;
  updatedAt: number;
}
