import React from "react";
import { useJobStream } from "@/features/jobs/context/JobStreamContext";
import { Loader2, CheckCircle, AlertCircle } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";

/**
 * Props definition for the ResultDisplay component.
 */
interface ResultDisplayProps {
  payload: unknown;
}

/**
 * Component to defensively format and display local AI generation assets or terminal logs,
 * with support for asynchronous job tracking.
 */
export function ResultDisplay({ payload }: ResultDisplayProps) {
  const { jobs } = useJobStream();
  const { t } = useTranslation();

  let jobId: string | undefined = undefined;
  if (typeof payload === "object" && payload !== null) {
    if ("jobId" in payload) {
      jobId = (payload as Record<string, unknown>).jobId as string;
    }
  } else if (typeof payload === "string") {
    try {
      const parsed = JSON.parse(payload);
      if (parsed && typeof parsed === "object" && "jobId" in parsed) {
        jobId = parsed.jobId;
      }
    } catch {
      // Not a JSON string with a jobId
    }
  }

  if (jobId) {
    const job = jobs.find((j) => j.id === jobId);
    if (!job) {
      return (
        <div className="flex items-center gap-2 text-zinc-500 py-2">
          <Loader2 className="h-4 w-4 animate-spin text-cyan-500" />
          <span>{t.playground.taskQueued.replace("{jobId}", jobId)}</span>
        </div>
      );
    }

    if (job.status === "PENDING" || job.status === "PROCESSING") {
      return (
        <div className="flex flex-col gap-2 p-4 rounded-lg bg-zinc-50/50 dark:bg-zinc-950/20 border border-dashed border-zinc-200 dark:border-zinc-800">
          <div className="flex items-center gap-2 text-zinc-700 dark:text-zinc-300 font-semibold text-sm">
            <Loader2 className="h-4 w-4 animate-spin text-cyan-500" />
            <span>{t.playground.taskActive}</span>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400">
            {t.playground.status}
            <span className="font-semibold uppercase text-cyan-600 dark:text-cyan-400">
              {job.status}
            </span>
          </p>
          <p className="text-[10px] text-zinc-400 font-mono">
            {t.playground.jobId} {jobId}
          </p>
        </div>
      );
    }

    if (job.status === "FAILED") {
      return (
        <div className="flex flex-col gap-2 p-4 rounded-lg bg-red-50 dark:bg-red-950/10 border border-red-200 dark:border-red-900/30 text-red-600 dark:text-red-400">
          <div className="flex items-center gap-1.5 font-semibold text-sm">
            <AlertCircle className="h-4 w-4 text-red-500" />
            <span>{t.playground.taskFailed}</span>
          </div>
          <p className="text-xs font-mono whitespace-pre-wrap leading-relaxed">
            {job.errorMessage || t.playground.unknownError}
          </p>
        </div>
      );
    }

    if (job.status === "COMPLETED") {
      const resultObj = job.result;
      if (!resultObj) {
        return (
          <div className="flex items-center gap-2 text-zinc-500 py-2">
            <CheckCircle className="h-4 w-4 text-green-500" />
            <span>{t.playground.taskCompletedNoOutput}</span>
          </div>
        );
      }

      // Render the completed result using the same visual patterns
      return <ResultDisplayInner payload={resultObj} />;
    }
  }

  return <ResultDisplayInner payload={payload} />;
}

function ResultDisplayInner({ payload }: ResultDisplayProps) {
  const { t } = useTranslation();
  const p =
    typeof payload === "object" && payload !== null
      ? (payload as Record<string, unknown>)
      : null;
  const rawUrl = p ? (p.url as string) : (payload as string);
  const format = p ? (p.format as string) : undefined;
  const content = p ? (p.content as string) : undefined;
  const analysis = p ? (p.analysis as string) : undefined;

  const absoluteUrl = React.useMemo(() => {
    if (typeof window !== "undefined" && rawUrl && rawUrl.startsWith("/")) {
      return `${window.location.origin}${rawUrl}`;
    }
    return rawUrl;
  }, [rawUrl]);

  if (p) {
    if ("transcript" in p || "keyframeCount" in p) {
      return (
        <div className="flex flex-col gap-2 mt-2">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold px-2 py-0.5 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
              {t.playground.keyframesExtracted}{" "}
              {(p.keyframeCount as number) ?? 0}
            </span>
          </div>
          <div className="rounded-lg bg-zinc-50 dark:bg-zinc-950/40 p-4 border border-zinc-200 dark:border-zinc-800 text-sm">
            <span className="text-xs font-semibold text-zinc-400 uppercase tracking-widest block mb-2">
              {t.playground.transcript}
            </span>
            <p className="text-zinc-700 dark:text-zinc-300 leading-relaxed italic">
              {(p.transcript as string) || t.playground.noDialogueDetected}
            </p>
          </div>
        </div>
      );
    }
  }

  if ((content || analysis) && !rawUrl) {
    return (
      <pre className="mt-2 overflow-x-auto rounded bg-zinc-950 p-4 font-mono text-xs text-zinc-50 whitespace-pre-wrap break-all">
        {content || analysis}
      </pre>
    );
  }

  if (!rawUrl) {
    return (
      <p className="text-zinc-500 dark:text-zinc-400 text-sm">
        {t.playground.noAssetOutput}
      </p>
    );
  }

  const isVideoData =
    format === "mp4" ||
    (rawUrl &&
      (rawUrl.startsWith("data:video/") ||
        rawUrl.endsWith(".mp4") ||
        (rawUrl.includes("/uploads/") && rawUrl.endsWith(".mp4"))));

  if (isVideoData) {
    return (
      <div className="mt-4 flex flex-col items-center justify-center rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-950/20 p-4">
        <video
          src={absoluteUrl}
          controls
          autoPlay
          muted
          playsInline
          loop
          className="max-h-[512px] w-full max-w-[512px] rounded-md bg-black shadow-md"
        />
        <span className="mt-2 text-xs text-zinc-500 dark:text-zinc-400 font-semibold uppercase tracking-wider">
          {t.playground.localCPlusPlusInference}
        </span>
      </div>
    );
  }

  const isAudioData =
    format === "mp3" ||
    (rawUrl &&
      (rawUrl.startsWith("data:audio/") ||
        rawUrl.endsWith(".mp3") ||
        rawUrl.endsWith(".wav") ||
        (rawUrl.includes("/uploads/") &&
          (rawUrl.endsWith(".mp3") || rawUrl.endsWith(".wav")))));

  if (isAudioData) {
    return (
      <div className="mt-4 flex flex-col items-center justify-center rounded-xl border border-zinc-200 dark:border-zinc-800/80 bg-zinc-50/40 dark:bg-zinc-900/10 p-6 shadow-sm backdrop-blur-sm transition-all duration-300">
        <audio
          src={absoluteUrl}
          controls
          preload="metadata"
          className="w-full max-w-[400px] rounded-lg shadow-inner focus:outline-none"
        />
        <span className="mt-3 text-[10px] text-zinc-400 dark:text-zinc-500 font-semibold uppercase tracking-widest">
          {t.playground.localAudioGeneration}
        </span>
      </div>
    );
  }

  const isImageData =
    rawUrl &&
    (rawUrl.startsWith("data:image/") ||
      rawUrl.startsWith("data:application/") ||
      format === "png" ||
      format === "jpg" ||
      format === "jpeg" ||
      rawUrl.endsWith(".png") ||
      rawUrl.endsWith(".jpg") ||
      rawUrl.endsWith(".jpeg") ||
      (rawUrl.includes("/uploads/") &&
        (rawUrl.endsWith(".png") ||
          rawUrl.endsWith(".jpg") ||
          rawUrl.endsWith(".jpeg"))));

  if (isImageData) {
    return (
      <div className="mt-4 flex flex-col items-center justify-center rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-950/20 p-4">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={absoluteUrl}
          alt="Orasaka Framework Local AI Generation"
          className="max-h-[512px] w-full max-w-[512px] rounded-md object-contain shadow-md transition-all duration-300"
          loading="lazy"
        />
        <span className="mt-2 text-xs text-zinc-500 dark:text-zinc-400 font-semibold uppercase tracking-wider">
          {t.playground.localImageGeneration}
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
