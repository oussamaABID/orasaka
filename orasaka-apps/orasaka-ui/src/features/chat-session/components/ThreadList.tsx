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
  onDeleteThread: (id: string) => void;
}

/**
 * ThreadList — sidebar panel listing active chat threads.
 * Calm Obsidian 2026 design: solid surfaces, no backdrop-blur, no hover:scale.
 */
export const ThreadList: React.FC<Props> = ({
  threads,
  activeId,
  onSelectThread,
  isLoading,
  onCreateThread,
  onDeleteThread,
}) => {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <div className="space-y-2 p-4">
        {[1, 2, 3].map((n) => (
          <div key={n} className="h-12 bg-[var(--surface-2)] rounded-lg" />
        ))}
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-[var(--surface-1)] border-r border-[var(--border-subtle)]">
      <div className="p-4 border-b border-[var(--border-subtle)] flex items-center justify-between">
        <h2 className="text-sm font-semibold text-[var(--text-primary)]">
          {t.chat.memoryBlocks}
        </h2>
        <button
          id="btn-new-block"
          onClick={onCreateThread}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg border border-[var(--border-default)] bg-[var(--surface-2)] text-[var(--text-secondary)] hover:bg-[var(--surface-3)] transition-colors duration-150"
        >
          <svg
            className="w-3.5 h-3.5"
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
            <div
              key={thread.conversationId}
              className={`group w-full relative flex items-center justify-between rounded-lg transition-colors duration-150 text-sm border ${
                isActive
                  ? "bg-[var(--surface-2)] border-[var(--border-default)] text-[var(--text-primary)] font-semibold"
                  : "bg-transparent border-transparent text-[var(--text-secondary)] hover:bg-[var(--surface-2)]"
              }`}
            >
              <button
                onClick={() => onSelectThread(thread.conversationId)}
                className="flex-1 text-left p-3 min-w-0"
              >
                <div className="truncate pr-6">{thread.title}</div>
                <div className="text-xs text-[var(--text-muted)] mt-1">
                  {format(thread.updatedAt, "yyyy-MM-dd")}
                </div>
              </button>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onDeleteThread(thread.conversationId);
                }}
                className="absolute right-3 p-1.5 rounded-lg text-[var(--text-muted)] hover:text-red-500 dark:hover:text-red-400 hover:bg-[var(--surface-3)] opacity-0 group-hover:opacity-100 focus:opacity-100 transition-all duration-150"
                title="Delete Session"
              >
                <svg
                  className="w-4 h-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                  />
                </svg>
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};
