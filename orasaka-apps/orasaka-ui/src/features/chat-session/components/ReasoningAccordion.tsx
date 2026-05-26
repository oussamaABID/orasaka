/* eslint-disable no-restricted-syntax */
"use client";

import React, { useState } from "react";
import { Icon } from "@/components/ui/icon";

interface ReasoningAccordionProps {
  /** Raw thought content extracted from <thought> tags */
  content: string;
  /** Whether the stream is still generating */
  isStreaming?: boolean;
}

/**
 * Collapsible accordion for rendering streamed reasoning blocks.
 *
 * When the backend returns tokens wrapped in <thought> tags and
 * `streamReasoning` is enabled, this component renders the
 * thinking process in an animated, Krizaka-styled container.
 *
 * Design: frosted-glass obsidian panel with ice-blue accent border,
 * zero-radius geometry (inherits from .theme-krizaka).
 */
export function ReasoningAccordion({
  content,
  isStreaming = false,
}: Readonly<ReasoningAccordionProps>) {
  const [isExpanded, setIsExpanded] = useState(false);

  if (!content) return null;

  return (
    <section
      className="my-2 border border-[var(--border-subtle)] bg-[var(--surface-1)]/80 backdrop-blur-md overflow-hidden transition-all duration-300"
      style={{ borderRadius: "var(--radius-md)" }}
    >
      {/* Toggle header */}
      <button
        type="button"
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full flex items-center gap-2 px-4 py-2.5 text-left transition-colors duration-150 hover:bg-[var(--surface-2)]/50"
        aria-expanded={isExpanded}
        aria-label="Toggle reasoning view"
      >
        <Icon
          name="spark"
          size={14}
          className={`text-[var(--accent)] transition-transform duration-200 ${
            isStreaming ? "animate-spin-slow" : ""
          }`}
        />
        <span className="text-[11px] font-semibold uppercase tracking-wider text-[var(--text-muted)]">
          {isStreaming ? "Thinking…" : "Reasoning"}
        </span>

        {/* Streaming pulse indicator */}
        {isStreaming && (
          <span className="flex items-center gap-1 ml-auto">
            <span className="w-1.5 h-1.5 rounded-full bg-[var(--accent)] animate-bounce" style={{ animationDelay: "0ms" }} />
            <span className="w-1.5 h-1.5 rounded-full bg-[var(--accent)] animate-bounce" style={{ animationDelay: "150ms" }} />
            <span className="w-1.5 h-1.5 rounded-full bg-[var(--accent)] animate-bounce" style={{ animationDelay: "300ms" }} />
          </span>
        )}

        <Icon
          name="chevronDown"
          size={12}
          className={`ml-auto text-[var(--text-muted)] transition-transform duration-200 ${
            isExpanded ? "rotate-180" : ""
          }`}
        />
      </button>

      {/* Collapsible body */}
      <div
        className={`transition-all duration-300 ease-in-out overflow-hidden ${
          isExpanded ? "max-h-[600px] opacity-100" : "max-h-0 opacity-0"
        }`}
      >
        <div className="px-4 pb-3 border-t border-[var(--border-subtle)]">
          <pre className="mt-2 text-[11px] leading-relaxed text-[var(--text-secondary)] font-mono whitespace-pre-wrap break-words max-h-[500px] overflow-y-auto">
            {content}
          </pre>
        </div>
      </div>
    </section>
  );
}

/**
 * Utility: Extract <thought> blocks from a raw streamed response.
 *
 * @param raw - The full streamed content string
 * @returns An object with `reasoning` (thought content) and `response` (clean output)
 */
export function parseThoughtTags(raw: string): {
  reasoning: string;
  response: string;
} {
  const thoughtRegex = /<thought>([\s\S]*?)<\/thought>/g;
  const thoughts: string[] = [];
  let match: RegExpExecArray | null;

  while ((match = thoughtRegex.exec(raw)) !== null) {
    thoughts.push(match[1].trim());
  }

  const response = raw.replace(thoughtRegex, "").trim();

  return {
    reasoning: thoughts.join("\n\n"),
    response,
  };
}
