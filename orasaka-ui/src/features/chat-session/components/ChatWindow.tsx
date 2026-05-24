"use client";

import React, { useState, useRef, useEffect } from "react";
import { useChatStream } from "../hooks/useChatStream";
import { ChatTimeline } from "./ChatTimeline";
import { ThreadList } from "./ThreadList";
import { ChatHeader } from "./ChatHeader";
import { ChatDrawer } from "./ChatDrawer";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useTranslation } from "@/core/context/LocaleContext";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ContextPlusMenu,
  OperationNode,
  fetchOperationGraph,
} from "./ContextPlusMenu";
import { ChatMessage } from "../types/chat.types";

interface Props {
  initialConversationId: string;
}

export const ChatWindow: React.FC<Props> = ({ initialConversationId }) => {
  const [activeConversationId, setActiveConversationId] = useState<string>(
    initialConversationId,
  );
  const [input, setInput] = useState<string>("");
  const [isThreadDrawerOpen, setIsThreadDrawerOpen] = useState(false);
  const [isPlusMenuOpen, setIsPlusMenuOpen] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const queryKey = ["chatMessages", activeConversationId];

  const {
    threads,
    isLoadingThreads,
    messages,
    isLoadingMessages,
    sendMessage,
    isSending,
    isGenerating,
    error,
    createThread,
  } = useChatStream(activeConversationId);

  const { data: nodes } = useQuery({
    queryKey: ["operationGraph"],
    queryFn: fetchOperationGraph,
    refetchOnWindowFocus: false,
  });

  const addMessageToCache = (content: string, kind: "image" | "audio") => {
    queryClient.setQueryData<ChatMessage[]>(queryKey, (old = []) => {
      const updated = [
        ...old,
        {
          id: `assistant-${Date.now()}`,
          role: "assistant" as const,
          content,
          timestamp: Date.now(),
          kind,
        },
      ];
      localStorage.setItem(
        `orasaka_messages_${activeConversationId}`,
        JSON.stringify(updated),
      );
      return updated;
    });
  };

  const imageMutation = useMutation({
    mutationFn: async (prompt: string) => {
      const response = await fetch("/api/graphql", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: `mutation GenerateImage($prompt: String!) { image(prompt: $prompt) { content } }`,
          variables: { prompt },
        }),
      });
      const res = await response.json();
      if (res.errors) throw new Error(res.errors[0].message);
      return res.data.image;
    },
    onSuccess: (data) => addMessageToCache(data.content, "image"),
  });

  const speechMutation = useMutation({
    mutationFn: async (prompt: string) => {
      const response = await fetch("/api/graphql", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: `mutation GenerateSpeech($prompt: String!) { speech(prompt: $prompt) { content } }`,
          variables: { prompt },
        }),
      });
      const res = await response.json();
      if (res.errors) throw new Error(res.errors[0].message);
      return res.data.speech;
    },
    onSuccess: (data) => addMessageToCache(data.content, "audio"),
  });

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [
    messages,
    isSending,
    isGenerating,
    imageMutation.isPending,
    speechMutation.isPending,
  ]);

  const handleSend = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!input.trim() || isSending || isGenerating) return;
    sendMessage(input.trim());
    setInput("");
  };

  const handleExecuteNode = (node: OperationNode) => {
    if (!input.trim()) {
      alert(`Please type a prompt in the input field first.`);
      return;
    }
    if (node.id === "orasaka.core.chat.image") {
      imageMutation.mutate(input.trim());
    } else if (node.id === "orasaka.core.chat.speech") {
      speechMutation.mutate(input.trim());
    }
    setInput("");
  };

  const plusNodes =
    nodes?.filter(
      (n: OperationNode) => n.presentationContext === "CONTEXT_MENU_PLUS",
    ) || [];

  return (
    <main className="flex h-full w-full bg-zinc-50/30 dark:bg-zinc-950/20 overflow-hidden relative">
      <ChatDrawer
        isOpen={isThreadDrawerOpen}
        onClose={() => setIsThreadDrawerOpen(false)}
        threads={threads ?? []}
        activeConversationId={activeConversationId}
        onSelectThread={setActiveConversationId}
        isLoadingThreads={isLoadingThreads}
        onCreateThread={() => {
          const newThread = createThread();
          setActiveConversationId(newThread.conversationId);
        }}
        t={t}
      />

      <aside className="w-72 flex-shrink-0 hidden md:block">
        <ThreadList
          threads={threads ?? []}
          activeId={activeConversationId}
          onSelectThread={setActiveConversationId}
          isLoading={isLoadingThreads}
          onCreateThread={() => {
            const newThread = createThread();
            setActiveConversationId(newThread.conversationId);
          }}
        />
      </aside>

      <section className="flex-1 flex flex-col h-full min-w-0 bg-white/40 dark:bg-zinc-900/10">
        <ChatHeader
          activeConversationId={activeConversationId}
          onOpenDrawer={() => setIsThreadDrawerOpen(true)}
          t={t}
        />

        <article className="flex-1 overflow-y-auto p-6 space-y-5 bg-zinc-50/10 dark:bg-zinc-900/5 relative">
          <ChatTimeline
            messages={messages ?? []}
            isLoadingMessages={isLoadingMessages}
            isGenerating={isGenerating}
            isImagePending={imageMutation.isPending}
            isSpeechPending={speechMutation.isPending}
            error={error}
            messagesEndRef={messagesEndRef}
          />
          <ContextPlusMenu
            isOpen={isPlusMenuOpen}
            onClose={() => setIsPlusMenuOpen(false)}
            onExecuteNode={handleExecuteNode}
            nodes={plusNodes}
          />
        </article>

        <form
          onSubmit={handleSend}
          className="p-4 bg-white/50 dark:bg-zinc-900/30 border-t border-zinc-200 dark:border-zinc-800 flex gap-3 items-center relative"
        >
          <button
            type="button"
            onClick={() => setIsPlusMenuOpen(!isPlusMenuOpen)}
            className="p-2.5 rounded-xl bg-zinc-100 hover:bg-zinc-200 dark:bg-zinc-800 dark:hover:bg-zinc-700 text-zinc-600 dark:text-zinc-300 transition-all shadow-sm flex items-center justify-center hover:scale-105 active:scale-95 flex-shrink-0"
            aria-label="Add Capability"
          >
            <svg
              className={`w-5 h-5 transition-transform duration-200 ${isPlusMenuOpen ? "rotate-45" : ""}`}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={2}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M12 4.5v15m7.5-7.5h-15"
              />
            </svg>
          </button>
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={isGenerating ? t.chat.typing : t.chat.typeMessage}
            className="flex-1 bg-white/80 dark:bg-zinc-900/40 text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-500"
            disabled={isSending || isGenerating}
            autoComplete="off"
          />
          <Button
            type="submit"
            disabled={isSending || isGenerating || !input.trim()}
          >
            {isSending
              ? t.chat.sending
              : isGenerating
                ? t.chat.typing
                : t.chat.send}
          </Button>
        </form>
      </section>
    </main>
  );
};
