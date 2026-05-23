/**
 * @file useChatStream.ts
 * @description Hook managing conversation thread listings, message history, and real-time Server-Sent Events (SSE) chat streaming.
 * Uses TanStack React Query to fetch, mutate, and cache thread states and local storage for state backup.
 *
 * State & Lifecycle: Manages EventSource reference objects in component refs to prevent memory leaks during stream updates.
 * Optimization: Mutates TanStack caches optimistically to reflect prompt input instantly on submit.
 */

import { useState, useEffect, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ChatMessage, ChatResponse, ChatThread } from "../types/chat.types";

/**
 * Retrieves cached conversation threads from the browser's local storage.
 * Synchronizes with a default starter list if the store is empty.
 *
 * @returns {ChatThread[]} The list of stored conversation threads.
 */
const getStoredThreads = (): ChatThread[] => {
  if (typeof window === "undefined") return [];
  const stored = localStorage.getItem("orasaka_threads");
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (e) {
      console.error("Failed to parse stored threads:", e);
    }
  }
  const defaultThreads = [
    {
      conversationId: "thread-1",
      title: "Cybernetic Enhancements Discussion",
      updatedAt: Date.now(),
    },
    {
      conversationId: "thread-2",
      title: "Orasaka Security Architecture",
      updatedAt: Date.now() - 3600000,
    },
  ];
  localStorage.setItem("orasaka_threads", JSON.stringify(defaultThreads));
  return defaultThreads;
};

/**
 * Persists the current list of conversation threads in local storage.
 *
 * @param {ChatThread[]} threads - The list of threads to serialize and save.
 */
const saveStoredThreads = (threads: ChatThread[]) => {
  if (typeof window === "undefined") return;
  saveStoredThreadsInternal(threads);
};

/**
 * Internal storage persistence helper.
 *
 * @param {ChatThread[]} threads - The threads to serialize.
 */
const saveStoredThreadsInternal = (threads: ChatThread[]) => {
  localStorage.setItem("orasaka_threads", JSON.stringify(threads));
};

const getStoredMessages = (conversationId: string): ChatMessage[] => {
  if (!conversationId || typeof window === "undefined") return [];
  const stored = localStorage.getItem(`orasaka_messages_${conversationId}`);
  let parsed: Partial<ChatMessage>[] = [];
  if (stored) {
    try {
      parsed = JSON.parse(stored);
    } catch (e) {
      console.error(`Failed to parse messages for ${conversationId}:`, e);
    }
  } else {
    parsed = [
      {
        id: `init-${conversationId}`,
        role: "assistant",
        content: `System online. Orasaka Gateway ready for thread ${conversationId}. How may I assist your engineering goals today?`,
        timestamp: Date.now() - 60000,
      },
    ];
    localStorage.setItem(
      `orasaka_messages_${conversationId}`,
      JSON.stringify(parsed),
    );
  }
  return parsed.map((msg) => ({
    id: msg.id || `msg-${Date.now()}`,
    role: msg.role || "assistant",
    content: msg.content || "",
    timestamp: msg.timestamp || Date.now(),
    kind: msg.kind || "text",
  }));
};

const saveStoredMessages = (
  conversationId: string,
  messages: ChatMessage[],
) => {
  if (!conversationId || typeof window === "undefined") return;
  localStorage.setItem(
    `orasaka_messages_${conversationId}`,
    JSON.stringify(messages),
  );
};

/**
 * Posts a chat message payload to the BFF GraphQL API gateway.
 *
 * @async
 * @param {Object} variables - GraphQL arguments.
 * @param {string} variables.prompt - The plain text prompt to submit.
 * @param {string} variables.conversationId - The session identifier target.
 * @returns {Promise<ChatResponse>} The parsed GraphQL response structure containing gateway identifiers.
 * @throws {Error} If network request falls below status 200/299 or Gateway returns GraphQL errors.
 */
const postChatMessage = async (variables: {
  prompt: string;
  conversationId: string;
}): Promise<ChatResponse> => {
  const query = `
    mutation SendChat($prompt: String!, $conversationId: String) {
      chat(prompt: $prompt, conversationId: $conversationId) {
        content
        conversationId
      }
    }
  `;

  const response = await fetch("/api/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      query,
      variables,
    }),
  });

  if (!response.ok) {
    throw new Error("Failed to post message to Orasaka Gateway");
  }

  const result = await response.json();
  if (result.errors && result.errors.length > 0) {
    throw new Error(result.errors[0].message || "GraphQL Mutation failed");
  }

  return result.data.chat;
};

/**
 * Custom React Hook providing message history state, listing threads, and starting SSE token streams.
 * Orchestrates local storage database fallbacks with Server-Sent Events (SSE).
 *
 * @param {string} conversationId - The active target conversation thread ID.
 * @returns An object containing the threads list, messages list, loading states, SSE streaming toggles, and mutator handlers.
 */
export function useChatStream(conversationId: string) {
  const queryClient = useQueryClient();
  const queryKey = ["chatMessages", conversationId];

  const [isStreaming, setIsStreaming] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  // Cleanup active EventSource on conversationId change or unmount
  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
        setIsStreaming(false);
      }
    };
  }, [conversationId]);

  // 1. Fetch Threads
  const threadsQuery = useQuery({
    queryKey: ["chatThreads"],
    queryFn: getStoredThreads,
  });

  // 2. Fetch Messages for Current Session
  const messagesQuery = useQuery({
    queryKey,
    queryFn: () => getStoredMessages(conversationId),
    enabled: !!conversationId,
  });

  // 3. Send Message Mutation (using TanStack Mutation)
  const mutation = useMutation({
    mutationFn: postChatMessage,
    onMutate: async ({ prompt }) => {
      // Cancel refetches
      await queryClient.cancelQueries({ queryKey });

      // Snapshot previous value
      const previousMessages =
        queryClient.getQueryData<ChatMessage[]>(queryKey) || [];

      const userMsg: ChatMessage = {
        id: `user-${Date.now()}`,
        role: "user",
        content: prompt,
        timestamp: Date.now(),
        kind: "text",
      };

      const updated = [...previousMessages, userMsg];

      // Optimistic update
      queryClient.setQueryData(queryKey, updated);
      saveStoredMessages(conversationId, updated);

      return { previousMessages };
    },
    onError: (err, variables, context) => {
      // Rollback
      if (context?.previousMessages) {
        queryClient.setQueryData(queryKey, context.previousMessages);
        saveStoredMessages(conversationId, context.previousMessages);
      }
    },
    onSuccess: (data, variables) => {
      // Trigger SSE/GraphQL streaming update dynamically via BFF
      startStreaming(data.conversationId, variables.prompt);
    },
  });

  /**
   * Initializes a Server-Sent Events connection with the BFF chat streaming API endpoint.
   * Feeds chunks incrementally into the TanStack Query cache.
   *
   * @param {string} convId - The target conversation thread ID.
   * @param {string} prompt - The payload user query prompt.
   */
  const startStreaming = (convId: string, prompt: string) => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    setIsStreaming(true);
    const eventSource = new EventSource(
      `/api/chat/stream/${convId}?prompt=${encodeURIComponent(prompt)}`,
    );
    eventSourceRef.current = eventSource;

    const assistantMsgId = `assistant-${Date.now()}`;
    let accumulatedContent = "";

    eventSource.onmessage = (event) => {
      let chunk = "";
      try {
        // Parse OrasakaChatResponse JSON from the gateway
        const parsed = JSON.parse(event.data);
        chunk = parsed.content || "";
      } catch {
        // Fallback to raw event data
        chunk = event.data;
      }

      accumulatedContent += chunk;

      queryClient.setQueryData<ChatMessage[]>(queryKey, (old = []) => {
        const lastMsg = old[old.length - 1];
        let updated: ChatMessage[];
        if (
          lastMsg &&
          lastMsg.role === "assistant" &&
          lastMsg.id === assistantMsgId
        ) {
          updated = [
            ...old.slice(0, -1),
            {
              ...lastMsg,
              content: accumulatedContent,
              kind: "text",
            },
          ];
        } else {
          updated = [
            ...old,
            {
              id: assistantMsgId,
              role: "assistant",
              content: accumulatedContent,
              timestamp: Date.now(),
              kind: "text",
            },
          ];
        }
        saveStoredMessages(convId, updated);
        return updated;
      });
    };

    eventSource.onerror = () => {
      eventSource.close();
      if (eventSourceRef.current === eventSource) {
        eventSourceRef.current = null;
      }
      setIsStreaming(false);

      // On stream completion (clean close triggers error event under standard SSE Emitter), update thread info
      const currentThreads = getStoredThreads();
      const updatedThreads = currentThreads.map((t) => {
        if (t.conversationId === convId) {
          const title =
            prompt.length > 40 ? prompt.substring(0, 40) + "..." : prompt;
          return { ...t, title, updatedAt: Date.now() };
        }
        return t;
      });
      saveStoredThreads(updatedThreads);

      // Invalidate threads to refresh updated timestamps
      queryClient.invalidateQueries({ queryKey: ["chatThreads"] });
    };
  };

  /**
   * Generates a new unique thread identifier, creates a corresponding thread log,
   * caches it, and populates initial messaging state in local storage.
   *
   * @returns {ChatThread} The new thread metadata block.
   */
  const createThread = (): ChatThread => {
    const newId =
      typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
        ? crypto.randomUUID()
        : `thread-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    const newThread: ChatThread = {
      conversationId: newId,
      title: "New Memory Block",
      updatedAt: Date.now(),
    };

    const currentThreads = getStoredThreads();
    const updatedThreads = [newThread, ...currentThreads];
    saveStoredThreads(updatedThreads);

    // Initialize messages for the new thread to avoid blank state
    const defaultMsg: ChatMessage[] = [
      {
        id: `init-${newId}`,
        role: "assistant",
        content: `System online. Orasaka Gateway ready for thread ${newId}. How may I assist your engineering goals today?`,
        timestamp: Date.now(),
        kind: "text",
      },
    ];
    saveStoredMessages(newId, defaultMsg);

    // Update query cache immediately
    queryClient.setQueryData(["chatThreads"], updatedThreads);
    queryClient.invalidateQueries({ queryKey: ["chatThreads"] });

    return newThread;
  };

  return {
    threads: threadsQuery.data || [],
    isLoadingThreads: threadsQuery.isLoading,
    messages: messagesQuery.data || [],
    isLoadingMessages: messagesQuery.isLoading,
    sendMessage: (prompt: string) =>
      mutation.mutate({ prompt, conversationId }),
    isSending: mutation.isPending,
    isStreaming,
    isGenerating: mutation.isPending || isStreaming,
    error: mutation.error,
    createThread,
  };
}
