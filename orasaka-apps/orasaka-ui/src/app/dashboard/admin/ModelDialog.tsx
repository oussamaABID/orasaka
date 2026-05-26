"use client";

import * as React from "react";
import { X, RefreshCw, AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useTranslation } from "@/core/context/LocaleContext";
import { MODEL_CATEGORY } from "@/core/constants/capability.constants";
import { CatalogModel } from "./CategoryCard";
import { CATEGORY_META, Tooltip, CategorySelector } from "./ModelDialogParts";

interface ModelDialogProps {
  isOpen: boolean;
  onClose: () => void;
  formModel: CatalogModel;
  setFormModel: React.Dispatch<React.SetStateAction<CatalogModel>>;
  formMode: "create" | "edit";
  saving: boolean;
  onSubmit: (e: React.SubmitEvent<HTMLFormElement>) => void;
  errorMessage: string | null;
  setErrorMessage: (msg: string | null) => void;
  providers: string[];
}

export function ModelDialog({
  isOpen,
  onClose,
  formModel,
  setFormModel,
  formMode,
  saving,
  onSubmit,
  errorMessage,
  setErrorMessage,
  providers,
}: Readonly<ModelDialogProps>) {
  const { t } = useTranslation();

  if (!isOpen) return null;

  const categories = [
    { value: MODEL_CATEGORY.SPEECH, label: t.admin.optSpeech },
    { value: MODEL_CATEGORY.IMAGE, label: t.admin.optImage },
    { value: MODEL_CATEGORY.VIDEO, label: t.admin.optVideo },
    { value: MODEL_CATEGORY.VISION, label: t.admin.optVision },
    { value: MODEL_CATEGORY.AUDIO, label: t.admin.optAudio },
    { value: "theme", label: t.admin.optTheme },
    { value: "code", label: t.admin.optCode },
  ];

  const catMeta = CATEGORY_META[formModel.category] || CATEGORY_META.code;
  const labelClass =
    "text-xs font-semibold text-[var(--text-muted)] uppercase tracking-wider flex items-center gap-1.5";
  const inputClass =
    "bg-[var(--surface-2)] border-[var(--border-default)] text-[var(--text-primary)] text-sm rounded-xl focus:ring-2 focus:ring-[var(--accent)] transition-all duration-200";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
      <button
        type="button"
        className="fixed inset-0 bg-transparent border-none cursor-default"
        aria-label="Close dialog"
        onClick={() => !saving && onClose()}
      />

      <div className="relative w-full max-w-2xl bg-[var(--surface-1)] border border-[var(--border-subtle)] shadow-2xl rounded-2xl flex flex-col animate-in zoom-in-95 slide-in-from-bottom-3 duration-300 z-10 max-h-[90vh] overflow-hidden">
        {/* Header with gradient accent */}
        <header className="flex items-center justify-between p-5 border-b border-[var(--border-subtle)] bg-[var(--surface-0)]">
          <div className="flex items-center gap-3">
            <span
              className={`inline-flex items-center justify-center w-9 h-9 rounded-xl text-lg border ${catMeta.color} transition-all duration-300`}
            >
              {catMeta.icon}
            </span>
            <div>
              <h3 className="text-base font-semibold text-[var(--text-primary)]">
                {formMode === "edit"
                  ? t.admin.editModelTitle
                  : t.admin.addModelTitle}
              </h3>
              <p className="text-xs text-[var(--text-muted)]">
                {formModel.category
                  ? categories.find((c) => c.value === formModel.category)
                      ?.label
                  : ""}
              </p>
            </div>
          </div>
          <button
            disabled={saving}
            onClick={onClose}
            className="p-2 text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)] rounded-xl transition-all duration-150"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        {/* Error banner */}
        {errorMessage && (
          <div className="mx-5 mt-4 p-3 bg-red-500/5 border border-red-500/20 rounded-xl flex items-start gap-2.5 text-red-600 dark:text-red-400 animate-in slide-in-from-top-2 duration-200">
            <AlertTriangle className="h-4 w-4 flex-shrink-0 mt-0.5" />
            <div className="text-xs font-medium flex-1">{errorMessage}</div>
            <button
              onClick={() => setErrorMessage(null)}
              className="text-red-500 hover:text-red-600"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </div>
        )}

        {/* Form content */}
        <form
          id="model-dialog-form"
          onSubmit={onSubmit}
          className="flex-1 overflow-y-auto p-5 space-y-5 scrollbar-thin"
        >
          {/* Category selector — radio card grid */}
          <div className="space-y-2">
            <label className={labelClass}>
              {t.admin.categoryLabel}
              <Tooltip text={t.admin.helpCategory} />
            </label>
            <CategorySelector
              categories={categories}
              selectedCategory={formModel.category}
              onSelect={(value) =>
                setFormModel({ ...formModel, category: value })
              }
              disabled={saving}
            />
          </div>

          {/* Provider + Model Name — 2-column */}
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <label className={labelClass}>
                {t.admin.providerLabel}
                <Tooltip text={t.admin.helpProvider} />
              </label>
              <select
                value={formModel.providerName || "ollama"}
                disabled={saving}
                onChange={(e) =>
                  setFormModel({ ...formModel, providerName: e.target.value })
                }
                className={`${inputClass} w-full h-10 px-3 py-2`}
              >
                {providers.map((prov) => (
                  <option key={prov} value={prov}>
                    {prov}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1.5">
              <label className={labelClass}>
                {t.admin.modelNameLabel}
                <Tooltip text={t.admin.helpModelName} />
              </label>
              <Input
                value={formModel.modelName}
                disabled={saving}
                onChange={(e) =>
                  setFormModel({ ...formModel, modelName: e.target.value })
                }
                placeholder={t.admin.placeholderModelName}
                className={inputClass}
              />
            </div>
          </div>

          {/* Model Label + Options — 2-column */}
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <label className={labelClass}>
                {t.admin.modelLabelLabel}
                <Tooltip text={t.admin.helpModelLabel} />
              </label>
              <Input
                value={formModel.modelLabel}
                disabled={saving}
                onChange={(e) =>
                  setFormModel({ ...formModel, modelLabel: e.target.value })
                }
                placeholder={t.admin.placeholderModelLabel}
                className={inputClass}
              />
            </div>

            <div className="space-y-1.5">
              <label className={labelClass}>
                {t.admin.optionsLabel}
                <Tooltip text={t.admin.helpOptions} />
              </label>
              <Input
                value={formModel.options}
                disabled={saving}
                onChange={(e) =>
                  setFormModel({ ...formModel, options: e.target.value })
                }
                placeholder={t.admin.placeholderOptions}
                className={inputClass}
              />
            </div>
          </div>

          {/* Default checkbox */}
          <div
            className="flex items-center gap-3 bg-[var(--surface-2)] p-3.5 rounded-xl border border-[var(--border-subtle)] cursor-pointer hover:border-[var(--border-default)] transition-all duration-200"
          >
            <input
              type="checkbox"
              id="isDefaultCheckbox"
              checked={formModel.isDefault || false}
              disabled={saving}
              onChange={(e) =>
                setFormModel({ ...formModel, isDefault: e.target.checked })
              }
              className="h-4 w-4 rounded border-[var(--border-default)] text-[var(--accent)] focus:ring-[var(--accent)] cursor-pointer"
            />
            <label
              htmlFor="isDefaultCheckbox"
              className="flex flex-col gap-0.5 select-none cursor-pointer"
            >
              <span className="text-xs font-semibold text-[var(--text-primary)]">
                {t.admin.setAsDefaultLabel}
              </span>
              <span className="text-[10px] text-[var(--text-muted)]">
                {t.admin.helpDefault}
              </span>
            </label>
          </div>
        </form>

        {/* Footer */}
        <footer className="flex items-center justify-between p-5 border-t border-[var(--border-subtle)] bg-[var(--surface-0)]">
          <span className="text-[10px] text-[var(--text-muted)] select-none hidden sm:inline">
            Esc
          </span>
          <div className="flex items-center gap-3 ml-auto">
            <Button
              type="button"
              variant="outline"
              disabled={saving}
              onClick={onClose}
              className="rounded-xl border-[var(--border-default)] text-sm"
            >
              {t.admin.cancel}
            </Button>
            <Button
              type="submit"
              form="model-dialog-form"
              disabled={saving}
              className="rounded-xl bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white font-semibold text-sm flex items-center justify-center gap-1.5 px-5"
            >
              {saving && <RefreshCw className="h-3.5 w-3.5 animate-spin" />}
              <span>
                {formMode === "edit"
                  ? t.admin.saveChanges
                  : t.admin.createModel}
              </span>
            </Button>
          </div>
        </footer>
      </div>
    </div>
  );
}
