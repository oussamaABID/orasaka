"use client";

import * as React from "react";
import { Job } from "@/features/jobs/types/jobs.types";
import { Button } from "@/components/ui/Button";
import { X, Copy, Check } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import { ResultDisplay } from "@/features/playground/components/ResultDisplay";
import {
  ProgressTimeline,
  StageLabels,
  StatusPill,
  LiveElapsed,
  PillTab,
  FileJson,
  FileOutput,
  AlertTriangle,
} from "./JobModalParts";
import type { TabKey } from "./JobModalParts";
import { JOB_STATUS } from "@/core/constants/http.constants";

export interface JobModalProps {
  job: Job | null;
  modalType: "payload" | "result" | null;
  onClose: () => void;
  copiedId: string | null;
  onCopy: (text: string, id: string) => void;
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
    if (job.status === JOB_STATUS.COMPLETED) tabs.push("result");
    if (job.status === JOB_STATUS.FAILED && job.errorMessage) tabs.push("error");

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

    globalThis.addEventListener("keydown", handler);
    return () => globalThis.removeEventListener("keydown", handler);
  }, [job, activeTab, onClose]);

  if (!job || !modalType) return null;

  const getDisplayData = () => {
    if (activeTab === "payload") return JSON.stringify(job.payload, null, 2);
    if (activeTab === "result") return JSON.stringify(job.result, null, 2);
    return job.errorMessage || "No error details available";
  };

  const dataToDisplay = getDisplayData();
  const isLive = job.status === JOB_STATUS.PROCESSING || job.status === JOB_STATUS.PENDING;

  return (
    <dialog
      open
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200 w-full h-full m-0 max-w-none max-h-none border-none bg-transparent"
      aria-label={`Job ${job.id} details`}
    >
      {/* Backdrop dismiss button — accessible and native */}
      <button
        type="button"
        className="fixed inset-0 w-full h-full bg-transparent border-none cursor-default"
        aria-label="Close dialog"
        onClick={onClose}
      />
      <article
        className="glass-card rounded-2xl max-w-2xl w-full max-h-[85vh] flex flex-col overflow-hidden animate-in zoom-in-95 slide-in-from-bottom-3 duration-300 text-[var(--text-primary)] relative z-10"
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
          {job.status === JOB_STATUS.COMPLETED && (
            <PillTab
              label={t.jobs?.resultModalTitle || "Output"}
              icon={FileOutput}
              isActive={activeTab === "result"}
              onClick={() => setActiveTab("result")}
            />
          )}
          {job.status === JOB_STATUS.FAILED && job.errorMessage && (
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
            </kbd>{" "}
            <kbd className="px-1 py-0.5 rounded bg-[var(--surface-2)] border border-[var(--border-subtle)] text-[8px]">
              →
            </kbd>{" "}
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
