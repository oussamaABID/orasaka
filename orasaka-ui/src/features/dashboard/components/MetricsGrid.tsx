import * as React from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/Card";

/**
 * Properties required by the {@link MetricsGrid} component.
 */
interface MetricsGridProps {
  /** Object containing workspace utilization metrics. */
  metrics: {
    /** Total active concurrent chat sessions. */
    activeSessions: number;
    /** Cumulative token budget used. */
    tokensUsed: number;
    /** Total saved memory nodes. */
    memoryNodes: number;
  };
  /** Aesthetic CSS configuration for active accent colors. */
  accentClasses: {
    /** CSS text color class utility. */
    text: string;
  };
  /** Dictionary containing local dashboard translation string values. */
  t: {
    dashboard: {
      activeSessions: string;
      runningParallel: string;
      tokensUsed: string;
      estimatedCumulative: string;
      memoryNodes: string;
      contextSaved: string;
    };
  };
}

/**
 * MetricsGrid renders the top summary cards for active sessions, token counts, and memory nodes.
 *
 * @param props - Component React properties.
 * @param props.metrics - Object containing workspace utilization metrics.
 * @param props.accentClasses - Aesthetic CSS configuration for active accent colors.
 * @param props.t - Dictionary containing local dashboard translation string values.
 * @returns The React component representing the metrics grid display.
 */
export function MetricsGrid({ metrics, accentClasses, t }: MetricsGridProps) {
  return (
    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {/* Metric Card 1: Active Sessions */}
      <Card className="hover:border-zinc-300 dark:hover:border-zinc-700 transition-all duration-200 hover:scale-[1.01]">
        <CardHeader className="pb-2">
          <p className="text-zinc-500 dark:text-zinc-400 font-semibold uppercase tracking-wider text-xs">
            {t.dashboard.activeSessions}
          </p>
        </CardHeader>
        <CardContent>
          <div className="flex items-baseline space-x-2">
            <p
              className={`text-4xl font-extrabold tracking-tight ${accentClasses.text}`}
            >
              {metrics.activeSessions}
            </p>
          </div>
          <p className="text-xs text-zinc-400 dark:text-zinc-500 mt-1">
            {t.dashboard.runningParallel}
          </p>
        </CardContent>
      </Card>

      {/* Metric Card 2: Tokens Used */}
      <Card className="hover:border-zinc-300 dark:hover:border-zinc-700 transition-all duration-200 hover:scale-[1.01]">
        <CardHeader className="pb-2">
          <p className="text-zinc-500 dark:text-zinc-400 font-semibold uppercase tracking-wider text-xs">
            {t.dashboard.tokensUsed}
          </p>
        </CardHeader>
        <CardContent>
          <div className="flex items-baseline space-x-2">
            <p
              className={`text-4xl font-extrabold tracking-tight ${accentClasses.text}`}
            >
              {metrics.tokensUsed >= 1000
                ? `${(metrics.tokensUsed / 1000).toFixed(1)}k`
                : metrics.tokensUsed.toLocaleString()}
            </p>
          </div>
          <p className="text-xs text-zinc-400 dark:text-zinc-500 mt-1">
            {t.dashboard.estimatedCumulative}
          </p>
        </CardContent>
      </Card>

      {/* Metric Card 3: Memory Nodes */}
      <Card className="hover:border-zinc-300 dark:hover:border-zinc-700 transition-all duration-200 hover:scale-[1.01]">
        <CardHeader className="pb-2">
          <p className="text-zinc-500 dark:text-zinc-400 font-semibold uppercase tracking-wider text-xs">
            {t.dashboard.memoryNodes}
          </p>
        </CardHeader>
        <CardContent>
          <div className="flex items-baseline space-x-2">
            <p
              className={`text-4xl font-extrabold tracking-tight ${accentClasses.text}`}
            >
              {metrics.memoryNodes.toLocaleString()}
            </p>
          </div>
          <p className="text-xs text-zinc-400 dark:text-zinc-500 mt-1">
            {t.dashboard.contextSaved}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
