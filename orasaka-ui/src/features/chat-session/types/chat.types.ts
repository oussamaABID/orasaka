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

/**
 * Represents a single message in a chat conversation.
 */
export interface ChatMessage {
  id: string;
  role: "user" | "assistant" | "system";
  content: string;
  timestamp: number;
}

/**
 * Thread item metadata representing a single conversation history session.
 */
export interface ChatThread {
  conversationId: string;
  title: string;
  updatedAt: number;
}
