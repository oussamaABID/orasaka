import React from "react";

interface ChatHeaderProps {
  activeConversationId: string;
  onOpenDrawer: () => void;
  t: {
    chat: {
      sessionTitle: string;
      id: string;
    };
  };
}

export function ChatHeader({
  activeConversationId,
  onOpenDrawer,
  t,
}: ChatHeaderProps) {
  return (
    <header className="p-4 border-b border-zinc-200/80 dark:border-zinc-800/60 bg-white/40 dark:bg-zinc-900/20 backdrop-blur-sm flex items-center justify-between">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onOpenDrawer}
          className="p-2 rounded-xl md:hidden border border-zinc-200/80 dark:border-zinc-800/60 transition-all"
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
    </header>
  );
}
