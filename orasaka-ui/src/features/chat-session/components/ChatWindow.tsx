"use client";

import React, { useState, useRef, useEffect } from "react";
import { useChatStream } from "../hooks/useChatStream";
import { MessageBubble } from "./MessageBubble";
import { ThreadList } from "./ThreadList";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useTranslation } from "@/core/context/LocaleContext";

interface Props {
  initialConversationId: string;
}

/**
 * ChatWindow component integrates the message list stream, thread selection drawer,
 * and user text entry layout.
 *
 * @param props Component properties containing the initial conversation session ID.
 * @returns The ChatWindow react node.
 */
export const ChatWindow: React.FC<Props> = ({ initialConversationId }) => {
  const [activeConversationId, setActiveConversationId] = useState<string>(
    initialConversationId,
  );
  const [input, setInput] = useState<string>("");
  const [isThreadDrawerOpen, setIsThreadDrawerOpen] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { t } = useTranslation();

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

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isSending, isGenerating]);

  const handleSend = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!input.trim() || isSending || isGenerating) return;

    sendMessage(input.trim());
    setInput("");
  };

  const showTypingIndicator =
    isGenerating &&
    messages.length > 0 &&
    messages[messages.length - 1].role === "user";

  return (
    <div className="flex h-full w-full bg-zinc-50/30 dark:bg-zinc-950/20 overflow-hidden relative">
      {/* Mobile Thread Drawer (left slide-out drawer) */}
      {isThreadDrawerOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          {/* Backdrop overlay */}
          <div
            className="fixed inset-0 bg-zinc-950/60 backdrop-blur-sm transition-opacity"
            onClick={() => setIsThreadDrawerOpen(false)}
          />
          {/* Drawer Content */}
          <div className="relative flex flex-col w-72 max-w-[80vw] h-full bg-white/95 dark:bg-zinc-900/95 border-r border-zinc-200/80 dark:border-zinc-800/60 backdrop-blur-md z-50 transform transition-transform duration-300">
            {/* Close button inside drawer */}
            <div className="p-4 border-b border-zinc-200/80 dark:border-zinc-800/60 flex justify-between items-center bg-white/50 dark:bg-zinc-900/50">
              <span className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">
                {t.chat.memoryBlocks}
              </span>
              <button
                onClick={() => setIsThreadDrawerOpen(false)}
                className="p-1 rounded-xl text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-all duration-200"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="w-5 h-5"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M6 18 18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
            <div className="flex-1 overflow-y-auto">
              <ThreadList
                threads={threads ?? []}
                activeId={activeConversationId}
                onSelectThread={(id) => {
                  setActiveConversationId(id);
                  setIsThreadDrawerOpen(false);
                }}
                isLoading={isLoadingThreads}
                onCreateThread={() => {
                  const newThread = createThread();
                  setActiveConversationId(newThread.conversationId);
                  setIsThreadDrawerOpen(false);
                }}
              />
            </div>
          </div>
        </div>
      )}

      {/* Sidebar Threads intégrée de manière standard */}
      <div className="w-72 flex-shrink-0 hidden md:block">
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
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col h-full min-w-0 bg-white/40 dark:bg-zinc-900/10">
        {/* Header Neutre */}
        <div className="p-4 border-b border-zinc-200/80 dark:border-zinc-800/60 bg-white/40 dark:bg-zinc-900/20 backdrop-blur-sm flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setIsThreadDrawerOpen(true)}
              className="p-2 rounded-xl text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-900 md:hidden border border-zinc-200/80 dark:border-zinc-800/60 transition-all duration-200"
              aria-label="Toggle Memory Blocks"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.5}
                stroke="currentColor"
                className="w-5 h-5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"
                />
              </svg>
            </button>
            <div className="flex flex-col">
              <h1 className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">
                {t.chat.sessionTitle}
              </h1>
              <span className="text-xs text-zinc-400 dark:text-zinc-500 mt-0.5 font-mono">
                {t.chat.id}: {activeConversationId}
              </span>
            </div>
          </div>
        </div>

        {/* Message Container */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5 bg-zinc-50/10 dark:bg-zinc-900/5 animate-in fade-in duration-300">
          {isLoadingMessages ? (
            <div className="h-full flex items-center justify-center">
              <span className="text-zinc-400 dark:text-zinc-500 text-sm animate-pulse">
                {t.chat.loadingMessages}
              </span>
            </div>
          ) : !messages || messages.length === 0 ? (
            <div className="h-full flex items-center justify-center text-zinc-400 dark:text-zinc-500 text-sm">
              {t.chat.noActiveConversation}
            </div>
          ) : (
            <>
              {messages.map((msg) => (
                <MessageBubble key={msg.id} message={msg} />
              ))}
              {showTypingIndicator && (
                <div className="flex items-start gap-3 animate-in fade-in duration-200">
                  <div className="flex-shrink-0 w-8 h-8 rounded-xl bg-zinc-100 dark:bg-zinc-900 border border-zinc-200/80 dark:border-zinc-800/60 flex items-center justify-center text-xs font-semibold text-zinc-500 dark:text-zinc-400 select-none shadow-sm">
                    {t.chat.ai}
                  </div>
                  <div className="bg-zinc-100 dark:bg-zinc-900 border border-zinc-200/80 dark:border-zinc-800/60 rounded-2xl px-4.5 py-3 max-w-[85%] sm:max-w-[70%] shadow-sm flex items-center gap-1.5 h-10">
                    <span
                      className="w-2 h-2 bg-zinc-400 dark:bg-zinc-500 rounded-full animate-bounce"
                      style={{ animationDelay: "0ms" }}
                    />
                    <span
                      className="w-2 h-2 bg-zinc-400 dark:bg-zinc-500 rounded-full animate-bounce"
                      style={{ animationDelay: "150ms" }}
                    />
                    <span
                      className="w-2 h-2 bg-zinc-400 dark:bg-zinc-500 rounded-full animate-bounce"
                      style={{ animationDelay: "300ms" }}
                    />
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </>
          )}

          {error && (
            <div className="p-4 bg-red-50 dark:bg-red-950/20 border border-red-200/80 dark:border-red-900/30 text-red-600 dark:text-red-400 rounded-xl text-sm transition-all duration-200">
              {t.chat.connectionError} ({(error as Error)?.message || ""})
            </div>
          )}
        </div>

        {/* Input Area épurée qui utilise tes composants de design system */}
        <form
          onSubmit={handleSend}
          className="p-4 bg-white/50 dark:bg-zinc-900/30 backdrop-blur-sm border-t border-zinc-200/80 dark:border-zinc-800/60 flex gap-3 items-center"
        >
          <Input
            value={input}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              setInput(e.target.value)
            }
            placeholder={isGenerating ? t.chat.typing : t.chat.typeMessage}
            className="flex-1 bg-white/80 dark:bg-zinc-900/40 border-zinc-200/80 dark:border-zinc-800/60 text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-500 focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-700"
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
      </div>
    </div>
  );
};
