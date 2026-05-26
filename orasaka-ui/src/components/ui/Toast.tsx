"use client";

import * as React from "react";
import { X, CheckCircle2, AlertTriangle, Info, XCircle } from "lucide-react";

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

const ICONS: Record<ToastVariant, React.ElementType> = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

const VARIANT_STYLES: Record<ToastVariant, string> = {
  success:
    "border-emerald-500/20 bg-emerald-500/5 text-emerald-700 dark:text-emerald-400",
  error: "border-red-500/20 bg-red-500/5 text-red-700 dark:text-red-400",
  warning:
    "border-amber-500/20 bg-amber-500/5 text-amber-700 dark:text-amber-400",
  info: "border-[var(--accent)]/20 bg-[var(--accent-soft)] text-[var(--accent)]",
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
  const Icon = ICONS[toast.variant];

  return (
    <div
      role="alert"
      className={`flex items-start gap-3 rounded-[var(--radius-md)] border px-4 py-3 shadow-[var(--shadow-md)] backdrop-blur-sm ${
        toast.exiting ? "toast-exit" : "toast-enter"
      } ${VARIANT_STYLES[toast.variant]}`}
    >
      <Icon className="h-4 w-4 mt-0.5 flex-shrink-0" />
      <p className="flex-1 text-[var(--text-sm)] font-medium leading-relaxed">
        {toast.message}
      </p>
      <button
        onClick={() => onDismiss(toast.id)}
        className="p-0.5 rounded hover:bg-black/5 dark:hover:bg-white/5 transition-colors duration-150"
        aria-label="Dismiss notification"
      >
        <X className="h-3.5 w-3.5" />
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
