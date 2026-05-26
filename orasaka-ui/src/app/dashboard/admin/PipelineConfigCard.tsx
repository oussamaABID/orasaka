"use client";

import * as React from "react";
import {
  GripVertical,
  RotateCcw,
  Save,
  Loader2,
  CheckCircle2,
  AlertCircle,
  Info,
} from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";

interface InterceptorConfig {
  interceptorKey: string;
  displayLabel: string;
  executionOrder: number;
  enabled: boolean;
  description: string;
}

interface PipelineConfigCardProps {
  fetchWithAuth: (url: string, options?: RequestInit) => Promise<Response>;
}

export function PipelineConfigCard({ fetchWithAuth }: PipelineConfigCardProps) {
  const { t } = useTranslation();
  const pl = t.admin.pipeline;

  const [interceptors, setInterceptors] = React.useState<InterceptorConfig[]>(
    [],
  );
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [toast, setToast] = React.useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);
  const [dragIdx, setDragIdx] = React.useState<number | null>(null);
  const [dirty, setDirty] = React.useState(false);

  const showToast = React.useCallback(
    (type: "success" | "error", message: string) => {
      setToast({ type, message });
      setTimeout(() => setToast(null), 3500);
    },
    [],
  );

  const loadConfig = React.useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetchWithAuth("/api/v1/admin/pipeline/interceptors");
      if (!res.ok) throw new Error(pl.errorLoading);
      const data: InterceptorConfig[] = await res.json();
      setInterceptors(data);
      setDirty(false);
    } catch {
      showToast("error", pl.errorLoading);
    } finally {
      setLoading(false);
    }
  }, [fetchWithAuth, pl, showToast]);

  React.useEffect(() => {
    const initConfig = async () => {
      await loadConfig();
    };
    initConfig();
  }, [loadConfig]);

  const handleDragStart = (idx: number) => setDragIdx(idx);

  const handleDragOver = (e: React.DragEvent, overIdx: number) => {
    e.preventDefault();
    if (dragIdx === null || dragIdx === overIdx) return;
    const updated = [...interceptors];
    const [dragged] = updated.splice(dragIdx, 1);
    updated.splice(overIdx, 0, dragged);
    const reordered = updated.map((item, i) => ({
      ...item,
      executionOrder: i + 1,
    }));
    setInterceptors(reordered);
    setDragIdx(overIdx);
    setDirty(true);
  };

  const handleDragEnd = () => setDragIdx(null);

  const handleToggle = (idx: number) => {
    setInterceptors((prev) =>
      prev.map((item, i) =>
        i === idx ? { ...item, enabled: !item.enabled } : item,
      ),
    );
    setDirty(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await fetchWithAuth("/api/v1/admin/pipeline/interceptors", {
        method: "PUT",
        body: JSON.stringify(interceptors),
      });
      if (!res.ok) throw new Error(pl.errorSaving);
      const saved: InterceptorConfig[] = await res.json();
      setInterceptors(saved);
      setDirty(false);
      showToast("success", pl.savedSuccess);
    } catch {
      showToast("error", pl.errorSaving);
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    if (!confirm(pl.resetConfirm)) return;
    setSaving(true);
    try {
      const res = await fetchWithAuth(
        "/api/v1/admin/pipeline/interceptors/reset",
        { method: "POST" },
      );
      if (!res.ok) throw new Error(pl.errorSaving);
      const defaults: InterceptorConfig[] = await res.json();
      setInterceptors(defaults);
      setDirty(false);
      showToast("success", pl.savedSuccess);
    } catch {
      showToast("error", pl.errorSaving);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <section className="bg-card-bg/70 border border-card-border rounded-2xl p-6 shadow-sm backdrop-blur-lg">
        <article className="animate-pulse space-y-3">
          <span className="h-5 w-56 bg-zinc-200 dark:bg-zinc-800 rounded block" />
          <span className="h-3 w-80 bg-zinc-200 dark:bg-zinc-800 rounded block" />
          {[1, 2, 3, 4].map((i) => (
            <span
              key={i}
              className="h-14 bg-zinc-100 dark:bg-zinc-900 rounded-xl block"
            />
          ))}
        </article>
      </section>
    );
  }

  return (
    <section className="bg-card-bg/70 border border-card-border rounded-2xl p-6 shadow-sm backdrop-blur-lg relative">
      {/* Toast */}
      {toast && (
        <aside
          className={`absolute top-4 right-4 z-10 flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium shadow-lg animate-in fade-in slide-in-from-top-2 duration-300 ${
            toast.type === "success"
              ? "bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800"
              : "bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-800"
          }`}
        >
          {toast.type === "success" ? (
            <CheckCircle2 className="w-4 h-4" />
          ) : (
            <AlertCircle className="w-4 h-4" />
          )}
          {toast.message}
        </aside>
      )}

      {/* Header */}
      <header className="flex items-center justify-between mb-4">
        <article>
          <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
            <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-amber-500/10 text-amber-500">
              ⚡
            </span>
            {pl.title}
          </h3>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
            {pl.subtitle}
          </p>
        </article>
        <nav className="flex items-center gap-2">
          <button
            onClick={handleReset}
            disabled={saving}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg border border-zinc-200 dark:border-zinc-700 text-zinc-600 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors disabled:opacity-50"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            {pl.resetToDefault}
          </button>
          <button
            onClick={handleSave}
            disabled={saving || !dirty}
            className="flex items-center gap-1.5 px-4 py-1.5 text-xs font-semibold rounded-lg bg-amber-500 hover:bg-amber-600 text-white shadow-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {saving ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Save className="w-3.5 h-3.5" />
            )}
            {saving ? pl.saving : pl.saveOrder}
          </button>
        </nav>
      </header>

      {/* Drag hint */}
      <div className="flex items-center gap-1.5 mb-3 text-[11px] text-zinc-400 dark:text-zinc-500">
        <Info className="w-3 h-3" />
        {pl.dragHint}
      </div>

      {/* Interceptor list */}
      <section className="space-y-1.5">
        {interceptors.map((item, idx) => (
          <div
            key={item.interceptorKey}
            draggable
            onDragStart={() => handleDragStart(idx)}
            onDragOver={(e) => handleDragOver(e, idx)}
            onDragEnd={handleDragEnd}
            className={`group flex items-center gap-3 p-3 rounded-xl border transition-all duration-200 cursor-grab active:cursor-grabbing ${
              dragIdx === idx
                ? "border-amber-400 dark:border-amber-600 bg-amber-50/50 dark:bg-amber-950/20 shadow-lg scale-[1.01]"
                : item.enabled
                  ? "border-card-border bg-background hover:border-zinc-300 dark:hover:border-zinc-600"
                  : "border-card-border bg-zinc-50/50 dark:bg-zinc-900/50 opacity-60"
            }`}
          >
            {/* Drag handle */}
            <div className="flex-shrink-0 text-zinc-300 dark:text-zinc-600 group-hover:text-zinc-500 dark:group-hover:text-zinc-400 transition-colors">
              <GripVertical className="w-4 h-4" />
            </div>

            {/* Order badge */}
            <span className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-md bg-amber-500/10 text-amber-600 dark:text-amber-400 text-[11px] font-bold tabular-nums">
              {item.executionOrder}
            </span>

            {/* Label & description */}
            <article className="flex-1 min-w-0">
              <header className="flex items-center gap-2">
                <span className="text-sm font-semibold text-zinc-800 dark:text-zinc-200 truncate">
                  {item.displayLabel}
                </span>
                <span className="text-[10px] font-mono text-zinc-400 dark:text-zinc-500 hidden sm:inline">
                  {item.interceptorKey}
                </span>
              </header>
              {item.description && (
                <p className="text-[11px] text-zinc-400 dark:text-zinc-500 truncate mt-0.5">
                  {item.description}
                </p>
              )}
            </article>

            {/* Disabled badge */}
            {!item.enabled && (
              <span className="flex-shrink-0 text-[10px] font-semibold uppercase tracking-wider text-zinc-400 dark:text-zinc-500 bg-zinc-100 dark:bg-zinc-800 px-2 py-0.5 rounded-full">
                {pl.disabled}
              </span>
            )}

            {/* Toggle */}
            <button
              onClick={() => handleToggle(idx)}
              className={`relative flex-shrink-0 w-9 h-5 rounded-full transition-colors duration-200 ${
                item.enabled ? "bg-amber-500" : "bg-zinc-300 dark:bg-zinc-700"
              }`}
              aria-label={`${pl.enabledLabel} ${item.displayLabel}`}
            >
              <span
                className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow-sm transition-transform duration-200 ${
                  item.enabled ? "translate-x-4" : "translate-x-0"
                }`}
              />
            </button>
          </div>
        ))}
      </section>
    </section>
  );
}
