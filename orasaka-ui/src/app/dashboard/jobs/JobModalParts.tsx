"use client";

import * as React from "react";
import { parseISO } from "date-fns";
import {
  Clock,
  Check,
  CheckCircle2,
  XCircle,
  Loader2,
  ArrowUpFromLine,
} from "lucide-react";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { JOB_STATUS } from "@/core/constants/http.constants";

/** Shared status-to-stage-index resolver — used by ProgressTimeline and StageLabels */
const resolveStatusIndex = (s: string): number => {
  if (s === JOB_STATUS.COMPLETED || s === JOB_STATUS.FAILED) return 2;
  if (s === JOB_STATUS.PROCESSING || s === JOB_STATUS.PENDING) return 1;
  return 0;
};

/* ─── Fluid Progress Bar ─── */
export function ProgressTimeline({ status }: Readonly<{ status: string }>) {
  const { t } = useTranslation();

  const stages = [
    {
      key: "submitted",
      label: t.jobs?.colCreated || "Submitted",
      icon: ArrowUpFromLine,
    },
    {
      key: "processing",
      label: t.jobs?.running || "Processing",
      icon: Loader2,
    },
    {
      key: "done",
      label:
        status === JOB_STATUS.FAILED
          ? t.jobs?.statusFailed || "Failed"
          : t.jobs?.statusCompleted || "Completed",
      icon: status === JOB_STATUS.FAILED ? XCircle : CheckCircle2,
    },
  ];

  const currentIdx = resolveStatusIndex(status);

  return (
    <section className="flex items-center gap-1 px-1">
      {stages.map((stage, i) => {
        const isDone = i < currentIdx;
        const Icon = isDone ? Check : stage.icon;
        const isActive = i === currentIdx;
        const isFailed = stage.key === "done" && status === JOB_STATUS.FAILED;

        return (
          <React.Fragment key={stage.key}>
            {/* Node */}
            <div
              className={`relative flex items-center justify-center w-9 h-9 rounded-xl transition-all duration-500 ${(() => {
                if (isDone)
                  return isFailed
                    ? "bg-red-500/10 text-red-500"
                    : "bg-emerald-500/10 text-emerald-500";
                if (isActive)
                  return isFailed
                    ? "bg-red-500/10 text-red-500"
                    : "bg-[var(--accent-soft)] text-[var(--accent)]";
                return "bg-[var(--surface-2)] text-[var(--text-muted)]";
              })()}`}
            >
              {isActive &&
                !isDone &&
                status !== JOB_STATUS.FAILED &&
                status !== JOB_STATUS.COMPLETED && (
                  <span className="absolute inset-0 rounded-xl bg-[var(--accent)] opacity-[0.06] animate-pulse" />
                )}
              <Icon
                className={`w-4 h-4 relative z-10 ${isActive && (status === JOB_STATUS.PROCESSING || status === JOB_STATUS.PENDING) ? "animate-spin" : ""}`}
              />
            </div>

            {/* Connector */}
            {i < stages.length - 1 && (
              <div className="flex-1 h-[2px] rounded-full bg-[var(--surface-3)] overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-700 ease-out ${(() => {
                    if (isDone || (isActive && i < currentIdx))
                      return isFailed
                        ? "bg-red-500 w-full"
                        : "bg-[var(--accent)] w-full";
                    if (isActive)
                      return "bg-[var(--accent)] w-1/2 animate-pulse";
                    return "w-0";
                  })()}`}
                />
              </div>
            )}
          </React.Fragment>
        );
      })}
    </section>
  );
}

/* ─── Stage Labels Row ─── */
export function StageLabels({ status }: Readonly<{ status: string }>) {
  const { t } = useTranslation();

  const labels = [
    t.jobs?.colCreated || "Submitted",
    t.jobs?.running || "Processing",
    status === JOB_STATUS.FAILED
      ? t.jobs?.statusFailed || "Failed"
      : t.jobs?.statusCompleted || "Completed",
  ];

  const currentIdx = resolveStatusIndex(status);

  return (
    <section className="flex justify-between px-3 mt-1.5">
      {labels.map((label, i) => (
        <span
          key={label}
          className={`text-[10px] font-medium transition-colors duration-300 ${
            i <= currentIdx
              ? "text-[var(--text-primary)]"
              : "text-[var(--text-muted)]"
          }`}
        >
          {label}
        </span>
      ))}
    </section>
  );
}

/* ─── Status Badge (compact) ─── */
export function StatusPill({ status }: Readonly<{ status: string }>) {
  const map: Record<string, { cls: string; label: string }> = {
    COMPLETED: {
      cls: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
      label: "Completed",
    },
    FAILED: {
      cls: "bg-red-500/10 text-red-600 dark:text-red-400",
      label: "Failed",
    },
    PROCESSING: {
      cls: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
      label: "Processing",
    },
    PENDING: {
      cls: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
      label: "Pending",
    },
  };
  const style = map[status] || map.PENDING;
  const dotColorMap: Record<string, string> = {
    COMPLETED: "bg-emerald-500",
    FAILED: "bg-red-500",
    PROCESSING: "bg-amber-500",
    PENDING: "bg-blue-500",
  };
  const dotColor = dotColorMap[status] || dotColorMap.PENDING;
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-semibold ${style.cls}`}
    >
      <span
        className={`w-1.5 h-1.5 rounded-full ${
          status === JOB_STATUS.PROCESSING || status === JOB_STATUS.PENDING ? "animate-pulse" : ""
        } ${dotColor}`}
      />
      {style.label}
    </span>
  );
}

/* ─── Live Elapsed Counter ─── */
export function LiveElapsed({
  createdAt,
  updatedAt,
  isLive,
}: Readonly<{
  createdAt?: string;
  updatedAt?: string;
  isLive: boolean;
}>) {
  const [now, setNow] = React.useState(() => Date.now());

  React.useEffect(() => {
    if (!isLive) return;
    const interval = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(interval);
  }, [isLive]);

  const startTime = createdAt ? parseISO(createdAt).getTime() : 0;
  const endTime = (() => {
    if (isLive) return now;
    if (updatedAt) return parseISO(updatedAt).getTime();
    return startTime;
  })();
  const diffMs = endTime - startTime;

  const formatElapsed = (ms: number): string => {
    if (ms < 1000) return `${ms}ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
    return `${Math.floor(ms / 60000)}m ${Math.round((ms % 60000) / 1000)}s`;
  };

  const elapsed = formatElapsed(diffMs);

  return (
    <span className="inline-flex items-center gap-1 text-[10px] text-[var(--text-muted)] font-mono tabular-nums">
      <Clock className="w-3 h-3" />
      {elapsed}
      {isLive && (
        <span className="w-1 h-1 rounded-full bg-amber-500 animate-pulse" />
      )}
    </span>
  );
}

/* ─── Tab Item ─── */
export type TabKey = "payload" | "result" | "error";

export function PillTab({
  label,
  icon: Icon,
  isActive,
  variant = "default",
  onClick,
}: Readonly<{
  label: string;
  icon: React.ElementType;
  isActive: boolean;
  variant?: "default" | "error";
  onClick: () => void;
}>) {
  const { accentClasses } = useTenant();
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-[11px] font-semibold transition-all duration-200 ${(() => {
          if (!isActive) return "text-[var(--text-muted)] hover:text-[var(--text-secondary)] hover:bg-[var(--surface-2)]";
          if (variant === "error") return "bg-red-500/10 text-red-600 dark:text-red-400 shadow-sm";
          return `bg-[var(--accent-soft)] ${accentClasses.text} shadow-sm`;
        })()}`}
    >
      <Icon className="w-3.5 h-3.5" />
      {label}
    </button>
  );
}

/* Re-export icon components for JobModal */
export { FileJson, FileOutput, AlertTriangle } from "lucide-react";
