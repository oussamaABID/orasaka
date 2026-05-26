"use client";

import * as React from "react";
import { getSession } from "next-auth/react";
import { useSettings } from "@/features/settings/hooks/useSettings";
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
import {
  AI_PERSONAS,
  AI_PERSONA_LABELS,
  THEME_ACCENTS,
  THEME_ACCENT_LABELS,
  THEME_LAYOUTS,
  THEME_LAYOUT_LABELS,
} from "@/constants/settings.constants";
import type {
  AiPersona,
  ThemeAccent,
  ThemeLayout,
} from "@/constants/settings.constants";
import { useTheme, type Theme } from "@/core/providers/ThemeProvider";
import { CredentialsSection } from "./CredentialsSection";
import { McpServersSection } from "./McpServersSection";
import { ThemeModeSelector } from "./ThemeModeSelector";
import { AiProvidersSection } from "./AiProvidersSection";
import { THEME_MODE } from "@/core/constants/http.constants";

const selectClass =
  "flex h-10 w-full rounded-md border border-card-border bg-card-bg px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-1 focus:ring-card-border";
const inputClass = "bg-card-bg border-card-border text-foreground";
const labelClass = "text-sm font-medium text-foreground opacity-80";

/**
 * SettingsForm component provides the form interface for updating user preferences,
 * localized language settings, AI personas, and tenant branding configuration.
 *
 * All option sets are driven by the canonical `as const` arrays in
 * `settings.constants.ts`, eliminating inline type assertions and
 * guaranteeing compile-time exhaustiveness.
 *
 * @returns The SettingsForm element.
 */
export function SettingsForm() {
  const { settings, isLoading, updateSettings, isUpdating } = useSettings();
  const { accentClasses } = useTenant();
  const { t, setLocale } = useTranslation();

  const [language, setLanguage] = React.useState<Locale>("en");
  const [aiPersona, setAiPersona] = React.useState<AiPersona>("standard");
  const [themeName, setThemeName] = React.useState("");
  const [themeTagline, setThemeTagline] = React.useState("");
  const [themeAccent, setThemeAccent] = React.useState<ThemeAccent>("zinc");
  const [themeLayout, setThemeLayout] = React.useState<ThemeLayout>("standard");
  const [theme, setTheme] = React.useState<Theme>(THEME_MODE.SYSTEM);
  const [tenantId, setTenantId] = React.useState("");
  const [availableThemes, setAvailableThemes] = React.useState<
    { value: string; label: string }[]
  >([]);

  const { setTheme: applyTheme } = useTheme();

  const fetchHeaders = React.useCallback(async (): Promise<
    Record<string, string>
  > => {
    const session = await getSession();
    return {
      "Content-Type": "application/json",
      ...(session?.user?.id
        ? { Authorization: `Bearer ${session.user.id}` }
        : {}),
    };
  }, []);

  React.useEffect(() => {
    const loadThemes = async () => {
      try {
        const headers = await fetchHeaders();
        const res = await fetch("/api/v1/models/catalog", { headers });
        if (!res.ok) throw new Error("Failed to fetch model catalog");
        const themes = (await res.json()).filter(
          (m: { category: string }) => m.category === "theme",
        );
        if (themes.length > 0) {
          setAvailableThemes(
            themes.map((m: { modelName: string; modelLabel: string }) => ({
              value: m.modelName,
              label: m.modelLabel,
            })),
          );
          return;
        }
      } catch {
        // Fallback to presets if active database lookup fails
      }
      setAvailableThemes(
        THEME_ACCENTS.map((a) => ({
          value: a,
          label:
            t.settings[
              THEME_ACCENT_LABELS[a].split(".")[1] as keyof typeof t.settings
            ] || a,
        })),
      );
    };
    loadThemes();
  }, [t, t.settings, fetchHeaders]);

  React.useEffect(() => {
    if (settings) {
      setTimeout(() => {
        setLanguage(settings.language as Locale);
        setAiPersona(settings.aiPersona);
        setThemeName(settings.themeName || "Orasaka");
        setThemeTagline(settings.themeTagline || "Decoupled Intelligence");
        setThemeAccent(settings.themeAccent || "zinc");
        setThemeLayout(settings.themeLayout || "standard");
        setTheme(settings.theme || THEME_MODE.SYSTEM);
        setTenantId(settings.tenantId || "orasaka-default");
      }, 0);
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
      theme,
      tenantId,
    });
  };

  return (
    <Card className="max-w-2xl border-card-border backdrop-blur-sm bg-card-bg shadow-sm animate-in fade-in duration-300 text-foreground">
      <CardHeader>
        <CardTitle className="text-xl font-bold text-foreground">
          {t.settings.title}
        </CardTitle>
        <CardDescription className="text-foreground opacity-70">
          {t.settings.description}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid gap-6 sm:grid-cols-2">
          <div className="space-y-2">
            <label className={labelClass}>{t.settings.language}</label>
            <select
              className={selectClass}
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
            <label className={labelClass}>{t.settings.aiPersona}</label>
            <select
              className={selectClass}
              value={aiPersona}
              onChange={(e) => setAiPersona(e.target.value as AiPersona)}
            >
              {AI_PERSONAS.map((p) => (
                <option key={p} value={p}>
                  {
                    t.settings[
                      AI_PERSONA_LABELS[p].split(
                        ".",
                      )[1] as keyof typeof t.settings
                    ]
                  }
                </option>
              ))}
            </select>
          </div>
        </div>

        <hr className="border-card-border opacity-50" />

        <section className="space-y-4">
          <h3 className="text-sm font-semibold text-foreground">
            {t.settings.tenantBranding}
          </h3>
          <div className="space-y-2">
            <label className={labelClass}>{t.settings.appName}</label>
            <Input
              value={themeName}
              onChange={(e) => setThemeName(e.target.value)}
              placeholder={t.settings.appNamePlaceholder}
              className={inputClass}
            />
          </div>
          <div className="space-y-2">
            <label className={labelClass}>{t.settings.tagline}</label>
            <Input
              value={themeTagline}
              onChange={(e) => setThemeTagline(e.target.value)}
              placeholder={t.settings.taglinePlaceholder}
              className={inputClass}
            />
          </div>
          <div className="space-y-2">
            <label className={labelClass}>{t.settings.tenantId}</label>
            <Input
              value={tenantId}
              onChange={(e) => setTenantId(e.target.value)}
              placeholder={t.settings.tenantIdPlaceholder}
              className={inputClass}
            />
          </div>

          <ThemeModeSelector
            theme={theme}
            onThemeChange={(val) => {
              setTheme(val);
              applyTheme(val);
            }}
          />

          <div className="grid gap-6 sm:grid-cols-2">
            <div className="space-y-2">
              <label className={labelClass}>{t.settings.colorAccent}</label>
              <select
                className={selectClass}
                value={themeAccent}
                onChange={(e) => setThemeAccent(e.target.value as ThemeAccent)}
              >
                {availableThemes.map((th) => (
                  <option key={th.value} value={th.value}>
                    {th.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <label className={labelClass}>{t.settings.layoutScale}</label>
              <select
                className={selectClass}
                value={themeLayout}
                onChange={(e) => setThemeLayout(e.target.value as ThemeLayout)}
              >
                {THEME_LAYOUTS.map((l) => (
                  <option key={l} value={l}>
                    {
                      t.settings[
                        THEME_LAYOUT_LABELS[l].split(
                          ".",
                        )[1] as keyof typeof t.settings
                      ]
                    }
                  </option>
                ))}
              </select>
            </div>
          </div>
        </section>

        <hr className="border-card-border opacity-50" />
        <AiProvidersSection />
        <hr className="border-card-border opacity-50" />
        <CredentialsSection fetchHeaders={fetchHeaders} />
        <hr className="border-card-border opacity-50" />
        <McpServersSection fetchHeaders={fetchHeaders} />
      </CardContent>
      <CardFooter className="border-t border-card-border pt-6">
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
