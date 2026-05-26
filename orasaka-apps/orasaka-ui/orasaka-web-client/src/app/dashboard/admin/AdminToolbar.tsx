"use client";

import React from "react";
import { Button } from "@/components/ui/Button";
import { Plus, Sliders, RefreshCw, X, AlertTriangle } from "lucide-react";
import type { TranslationDictionary } from "@/core/context/LocaleContext";

interface AdminToolbarProps {
  loadingModels: boolean;
  errorMessage: string | null;
  onRefresh: () => void;
  onAddModel: () => void;
  onClearError: () => void;
  t: TranslationDictionary;
}

/**
 * Top toolbar for the admin dashboard with title, refresh and add model buttons,
 * plus an inline error banner.
 */
export const AdminToolbar: React.FC<AdminToolbarProps> = ({
  loadingModels,
  errorMessage,
  onRefresh,
  onAddModel,
  onClearError,
  t,
}) => (
  <>
    <header className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <div className="space-y-1">
        <h2 className="text-3xl font-extrabold tracking-tight text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
          <Sliders className="h-8 w-8 text-amber-500" />
          {t.admin.title}
        </h2>
        <p className="text-zinc-500 dark:text-zinc-400 text-sm">
          {t.admin.subtitle}
        </p>
      </div>
      <div className="flex items-center space-x-2">
        <Button
          variant="outline"
          onClick={onRefresh}
          className="rounded-xl flex items-center space-x-2 text-sm font-semibold border-zinc-200 dark:border-zinc-800 dark:hover:bg-zinc-900 transition-colors"
        >
          <RefreshCw
            className={`w-4 h-4 ${loadingModels ? "animate-spin" : ""}`}
          />
          <span>{t.admin.refresh}</span>
        </Button>
        <Button
          onClick={onAddModel}
          className="rounded-xl flex items-center space-x-2 text-sm font-semibold bg-amber-500 hover:bg-amber-600 text-white shadow-md border-transparent transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>{t.admin.addModel}</span>
        </Button>
      </div>
    </header>

    {errorMessage && (
      <div className="p-4 bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/50 rounded-2xl flex items-start gap-3 text-red-700 dark:text-red-400">
        <AlertTriangle className="h-5 w-5 flex-shrink-0 mt-0.5" />
        <div className="text-sm font-medium flex-1">{errorMessage}</div>
        <button
          onClick={onClearError}
          className="text-red-500 hover:text-red-600"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    )}
  </>
);
