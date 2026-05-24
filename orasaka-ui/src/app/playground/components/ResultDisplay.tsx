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
  const format = typeof payload === "object" ? payload?.format : undefined;

  if (!rawUrl) {
    return (
      <p className="text-zinc-500 dark:text-zinc-400 text-sm">
        No asset output returned.
      </p>
    );
  }

  const isVideoData = format === "mp4" || rawUrl.startsWith("data:video/");

  if (isVideoData) {
    return (
      <div className="mt-4 flex flex-col items-center justify-center rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-950/20 p-4">
        <video
          src={rawUrl}
          controls
          autoPlay
          loop
          className="max-h-[512px] w-full max-w-[512px] rounded-md bg-black shadow-md"
        />
        <span className="mt-2 text-xs text-zinc-500 dark:text-zinc-400 font-semibold uppercase tracking-wider">
          Local Bare-Metal C++ Inference (MP4)
        </span>
      </div>
    );
  }

  const isAudioData = rawUrl.startsWith("data:audio/");

  if (isAudioData) {
    return (
      <div className="mt-4 flex flex-col items-center justify-center rounded-xl border border-zinc-200 dark:border-zinc-800/80 bg-zinc-50/40 dark:bg-zinc-900/10 p-6 shadow-sm backdrop-blur-sm transition-all duration-300">
        <div className="w-12 h-12 rounded-full bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 flex items-center justify-center mb-3 animate-pulse">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="w-6 h-6"
          >
            <path d="M9 18V5l12-2v13" />
            <circle cx="6" cy="18" r="3" />
            <circle cx="18" cy="16" r="3" />
          </svg>
        </div>
        <audio
          src={rawUrl}
          controls
          className="w-full max-w-[400px] rounded-lg shadow-inner focus:outline-none"
        />
        <span className="mt-3 text-[10px] text-zinc-400 dark:text-zinc-500 font-semibold uppercase tracking-widest">
          Local Bare-Metal Generation (MP3)
        </span>
      </div>
    );
  }

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
