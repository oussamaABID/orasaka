"use client";

import * as React from "react";
import {
  ToastContainer,
  type ToastItem,
  type ToastVariant,
} from "@/components/ui/Toast";

const AUTO_DISMISS_MS = 4000;
const EXIT_ANIMATION_MS = 200;

interface ToastContextValue {
  addToast: (message: string, variant?: ToastVariant) => void;
}

const ToastContext = React.createContext<ToastContextValue | null>(null);

/**
 * Hook to access the toast notification system.
 *
 * @returns The toast context value with `addToast` method.
 * @throws If used outside of a ToastProvider.
 * @example
 * ```tsx
 * const { addToast } = useToast();
 * addToast("Chat created!", "success");
 * ```
 */
export function useToast(): ToastContextValue {
  const ctx = React.useContext(ToastContext);
  if (!ctx) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return ctx;
}

/**
 * Toast provider component that manages notification state
 * and renders the ToastContainer overlay.
 *
 * @param props - Provider properties.
 * @param props.children - Child React nodes to wrap.
 * @returns The provider element with toast container.
 */
export function ToastProvider({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const [toasts, setToasts] = React.useState<ToastItem[]>([]);

  const removeToast = React.useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const dismiss = React.useCallback((id: string) => {
    setToasts((prev) =>
      prev.map((t) => (t.id === id ? { ...t, exiting: true } : t)),
    );
    setTimeout(() => removeToast(id), EXIT_ANIMATION_MS);
  }, [removeToast]);

  const addToast = React.useCallback(
    (message: string, variant: ToastVariant = "info") => {
      const id = `toast-${crypto.randomUUID()}`;
      setToasts((prev) => [...prev, { id, message, variant }]);
      setTimeout(() => dismiss(id), AUTO_DISMISS_MS);
    },
    [dismiss],
  );

  const value = React.useMemo(() => ({ addToast }), [addToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}
