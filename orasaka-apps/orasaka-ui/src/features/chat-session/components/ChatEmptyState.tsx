"use client";

import React from "react";
import type { TranslationDictionary } from "@/core/context/LocaleContext";
import { WelcomeHero } from "./WelcomeHero";

interface ChatEmptyStateProps {
  t: TranslationDictionary;
  onCreateThread: () => Promise<void>;
  onChipClick: (label: string) => Promise<void>;
}

/**
 * ChatEmptyState — Renders the WelcomeHero greeting and a "Start Conversation"
 * CTA when no active conversation is selected. Composes the typewriter greeting
 * from WelcomeHero and adds the actionable new-chat button.
 */
export function ChatEmptyState({
  t,
  onCreateThread,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onChipClick,
}: Readonly<ChatEmptyStateProps>) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center text-center px-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Typewriter greeting */}
      <WelcomeHero t={t} />

      {/* New chat CTA — fades in after greeting */}
      <button
        onClick={onCreateThread}
        className="mt-8 px-6 py-2.5 text-sm font-semibold text-white bg-[var(--accent)] hover:bg-[var(--accent-hover)] transition-all duration-200 shadow-sm hover:shadow-md animate-in fade-in duration-700 [animation-delay:1200ms] [animation-fill-mode:backwards]"
      >
        {t.dashboard.startNewChat}
      </button>
    </div>
  );
}
