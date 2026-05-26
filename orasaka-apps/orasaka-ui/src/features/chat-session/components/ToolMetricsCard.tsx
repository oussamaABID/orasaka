"use client";

import React from "react";

interface ToolMetric {
  label: string;
  value: string | number;
  unit?: string;
}

interface ToolMetricsPayload {
  type: "tool_metrics";
  toolName: string;
  data: Record<string, string | number>;
}

interface ToolMetricsCardProps {
  payload: ToolMetricsPayload;
}

/**
 * ToolMetricsCard — Generative UI for structured tool metric payloads.
 *
 * When the SSE stream contains a JSON block with `"type":"tool_metrics"`,
 * this component renders a reactive grid card instead of plain markdown text.
 * Uses the existing Krizaka design system (glass-card, CSS variables).
 *
 * @example Payload shape:
 * ```json
 * {"type":"tool_metrics","toolName":"doctor","data":{"cpu":"82%","memory":"4.2GB","latency":"120ms"}}
 * ```
 */
export function ToolMetricsCard({ payload }: Readonly<ToolMetricsCardProps>) {
  const metrics: ToolMetric[] = Object.entries(payload.data).map(
    ([key, value]) => ({
      label: formatLabel(key),
      value,
    }),
  );

  return (
    <article className="glass-card p-4 max-w-lg w-full animate-in fade-in slide-in-from-bottom-2 duration-300">
      {/* Header */}
      <div className="flex items-center gap-2 mb-3 pb-2 border-b border-[var(--border-subtle)]">
        <div className="w-6 h-6 flex items-center justify-center bg-[var(--accent-soft)] text-[var(--accent)]">
          <svg
            className="w-3.5 h-3.5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M10.5 6h9.75M10.5 6a1.5 1.5 0 11-3 0m3 0a1.5 1.5 0 10-3 0M3.75 6H7.5m3 12h9.75m-9.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-3.75 0H7.5m9-6h3.75m-3.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-9.75 0h9.75"
            />
          </svg>
        </div>
        <span className="text-xs font-semibold text-[var(--text-primary)] tracking-wide uppercase">
          {payload.toolName}
        </span>
        <span className="ml-auto text-[10px] text-[var(--text-muted)] font-mono">
          METRICS
        </span>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {metrics.map((metric) => (
          <div
            key={metric.label}
            className="flex flex-col gap-0.5 p-2 bg-[var(--surface-2)] border border-[var(--border-subtle)] transition-colors duration-200 hover:border-[var(--border-default)]"
          >
            <span className="text-[10px] font-medium text-[var(--text-muted)] uppercase tracking-wider">
              {metric.label}
            </span>
            <span className="text-sm font-semibold text-[var(--text-primary)] font-mono">
              {metric.value}
              {metric.unit && (
                <span className="text-[10px] text-[var(--text-muted)] ml-0.5">
                  {metric.unit}
                </span>
              )}
            </span>
          </div>
        ))}
      </div>
    </article>
  );
}

/**
 * Attempts to parse a raw string as a ToolMetrics JSON payload.
 * Returns the parsed payload or null if not a valid tool_metrics block.
 */
export function parseToolMetrics(
  raw: string,
): ToolMetricsPayload | null {
  try {
    // Check for JSON block markers in markdown
    const jsonMatch = raw.match(
      /```(?:json)?\s*(\{[\s\S]*?"type"\s*:\s*"tool_metrics"[\s\S]*?\})\s*```/,
    );
    const toParse = jsonMatch ? jsonMatch[1] : raw;
    const parsed = JSON.parse(toParse);
    if (
      parsed &&
      parsed.type === "tool_metrics" &&
      typeof parsed.toolName === "string" &&
      typeof parsed.data === "object"
    ) {
      return parsed as ToolMetricsPayload;
    }
  } catch {
    // Not a tool_metrics payload — fall through
  }
  return null;
}

/** Converts camelCase/snake_case keys to human-readable labels. */
function formatLabel(key: string): string {
  return key
    .replace(/_/g, " ")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .toLowerCase();
}
