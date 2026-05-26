"use client";

import React from "react";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { useTranslation } from "@/core/context/LocaleContext";
import { useOperationGraph } from "@/features/playground/hooks/useOperationGraph";
import {
  Network,
  RefreshCw,
  CheckCircle2,
  Lock,
  EyeOff,
  Loader2,
} from "lucide-react";
import type { OperationNode } from "@/features/playground/types/playground.types";

/**
 * Renders a state badge for an Operation Graph node.
 */
function StateBadge({
  type,
  labels,
}: Readonly<{
  type: OperationNode["state"]["type"];
  labels: { active: string; locked: string; invisible: string };
}>) {
  switch (type) {
    case "ACTIVE":
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
          <CheckCircle2 className="h-3 w-3" />
          {labels.active}
        </span>
      );
    case "LOCKED":
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/20">
          <Lock className="h-3 w-3" />
          {labels.locked}
        </span>
      );
    case "INVISIBLE":
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-zinc-500/15 text-zinc-500 dark:text-zinc-400 border border-zinc-500/20">
          <EyeOff className="h-3 w-3" />
          {labels.invisible}
        </span>
      );
  }
}

/**
 * A dedicated visualization card for the Orasaka Operation Graph (SDUI).
 * Renders a compact table of all registered nodes with their state badges,
 * feature keys, labels, and endpoint routing information.
 */
export function OperationGraphCard() {
  const { t } = useTranslation();
  const { nodes, isLoading, invalidate } = useOperationGraph(true);
  const [isRefreshing, setIsRefreshing] = React.useState(false);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await invalidate();
    setTimeout(() => setIsRefreshing(false), 600);
  };

  const stateLabels = {
    active: t.operationGraph.stateActive,
    locked: t.operationGraph.stateLocked,
    invisible: t.operationGraph.stateInvisible,
  };

  return (
    <Card className="col-span-full border-card-border bg-card-bg/60 backdrop-blur-xl shadow-lg transition-all duration-300 hover:shadow-xl">
      {/* Header */}
      <div className="flex items-center justify-between p-5 border-b border-card-border">
        <header className="flex items-center gap-3">
          <figure className="flex items-center justify-center h-10 w-10 rounded-xl bg-gradient-to-br from-violet-500/20 to-fuchsia-500/20 border border-violet-500/20">
            <Network className="h-5 w-5 text-violet-500" />
          </figure>
          <hgroup>
            <h3 className="font-semibold text-sm text-foreground tracking-tight">
              {t.operationGraph.title}
            </h3>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 max-w-md">
              {t.operationGraph.subtitle}
            </p>
          </hgroup>
        </header>
        <div className="flex items-center gap-3">
          {!isLoading && nodes.length > 0 && (
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400 tabular-nums">
              {t.operationGraph.nodeCount(nodes.length)}
            </span>
          )}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="h-8 w-8 p-0"
          >
            <RefreshCw
              className={`h-4 w-4 ${isRefreshing ? "animate-spin" : ""}`}
            />
          </Button>
        </div>
      </div>

      {/* Body */}
      <div className="p-5">
        {(() => {
          if (isLoading) {
            return (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-violet-500" />
              </div>
            );
          }
          if (nodes.length === 0) {
            return (
              <p className="text-center text-sm text-zinc-500 dark:text-zinc-400 py-8">
                {t.operationGraph.noNodes}
              </p>
            );
          }
          return (
            <div className="overflow-x-auto rounded-lg border border-card-border">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-card-border bg-zinc-50/50 dark:bg-zinc-900/50">
                    <th className="text-left px-4 py-2.5 font-medium text-zinc-600 dark:text-zinc-400">
                      {t.operationGraph.colFeature}
                    </th>
                    <th className="text-left px-4 py-2.5 font-medium text-zinc-600 dark:text-zinc-400">
                      {t.operationGraph.colLabel}
                    </th>
                    <th className="text-left px-4 py-2.5 font-medium text-zinc-600 dark:text-zinc-400">
                      {t.operationGraph.colState}
                    </th>
                    <th className="text-left px-4 py-2.5 font-medium text-zinc-600 dark:text-zinc-400 hidden md:table-cell">
                      {t.operationGraph.colEndpoint}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {nodes.map((node, idx) => (
                    <tr
                      key={node.id}
                      className={`border-b border-card-border last:border-b-0 transition-colors hover:bg-zinc-50/80 dark:hover:bg-zinc-800/40 ${
                        idx % 2 === 0 ? "" : "bg-zinc-25 dark:bg-zinc-900/30"
                      }`}
                    >
                      <td className="px-4 py-2.5">
                        <code className="text-xs font-mono text-violet-600 dark:text-violet-400 bg-violet-500/10 px-1.5 py-0.5 rounded">
                          {node.id}
                        </code>
                      </td>
                      <td className="px-4 py-2.5 text-foreground font-medium">
                        {node.label}
                      </td>
                      <td className="px-4 py-2.5">
                        <StateBadge type={node.state.type} labels={stateLabels} />
                      </td>
                      <td className="px-4 py-2.5 hidden md:table-cell">
                        <span className="text-xs font-mono text-zinc-500 dark:text-zinc-400">
                          {node.executionDetails.httpMethod}{" "}
                          {node.executionDetails.uriPath}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          );
        })()}
      </div>
    </Card>
  );
}
