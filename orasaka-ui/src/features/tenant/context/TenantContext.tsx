"use client";

/**
 * @file TenantContext.tsx
 * @description Context provider and hook for managing and resolving dynamic tenant configuration and styling accents.
 *
 * Client Directive: "use client"
 * State & Memoization: Uses React.useMemo to compute and cache layout config based on theme properties.
 * See: {@link TenantConfig}
 */

import * as React from "react";
import { useSettings } from "@/features/settings/hooks/useSettings";
import { TenantConfig } from "@/features/tenant/types/tenant.types";

/**
 * Interface mapping available visual utility classes for Tailwind css components.
 */
export interface AccentClasses {
  text: string;
  bg: string;
  hoverBg: string;
  border: string;
  ring: string;
  bgSoft: string;
  textBright: string;
  accentGradient: string;
}

/**
 * Map containing functional CSS classes corresponding to color theme accents.
 * Prevents hardcoding of color utility lists inside core components.
 */
export const accentMap: Record<string, AccentClasses> = {
  rose: {
    text: "text-rose-600 dark:text-rose-400",
    bg: "bg-rose-600 dark:bg-rose-500",
    hoverBg: "hover:bg-rose-700 dark:hover:bg-rose-600",
    border: "border-rose-200 dark:border-rose-800",
    ring: "focus:ring-rose-500 focus-visible:ring-rose-500",
    bgSoft: "bg-rose-50 dark:bg-rose-950/20",
    textBright: "text-rose-700 dark:text-rose-300",
    accentGradient: "from-rose-500 to-pink-600",
  },
  emerald: {
    text: "text-emerald-600 dark:text-emerald-400",
    bg: "bg-emerald-600 dark:bg-emerald-500",
    hoverBg: "hover:bg-emerald-700 dark:hover:bg-emerald-600",
    border: "border-emerald-200 dark:border-emerald-800",
    ring: "focus:ring-emerald-500 focus-visible:ring-emerald-500",
    bgSoft: "bg-emerald-50 dark:bg-emerald-950/20",
    textBright: "text-emerald-700 dark:text-emerald-300",
    accentGradient: "from-emerald-500 to-teal-650",
  },
  amber: {
    text: "text-amber-600 dark:text-amber-400",
    bg: "bg-amber-600 dark:bg-amber-500",
    hoverBg: "hover:bg-amber-700 dark:hover:bg-amber-600",
    border: "border-amber-200 dark:border-amber-800",
    ring: "focus:ring-amber-500 focus-visible:ring-amber-500",
    bgSoft: "bg-amber-50 dark:bg-amber-950/20",
    textBright: "text-amber-700 dark:text-amber-300",
    accentGradient: "from-amber-500 to-orange-600",
  },
  zinc: {
    text: "text-zinc-600 dark:text-zinc-400",
    bg: "bg-zinc-600 dark:bg-zinc-500",
    hoverBg: "hover:bg-zinc-700 dark:hover:bg-zinc-600",
    border: "border-zinc-200 dark:border-zinc-800",
    ring: "focus:ring-zinc-500 focus-visible:ring-zinc-500",
    bgSoft: "bg-zinc-50 dark:bg-zinc-950/20",
    textBright: "text-zinc-700 dark:text-zinc-300",
    accentGradient:
      "from-zinc-500 to-zinc-700 dark:from-zinc-400 dark:to-zinc-600",
  },
};

/**
 * Shape of the Tenant context state object.
 */
interface TenantContextType {
  config: TenantConfig;
  accentClasses: AccentClasses;
}

/**
 * Context container for holding active tenant metadata.
 */
const TenantContext = React.createContext<TenantContextType | undefined>(
  undefined,
);

/**
 * TenantProvider component wrapping context injection.
 * Resolves active tenant config from global state hooks and passes down theme layouts.
 *
 * @param props - Component React properties.
 * @param props.children - Child nodes to be injected inside the provider wrapper.
 * @returns The context Provider rendering element.
 */
export function TenantProvider({ children }: { children: React.ReactNode }) {
  const { settings } = useSettings();

  const themeAccent = settings?.themeAccent || "zinc";
  const themeName = settings?.themeName || "Orasaka";
  const themeTagline = settings?.themeTagline || "Decoupled Intelligence";
  const themeLayout = settings?.themeLayout || "standard";
  // Check user settings preferences or fallback to a default ID
  const tenantId = settings?.tenantId || "orasaka-default";

  const contextValue = React.useMemo<TenantContextType>(() => {
    const accentClass = accentMap[themeAccent] ? themeAccent : "zinc";
    const accentClasses = accentMap[accentClass];

    return {
      config: {
        accentClass,
        displayName: themeName,
        tagline: themeTagline,
        tenantId,
        layoutMode: themeLayout,
      },
      accentClasses,
    };
  }, [themeAccent, themeName, themeTagline, themeLayout, tenantId]);

  return (
    <TenantContext.Provider value={contextValue}>
      {children}
    </TenantContext.Provider>
  );
}

/**
 * Custom React Hook to consume the Tenant context.
 *
 * @throws {Error} If called outside of a wrapping {@link TenantProvider}.
 * @returns The active {@link TenantContextType} state configuration.
 */
export function useTenant() {
  const context = React.useContext(TenantContext);
  if (context === undefined) {
    throw new Error("useTenant must be used within a TenantProvider");
  }
  return context;
}
