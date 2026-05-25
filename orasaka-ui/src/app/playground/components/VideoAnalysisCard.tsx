"use client";

import React from "react";
import { Film, Upload, Loader2, CheckCircle2, AlertCircle } from "lucide-react";

const ACCEPTED_TYPES = ["video/mp4", "video/quicktime"];
const MAX_FILE_SIZE_MB = 100;

type AnalysisState = "idle" | "uploading" | "processing" | "success" | "error";

interface AnalysisResult {
  transcript: string;
  keyframeCount: number;
}

export function VideoAnalysisCard() {
  const [state, setState] = React.useState<AnalysisState>("idle");
  const [result, setResult] = React.useState<AnalysisResult | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [dragOver, setDragOver] = React.useState(false);
  const inputRef = React.useRef<HTMLInputElement>(null);

  const handleFile = async (file: File) => {
    if (!ACCEPTED_TYPES.includes(file.type)) {
      setError("Only MP4 and QuickTime files are supported.");
      setState("error");
      return;
    }
    if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
      setError(`File exceeds ${MAX_FILE_SIZE_MB}MB limit.`);
      setState("error");
      return;
    }

    setState("uploading");
    setError(null);
    setResult(null);

    try {
      const formData = new FormData();
      formData.append("video", file);

      setState("processing");
      const response = await fetch("/api/video/analyze", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Analysis failed: ${response.statusText}`);
      }

      const data = await response.json();
      setResult({
        transcript: data.transcript ?? "",
        keyframeCount: data.keyframeCount ?? 0,
      });
      setState("success");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "An unexpected error occurred.",
      );
      setState("error");
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  };

  const reset = () => {
    setState("idle");
    setResult(null);
    setError(null);
    if (inputRef.current) inputRef.current.value = "";
  };

  return (
    <article
      className={`
        relative rounded-xl border p-6 transition-all duration-200
        ${
          dragOver
            ? "border-violet-500 bg-violet-50 dark:bg-violet-950/30"
            : "border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-900/50"
        }
      `}
      onDragOver={(e) => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
    >
      <header className="flex items-center gap-3 mb-4">
        <Film className="h-6 w-6 text-violet-500" />
        <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
          Video Analysis
        </h3>
      </header>

      {state === "idle" && (
        <section className="flex flex-col items-center gap-4 py-6">
          <p className="text-sm text-zinc-500 dark:text-zinc-400 text-center max-w-xs">
            Drop a video file here or click to browse. Supports MP4 and
            QuickTime up to {MAX_FILE_SIZE_MB}MB.
          </p>
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            className="flex items-center gap-2 rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-violet-700 active:bg-violet-800"
          >
            <Upload className="h-4 w-4" />
            Select Video
          </button>
          <input
            ref={inputRef}
            type="file"
            accept={ACCEPTED_TYPES.join(",")}
            onChange={handleInputChange}
            className="hidden"
          />
        </section>
      )}

      {(state === "uploading" || state === "processing") && (
        <section className="flex flex-col items-center gap-3 py-8">
          <Loader2 className="h-8 w-8 animate-spin text-violet-500" />
          <p className="text-sm font-medium text-zinc-600 dark:text-zinc-300">
            {state === "uploading"
              ? "Uploading video…"
              : "Extracting keyframes & transcript…"}
          </p>
        </section>
      )}

      {state === "success" && result && (
        <section className="space-y-3">
          <p className="flex items-center gap-2 text-sm font-medium text-emerald-600 dark:text-emerald-400">
            <CheckCircle2 className="h-4 w-4" />
            Analysis complete
          </p>
          <dl className="grid grid-cols-2 gap-2 text-sm">
            <dt className="text-zinc-500 dark:text-zinc-400">Keyframes</dt>
            <dd className="font-mono text-zinc-900 dark:text-zinc-100">
              {result.keyframeCount}
            </dd>
            <dt className="text-zinc-500 dark:text-zinc-400">Transcript</dt>
            <dd className="font-mono text-zinc-900 dark:text-zinc-100 truncate">
              {result.transcript || "—"}
            </dd>
          </dl>
          <button
            type="button"
            onClick={reset}
            className="mt-2 text-xs text-violet-600 hover:text-violet-700 dark:text-violet-400 dark:hover:text-violet-300 underline"
          >
            Analyze another video
          </button>
        </section>
      )}

      {state === "error" && (
        <section className="flex flex-col items-center gap-3 py-6">
          <AlertCircle className="h-6 w-6 text-red-500" />
          <p className="text-sm text-red-600 dark:text-red-400 text-center">
            {error}
          </p>
          <button
            type="button"
            onClick={reset}
            className="text-xs text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200 underline"
          >
            Try again
          </button>
        </section>
      )}
    </article>
  );
}
