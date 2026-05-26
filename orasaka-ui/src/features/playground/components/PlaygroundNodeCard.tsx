import React from "react";
import type { OperationNode } from "@/features/playground/types/playground.types";
import { NODE_ID } from "@/core/constants/capability.constants";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import {
  Lock,
  Play,
  FileText,
  Image as ImageIcon,
  Music,
  Loader2,
} from "lucide-react";
import { NodeFieldRenderer } from "./NodeFieldRenderer";
import { NodeHints } from "./NodeHints";
import { NodeResultFooter } from "./NodeResultFooter";
import { ExecutionTimeline } from "./ExecutionTimeline";
import { useNodeCardState } from "@/features/playground/hooks/useNodeCardState";

interface PlaygroundNodeCardProps {
  node: OperationNode;
  onExecuted?: () => void;
}

/**
 * Renders a single capability card in the Playground grid.
 * All state management is delegated to useNodeCardState;
 * field rendering to NodeFieldRenderer; hints to NodeHints;
 * and result display to NodeResultFooter.
 */
export function PlaygroundNodeCard({
  node,
  onExecuted,
}: PlaygroundNodeCardProps) {
  const {
    t,
    kind,
    inputs,
    globalResult,
    placeholders,
    category,
    currentProvider,
    uniqueProviders,
    modelCatalog,
    uploadingFields,
    currentModel,
    activeProgress,
    displayPending,
    isLocked,
    handleInputChange,
    handleFileUpload,
    executeNode,
  } = useNodeCardState(node, onExecuted);

  const iconStyle = "h-5 w-5 text-amber-500";

  return (
    <Card
      id={`node-card-${node.id.replace(/\./g, "-")}`}
      data-node-id={node.id}
      className={`p-6 bg-[var(--surface-1)] border-[var(--border-subtle)] flex flex-col gap-5 shadow-sm relative overflow-hidden transition-all duration-200 ${
        isLocked ? "opacity-75 bg-[var(--surface-0)]" : ""
      }`}
    >
      {isLocked && (
        <div className="absolute top-3 right-3 bg-red-500/10 text-red-500 border border-red-500/20 px-2 py-1 rounded-lg flex items-center gap-1.5 text-xs font-semibold select-none">
          <Lock className="h-3 w-3" /> {t.playground.locked}
        </div>
      )}

      <h2 className="text-lg font-semibold flex items-center justify-between border-b pb-3 border-[var(--border-subtle)]">
        <div className="flex items-center gap-2">
          {kind === "image" ? (
            <ImageIcon className={iconStyle} />
          ) : kind === "audio" ? (
            <Music className={iconStyle} />
          ) : (
            <FileText className={iconStyle} />
          )}
          {node.label}
        </div>

        <span
          className={`text-[10px] px-2 py-0.5 rounded-full border font-semibold tracking-wide ${
            ["gemini", "anthropic", "openai"].includes(currentProvider)
              ? "bg-amber-500/10 text-amber-600 border-amber-500/20"
              : "bg-emerald-500/10 text-emerald-600 border-emerald-500/20"
          }`}
        >
          {["gemini", "anthropic", "openai"].includes(currentProvider)
            ? t.playground.commercialCloudBridge
            : t.playground.localInfrastructure}
        </span>
      </h2>

      <div className="flex flex-col gap-4 flex-1">
        <NodeFieldRenderer
          nodeId={node.id}
          placeholders={placeholders}
          inputs={inputs}
          isLocked={isLocked}
          displayPending={displayPending}
          category={category}
          currentProvider={currentProvider}
          uniqueProviders={uniqueProviders}
          modelCatalog={modelCatalog}
          uploadingFields={uploadingFields}
          onInputChange={handleInputChange}
          onFileUpload={handleFileUpload}
          onExecute={executeNode}
          nodeIcon={node.icon}
        />

        <NodeHints
          nodeId={node.id}
          isLocked={isLocked}
          inputs={inputs}
          onInputChange={handleInputChange}
          t={t}
        />

        {isLocked && node.state.reason && (
          <div className="p-3 bg-red-500/5 border border-red-500/10 rounded-xl text-xs text-red-600 dark:text-red-400/90 leading-relaxed">
            {node.state.reason}
          </div>
        )}
      </div>

      {node.id === NODE_ID.MEDIA_VIDEO && displayPending && (
        <div className="w-full bg-[var(--surface-2)] rounded-full h-2 mt-1 overflow-hidden">
          <div
            className="bg-amber-500 h-full transition-all duration-300 rounded-full"
            style={{
              width: `${activeProgress !== undefined ? activeProgress : 0}%`,
            }}
          />
        </div>
      )}

      <Button
        onClick={executeNode}
        disabled={displayPending || isLocked}
        className="w-full flex items-center justify-center gap-2 mt-2"
      >
        {displayPending ? (
          <>
            {node.id === NODE_ID.MEDIA_VIDEO && activeProgress !== undefined ? (
              <span className="font-semibold text-amber-500 animate-pulse">
                {t.playground.processingProgress.replace(
                  "{percent}",
                  activeProgress.toString(),
                )}
              </span>
            ) : (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />{" "}
                {t.playground.runningIngestion}
              </>
            )}
          </>
        ) : (
          <>
            <Play className="h-4 w-4" />{" "}
            {node.id === NODE_ID.CHAT_SPEECH
              ? t.playground.synthesizeSpeech
              : t.playground.executeIngestion}
          </>
        )}
      </Button>

      <ExecutionTimeline
        progress={activeProgress}
        modelName={currentModel || ""}
        isPending={displayPending}
      />

      {globalResult && <NodeResultFooter result={globalResult} t={t} />}
    </Card>
  );
}
