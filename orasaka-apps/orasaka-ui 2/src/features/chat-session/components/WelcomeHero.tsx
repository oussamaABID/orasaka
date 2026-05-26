"use client";

import React, { useState, useMemo } from "react";
import { useAuth } from "@/core/hooks/useAuth";
import type { TranslationDictionary } from "@/core/context/LocaleContext";

interface WelcomeHeroProps {
  t: TranslationDictionary;
}

/**
 * WelcomeHero — Type-animated personalized greeting block.
 *
 * Displays a typewriter-animated greeting driven by the user profile context
 * and welcome_agent.md persona prompt (Option C: profile-only, zero external deps).
 * Shows user display name, time-of-day greeting, and accent-colored highlights
 * with a blinking cursor caret.
 */
export function WelcomeHero({ t }: Readonly<WelcomeHeroProps>) {
  const { user } = useAuth();
  const [displayedText, setDisplayedText] = useState("");
  const [isTypingComplete, setIsTypingComplete] = useState(false);

  // Time-of-day greeting context
  const greetingContext = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return { emoji: "☀️", period: "morning" };
    if (hour < 18) return { emoji: "🌤️", period: "afternoon" };
    return { emoji: "🌙", period: "evening" };
  }, []);

  const userName = user?.name || "there";
  const fullGreeting = `Good ${greetingContext.period}, ${userName}. What would you like to build today?`;

  // Typewriter effect — uses ref to avoid setState in effect body
  const indexRef = React.useRef(0);
  const intervalRef = React.useRef<ReturnType<typeof setInterval> | null>(null);

  React.useLayoutEffect(() => {
    indexRef.current = 0;
    if (intervalRef.current) clearInterval(intervalRef.current);

    intervalRef.current = setInterval(() => {
      if (indexRef.current < fullGreeting.length) {
        indexRef.current++;
        setDisplayedText(fullGreeting.slice(0, indexRef.current));
      } else {
        setIsTypingComplete(true);
        if (intervalRef.current) clearInterval(intervalRef.current);
      }
    }, 32);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [fullGreeting]);

  return (
    <div className="flex flex-col items-center justify-center text-center px-6 animate-in fade-in duration-500">
      {/* Greeting emoji */}
      <div className="text-4xl mb-4 animate-in fade-in zoom-in-50 duration-300">
        {greetingContext.emoji}
      </div>

      {/* Typewriter text */}
      <h2 className="text-[var(--text-xl)] font-bold tracking-tight text-[var(--text-primary)] mb-3 min-h-[2.5rem]">
        <span>{displayedText}</span>
        {!isTypingComplete && <span className="typewriter-cursor" />}
      </h2>

      {/* Subtitle — fades in after typing completes */}
      <p
        className={`text-[var(--text-sm)] text-[var(--text-muted)] max-w-md transition-opacity duration-500 ${
          isTypingComplete ? "opacity-100" : "opacity-0"
        }`}
      >
        {t.chat.startConversationDesc}
      </p>

      {/* Suggestion chips — stagger in after typing */}
      <div
        className={`flex flex-wrap justify-center gap-2.5 mt-8 transition-all duration-500 ${
          isTypingComplete
            ? "opacity-100 translate-y-0"
            : "opacity-0 translate-y-2"
        }`}
      >
        {[
          { label: t.chat.suggestionImage, icon: "🎨" },
          { label: t.chat.suggestionCode, icon: "⚡" },
          { label: t.chat.suggestionAsk, icon: "💬" },
        ].map((chip) => (
          <span
            key={chip.label}
            className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-medium text-[var(--text-secondary)] bg-[var(--surface-2)] border border-[var(--border-subtle)] transition-all duration-200 hover:bg-[var(--surface-3)] hover:text-[var(--text-primary)] hover:border-[var(--border-default)] cursor-default select-none"
          >
            <span>{chip.icon}</span>
            {chip.label}
          </span>
        ))}
      </div>
    </div>
  );
}
