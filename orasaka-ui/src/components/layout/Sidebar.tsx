"use client";

import * as React from "react";
import Link from "next/link";
import Image from "next/image";
import { Home, MessageSquare, LogOut, X, Cpu } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { Button } from "@/components/ui/Button";
import { useSidebar } from "@/core/context/SidebarContext";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";

export function Sidebar() {
  const { logout } = useAuth();
  const { isOpen, close } = useSidebar();
  const { config, accentClasses } = useTenant();
  const { t } = useTranslation();

  const isCompact = config.layoutMode === "compact";
  const navItemBaseClass =
    "w-full justify-start rounded-xl text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-50 transition-all duration-200 hover:translate-x-0.5";

  const sidebarContent = (
    <div className="flex h-full w-full flex-col border-r border-zinc-200/80 bg-zinc-50/80 dark:border-zinc-800/60 dark:bg-zinc-950/80 backdrop-blur-md transition-all duration-200">
      <div
        className={`flex ${isCompact ? "h-12" : "h-16"} items-center justify-between px-6 border-b border-zinc-200/80 dark:border-zinc-800/60`}
      >
        <Link href="/" className="flex items-center" onClick={close}>
          <Image
            src="/logo.svg"
            alt="Orasaka Logo"
            width={28}
            height={28}
            className="mr-2.5 transition-transform duration-300 hover:rotate-12"
          />
          <h1 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 hover:opacity-95 transition-opacity">
            {config.displayName}
          </h1>
        </Link>
        <button
          onClick={close}
          className="p-1.5 rounded-xl text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200 md:hidden hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-all duration-200"
          aria-label="Close Sidebar"
        >
          <X className="h-5 w-5" />
        </button>
      </div>
      <div className={`flex-1 overflow-auto ${isCompact ? "py-2" : "py-4"}`}>
        <nav className={`${isCompact ? "space-y-0.5" : "space-y-1.5"} px-4`}>
          <Link href="/" onClick={close}>
            <Button
              variant="ghost"
              className={`${navItemBaseClass} hover:${accentClasses.bgSoft} hover:${accentClasses.text}`}
            >
              <Home className="mr-3 h-5 w-5 flex-shrink-0" />
              {t.sidebar.dashboard}
            </Button>
          </Link>
          <Link href="/chat" onClick={close}>
            <Button
              variant="ghost"
              className={`${navItemBaseClass} hover:${accentClasses.bgSoft} hover:${accentClasses.text}`}
            >
              <MessageSquare className="mr-3 h-5 w-5 flex-shrink-0" />
              {t.sidebar.chatSessions}
            </Button>
          </Link>
          <Link href="/playground" onClick={close}>
            <Button
              variant="ghost"
              className={`${navItemBaseClass} hover:${accentClasses.bgSoft} hover:${accentClasses.text}`}
            >
              <Cpu className="mr-3 h-5 w-5 flex-shrink-0" />
              {t.sidebar.playground}
            </Button>
          </Link>
        </nav>
      </div>
      <div className="p-4 border-t border-zinc-200/80 dark:border-zinc-800/60">
        <Button
          variant="outline"
          className="w-full justify-start rounded-xl text-red-600 hover:bg-red-50 hover:text-red-700 dark:border-red-950 dark:text-red-500 dark:hover:bg-red-950/30 transition-all duration-200"
          onClick={() => {
            close();
            logout();
          }}
        >
          <LogOut className="mr-3 h-5 w-5 flex-shrink-0" />
          {t.sidebar.logout}
        </Button>
      </div>
    </div>
  );

  return (
    <>
      <div className="hidden md:flex h-full w-64 flex-col flex-shrink-0">
        {sidebarContent}
      </div>

      {isOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          <div
            className="fixed inset-0 bg-zinc-950/60 backdrop-blur-sm transition-opacity duration-200"
            onClick={close}
          />
          <div className="relative flex flex-col w-64 max-w-[80vw] h-full bg-zinc-50/90 dark:bg-zinc-950/90 backdrop-blur-md z-50 transform transition-transform duration-300">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
}
