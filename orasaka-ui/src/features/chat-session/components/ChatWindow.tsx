"use client";

import React, { useState, useRef, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { ArrowDown } from "lucide-react";
import { useChatStream } from "@/features/chat-session/hooks/useChatStream";
import { ChatTimeline } from "./ChatTimeline";
import { ThreadList } from "./ThreadList";
import { ChatHeader } from "./ChatHeader";
import { ChatDrawer } from "./ChatDrawer";
import { ChatInputBar } from "./ChatInputBar";
import { useTranslation } from "@/core/context/LocaleContext";
import type { BootstrapFeature } from "./ContextPlusMenu";
import { useJobStream } from "@/features/jobs/context/JobStreamContext";
import { useChatActions } from "@/features/chat-session/hooks/useChatActions";

interface Props {
  initialConversationId: string;
}

export const ChatWindow: React.FC<Props> = ({ initialConversationId }) => {
  const router = useRouter();
  const {
    activeConversationId,
    setActiveConversationId,
    chatInput,
    setChatInput,
  } = useJobStream();

  useEffect(() => {
    if (
      initialConversationId &&
      initialConversationId !== activeConversationId
    ) {
      setActiveConversationId(initialConversationId);
      router.push(`/chat?conversationId=${initialConversationId}`);
    }
  }, [
    initialConversationId,
    activeConversationId,
    setActiveConversationId,
    router,
  ]);

  const [isThreadDrawerOpen, setIsThreadDrawerOpen] = useState(false);
  const [isPlusMenuOpen, setIsPlusMenuOpen] = useState(false);
  const [bootstrapFeatures, setBootstrapFeatures] = useState<
    BootstrapFeature[]
  >([]);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const [showScrollFab, setShowScrollFab] = useState(false);
  const { t } = useTranslation();

  const {
    selectedFeature,
    setSelectedFeature,
    attachment,
    setAttachment,
    isUploadingAttachment,
    fileInputRef,
    nodeMutation,
    handleSelectThread,
    handleFileChange,
    handleSend,
    handleExecuteNode,
  } = useChatActions({ activeConversationId });

  useEffect(() => {
    fetch("/api/v1/bootstrap/features")
      .then((res) => {
        if (!res.ok)
          throw new Error("Bootstrap features returned " + res.status);
        return res.json();
      })
      .then((data) => setBootstrapFeatures(data))
      .catch((err) => console.error("Error loading bootstrap features:", err));
  }, []);

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
    renameThread,
    deleteThread,
  } = useChatStream(activeConversationId);

  const handleCreateThread = async () => {
    try {
      const newThread = await createThread();
      setActiveConversationId(newThread.conversationId);
      router.push(`/chat?conversationId=${newThread.conversationId}`);
    } catch (e) {
      console.error("Failed to create thread:", e);
    }
  };

  const handleRenameThread = async (id: string, title: string) => {
    try {
      await renameThread(id, title);
    } catch (e) {
      console.error("Failed to rename thread:", e);
    }
  };

  const handleDeleteThread = async (id: string) => {
    if (
      typeof window !== "undefined" &&
      !window.confirm(t.chat.deleteConfirm)
    ) {
      return;
    }
    try {
      await deleteThread(id);
      if (activeConversationId === id) {
        setActiveConversationId("");
        router.push("/chat");
      }
    } catch (e) {
      console.error("Failed to delete thread:", e);
    }
  };

  const activeThread = threads.find(
    (th) => th.conversationId === activeConversationId,
  );
  const threadTitle = activeThread ? activeThread.title : "";

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isSending, isGenerating, nodeMutation.isPending]);

  /* Track scroll position to show/hide FAB */
  const handleScroll = useCallback(() => {
    const el = scrollContainerRef.current;
    if (!el) return;
    const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    setShowScrollFab(distanceFromBottom > 200);
  }, []);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    setShowScrollFab(false);
  }, []);

  const isImagePending =
    nodeMutation.isPending && nodeMutation.variables?.feature.icon === "image";
  const isSpeechPending =
    nodeMutation.isPending && nodeMutation.variables?.feature.icon === "mic";

  return (
    <main className="flex h-full w-full bg-[var(--surface-0)] overflow-hidden relative">
      <ChatDrawer
        isOpen={isThreadDrawerOpen}
        onClose={() => setIsThreadDrawerOpen(false)}
        threads={threads ?? []}
        activeConversationId={activeConversationId}
        onSelectThread={handleSelectThread}
        isLoadingThreads={isLoadingThreads}
        onCreateThread={handleCreateThread}
        onDeleteThread={handleDeleteThread}
        t={t}
      />

      <aside className="w-72 flex-shrink-0 hidden md:block">
        <ThreadList
          threads={threads ?? []}
          activeId={activeConversationId}
          onSelectThread={handleSelectThread}
          isLoading={isLoadingThreads}
          onCreateThread={handleCreateThread}
          onDeleteThread={handleDeleteThread}
        />
      </aside>

      <section className="flex-1 flex flex-col h-full min-w-0 bg-[var(--surface-0)]">
        {activeConversationId ? (
          <>
            <ChatHeader
              activeConversationId={activeConversationId}
              threadTitle={threadTitle}
              onOpenDrawer={() => setIsThreadDrawerOpen(true)}
              onRename={(title) =>
                handleRenameThread(activeConversationId, title)
              }
              t={t}
            />

            <article
              ref={scrollContainerRef}
              onScroll={handleScroll}
              className="flex-1 overflow-y-auto p-6 space-y-5 relative scroll-smooth"
            >
              <ChatTimeline
                messages={messages ?? []}
                isLoadingMessages={isLoadingMessages}
                isGenerating={isGenerating}
                isImagePending={isImagePending}
                isSpeechPending={isSpeechPending}
                error={error}
                messagesEndRef={messagesEndRef}
              />

              {/* Scroll to bottom FAB */}
              {showScrollFab && (
                <button
                  type="button"
                  onClick={scrollToBottom}
                  className="sticky bottom-4 left-1/2 -translate-x-1/2 z-10 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-[var(--surface-2)] border border-[var(--border-default)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-3)] shadow-lg transition-all duration-200 text-[11px] font-medium animate-in fade-in slide-in-from-bottom-2 duration-200"
                  aria-label="Scroll to bottom"
                >
                  <ArrowDown className="w-3.5 h-3.5" />
                  {t.chat.newMessages}
                </button>
              )}
            </article>

            <ChatInputBar
              input={chatInput}
              onInputChange={setChatInput}
              onSubmit={(e) =>
                handleSend(e, sendMessage, isSending, isGenerating)
              }
              isSending={isSending}
              isGenerating={isGenerating}
              isUploadingAttachment={isUploadingAttachment}
              selectedFeature={selectedFeature}
              attachment={attachment}
              onClearFeature={() => setSelectedFeature(null)}
              onClearAttachment={() => setAttachment(null)}
              isPlusMenuOpen={isPlusMenuOpen}
              onTogglePlusMenu={() => setIsPlusMenuOpen(!isPlusMenuOpen)}
              onClosePlusMenu={() => setIsPlusMenuOpen(false)}
              onExecuteNode={(feature) =>
                handleExecuteNode(feature, isPlusMenuOpen, setIsPlusMenuOpen)
              }
              bootstrapFeatures={bootstrapFeatures}
              fileInputRef={fileInputRef}
              onFileChange={(e) => handleFileChange(e, t)}
              t={t}
            />
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-center px-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="w-16 h-16 rounded-2xl bg-[var(--accent-soft)] flex items-center justify-center mb-5">
              <svg
                className="w-8 h-8 text-[var(--accent)]"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={1.5}
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 01-2.555-.337A5.972 5.972 0 015.41 20.97a5.969 5.969 0 01-.474-.065 4.48 4.48 0 00.978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25z"
                />
              </svg>
            </div>
            <h2 className="text-[15px] font-semibold text-[var(--text-primary)] mb-2">
              {t.chat.startConversation}
            </h2>
            <p className="text-[13px] text-[var(--text-muted)] mb-8 max-w-sm">
              {t.chat.startConversationDesc}
            </p>

            {/* Suggestion chips */}
            <div className="flex flex-wrap justify-center gap-2.5 mb-8">
              {[
                { label: t.chat.suggestionImage, icon: "🎨" },
                { label: t.chat.suggestionCode, icon: "⚡" },
                { label: t.chat.suggestionAsk, icon: "💬" },
              ].map((chip) => (
                <button
                  key={chip.label}
                  onClick={async () => {
                    const newThread = await createThread();
                    setActiveConversationId(newThread.conversationId);
                    router.push(
                      `/chat?conversationId=${newThread.conversationId}`,
                    );
                    setChatInput(chip.label);
                  }}
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium text-[var(--text-secondary)] bg-[var(--surface-2)] border border-[var(--border-subtle)] hover:bg-[var(--surface-3)] hover:text-[var(--text-primary)] hover:border-[var(--border-default)] transition-all duration-200 hover:shadow-sm"
                >
                  <span>{chip.icon}</span>
                  {chip.label}
                </button>
              ))}
            </div>

            {/* New chat CTA */}
            <button
              onClick={handleCreateThread}
              className="px-6 py-2.5 rounded-xl text-sm font-semibold text-white bg-[var(--accent)] hover:bg-[var(--accent-hover)] transition-all duration-200 shadow-sm hover:shadow-md"
            >
              {t.dashboard.startNewChat}
            </button>
          </div>
        )}
      </section>
    </main>
  );
};
