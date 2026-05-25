import React from "react";
import { format } from "date-fns";
import { ChatThread } from "@/features/chat-session/types/chat.types";
import { useTranslation } from "@/core/context/LocaleContext";

interface Props {
  threads: ChatThread[];
  activeId: string;
  onSelectThread: (id: string) => void;
  isLoading: boolean;
  onCreateThread: () => void;
}

/**
 * ThreadList component displaying active chat thread blocks and allowing creation of new blocks.
 *
 * @param props Component properties containing threads, callback handlers, and loading states.
 * @returns The ThreadList react node.
 */
export const ThreadList: React.FC<Props> = ({
  threads,
  activeId,
  onSelectThread,
  isLoading,
  onCreateThread,
}) => {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <div className="space-y-2 p-4">
        {[1, 2, 3].map((n) => (
          <div
            key={n}
            className="h-12 bg-zinc-100/80 dark:bg-zinc-900/50 animate-pulse rounded-xl"
          />
        ))}
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-zinc-50/40 dark:bg-zinc-950/20 border-r border-zinc-200/80 dark:border-zinc-800/60 backdrop-blur-md">
      <div className="p-4 border-b border-zinc-200/80 dark:border-zinc-800/60 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">
          {t.chat.memoryBlocks}
        </h2>
        <button
          onClick={onCreateThread}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-xl border border-zinc-200/80 dark:border-zinc-800/60 bg-white/80 dark:bg-zinc-900/50 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50/80 dark:hover:bg-zinc-800/80 transition-all duration-200 hover:scale-[1.02] active:scale-[0.98] shadow-sm hover:shadow-md"
        >
          <svg
            className="w-3.5 h-3.5 text-zinc-500 dark:text-zinc-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 4v16m8-8H4"
            />
          </svg>
          {t.chat.newBlock}
        </button>
      </div>
      <div className="flex-1 overflow-y-auto p-2 space-y-1">
        {threads.map((thread) => {
          const isActive = thread.conversationId === activeId;
          return (
            <button
              key={thread.conversationId}
              onClick={() => onSelectThread(thread.conversationId)}
              className={`w-full text-left p-3 rounded-xl transition-all duration-200 text-sm border hover:scale-[1.01] active:scale-[0.99] ${
                isActive
                  ? "bg-zinc-100/80 dark:bg-zinc-900/65 border-zinc-200/80 dark:border-zinc-800/60 text-zinc-900 dark:text-zinc-50 font-semibold shadow-sm"
                  : "bg-transparent border-transparent text-zinc-500 dark:text-zinc-400 hover:bg-zinc-100/40 dark:hover:bg-zinc-900/35"
              }`}
            >
              <div className="truncate">{thread.title}</div>
              <div className="text-xs text-zinc-400 dark:text-zinc-500 mt-1">
                {format(new Date(thread.updatedAt), "yyyy-MM-dd")}
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
};
