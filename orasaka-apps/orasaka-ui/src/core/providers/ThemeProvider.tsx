"use client";

import * as React from "react";

export type Theme =
  | "dark"
  | "light"
  | "custom"
  | "system"
  | "cyberpunk"
  | "solarized";

export type ResolvedTheme = Exclude<Theme, "system">;

export interface ThemeContextType {
  theme: Theme;
  resolvedTheme: ResolvedTheme;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = React.createContext<ThemeContextType | undefined>(
  undefined,
);

export function ThemeProvider({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const [theme, setThemeState] = React.useState<Theme>("system");
  const [resolvedTheme, setResolvedTheme] =
    React.useState<ResolvedTheme>("light");

  const applyTheme = React.useCallback((newTheme: Theme) => {
    if (globalThis.window === undefined) return;
    const root = document.documentElement;
    root.classList.remove("dark", "light", "custom", "cyberpunk", "solarized");

    const resolved: ResolvedTheme = (() => {
      if (newTheme !== "system") return newTheme;
      return globalThis.matchMedia("(prefers-color-scheme: dark)").matches
        ? "dark"
        : "light";
    })();

    root.classList.add(resolved);
    if (resolved === "custom" || resolved === "cyberpunk") {
      root.classList.add("dark");
    } else if (resolved === "solarized") {
      root.classList.add("light");
    }

    setResolvedTheme(resolved);
    localStorage.setItem("theme", newTheme);
  }, []);

  const setTheme = React.useCallback(
    (newTheme: Theme) => {
      setThemeState(newTheme);
      applyTheme(newTheme);
    },
    [applyTheme],
  );

  // Handle system theme changes
  React.useEffect(() => {
    if (theme === "system") {
      const mediaQuery = globalThis.matchMedia("(prefers-color-scheme: dark)");
      const handleChange = () => applyTheme("system");
      mediaQuery.addEventListener("change", handleChange);
      return () => mediaQuery.removeEventListener("change", handleChange);
    }
  }, [theme, applyTheme]);

  // Synchronize on mount from localStorage
  React.useEffect(() => {
    const saved = (localStorage.getItem("theme") ?? "system") as Theme;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setThemeState(saved);
    applyTheme(saved);
  }, [applyTheme]);

  const value = React.useMemo(
    () => ({ theme, resolvedTheme, setTheme }),
    [theme, resolvedTheme, setTheme],
  );

  return (
    <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = React.useContext(ThemeContext);
  if (context === undefined) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }
  return context;
}
