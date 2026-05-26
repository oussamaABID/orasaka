"use client";

/**
 * @file LocaleContext.tsx
 * @description Provides internationalization context, switching controls, and translations.
 * Supports English ('en') and French ('fr').
 *
 * Client Directive: "use client"
 * State & Syncing: Syncs language code preferences to local browser storage and settings database service.
 */

import * as React from "react";
import { useSettings } from "@/core/hooks/useSettings";
import {
  type Locale,
  type TranslationDictionary,
  translations,
} from "./translations";
export type { Locale, TranslationDictionary };

/**
 * Shape of the language translation context state.
 */
interface LocaleContextType {
  /** The current active locale. */
  locale: Locale;
  /** Callback to change active locale. */
  setLocale: (locale: Locale) => void;
  /** Translation dictionary matching the active locale. */
  t: TranslationDictionary;
}

/**
 * React Context storing current UI language preferences.
 */
const LocaleContext = React.createContext<LocaleContextType | undefined>(
  undefined,
);

/**
 * LocaleProvider component handling internationalization context wrapper.
 * Syncs locale settings between client memory, browser local storage, and gateway services.
 *
 * @param props - Component React properties.
 * @param props.children - Child nodes to render inside language context.
 * @returns The context Provider element.
 */
export function LocaleProvider({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const { settings, updateSettings } = useSettings();
  const [locale, setLocaleState] = React.useState<Locale>("en");

  // Load language preference from localStorage first
  React.useEffect(() => {
    if (globalThis.window !== undefined) {
      const stored = localStorage.getItem("orasaka_language") as Locale;
      if (stored === "en" || stored === "fr") {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setLocaleState(stored);
      }
    }
  }, []);

  // Sync with settings backend configuration if it updates
  React.useEffect(() => {
    if (settings?.language) {
      const settingsLang = settings.language as Locale;
      if (settingsLang === "en" || settingsLang === "fr") {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setLocaleState(settingsLang);
        localStorage.setItem("orasaka_language", settingsLang);
      }
    }
  }, [settings]);

  const setLocale = React.useCallback(
    (newLocale: Locale) => {
      setLocaleState(newLocale);
      if (globalThis.window !== undefined) {
        localStorage.setItem("orasaka_language", newLocale);
      }

      // If settings are available and backend language differs, update it
      if (settings && settings.language !== newLocale) {
        updateSettings({ language: newLocale });
      }
    },
    [settings, updateSettings],
  );

  const contextValue = React.useMemo(
    () => ({
      locale,
      setLocale,
      t: translations[locale],
    }),
    [locale, setLocale],
  );

  return (
    <LocaleContext.Provider value={contextValue}>
      {children}
    </LocaleContext.Provider>
  );
}

/**
 * Custom React Hook to consume translation dictionary and locale switching functionality.
 *
 * @throws {Error} If called outside of a wrapping {@link LocaleProvider}.
 * @returns The active {@link LocaleContextType} interface.
 */
export function useTranslation() {
  const context = React.useContext(LocaleContext);
  if (context === undefined) {
    throw new Error("useTranslation must be used within a LocaleProvider");
  }
  return context;
}
