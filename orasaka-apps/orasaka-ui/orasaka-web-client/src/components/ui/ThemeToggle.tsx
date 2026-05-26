"use client";

import * as React from "react";
import { Icon } from "@/components/ui/icon";
import { useTheme } from "@/core/providers/ThemeProvider";
import { THEME_MODE } from "@/core/constants/http.constants";

/**
 * Client Component for switching the application's color theme between light and dark modes.
 *
 * <p>Uses `next-themes` to manage active theme status and implements a smooth transitions, scale on hover
 * micro-interactions, and a loading skeleton state to prevent server-side rendering mismatch.
 *
 * @returns An interactive toggle button or a skeleton loading div.
 * @see {@link useTheme}
 */
export function ThemeToggle() {
  const { setTheme, resolvedTheme } = useTheme();
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  const isDark = resolvedTheme === THEME_MODE.DARK;

  const toggle = () => setTheme(isDark ? "light" : THEME_MODE.DARK);

  if (!mounted) {
    return (
      <div
        aria-hidden="true"
        className="h-9 w-9 rounded-xl bg-zinc-100 dark:bg-zinc-800 animate-pulse"
      />
    );
  }

  return (
    <button
      id="theme-toggle"
      type="button"
      onClick={toggle}
      aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
      title={isDark ? "Switch to light mode" : "Switch to dark mode"}
      className={[
        "relative flex h-9 w-9 items-center justify-center rounded-xl",
        "border transition-all duration-300 ease-in-out",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-500",
        "hover:scale-105 active:scale-95",
        isDark
          ? "border-zinc-700 bg-zinc-800 text-amber-300 hover:bg-zinc-700 hover:border-zinc-600"
          : "border-zinc-200 bg-white text-violet-600 hover:bg-zinc-50 hover:border-zinc-300",
      ].join(" ")}
    >
      {/* Sun icon — visible in light mode */}
      <span
        className={[
          "absolute inset-0 flex items-center justify-center transition-all duration-300",
          isDark
            ? "opacity-0 rotate-90 scale-50"
            : "opacity-100 rotate-0 scale-100",
        ].join(" ")}
      >
        <Icon name="sun" size={16} />
      </span>

      {/* Moon icon — visible in dark mode */}
      <span
        className={[
          "absolute inset-0 flex items-center justify-center transition-all duration-300",
          isDark
            ? "opacity-100 rotate-0 scale-100"
            : "opacity-0 -rotate-90 scale-50",
        ].join(" ")}
      >
        <Icon name="moon" size={16} />
      </span>
    </button>
  );
}
