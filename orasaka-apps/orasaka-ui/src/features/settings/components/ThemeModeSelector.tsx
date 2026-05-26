"use client";

import * as React from "react";
import {
  Laptop,
  Sun,
  Moon,
  Sparkles,
  Shield,
  Compass,
  Palette,
} from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";
import type { Theme } from "@/core/providers/ThemeProvider";
import { ThemePreviewCard } from "./ThemePreviewCard";

interface ThemeModeSelectorProps {
  theme: Theme;
  onThemeChange: (theme: Theme) => void;
}

/** Preview color tokens for each theme mode. */
const PREVIEWS: Record<
  Theme,
  {
    sidebar: string;
    header: string;
    body: string;
    accent: string;
    text: string;
  }
> = {
  system: {
    sidebar: "bg-zinc-200 dark:bg-zinc-800",
    header: "bg-zinc-100 dark:bg-zinc-900",
    body: "bg-white dark:bg-zinc-950",
    accent: "bg-amber-400",
    text: "bg-zinc-300 dark:bg-zinc-700",
  },
  light: {
    sidebar: "bg-zinc-100",
    header: "bg-white",
    body: "bg-zinc-50",
    accent: "bg-amber-400",
    text: "bg-zinc-200",
  },
  dark: {
    sidebar: "bg-zinc-900",
    header: "bg-zinc-950",
    body: "bg-zinc-950",
    accent: "bg-amber-500",
    text: "bg-zinc-800",
  },
  custom: {
    sidebar: "bg-indigo-950",
    header: "bg-slate-950",
    body: "bg-slate-950",
    accent: "bg-violet-500",
    text: "bg-slate-800",
  },
  cyberpunk: {
    sidebar: "bg-violet-950",
    header: "bg-black",
    body: "bg-violet-950/80",
    accent: "bg-fuchsia-500",
    text: "bg-violet-900",
  },
  solarized: {
    sidebar: "bg-amber-100",
    header: "bg-amber-50",
    body: "bg-amber-50/60",
    accent: "bg-sky-500",
    text: "bg-amber-200",
  },
};

/**
 * Premium theme mode selector with live mini-previews, animated gradient
 * borders, selection feedback, and contextual status bar.
 *
 * Interactions:
 * - Hover  → "Click to apply" overlay appears
 * - Click  → pulse animation + status bar slides in with confirmation
 * - Active → rotating conic gradient border + checkmark badge
 */
export function ThemeModeSelector({
  theme,
  onThemeChange,
}: Readonly<ThemeModeSelectorProps>) {
  const { t } = useTranslation();
  const [statusTheme, setStatusTheme] = React.useState<string | null>(null);
  const statusTimeout = React.useRef<ReturnType<typeof setTimeout> | null>(
    null,
  );

  const options: {
    value: Theme;
    label: string;
    desc: string;
    icon: React.ReactNode;
  }[] = [
    {
      value: "system",
      label: t.settings.themeSystem,
      icon: <Laptop className="w-3.5 h-3.5" />,
      desc: t.settings.themeSystemDesc,
    },
    {
      value: "light",
      label: t.settings.themeLight,
      icon: <Sun className="w-3.5 h-3.5" />,
      desc: t.settings.themeLightDesc,
    },
    {
      value: "dark",
      label: t.settings.themeDark,
      icon: <Moon className="w-3.5 h-3.5" />,
      desc: t.settings.themeDarkDesc,
    },
    {
      value: "custom",
      label: t.settings.themeCustom,
      icon: <Shield className="w-3.5 h-3.5" />,
      desc: t.settings.themeCustomDesc,
    },
    {
      value: "cyberpunk",
      label: t.settings.themeCyberpunk,
      icon: <Sparkles className="w-3.5 h-3.5" />,
      desc: t.settings.themeCyberpunkDesc,
    },
    {
      value: "solarized",
      label: t.settings.themeSolarized,
      icon: <Compass className="w-3.5 h-3.5" />,
      desc: t.settings.themeSolarizedDesc,
    },
  ];

  const handleThemeChange = (val: Theme) => {
    onThemeChange(val);
    const selected = options.find((o) => o.value === val);
    setStatusTheme(selected?.label ?? val);
    if (statusTimeout.current) clearTimeout(statusTimeout.current);
    statusTimeout.current = setTimeout(() => setStatusTheme(null), 3000);
  };

  return (
    <section className="space-y-3">
      {/* ── Section header ─────────────────────────── */}
      <header className="flex items-center gap-2">
        <figure className="w-7 h-7 rounded-lg bg-accent/10 flex items-center justify-center">
          <Palette className="w-3.5 h-3.5 text-foreground/70" />
        </figure>
        <aside>
          <label className="text-sm font-semibold text-foreground block leading-tight">
            {t.settings.themeMode.replace(" (theme)", "")}
          </label>
          <span className="text-[11px] text-foreground/40 leading-tight">
            {t.settings.themeChangesInstant}
          </span>
        </aside>
      </header>

      {/* ── Theme grid ─────────────────────────────── */}
      <div className="grid gap-2.5 grid-cols-3 stagger-children">
        {options.map((opt, i) => (
          <ThemePreviewCard
            key={opt.value}
            value={opt.value}
            label={opt.label}
            desc={opt.desc}
            icon={opt.icon}
            preview={PREVIEWS[opt.value]}
            isActive={theme === opt.value}
            onClick={() => handleThemeChange(opt.value)}
            index={i}
            clickToApplyLabel={t.settings.themeClickToApply}
          />
        ))}
      </div>

      {/* ── Status feedback bar ────────────────────── */}
      {statusTheme && (
        <div
          key={statusTheme}
          className="flex items-center gap-2 px-3 py-2 rounded-lg bg-emerald-500/8 border border-emerald-500/15 theme-status-slide"
        >
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
          </span>
          <span className="text-xs text-foreground/70">
            <strong className="text-foreground/90">{statusTheme}</strong>{" "}
            {t.settings.themeApplied}
          </span>
        </div>
      )}
    </section>
  );
}
