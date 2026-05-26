import React from "react";
import type { TranslationDictionary } from "@/core/context/translations.types";
import { NODE_ID } from "@/core/constants/capability.constants";

interface NodeHintsProps {
  nodeId: string;
  isLocked: boolean;
  inputs: Record<string, string>;
  onInputChange: (field: string, value: string) => void;
  t: TranslationDictionary;
}

/**
 * Renders node-specific contextual hints, example prompts, and style tokens
 * for vision, audio, video, image, and speech playground nodes.
 */
export function NodeHints({
  nodeId,
  isLocked,
  inputs,
  onInputChange,
  t,
}: Readonly<NodeHintsProps>) {
  return (
    <>
      {/* Vision guidelines */}
      {nodeId === NODE_ID.MEDIA_VISION && (
        <div className="text-xs text-[var(--text-secondary)] bg-[var(--surface-1)] p-3 rounded-lg border border-[var(--border-subtle)] leading-relaxed">
          <p className="font-semibold text-[var(--text-primary)] mb-1">
            {t.playground.posterGuidelines}
          </p>
          <div className="flex items-center gap-1.5 mt-1.5">
            <span className="text-[10px] font-bold text-[var(--text-muted)] uppercase">
              {t.playground.tryLabel}
            </span>
            <button
              type="button"
              disabled={isLocked}
              onClick={() =>
                onInputChange("prompt", "Identify elements in this poster")
              }
              className="text-[11px] text-amber-500 hover:text-amber-600 underline font-medium"
            >
              &quot;Identify elements in this poster&quot;
            </button>
          </div>
        </div>
      )}

      {/* Audio guidelines */}
      {nodeId === NODE_ID.MEDIA_AUDIO && (
        <div className="text-xs text-[var(--text-secondary)] bg-[var(--surface-1)] p-3 rounded-lg border border-[var(--border-subtle)] leading-relaxed">
          <p className="font-semibold text-[var(--text-primary)]">
            {t.playground.audioGuidelines}
          </p>
        </div>
      )}

      {/* Image style tokens */}
      {nodeId === NODE_ID.CHAT_IMAGE && (
        <div className="flex flex-col gap-1.5 mt-1">
          <span className="text-[10px] font-semibold text-[var(--text-muted)] uppercase tracking-wider">
            {t.playground.imageTokensLabel}
          </span>
          <div className="flex flex-wrap gap-1.5">
            {[
              t.playground.imageTokenIllustration,
              t.playground.imageTokenCyberpunk,
              t.playground.imageTokenPhotorealistic,
              t.playground.imageTokenAntigravity,
            ].map((token) => (
              <button
                key={token}
                type="button"
                disabled={isLocked}
                onClick={() => {
                  const currentVal = inputs["prompt"] || "";
                  const newVal = currentVal ? `${currentVal}, ${token}` : token;
                  onInputChange("prompt", newVal);
                }}
                className="text-xs px-2.5 py-1 rounded-full border border-[var(--border-default)] bg-[var(--surface-1)] hover:bg-[var(--surface-2)] text-[var(--text-secondary)] font-medium transition-colors cursor-pointer select-none"
              >
                +{token}
              </button>
            ))}
            <button
              key="verification-prompt"
              type="button"
              disabled={isLocked}
              onClick={() => {
                onInputChange("prompt", t.playground.illustrationExamplePrompt);
              }}
              className="text-xs px-2.5 py-1 rounded-full border border-[var(--border-default)] bg-[var(--surface-1)] hover:bg-[var(--surface-2)] text-[var(--text-secondary)] font-medium transition-colors cursor-pointer select-none"
            >
              &quot;{t.playground.illustrationExamplePrompt}&quot;
            </button>
          </div>
        </div>
      )}
    </>
  );
}
