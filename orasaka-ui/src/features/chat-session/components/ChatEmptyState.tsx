"use client";

import React from "react";
import type { TranslationDictionary } from "@/core/i18n/translations";

interface ChatEmptyStateProps {
  t: TranslationDictionary;
  onCreateThread: () => Promise<void>;
  onChipClick: (label: string) => Promise<void>;
}

export function ChatEmptyState({
  t,
  onCreateThread,
  onChipClick,
}: Readonly<ChatEmptyStateProps>) {
  return (
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
            onClick={() => onChipClick(chip.label)}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium text-[var(--text-secondary)] bg-[var(--surface-2)] border border-[var(--border-subtle)] hover:bg-[var(--surface-3)] hover:text-[var(--text-primary)] hover:border-[var(--border-default)] transition-all duration-200 hover:shadow-sm"
          >
            <span>{chip.icon}</span>
            {chip.label}
          </button>
        ))}
      </div>

      {/* New chat CTA */}
      <button
        onClick={onCreateThread}
        className="px-6 py-2.5 rounded-xl text-sm font-semibold text-white bg-[var(--accent)] hover:bg-[var(--accent-hover)] transition-all duration-200 shadow-sm hover:shadow-md"
      >
        {t.dashboard.startNewChat}
      </button>
    </div>
  );
}
