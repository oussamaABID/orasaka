/* eslint-disable no-restricted-syntax */
"use client";

import React, { useState, useRef } from "react";
import {
  Film,
  Upload,
  Loader2,
  CheckCircle,
  AlertCircle,
  Play,
} from "lucide-react";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { useVideoAnalysis } from "@/features/playground/hooks/useVideoAnalysis";
import { useTranslation } from "@/core/context/LocaleContext";
import { useJobStream } from "@/core/context/JobStreamContext";
import { ResultDisplay } from "./ResultDisplay";
import { ExecutionTimeline } from "./ExecutionTimeline";

const ACCEPTED_TYPES = ["video/mp4", "video/quicktime"];
const MAX_FILE_SIZE_MB = 100;

/**
 * Component for uploading and analyzing video files to extract keyframes and transcript metadata.
 * Styled to match the PlaygroundNodeCard design system, including localized keys (ERR-115).
 */
export function VideoAnalysisCard() {
  const {
    analyze,
    state,
    result,
    error: hookError,
    reset,
  } = useVideoAnalysis();
  const { jobProgress, videoAnalysisJobId } = useJobStream();
  const { t } = useTranslation();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [selectedModel, setSelectedModel] = useState<string>("whisper-base");
  const inputRef = useRef<HTMLInputElement>(null);

  const activeProgress = videoAnalysisJobId
    ? jobProgress[videoAnalysisJobId]
    : undefined;

  const handleFileSelection = (file: File) => {
    if (!ACCEPTED_TYPES.includes(file.type)) {
      alert(t.playground.onlyMp4MovSupported);
      return;
    }
    if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
      alert(t.playground.fileLimitExceeded);
      return;
    }
    setSelectedFile(file);
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragOver(true);
    } else if (e.type === "dragleave") {
      setDragOver(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(false);
    if (state !== "idle" && state !== "success" && state !== "error") return;
    const file = e.dataTransfer.files?.[0];
    if (file) handleFileSelection(file);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFileSelection(file);
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
    if (inputRef.current) inputRef.current.value = "";
  };

  const handleExecute = () => {
    if (selectedFile) {
      analyze(selectedFile, selectedModel);
    }
  };

  const handleReset = () => {
    reset();
    setSelectedFile(null);
    if (inputRef.current) inputRef.current.value = "";
  };

  const displayPending = state === "uploading" || state === "processing";

  return (
    <Card
      id="video-analysis-card"
      className="p-6 bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 flex flex-col gap-5 shadow-sm relative overflow-hidden transition-all duration-200"
    >
      <h2 className="text-lg font-semibold flex items-center gap-2 border-b pb-3 border-zinc-100 dark:border-zinc-800">
        <Film className="h-5 w-5 text-amber-500" />
        {t.playground.videoAnalysis}
      </h2>

      <div className="flex flex-col gap-4 flex-1">
        {state === "idle" && (
          <div className="flex flex-col gap-2">
            <span className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">
              {t.playground.mediaPayload} (video)
            </span>
            <section
              aria-label={t.playground.mediaPayload}
              onDragEnter={handleDrag}
              onDragOver={handleDrag}
              onDragLeave={handleDrag}
              onDrop={handleDrop}
              className={`border-2 border-dashed rounded-xl p-4 flex flex-col items-center justify-center min-h-[140px] transition-colors ${
                dragOver
                  ? "border-amber-500 bg-amber-500/5 dark:bg-amber-500/5"
                  : "border-zinc-200 dark:border-zinc-800 bg-zinc-50/30 dark:bg-zinc-950/10"
              }`}
            >
              {selectedFile ? (
                <div className="flex flex-col items-center gap-2 w-full">
                  <span className="text-xs font-semibold text-zinc-600 dark:text-zinc-400 break-all px-2 text-center">
                    {selectedFile.name} (
                    {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB)
                  </span>
                  <button
                    type="button"
                    onClick={handleRemoveFile}
                    className="text-xs text-red-500 hover:underline"
                  >
                    {t.playground.removeVideo}
                  </button>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-2">
                  <Upload className="h-8 w-8 text-zinc-400 animate-pulse" />
                  <p className="text-xs text-zinc-500 text-center">
                    {t.playground.dragAndDropVideoOr}
                    <label className="text-amber-500 cursor-pointer hover:underline">
                      {t.playground.browse}
                      <input
                        ref={inputRef}
                        type="file"
                        onChange={handleInputChange}
                        accept={ACCEPTED_TYPES.join(",")}
                        className="hidden"
                      />
                    </label>
                  </p>
                  <p className="text-[10px] text-zinc-400 text-center mt-1 font-medium">
                    {t.playground.supportedFormatsVideo}
                    {MAX_FILE_SIZE_MB}MB)
                  </p>
                </div>
              )}
            </section>
            <div className="flex flex-col gap-1.5">
              <span className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">
                {t.playground.model || "Model"}
              </span>
              <select
                id="video-model-select"
                value={selectedModel}
                onChange={(e) => setSelectedModel(e.target.value)}
                className="bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-lg p-2.5 text-sm text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all duration-200"
              >
                <option value="whisper-base">whisper-base</option>
                <option value="whisper-tiny-en">whisper-tiny-en</option>
              </select>
            </div>
            <div className="text-xs text-zinc-500 dark:text-zinc-400 bg-zinc-50 dark:bg-zinc-900/50 p-3 rounded-lg border border-zinc-200 dark:border-zinc-800/80 leading-relaxed">
              <p className="font-semibold text-zinc-700 dark:text-zinc-300">
                {t.playground.videoAnalysisDesc}
              </p>
            </div>
          </div>
        )}

        {displayPending && (
          <div className="flex flex-col items-center gap-3 py-8">
            <Loader2 className="h-8 w-8 animate-spin text-amber-500" />
            <p className="text-sm font-medium text-zinc-600 dark:text-zinc-300">
              {state === "uploading"
                ? t.playground.uploadingVideo
                : t.playground.processingVideo}
            </p>
          </div>
        )}

        {state === "success" && result && (
          <div className="flex flex-col gap-3">
            <div className="text-xs text-zinc-500 dark:text-zinc-400 bg-zinc-50 dark:bg-zinc-900/50 p-3 rounded-lg border border-zinc-200 dark:border-zinc-800/80 leading-relaxed">
              <p className="font-semibold text-zinc-700 dark:text-zinc-300">
                {t.playground.videoAnalysisDesc}
              </p>
            </div>
          </div>
        )}

        {state === "error" && (
          <div className="flex flex-col items-center gap-3 py-6">
            <AlertCircle className="h-6 w-6 text-red-500" />
            <p className="text-sm text-red-600 dark:text-red-400 text-center font-mono text-xs">
              {hookError}
            </p>
          </div>
        )}
      </div>

      {displayPending && (
        <div className="w-full bg-zinc-100 dark:bg-zinc-800 rounded-full h-2 mt-1 overflow-hidden">
          <div
            className="bg-amber-500 h-full transition-all duration-300 rounded-full"
            // eslint-disable-next-line no-restricted-syntax
            style={{
              width: `${activeProgress ?? 0}%`,
            }}
          />
        </div>
      )}

      {state === "idle" && (
        <Button
          onClick={handleExecute}
          disabled={!selectedFile}
          className="w-full flex items-center justify-center gap-2 mt-2"
        >
          <Play className="h-4 w-4" />
          {t.playground.executeIngestion}
        </Button>
      )}

      {displayPending && (
        <Button
          disabled
          className="w-full flex items-center justify-center gap-2 mt-2"
        >
          {state === "processing" && activeProgress !== undefined ? (
            <span className="font-semibold text-amber-500 animate-pulse">
              {t.playground.processingProgress.replace(
                "{percent}",
                activeProgress.toString(),
              )}
            </span>
          ) : (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              {state === "uploading"
                ? t.playground.uploadingVideo
                : t.playground.runningIngestion}
            </>
          )}
        </Button>
      )}

      {(state === "success" || state === "error") && (
        <Button
          onClick={handleReset}
          className="w-full flex items-center justify-center gap-2 mt-2"
        >
          {t.playground.analyzeAnother}
        </Button>
      )}

      {state === "success" && result && (
        <div className="p-4 rounded-xl border text-sm mt-3 bg-zinc-50/50 dark:bg-zinc-950/20 border-zinc-200 dark:border-zinc-800">
          <div className="flex items-center gap-1.5 font-semibold mb-2">
            <CheckCircle className="h-4.5 w-4.5 text-green-500" />
            {t.playground.outputResult}
          </div>
          <ResultDisplay payload={result} />
        </div>
      )}

      <ExecutionTimeline
        progress={activeProgress}
        modelName={selectedModel}
        isPending={displayPending}
      />

      {state === "error" && (
        <div className="p-4 rounded-xl border text-sm mt-3 bg-red-50 dark:bg-red-950/10 border-red-200 dark:border-red-900/30 text-red-600 dark:text-red-400">
          <div className="flex items-center gap-1.5 font-semibold mb-2">
            <AlertCircle className="h-4.5 w-4.5 text-red-500" />
            {t.playground.gatewayRestriction}
          </div>
          <p className="whitespace-pre-line leading-relaxed font-mono text-xs text-zinc-700 dark:text-zinc-300">
            {hookError}
          </p>
        </div>
      )}
    </Card>
  );
}
