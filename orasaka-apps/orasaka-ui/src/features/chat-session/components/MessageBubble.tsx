/* eslint-disable no-restricted-syntax */
"use client";

import React from "react";
import { format } from "date-fns";
import { Icon } from "@/components/ui/icon";
import { ChatMessage } from "@/features/chat-session/types/chat.types";
import { useTranslation } from "@/core/context/LocaleContext";

interface Props {
  message: ChatMessage;
  /** Stagger index for slide-in animation delay */
  index?: number;
}

/**
 * Individual chat message bubble — Calm Obsidian 2026 design.
 *
 * User messages align right with accent-tinted surface.
 * AI messages align left with surface-1 background.
 * Includes hover-to-copy action bar and staggered slide-in.
 */
export const MessageBubble: React.FC<Props> = ({ message, index = 0 }) => {
  const isUser = message.role === "user";
  const { t } = useTranslation();
  const [copied, setCopied] = React.useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard API not available
    }
  };

  return (
    <article
      className={`flex w-full mb-4 group/msg ${isUser ? "justify-end" : "justify-start"}`}
      style={{
        animationDelay: `${Math.min(index * 50, 300)}ms`,
        animationFillMode: "backwards",
      }}
    >
      <section
        className={`flex max-w-[75%] gap-3 items-start animate-in fade-in duration-300 ${
          isUser
            ? "flex-row-reverse slide-in-from-right-3"
            : "flex-row slide-in-from-left-3"
        }`}
        style={{
          animationDelay: `${Math.min(index * 50, 300)}ms`,
          animationFillMode: "backwards",
        }}
      >
        {/* Avatar */}
        <figure
          className={`h-9 w-9 rounded-2xl flex items-center justify-center text-[11px] font-bold border select-none flex-shrink-0 transition-colors duration-200 ${
            isUser
              ? "bg-[var(--accent-soft)] border-[var(--accent)]/20 text-[var(--accent)]"
              : "bg-[var(--surface-2)] border-[var(--border-subtle)] text-[var(--text-secondary)]"
          }`}
        >
          {isUser ? "U" : "AI"}
        </figure>

        {/* Text bubble + actions */}
        <section className="relative">
          <section
            className={`p-4 rounded-2xl border text-[13px] leading-[1.7] text-[var(--text-primary)] transition-shadow duration-200 ${
              isUser
                ? "bg-[var(--accent-soft)] border-[var(--accent)]/10 rounded-tr-md"
                : "bg-[var(--surface-1)] border-[var(--border-subtle)] rounded-tl-md"
            }`}
          >
            <p className="whitespace-pre-wrap">{message.content}</p>
            <span className="text-[10px] text-[var(--text-muted)] block mt-2 text-right select-none">
              {format(message.timestamp, "HH:mm")}
            </span>
          </section>

          {/* Hover action bar */}
          <section
            className={`absolute -bottom-3 opacity-0 group-hover/msg:opacity-100 transition-all duration-200 flex items-center gap-1 ${
              isUser ? "right-0" : "left-0"
            }`}
          >
            <button
              type="button"
              onClick={handleCopy}
              className="inline-flex items-center gap-1 px-2 py-1 rounded-lg text-[10px] font-medium bg-[var(--surface-2)] border border-[var(--border-subtle)] text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-3)] transition-all duration-150 shadow-sm"
              aria-label={t.chat.copyMessage}
            >
              {copied ? (
                <>
                  <Icon name="check" size={12} className="text-emerald-500" />
                  <span className="text-emerald-500">
                    {t.chat.copiedMessage}
                  </span>
                </>
              ) : (
                <>
                  <Icon name="copy" size={12} />
                  <span>{t.chat.copyMessage}</span>
                </>
              )}
            </button>
          </section>
        </section>
      </section>
    </article>
  );
};
