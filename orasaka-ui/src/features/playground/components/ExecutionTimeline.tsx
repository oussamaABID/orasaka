import React from "react";
import { CheckCircle2, Loader2, Circle } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";

interface ExecutionTimelineProps {
  progress: number | undefined;
  modelName: string;
  isPending: boolean;
}

/**
 * Vertical timeline showing 4-step execution progress for AI inference.
 * Calm Obsidian 2026 — solid surfaces, no backdrop-blur.
 */
export function ExecutionTimeline({
  progress = 0,
  modelName,
  isPending,
}: ExecutionTimelineProps) {
  const { t } = useTranslation();

  if (!isPending && progress === 0) return null;

  // Dynamically compile steps from translations context mapping to live progress
  const steps = [1, 2, 3, 4].map((id) => {
    const titleKey = `step${id}Title` as keyof typeof t.executionTimeline;
    const descKey = `step${id}Desc` as keyof typeof t.executionTimeline;
    const title = t.executionTimeline[titleKey] as string;
    let desc = t.executionTimeline[descKey] as string;
    if (id === 2) {
      desc = desc.replace("{modelName}", modelName || "default");
    }
    return {
      id,
      title,
      desc,
      minProgress: (id - 1) * 25,
      completeProgress: id * 25,
    };
  });

  return (
    <div className="mt-4 p-4 rounded-xl border border-[var(--border-subtle)] bg-[var(--surface-1)]">
      <h4 className="text-xs font-bold text-[var(--text-muted)] uppercase tracking-wider mb-3">
        {t.executionTimeline.title}
      </h4>
      <div className="relative border-l border-[var(--border-subtle)] ml-2.5 pl-5 space-y-4">
        {steps.map((step) => {
          let status: "pending" | "active" | "completed" = "pending";

          if (progress >= step.completeProgress) {
            status = "completed";
          } else if (progress >= step.minProgress && isPending) {
            status = "active";
          }

          return (
            <div
              key={step.id}
              className="relative group transition-all duration-200"
            >
              {/* Timeline Icon indicator */}
              <div className="absolute -left-[31px] top-0.5 bg-[var(--surface-1)] rounded-full p-0.5">
                {status === "completed" ? (
                  <CheckCircle2 className="h-4.5 w-4.5 text-emerald-500" />
                ) : status === "active" ? (
                  <Loader2 className="h-4.5 w-4.5 text-amber-500 animate-spin" />
                ) : (
                  <Circle className="h-4.5 w-4.5 text-[var(--text-muted)]" />
                )}
              </div>

              {/* Text content */}
              <div className="flex flex-col gap-0.5">
                <span
                  className={`text-xs font-semibold ${
                    status === "completed"
                      ? "text-[var(--text-muted)] line-through decoration-[var(--border-subtle)]"
                      : status === "active"
                        ? "text-[var(--text-primary)] font-bold"
                        : "text-[var(--text-muted)]"
                  }`}
                >
                  {step.title}
                </span>
                <span
                  className={`text-[10px] ${
                    status === "active"
                      ? "text-amber-500 font-medium"
                      : "text-[var(--text-muted)]"
                  }`}
                >
                  {step.desc}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
