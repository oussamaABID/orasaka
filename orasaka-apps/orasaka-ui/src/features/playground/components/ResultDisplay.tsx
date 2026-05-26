import React from "react";
import { useJobStream } from "@/core/context/JobStreamContext";
import { Loader2, CheckCircle, AlertCircle } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import { JOB_STATUS } from "@/core/constants/http.constants";

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
function getJobId(payload: unknown): string | undefined {
  if (typeof payload === "object" && payload !== null) {
    if ("jobId" in payload) {
      return (payload as Record<string, unknown>).jobId as string;
    }
  } else if (typeof payload === "string") {
    try {
      const parsed = JSON.parse(payload);
      if (parsed && typeof parsed === "object" && "jobId" in parsed) {
        return parsed.jobId;
      }
    } catch {
      // Not a JSON string with a jobId
    }
  }
  return undefined;
}

interface JobResultDisplayProps {
  job: { status: string; errorMessage?: string | null; result?: unknown };
  jobId: string;
}

function JobResultDisplay({ job, jobId }: Readonly<JobResultDisplayProps>) {
  const { t } = useTranslation();

  if (job.status === JOB_STATUS.PENDING || job.status === JOB_STATUS.PROCESSING) {
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

  if (job.status === JOB_STATUS.FAILED) {
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

  if (job.status === JOB_STATUS.COMPLETED) {
    const resultObj = job.result;
    if (!resultObj) {
      return (
        <div className="flex items-center gap-2 text-zinc-500 py-2">
          <CheckCircle className="h-4 w-4 text-green-500" />
          <span>{t.playground.taskCompletedNoOutput}</span>
        </div>
      );
    }
    return <ResultDisplayInner payload={resultObj} />;
  }

  return null;
}

export function ResultDisplay({ payload }: Readonly<ResultDisplayProps>) {
  const { jobs } = useJobStream();
  const { t } = useTranslation();

  const jobId = getJobId(payload);

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
    return <JobResultDisplay job={job} jobId={jobId} />;
  }

  return <ResultDisplayInner payload={payload} />;
}

function isVideo(format: string | undefined, url: string): boolean {
  return (
    format === "mp4" ||
    (!!url &&
      (url.startsWith("data:video/") ||
        url.endsWith(".mp4") ||
        (url.includes("/uploads/") && url.endsWith(".mp4"))))
  );
}

function isAudio(format: string | undefined, url: string): boolean {
  return (
    format === "mp3" ||
    (!!url &&
      (url.startsWith("data:audio/") ||
        url.endsWith(".mp3") ||
        url.endsWith(".wav") ||
        (url.includes("/uploads/") &&
          (url.endsWith(".mp3") || url.endsWith(".wav")))))
  );
}

function isImage(format: string | undefined, url: string): boolean {
  return (
    !!url &&
    (url.startsWith("data:image/") ||
      url.startsWith("data:application/") ||
      format === "png" ||
      format === "jpg" ||
      format === "jpeg" ||
      url.endsWith(".png") ||
      url.endsWith(".jpg") ||
      url.endsWith(".jpeg") ||
      (url.includes("/uploads/") &&
        (url.endsWith(".png") ||
          url.endsWith(".jpg") ||
          url.endsWith(".jpeg"))))
  );
}

function ResultDisplayInner({ payload }: Readonly<ResultDisplayProps>) {
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
    if (globalThis.window !== undefined && rawUrl?.startsWith("/")) {
      return `${globalThis.location.origin}${rawUrl}`;
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

  if (isVideo(format, rawUrl)) {
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
        >
          <track kind="captions" />
        </video>
        <span className="mt-2 text-xs text-zinc-500 dark:text-zinc-400 font-semibold uppercase tracking-wider">
          {t.playground.localCPlusPlusInference}
        </span>
      </div>
    );
  }

  if (isAudio(format, rawUrl)) {
    return (
      <div className="mt-4 flex flex-col items-center justify-center rounded-xl border border-zinc-200 dark:border-zinc-800/80 bg-zinc-50/40 dark:bg-zinc-900/10 p-6 shadow-sm backdrop-blur-sm transition-all duration-300">
        <audio
          src={absoluteUrl}
          controls
          preload="metadata"
          className="w-full max-w-[400px] rounded-lg shadow-inner focus:outline-none"
        >
          <track kind="captions" />
        </audio>
        <span className="mt-3 text-[10px] text-zinc-400 dark:text-zinc-500 font-semibold uppercase tracking-widest">
          {t.playground.localAudioGeneration}
        </span>
      </div>
    );
  }

  if (isImage(format, rawUrl)) {
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
