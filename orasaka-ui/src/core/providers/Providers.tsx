"use client";

import { ReactNode } from "react";
import { SessionProvider } from "next-auth/react";
import { QueryProvider } from "./QueryProvider";
import { ThemeProvider } from "./ThemeProvider";
import { SidebarProvider } from "@/core/context/SidebarContext";
import { TenantProvider } from "@/features/tenant/context/TenantContext";
import { LocaleProvider } from "@/core/context/LocaleContext";

interface ProvidersProps {
  children: ReactNode;
}

/**
 * Unified application provider tree wrapping all required context contexts.
 *
 * @param props The provider tree properties.
 * @returns The wrapped ReactNode provider layout.
 */
export function Providers({ children }: ProvidersProps) {
  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
      <SessionProvider>
        <QueryProvider>
          <LocaleProvider>
            <TenantProvider>
              <SidebarProvider>{children}</SidebarProvider>
            </TenantProvider>
          </LocaleProvider>
        </QueryProvider>
      </SessionProvider>
    </ThemeProvider>
  );
}
