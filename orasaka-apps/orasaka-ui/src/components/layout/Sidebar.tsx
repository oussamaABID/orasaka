/* eslint-disable */
"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Icon, type IconName } from "@/components/ui/icon";
import { SentinelMini } from "@/components/ui/SentinelMini";
import { useAuth } from "@/core/hooks/useAuth";
import { useSidebar } from "@/core/context/SidebarContext";
import { useTenant } from "@/core/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";

interface NavItem {
  href: string;
  icon: IconName;
  label: string;
  adminOnly?: boolean;
}

/**
 * Krizaka Icon-Only Sidebar Rail (56px / w-14).
 *
 * Desktop: icon-only rail with tooltip labels on hover, active route indicator,
 * and bottom-anchored user profile avatar. Mobile: full-width drawer with labels.
 */
export function Sidebar() {
  const { user } = useAuth();
  const { isOpen, close } = useSidebar();
  const { config } = useTenant();
  const { t } = useTranslation();
  const pathname = usePathname();

  const navItems: NavItem[] = [
    { href: "/", icon: "dashboard", label: t.sidebar.dashboard },
    { href: "/chat", icon: "chat", label: t.sidebar.chatSessions },
    { href: "/playground", icon: "playground", label: t.sidebar.playground },
    { href: "/dashboard/jobs", icon: "history", label: t.sidebar.jobsHistory },
    {
      href: "/dashboard/admin",
      icon: "admin",
      label: t.sidebar.adminPanel,
      adminOnly: true,
    },
  ];

  const visibleItems = navItems.filter(
    (item) => !item.adminOnly || user?.role === "admin",
  );

  const isActive = (href: string) => {
    if (href === "/") return pathname === "/";
    return pathname.startsWith(href);
  };

  // User initials for avatar fallback
  const userInitials = user?.name
    ? user.name
        .split(" ")
        .map((n: string) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : "?";

  /* ─── Desktop: Icon-Only Rail ────────────────────────── */
  const desktopRail = (
    <div className="krizaka-navbar h-full py-3 relative">
      {/* Brand logo */}
      <Link
        href="/"
        className="flex items-center justify-center w-10 h-10 mb-4 transition-opacity duration-200 hover:opacity-80"
      >
        <SentinelMini size={22} />
      </Link>

      {/* Nav icons */}
      <nav className="flex-1 flex flex-col items-center gap-1 w-full px-1.5">
        {visibleItems.map(({ href, icon: iconName, label }, index) => {
          const active = isActive(href);
          return (
            <Link
              key={href}
              href={href}
              title={label}
              style={{
                animationDelay: `${index * 40}ms`,
                animationFillMode: "backwards",
              }}
              className="animate-in fade-in duration-300 block w-full"
            >
              <div
                className={`relative flex items-center justify-center w-full h-10 transition-all duration-200 group ${
                  active
                    ? "text-[var(--accent)] bg-[var(--surface-2)]"
                    : "text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)]"
                }`}
              >
                {/* Active indicator pill */}
                {active && (
                  <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 bg-[var(--accent)] animate-in fade-in zoom-in-50 duration-200" />
                )}
                <Icon name={iconName} size={18} />

                {/* Tooltip label on hover */}
                <span className="absolute left-full ml-2 px-2.5 py-1 text-[11px] font-medium text-[var(--text-primary)] bg-[var(--surface-2)] border border-[var(--border-subtle)] shadow-lg whitespace-nowrap opacity-0 pointer-events-none group-hover:opacity-100 transition-opacity duration-150 z-50">
                  {label}
                </span>
              </div>
            </Link>
          );
        })}
      </nav>

      {/* Settings icon */}
      <Link
        href="/settings"
        title={t.sidebar.settings || "Settings"}
        className="flex items-center justify-center w-full"
      >
        <div
          className={`flex items-center justify-center w-10 h-10 transition-all duration-200 ${
            isActive("/settings")
              ? "text-[var(--accent)] bg-[var(--surface-2)]"
              : "text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)]"
          }`}
        >
          <Icon name="settings" size={18} />
        </div>
      </Link>

      {/* Bottom-anchored user avatar */}
      <div className="krizaka-avatar-anchor">
        <Link href="/profile" title={user?.name || "Profile"}>
          <div className="w-8 h-8 rounded-full bg-[var(--accent-soft)] border border-[var(--border-subtle)] flex items-center justify-center text-[10px] font-semibold text-[var(--accent)] transition-all duration-200 hover:border-[var(--accent)] hover:shadow-[0_0_12px_var(--accent-soft)]">
            {userInitials}
          </div>
        </Link>
      </div>
    </div>
  );

  /* ─── Mobile: Full-Width Drawer with Labels ──────────── */
  const mobileDrawerContent = (
    <div className="flex h-full w-full flex-col border-r border-[var(--border-subtle)] bg-[color-mix(in_srgb,var(--surface-1)_82%,transparent)] backdrop-blur-xl backdrop-saturate-[180%]">
      {/* Brand header */}
      <div className="flex h-14 items-center justify-between px-5 border-b border-[var(--border-subtle)]">
        <Link href="/" className="flex items-center gap-2.5" onClick={close}>
          <SentinelMini size={24} className="transition-opacity duration-200 hover:opacity-80" />
          <h1 className="text-[13px] font-semibold tracking-[-0.02em] text-[var(--text-primary)]">
            {config.displayName}
          </h1>
        </Link>
        <button
          onClick={close}
          className="p-1.5 rounded-lg text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)] transition-colors duration-150"
          aria-label="Close Sidebar"
        >
          <Icon name="close" size={16} />
        </button>
      </div>

      {/* Nav items with labels */}
      <div className="flex-1 overflow-auto py-3">
        <span className="hud-label px-5 mb-1 block">{t.sidebar.navigation}</span>
        <nav className="space-y-0.5 px-3">
          {visibleItems.map(({ href, icon: iconName, label }, index) => {
            const active = isActive(href);
            return (
              <Link
                key={href}
                href={href}
                onClick={close}
                style={{
                  animationDelay: `${index * 40}ms`,
                  animationFillMode: "backwards",
                }}
                className="animate-in fade-in slide-in-from-left-2 duration-300 block"
              >
                <div
                  className={`w-full flex items-center gap-2.5 h-9 px-3 text-sm transition-all duration-200 relative ${
                    active
                      ? "font-semibold text-[var(--text-primary)] bg-[var(--surface-2)]"
                      : "font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)]"
                  }`}
                >
                  {active && (
                    <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 bg-[var(--accent)] animate-in fade-in zoom-in-50 duration-200" />
                  )}
                  <Icon
                    name={iconName}
                    size={16}
                    className={`flex-shrink-0 transition-colors duration-200 ${
                      active ? "text-[var(--accent)]" : ""
                    }`}
                  />
                  {label}
                </div>
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Settings */}
      <div className="p-3 border-t border-[var(--border-subtle)]">
        <Link href="/settings" onClick={close}>
          <div
            className={`w-full flex items-center gap-2.5 h-9 px-3 text-sm font-medium transition-all duration-200 ${
              isActive("/settings")
                ? "text-[var(--text-primary)] bg-[var(--surface-2)] font-semibold"
                : "text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)]"
            }`}
          >
            <Icon name="settings" size={16} className="flex-shrink-0" />
            {t.sidebar.settings || "Settings"}
          </div>
        </Link>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop: icon-only rail */}
      <div className="hidden md:flex h-full w-14 flex-col flex-shrink-0">
        {desktopRail}
      </div>

      {/* Mobile: full drawer overlay */}
      {isOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          <button
            type="button"
            className="fixed inset-0 bg-black/40 backdrop-blur-sm transition-opacity duration-200 border-none cursor-default"
            aria-label="Close sidebar"
            onClick={close}
          />
          <div className="relative flex flex-col w-56 max-w-[80vw] h-full bg-[color-mix(in_srgb,var(--surface-1)_90%,transparent)] backdrop-blur-xl backdrop-saturate-[180%] z-50 transition-transform duration-200">
            {mobileDrawerContent}
          </div>
        </div>
      )}
    </>
  );
}
