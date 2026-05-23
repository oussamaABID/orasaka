import React from "react";

/**
 * Props definition for the ResultDisplay component.
 */
interface ResultDisplayProps {
  payload:
    | {
        url?: string;
        format?: string;
      }
    | string;
}

/**
 * Component to defensively format and display local AI generation assets or terminal logs.
 */
export function ResultDisplay({ payload }: ResultDisplayProps) {
  const rawUrl = typeof payload === "object" ? payload?.url : payload;

  if (!rawUrl) {
    return (
      <p className="text-zinc-500 dark:text-zinc-400 text-sm">
        No asset output returned.
      </p>
    );
  }

  // Defensive check: Verify if the payload is an encoded Base64 Data URL asset
  const isImageData =
    rawUrl.startsWith("data:image/") || rawUrl.startsWith("data:application/");

  if (isImageData) {
    return (
      <div className="mt-4 flex flex-col items-center justify-center rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-950/20 p-4">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={rawUrl}
          alt="Orasaka Framework Local AI Generation"
          className="max-h-[512px] w-full max-w-[512px] rounded-md object-contain shadow-md transition-all duration-300"
          loading="lazy"
        />
        <span className="mt-2 text-xs text-zinc-500 dark:text-zinc-400 font-semibold uppercase tracking-wider">
          Local Bare-Metal Generation (PNG)
        </span>
      </div>
    );
  }

  return (
    <pre className="mt-2 overflow-x-auto rounded bg-zinc-950 p-4 font-mono text-xs text-zinc-50 whitespace-pre-wrap break-all">
      {rawUrl}
    </pre>
  );
}
