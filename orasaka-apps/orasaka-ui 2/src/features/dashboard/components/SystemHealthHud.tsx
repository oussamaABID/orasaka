/* eslint-disable no-restricted-syntax */
"use client";

import React from "react";
import { Icon } from "@/components/ui/icon";

/**
 * SystemHealthHud — Compact real-time system health panel.
 * Shows memory pressure, active model, pipeline state, and uptime.
 * JetBrains Mono for numeric values. Mini sparkline for memory trend.
 */
export function SystemHealthHud() {
  // Mock data — in production, these come from the BFF /api/health endpoint
  const health = {
    memoryPercent: 42,
    activeModel: "llama3.1:8b",
    provider: "Ollama",
    pipelineEnabled: true,
    uptimeHours: 127,
    requestsToday: 284,
  };

  const memoryColor =
    health.memoryPercent > 85
      ? "var(--status-error)"
      : health.memoryPercent > 60
        ? "var(--status-warning)"
        : "var(--status-success)";

  return (
    <section className="glass-card rounded-[var(--radius-lg)] p-[var(--space-card)] space-y-4">
      {/* Header */}
      <header className="flex items-center gap-2">
        <Icon name="model" size={16} className="text-[var(--accent)]" />
        <h3 className="hud-label !mb-0">SYSTEM HEALTH</h3>
      </header>

      {/* Active Model */}
      <article className="space-y-1">
        <span className="text-[10px] uppercase tracking-wider text-[var(--text-muted)] font-semibold">
          Active Model
        </span>
        <p className="flex items-center gap-2">
          <span className="status-dot text-[var(--status-success)]" />
          <span className="hud-value">{health.activeModel}</span>
          <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-[var(--surface-2)] text-[var(--text-muted)] font-medium">
            {health.provider}
          </span>
        </p>
      </article>

      {/* Memory Pressure Gauge */}
      <figure className="space-y-2">
        <figcaption className="flex items-center justify-between">
          <span className="text-[10px] uppercase tracking-wider text-[var(--text-muted)] font-semibold">
            Memory Pressure
          </span>
          <span
            className={`hud-value text-xs ${health.memoryPercent > 85 ? "text-[var(--status-error)]" : health.memoryPercent > 60 ? "text-[var(--status-warning)]" : "text-[var(--status-success)]"}`}
          >
            {health.memoryPercent}%
          </span>
        </figcaption>
        {/* Gauge bar */}
        <aside className="h-1.5 rounded-full bg-[var(--surface-3)] overflow-hidden">
          {/* eslint-disable-next-line no-restricted-syntax */}
          <span
            className="block h-full rounded-full transition-all duration-700 ease-out"
            style={{
              width: `${health.memoryPercent}%`,
              background: `linear-gradient(90deg, ${memoryColor}, color-mix(in srgb, ${memoryColor} 70%, white))`,
            }}
          />
        </aside>
        {/* Mini sparkline (static SVG placeholder) */}
        <svg
          viewBox="0 0 100 20"
          className="w-full h-4 opacity-30"
          preserveAspectRatio="none"
        >
          <polyline
            points="0,15 10,12 20,14 30,10 40,13 50,8 60,11 70,9 80,12 90,7 100,10"
            fill="none"
            stroke={memoryColor}
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </figure>

      {/* Stats row */}
      <section className="grid grid-cols-2 gap-3">
        <article className="space-y-1">
          <span className="text-[10px] uppercase tracking-wider text-[var(--text-muted)] font-semibold">
            Pipeline
          </span>
          <p className="flex items-center gap-1.5">
            <span
              className={`status-dot ${health.pipelineEnabled ? "text-[var(--status-success)]" : "text-[var(--status-error)]"}`}
            />
            <span className="hud-value text-xs">
              {health.pipelineEnabled ? "Enabled" : "Disabled"}
            </span>
          </p>
        </article>
        <article className="space-y-1">
          <span className="text-[10px] uppercase tracking-wider text-[var(--text-muted)] font-semibold">
            Requests Today
          </span>
          <span className="hud-value text-xs block">
            {health.requestsToday.toLocaleString()}
          </span>
        </article>
      </section>

      {/* Uptime */}
      <footer className="pt-2 border-t border-[var(--border-subtle)]">
        <p className="flex items-center justify-between">
          <span className="text-[10px] text-[var(--text-muted)]">Uptime</span>
          <span className="hud-value text-[11px]">
            {Math.floor(health.uptimeHours / 24)}d {health.uptimeHours % 24}h
          </span>
        </p>
      </footer>
    </section>
  );
}
