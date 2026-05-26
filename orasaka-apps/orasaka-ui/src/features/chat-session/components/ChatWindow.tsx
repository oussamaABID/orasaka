"use client";

import React, { useState, useRef, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Icon } from "@/components/ui/icon";
import { useChatStream } from "@/features/chat-session/hooks/useChatStream";
import { ChatTimeline } from "./ChatTimeline";
import { ThreadList } from "./ThreadList";
import { ChatHeader } from "./ChatHeader";
import { ChatDrawer } from "./ChatDrawer";
import { ChatInputBar } from "./ChatInputBar";
import { ChatEmptyState } from "./ChatEmptyState";
import { useTranslation } from "@/core/context/LocaleContext";
import type { BootstrapFeature } from "./ContextPlusMenu";
import { useJobStream } from "@/core/context/JobStreamContext";
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
      globalThis.window !== undefined &&
      !globalThis.confirm(t.chat.deleteConfirm)
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

  /** Whether the input bar should render in centered (search) mode */
  const isInputCentered = !activeConversationId;

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

      {/* Thread list — hidden in empty state for clean viewport */}
      {activeConversationId && (
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
      )}

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
                  className="sticky bottom-4 left-1/2 -translate-x-1/2 z-10 inline-flex items-center gap-1.5 px-3 py-1.5 bg-[var(--surface-2)] border border-[var(--border-default)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-3)] shadow-lg transition-all duration-200 text-[11px] font-medium animate-in fade-in slide-in-from-bottom-2 duration-200"
                  aria-label="Scroll to bottom"
                >
                  <Icon name="arrowDown" size={14} />
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
              isCentered={false}
            />
          </>
        ) : (
          <>
            {/* Empty state: centered input + WelcomeHero */}
            <ChatEmptyState
              t={t}
              onCreateThread={handleCreateThread}
              onChipClick={async (label) => {
                const newThread = await createThread();
                setActiveConversationId(newThread.conversationId);
                router.push(
                  `/chat?conversationId=${newThread.conversationId}`,
                );
                setChatInput(label);
              }}
            />

            <ChatInputBar
              input={chatInput}
              onInputChange={setChatInput}
              onSubmit={async (e) => {
                e.preventDefault();
                if (!chatInput.trim()) return;
                const newThread = await createThread();
                setActiveConversationId(newThread.conversationId);
                router.push(
                  `/chat?conversationId=${newThread.conversationId}`,
                );
                // Message will be sent once conversation is active
              }}
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
              isCentered={isInputCentered}
            />
          </>
        )}
      </section>
    </main>
  );
};
