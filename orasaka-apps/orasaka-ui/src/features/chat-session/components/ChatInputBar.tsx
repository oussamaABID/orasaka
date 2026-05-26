/* eslint-disable no-restricted-syntax */
"use client";

import React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/icon";
import type { BootstrapFeature } from "./ContextPlusMenu";
import { ContextPlusMenu } from "./ContextPlusMenu";
import type { TranslationDictionary } from "@/core/context/LocaleContext";

interface ChatIndicatorProps {
  selectedFeature: BootstrapFeature | null;
  attachment: { assetId: string; name: string } | null;
  isUploadingAttachment: boolean;
  onClearFeature: () => void;
  onClearAttachment: () => void;
  t: TranslationDictionary;
}

const ChatIndicators: React.FC<ChatIndicatorProps> = ({
  selectedFeature,
  attachment,
  isUploadingAttachment,
  onClearFeature,
  onClearAttachment,
  t,
}) => {
  if (!selectedFeature && !attachment && !isUploadingAttachment) return null;
  return (
    <section className="flex flex-wrap gap-2 px-1 border-b border-[var(--border-subtle)] pb-2 mb-1">
      {selectedFeature && (
        <article className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
          <span>⚡ {selectedFeature.label}</span>
          <button
            type="button"
            onClick={onClearFeature}
            className="hover:text-amber-800 dark:hover:text-amber-200 transition-colors ml-1 font-bold"
          >
            ✕
          </button>
        </article>
      )}
      {attachment && (
        <article className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
          <span>📎 {attachment.name}</span>
          <button
            type="button"
            onClick={onClearAttachment}
            className="hover:text-emerald-800 dark:hover:text-emerald-200 transition-colors ml-1 font-bold"
          >
            ✕
          </button>
        </article>
      )}
      {isUploadingAttachment && (
        <span className="text-xs text-[var(--text-muted)]">
          {t.chat.uploadingFile}
        </span>
      )}
    </section>
  );
};

interface ChatInputBarProps {
  input: string;
  onInputChange: (value: string) => void;
  onSubmit: (e: React.SubmitEvent<HTMLFormElement>) => void;
  isSending: boolean;
  isGenerating: boolean;
  isUploadingAttachment: boolean;
  selectedFeature: BootstrapFeature | null;
  attachment: { assetId: string; name: string } | null;
  onClearFeature: () => void;
  onClearAttachment: () => void;
  isPlusMenuOpen: boolean;
  onTogglePlusMenu: () => void;
  onClosePlusMenu: () => void;
  onExecuteNode: (feature: BootstrapFeature) => void;
  bootstrapFeatures: BootstrapFeature[];
  fileInputRef: React.RefObject<HTMLInputElement | null>;
  onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  t: TranslationDictionary;
  /** Controls Framer Motion layout mode: true = centered, false = bottom-docked */
  isCentered?: boolean;
}

export const ChatInputBar: React.FC<ChatInputBarProps> = ({
  input,
  onInputChange,
  onSubmit,
  isSending,
  isGenerating,
  isUploadingAttachment,
  selectedFeature,
  attachment,
  onClearFeature,
  onClearAttachment,
  isPlusMenuOpen,
  onTogglePlusMenu,
  onClosePlusMenu,
  onExecuteNode,
  bootstrapFeatures,
  fileInputRef,
  onFileChange,
  t,
  isCentered = false,
}) => {
  /* ── Input Blocking Invariant ─────────────────────────────
   * When the agent is actively thinking/streaming, the entire
   * input surface is locked: textarea, attach, plus-menu,
   * and submit button. Matches Gemini's interaction model.
   * ───────────────────────────────────────────────────────── */
  const isAgentBusy = isSending || isGenerating;

  const isSubmitDisabled =
    isAgentBusy ||
    isUploadingAttachment ||
    (!input.trim() && !attachment && !selectedFeature);

  return (
    <AnimatePresence mode="wait">
      <motion.form
        key="chat-input-form"
        layoutId="chat-input-bar"
        onSubmit={onSubmit}
        className={`w-full bg-transparent flex flex-col items-center relative z-25 ${
          isCentered
            ? "px-6 py-0 flex-1 justify-center"
            : "px-4 pb-5 md:pb-7 pt-0"
        }`}
        layout
        transition={{
          layout: { type: "spring", stiffness: 300, damping: 30, mass: 0.8 },
        }}
      >
        <motion.section
          layoutId="chat-input-card"
          className={`glass-card w-full shadow-xl p-3 flex flex-col gap-2 transition-[border-color,box-shadow] duration-300 focus-within:border-[var(--accent)] focus-within:shadow-[var(--accent-glow)] ${
            isCentered ? "max-w-2xl" : "max-w-3xl"
          }`}
          layout
        >
          <ChatIndicators
            selectedFeature={selectedFeature}
            attachment={attachment}
            isUploadingAttachment={isUploadingAttachment}
            onClearFeature={onClearFeature}
            onClearAttachment={onClearAttachment}
            t={t}
          />

          <section className="flex gap-3 items-end relative">
            <ContextPlusMenu
              isOpen={isPlusMenuOpen}
              onClose={onClosePlusMenu}
              onExecuteNode={onExecuteNode}
              features={bootstrapFeatures}
            />
            <button
              type="button"
              onClick={onTogglePlusMenu}
              disabled={isAgentBusy}
              className={`p-2.5 bg-[var(--surface-2)] hover:bg-[var(--surface-3)] border border-[var(--border-subtle)] text-[var(--text-secondary)] transition-all duration-150 flex items-center justify-center flex-shrink-0 mb-0.5 ${
                isAgentBusy ? "opacity-40 cursor-not-allowed pointer-events-none" : ""
              }`}
              aria-label={t.chat.addCapability}
            >
              <svg
                className={`w-5 h-5 transition-transform duration-200 ${isPlusMenuOpen ? "rotate-45" : ""}`}
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2}
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M12 4.5v15m7.5-7.5h-15"
                />
              </svg>
            </button>

            <input
              type="file"
              ref={fileInputRef}
              className="hidden"
              onChange={onFileChange}
            />

            <section className="flex-1 relative flex items-end">
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isAgentBusy}
                className={`absolute left-3 bottom-3 p-1.5 text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-all duration-150 z-10 ${
                  isAgentBusy ? "opacity-40 cursor-not-allowed pointer-events-none" : ""
                }`}
                aria-label="Attach File"
              >
                <Icon name="attach" size={20} />
              </button>
              <textarea
                data-testid="chat-input"
                value={input}
                onChange={(e) => onInputChange(e.target.value)}
                onKeyDown={(e) => {
                  if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
                    e.preventDefault();
                    if (!isSubmitDisabled) {
                      const form = e.currentTarget.closest("form");
                      form?.requestSubmit();
                    }
                  }
                }}
                disabled={isAgentBusy}
                placeholder={isAgentBusy ? t.chat.typing : t.chat.typeMessage}
                rows={1}
                className={`w-full pl-10 pr-24 py-3 bg-transparent border-0 text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-0 resize-none overflow-hidden text-sm leading-relaxed transition-opacity duration-200 ${
                  isAgentBusy ? "opacity-50 cursor-not-allowed" : ""
                }`}
                style={{
                  minHeight: "44px",
                  maxHeight: "140px",
                  height: "auto",
                }}
                ref={(el) => {
                  if (el) {
                    el.style.height = "auto";
                    el.style.height = `${Math.min(el.scrollHeight, 140)}px`;
                    el.style.overflow = el.scrollHeight > 140 ? "auto" : "hidden";
                  }
                }}
                autoComplete="off"
              />
              <section className="absolute right-2 bottom-1.5 flex items-center gap-2">
                <span className="text-[10px] text-[var(--text-muted)] hidden sm:inline-block select-none">
                  {t.chat.cmdEnterHint}
                </span>
                <Button
                  type="submit"
                  disabled={isSubmitDisabled}
                  data-testid="chat-submit"
                  className="px-4 py-1.5 h-8 text-xs font-semibold group"
                >
                  {(() => {
                    if (isSending) return t.chat.sending;
                    if (isGenerating) return t.chat.typing;
                    return (
                    <span className="flex items-center gap-1.5">
                      {t.chat.send}
                      <svg
                        className="w-3.5 h-3.5 transition-transform duration-200 group-hover:translate-x-0.5"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth={2.5}
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3"
                        />
                      </svg>
                    </span>
                    );
                  })()}
                </Button>
              </section>
            </section>
          </section>
        </motion.section>
      </motion.form>
    </AnimatePresence>
  );
};
