"use client";

import * as React from "react";

export type Theme =
  | "dark"
  | "light"
  | "custom"
  | "system"
  | "cyberpunk"
  | "solarized";

export interface ThemeContextType {
  theme: Theme;
  resolvedTheme: "dark" | "light" | "custom" | "cyberpunk" | "solarized";
  setTheme: (theme: Theme) => void;
}

const ThemeContext = React.createContext<ThemeContextType | undefined>(
  undefined,
);

export function ThemeProvider({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const [theme, setThemeState] = React.useState<Theme>("system");
  const [resolvedTheme, setResolvedTheme] = React.useState<
    "dark" | "light" | "custom" | "cyberpunk" | "solarized"
  >("light");

  const applyTheme = React.useCallback((newTheme: Theme) => {
    if (typeof window === "undefined") return;
    const root = document.documentElement;
    root.classList.remove("dark", "light", "custom", "cyberpunk", "solarized");

    let resolved: "dark" | "light" | "custom" | "cyberpunk" | "solarized" =
      "light";
    if (newTheme === "system") {
      const isDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
      resolved = isDark ? "dark" : "light";
    } else {
      resolved = newTheme as
        | "dark"
        | "light"
        | "custom"
        | "cyberpunk"
        | "solarized";
    }

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
      const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
      const handleChange = () => applyTheme("system");
      mediaQuery.addEventListener("change", handleChange);
      return () => mediaQuery.removeEventListener("change", handleChange);
    }
  }, [theme, applyTheme]);

  // Synchronize on mount from localStorage
  React.useEffect(() => {
    const saved = (localStorage.getItem("theme") as Theme) || "system";
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
