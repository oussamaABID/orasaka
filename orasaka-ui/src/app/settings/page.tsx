"use client";

import * as React from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { SettingsForm } from "@/features/settings/components/SettingsForm";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useRouter } from "next/navigation";
import { useTranslation } from "@/core/context/LocaleContext";

/**
 * SettingsPage component allowing users to view and customize their preferences.
 *
 * @returns The rendered React element for the settings page.
 */
export default function SettingsPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();
  const { t } = useTranslation();

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading || !isAuthenticated) return null;

  return (
    <section className="flex h-screen overflow-hidden bg-[var(--surface-0)] transition-colors duration-200">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-auto p-6 scrollbar-thin ambient-grid">
          <div className="mx-auto max-w-5xl space-y-6 animate-in fade-in slide-in-from-bottom-3 duration-300">
            <h2 className="fluid-2xl font-bold tracking-tight text-[var(--text-primary)]">
              {t.sidebar.settings || "Settings"}
            </h2>
            <SettingsForm />
          </div>
        </main>
      </div>
    </section>
  );
}
