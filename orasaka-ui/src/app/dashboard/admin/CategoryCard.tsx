"use client";

import * as React from "react";
import { Edit2, Trash2, CheckCircle2 } from "lucide-react";
import { Card } from "@/components/ui/Card";
import { useTranslation } from "@/core/context/LocaleContext";
import { MODEL_CATEGORY } from "@/core/constants/capability.constants";

export interface CatalogModel {
  id?: number;
  modelName: string;
  modelLabel: string;
  category: string;
  options?: string;
  isDefault?: boolean;
  providerName?: string;
}

interface CategoryCardProps {
  category: string;
  models: CatalogModel[];
  onEdit: (model: CatalogModel) => void;
  onDelete: (id: number) => void;
}

export function CategoryCard({
  category,
  models,
  onEdit,
  onDelete,
}: CategoryCardProps) {
  const { t } = useTranslation();

  const getCategoryLabel = React.useCallback(
    (cat: string) => {
      switch (cat) {
        case MODEL_CATEGORY.SPEECH:
          return t.admin.optSpeech;
        case MODEL_CATEGORY.IMAGE:
          return t.admin.optImage;
        case MODEL_CATEGORY.VIDEO:
          return t.admin.optVideo;
        case MODEL_CATEGORY.VISION:
          return t.admin.optVision;
        case MODEL_CATEGORY.AUDIO:
          return t.admin.optAudio;
        case "theme":
          return t.admin.optTheme;
        case "code":
          return t.admin.optCode;
        default:
          return cat;
      }
    },
    [t.admin],
  );

  return (
    <Card className="p-6 bg-card-bg/70 border-card-border backdrop-blur-lg flex flex-col gap-4 shadow-sm hover:shadow-md transition-shadow duration-200">
      <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-100 border-b pb-2 border-zinc-100 dark:border-zinc-800 capitalize flex items-center justify-between">
        <span>
          {getCategoryLabel(category)} {t.admin.modelsSuffix}
        </span>
        <span className="text-xs bg-zinc-100 dark:bg-zinc-800 text-zinc-500 dark:text-zinc-400 px-2.5 py-0.5 rounded-full font-bold">
          {models.length}
        </span>
      </h3>
      {models.length === 0 ? (
        <p className="text-sm text-zinc-400 py-8 text-center italic">
          {t.admin.noModels}
        </p>
      ) : (
        <ul className="divide-y divide-zinc-100 dark:divide-zinc-800">
          {models.map((model) => (
            <li
              key={model.id}
              className="py-3 flex items-center justify-between group first:pt-0 last:pb-0"
            >
              <article className="space-y-1 pr-4 flex-1 min-w-0">
                <header className="flex items-center gap-2">
                  <h4 className="text-sm font-bold text-zinc-800 dark:text-zinc-200 truncate">
                    {model.modelLabel}
                  </h4>
                  {model.isDefault && (
                    <span className="text-[10px] bg-amber-500/10 dark:bg-amber-500/20 text-amber-600 dark:text-amber-400 font-bold px-2 py-0.5 rounded-full flex items-center gap-1 shrink-0">
                      <CheckCircle2 className="h-2.5 w-2.5" />
                      {t.admin.activeDefault}
                    </span>
                  )}
                </header>
                <p className="text-xs text-zinc-500 font-mono truncate">
                  {model.modelName}
                </p>
                {model.options && (
                  <footer className="text-[10px] text-amber-600 dark:text-amber-400 font-semibold bg-amber-500/5 px-1.5 py-0.5 rounded inline-block">
                    {t.admin.optionsPrefix}
                    {model.options}
                  </footer>
                )}
              </article>
              <section className="flex items-center gap-1.5 shrink-0 opacity-80 group-hover:opacity-100 transition-opacity">
                <button
                  onClick={() => onEdit(model)}
                  className="p-2 rounded-xl text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 hover:bg-zinc-50/50 dark:hover:bg-zinc-800 transition-colors"
                  title={t.admin.editModelTitle}
                >
                  <Edit2 className="h-4 w-4" />
                </button>
                <button
                  onClick={() => model.id && onDelete(model.id)}
                  className="p-2 rounded-xl text-zinc-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/20 transition-colors"
                  title={t.admin.deleteModelTitle}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </section>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
