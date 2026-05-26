"use client";

import React from "react";
import { Icon, type IconName } from "@/components/ui/icon";

export interface Toast {
  id: string;
  message: string;
  type: "info" | "success" | "error";
}

interface ToastOverlayProps {
  toasts: Toast[];
  onRemoveToast: (id: string) => void;
}

const ICONS: Record<"info" | "success" | "error", IconName> = {
  success: "checkCircle",
  error: "error",
  info: "info",
};

const ICON_COLORS: Record<"info" | "success" | "error", string> = {
  success: "text-emerald-500 dark:text-emerald-400",
  error: "text-rose-500 dark:text-rose-400",
  info: "text-[var(--accent)]",
};

const BORDER_COLORS: Record<"info" | "success" | "error", string> = {
  success: "border-emerald-500/20 dark:border-emerald-500/15 shadow-emerald-950/[0.05]",
  error: "border-red-500/20 dark:border-red-500/15 shadow-red-950/[0.05]",
  info: "border-[var(--accent)]/20 shadow-[var(--accent)]/[0.05]",
};

/**
 * ToastOverlay component that renders micro-animated notifications
 * for background task progression.
 */
export function ToastOverlay({ toasts, onRemoveToast }: Readonly<ToastOverlayProps>) {
  return (
    <section className="fixed bottom-4 right-4 z-50 flex flex-col space-y-2.5 max-w-sm w-full pointer-events-none">
      {toasts.map((toast) => {
        const iconName = ICONS[toast.type];
        return (
          <div
            key={toast.id}
            role="alert"
            className={`flex items-start gap-3.5 rounded-xl border px-4 py-3.5 shadow-[0_8px_30px_rgba(0,0,0,0.25)] backdrop-blur-md transition-all duration-300 pointer-events-auto bg-[color-mix(in_srgb,var(--surface-1)_92%,transparent)] dark:bg-[color-mix(in_srgb,var(--surface-1)_88%,transparent)] text-[var(--text-primary)] animate-in fade-in slide-in-from-bottom-4 duration-300 w-full ${
              BORDER_COLORS[toast.type]
            }`}
          >
            <div className={`p-1 rounded-lg bg-black/5 dark:bg-white/5 flex-shrink-0 mt-0.5 ${ICON_COLORS[toast.type]}`}>
              <Icon name={iconName} size={15} />
            </div>
            <p className="flex-1 text-[var(--text-sm)] font-medium leading-relaxed pr-1 mt-0.5">
              {toast.message}
            </p>
            <button
              onClick={() => onRemoveToast(toast.id)}
              className="p-1 rounded-lg text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-black/5 dark:hover:bg-white/5 transition-all duration-150 flex-shrink-0 mt-0.5"
              aria-label="Dismiss notification"
            >
              <Icon name="close" size={14} />
            </button>
          </div>
        );
      })}
    </section>
  );
}
