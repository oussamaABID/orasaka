"use client";

import * as React from "react";
import type { Theme } from "@/core/providers/ThemeProvider";

interface ThemePreviewCardProps {
  value: Theme;
  label: string;
  desc: string;
  icon: React.ReactNode;
  preview: {
    sidebar: string;
    header: string;
    body: string;
    accent: string;
    text: string;
  };
  isActive: boolean;
  onClick: () => void;
  index: number;
  clickToApplyLabel: string;
}

/**
 * Individual theme preview card with mini app layout preview,
 * animated gradient border on active state, selection pulse,
 * and hover interaction guidance.
 */
export function ThemePreviewCard({
  value,
  label,
  desc,
  icon,
  preview,
  isActive,
  onClick,
  index,
  clickToApplyLabel,
}: Readonly<ThemePreviewCardProps>) {
  const [justSelected, setJustSelected] = React.useState(false);

  const handleClick = () => {
    if (isActive) return;
    setJustSelected(true);
    onClick();
    setTimeout(() => setJustSelected(false), 350);
  };

  return (
    <button
      key={value}
      type="button"
      id={`theme-card-${value}`}
      onClick={handleClick}
      style={{ animationDelay: `${index * 50}ms` }}
      className={[
        "group relative flex flex-col rounded-xl overflow-hidden text-left",
        "transition-all duration-250 ease-out cursor-pointer",
        "focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2",
        "animation-fade-up",
        justSelected ? "theme-card-pulse" : "",
        isActive
          ? "theme-card-active border border-transparent shadow-lg"
          : "border border-card-border hover:border-zinc-400 dark:hover:border-zinc-500 hover:shadow-md opacity-70 hover:opacity-100",
      ].join(" ")}
    >
      {/* ── Mini Preview Window ─────────────────────── */}
      <figure className="relative h-[80px] overflow-hidden bg-zinc-200 dark:bg-zinc-800">
        {/* Sidebar */}
        <aside
          className={`absolute left-0 top-0 bottom-0 w-[22px] ${preview.sidebar} border-r border-black/5`}
        >
          <nav className="flex flex-col gap-1.5 pt-3 px-1">
            <span className="w-2.5 h-2.5 rounded-sm bg-white/20 block" />
            <span className="w-2.5 h-1 rounded-full bg-white/15 block" />
            <span className="w-2.5 h-1 rounded-full bg-white/10 block" />
            <span className="w-2.5 h-1 rounded-full bg-white/10 block" />
          </nav>
        </aside>
        {/* Header */}
        <header
          className={`absolute left-[22px] top-0 right-0 h-[14px] ${preview.header} border-b border-black/5`}
        >
          <nav className="flex items-center justify-between h-full px-2">
            <span
              className={`w-5 h-1 rounded-full ${preview.text} opacity-50 block`}
            />
            <span className="flex gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-white/15 block" />
              <span className="w-1.5 h-1.5 rounded-full bg-white/15 block" />
            </span>
          </nav>
        </header>
        {/* Body */}
        <main
          className={`absolute left-[22px] top-[14px] right-0 bottom-0 ${preview.body} p-2 flex flex-col gap-[5px]`}
        >
          <span
            className={`h-[5px] w-[65%] rounded-full ${preview.text} opacity-50 block`}
          />
          <span
            className={`h-[5px] w-[45%] rounded-full ${preview.text} opacity-35 block`}
          />
          <span
            className={`h-[5px] w-[55%] rounded-full ${preview.text} opacity-40 block`}
          />
          {/* Accent button */}
          <footer className="mt-auto flex gap-1.5 items-center">
            <span
              className={`h-[7px] w-[28px] rounded-sm ${preview.accent} opacity-90 block`}
            />
            <span
              className={`h-[5px] w-[18px] rounded-full ${preview.text} opacity-25 block`}
            />
          </footer>
        </main>
        {/* Active checkmark */}
        {isActive && (
          <mark className="absolute top-1 right-1 w-[18px] h-[18px] rounded-full bg-emerald-500 flex items-center justify-center shadow-md theme-check-pop">
            <svg
              className="w-2.5 h-2.5 text-white"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={3}
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M4.5 12.75l6 6 9-13.5"
              />
            </svg>
          </mark>
        )}
        {/* Hover overlay */}
        {!isActive && (
          <figcaption className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors duration-200 flex items-center justify-center">
            <span className="text-[10px] font-medium text-white opacity-0 group-hover:opacity-100 transition-opacity duration-200 bg-black/50 px-2 py-0.5 rounded-full backdrop-blur-sm">
              {clickToApplyLabel}
            </span>
          </figcaption>
        )}
      </figure>

      {/* ── Label row ──────────────────────────────── */}
      <footer className="flex items-center gap-1.5 px-3 pt-2.5 pb-0.5 bg-card-bg">
        <span className="text-foreground/60 group-hover:text-foreground/90 transition-colors">
          {icon}
        </span>
        <span className="text-[11px] font-semibold text-foreground tracking-wide truncate">
          {label}
        </span>
      </footer>
      <span className="text-[10px] text-foreground/40 px-3 pb-2.5 leading-tight bg-card-bg">
        {desc}
      </span>
    </button>
  );
}
