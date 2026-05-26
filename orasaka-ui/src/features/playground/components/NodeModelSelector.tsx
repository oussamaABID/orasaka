import React from "react";
import { useTranslation } from "@/core/context/LocaleContext";
import {
  FALLBACK_MODELS,
  COMMERCIAL_DEFAULTS,
} from "@/core/constants/capability.constants";

interface ModelOption {
  value: string;
  label: string;
}

interface CatalogModel {
  id: number;
  modelName: string;
  modelLabel: string;
  category: string;
  providerName: string;
  options?: string;
}

interface NodeModelSelectorProps {
  category: string;
  currentProvider: string;
  uniqueProviders: string[];
  modelCatalog: CatalogModel[];
  inputs: Record<string, string>;
  isLocked: boolean;
  onInputChange: (field: string, value: string) => void;
}

/**
 * Renders the provider and model selector pair for a PlaygroundNodeCard.
 * Resolves commercial fallbacks and local infrastructure defaults.
 */
export function NodeModelSelector({
  category,
  currentProvider,
  uniqueProviders,
  modelCatalog,
  inputs,
  isLocked,
  onInputChange,
}: NodeModelSelectorProps) {
  const { t } = useTranslation();
  const isCommercialProvider = ["gemini", "anthropic", "openai"].includes(
    currentProvider,
  );
  const categoryModels = modelCatalog.filter((m) => m.category === category);

  const filteredModels = categoryModels.filter(
    (m) => m.providerName === currentProvider,
  );

  let displayModels: ModelOption[] = filteredModels.map((m) => ({
    value: m.modelName,
    label: m.modelLabel,
  }));

  if (isCommercialProvider && displayModels.length === 0) {
    let commercialModels: string[] = [];
    if (currentProvider === "gemini") {
      commercialModels = [
        "gemini-1.5-pro",
        "gemini-1.5-flash",
        "gemini-2.0-flash-exp",
      ];
    } else if (currentProvider === "anthropic") {
      commercialModels = [
        "claude-3-5-sonnet-latest",
        "claude-3-5-haiku-latest",
        "claude-3-opus-latest",
      ];
    } else if (currentProvider === "openai") {
      if (category === "speech") {
        commercialModels = ["tts-1", "tts-1-hd"];
      } else if (category === "image") {
        commercialModels = ["dall-e-3", "dall-e-2"];
      } else if (category === "audio") {
        commercialModels = ["whisper-1"];
      } else {
        commercialModels = ["gpt-4o", "gpt-4o-mini", "gpt-4-turbo"];
      }
    }
    displayModels = commercialModels.map((m) => ({ value: m, label: m }));
  }

  if (displayModels.length === 0) {
    const fallbackList = FALLBACK_MODELS[category] ?? [];
    displayModels = fallbackList.map((m) => ({ value: m, label: m }));
  }

  const selectedModelVal = inputs["model"] || displayModels[0]?.value || "";

  const handleProviderChange = (newProv: string) => {
    const provModels = categoryModels.filter((m) => m.providerName === newProv);
    if (provModels.length > 0) {
      onInputChange("model", provModels[0].modelName);
    } else {
      const provDefaults = COMMERCIAL_DEFAULTS[newProv];
      if (provDefaults) {
        const resolved = provDefaults[category] ?? provDefaults.default;
        onInputChange("model", resolved);
      }
    }
  };

  return (
    <div className="grid grid-cols-2 gap-3">
      <div className="flex flex-col gap-1.5">
        <span className="text-xs font-semibold text-[var(--text-muted)] uppercase tracking-wider">
          {t.playground.providerLabel}
        </span>
        <select
          value={currentProvider}
          disabled={isLocked}
          onChange={(e) => handleProviderChange(e.target.value)}
          className="bg-[var(--surface-1)] border border-[var(--border-default)] rounded-lg p-2.5 text-sm text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all duration-200"
        >
          {uniqueProviders.map((p) => (
            <option key={p} value={p}>
              {p.toUpperCase()}
            </option>
          ))}
        </select>
      </div>
      <div className="flex flex-col gap-1.5">
        <span className="text-xs font-semibold text-[var(--text-muted)] uppercase tracking-wider">
          {t.playground.modelSelectorLabel}
        </span>
        <select
          value={selectedModelVal}
          disabled={isLocked}
          onChange={(e) => onInputChange("model", e.target.value)}
          className="bg-[var(--surface-1)] border border-[var(--border-default)] rounded-lg p-2.5 text-sm text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all duration-200"
        >
          {displayModels.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
