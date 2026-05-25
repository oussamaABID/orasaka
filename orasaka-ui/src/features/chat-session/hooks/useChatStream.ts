/**
 * @file useChatStream.ts
 * @description Hook orchestrating chat mutation via GraphQL BFF and real-time SSE token streaming.
 * Composes {@link useThreadManagement} and {@link useMessageHistory} as focused sub-hooks.
 */

import { useState, useEffect, useRef } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ChatMessage, ChatResponse } from "@/features/chat-session/types/chat.types";
import { useThreadManagement, getStoredThreads, saveStoredThreads } from "@/features/chat-session/hooks/useThreadManagement";
import { useMessageHistory, saveStoredMessages } from "@/features/chat-session/hooks/useMessageHistory";

// ── GraphQL BFF API ─────────────────────────────────────────────────────────

const SEND_CHAT_MUTATION = `
  mutation SendChat($prompt: String!, $conversationId: String) {
    chat(prompt: $prompt, conversationId: $conversationId) {
      content
      conversationId
    }
  }
`;

/**
 * Posts a chat message payload to the BFF GraphQL API gateway.
 *
 * @async
 * @param {Object} variables - GraphQL arguments.
 * @param {string} variables.prompt - The plain text prompt to submit.
 * @param {string} variables.conversationId - The session identifier target.
 * @returns {Promise<ChatResponse>} The parsed GraphQL response structure.
 * @throws {Error} If the network request fails or GraphQL returns errors.
 */
const postChatMessage = async (variables: {
  prompt: string;
  conversationId: string;
}): Promise<ChatResponse> => {
  const response = await fetch("/api/graphql", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query: SEND_CHAT_MUTATION, variables }),
  });

  if (!response.ok) throw new Error("Failed to post message to Orasaka Gateway");

  const result = await response.json();
  if (result.errors?.length > 0) throw new Error(result.errors[0].message ?? "GraphQL Mutation failed");

  return result.data.chat;
};

// ── Message Factories ────────────────────────────────────────────────────────

const createUserMessage = (prompt: string): ChatMessage => ({
  id: `user-${Date.now()}`,
  role: "user",
  content: prompt,
  timestamp: Date.now(),
  kind: "text",
});

const createAssistantMessage = (content: string): ChatMessage => ({
  id: `assistant-mutation-${Date.now()}`,
  role: "assistant",
  content,
  timestamp: Date.now(),
  kind: "text",
});

const generateAssistantMsgId = (): string => `assistant-${Date.now()}`;

// ── SSE Stream ───────────────────────────────────────────────────────────────

const parseChunk = (eventData: string): string => {
  try {
    const parsed = JSON.parse(eventData);
    return parsed.content ?? parsed.text ?? "";
  } catch {
    return eventData;
  }
};

// ── Hook ─────────────────────────────────────────────────────────────────────

/**
 * Orchestrating hook providing message mutation, SSE streaming, thread management, and message history.
 *
 * @param {string} conversationId - The active target conversation thread ID.
 * @returns An object containing threads, messages, loading states, SSE streaming toggle, and mutator handlers.
 */
export function useChatStream(conversationId: string) {
  const queryClient = useQueryClient();

  const { threads, isLoadingThreads, createThread } = useThreadManagement();
  const { messages, isLoadingMessages, queryKey } = useMessageHistory(conversationId);

  const [isStreaming, setIsStreaming] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    return () => {
      eventSourceRef.current?.close();
      eventSourceRef.current = null;
      setIsStreaming(false);
    };
  }, [conversationId]);

  const startStreaming = (convId: string, prompt: string): void => {
    eventSourceRef.current?.close();
    setIsStreaming(true);

    const assistantMsgId = generateAssistantMsgId();
    let accumulatedContent = "";

    const eventSource = new EventSource(`/api/chat/stream/${convId}?prompt=${encodeURIComponent(prompt)}`);
    eventSourceRef.current = eventSource;

    eventSource.onmessage = (event) => {
      const chunk = parseChunk(event.data);
      if (chunk) accumulatedContent += chunk;
      if (!accumulatedContent) return;

      queryClient.setQueryData<ChatMessage[]>(queryKey, (old = []) => {
        const lastMsg = old[old.length - 1];
        const updated: ChatMessage[] =
          lastMsg?.role === "assistant"
            ? [...old.slice(0, -1), { ...lastMsg, content: accumulatedContent, kind: "text" }]
            : [...old, { id: assistantMsgId, role: "assistant", content: accumulatedContent, timestamp: Date.now(), kind: "text" }];
        saveStoredMessages(convId, updated);
        return updated;
      });
    };

    eventSource.onerror = () => {
      eventSource.close();
      if (eventSourceRef.current === eventSource) eventSourceRef.current = null;
      setIsStreaming(false);
      updateThreadTitle(convId, prompt);
      queryClient.invalidateQueries({ queryKey: ["chatThreads"] });
      queryClient.invalidateQueries({ queryKey: ["operationGraph"] });
    };
  };

  const updateThreadTitle = (convId: string, prompt: string): void => {
    const title = prompt.length > 40 ? `${prompt.substring(0, 40)}...` : prompt;
    const updated = getStoredThreads().map((t) =>
      t.conversationId === convId ? { ...t, title, updatedAt: Date.now() } : t,
    );
    saveStoredThreads(updated);
  };

  const mutation = useMutation({
    mutationFn: postChatMessage,
    onMutate: async ({ prompt }) => {
      await queryClient.cancelQueries({ queryKey });
      const previousMessages = queryClient.getQueryData<ChatMessage[]>(queryKey) ?? [];
      const updated = [...previousMessages, createUserMessage(prompt)];
      queryClient.setQueryData(queryKey, updated);
      saveStoredMessages(conversationId, updated);
      return { previousMessages };
    },
    onError: (_err, _vars, context) => {
      if (context?.previousMessages) {
        queryClient.setQueryData(queryKey, context.previousMessages);
        saveStoredMessages(conversationId, context.previousMessages);
      }
    },
    onSuccess: (data, variables) => {
      const convId = data?.conversationId ?? variables.conversationId;
      const content = data?.content ?? (data as unknown as { text?: string }).text ?? "";

      if (content) {
        queryClient.setQueryData<ChatMessage[]>(queryKey, (old = []) => {
          const exists = old.some((m) => m.role === "assistant" && m.content === content);
          if (exists) return old;
          const updated = [...old, createAssistantMessage(content)];
          saveStoredMessages(convId, updated);
          return updated;
        });
      }

      queryClient.invalidateQueries({ queryKey: ["operationGraph"] });
      startStreaming(convId, variables.prompt);
    },
  });

  return {
    threads,
    isLoadingThreads,
    messages,
    isLoadingMessages,
    sendMessage: (prompt: string) => mutation.mutate({ prompt, conversationId }),
    isSending: mutation.isPending,
    isStreaming,
    isGenerating: mutation.isPending || isStreaming,
    error: mutation.error,
    createThread,
  };
}
