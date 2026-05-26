"use client";

import * as React from "react";
import { differenceInMilliseconds, parseISO } from "date-fns";
import { Job } from "@/features/jobs/types/jobs.types";
import { resolveProviderFromFeature } from "@/core/constants/capability.constants";
import { Cpu, Timer, Layers, Gauge, FileOutput, User, Zap } from "lucide-react";

interface JobKpiBarProps {
  job: Job;
  /** When true, shows userId column (admin view) */
  showUser?: boolean;
}

/**
 * Inline KPI bar rendering consumption metrics per job.
 * Glassmorphism chips with gradient accents and hover micro-animations.
 *
 * Extracted data sources:
 * - payload.model → AI model consumed
 * - payload.provider → inference provider
 * - payload.voice → TTS voice used
 * - result.durationMs → inference duration
 * - result.format → output format
 * - result.metrics → extra analytics (keyframes, tokens, etc.)
 */
export const JobKpiBar: React.FC<JobKpiBarProps> = ({ job, showUser }) => {
  const model = extractModel(job);
  const provider = extractProvider(job);
  const inferenceDuration = extractInferenceDuration(job);
  const outputFormat = extractOutputFormat(job);
  const voice = extractVoice(job);
  const extraMetrics = extractExtraMetrics(job);

  const chips: ChipDef[] = [];

  if (model) {
    chips.push({
      icon: <Cpu className="w-3 h-3" />,
      label: "MODEL",
      value: model,
      gradient: "from-cyan-500/20 to-cyan-600/5",
      border: "border-cyan-500/15",
      text: "text-cyan-600 dark:text-cyan-400",
      glow: "hover:shadow-cyan-500/10",
    });
  }

  if (provider) {
    chips.push({
      icon: <Layers className="w-3 h-3" />,
      label: "PROVIDER",
      value: provider,
      gradient: "from-violet-500/20 to-violet-600/5",
      border: "border-violet-500/15",
      text: "text-violet-600 dark:text-violet-400",
      glow: "hover:shadow-violet-500/10",
    });
  }

  if (inferenceDuration) {
    chips.push({
      icon: <Timer className="w-3 h-3" />,
      label: "INFERENCE",
      value: inferenceDuration,
      gradient: "from-amber-500/20 to-amber-600/5",
      border: "border-amber-500/15",
      text: "text-amber-600 dark:text-amber-400",
      glow: "hover:shadow-amber-500/10",
    });
  }

  if (outputFormat) {
    chips.push({
      icon: <FileOutput className="w-3 h-3" />,
      label: "OUTPUT",
      value: outputFormat.toUpperCase(),
      gradient: "from-emerald-500/20 to-emerald-600/5",
      border: "border-emerald-500/15",
      text: "text-emerald-600 dark:text-emerald-400",
      glow: "hover:shadow-emerald-500/10",
    });
  }

  if (voice) {
    chips.push({
      icon: <Zap className="w-3 h-3" />,
      label: "VOICE",
      value: voice,
      gradient: "from-pink-500/20 to-pink-600/5",
      border: "border-pink-500/15",
      text: "text-pink-600 dark:text-pink-400",
      glow: "hover:shadow-pink-500/10",
    });
  }

  if (showUser && job.userId) {
    chips.push({
      icon: <User className="w-3 h-3" />,
      label: "USER",
      value:
        job.userId.length > 12 ? `${job.userId.substring(0, 12)}…` : job.userId,
      gradient: "from-zinc-500/15 to-zinc-600/5",
      border: "border-zinc-500/10",
      text: "text-zinc-600 dark:text-zinc-400",
      glow: "hover:shadow-zinc-500/10",
    });
  }

  if (extraMetrics.length > 0) {
    extraMetrics.forEach((m) => {
      chips.push({
        icon: <Gauge className="w-3 h-3" />,
        label: m.key.toUpperCase(),
        value: m.value,
        gradient: "from-orange-500/15 to-orange-600/5",
        border: "border-orange-500/12",
        text: "text-orange-600 dark:text-orange-400",
        glow: "hover:shadow-orange-500/10",
      });
    });
  }

  if (chips.length === 0) return null;

  return (
    <tr className="bg-transparent">
      <td
        colSpan={7}
        className="px-4 py-1.5 border-t border-white/[0.03] dark:border-white/[0.02]"
      >
        <div className="flex flex-wrap items-center gap-1.5">
          {chips.map((chip, i) => (
            <span
              key={`${chip.label}-${i}`}
              className={`
                group/chip relative overflow-hidden
                inline-flex items-center gap-1
                px-2.5 py-1 rounded-lg
                bg-gradient-to-r ${chip.gradient}
                border ${chip.border}
                backdrop-blur-sm
                ${chip.text}
                text-[10px] font-semibold tracking-wide
                transition-all duration-200
                hover:scale-[1.04]
                ${chip.glow} hover:shadow-md
                cursor-default
              `}
              title={`${chip.label}: ${chip.value}`}
            >
              {/* Shimmer effect on hover */}
              <span className="absolute inset-0 -translate-x-full group-hover/chip:translate-x-full transition-transform duration-700 bg-gradient-to-r from-transparent via-white/[0.08] to-transparent pointer-events-none" />

              <span className="relative z-10 flex items-center gap-1">
                {chip.icon}
                <span className="opacity-50 text-[9px]">{chip.label}</span>
                <span className="font-bold">{chip.value}</span>
              </span>
            </span>
          ))}
        </div>
      </td>
    </tr>
  );
};

// ── Types ─────────────────────────────────────────

interface ChipDef {
  icon: React.ReactNode;
  label: string;
  value: string;
  gradient: string;
  border: string;
  text: string;
  glow: string;
}

// ── Extraction utilities ──────────────────────────

function extractModel(job: Job): string | null {
  const p = job.payload;
  if (p?.model && typeof p.model === "string") return p.model;
  const r = job.result;
  if (r?.metadata && typeof r.metadata === "object") {
    const meta = r.metadata as Record<string, unknown>;
    if (meta.model && typeof meta.model === "string") return meta.model;
  }
  return null;
}

function extractProvider(job: Job): string | null {
  const p = job.payload;
  if (p?.provider && typeof p.provider === "string") return p.provider;
  return resolveProviderFromFeature(job.featureKey);
}

function extractInferenceDuration(job: Job): string | null {
  const r = job.result;
  if (r?.durationMs && typeof r.durationMs === "number") {
    return formatMs(r.durationMs);
  }
  if (job.status === "COMPLETED" && job.createdAt && job.updatedAt) {
    try {
      const diff = differenceInMilliseconds(
        parseISO(job.updatedAt),
        parseISO(job.createdAt),
      );
      if (diff >= 0) return formatMs(diff);
    } catch {
      /* ignore */
    }
  }
  return null;
}

function extractOutputFormat(job: Job): string | null {
  const r = job.result;
  if (r?.format && typeof r.format === "string") return r.format;
  return null;
}

function extractVoice(job: Job): string | null {
  const p = job.payload;
  if (p?.voice && typeof p.voice === "string") return p.voice;
  return null;
}

function extractExtraMetrics(job: Job): { key: string; value: string }[] {
  const r = job.result;
  const results: { key: string; value: string }[] = [];

  if (r?.metrics && typeof r.metrics === "object") {
    const metrics = r.metrics as Record<string, unknown>;
    Object.entries(metrics).forEach(([key, val]) => {
      if (val !== null && val !== undefined) {
        results.push({
          key: key.replace(/([A-Z])/g, " $1").trim(),
          value: String(val),
        });
      }
    });
  }

  if (r?.keyframeCount && typeof r.keyframeCount === "number") {
    results.push({ key: "Keyframes", value: String(r.keyframeCount) });
  }

  return results;
}

function formatMs(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  const secs = Math.floor(ms / 1000);
  if (secs < 60) return `${secs}.${Math.round((ms % 1000) / 100)}s`;
  const mins = Math.floor(secs / 60);
  const remSecs = secs % 60;
  return `${mins}m ${remSecs}s`;
}
