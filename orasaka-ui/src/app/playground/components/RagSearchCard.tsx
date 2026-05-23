import React, { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Search, Loader2 } from "lucide-react";

export function RagSearchCard() {
  const [ragQuery, setRagQuery] = useState("");
  const [ragResult, setRagResult] = useState("");

  const searchMutation = useMutation({
    mutationFn: async (query: string) => {
      const response = await fetch(
        `/api/v1/media/search-rag?q=${encodeURIComponent(query)}`,
        {
          headers: { Authorization: "Bearer user-mock" },
        },
      );
      if (!response.ok) {
        throw new Error(`RAG query failed with status ${response.status}`);
      }
      const data = await response.json();
      return data.context || "No context found matching query.";
    },
    onSuccess: (context) => {
      setRagResult(context);
    },
    onError: (e: Error) => {
      setRagResult(e.message || "RAG query failed.");
    },
  });

  const runRagSearch = () => {
    if (!ragQuery.trim()) return;
    setRagResult("");
    searchMutation.mutate(ragQuery);
  };

  return (
    <Card className="p-6 bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 flex flex-col gap-5 shadow-sm col-span-1 lg:col-span-2">
      <h2 className="text-lg font-semibold flex items-center gap-2 border-b pb-3 border-zinc-100 dark:border-zinc-800">
        <Search className="h-5 w-5 text-amber-500" />
        Passive RAG Context Retrieval (all-minilm)
      </h2>

      <div className="flex gap-2">
        <Input
          value={ragQuery}
          onChange={(e) => setRagQuery(e.target.value)}
          placeholder="Enter query to search semantic indexes"
          className="bg-zinc-50 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 text-sm"
          onKeyDown={(e) => e.key === "Enter" && runRagSearch()}
        />
        <Button
          onClick={runRagSearch}
          disabled={searchMutation.isPending || !ragQuery.trim()}
          className="flex items-center gap-1"
        >
          {searchMutation.isPending ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Search className="h-4 w-4" />
          )}
          Search
        </Button>
      </div>

      {ragResult && (
        <div className="p-4 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-950/20 text-sm max-h-48 overflow-y-auto">
          <div className="font-semibold mb-2">Retrieved Context</div>
          <p className="font-mono text-xs text-zinc-600 dark:text-zinc-400 whitespace-pre-line leading-relaxed">
            {ragResult}
          </p>
        </div>
      )}
    </Card>
  );
}
