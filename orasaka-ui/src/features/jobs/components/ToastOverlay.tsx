"use client";

import React from "react";

export interface Toast {
  id: string;
  message: string;
  type: "info" | "success" | "error";
}

interface ToastOverlayProps {
  toasts: Toast[];
  onRemoveToast: (id: string) => void;
}

/**
 * ToastOverlay component that renders micro-animated notifications
 * for background task progression.
 */
export function ToastOverlay({ toasts, onRemoveToast }: Readonly<ToastOverlayProps>) {
  return (
    <section className="fixed bottom-4 right-4 z-50 flex flex-col space-y-2.5 max-w-sm w-full">
      {toasts.map((toast) => (
        <button
          key={toast.id}
          type="button"
          onClick={() => onRemoveToast(toast.id)}
          className={`flex items-start justify-between p-4 rounded-2xl border backdrop-blur-md shadow-lg transform transition-all duration-300 hover:scale-[1.02] cursor-pointer animate-in fade-in slide-in-from-bottom-4 duration-300 w-full text-left ${(() => {
            if (toast.type === "success") return "bg-emerald-50/90 dark:bg-emerald-950/95 border-emerald-200/70 dark:border-emerald-900/60 text-emerald-800 dark:text-emerald-300";
            if (toast.type === "error") return "bg-red-50/90 dark:bg-red-950/95 border-red-200/70 dark:border-red-900/60 text-red-800 dark:text-red-300";
            return "bg-zinc-900/95 dark:bg-zinc-800/95 border-zinc-700/80 text-zinc-100 dark:text-zinc-200";
          })()}`}
        >
          <p className="text-sm font-medium pr-4">{toast.message}</p>
          <span className="text-zinc-400 hover:text-zinc-200 text-xs font-bold" aria-hidden="true">
            ✕
          </span>
        </button>
      ))}
    </section>
  );
}
