import React from "react";
import { CheckCircle, AlertCircle } from "lucide-react";
import { ResultDisplay } from "./ResultDisplay";
import type { TranslationDictionary } from "@/core/context/translations.types";

interface NodeResult {
  success: boolean;
  data: string;
  error?: string;
}

interface NodeResultFooterProps {
  result: NodeResult;
  t: TranslationDictionary;
}

/**
 * Renders the execution result panel (success or error) for a PlaygroundNodeCard.
 */
export function NodeResultFooter({ result, t }: Readonly<NodeResultFooterProps>) {
  return (
    <div
      className={`p-4 rounded-xl border text-sm mt-3 ${
        result.success
          ? "bg-[var(--surface-1)] border-[var(--border-subtle)]"
          : "bg-red-50 dark:bg-red-950/10 border-red-200 dark:border-red-900/30 text-red-600 dark:text-red-400"
      }`}
    >
      <div className="flex items-center gap-1.5 font-semibold mb-2">
        {result.success ? (
          <CheckCircle className="h-4.5 w-4.5 text-green-500" />
        ) : (
          <AlertCircle className="h-4.5 w-4.5 text-red-500" />
        )}
        {result.success
          ? t.playground.outputResult
          : t.playground.gatewayRestriction}
      </div>
      {result.success ? (
        <ResultDisplay payload={result.data} />
      ) : (
        <p className="whitespace-pre-line leading-relaxed font-mono text-xs text-[var(--text-secondary)]">
          {result.data}
        </p>
      )}
    </div>
  );
}
