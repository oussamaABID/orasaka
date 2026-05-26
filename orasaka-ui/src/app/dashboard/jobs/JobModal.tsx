"use client";

import * as React from "react";
import { parseISO } from "date-fns";
import { Job } from "@/features/jobs/types/jobs.types";
import { Button } from "@/components/ui/Button";
import {
  X,
  Copy,
  Check,
  Clock,
  CheckCircle2,
  XCircle,
  Loader2,
  ArrowUpFromLine,
  FileJson,
  FileOutput,
  AlertTriangle,
} from "lucide-react";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { ResultDisplay } from "@/features/playground/components/ResultDisplay";

export interface JobModalProps {
  job: Job | null;
  modalType: "payload" | "result" | null;
  onClose: () => void;
  copiedId: string | null;
  onCopy: (text: string, id: string) => void;
}

/* ─── Fluid Progress Bar ─── */
function ProgressTimeline({ status }: Readonly<{ status: string }>) {
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
        status === "FAILED"
          ? t.jobs?.statusFailed || "Failed"
          : t.jobs?.statusCompleted || "Completed",
      icon: status === "FAILED" ? XCircle : CheckCircle2,
    },
  ];

  const resolveCurrentIdx = (s: string): number => {
    if (s === "COMPLETED" || s === "FAILED") return 2;
    if (s === "PROCESSING" || s === "PENDING") return 1;
    return 0;
  };

  const currentIdx = resolveCurrentIdx(status);

  return (
    <section className="flex items-center gap-1 px-1">
      {stages.map((stage, i) => {
        const isDone = i < currentIdx;
        const Icon = isDone ? Check : stage.icon;
        const isActive = i === currentIdx;
        const isFailed = stage.key === "done" && status === "FAILED";

        return (
          <React.Fragment key={stage.key}>
            {/* Node */}
            <figure
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
                status !== "FAILED" &&
                status !== "COMPLETED" && (
                  <span className="absolute inset-0 rounded-xl bg-[var(--accent)] opacity-[0.06] animate-pulse" />
                )}
              <Icon
                className={`w-4 h-4 relative z-10 ${isActive && (status === "PROCESSING" || status === "PENDING") ? "animate-spin" : ""}`}
              />
            </figure>

            {/* Connector */}
            {i < stages.length - 1 && (
              <figure className="flex-1 h-[2px] rounded-full bg-[var(--surface-3)] overflow-hidden">
                <figure
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
              </figure>
            )}
          </React.Fragment>
        );
      })}
    </section>
  );
}

/* ─── Stage Labels Row ─── */
function StageLabels({ status }: Readonly<{ status: string }>) {
  const { t } = useTranslation();

  const labels = [
    t.jobs?.colCreated || "Submitted",
    t.jobs?.running || "Processing",
    status === "FAILED"
      ? t.jobs?.statusFailed || "Failed"
      : t.jobs?.statusCompleted || "Completed",
  ];

  const resolveIdx = (s: string): number => {
    if (s === "COMPLETED" || s === "FAILED") return 2;
    if (s === "PROCESSING" || s === "PENDING") return 1;
    return 0;
  };

  const currentIdx = resolveIdx(status);

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
function StatusPill({ status }: Readonly<{ status: string }>) {
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
          status === "PROCESSING" || status === "PENDING" ? "animate-pulse" : ""
        } ${dotColor}`}
      />
      {style.label}
    </span>
  );
}

/* ─── Live Elapsed Counter ─── */
function LiveElapsed({
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
  const endTime = isLive
    ? now
    : updatedAt
      ? parseISO(updatedAt).getTime()
      : startTime;
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
type TabKey = "payload" | "result" | "error";

function PillTab({
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
      className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-[11px] font-semibold transition-all duration-200 ${
        isActive
          ? variant === "error"
            ? "bg-red-500/10 text-red-600 dark:text-red-400 shadow-sm"
            : `bg-[var(--accent-soft)] ${accentClasses.text} shadow-sm`
          : "text-[var(--text-muted)] hover:text-[var(--text-secondary)] hover:bg-[var(--surface-2)]"
      }`}
    >
      <Icon className="w-3.5 h-3.5" />
      {label}
    </button>
  );
}

/* ═══════════════════════════════════════════════
   MAIN MODAL COMPONENT
   ═══════════════════════════════════════════════ */
export const JobModal: React.FC<JobModalProps> = ({
  job,
  modalType,
  onClose,
  copiedId,
  onCopy,
}) => {
  const { t } = useTranslation();

  const [activeTab, setActiveTab] = React.useState<TabKey>(
    modalType === "result" ? "result" : "payload",
  );

  /* Keyboard shortcuts: Esc to close, ←/→ to switch tabs */
  React.useEffect(() => {
    if (!job) return;

    const tabs: TabKey[] = ["payload"];
    if (job.status === "COMPLETED") tabs.push("result");
    if (job.status === "FAILED" && job.errorMessage) tabs.push("error");

    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
        return;
      }
      if (e.key === "ArrowRight" || e.key === "ArrowLeft") {
        const dir = e.key === "ArrowRight" ? 1 : -1;
        const curIdx = tabs.indexOf(activeTab);
        const nextIdx = Math.max(0, Math.min(tabs.length - 1, curIdx + dir));
        setActiveTab(tabs[nextIdx]);
      }
    };

    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [job, activeTab, onClose]);

  if (!job || !modalType) return null;

  const getDisplayData = () => {
    if (activeTab === "payload") return JSON.stringify(job.payload, null, 2);
    if (activeTab === "result") return JSON.stringify(job.result, null, 2);
    return job.errorMessage || "No error details available";
  };

  const dataToDisplay = getDisplayData();
  const isLive = job.status === "PROCESSING" || job.status === "PENDING";

  return (
    <dialog
      open
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200 w-full h-full m-0 max-w-none max-h-none border-none bg-transparent"
      onClick={onClose}
      onKeyDown={(e) => {
        if (e.key === "Escape") onClose();
      }}
      aria-label={`Job ${job.id} details`}
    >
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
      <article
        className="glass-card rounded-2xl max-w-2xl w-full max-h-[85vh] flex flex-col overflow-hidden animate-in zoom-in-95 slide-in-from-bottom-3 duration-300 text-[var(--text-primary)]"
        onClick={(e) => e.stopPropagation()}
        onKeyDown={(e) => e.stopPropagation()}
      >
        {/* ── Header ── */}
        <header className="flex items-center justify-between p-5 border-b border-[var(--border-subtle)]">
          <section className="flex items-center gap-3">
            <StatusPill status={job.status} />
            <section className="flex flex-col">
              <h3 className="font-semibold text-[13px] text-[var(--text-primary)]">
                {t.jobs?.title || "Task Details"}
              </h3>
              <span className="text-[10px] text-[var(--text-muted)] font-mono">
                {job.featureKey}
              </span>
            </section>
          </section>
          <section className="flex items-center gap-3">
            <LiveElapsed
              createdAt={job.createdAt}
              updatedAt={job.updatedAt}
              isLive={isLive}
            />
            <button
              onClick={onClose}
              className="p-2 rounded-xl hover:bg-[var(--surface-2)] text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-all duration-150"
              aria-label="Close details"
            >
              <X className="w-4 h-4" />
            </button>
          </section>
        </header>

        {/* ── Progress Timeline ── */}
        <section className="px-5 py-3 border-b border-[var(--border-subtle)]">
          <ProgressTimeline status={job.status} />
          <StageLabels status={job.status} />
        </section>

        {/* ── Pill Tabs ── */}
        <section className="flex items-center gap-1.5 px-5 py-2.5 border-b border-[var(--border-subtle)] bg-[var(--surface-0)]/50">
          <PillTab
            label={t.jobs?.payloadModalTitle || "Input"}
            icon={FileJson}
            isActive={activeTab === "payload"}
            onClick={() => setActiveTab("payload")}
          />
          {job.status === "COMPLETED" && (
            <PillTab
              label={t.jobs?.resultModalTitle || "Output"}
              icon={FileOutput}
              isActive={activeTab === "result"}
              onClick={() => setActiveTab("result")}
            />
          )}
          {job.status === "FAILED" && job.errorMessage && (
            <PillTab
              label={t.jobs?.errorDetails || "Error"}
              icon={AlertTriangle}
              isActive={activeTab === "error"}
              variant="error"
              onClick={() => setActiveTab("error")}
            />
          )}
          {/* Keyboard hint */}
          <span className="ml-auto text-[9px] text-[var(--text-muted)] font-mono hidden sm:inline-flex items-center gap-1">
            <kbd className="px-1 py-0.5 rounded bg-[var(--surface-2)] border border-[var(--border-subtle)] text-[8px]">
              ←
            </kbd>
            <kbd className="px-1 py-0.5 rounded bg-[var(--surface-2)] border border-[var(--border-subtle)] text-[8px]">
              →
            </kbd>
            switch
          </span>
        </section>

        {/* ── Content ── */}
        <section className="p-5 overflow-y-auto flex-1 space-y-4 scrollbar-thin">
          {/* Meta bar */}
          <section className="flex flex-wrap items-center justify-between gap-3 text-[11px] glass-card p-3 rounded-xl">
            <section className="flex items-center gap-2">
              <span className="font-semibold text-[var(--text-muted)]">ID</span>
              <span className="font-mono text-[var(--text-secondary)] bg-[var(--surface-2)] px-2 py-0.5 rounded-md">
                {job.id}
              </span>
            </section>
            <button
              onClick={() => onCopy(String(job.id), "modal-id")}
              className="inline-flex items-center gap-1 px-2 py-1 rounded-lg text-[10px] font-medium text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-3)] transition-colors"
            >
              {copiedId === "modal-id" ? (
                <Check className="w-3 h-3 text-emerald-500" />
              ) : (
                <Copy className="w-3 h-3" />
              )}
              {copiedId === "modal-id" ? t.jobs?.copied || "Copied" : "Copy ID"}
            </button>
          </section>

          {activeTab === "result" ? (
            <section className="flex flex-col gap-4">
              <section className="p-4 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-2)]">
                <ResultDisplay payload={job.result} />
              </section>
              <details className="text-[11px] text-[var(--text-muted)]">
                <summary className="cursor-pointer select-none font-semibold hover:text-[var(--text-secondary)] transition-colors">
                  Show Raw JSON Output
                </summary>
                <section className="relative mt-2">
                  <pre className="bg-[var(--surface-2)] text-[var(--text-primary)] p-4 rounded-xl overflow-auto text-[11px] font-mono max-h-64 leading-relaxed border border-[var(--border-subtle)] scrollbar-thin">
                    {dataToDisplay}
                  </pre>
                  <button
                    onClick={() => onCopy(dataToDisplay, "modal-json")}
                    className="absolute top-3 right-3 p-1.5 bg-[var(--surface-3)] hover:bg-[var(--surface-3)] border border-[var(--border-subtle)] text-[var(--text-muted)] hover:text-[var(--text-primary)] rounded-lg transition-colors"
                  >
                    {copiedId === "modal-json" ? (
                      <Check className="w-3.5 h-3.5 text-emerald-500" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </section>
              </details>
            </section>
          ) : (
            <section className="relative">
              <pre className="bg-[var(--surface-2)] text-[var(--text-primary)] p-4 rounded-xl overflow-auto text-[11px] font-mono max-h-96 leading-relaxed border border-[var(--border-subtle)] scrollbar-thin">
                {dataToDisplay}
              </pre>
              <button
                onClick={() => onCopy(dataToDisplay, "modal-json")}
                className="absolute top-3 right-3 p-1.5 bg-[var(--surface-3)] hover:bg-[var(--surface-3)] border border-[var(--border-subtle)] text-[var(--text-muted)] hover:text-[var(--text-primary)] rounded-lg transition-colors"
              >
                {copiedId === "modal-json" ? (
                  <Check className="w-3.5 h-3.5 text-emerald-500" />
                ) : (
                  <Copy className="w-3.5 h-3.5" />
                )}
              </button>
            </section>
          )}
        </section>

        {/* ── Footer ── */}
        <footer className="flex justify-between items-center gap-3 p-4 border-t border-[var(--border-subtle)]">
          <span className="text-[9px] text-[var(--text-muted)] font-mono hidden sm:block">
            <kbd className="px-1 py-0.5 rounded bg-[var(--surface-2)] border border-[var(--border-subtle)] text-[8px]">
              Esc
            </kbd>{" "}
            close
          </span>
          <Button
            variant="outline"
            onClick={onClose}
            className="rounded-xl text-[11px] font-semibold border-[var(--border-default)]"
          >
            {t.admin?.cancel || "Close"}
          </Button>
        </footer>
      </article>
    </dialog>
  );
};
