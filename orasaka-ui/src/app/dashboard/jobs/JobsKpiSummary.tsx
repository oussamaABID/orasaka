"use client";

import * as React from "react";
import { Job } from "@/features/jobs/types/jobs.types";
import { Activity, Clock, Cpu, BarChart3, Zap, TrendingUp } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import { computeJobStats } from "./jobStats.utils";
import { AnimatedCounter } from "./AnimatedCounter";

interface JobsKpiSummaryProps {
  jobs: Job[];
}

interface CardDef {
  icon: React.ReactNode;
  label: string;
  value: number;
  valueSuffix: string;
  displayOverride?: string;
  sub: string;
  gradient: string;
  glow: string;
  iconBg: string;
  accent: string;
}

/**
 * Aggregated KPI summary banner with Jarvis-grade glassmorphism.
 * Each card features animated counters, gradient borders, and glow effects.
 */
export const JobsKpiSummary: React.FC<JobsKpiSummaryProps> = ({ jobs }) => {
  const { t } = useTranslation();
  const stats = React.useMemo(() => computeJobStats(jobs), [jobs]);

  const cards: CardDef[] = [
    {
      icon: <Activity className="w-4 h-4" />,
      label: t.jobs.kpiTotalJobs,
      value: stats.total,
      valueSuffix: "",
      sub: `${stats.completed} completed`,
      gradient: "from-zinc-400/20 via-zinc-500/10 to-transparent",
      glow: "shadow-zinc-500/5",
      iconBg: "bg-zinc-500/10",
      accent: "text-zinc-200",
    },
    {
      icon: <TrendingUp className="w-4 h-4" />,
      label: t.jobs.kpiSuccessRate,
      value:
        stats.total > 0 ? Math.round((stats.completed / stats.total) * 100) : 0,
      valueSuffix: "%",
      sub: `${stats.failed} failed`,
      gradient:
        stats.completed / Math.max(stats.total, 1) > 0.8
          ? "from-emerald-500/20 via-emerald-600/5 to-transparent"
          : "from-amber-500/20 via-amber-600/5 to-transparent",
      glow:
        stats.completed / Math.max(stats.total, 1) > 0.8
          ? "shadow-emerald-500/10"
          : "shadow-amber-500/10",
      iconBg:
        stats.completed / Math.max(stats.total, 1) > 0.8
          ? "bg-emerald-500/10"
          : "bg-amber-500/10",
      accent:
        stats.completed / Math.max(stats.total, 1) > 0.8
          ? "text-emerald-400"
          : "text-amber-400",
    },
    {
      icon: <Clock className="w-4 h-4" />,
      label: "Avg. Duration",
      value: stats.avgMs,
      valueSuffix: "",
      displayOverride: stats.avgDuration,
      sub: `max ${stats.maxDuration}`,
      gradient: "from-cyan-500/20 via-cyan-600/5 to-transparent",
      glow: "shadow-cyan-500/10",
      iconBg: "bg-cyan-500/10",
      accent: "text-cyan-400",
    },
    {
      icon: <Cpu className="w-4 h-4" />,
      label: t.jobs.kpiModelsUsed,
      value: stats.uniqueModels,
      valueSuffix: "",
      sub: stats.topModel || "—",
      gradient: "from-violet-500/20 via-violet-600/5 to-transparent",
      glow: "shadow-violet-500/10",
      iconBg: "bg-violet-500/10",
      accent: "text-violet-400",
    },
    {
      icon: <BarChart3 className="w-4 h-4" />,
      label: t.jobs.kpiCapabilities,
      value: stats.uniqueFeatures,
      valueSuffix: "",
      sub: stats.topFeature || "—",
      gradient: "from-amber-500/20 via-amber-600/5 to-transparent",
      glow: "shadow-amber-500/10",
      iconBg: "bg-amber-500/10",
      accent: "text-amber-400",
    },
    {
      icon: <Zap className="w-4 h-4" />,
      label: t.jobs.kpiTotalInference,
      value: stats.totalMs,
      valueSuffix: "",
      displayOverride: stats.totalDuration,
      sub: `across ${stats.completed} jobs`,
      gradient: "from-pink-500/20 via-pink-600/5 to-transparent",
      glow: "shadow-pink-500/10",
      iconBg: "bg-pink-500/10",
      accent: "text-pink-400",
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
      {cards.map((card, i) => (
        <div
          key={card.label}
          className={`
            group relative overflow-hidden
            flex flex-col gap-1.5 p-4 rounded-2xl
            bg-white/[0.03] dark:bg-white/[0.02]
            border border-white/[0.06]
            backdrop-blur-xl
            ${card.glow} shadow-lg
            transition-all duration-300
            hover:scale-[1.03] hover:shadow-xl
            hover:border-white/[0.12]
            hover:bg-white/[0.05]
          `}
          style={{
            animationDelay: `${i * 60}ms`,
          }}
        >
          {/* Gradient overlay */}
          <div
            className={`absolute inset-0 bg-gradient-to-br ${card.gradient} opacity-60 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none`}
          />

          {/* Subtle grid pattern */}
          <div
            className="absolute inset-0 opacity-[0.015] pointer-events-none"
            style={{
              backgroundImage:
                "linear-gradient(rgba(255,255,255,.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.1) 1px, transparent 1px)",
              backgroundSize: "20px 20px",
            }}
          />

          {/* Content */}
          <section className="relative z-10 flex flex-col gap-1.5">
            {/* Icon + Label */}
            <header className="flex items-center gap-1.5">
              <figure
                className={`${card.iconBg} p-1 rounded-md ${card.accent} transition-transform duration-300 group-hover:scale-110`}
              >
                {card.icon}
              </figure>
              <span className="text-[9px] font-bold uppercase tracking-[0.12em] text-zinc-500 dark:text-zinc-400">
                {card.label}
              </span>
            </header>

            {/* Value */}
            <figure
              className={`text-xl font-black tracking-tight ${card.accent} transition-colors duration-300`}
            >
              {card.displayOverride ? (
                card.displayOverride
              ) : (
                <AnimatedCounter value={card.value} suffix={card.valueSuffix} />
              )}
            </figure>

            {/* Sub text */}
            <footer className="text-[10px] text-zinc-500 dark:text-zinc-500 font-medium truncate">
              {card.sub}
            </footer>
          </section>

          {/* Glow dot */}
          <div
            className={`absolute -top-1 -right-1 w-2 h-2 rounded-full ${card.accent.replace("text-", "bg-")} opacity-40 blur-[2px] group-hover:opacity-70 transition-opacity duration-500`}
          />
        </div>
      ))}
    </div>
  );
};
