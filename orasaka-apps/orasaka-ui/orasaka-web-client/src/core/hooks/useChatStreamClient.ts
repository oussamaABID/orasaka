"use client";

import { useRef, useCallback, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import type { ChatMessage } from "@/core/types/chat.types";
import { useAuth } from "@/core/hooks/useAuth";

async function readChatStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  assistantMsgId: string,
  userId: string,
  convId: string,
  queryClient: ReturnType<typeof useQueryClient>,
  queryKey: readonly ["chatMessages", string, string],
  onAccumulated: (content: string) => void,
) {
  const decoder = new TextDecoder();
  let buffer = "";
  let accumulatedContent = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() || "";

    for (const line of lines) {
      const cleaned = line.trim();
      if (!cleaned) continue;

      if (cleaned.startsWith("data:")) {
        let chunk = cleaned.slice(5).trim();
        try {
          const parsed = JSON.parse(chunk);
          chunk = parsed.content ?? parsed.text ?? "";
        } catch {
          // Fallback to raw string
        }

        if (chunk) {
          accumulatedContent += chunk;
          onAccumulated(accumulatedContent);
          queryClient.setQueryData<ChatMessage[]>(
            queryKey,
            (old = []) => {
              const lastMsg = old.at(-1);
              const updated: ChatMessage[] =
                lastMsg?.role === "assistant"
                  ? [
                      ...old.slice(0, -1),
                      {
                        ...lastMsg,
                        content: accumulatedContent,
                        kind: "text" as const,
                      },
                    ]
                  : [
                      ...old,
                      {
                        id: assistantMsgId,
                        role: "assistant",
                        content: accumulatedContent,
                        timestamp: Date.now(),
                        kind: "text" as const,
                      },
                    ];
              if (globalThis.window !== undefined) {
                localStorage.setItem(
                  `orasaka_messages_${userId}_${convId}`,
                  JSON.stringify(updated),
                );
              }
              return updated;
            },
          );
        }
      }
    }
  }
}

function handleStreamError(
  err: unknown,
  prompt: string,
  convId: string,
  userId: string,
  accumulatedContent: string,
  queryClient: ReturnType<typeof useQueryClient>,
  queryKey: readonly ["chatMessages", string, string],
) {
  if (err instanceof Error && err.name === "AbortError") {
    return;
  }
  console.error("SSE stream error:", err);

  if (!accumulatedContent) {
    queryClient.setQueryData<ChatMessage[]>(queryKey, (old = []) => {
      const errorMsg = {
        id: `error-${Date.now()}`,
        role: "assistant" as const,
        content:
          "⚠️ Connection to the AI model failed. Make sure Ollama is running at the configured endpoint and try again.",
        timestamp: Date.now(),
        kind: "text" as const,
      };
      const updated = [...old, errorMsg];
      if (globalThis.window !== undefined) {
        localStorage.setItem(
          `orasaka_messages_${userId}_${convId}`,
          JSON.stringify(updated),
        );
      }
      return updated;
    });
  }

  const title = prompt.length > 40 ? `${prompt.substring(0, 40)}...` : prompt;
  if (globalThis.window !== undefined) {
    const storedThreadsStr = localStorage.getItem("orasaka_threads");
    if (storedThreadsStr) {
      try {
        const threads = JSON.parse(storedThreadsStr) as Record<string, unknown>[];
        const updated = threads.map((t: Record<string, unknown>) =>
          t.conversationId === convId
            ? { ...t, title, updatedAt: Date.now() }
            : t,
        );
        localStorage.setItem("orasaka_threads", JSON.stringify(updated));
      } catch (e) {
        console.error(e);
      }
    }
  }

  queryClient.invalidateQueries({ queryKey: ["chatThreads"] });
  queryClient.invalidateQueries({ queryKey: ["operationGraph"] });
}

/**
 * Encapsulates the SSE chat streaming lifecycle:
 * - AbortController management
 * - Incremental token accumulation into React Query cache
 * - Error handling with localStorage fallback persistence
 */
export function useChatStreamClient() {
  const [isChatStreaming, setIsChatStreaming] = useState(false);
  const chatAbortControllerRef = useRef<AbortController | null>(null);
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const userId = user?.id || user?.email || "anonymous";

  const stopChatStream = useCallback(() => {
    if (chatAbortControllerRef.current) {
      chatAbortControllerRef.current.abort();
      chatAbortControllerRef.current = null;
    }
    setIsChatStreaming(false);
  }, []);

  const startChatStream = useCallback(
    async (convId: string, prompt: string, assetIds: string[] = []) => {
      stopChatStream();
      setIsChatStreaming(true);

      const controller = new AbortController();
      chatAbortControllerRef.current = controller;

      const assistantMsgId = `assistant-${Date.now()}`;
      let accumulated = "";
      const queryKey = ["chatMessages", userId, convId] as const;

      try {
        const response = await fetch(`/api/chat/stream/${convId}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ prompt, assetIds }),
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`Gateway stream error: ${response.statusText}`);
        }

        if (!response.body) {
          throw new Error("Response body is empty");
        }

        const reader = response.body.getReader();
        await readChatStream(
          reader,
          assistantMsgId,
          userId,
          convId,
          queryClient,
          queryKey,
          (content) => {
            accumulated = content;
          },
        );
      } catch (err) {
        handleStreamError(
          err,
          prompt,
          convId,
          userId,
          accumulated,
          queryClient,
          queryKey,
        );
      } finally {
        setIsChatStreaming(false);
        if (chatAbortControllerRef.current === controller) {
          chatAbortControllerRef.current = null;
        }
      }
    },
    [stopChatStream, queryClient, userId],
  );

  return { isChatStreaming, startChatStream, stopChatStream };
}
