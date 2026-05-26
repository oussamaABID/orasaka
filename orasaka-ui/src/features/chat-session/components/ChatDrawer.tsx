import React from "react";
import { ThreadList } from "./ThreadList";
import { ChatThread } from "@/features/chat-session/types/chat.types";

interface ChatDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  threads: ChatThread[];
  activeConversationId: string;
  onSelectThread: (id: string) => void;
  isLoadingThreads: boolean;
  onCreateThread: () => void;
  onDeleteThread: (id: string) => void;
  t: {
    chat: {
      memoryBlocks: string;
    };
  };
}

export function ChatDrawer({
  isOpen,
  onClose,
  threads,
  activeConversationId,
  onSelectThread,
  isLoadingThreads,
  onCreateThread,
  onDeleteThread,
  t,
}: ChatDrawerProps) {
  if (!isOpen) return null;

  return (
    <aside className="fixed inset-0 z-50 md:hidden flex">
      <button
        type="button"
        className="fixed inset-0 bg-zinc-950/60 backdrop-blur-sm border-none cursor-default"
        aria-label="Close drawer"
        onClick={onClose}
      />
      <div className="relative flex flex-col w-72 max-w-[80vw] h-full bg-white/95 dark:bg-zinc-900/95 border-r border-zinc-200/80 dark:border-zinc-800/60 backdrop-blur-md z-50">
        <div className="p-4 border-b border-zinc-200/80 dark:border-zinc-800/60 flex justify-between items-center bg-white/50 dark:bg-zinc-900/50">
          <span className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">
            {t.chat.memoryBlocks}
          </span>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-zinc-400 hover:text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
          >
            <svg
              className="w-5 h-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M6 18 18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
        <div className="flex-1 overflow-y-auto">
          <ThreadList
            threads={threads}
            activeId={activeConversationId}
            onSelectThread={(id) => {
              onSelectThread(id);
              onClose();
            }}
            isLoading={isLoadingThreads}
            onCreateThread={() => {
              onCreateThread();
              onClose();
            }}
            onDeleteThread={onDeleteThread}
          />
        </div>
      </div>
    </aside>
  );
}
