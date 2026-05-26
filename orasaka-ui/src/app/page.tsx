"use client";

import * as React from "react";
import { format } from "date-fns";
import { fr, enUS } from "date-fns/locale";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { useTenant } from "@/features/tenant/context/TenantContext";
import { useTranslation } from "@/core/context/LocaleContext";
import { useChatStream } from "@/features/chat-session/hooks/useChatStream";
import { useToast } from "@/core/context/ToastContext";
import { Skeleton } from "@/components/ui/Skeleton";

import { QuickActions } from "@/features/dashboard/components/QuickActions";
import { RecentSessions } from "@/features/dashboard/components/RecentSessions";

/**
 * HomePage component rendering the dynamic user dashboard.
 * Uses extracted sub-components to stay modular and readable.
 *
 * @returns The rendered React element for the dashboard view.
 */
export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const { accentClasses } = useTenant();
  const { locale, t } = useTranslation();
  const { addToast } = useToast();

  // Use useChatStream to fetch threads list and access createThread helper
  const { threads, createThread } = useChatStream("dashboard");

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  const handleStartNewChat = async () => {
    try {
      const newThread = await createThread();
      router.push(`/chat?conversationId=${newThread.conversationId}`);
      addToast(t.dashboard.startNewChat, "success");
    } catch (e) {
      console.error("Failed to start new chat:", e);
      addToast(t.errors.generic, "error");
    }
  };

  const formatDate = (timestamp: number) => {
    try {
      return format(timestamp, "MMM d, HH:mm", {
        locale: locale === "fr" ? fr : enUS,
      });
    } catch {
      return "";
    }
  };

  if (isLoading || !isAuthenticated) {
    return (
      <section className="flex min-h-screen items-center justify-center bg-[var(--surface-0)] ambient-grid">
        <div className="w-full max-w-5xl px-6 space-y-8">
          {/* Skeleton header */}
          <div className="space-y-2">
            <Skeleton variant="text" width="40%" height="2rem" />
            <Skeleton variant="text" width="60%" />
          </div>
          {/* Skeleton grid */}
          <section className="grid gap-6 md:grid-cols-3">
            <section className="md:col-span-2 space-y-3">
              <Skeleton variant="text" width="30%" />
              <Skeleton variant="rect" height="5rem" />
              <Skeleton variant="rect" height="5rem" />
              <Skeleton variant="rect" height="5rem" />
            </section>
            <aside className="md:col-span-1 space-y-3">
              <Skeleton variant="text" width="40%" />
              <Skeleton variant="rect" height="12rem" />
            </aside>
          </section>
        </div>
      </section>
    );
  }

  // Get most recent 3 threads to show in activity panel
  const recentThreads = [...(threads || [])]
    .sort((a, b) => b.updatedAt - a.updatedAt)
    .slice(0, 3);

  return (
    <section className="flex h-screen overflow-hidden bg-[var(--surface-0)] transition-colors duration-200">
      <Sidebar />

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />

        <main className="flex-1 overflow-auto p-6 scrollbar-thin ambient-grid">
          <div className="mx-auto max-w-5xl space-y-8 animate-in fade-in slide-in-from-bottom-3 duration-300">
            {/* Page Header */}
            <header className="space-y-1">
              <h2 className="fluid-2xl font-bold tracking-tight text-[var(--text-primary)]">
                {t.dashboard.welcome},{" "}
                <span className="text-[var(--accent)]">
                  {user?.name || "Admin"}
                </span>
              </h2>
              <p className="text-[var(--text-secondary)] fluid-sm">
                {t.dashboard.overview}
              </p>
            </header>

            {/* Split layout: Quick Actions & Recent Sessions */}
            <div className="grid gap-6 md:grid-cols-3 stagger-children">
              <QuickActions
                onStartNewChat={handleStartNewChat}
                onResumeProfile={() => router.push("/profile")}
                onConfigureSettings={() => router.push("/settings")}
                accentClasses={accentClasses}
                t={t}
              />
              <RecentSessions
                recentThreads={recentThreads}
                onStartNewChat={handleStartNewChat}
                onResumeSession={(id) =>
                  router.push(`/chat?conversationId=${id}`)
                }
                formatDate={formatDate}
                t={t}
              />
            </div>
          </div>
        </main>
      </div>
    </section>
  );
}
