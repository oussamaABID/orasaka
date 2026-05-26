"use client";

import * as React from "react";
import {
  ShieldCheck,
  Save,
  Loader2,
  CheckCircle2,
  AlertCircle,
} from "lucide-react";

interface ValidationTierConfig {
  id: string;
  stepType: string;
  enabled: boolean;
  executionOrder: number;
  configurationPayload: Record<string, unknown>;
}

/** Tier display metadata. */
const TIER_META: Record<
  string,
  { label: string; badge: string; color: string; description: string }
> = {
  STRUCTURAL_A: {
    label: "Tier A — Deterministic JSON Schema",
    badge: "A",
    color: "emerald",
    description: "Zero-token structural JSON validation via Jackson ObjectMapper.",
  },
  SANDBOX_B: {
    label: "Tier B — MCP Sandbox Crash-Test",
    badge: "B",
    color: "sky",
    description:
      "Code block extraction and isolated MCP sandbox compilation.",
  },
  SEMANTIC_C: {
    label: "Tier C — Semantic Consensus Debate",
    badge: "C",
    color: "violet",
    description:
      "Critic vs Advocate debate at temperature 0.0 for semantic alignment.",
  },
  TDR_D: {
    label: "Tier D — Test-Driven Response (TDR)",
    badge: "D",
    color: "amber",
    description:
      "Pre-generates assertion schemas via fast reasoning model and validates responses.",
  },
};

const COLOR_CLASSES: Record<
  string,
  { badge: string; ring: string; glow: string }
> = {
  emerald: {
    badge:
      "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20",
    ring: "ring-emerald-500/30",
    glow: "shadow-emerald-500/10",
  },
  sky: {
    badge:
      "bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20",
    ring: "ring-sky-500/30",
    glow: "shadow-sky-500/10",
  },
  violet: {
    badge:
      "bg-violet-500/10 text-violet-600 dark:text-violet-400 border-violet-500/20",
    ring: "ring-violet-500/30",
    glow: "shadow-violet-500/10",
  },
  amber: {
    badge:
      "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20",
    ring: "ring-amber-500/30",
    glow: "shadow-amber-500/10",
  },
};

interface ValidationPipelineCardProps {
  fetchWithAuth: (url: string, options?: RequestInit) => Promise<Response>;
}

export function ValidationPipelineCard({
  fetchWithAuth,
}: Readonly<ValidationPipelineCardProps>) {
  const [tiers, setTiers] = React.useState<ValidationTierConfig[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [dirty, setDirty] = React.useState(false);
  const [toast, setToast] = React.useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

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
      const res = await fetchWithAuth("/api/v1/admin/validation-pipeline");
      if (!res.ok) throw new Error("Failed to load validation pipeline");
      const data: ValidationTierConfig[] = await res.json();
      setTiers(data);
      setDirty(false);
    } catch {
      showToast("error", "Failed to load validation pipeline configuration.");
    } finally {
      setLoading(false);
    }
  }, [fetchWithAuth, showToast]);

  React.useEffect(() => {
    const init = async () => {
      await loadConfig();
    };
    init();
  }, [loadConfig]);

  const handleToggle = (idx: number) => {
    setTiers((prev) =>
      prev.map((tier, i) =>
        i === idx ? { ...tier, enabled: !tier.enabled } : tier,
      ),
    );
    setDirty(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await fetchWithAuth("/api/v1/admin/validation-pipeline", {
        method: "PUT",
        body: JSON.stringify(tiers),
      });
      if (!res.ok) throw new Error("Failed to save");
      const saved: ValidationTierConfig[] = await res.json();
      setTiers(saved);
      setDirty(false);
      showToast("success", "Validation pipeline saved successfully.");
    } catch {
      showToast("error", "Failed to save validation pipeline configuration.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <section
        id="validation-pipeline-card"
        className="bg-card-bg/70 border border-card-border rounded-2xl p-6 shadow-sm backdrop-blur-lg"
      >
        <article className="animate-pulse space-y-3">
          <span className="h-5 w-64 bg-[var(--surface-2)] rounded block" />
          <span className="h-3 w-96 bg-[var(--surface-2)] rounded block" />
          {[1, 2, 3, 4].map((i) => (
            <span
              key={i}
              className="h-20 bg-[var(--surface-1)] rounded-xl block"
            />
          ))}
        </article>
      </section>
    );
  }

  return (
    <section
      id="validation-pipeline-card"
      className="bg-card-bg/70 border border-card-border rounded-2xl p-6 shadow-sm backdrop-blur-lg relative"
    >
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
          <h3 className="text-lg font-bold text-[var(--text-primary)] flex items-center gap-2">
            <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-violet-500/10 text-violet-500">
              <ShieldCheck className="w-4 h-4" />
            </span>
            Validation Matrix
          </h3>
          <p className="text-xs text-[var(--text-muted)] mt-0.5">
            Configure the 4-tier autonomous self-correction chain.
          </p>
        </article>
        <button
          id="save-validation-pipeline"
          onClick={handleSave}
          disabled={saving || !dirty}
          className="flex items-center gap-1.5 px-4 py-1.5 text-xs font-semibold rounded-lg bg-violet-500 hover:bg-violet-600 text-white shadow-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {saving ? (
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
          ) : (
            <Save className="w-3.5 h-3.5" />
          )}
          {saving ? "Saving…" : "Save Changes"}
        </button>
      </header>

      {/* Tier cards */}
      <ul className="space-y-2">
        {tiers.map((tier, idx) => {
          const meta = TIER_META[tier.stepType] ?? {
            label: tier.stepType,
            badge: "?",
            color: "zinc",
            description: "",
          };
          const colors = COLOR_CLASSES[meta.color] ?? COLOR_CLASSES.amber;

          return (
            <li
              key={tier.id}
              id={`validation-tier-${tier.stepType}`}
              className={`group flex items-center gap-4 p-4 rounded-xl border transition-all duration-200 ${
                tier.enabled
                  ? `border-card-border bg-[var(--surface-0)] hover:ring-1 ${colors.ring} hover:${colors.glow}`
                  : "border-card-border bg-[var(--surface-1)]/50 opacity-60"
              }`}
            >
              {/* Tier badge */}
              <span
                className={`flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-lg text-sm font-bold border ${colors.badge}`}
              >
                {meta.badge}
              </span>

              {/* Info */}
              <article className="flex-1 min-w-0">
                <header className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-[var(--text-primary)] truncate">
                    {meta.label}
                  </span>
                  <span className="text-[10px] font-mono text-[var(--text-muted)] hidden sm:inline">
                    order: {tier.executionOrder}
                  </span>
                </header>
                <p className="text-[11px] text-[var(--text-muted)] truncate mt-0.5">
                  {meta.description}
                </p>
              </article>

              {/* Disabled badge */}
              {!tier.enabled && (
                <span className="flex-shrink-0 text-[10px] font-semibold uppercase tracking-wider text-[var(--text-muted)] bg-[var(--surface-2)] px-2 py-0.5 rounded-full">
                  Disabled
                </span>
              )}

              {/* Toggle */}
              <button
                id={`toggle-tier-${tier.stepType}`}
                onClick={() => handleToggle(idx)}
                className={`relative flex-shrink-0 w-10 h-5 rounded-full transition-colors duration-200 ${
                  tier.enabled
                    ? "bg-violet-500"
                    : "bg-[var(--surface-3)]"
                }`}
                aria-label={`Toggle ${meta.label}`}
              >
                <span
                  className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow-sm transition-transform duration-200 ${
                    tier.enabled ? "translate-x-5" : "translate-x-0"
                  }`}
                />
              </button>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
