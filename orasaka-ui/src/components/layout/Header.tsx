"use client";

import * as React from "react";
import Link from "next/link";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { useSidebar } from "@/core/context/SidebarContext";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { Menu, Globe, ChevronDown, Check } from "lucide-react";

/**
 * Global Header layout Component.
 *
 * <p>Contains the branding context, sidebar toggle controls for responsive screens,
 * language configuration dropdown switcher, theme toggle selector, and user profile management actions.
 * Rendered exclusively as a Client Component ("use client") due to stateful dropdowns and context consumption.
 *
 * @returns The global top header navbar React element.
 * @see {@link useAuth}
 * @see {@link useSidebar}
 * @see {@link useTenant}
 * @see {@link useTranslation}
 */
export function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const { open } = useSidebar();
  const { accentClasses } = useTenant();
  const { locale, setLocale, t } = useTranslation();

  const [dropdownOpen, setDropdownOpen] = React.useState(false);
  const [langDropdownOpen, setLangDropdownOpen] = React.useState(false);

  const initial = React.useMemo(() => {
    if (!user) return "U";
    const val = user.name || user.email || "User";
    return val[0].toUpperCase();
  }, [user]);

  const dropdownItemClass =
    "block rounded-lg px-3 py-2 text-sm text-zinc-700 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800/80 transition-all duration-200 hover:scale-[1.01]";

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-zinc-200/80 bg-white/70 px-6 backdrop-blur-md dark:border-zinc-800/60 dark:bg-zinc-950/70 transition-all duration-200">
      <div className="flex items-center md:hidden">
        <button
          onClick={open}
          className="mr-2 p-2 rounded-xl text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200 hover:bg-zinc-100/80 dark:hover:bg-zinc-900/50 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-zinc-400"
          aria-label="Open Sidebar"
        >
          <Menu className="h-5 w-5" />
        </button>
      </div>
      <div className="flex-1" /> {/* Spacer */}
      <div className="flex items-center space-x-4">
        {/* Language Switcher Dropdown */}
        <div className="relative">
          <button
            onClick={() => setLangDropdownOpen(!langDropdownOpen)}
            className="flex items-center space-x-1.5 focus:outline-none focus:ring-2 focus:ring-zinc-400 rounded-xl px-3 py-1.5 text-sm font-medium text-zinc-600 dark:text-zinc-400 hover:bg-zinc-100/65 dark:hover:bg-zinc-900/55 hover:text-zinc-900 dark:hover:text-zinc-200 transition-all duration-200"
            aria-label="Change Language"
          >
            <Globe className="h-4 w-4" />
            <span className="uppercase">{locale}</span>
            <ChevronDown className="h-3 w-3 opacity-60" />
          </button>

          {langDropdownOpen && (
            <>
              <div
                className="fixed inset-0 z-10"
                onClick={() => setLangDropdownOpen(false)}
              />
              <div className="absolute right-0 mt-2 w-36 rounded-xl border border-zinc-200/80 bg-white/90 p-1.5 shadow-xl dark:border-zinc-800/60 dark:bg-zinc-900/90 backdrop-blur-md z-20 animate-in fade-in slide-in-from-top-2 duration-200">
                <button
                  onClick={() => {
                    setLocale("en");
                    setLangDropdownOpen(false);
                  }}
                  className={`w-full flex items-center justify-between rounded-lg px-3 py-2 text-sm text-left transition-all duration-200 ${
                    locale === "en"
                      ? `${accentClasses.bgSoft} ${accentClasses.text} font-semibold`
                      : "text-zinc-700 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800/80"
                  }`}
                >
                  <span>English</span>
                  {locale === "en" && <Check className="h-3.5 w-3.5" />}
                </button>
                <button
                  onClick={() => {
                    setLocale("fr");
                    setLangDropdownOpen(false);
                  }}
                  className={`w-full flex items-center justify-between rounded-lg px-3 py-2 text-sm text-left transition-all duration-200 ${
                    locale === "fr"
                      ? `${accentClasses.bgSoft} ${accentClasses.text} font-semibold`
                      : "text-zinc-700 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800/80"
                  }`}
                >
                  <span>Français</span>
                  {locale === "fr" && <Check className="h-3.5 w-3.5" />}
                </button>
              </div>
            </>
          )}
        </div>

        <ThemeToggle />

        {/* User Account Dropdown */}
        {isAuthenticated && user && (
          <div className="relative">
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className="flex items-center space-x-3 focus:outline-none focus:ring-2 focus:ring-zinc-400 rounded-xl p-1.5 transition-all duration-200 hover:bg-zinc-100/50 dark:hover:bg-zinc-900/40"
            >
              <span className="text-sm font-medium text-zinc-700 dark:text-zinc-300 hidden sm:inline-block">
                {user.name || user.email || "Admin"}
              </span>
              <div
                className={`h-8 w-8 rounded-full ${accentClasses.bg} text-white flex items-center justify-center font-bold text-sm shadow-sm transition-all duration-200 hover:scale-105 active:scale-95`}
              >
                {initial}
              </div>
            </button>

            {dropdownOpen && (
              <>
                <div
                  className="fixed inset-0 z-10"
                  onClick={() => setDropdownOpen(false)}
                />
                <div className="absolute right-0 mt-2 w-48 rounded-xl border border-zinc-200/80 bg-white/90 p-1.5 shadow-xl dark:border-zinc-800/60 dark:bg-zinc-900/90 backdrop-blur-md z-20 animate-in fade-in slide-in-from-top-2 duration-200">
                  <Link
                    href="/profile"
                    onClick={() => setDropdownOpen(false)}
                    className={dropdownItemClass}
                  >
                    {t.header.profile}
                  </Link>
                  <Link
                    href="/settings"
                    onClick={() => setDropdownOpen(false)}
                    className={dropdownItemClass}
                  >
                    {t.header.settings}
                  </Link>
                  <hr className="my-1 border-zinc-200/80 dark:border-zinc-800/60" />
                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      logout();
                    }}
                    className="w-full text-left rounded-lg block px-3 py-2 text-sm text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/20 transition-all duration-200 active:scale-[0.99]"
                  >
                    {t.header.logout}
                  </button>
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </header>
  );
}
