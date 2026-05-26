"use client";

import * as React from "react";
import { Icon, type IconName } from "@/components/ui/icon";

/**
 * Supported toast notification types.
 */
export type ToastVariant = "success" | "error" | "warning" | "info";

/**
 * A single toast notification entry.
 */
export interface ToastItem {
  id: string;
  message: string;
  variant: ToastVariant;
  exiting?: boolean;
}

const ICONS: Record<ToastVariant, IconName> = {
  success: "checkCircle",
  error: "error",
  warning: "warning",
  info: "info",
};

const ICON_COLORS: Record<ToastVariant, string> = {
  success: "text-emerald-500 dark:text-emerald-400",
  error: "text-rose-500 dark:text-rose-400",
  warning: "text-amber-500 dark:text-amber-400",
  info: "text-[var(--accent)]",
};

const BORDER_COLORS: Record<ToastVariant, string> = {
  success: "border-emerald-500/20 dark:border-emerald-500/15 shadow-emerald-950/[0.05]",
  error: "border-red-500/20 dark:border-red-500/15 shadow-red-950/[0.05]",
  warning: "border-amber-500/20 dark:border-amber-500/15 shadow-amber-950/[0.05]",
  info: "border-[var(--accent)]/20 shadow-[var(--accent)]/[0.05]",
};

/**
 * Renders a single toast notification with auto-dismiss and exit animation.
 *
 * @param props - The toast properties.
 * @param props.toast - The toast item data.
 * @param props.onDismiss - Callback to dismiss the toast.
 * @returns A styled toast notification element.
 */
function ToastEntry({
  toast,
  onDismiss,
}: Readonly<{
  toast: ToastItem;
  onDismiss: (id: string) => void;
}>) {
  const iconName = ICONS[toast.variant];

  return (
    <div
      role="alert"
      className={`flex items-start gap-3.5 rounded-xl border px-4 py-3 shadow-[0_8px_30px_rgba(0,0,0,0.25)] backdrop-blur-md transition-all duration-300 pointer-events-auto bg-[color-mix(in_srgb,var(--surface-1)_92%,transparent)] dark:bg-[color-mix(in_srgb,var(--surface-1)_88%,transparent)] text-[var(--text-primary)] ${
        toast.exiting ? "toast-exit" : "toast-enter"
      } ${BORDER_COLORS[toast.variant]}`}
    >
      <div className={`p-1 rounded-lg bg-black/5 dark:bg-white/5 flex-shrink-0 mt-0.5 ${ICON_COLORS[toast.variant]}`}>
        <Icon name={iconName} size={15} />
      </div>
      <p className="flex-1 text-[var(--text-sm)] font-medium leading-relaxed pr-1 mt-0.5">
        {toast.message}
      </p>
      <button
        onClick={() => onDismiss(toast.id)}
        className="p-1 rounded-lg text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-black/5 dark:hover:bg-white/5 transition-all duration-150 flex-shrink-0 mt-0.5"
        aria-label="Dismiss notification"
      >
        <Icon name="close" size={14} />
      </button>
    </div>
  );
}

/**
 * Toast container that renders all active notifications.
 * Positioned at bottom-right on desktop, bottom-center on mobile.
 *
 * @param props - Container properties.
 * @param props.toasts - List of active toast items.
 * @param props.onDismiss - Callback to dismiss a toast by ID.
 * @returns A portal-style toast container element.
 */
export function ToastContainer({
  toasts,
  onDismiss,
}: Readonly<{
  toasts: ToastItem[];
  onDismiss: (id: string) => void;
}>) {
  if (toasts.length === 0) return null;

  return (
    <div
      className="fixed bottom-4 right-4 left-4 sm:left-auto sm:w-96 z-50 flex flex-col gap-2 pointer-events-none"
      aria-live="polite"
      aria-atomic="false"
    >
      {toasts.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <ToastEntry toast={toast} onDismiss={onDismiss} />
        </div>
      ))}
    </div>
  );
}
