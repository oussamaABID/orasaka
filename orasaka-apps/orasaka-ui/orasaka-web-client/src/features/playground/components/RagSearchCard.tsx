import React from "react";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Search, Loader2 } from "lucide-react";
import { useRagSearch } from "@/features/playground/hooks/useRagSearch";
import { useJobStream } from "@/core/context/JobStreamContext";
import { useTranslation } from "@/core/context/LocaleContext";

export function RagSearchCard() {
  const { t } = useTranslation();
  const { ragQuery, setRagQuery } = useJobStream();
  const { search, result: ragResult, isPending, error } = useRagSearch();

  const runRagSearch = () => {
    if (!ragQuery.trim()) return;
    search(ragQuery);
  };

  return (
    <Card className="p-6 bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 flex flex-col gap-5 shadow-sm">
      <h2 className="text-lg font-semibold flex items-center gap-2 border-b pb-3 border-zinc-100 dark:border-zinc-800">
        <Search className="h-5 w-5 text-amber-500" />
        {t.playground.passiveRagTitle}
      </h2>

      <div className="flex flex-col gap-2">
        <div className="flex gap-2">
          <Input
            value={ragQuery}
            onChange={(e) => setRagQuery(e.target.value)}
            placeholder={t.playground.passiveRagPlaceholder}
            className="bg-zinc-50 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 text-sm"
            onKeyDown={(e) => e.key === "Enter" && runRagSearch()}
          />
          <Button
            onClick={runRagSearch}
            disabled={isPending || !ragQuery.trim()}
            className="flex items-center gap-1"
          >
            {isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Search className="h-4 w-4" />
            )}
            {t.playground.search}
          </Button>
        </div>
        <div className="flex flex-wrap gap-2 items-center">
          <span className="text-[10px] uppercase tracking-wider font-semibold text-zinc-400 dark:text-zinc-500">
            {t.playground.examples}
          </span>
          {[
            "Virtual thread concurrency",
            "OAuth2 token validation",
            "BFF proxy routes",
            "Tool callback registry",
          ].map((example) => (
            <button
              key={example}
              type="button"
              onClick={() => {
                setRagQuery(example);
                search(example);
              }}
              disabled={isPending}
              className="text-xs px-2.5 py-1 rounded-full border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900 hover:bg-zinc-100 dark:hover:bg-zinc-800 text-zinc-600 dark:text-zinc-400 font-medium transition-colors cursor-pointer select-none"
            >
              {example}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="p-3 bg-red-500/5 border border-red-500/10 rounded-xl text-xs text-red-600 dark:text-red-400">
          {error}
        </div>
      )}

      {ragResult && (
        <div className="p-4 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-950/20 text-sm max-h-48 overflow-y-auto">
          <div className="font-semibold mb-2">
            {t.playground.retrievedContext}
          </div>
          <p className="font-mono text-xs text-zinc-600 dark:text-zinc-400 whitespace-pre-line leading-relaxed">
            {ragResult}
          </p>
        </div>
      )}
    </Card>
  );
}
