"use client";

import React, { useState, useEffect } from "react";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import {
  Terminal,
  Loader2,
  Play,
  AlertCircle,
  CheckCircle,
} from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import { useSession } from "next-auth/react";

interface ToolInfo {
  name: string;
  description: string;
  inputSchema: string;
}

interface ParsedSchema {
  type: string;
  properties?: Record<string, { type: string; description?: string }>;
  required?: string[];
}

export function McpToolTester() {
  const { t } = useTranslation();
  const { data: session } = useSession();
  const token = session?.user?.id;

  const [tools, setTools] = useState<ToolInfo[]>([]);
  const [selectedToolName, setSelectedToolName] = useState<string>("");
  const [argumentsMap, setArgumentsMap] = useState<Record<string, string>>({});
  const [isLoadingTools, setIsLoadingTools] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoadingTools(true);
    fetch("/api/v1/mcp/tools", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error(t.playground.mcpFetchError);
        return res.json();
      })
      .then((data) => {
        setTools(data);
        if (data.length > 0) {
          setSelectedToolName(data[0].name);
        }
      })
      .catch((err) => {
        console.error(err);
        setError(t.playground.mcpLoadError);
      })
      .finally(() => {
        setIsLoadingTools(false);
      });
  }, [token, t.playground.mcpFetchError, t.playground.mcpLoadError]);

  const selectedTool = tools.find((t) => t.name === selectedToolName);

  let parsedSchema: ParsedSchema | null = null;
  if (selectedTool?.inputSchema) {
    try {
      parsedSchema = JSON.parse(selectedTool.inputSchema);
    } catch (e) {
      console.error("Failed to parse schema for tool: " + selectedTool.name, e);
    }
  }

  const handleArgChange = (key: string, value: string) => {
    setArgumentsMap((prev) => ({ ...prev, [key]: value }));
  };

  const handleExecute = async (e?: React.SubmitEvent) => {
    if (e) e.preventDefault();
    if (!selectedToolName) return;

    setIsExecuting(true);
    setResult(null);
    setError(null);

    // Build execution payload
    const payload: Record<string, string> = {};
    if (parsedSchema?.properties) {
      Object.keys(parsedSchema.properties).forEach((key) => {
        const val = argumentsMap[key] || "";
        payload[key] = val;
      });
    }

    try {
      const headers: Record<string, string> = {
        "Content-Type": "application/json",
      };
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const res = await fetch(`/api/v1/mcp/tools/${selectedToolName}/execute`, {
        method: "POST",
        headers,
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || t.playground.mcpExecError);
      }

      setResult(data.result || JSON.stringify(data, null, 2));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : t.playground.mcpExecError;
      setError(message);
    } finally {
      setIsExecuting(false);
    }
  };

  // Reset arguments when selected tool changes
  useEffect(() => {
    setTimeout(() => {
      setArgumentsMap({});
      setResult(null);
      setError(null);
    }, 0);
  }, [selectedToolName]);

  return (
    <Card className="p-6 bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 flex flex-col gap-5 shadow-sm col-span-1">
      <h2 className="text-lg font-semibold flex items-center gap-2 border-b pb-3 border-zinc-100 dark:border-zinc-800">
        <Terminal className="h-5 w-5 text-violet-500" />
        {t.playground.mcpTitle}
      </h2>

      {(() => {
        if (isLoadingTools) {
          return (
            <div className="flex items-center justify-center py-6">
              <Loader2 className="h-6 w-6 animate-spin text-violet-500" />
            </div>
          );
        }
        if (tools.length === 0) {
          return (
            <div className="text-zinc-500 dark:text-zinc-400 text-sm py-4 text-center">
              {t.playground.mcpNoTools}
            </div>
          );
        }
        return (
          <form onSubmit={handleExecute} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">
                {t.playground.mcpSelectTool}
              </label>
              <select
                value={selectedToolName}
                onChange={(e) => setSelectedToolName(e.target.value)}
                className="w-full rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950 p-2 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-violet-500"
              >
                {tools.map((t) => (
                  <option key={t.name} value={t.name}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>

            {selectedTool && (
              <div className="text-xs text-zinc-500 dark:text-zinc-400 leading-relaxed bg-zinc-50 dark:bg-zinc-950/40 p-3 rounded-lg border border-zinc-100 dark:border-zinc-800/60">
                {selectedTool.description}
              </div>
            )}

            {parsedSchema?.properties && (
              <div className="flex flex-col gap-3">
                {Object.entries(parsedSchema.properties).map(([key, prop]) => (
                  <div key={key} className="flex flex-col gap-1.5">
                    <span className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">
                      {key}{" "}
                      {parsedSchema?.required?.includes(key) && (
                        <span className="text-red-500">*</span>
                      )}
                    </span>
                    <Input
                      value={argumentsMap[key] || ""}
                      onChange={(e) => handleArgChange(key, e.target.value)}
                      placeholder={
                        prop.description ||
                        t.playground.mcpEnterValue.replace("{key}", key)
                      }
                      className="bg-zinc-50 dark:bg-zinc-950 border-zinc-200 dark:border-zinc-800 text-sm"
                      required={parsedSchema?.required?.includes(key)}
                    />
                  </div>
                ))}
              </div>
            )}

            <Button
              type="submit"
              disabled={isExecuting || !selectedToolName}
              className="w-full flex items-center justify-center gap-2 mt-2 bg-violet-600 hover:bg-violet-700 active:bg-violet-800 text-white"
            >
              {isExecuting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />{" "}
                  {t.playground.mcpExecuting}
                </>
              ) : (
                <>
                  <Play className="h-4 w-4" /> {t.playground.mcpRunTool}
                </>
              )}
            </Button>
          </form>
        );
      })()}

      {error && (
        <div className="p-3 bg-red-500/5 border border-red-500/10 rounded-xl text-xs text-red-600 dark:text-red-400 flex items-start gap-2">
          <AlertCircle className="h-4 w-4 text-red-500 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {result && (
        <div className="p-4 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-950/20 text-sm max-h-48 overflow-y-auto">
          <div className="flex items-center gap-1.5 font-semibold mb-2 text-emerald-600 dark:text-emerald-400">
            <CheckCircle className="h-4 w-4" />
            {t.playground.mcpExecutionResult}
          </div>
          <pre className="font-mono text-xs text-zinc-700 dark:text-zinc-300 whitespace-pre-wrap break-all leading-relaxed">
            {result}
          </pre>
        </div>
      )}
    </Card>
  );
}
