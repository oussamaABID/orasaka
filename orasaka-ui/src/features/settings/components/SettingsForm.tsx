"use client";

import * as React from "react";
import { useSettings } from "../hooks/useSettings";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  CardFooter,
} from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation, Locale } from "@/core/context/LocaleContext";

/**
 * SettingsForm component provides the form interface for updating user preferences,
 * localized language settings, AI personas, and tenant branding configuration.
 *
 * @returns The SettingsForm element.
 */
export function SettingsForm() {
  const { settings, isLoading, updateSettings, isUpdating } = useSettings();
  const { accentClasses } = useTenant();
  const { t, setLocale } = useTranslation();

  const [language, setLanguage] = React.useState<Locale>("en");
  const [aiPersona, setAiPersona] = React.useState<
    "standard" | "concise" | "creative"
  >("standard");
  const [themeName, setThemeName] = React.useState("");
  const [themeTagline, setThemeTagline] = React.useState("");
  const [themeAccent, setThemeAccent] = React.useState<
    "rose" | "emerald" | "amber" | "zinc"
  >("zinc");
  const [themeLayout, setThemeLayout] = React.useState<"standard" | "compact">(
    "standard",
  );
  const [tenantId, setTenantId] = React.useState("");

  // Sync state once loaded
  React.useEffect(() => {
    if (settings) {
      Promise.resolve().then(() => {
        setLanguage(settings.language as Locale);
        setAiPersona(settings.aiPersona);
        setThemeName(settings.themeName || "Orasaka");
        setThemeTagline(settings.themeTagline || "Decoupled Intelligence");
        setThemeAccent(settings.themeAccent || "zinc");
        setThemeLayout(settings.themeLayout || "standard");
        setTenantId(settings.tenantId || "orasaka-default");
      });
    }
  }, [settings]);

  if (isLoading) {
    return (
      <div className="animate-pulse h-96 bg-zinc-100 dark:bg-zinc-900 rounded-lg" />
    );
  }

  const handleSave = () => {
    updateSettings({
      language,
      aiPersona,
      themeName,
      themeTagline,
      themeAccent,
      themeLayout,
      tenantId,
    });
  };

  return (
    <Card className="max-w-2xl border-zinc-200/80 dark:border-zinc-800/80 backdrop-blur-sm bg-white/70 dark:bg-zinc-950/70 shadow-sm animate-in fade-in duration-300">
      <CardHeader>
        <CardTitle className="text-xl font-bold text-zinc-900 dark:text-zinc-100">
          {t.settings.title}
        </CardTitle>
        <CardDescription className="text-zinc-500 dark:text-zinc-400">
          {t.settings.description}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Basic Preferences */}
        <div className="grid gap-6 sm:grid-cols-2">
          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
              {t.settings.language}
            </label>
            <select
              className="flex h-10 w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm dark:border-zinc-800 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-700"
              value={language}
              onChange={(e) => {
                const newLang = e.target.value as Locale;
                setLanguage(newLang);
                setLocale(newLang);
              }}
            >
              <option value="en">{t.settings.english}</option>
              <option value="fr">{t.settings.french}</option>
            </select>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
              {t.settings.aiPersona}
            </label>
            <select
              className="flex h-10 w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm dark:border-zinc-800 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-700"
              value={aiPersona}
              onChange={(e) =>
                setAiPersona(
                  e.target.value as "standard" | "concise" | "creative",
                )
              }
            >
              <option value="standard">{t.settings.standardPersona}</option>
              <option value="concise">{t.settings.concisePersona}</option>
              <option value="creative">{t.settings.creativePersona}</option>
            </select>
          </div>
        </div>

        <hr className="border-zinc-200 dark:border-zinc-800/80" />

        {/* Dynamic Tenant Branding Configuration */}
        <div className="space-y-4">
          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
            {t.settings.tenantBranding}
          </h3>

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
              {t.settings.appName}
            </label>
            <Input
              value={themeName}
              onChange={(e) => setThemeName(e.target.value)}
              placeholder={t.settings.appNamePlaceholder}
              className="bg-white dark:bg-zinc-950 border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
              {t.settings.tagline}
            </label>
            <Input
              value={themeTagline}
              onChange={(e) => setThemeTagline(e.target.value)}
              placeholder={t.settings.taglinePlaceholder}
              className="bg-white dark:bg-zinc-950 border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
              {t.settings.tenantId}
            </label>
            <Input
              value={tenantId}
              onChange={(e) => setTenantId(e.target.value)}
              placeholder={t.settings.tenantIdPlaceholder}
              className="bg-white dark:bg-zinc-950 border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-zinc-100"
            />
          </div>

          <div className="grid gap-6 sm:grid-cols-2">
            <div className="space-y-2">
              <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                {t.settings.colorAccent}
              </label>
              <select
                className="flex h-10 w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm dark:border-zinc-800 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-700"
                value={themeAccent}
                onChange={(e) =>
                  setThemeAccent(
                    e.target.value as "rose" | "emerald" | "amber" | "zinc",
                  )
                }
              >
                <option value="zinc">{t.settings.zincAccent}</option>
                <option value="rose">{t.settings.roseAccent}</option>
                <option value="emerald">{t.settings.emeraldAccent}</option>
                <option value="amber">{t.settings.amberAccent}</option>
              </select>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                {t.settings.layoutScale}
              </label>
              <select
                className="flex h-10 w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm dark:border-zinc-800 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-700"
                value={themeLayout}
                onChange={(e) =>
                  setThemeLayout(e.target.value as "standard" | "compact")
                }
              >
                <option value="standard">{t.settings.standardLayout}</option>
                <option value="compact">{t.settings.compactLayout}</option>
              </select>
            </div>
          </div>
        </div>
      </CardContent>
      <CardFooter className="border-t border-zinc-100 dark:border-zinc-800/80 pt-6">
        <Button
          onClick={handleSave}
          disabled={isUpdating}
          className={`${accentClasses.bg} ${accentClasses.hoverBg} text-white transition-colors px-6 py-2.5 font-medium rounded-lg shadow-sm`}
        >
          {isUpdating ? t.settings.saving : t.settings.saveTheme}
        </Button>
      </CardFooter>
    </Card>
  );
}
