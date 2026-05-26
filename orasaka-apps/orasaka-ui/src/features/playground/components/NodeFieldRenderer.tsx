import React from "react";
import { Input } from "@/components/ui/Input";
import { MediaUploadField } from "./MediaUploadField";
import { NodeModelSelector } from "./NodeModelSelector";
import { useTranslation } from "@/core/context/LocaleContext";
import { NODE_ID, NODE_ICON } from "@/core/constants/capability.constants";

interface CatalogModel {
  id: number;
  modelName: string;
  modelLabel: string;
  category: string;
  providerName: string;
  options?: string;
}

interface NodeFieldRendererProps {
  nodeId: string;
  placeholders: string[];
  inputs: Record<string, string>;
  isLocked: boolean;
  displayPending: boolean;
  category: string;
  currentProvider: string;
  uniqueProviders: string[];
  modelCatalog: CatalogModel[];
  uploadingFields: Record<string, boolean>;
  onInputChange: (field: string, value: string) => void;
  onFileUpload: (field: string, file: File) => void;
  onExecute: () => void;
  nodeIcon?: string;
}

/**
 * Renders the dynamic form fields for a PlaygroundNodeCard based on
 * parsed payload template placeholders. Delegates specialized inputs
 * (model, voice, media upload) to their respective sub-components.
 */
export function NodeFieldRenderer({
  nodeId,
  placeholders,
  inputs,
  isLocked,
  displayPending,
  category,
  currentProvider,
  uniqueProviders,
  modelCatalog,
  uploadingFields,
  onInputChange,
  onFileUpload,
  onExecute,
  nodeIcon,
}: Readonly<NodeFieldRendererProps>) {
  const { t } = useTranslation();

  return (
    <>
      {placeholders.map((field) => {
        const isBase64 =
          field.toLowerCase().includes("base64") ||
          field.toLowerCase().includes("image") ||
          field.toLowerCase().includes("assetid");
        if (isBase64) {
          const isAudioNode =
            nodeId === NODE_ID.MEDIA_AUDIO || nodeIcon === NODE_ICON.MIC;
          return (
            <MediaUploadField
              key={field}
              field={field}
              inputs={inputs}
              isLocked={isLocked}
              handleInputChange={onInputChange}
              handleFileUpload={onFileUpload}
              isAudio={isAudioNode}
              isUploading={uploadingFields[field]}
            />
          );
        }

        if (field === "model") {
          return (
            <NodeModelSelector
              key={field}
              category={category}
              currentProvider={currentProvider}
              uniqueProviders={uniqueProviders}
              modelCatalog={modelCatalog}
              inputs={inputs}
              isLocked={isLocked}
              onInputChange={onInputChange}
            />
          );
        }

        if (field === "voice") {
          const selectedModelName = inputs.model || "";
          const dbModel = modelCatalog.find(
            (x) => x.modelName === selectedModelName,
          );
          const voiceOptions = dbModel?.options
            ? dbModel.options.split(",")
            : ["alloy", "echo", "fable", "onyx", "nova", "shimmer"];

          return (
            <div key={field} className="flex flex-col gap-1.5">
              <span className="text-xs font-semibold text-[var(--text-muted)] uppercase tracking-wider">
                {field}
              </span>
              <select
                value={
                  inputs[field] ||
                  (voiceOptions.length > 0 ? voiceOptions[0] : "")
                }
                disabled={isLocked}
                onChange={(e) => onInputChange(field, e.target.value)}
                className="bg-[var(--surface-1)] border border-[var(--border-default)] rounded-lg p-2.5 text-sm text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all duration-200"
              >
                {voiceOptions.map((v) => (
                  <option key={v} value={v}>
                    {v}
                  </option>
                ))}
              </select>
            </div>
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
              onChange={(e) => onInputChange(field, e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !displayPending && !isLocked) {
                  onExecute();
                }
              }}
              placeholder={t.playground.enterValue.replace("{field}", field)}
              className="bg-[var(--surface-1)] border-[var(--border-subtle)] text-sm"
            />
            {nodeId === NODE_ID.CHAT_SPEECH && field === "text" && (
              <button
                type="button"
                className="text-xs text-[var(--text-muted)] mt-1 cursor-pointer hover:text-amber-500 dark:hover:text-amber-400 transition-colors select-none text-left p-0 bg-transparent border-none"
                onClick={() =>
                  onInputChange(
                    field,
                    "Hello, this is a local speech synthesis verification test running on macOS bare-metal.",
                  )
                }
              >
                {t.playground.speechHelper}
              </button>
            )}
            {nodeId === NODE_ID.MEDIA_VIDEO && field === "prompt" && (
              <div className="flex flex-col gap-1.5 mt-1.5">
                <span className="text-[10px] font-semibold text-[var(--text-muted)] uppercase tracking-wider">
                  {t.playground.videoExamplesLabel}
                </span>
                <div className="flex flex-col gap-1.5">
                  {[
                    t.playground.videoExampleVerification,
                    t.playground.videoExampleCinematic,
                    t.playground.videoExampleDynamic,
                  ].map((example) => (
                    <button
                      key={example}
                      type="button"
                      disabled={isLocked}
                      onClick={() => onInputChange("prompt", example)}
                      className="text-left text-xs text-[var(--text-secondary)] hover:text-amber-500 dark:hover:text-amber-400 transition-colors cursor-pointer select-none font-medium"
                    >
                      • {example}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        );
      })}
    </>
  );
}
