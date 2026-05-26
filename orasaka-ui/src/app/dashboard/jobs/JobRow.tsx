"use client";

import * as React from "react";
import {
  parseISO,
  differenceInMilliseconds,
  formatDistanceToNow,
} from "date-fns";
import { fr, enUS } from "date-fns/locale";
import { useTranslation } from "@/core/context/LocaleContext";
import { Job, JobStatus } from "@/features/jobs/types/jobs.types";
import { Button } from "@/components/ui/Button";
import {
  Copy,
  Check,
  Code,
  Eye,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Loader2,
  Terminal,
} from "lucide-react";
import { JOB_STATUS } from "@/core/constants/http.constants";

export interface JobRowProps {
  job: Job;
  copiedId: string | null;
  onCopy: (text: string, id: string) => void;
  isExpandedError: boolean;
  onToggleExpandError: () => void;
  onOpenModal: (job: Job, type: "payload" | "result") => void;
}

export const JobRow: React.FC<JobRowProps> = ({
  job,
  copiedId,
  onCopy,
  isExpandedError,
  onToggleExpandError,
  onOpenModal,
}) => {
  const { locale, t } = useTranslation();

  const getDuration = (j: Job) => {
    const start = parseISO(j.createdAt);
    const end = parseISO(j.updatedAt);
    const diff = differenceInMilliseconds(end, start);
    if (diff < 0) return "0s";
    if (diff < 1000) return `${diff}ms`;
    const secs = Math.floor(diff / 1000);
    if (secs < 60) return `${secs}s`;
    const mins = Math.floor(secs / 60);
    const remainingSecs = secs % 60;
    return `${mins}m ${remainingSecs}s`;
  };

  const getFeatureDisplayName = (featureKey: string) => {
    if (featureKey.includes("video")) return t.notifications.videoGen;
    if (featureKey.includes("image")) return t.notifications.imageGen;
    if (featureKey.includes("speech")) return t.notifications.speechGen;
    return t.notifications.textGen;
  };

  const getStatusBadge = (status: JobStatus) => {
    switch (status) {
      case JOB_STATUS.PENDING:
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-semibold bg-[var(--surface-2)] text-[var(--text-secondary)] border border-[var(--border-subtle)]">
            <Clock className="w-3.5 h-3.5 mr-1 text-[var(--text-muted)]" />
            PENDING
          </span>
        );
      case JOB_STATUS.PROCESSING:
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/15 animate-pulse">
            <Loader2 className="w-3.5 h-3.5 mr-1 text-blue-500 animate-spin" />
            PROCESSING
          </span>
        );
      case JOB_STATUS.COMPLETED:
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/15">
            <CheckCircle2 className="w-3.5 h-3.5 mr-1 text-emerald-500" />
            COMPLETED
          </span>
        );
      case JOB_STATUS.FAILED:
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-semibold bg-red-500/10 text-red-600 dark:text-red-400 border border-red-500/15">
            <AlertTriangle className="w-3.5 h-3.5 mr-1 text-red-500" />
            FAILED
          </span>
        );
    }
  };

  const formatDate = (isoString: string) => {
    try {
      const date = parseISO(isoString);
      return formatDistanceToNow(date, {
        addSuffix: true,
        locale: locale === "fr" ? fr : enUS,
      });
    } catch {
      return "—";
    }
  };

  return (
    <React.Fragment>
      <tr className="hover:bg-[var(--surface-2)]/50 transition-colors">
        {/* Job ID column */}
        <td className="p-4 font-mono text-[11px]">
          <div className="flex items-center space-x-1.5 text-[var(--text-secondary)]">
            <span title={job.id}>{job.id.substring(0, 8)}...</span>
            <button
              onClick={() => onCopy(job.id, job.id)}
              className="p-1 text-[var(--text-muted)] hover:text-[var(--text-primary)] rounded-md transition-colors"
              aria-label="Copy full job ID"
            >
              {copiedId === job.id ? (
                <Check className="w-3.5 h-3.5 text-emerald-500" />
              ) : (
                <Copy className="w-3.5 h-3.5" />
              )}
            </button>
          </div>
        </td>

        {/* Task Type / Feature column */}
        <td className="p-4">
          <div className="font-semibold text-[13px] text-[var(--text-primary)]">
            {getFeatureDisplayName(job.featureKey)}
          </div>
          <div
            className="text-[10px] text-[var(--text-muted)] font-mono truncate max-w-xs mt-0.5"
            title={job.featureKey}
          >
            {job.featureKey}
          </div>
        </td>

        {/* Model column */}
        <td className="p-4">
          {job.payload?.model ? (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-[var(--accent-soft)] border border-[var(--accent)]/15 text-[11px] font-bold text-[var(--accent)] font-mono tracking-tight">
              {typeof job.payload.model === "object" ? JSON.stringify(job.payload.model) : String(job.payload.model)}
            </span>
          ) : (
            <span className="text-[11px] text-[var(--text-muted)] italic">
              —
            </span>
          )}
        </td>

        {/* Status Badge column */}
        <td className="p-4">{getStatusBadge(job.status)}</td>

        {/* Created At column — relative */}
        <td
          className="p-4 text-[var(--text-muted)] text-[11px]"
          title={job.createdAt}
        >
          {formatDate(job.createdAt)}
        </td>

        {/* Duration column */}
        <td className="p-4 font-medium text-[var(--text-secondary)] text-[11px]">
          {job.status === JOB_STATUS.PENDING || job.status === JOB_STATUS.PROCESSING ? (
            <div className="space-y-1">
              <span className="text-blue-500 animate-pulse font-semibold text-[11px]">
                {t.jobs.running}
              </span>
              {/* Mini progress bar */}
              <div className="h-1 w-16 rounded-full bg-[var(--surface-3)] overflow-hidden">
                <div className="h-full w-1/2 rounded-full bg-blue-500 animate-pulse" />
              </div>
            </div>
          ) : (
            getDuration(job)
          )}
        </td>

        {/* Action buttons column */}
        <td className="p-4 text-right space-x-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => onOpenModal(job, "payload")}
            className="text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)] transition-colors"
            title={t.jobs.viewPayload}
          >
            <Code className="w-4 h-4" />
          </Button>

          <Button
            variant="ghost"
            size="icon"
            disabled={job.status !== JOB_STATUS.COMPLETED}
            onClick={() => onOpenModal(job, "result")}
            className="text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)] disabled:opacity-40 disabled:hover:bg-transparent disabled:pointer-events-none transition-colors"
            title={t.jobs.viewResult}
          >
            <Eye className="w-4 h-4" />
          </Button>

          {job.status === JOB_STATUS.FAILED && job.errorMessage && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onToggleExpandError}
              className={`transition-colors ${
                isExpandedError
                  ? "bg-red-50 text-red-600 dark:bg-red-950/20 dark:text-red-400"
                  : "text-red-400 hover:text-red-600 dark:hover:text-red-300 hover:bg-zinc-100 dark:hover:bg-zinc-800/80"
              }`}
              title={t.jobs.viewErrorLogs}
            >
              <Terminal className="w-4 h-4" />
            </Button>
          )}
        </td>
      </tr>

      {/* Collapsible Error Row */}
      {job.status === JOB_STATUS.FAILED && isExpandedError && job.errorMessage && (
        <tr className="bg-red-50/20 dark:bg-red-950/5 animate-in fade-in slide-in-from-top-1 duration-200">
          <td
            colSpan={7}
            className="p-4 border-t border-zinc-100 dark:border-zinc-800/40"
          >
            <div className="rounded-xl border border-red-200/50 dark:border-red-900/30 bg-red-50/30 dark:bg-red-950/10 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-red-800 dark:text-red-400 uppercase tracking-wider flex items-center">
                  <AlertTriangle className="w-3.5 h-3.5 mr-1.5" />
                  {t.jobs?.errorDetails || "Error details:"}
                </span>
                <button
                  onClick={() =>
                    onCopy(job.errorMessage || "", `err-${job.id}`)
                  }
                  className="text-xs font-semibold text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-200 flex items-center space-x-1"
                >
                  {copiedId === `err-${job.id}` ? (
                    <>
                      <Check className="w-3 h-3 text-emerald-500" />
                      <span className="text-emerald-500">{t.jobs.copied}</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3 h-3" />
                      <span>{t.jobs.copyError}</span>
                    </>
                  )}
                </button>
              </div>
              <pre className="text-xs font-mono text-red-700 dark:text-red-300 overflow-x-auto whitespace-pre-wrap leading-relaxed max-w-full">
                {job.errorMessage}
              </pre>
            </div>
          </td>
        </tr>
      )}
    </React.Fragment>
  );
};
