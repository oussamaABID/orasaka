import React, { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { OperationNode } from "../types";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import {
  Lock,
  Play,
  FileText,
  Image as ImageIcon,
  Music,
  Loader2,
  CheckCircle,
  AlertCircle,
} from "lucide-react";
import { ResultDisplay } from "./ResultDisplay";
import { MediaUploadField } from "./MediaUploadField";

interface PlaygroundNodeCardProps {
  node: OperationNode;
  onExecuted?: () => void;
}

/**
 * Component representing a single capability node card in the playground.
 */
export function PlaygroundNodeCard({
  node,
  onExecuted,
}: PlaygroundNodeCardProps) {
  const [inputs, setInputs] = useState<Record<string, string>>({});
  const [result, setResult] = useState<{
    success: boolean;
    data: string;
  } | null>(null);

  const idLower = node.id.toLowerCase();
  const kind =
    idLower.includes("image") ||
    idLower.includes("vision") ||
    node.icon === "image"
      ? "image"
      : idLower.includes("audio") ||
          idLower.includes("speech") ||
          node.icon === "mic"
        ? "audio"
        : "text";

  const template = node.executionDetails.payloadTemplate;
  const placeholders = template
    ? (template.match(/\${(.*?)}/g) || []).map((m) => m.replace(/\${|}/g, ""))
    : [];

  const handleInputChange = (field: string, value: string) => {
    setInputs((prev) => ({ ...prev, [field]: value }));
  };

  const handleFileUpload = (field: string, file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      if (e.target?.result && typeof e.target.result === "string") {
        handleInputChange(field, e.target.result.split(",")[1]);
      }
    };
    reader.readAsDataURL(file);
  };

  const executeMutation = useMutation({
    mutationFn: async (payload: string) => {
      const response = await fetch(node.executionDetails.uriPath, {
        method: node.executionDetails.httpMethod,
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer user-mock",
        },
        body: node.executionDetails.httpMethod !== "GET" ? payload : undefined,
      });
      if (!response.ok) {
        throw new Error(
          response.status === 403
            ? "Access Forbidden: Restricted by gateway protection policy."
            : `Execution failed with status ${response.status}`,
        );
      }
      const contentType = response.headers.get("content-type") || "";
      if (contentType.includes("application/json")) {
        const json = await response.json();
        return json.analysis || json.content || JSON.stringify(json, null, 2);
      }
      return response.text();
    },
    onSuccess: (data) => {
      setResult({ success: true, data });
      if (onExecuted) {
        onExecuted();
      }
    },
    onError: (e: Error) => {
      setResult({
        success: false,
        data: e.message || "Unknown execution error",
      });
    },
  });

  const executeNode = () => {
    let payload = "";
    if (template) {
      let temp = template;
      for (const p of placeholders) {
        temp = temp.replace(`\${${p}}`, inputs[p] || "");
      }
      payload = temp;
    }
    setResult(null);
    executeMutation.mutate(payload);
  };

  const isLocked = node.state.type === "LOCKED";
  const iconStyle = "h-5 w-5 text-amber-500";

  return (
    <Card
      className={`p-6 bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 flex flex-col gap-5 shadow-sm relative overflow-hidden transition-all duration-200 ${
        isLocked
          ? "opacity-75 border-zinc-300 dark:border-zinc-800/80 bg-zinc-50/50 dark:bg-zinc-900/40"
          : ""
      }`}
    >
      {isLocked && (
        <div className="absolute top-3 right-3 bg-red-500/10 text-red-500 border border-red-500/20 px-2 py-1 rounded-lg flex items-center gap-1.5 text-xs font-semibold select-none">
          <Lock className="h-3 w-3" /> Locked
        </div>
      )}

      <h2 className="text-lg font-semibold flex items-center gap-2 border-b pb-3 border-zinc-100 dark:border-zinc-800">
        {kind === "image" ? (
          <ImageIcon className={iconStyle} />
        ) : kind === "audio" ? (
          <Music className={iconStyle} />
        ) : (
          <FileText className={iconStyle} />
        )}
        {node.label}
      </h2>

      <div className="flex flex-col gap-4 flex-1">
        {placeholders.map((field) => {
          const isBase64 =
            field.toLowerCase().includes("base64") ||
            field.toLowerCase().includes("image");
          if (isBase64) {
            return (
              <MediaUploadField
                key={field}
                field={field}
                inputs={inputs}
                isLocked={isLocked}
                handleInputChange={handleInputChange}
                handleFileUpload={handleFileUpload}
              />
            );
          }

          return (
            <div key={field} className="flex flex-col gap-1.5">
              <span className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">
                {field}
              </span>
              <Input
                value={inputs[field] || ""}
                disabled={isLocked}
                onChange={(e) => handleInputChange(field, e.target.value)}
                placeholder={`Enter ${field} value`}
                className="bg-zinc-50 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 text-sm"
              />
            </div>
          );
        })}

        {isLocked && node.state.reason && (
          <div className="p-3 bg-red-500/5 border border-red-500/10 rounded-xl text-xs text-red-600 dark:text-red-400/90 leading-relaxed">
            {node.state.reason}
          </div>
        )}
      </div>

      <Button
        onClick={executeNode}
        disabled={executeMutation.isPending || isLocked}
        className="w-full flex items-center justify-center gap-2 mt-2"
      >
        {executeMutation.isPending ? (
          <>
            <Loader2 className="h-4 w-4 animate-spin" /> Running Ingestion...
          </>
        ) : (
          <>
            <Play className="h-4 w-4" /> Execute Ingestion
          </>
        )}
      </Button>

      {result && (
        <div
          className={`p-4 rounded-xl border text-sm mt-3 ${
            result.success
              ? "bg-zinc-50/50 dark:bg-zinc-950/20 border-zinc-200 dark:border-zinc-800"
              : "bg-red-50 dark:bg-red-950/10 border-red-200 dark:border-red-900/30 text-red-600 dark:text-red-400"
          }`}
        >
          <div className="flex items-center gap-1.5 font-semibold mb-2">
            {result.success ? (
              <CheckCircle className="h-4.5 w-4.5 text-green-500" />
            ) : (
              <AlertCircle className="h-4.5 w-4.5 text-red-500" />
            )}
            {result.success ? "Output Result" : "Gateway Restriction"}
          </div>
          {result.success ? (
            <ResultDisplay payload={result.data} />
          ) : (
            <p className="whitespace-pre-line leading-relaxed font-mono text-xs text-zinc-700 dark:text-zinc-300">
              {result.data}
            </p>
          )}
        </div>
      )}
    </Card>
  );
}
