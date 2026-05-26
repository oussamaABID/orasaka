"use client";

import { ReactNode } from "react";
import { SessionProvider } from "next-auth/react";
import { QueryProvider } from "./QueryProvider";
import { ThemeProvider } from "./ThemeProvider";
import { SidebarProvider } from "@/core/context/SidebarContext";
import { TenantProvider } from "@/core/context/TenantContext";
import { LocaleProvider } from "@/core/context/LocaleContext";
import { JobStreamProvider } from "@/core/context/JobStreamContext";
import { ToastProvider } from "@/core/context/ToastContext";
import { CommandPalette } from "@/components/ui/CommandPalette";

interface ProvidersProps {
  children: ReactNode;
}

/**
 * Unified application provider tree wrapping all required context contexts.
 *
 * @param props The provider tree properties.
 * @returns The wrapped ReactNode provider layout.
 */
export function Providers({ children }: Readonly<ProvidersProps>) {
  return (
    <ThemeProvider>
      <SessionProvider>
        <QueryProvider>
          <LocaleProvider>
            <TenantProvider>
              <SidebarProvider>
                <JobStreamProvider>
                  <ToastProvider>
                    <CommandPalette />
                    {children}
                  </ToastProvider>
                </JobStreamProvider>
              </SidebarProvider>
            </TenantProvider>
          </LocaleProvider>
        </QueryProvider>
      </SessionProvider>
    </ThemeProvider>
  );
}
