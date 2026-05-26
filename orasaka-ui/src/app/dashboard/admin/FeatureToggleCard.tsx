"use client";

import React from "react";
import type { TranslationDictionary } from "@/core/context/LocaleContext";

interface Feature {
  featureKey: string;
  isEnabled: boolean;
}

interface FeatureToggleCardProps {
  features: Feature[];
  onToggle: (featureKey: string, currentStatus: boolean) => void;
  t: TranslationDictionary;
}

/**
 * Renders the Capabilities Registry Overrides card with toggle switches
 * for each database-configured feature flag.
 */
export const FeatureToggleCard: React.FC<FeatureToggleCardProps> = ({
  features,
  onToggle,
  t,
}) => (
  <div className="bg-card-bg/70 border border-card-border rounded-2xl p-6 shadow-sm flex flex-col gap-4 backdrop-blur-lg">
    <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-100 border-b pb-2 border-zinc-100 dark:border-zinc-800">
      {t.admin.featureOverridesTitle}
    </h3>
    {features.length === 0 ? (
      <p className="text-sm text-zinc-400 py-4 text-center italic">
        {t.admin.featureNoOverrides}
      </p>
    ) : (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {features.map((feat) => (
          <div
            key={feat.featureKey}
            className="flex items-center justify-between p-3.5 bg-background rounded-xl border border-card-border"
          >
            <div className="flex flex-col gap-0.5 min-w-0 pr-2">
              <span className="text-xs font-bold text-zinc-800 dark:text-zinc-200 font-mono truncate block">
                {feat.featureKey}
              </span>
              <span className="text-[10px] text-zinc-400">
                {t.admin.featureOverrideEnabled}
              </span>
            </div>
            <input
              type="checkbox"
              checked={feat.isEnabled}
              onChange={() => onToggle(feat.featureKey, feat.isEnabled)}
              className="h-4.5 w-4.5 rounded border-zinc-300 dark:border-zinc-700 text-amber-500 focus:ring-amber-500 cursor-pointer"
            />
          </div>
        ))}
      </div>
    )}
  </div>
);
