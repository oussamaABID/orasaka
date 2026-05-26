"use client";

import * as React from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import {
  Home,
  MessageSquare,
  X,
  Cpu,
  History,
  Sliders,
  Settings,
} from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { Button } from "@/components/ui/Button";
import { useSidebar } from "@/core/context/SidebarContext";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";

interface NavItem {
  href: string;
  icon: React.ElementType;
  label: string;
  adminOnly?: boolean;
}

/**
 * Sidebar navigation with active route indicator,
 * staggered mount animation, and settings shortcut.
 * Logout is handled exclusively via the Header user dropdown.
 */
export function Sidebar() {
  const { user } = useAuth();
  const { isOpen, close } = useSidebar();
  const { config } = useTenant();
  const { t } = useTranslation();
  const pathname = usePathname();

  const isCompact = config.layoutMode === "compact";

  const navItems: NavItem[] = [
    { href: "/", icon: Home, label: t.sidebar.dashboard },
    { href: "/chat", icon: MessageSquare, label: t.sidebar.chatSessions },
    { href: "/playground", icon: Cpu, label: t.sidebar.playground },
    { href: "/dashboard/jobs", icon: History, label: t.sidebar.jobsHistory },
    {
      href: "/dashboard/admin",
      icon: Sliders,
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

  const sidebarContent = (
    <div className="flex h-full w-full flex-col border-r border-[var(--border-subtle)] bg-[color-mix(in_srgb,var(--surface-1)_82%,transparent)] backdrop-blur-xl backdrop-saturate-[180%]">
      {/* Brand logo */}
      <div
        className={`flex ${isCompact ? "h-12" : "h-14"} items-center justify-between px-5 border-b border-[var(--border-subtle)]`}
      >
        <Link href="/" className="flex items-center gap-2.5" onClick={close}>
          <Image
            src="/logo.svg"
            alt="Orasaka Logo"
            width={24}
            height={24}
            className="transition-opacity duration-200 hover:opacity-80"
          />
          <h1 className="text-[13px] font-semibold tracking-[-0.02em] text-[var(--text-primary)]">
            {config.displayName}
          </h1>
        </Link>
        <button
          onClick={close}
          className="p-1.5 rounded-lg text-[var(--text-muted)] hover:text-[var(--text-primary)] md:hidden hover:bg-[var(--surface-2)] transition-colors duration-150"
          aria-label="Close Sidebar"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className={`flex-1 overflow-auto ${isCompact ? "py-2" : "py-3"}`}>
        <span className="hud-label px-5 mb-1 block">
          {t.sidebar.navigation}
        </span>
        <nav className="space-y-0.5 px-3">
          {visibleItems.map(({ href, icon: Icon, label }, index) => {
            const active = isActive(href);

            const playgroundCategories =
              href === "/playground"
                ? [
                    {
                      label: t.sidebar.videoCategory,
                      items: [
                        {
                          href: "/playground/video/generate",
                          label: t.sidebar.generateVideo,
                        },
                        {
                          href: "/playground/video/analyze",
                          label: t.sidebar.analyzeVideo,
                        },
                      ],
                    },
                    {
                      label: t.sidebar.audioCategory,
                      items: [
                        {
                          href: "/playground/audio/analyze",
                          label: t.sidebar.analyzeAudio,
                        },
                      ],
                    },
                    {
                      label: t.sidebar.textCategory,
                      items: [
                        {
                          href: "/playground/text/chat",
                          label: t.sidebar.textChat,
                        },
                      ],
                    },
                    {
                      label: t.sidebar.imageCategory,
                      items: [
                        {
                          href: "/playground/image/generate",
                          label: t.sidebar.generateImage,
                        },
                      ],
                    },
                    {
                      label: t.sidebar.codeCategory,
                      items: [
                        {
                          href: "/playground/code/scaffold",
                          label: t.sidebar.featureToCode,
                        },
                      ],
                    },
                    {
                      label: t.sidebar.speechCategory,
                      items: [
                        {
                          href: "/playground/speech/synthesis",
                          label: t.sidebar.speechSynthesis,
                        },
                      ],
                    },
                    {
                      label: t.sidebar.visionCategory,
                      items: [
                        {
                          href: "/playground/vision/analyze",
                          label: t.sidebar.visionAnalysis,
                        },
                      ],
                    },
                  ]
                : [];

            return (
              <React.Fragment key={href}>
                <Link
                  href={href}
                  onClick={close}
                  style={{
                    animationDelay: `${index * 40}ms`,
                    animationFillMode: "backwards",
                  }}
                  className="animate-in fade-in slide-in-from-left-2 duration-300 block"
                >
                  <Button
                    variant="ghost"
                    className={`w-full justify-start rounded-[var(--radius-sm)] fluid-xs h-9 px-3 transition-all duration-200 relative group ${
                      active
                        ? "font-semibold text-[var(--text-primary)] bg-[var(--surface-2)]"
                        : "font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)]"
                    }`}
                  >
                    {/* Active route indicator pill */}
                    {active && (
                      <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 rounded-full bg-[var(--accent)] transition-all duration-300 animate-in fade-in zoom-in-50 duration-200" />
                    )}
                    <Icon
                      className={`mr-2.5 h-4 w-4 flex-shrink-0 transition-colors duration-200 ${
                        active
                          ? "text-[var(--accent)]"
                          : "group-hover:text-[var(--text-primary)]"
                      }`}
                    />
                    {label}
                  </Button>
                </Link>

                {href === "/playground" &&
                  pathname.startsWith("/playground") && (
                    <div className="pl-4 ml-6 border-l border-[var(--border-subtle)] space-y-3 mt-1.5 mb-2 animate-in slide-in-from-top-2 duration-200">
                      {playgroundCategories.map((cat) => (
                        <div key={cat.label} className="space-y-1">
                          <span className="block text-[10px] uppercase tracking-wider text-[var(--text-muted)] font-semibold select-none">
                            {cat.label}
                          </span>
                          <div className="space-y-0.5">
                            {cat.items.map((item) => {
                              const subActive = pathname === item.href;
                              return (
                                <Link
                                  key={item.href}
                                  href={item.href}
                                  onClick={close}
                                  className={`block text-xs font-medium py-1 transition-colors duration-150 relative ${
                                    subActive
                                      ? "text-[var(--text-primary)] font-semibold"
                                      : "text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
                                  }`}
                                >
                                  {subActive && (
                                    <span className="absolute -left-[21px] top-1/2 -translate-y-1/2 w-1.5 h-1.5 rounded-full bg-[var(--accent)] shadow-[0_0_8px_var(--accent)]" />
                                  )}
                                  {item.label}
                                </Link>
                              );
                            })}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
              </React.Fragment>
            );
          })}
        </nav>
      </div>

      <div className="p-3 border-t border-[var(--border-subtle)]">
        <span className="hud-label px-2 mb-1 block">SETTINGS</span>
        <Link href="/settings" onClick={close}>
          <Button
            variant="ghost"
            className={`w-full justify-start rounded-[var(--radius-sm)] fluid-xs font-medium h-9 px-3 transition-all duration-200 ${
              isActive("/settings")
                ? "text-[var(--text-primary)] bg-[var(--surface-2)] font-semibold"
                : "text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--surface-2)]"
            }`}
          >
            <Settings className="mr-2.5 h-4 w-4 flex-shrink-0" />
            {t.sidebar.settings || "Settings"}
          </Button>
        </Link>
      </div>
    </div>
  );

  return (
    <>
      <div className="hidden md:flex h-full w-56 flex-col flex-shrink-0">
        {sidebarContent}
      </div>

      {isOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          <button
            type="button"
            className="fixed inset-0 bg-black/40 backdrop-blur-sm transition-opacity duration-200 border-none cursor-default"
            aria-label="Close sidebar"
            onClick={close}
          />
          <div className="relative flex flex-col w-56 max-w-[80vw] h-full bg-[color-mix(in_srgb,var(--surface-1)_90%,transparent)] backdrop-blur-xl backdrop-saturate-[180%] z-50 transition-transform duration-200">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
}
