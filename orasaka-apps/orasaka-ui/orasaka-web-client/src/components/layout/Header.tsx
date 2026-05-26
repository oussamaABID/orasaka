"use client";

import * as React from "react";
import Link from "next/link";
import { useAuth } from "@/core/hooks/useAuth";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { useSidebar } from "@/core/context/SidebarContext";
import { useTenant } from "@/core/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { Icon } from "@/components/ui/icon";
import { NotificationBell } from "./NotificationBell";

/**
 * Global Header layout Component — Calm Obsidian 2026.
 *
 * <p>Compact 56px height, solid surface-1 background with subtle border.
 * Dropdowns use surface-2 with clean border-default, no glass effects.
 * Rendered exclusively as a Client Component ("use client") due to
 * stateful dropdowns and context consumption.
 *
 * @returns The global top header navbar React element.
 * @see {@link useAuth}
 * @see {@link useSidebar}
 * @see {@link useTenant}
 * @see {@link useTranslation}
 */
export const Header: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const { open } = useSidebar();
  const { accentClasses } = useTenant();
  const { locale, setLocale, t } = useTranslation();

  const [dropdownOpen, setDropdownOpen] = React.useState(false);
  const [langDropdownOpen, setLangDropdownOpen] = React.useState(false);
  const [bellOpen, setBellOpen] = React.useState(false);

  const initial = React.useMemo(() => {
    if (!user) return "U";
    const val = user.name || user.email || "User";
    return val[0].toUpperCase();
  }, [user]);

  const dropdownItemClass =
    "block rounded-lg px-3 py-2 text-sm text-[var(--text-secondary)] hover:bg-[var(--surface-3)] hover:text-[var(--text-primary)] transition-colors duration-150";

  const dropdownPanelClass =
    "absolute right-0 mt-2 rounded-lg border border-[var(--border-default)] bg-[var(--surface-2)] p-1 shadow-lg z-20";

  return (
    <header className="sticky top-0 z-30 flex h-14 w-full items-center justify-between border-b border-[var(--border-subtle)] bg-[var(--surface-1)] px-5">
      <div className="flex items-center md:hidden">
        <button
          onClick={open}
          className="mr-2 p-2 rounded-lg text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)] transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)]"
          aria-label="Open Sidebar"
        >
          <Icon name="menu" size={20} />
        </button>
      </div>
      <div className="flex-1" /> {/* Spacer */}
      <div className="flex items-center gap-3">
        {/* Language Switcher Dropdown */}
        <div className="relative">
          <button
            onClick={() => {
              setLangDropdownOpen(!langDropdownOpen);
              setDropdownOpen(false);
              setBellOpen(false);
            }}
            className="flex items-center gap-1.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] rounded-lg px-2.5 py-1.5 text-xs font-medium text-[var(--text-secondary)] hover:bg-[var(--surface-2)] hover:text-[var(--text-primary)] transition-colors duration-150"
            aria-label="Change Language"
          >
            <Icon name="language" size={16} />
            <span className="uppercase">{locale}</span>
            <Icon name="chevronDown" size={12} className="opacity-50" />
          </button>

          {langDropdownOpen && (
            <>
              <button
                type="button"
                className="fixed inset-0 z-10 bg-transparent border-none cursor-default"
                aria-label="Close language menu"
                onClick={() => setLangDropdownOpen(false)}
              />
              <div className={`${dropdownPanelClass} w-36`}>
                <button
                  onClick={() => {
                    setLocale("en");
                    setLangDropdownOpen(false);
                  }}
                  className={`w-full flex items-center justify-between rounded-lg px-3 py-2 text-sm text-left transition-colors duration-150 ${
                    locale === "en"
                      ? `${accentClasses.bgSoft} ${accentClasses.text} font-medium`
                      : "text-[var(--text-secondary)] hover:bg-[var(--surface-3)] hover:text-[var(--text-primary)]"
                  }`}
                >
                  <span>{t.settings.english}</span>
                  {locale === "en" && <Icon name="shield" size={14} />}
                </button>
                <button
                  onClick={() => {
                    setLocale("fr");
                    setLangDropdownOpen(false);
                  }}
                  className={`w-full flex items-center justify-between rounded-lg px-3 py-2 text-sm text-left transition-colors duration-150 ${
                    locale === "fr"
                      ? `${accentClasses.bgSoft} ${accentClasses.text} font-medium`
                      : "text-[var(--text-secondary)] hover:bg-[var(--surface-3)] hover:text-[var(--text-primary)]"
                  }`}
                >
                  <span>{t.settings.french}</span>
                  {locale === "fr" && <Icon name="shield" size={14} />}
                </button>
              </div>
            </>
          )}
        </div>

        <ThemeToggle />

        {/* Notification Bell Dropdown */}
        {isAuthenticated && (
          <NotificationBell
            bellOpen={bellOpen}
            onToggle={(openState) => {
              setBellOpen(openState);
              setDropdownOpen(false);
              setLangDropdownOpen(false);
            }}
          />
        )}

        {/* User Account Dropdown */}
        {isAuthenticated && user && (
          <div className="relative">
            <button
              onClick={() => {
                setDropdownOpen(!dropdownOpen);
                setLangDropdownOpen(false);
                setBellOpen(false);
              }}
              className="flex items-center gap-2.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)] rounded-lg p-1 transition-colors duration-150 hover:bg-[var(--surface-2)]"
            >
              <span className="text-sm font-medium text-[var(--text-secondary)] hidden sm:inline-block">
                {user.name || user.email || "Admin"}
              </span>
              <div
                className={`h-7 w-7 rounded-full ${accentClasses.bg} text-white flex items-center justify-center font-semibold text-xs`}
              >
                {initial}
              </div>
            </button>

            {dropdownOpen && (
              <>
                <button
                  type="button"
                  className="fixed inset-0 z-10 bg-transparent border-none cursor-default"
                  aria-label="Close profile menu"
                  onClick={() => setDropdownOpen(false)}
                />
                <div className={`${dropdownPanelClass} w-56`}>
                  {/* User info header */}
                  <section className="px-3 py-2.5 border-b border-[var(--border-subtle)] mb-1">
                    <p className="text-[13px] font-semibold text-[var(--text-primary)] truncate">
                      {user.name || user.email || "Admin"}
                    </p>
                    <p className="text-[10px] text-[var(--text-muted)] truncate">
                      {user.email || ""}
                    </p>
                  </section>

                  <Link
                    href="/profile"
                    onClick={() => setDropdownOpen(false)}
                    className={`${dropdownItemClass} flex items-center gap-2.5`}
                  >
                    <Icon name="profile" size={14} className="flex-shrink-0 text-[var(--text-muted)]" />
                    {t.header.profile}
                  </Link>
                  <Link
                    href="/settings"
                    onClick={() => setDropdownOpen(false)}
                    className={`${dropdownItemClass} flex items-center gap-2.5`}
                  >
                    <Icon name="settings" size={14} className="flex-shrink-0 text-[var(--text-muted)]" />
                    {t.header.settings}
                  </Link>
                  <hr className="my-1 border-[var(--border-subtle)]" />
                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      logout();
                    }}
                    className="w-full text-left rounded-lg flex items-center gap-2.5 px-3 py-2 text-sm text-red-500 hover:bg-red-500/5 dark:text-red-400 dark:hover:bg-red-950/20 transition-colors duration-150"
                  >
                    <Icon name="logout" size={14} className="flex-shrink-0" />
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
};
