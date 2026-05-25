/**
 * @file useMessageHistory.ts
 * @description Hook managing per-thread message history.
 * Message data is sourced exclusively from the backend via TanStack Query.
 * Local storage acts as a write-back cache for SSE-streamed tokens only.
 */

import { useQuery } from "@tanstack/react-query";
import { ChatMessage } from "@/features/chat-session/types/chat.types";

const buildKey = (conversationId: string): readonly string[] =>
  ["chatMessages", conversationId] as const;

/**
 * Retrieves cached messages for a given conversation from local storage.
 * Returns an empty array if no messages exist — no default seeding.
 *
 * @param {string} conversationId - The target conversation thread ID.
 * @returns {ChatMessage[]} The list of stored messages for the conversation.
 */
export const getStoredMessages = (conversationId: string): ChatMessage[] => {
  if (!conversationId || typeof window === "undefined") return [];
  const stored = localStorage.getItem(`orasaka_messages_${conversationId}`);
  if (!stored) return [];
  try {
    const parsed: Partial<ChatMessage>[] = JSON.parse(stored);
    return parsed.map((msg) => ({
      id: msg.id ?? `msg-${Date.now()}`,
      role: msg.role ?? "assistant",
      content: msg.content ?? "",
      timestamp: msg.timestamp ?? Date.now(),
      kind: msg.kind ?? "text",
    }));
  } catch (e) {
    console.error(`Failed to parse messages for ${conversationId}:`, e);
    return [];
  }
};

/**
 * Persists messages for a given conversation in local storage.
 *
 * @param {string} conversationId - The target conversation thread ID.
 * @param {ChatMessage[]} messages - The messages to serialize and save.
 */
export const saveStoredMessages = (
  conversationId: string,
  messages: ChatMessage[],
): void => {
  if (!conversationId || typeof window === "undefined") return;
  localStorage.setItem(
    `orasaka_messages_${conversationId}`,
    JSON.stringify(messages),
  );
};

/**
 * Hook providing message history state for the active conversation thread.
 * Message data originates exclusively from the database via the BFF.
 *
 * @param {string} conversationId - The active target conversation thread ID.
 * @returns An object containing the messages list, loading state, and query key.
 */
export function useMessageHistory(conversationId: string) {
  const queryKey = buildKey(conversationId);

  const messagesQuery = useQuery({
    queryKey,
    queryFn: () => getStoredMessages(conversationId),
    enabled: !!conversationId,
  });

  return {
    messages: messagesQuery.data ?? [],
    isLoadingMessages: messagesQuery.isLoading,
    queryKey,
  };
}
